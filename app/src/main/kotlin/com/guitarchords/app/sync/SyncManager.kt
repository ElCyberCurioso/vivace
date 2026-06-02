package com.guitarchords.app.sync

import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import java.util.UUID

/**
 * Two-way differential sync between the local database and an R2 bucket.
 *
 * Pull: download objects that are new on the server or changed since the last
 *       sync. If a changed object was also edited locally -> [SyncConflict].
 * Push: upload songs flagged `dirty` (created or edited on the device).
 *
 * Only new/changed content moves in either direction; unchanged songs are
 * compared by ETag and skipped.
 */
class SyncManager(private val repo: Repository) {

    suspend fun sync(client: R2Client): SyncResult {
        val remote = client.list()
        val remoteByKey = remote.associateBy { it.key }
        var downloaded = 0
        var uploaded = 0
        val conflicts = mutableListOf<SyncConflict>()

        // ---- PULL ----
        for (ro in remote) {
            val local = repo.songByRemoteKey(ro.key)
            when {
                local == null -> {
                    val content = client.get(ro.key)
                    repo.importRemoteSong(ro, content.text)
                    downloaded++
                }
                local.remoteEtag != ro.etag -> {
                    if (local.dirty) {
                        conflicts += SyncConflict(
                            songId = local.id,
                            title = local.title,
                            remoteKey = ro.key,
                            localUpdatedAt = local.updatedAt,
                            remoteUpdatedAt = ro.uploaded
                        )
                        fillTitleFromRemote(local, ro)   // title still flows during a content conflict
                    } else {
                        val content = client.get(ro.key)
                        repo.updateFromRemote(local, ro, content.text)
                        downloaded++
                    }
                }
                else -> fillTitleFromRemote(local, ro)   // ETag matches: backfill a missing title
            }
        }

        // ---- PUSH ----
        for (song in repo.dirtySongs()) {
            val key = song.remoteKey
            if (key == null) {
                // Brand-new local song: assign a stable remote key.
                val newKey = "songs/" + UUID.randomUUID() + ".txt"
                val res = client.put(newKey, repo.encodeSong(song))
                repo.markSynced(song.id, res.key.ifBlank { newKey }, res.etag, res.uploaded)
                uploaded++
            } else {
                val ro = remoteByKey[key]
                if (ro != null && ro.etag != song.remoteEtag) {
                    // Server moved too -> already recorded as a conflict in PULL.
                    continue
                }
                val res = client.put(key, repo.encodeSong(song))
                repo.markSynced(song.id, key, res.etag, res.uploaded)
                uploaded++
            }
        }

        return SyncResult(downloaded, uploaded, conflicts)
    }

    /**
     * Copy the metadata the Worker reports (title/artist/capo from R2 customMetadata) onto a
     * local song that has none yet. Cheap: no body download, content and dirty flag untouched.
     * Lets values set in the Worker reach songs whose content did not change, and never
     * clobbers a value the user already set on the device.
     */
    private suspend fun fillTitleFromRemote(local: Song, ro: RemoteObject) {
        val remoteTitle = ro.title.trim()
        val needsTitle = local.title.isBlank() || local.title == "Sin título"
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
