package com.guitarchords.app.sync

import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import java.util.UUID

/**
 * Two-way differential sync between the local database and an R2 bucket,
 * modelled after git:
 *
 * Pull: applied automatically — objects that are new on the server or changed
 *       since the last sync are downloaded, UNLESS the song was also edited
 *       locally; then it becomes a [SyncConflict] and nothing moves until the
 *       user resolves that song explicitly ([resolveConflict]).
 * Push: never automatic. [sync] only reports the dirty songs as
 *       [PendingUpload]s; the caller must confirm and invoke [push].
 *
 * Only new/changed content moves in either direction; unchanged songs are
 * compared by ETag and skipped.
 */
class SyncManager(private val repo: Repository) {

    suspend fun sync(client: R2Client): SyncResult {
        val remote = client.list()
        var downloaded = 0
        val conflicts = mutableListOf<SyncConflict>()

        // ---- PULL ----
        for (ro in remote) {
            val local = repo.songByRemoteKey(ro.key)
            when (SyncPolicy.decidePull(local, ro.etag)) {
                PullAction.IMPORT -> {
                    val content = client.get(ro.key)
                    repo.importRemoteSong(ro, content.text)
                    downloaded++
                }
                PullAction.DOWNLOAD -> {
                    val content = client.get(ro.key)
                    repo.updateFromRemote(local!!, ro, content.text)
                    downloaded++
                }
                PullAction.CONFLICT -> {
                    conflicts += SyncConflict(
                        songId = local!!.id,
                        title = local.title,
                        remoteKey = ro.key,
                        localUpdatedAt = local.updatedAt,
                        remoteUpdatedAt = ro.uploaded
                    )
                    fillTitleFromRemote(local, ro)   // title still flows during a content conflict
                }
                PullAction.METADATA_ONLY -> fillTitleFromRemote(local!!, ro)
                // En la papelera: no se descarga ni se pide resolver nada.
                PullAction.SKIP_TRASHED -> Unit
            }
        }

        // ---- HUÉRFANAS ----
        // Lo que ya no está en el bucket deja de considerarse sincronizado
        // (el SyncBadge pasará a "solo local"). No se borra nada del dispositivo.
        val remoteKeys = remote.mapTo(HashSet()) { it.key }
        SyncPolicy.orphanIds(repo.songsWithRemoteKey(), remoteKeys)
            .forEach { repo.clearRemoteLink(it) }

        // ---- PUSH PLAN ----
        // Dirty songs are only reported; the upload happens in [push] once the
        // user confirms. Songs already in conflict are excluded: they must be
        // resolved one by one first.
        val conflictIds = conflicts.mapTo(HashSet()) { it.songId }
        val pending = repo.dirtySongs()
            .filter { it.id !in conflictIds }
            .map { PendingUpload(it.id, it.title, isNew = it.remoteKey == null) }

        return SyncResult(downloaded, 0, pending, conflicts)
    }

    /** Upload the confirmed dirty songs. Returns how many were actually sent. */
    suspend fun push(client: R2Client, songIds: List<Long>): Int {
        var uploaded = 0
        for (id in songIds) {
            val song = repo.songOnce(id) ?: continue
            if (!song.dirty) continue
            // Brand-new local song: assign a stable remote key.
            val key = song.remoteKey ?: ("songs/" + UUID.randomUUID() + ".txt")
            val res = client.put(key, repo.encodeSong(song))
            repo.markSynced(song.id, res.key.ifBlank { key }, res.etag, res.uploaded)
            uploaded++
        }
        return uploaded
    }

    /**
     * Copy the metadata the Worker reports (title/artist/capo from R2 customMetadata) onto a
     * local song that has none yet. Cheap: no body download, content and dirty flag untouched.
     * Lets values set in the Worker reach songs whose content did not change, and never
     * clobbers a value the user already set on the device.
     */
    private suspend fun fillTitleFromRemote(local: Song, ro: RemoteObject) {
        val remoteTitle = ro.title.trim()
        val needsTitle = local.title.isBlank() || local.title == SongTextFormat.UNTITLED
        if (remoteTitle.isNotEmpty() && needsTitle && local.title != remoteTitle) {
            repo.setRemoteTitle(local.id, remoteTitle)
        }

        val remoteArtist = ro.artist.trim()
        if (remoteArtist.isNotEmpty() && local.artist.isBlank() && local.artist != remoteArtist) {
            repo.setRemoteArtist(local.id, remoteArtist)
        }

        val remoteCapo = ro.capo.trim().toIntOrNull()?.coerceIn(0, 12)
        if (remoteCapo != null && remoteCapo > 0 && local.capo == 0) {
            repo.setRemoteCapo(local.id, remoteCapo)
        }

        val remoteUrl = ro.url.trim()
        if (remoteUrl.isNotEmpty() && local.sourceUrl.isBlank()) {
            repo.setRemoteSourceUrl(local.id, remoteUrl)
        }

        // El candado es autoritativo del Worker: se aplica aunque el ETag
        // coincida (p. ej. metadata que cambió sin tocar el cuerpo).
        val remoteLocked = ro.locked.trim().equals("true", ignoreCase = true)
        if (remoteLocked != local.locked) {
            repo.setRemoteLocked(local.id, remoteLocked)
        }
    }

    /** Resolve one conflict: keep the device version (upload) or the server version (download). */
    suspend fun resolveConflict(client: R2Client, conflict: SyncConflict, keepLocal: Boolean) {
        val song = repo.songOnce(conflict.songId) ?: return
        if (keepLocal) {
            val res = client.put(conflict.remoteKey, repo.encodeSong(song))
            repo.markSynced(song.id, conflict.remoteKey, res.etag, res.uploaded)
        } else {
            val content = client.get(conflict.remoteKey)
            repo.updateFromRemote(
                song,
                RemoteObject(conflict.remoteKey, content.etag, 0, content.uploaded),
                content.text
            )
        }
    }
}
