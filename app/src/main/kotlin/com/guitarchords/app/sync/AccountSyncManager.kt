package com.guitarchords.app.sync

import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song

/**
 * Sincronización con la cuenta de Vivace (API multiusuario). Mantiene el modelo
 * de siempre, ahora sobre partituras con dueño:
 *
 *  - Pull automático: lo nuevo o modificado en el servidor baja solo, salvo que
 *    también se haya editado en el dispositivo (entonces es conflicto).
 *  - Push confirmado: las canciones con cambios locales se listan y solo suben
 *    cuando el usuario acepta.
 *  - La papelera manda: lo borrado en el dispositivo no resucita.
 *
 * La primera sincronización tras iniciar sesión **re-enlaza** las partituras
 * que ya estaban subidas con el token compartido: se reconocen por su clave de
 * R2, así no se duplican.
 */
class AccountSyncManager(private val repo: Repository) {

    suspend fun sync(client: VivaceClient): SyncResult {
        val remote = client.listSongs()
        var downloaded = 0
        val conflicts = mutableListOf<SyncConflict>()

        // Re-enlace único: partituras que ya existían con el flujo anterior.
        relinkByRemoteKey(remote)

        for (ro in remote) {
            val local = repo.songByRemoteId(ro.id)
            when {
                local == null -> {
                    val detail = client.getSong(ro.id)
                    repo.importAccountSong(detail)
                    downloaded++
                }
                local.deletedAt > 0 -> Unit          // en la papelera: no se toca
                ro.updatedAt > local.remoteUpdatedAt -> {
                    if (local.dirty) {
                        conflicts += SyncConflict(
                            songId = local.id,
                            title = local.title,
                            remoteKey = ro.id,
                            localUpdatedAt = local.updatedAt,
                            remoteUpdatedAt = ro.updatedAt
                        )
                    } else {
                        val detail = client.getSong(ro.id)
                        repo.updateFromAccount(local, detail)
                        downloaded++
                    }
                }
                else -> repo.applyRemoteFlags(local, ro)   // candado/visibilidad
            }
        }

        // Lo que ya no está en el servidor deja de considerarse sincronizado.
        val ids = remote.mapTo(HashSet()) { it.id }
        if (ids.isNotEmpty()) {
            repo.songsWithRemoteId()
                .filter { it.remoteId !in ids }
                .forEach { repo.clearAccountLink(it.id) }
        }

        val conflictIds = conflicts.mapTo(HashSet()) { it.songId }
        val pending = repo.dirtySongs()
            .filter { it.id !in conflictIds }
            .map { PendingUpload(it.id, it.title, isNew = it.remoteId == null) }

        return SyncResult(downloaded, 0, pending, conflicts)
    }

    /** Sube las canciones confirmadas. Devuelve cuántas se enviaron. */
    suspend fun push(client: VivaceClient, songIds: List<Long>): Int {
        var uploaded = 0
        for (id in songIds) {
            val song = repo.songOnce(id) ?: continue
            if (!song.dirty) continue
            val content = repo.encodeSong(song)
            val remoteId = song.remoteId
            val saved = if (remoteId == null) {
                client.createSong(
                    song.title, song.artist, song.genre, song.capo,
                    song.sourceUrl, song.locked, song.visibility, content
                )
            } else {
                client.updateSong(
                    remoteId, song.title, song.artist, song.genre, song.capo,
                    song.sourceUrl, song.locked, song.visibility, content
                )
            }
            repo.markAccountSynced(song.id, saved.id, saved.updatedAt)
            uploaded++
        }
        return uploaded
    }

    /** Resuelve un conflicto quedándose con la versión local o con la del servidor. */
    suspend fun resolveConflict(client: VivaceClient, conflict: SyncConflict, keepLocal: Boolean) {
        val song = repo.songOnce(conflict.songId) ?: return
        val remoteId = song.remoteId ?: conflict.remoteKey
        if (keepLocal) {
            val saved = client.updateSong(
                remoteId, song.title, song.artist, song.genre, song.capo,
                song.sourceUrl, song.locked, song.visibility, repo.encodeSong(song)
            )
            repo.markAccountSynced(song.id, saved.id, saved.updatedAt)
        } else {
            repo.updateFromAccount(song, client.getSong(remoteId))
        }
    }

    /**
     * Enlaza por clave de R2 las partituras que se sincronizaron con el token
     * compartido: el servidor devuelve esa clave para las partituras propias,
     * así que se puede casar cada una con su id nuevo sin duplicar nada.
     */
    private suspend fun relinkByRemoteKey(remote: List<RemoteSong>) {
        val byKey = remote.mapNotNull { r -> r.r2Key?.let { it to r } }.toMap()
        if (byKey.isEmpty()) return
        for (local in repo.songsPendingRelink()) {
            val match = byKey[local.remoteKey] ?: continue
            repo.markAccountSynced(local.id, match.id, match.updatedAt, keepDirty = local.dirty)
        }
    }
}
