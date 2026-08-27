package com.guitarchords.app.sync

import com.guitarchords.app.data.PendingDelete
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import com.guitarchords.app.data.SongVersion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor de sincronización de partituras, listas y versiones.
 *
 * Sustituye a `AccountSyncManager`, que pedía la lista entera y una petición por
 * canción, no sabía borrar en el servidor y dejaba la subida a un botón.
 *
 * Cómo funciona una pasada:
 *  1. **Bajar** por el feed de cambios (`/api/sync/changes`), con un cursor por
 *     flujo, hasta que no quede nada. El texto viene incrustado.
 *  2. **Subir** en tandas todo lo que esté marcado `dirty`, más la cola de
 *     borrados definitivos.
 *  3. **Purgar** lo borrado que el servidor ya ha confirmado.
 *
 * Los conflictos NO se le preguntan a nadie: con la sincronización en segundo
 * plano no hay quien conteste. Gana el servidor como Original y la copia local
 * se guarda como versión alternativa, marcada para subir. Nunca se pierde nada.
 */
class SyncEngine(
    private val repo: Repository,
    private val prefs: SyncPrefs,
    private val clientFactory: (String, String) -> VivaceClient = { url, token -> VivaceClient(url, token) }
) {
    private val mutex = Mutex()

    /** Tope de vueltas al bajar y al subir: un corte de red no debe dejar un bucle vivo. */
    private val maxRondas = 50

    suspend fun sync(): SyncOutcome = mutex.withLock {
        if (!prefs.isLoggedIn) return@withLock SyncOutcome.Skipped
        val client = clientFactory(prefs.baseUrl, prefs.authToken)
        try {
            val bajado = pull(client)
            val subido = push(client)
            repo.purgeSyncedDeletions()
            prefs.lastSync = System.currentTimeMillis()
            prefs.lastSyncError = ""
            SyncOutcome.Done(
                SyncResult(
                    downloaded = bajado.downloaded,
                    uploaded = subido.uploaded,
                    deleted = bajado.deleted + subido.deleted,
                    conflicts = bajado.conflicts + subido.conflicts
                )
            )
        } catch (e: UnauthorizedException) {
            // La sesión ya no vale. Reintentar no arregla nada: se cierra y se
            // deja constancia para que la interfaz lo pueda avisar.
            prefs.clearSession()
            prefs.lastSyncError = e.message.orEmpty()
            SyncOutcome.Failed(SyncFailure.Unauthorized(e.message.orEmpty()))
        } catch (e: IOException) {
            prefs.lastSyncError = e.message.orEmpty()
            SyncOutcome.Failed(SyncFailure.Network(e.message.orEmpty()))
        } catch (e: Exception) {
            prefs.lastSyncError = e.message.orEmpty()
            SyncOutcome.Failed(SyncFailure.Other(e.message.orEmpty()))
        }
    }

    /* --------------------------------- bajar --------------------------------- */

    private suspend fun pull(client: VivaceClient): SyncResult {
        var downloaded = 0
        var deleted = 0
        val conflictos = mutableListOf<ResolvedConflict>()
        var cursors = prefs.cursors()
        var rondas = 0

        do {
            val respuesta = client.changes(cursors)
            for (lista in respuesta.playlists.items) applyPlaylist(lista)
            for (remota in respuesta.songs.items) {
                when (applySong(remota, conflictos)) {
                    Applied.DOWNLOADED -> downloaded++
                    Applied.DELETED -> deleted++
                    Applied.NOTHING -> Unit
                }
            }
            for (version in respuesta.versions.items) applyVersion(version)

            cursors = SyncCursors(
                playlists = respuesta.playlists.cursor.ifBlank { cursors.playlists },
                songs = respuesta.songs.cursor.ifBlank { cursors.songs },
                versions = respuesta.versions.cursor.ifBlank { cursors.versions }
            )
            // Se guarda en cada vuelta: si el proceso muere a mitad, la próxima
            // pasada sigue donde iba en vez de empezar de cero.
            prefs.saveCursors(cursors)
            rondas++
        } while (respuesta.more && rondas < maxRondas)

        return SyncResult(downloaded = downloaded, deleted = deleted, conflicts = conflictos)
    }

    private enum class Applied { DOWNLOADED, DELETED, NOTHING }

    private suspend fun applyPlaylist(remota: RemotePlaylist) {
        val local = repo.playlistByRemoteId(remota.id)
        if (remota.deletedAt > 0) {
            if (local != null && !local.dirty) repo.deletePlaylistRow(local.id)
            return
        }
        if (local == null) {
            // Primera sincronización: casa por nombre con las carpetas que ya
            // existían aquí, para no acabar con "Conciertos" dos veces.
            val porNombre = repo.playlistByName(remota.name)
            if (porNombre != null && porNombre.remoteId == null) {
                repo.markPlaylistSynced(porNombre.id, remota.id)
            } else {
                repo.upsertPlaylist(
                    Playlist(
                        name = remota.name, remoteId = remota.id, position = remota.position,
                        dirty = false, updatedAt = remota.updatedAt, createdAt = remota.createdAt
                    )
                )
            }
            return
        }
        if (!local.dirty) {
            repo.upsertPlaylist(
                local.copy(
                    name = remota.name, position = remota.position,
                    dirty = false, updatedAt = remota.updatedAt
                )
            )
        }
    }

    private suspend fun applySong(
        remota: RemoteSong,
        conflictos: MutableList<ResolvedConflict>
    ): Applied {
        // Re-enlace: lo que se subió con el flujo antiguo se reconoce por su
        // clave de R2, así no se duplica al estrenar la cuenta.
        var local = repo.songByRemoteId(remota.id)
        if (local == null && remota.r2Key != null) {
            val candidata = repo.songsPendingRelink().firstOrNull { it.remoteKey == remota.r2Key }
            if (candidata != null) {
                repo.markAccountSynced(
                    candidata.id, remota.id, remota.updatedAt, remota.rev, keepDirty = candidata.dirty
                )
                local = repo.songOnce(candidata.id)
            }
        }

        val accion = SyncPlan.decidePull(local, remota.rev, remota.deletedAt > 0)
        // A partir de aquí, todas las acciones salvo IMPORT e IGNORE tienen copia
        // local por definición (ver SyncPlan.decidePull).
        val copia = local
        return when (accion) {
            SyncPlan.PullAction.IMPORT -> {
                repo.importAccountSong(remota, remota.content)
                Applied.DOWNLOADED
            }
            SyncPlan.PullAction.DOWNLOAD -> {
                copia?.let { repo.updateFromAccount(it, remota, remota.content) }
                Applied.DOWNLOADED
            }
            SyncPlan.PullAction.CONFLICT -> {
                if (copia != null) {
                    guardarComoVersion(copia, conflictos)
                    repo.updateFromAccount(copia, remota, remota.content)
                }
                Applied.DOWNLOADED
            }
            SyncPlan.PullAction.DELETE_LOCAL -> {
                copia?.let { repo.applyRemoteDeletion(it) }
                Applied.DELETED
            }
            SyncPlan.PullAction.UP_TO_DATE -> {
                if (copia != null && remota.deletedAt == 0L) repo.applyRemoteFlags(copia, remota)
                Applied.NOTHING
            }
            SyncPlan.PullAction.IGNORE -> Applied.NOTHING
        }
    }

    /** Guarda la copia local como versión alternativa antes de que gane el servidor. */
    private suspend fun guardarComoVersion(local: Song, conflictos: MutableList<ResolvedConflict>) {
        val nombre = SyncPlan.conflictVersionName(
            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        )
        repo.addVersion(local.id, nombre, local.content, local.capo)
        conflictos += ResolvedConflict(local.id, local.title, nombre)
    }

    private suspend fun applyVersion(remota: RemoteVersion) {
        val local = repo.versionByRemoteId(remota.id)
        if (remota.deletedAt > 0) {
            if (local != null && !local.dirty) repo.deleteVersionRow(local.id)
            return
        }
        val song = repo.songByRemoteId(remota.songId) ?: return
        if (local == null) {
            repo.upsertVersion(
                SongVersion(
                    songId = song.id, name = remota.name, content = remota.content,
                    capo = remota.capo, sourceUrl = remota.sourceUrl, position = remota.position,
                    remoteId = remota.id, dirty = false, updatedAt = remota.updatedAt
                )
            )
        } else if (!local.dirty) {
            repo.upsertVersion(
                local.copy(
                    name = remota.name, content = remota.content, capo = remota.capo,
                    sourceUrl = remota.sourceUrl, position = remota.position,
                    dirty = false, updatedAt = remota.updatedAt
                )
            )
        }
    }

    /* --------------------------------- subir --------------------------------- */

    private suspend fun push(client: VivaceClient): SyncResult {
        var uploaded = 0
        var deleted = 0
        val conflictos = mutableListOf<ResolvedConflict>()
        var rondas = 0

        while (rondas < maxRondas) {
            val listas = repo.dirtyPlaylists()
            val canciones = repo.dirtySongs().filter { SyncPlan.shouldPush(it) }
            val versiones = repo.dirtyVersions()
            val borrados = repo.pendingDeletes()
            if (listas.isEmpty() && canciones.isEmpty() && versiones.isEmpty() && borrados.isEmpty()) break

            val loteListas = listas.take(LOTE_LISTAS)
            // Los borrados definitivos van primero: liberan sitio en el lote y
            // son lo que impide que una partitura resucite.
            val lotePurgas = borrados.take(LOTE_CANCIONES)
            val loteCanciones = canciones.take(LOTE_CANCIONES - lotePurgas.size)
            val loteVersiones = versiones.take(LOTE_VERSIONES)

            val peticion = PushRequest(
                playlists = loteListas.map { it.toPush() },
                songs = lotePurgas.map {
                    PushSong(clientId = "purge-${it.remoteId}", id = it.remoteId, purge = true)
                } + loteCanciones.map { it.toPush(repo.playlistById(it.playlistId ?: -1L)?.remoteId) },
                versions = loteVersiones.map { it.toPush(repo.songOnce(it.songId)?.remoteId) }
            )
            val respuesta = client.push(peticion)

            aplicarListas(loteListas, respuesta.playlists)
            uploaded += aplicarCanciones(loteCanciones, respuesta.songs, conflictos)
            deleted += aplicarPurgas(lotePurgas, respuesta.songs)
            aplicarVersiones(loteVersiones, respuesta.versions)

            // Si nada avanzó, parar: seguir sería un bucle infinito contra un
            // elemento que el servidor rechaza una y otra vez.
            val quedan = repo.dirtyPlaylists().size + repo.dirtySongs().size +
                repo.dirtyVersions().size + repo.pendingDeletes().size
            val antes = listas.size + canciones.size + versiones.size + borrados.size
            if (quedan >= antes) break
            rondas++
        }
        return SyncResult(uploaded = uploaded, deleted = deleted, conflicts = conflictos)
    }

    private fun Playlist.toPush() = PushPlaylist(
        clientId = "pl-$id", id = remoteId, name = name, position = position,
        deleted = deletedAt > 0
    )

    private fun Song.toPush(playlistRemoteId: String?) = PushSong(
        clientId = "song-$id",
        id = remoteId,
        baseRev = SyncPlan.baseRevFor(this),
        title = title, artist = artist, genre = genre, capo = capo,
        sourceUrl = sourceUrl, locked = locked, favorite = favorite, position = position,
        playlistId = playlistRemoteId,
        playlistClientId = if (playlistRemoteId == null && playlistId != null) "pl-$playlistId" else null,
        content = content,
        deleted = deletedAt > 0
    )

    private fun SongVersion.toPush(songRemoteId: String?) = PushVersion(
        clientId = "ver-$id",
        id = remoteId,
        songId = songRemoteId,
        songClientId = if (songRemoteId == null) "song-$songId" else null,
        name = name, capo = capo, sourceUrl = sourceUrl, position = position,
        content = content, deleted = deletedAt > 0
    )

    private suspend fun aplicarListas(enviadas: List<Playlist>, resultados: List<PushOutcome>) {
        val porId = resultados.associateBy { it.clientId }
        for (lista in enviadas) {
            val r = porId["pl-${lista.id}"] ?: continue
            if (!r.ok) continue
            if (lista.deletedAt > 0) repo.markPlaylistDeletionSynced(lista.id)
            else r.id?.let { repo.markPlaylistSynced(lista.id, it) }
        }
    }

    private suspend fun aplicarCanciones(
        enviadas: List<Song>,
        resultados: List<PushOutcome>,
        conflictos: MutableList<ResolvedConflict>
    ): Int {
        val porId = resultados.associateBy { it.clientId }
        var subidas = 0
        for (song in enviadas) {
            val r = porId["song-${song.id}"] ?: continue
            when {
                r.ok && r.id != null -> {
                    repo.markAccountSynced(song.id, r.id, r.updatedAt, r.rev)
                    subidas++
                }
                r.conflict && r.server != null -> {
                    // El servidor cambió mientras tanto: se guarda lo local como
                    // versión y se adopta la copia del servidor.
                    guardarComoVersion(song, conflictos)
                    repo.updateFromAccount(song, r.server, r.server.content)
                }
                r.gone -> {
                    // Alguien la borró del todo en el servidor. Se suelta el
                    // enlace y sigue sucia: subirá como partitura nueva.
                    repo.clearAccountLink(song.id)
                }
            }
        }
        return subidas
    }

    private suspend fun aplicarPurgas(
        enviadas: List<PendingDelete>,
        resultados: List<PushOutcome>
    ): Int {
        val porId = resultados.associateBy { it.clientId }
        var hechas = 0
        for (item in enviadas) {
            val r = porId["purge-${item.remoteId}"]
            // Un 404 también vale: si ya no está, la lápida ha cumplido.
            if (r != null && (r.ok || r.gone)) {
                repo.clearPendingDelete(item.remoteId)
                hechas++
            }
        }
        return hechas
    }

    private suspend fun aplicarVersiones(enviadas: List<SongVersion>, resultados: List<PushOutcome>) {
        val porId = resultados.associateBy { it.clientId }
        for (version in enviadas) {
            val r = porId["ver-${version.id}"] ?: continue
            if (!r.ok) continue
            if (version.deletedAt > 0) repo.markVersionDeletionSynced(version.id)
            else r.id?.let { repo.markVersionSynced(version.id, it) }
        }
    }

    private companion object {
        // Los mismos topes que acepta el servidor (ver worker/src/sync.js).
        const val LOTE_LISTAS = 50
        const val LOTE_CANCIONES = 20
        const val LOTE_VERSIONES = 20
    }
}

/** Cómo acabó una pasada. */
sealed interface SyncOutcome {
    /** No hay sesión: no hay nada que sincronizar. */
    data object Skipped : SyncOutcome
    data class Done(val result: SyncResult) : SyncOutcome
    data class Failed(val failure: SyncFailure) : SyncOutcome
}
