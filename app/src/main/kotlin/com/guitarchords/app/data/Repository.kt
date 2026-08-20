package com.guitarchords.app.data

import androidx.room.withTransaction
import com.guitarchords.app.sync.RemoteObject
import com.guitarchords.app.sync.SongTextFormat
import kotlinx.coroutines.flow.Flow

class Repository(
    private val db: AppDatabase,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val songVersionDao: SongVersionDao
) {
    fun playlists(): Flow<List<Playlist>> = playlistDao.observeAll()
    fun songs(playlistId: Long): Flow<List<Song>> = songDao.observeByPlaylist(playlistId)
    fun song(id: Long): Flow<Song?> = songDao.observeById(id)
    fun favorites(): Flow<List<Song>> = songDao.observeFavorites()
    fun search(q: String): Flow<List<Song>> = songDao.search(q)
    fun unassigned(): Flow<List<Song>> = songDao.observeUnassigned()
    fun unassignedCount(): Flow<Int> = songDao.countUnassigned()
    fun pendingUploadCount(): Flow<Int> = songDao.countDirty()

    suspend fun moveSong(songId: Long, targetPlaylistId: Long?) {
        val pos = if (targetPlaylistId == null) songDao.nextPositionUnassigned()
                  else songDao.nextPosition(targetPlaylistId)
        songDao.moveSong(songId, targetPlaylistId, pos)
    }

    suspend fun playlist(id: Long): Playlist? = playlistDao.getById(id)
    suspend fun songsOf(playlistId: Long): List<Song> = songDao.getByPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insert(Playlist(name = name))

    suspend fun renamePlaylist(p: Playlist, newName: String) =
        playlistDao.update(p.copy(name = newName))

    /**
     * Borra la lista pero NO sus partituras: estas pasan a la carpeta por
     * defecto "Sin lista" (playlist_id = NULL). Las canciones solo se borran
     * definitivamente desde la papelera.
     */
    suspend fun deletePlaylist(id: Long) {
        songDao.unassignAllFromPlaylist(id)
        playlistDao.deleteById(id)
    }

    suspend fun upsertSong(song: Song): Long {
        // Local edits flag the song dirty so the next sync uploads it.
        val s = song.copy(dirty = true, updatedAt = System.currentTimeMillis())
        return if (s.id == 0L) songDao.insert(s)
               else { songDao.update(s); s.id }
    }

    /** Borrado "normal": va a la papelera, no se elimina de la base de datos. */
    suspend fun deleteSong(id: Long) = songDao.setDeletedAt(id, System.currentTimeMillis())

    suspend fun toggleFavorite(song: Song) =
        songDao.setFavorite(song.id, !song.favorite)

    // ---- Papelera de reciclaje ----
    fun trash(): Flow<List<Song>> = songDao.observeTrash()
    fun trashCount(): Flow<Int> = songDao.countTrash()
    suspend fun restoreFromTrash(id: Long) = songDao.setDeletedAt(id, 0)
    /** Deshacer un envío a la papelera (una o varias de golpe). */
    suspend fun restoreFromTrash(ids: Collection<Long>) = songDao.setDeletedAtFor(ids, 0)
    /** Borrado definitivo (desde la papelera, con confirmación en la UI). */
    suspend fun deleteForever(id: Long) = songDao.deleteById(id)
    suspend fun deleteForever(ids: Collection<Long>) = songDao.deleteByIds(ids)
    /** Purga las partituras que llevan en la papelera más de [maxAgeMillis]. */
    suspend fun purgeExpiredTrash(maxAgeMillis: Long) =
        songDao.purgeTrashOlderThan(System.currentTimeMillis() - maxAgeMillis)

    // ---- Bulk operations (multi-select) ----
    // Una sola sentencia SQL por operación (o una transacción cuando hay que
    // escribir valores distintos por fila): si algo falla no queda a medias.
    suspend fun deleteSongs(ids: Collection<Long>) =
        songDao.setDeletedAtFor(ids, System.currentTimeMillis())

    suspend fun moveSongs(ids: Collection<Long>, targetPlaylistId: Long?) =
        db.withTransaction { ids.forEach { moveSong(it, targetPlaylistId) } }

    /** Persiste un orden manual: la posición pasa a ser el índice en [orderedIds]. */
    suspend fun reorderSongs(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> songDao.setPosition(id, index) }
    }

    suspend fun setFavoriteFor(ids: Collection<Long>, fav: Boolean) =
        songDao.setFavoriteFor(ids, fav)

    suspend fun importPlaylist(exp: PlaylistExport): Long {
        val pid = playlistDao.insert(Playlist(name = exp.name))
        exp.songs.forEachIndexed { idx, s ->
            songDao.insert(
                Song(
                    playlistId = pid,
                    title = s.title,
                    artist = s.artist,
                    genre = s.genre,
                    content = s.content,
                    favorite = s.favorite,
                    capo = s.capo,
                    sourceUrl = s.sourceUrl,
                    position = if (s.position != 0) s.position else idx
                )
            )
        }
        return pid
    }

    suspend fun exportPlaylist(id: Long): PlaylistExport? {
        val p = playlistDao.getById(id) ?: return null
        val songs = songDao.getByPlaylist(id)
        return PlaylistExport(
            name = p.name,
            songs = songs.map {
                SongExport(
                    title = it.title,
                    artist = it.artist,
                    genre = it.genre,
                    content = it.content,
                    favorite = it.favorite,
                    capo = it.capo,
                    sourceUrl = it.sourceUrl,
                    position = it.position
                )
            }
        )
    }

    // ---- Song versions ----

    fun versions(songId: Long): Flow<List<SongVersion>> =
        songVersionDao.observeBySong(songId)

    suspend fun versionOnce(id: Long): SongVersion? = songVersionDao.getById(id)

    suspend fun addVersion(songId: Long, name: String, content: String, capo: Int): Long {
        val pos = songVersionDao.nextPosition(songId)
        return songVersionDao.insert(
            SongVersion(
                songId = songId,
                name = name,
                content = content,
                capo = capo,
                position = pos
            )
        )
    }

    suspend fun updateVersion(version: SongVersion) =
        songVersionDao.update(version.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteVersion(id: Long) = songVersionDao.deleteById(id)

    // ---- R2 sync support ----

    suspend fun songOnce(id: Long): Song? = songDao.getById(id)
    suspend fun songByRemoteKey(key: String): Song? = songDao.getByRemoteKey(key)
    suspend fun dirtySongs(): List<Song> = songDao.dirtySongs()

    /** Soporte para detectar canciones cuyo objeto remoto ya no existe. */
    suspend fun songsWithRemoteKey(): List<Song> = songDao.songsWithRemoteKey()
    suspend fun clearRemoteLink(id: Long) = songDao.clearRemoteLink(id)

    // ---- sincronización con cuenta de usuario (API de Vivace) ----
    suspend fun songByRemoteId(remoteId: String): Song? = songDao.getByRemoteId(remoteId)
    suspend fun songsWithRemoteId(): List<Song> = songDao.songsWithRemoteId()
    suspend fun songsPendingRelink(): List<Song> = songDao.songsPendingRelink()
    /** La partitura ya no está en la cuenta: pasa a ser solo local. */
    suspend fun clearAccountLink(id: Long) = songDao.clearAccountLink(id)

    suspend fun markAccountSynced(
        id: Long, remoteId: String, updatedAt: Long, keepDirty: Boolean = false
    ) = songDao.markAccountSynced(id, remoteId, updatedAt, keepDirty)

    /** Alta local de una partitura que llega de la cuenta. */
    suspend fun importAccountSong(detail: com.guitarchords.app.sync.SongDetail) {
        val parsed = SongTextFormat.decode(detail.content)
        val pid = parsed.playlist?.let { findOrCreatePlaylist(it) }
        val pos = if (pid != null) songDao.nextPosition(pid) else songDao.nextPositionUnassigned()
        songDao.insert(
            Song(
                playlistId = pid,
                title = detail.song.title.ifBlank { parsed.title },
                artist = detail.song.artist.ifBlank { parsed.artist },
                genre = parsed.genre,
                content = parsed.content,
                favorite = parsed.favorite,
                locked = detail.song.locked || parsed.locked,
                capo = if (detail.song.capo > 0) detail.song.capo else parsed.capo,
                sourceUrl = detail.song.sourceUrl.ifBlank { parsed.sourceUrl },
                remoteId = detail.song.id,
                remoteKey = detail.song.r2Key,
                remoteUpdatedAt = detail.song.updatedAt,
                visibility = detail.song.visibility,
                dirty = false,
                position = pos
            )
        )
    }

    /** Sobrescribe la copia local con la del servidor. */
    suspend fun updateFromAccount(local: Song, detail: com.guitarchords.app.sync.SongDetail) {
        val parsed = SongTextFormat.decode(detail.content)
        val pid = parsed.playlist?.let { findOrCreatePlaylist(it) } ?: local.playlistId
        songDao.update(
            local.copy(
                playlistId = pid,
                title = detail.song.title.ifBlank { parsed.title },
                artist = detail.song.artist.ifBlank { parsed.artist },
                genre = parsed.genre,
                content = parsed.content,
                favorite = parsed.favorite,
                locked = detail.song.locked || parsed.locked,
                capo = if (detail.song.capo > 0) detail.song.capo else parsed.capo,
                sourceUrl = detail.song.sourceUrl.ifBlank { parsed.sourceUrl },
                remoteId = detail.song.id,
                remoteUpdatedAt = detail.song.updatedAt,
                visibility = detail.song.visibility,
                dirty = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Aplica candado y visibilidad cuando el contenido no cambió. */
    suspend fun applyRemoteFlags(local: Song, remote: com.guitarchords.app.sync.RemoteSong) {
        if (remote.locked != local.locked) songDao.setLocked(local.id, remote.locked)
        if (remote.visibility != local.visibility) songDao.setVisibility(local.id, remote.visibility)
    }

    /** Cambia quién puede ver la partitura (se subirá en el próximo push). */
    suspend fun setVisibility(id: Long, visibility: String) {
        songDao.setVisibility(id, visibility)
        songOnce(id)?.let { songDao.update(it.copy(dirty = true, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun markSynced(id: Long, key: String, etag: String, updated: Long) =
        songDao.markSynced(id, key, etag, updated)

    /** Pull metadata from the Worker onto a local song without touching content or dirty flag. */
    suspend fun setRemoteTitle(id: Long, title: String) = songDao.setTitle(id, title)
    suspend fun setRemoteArtist(id: Long, artist: String) = songDao.setArtist(id, artist)
    suspend fun setRemoteCapo(id: Long, capo: Int) = songDao.setCapo(id, capo)
    suspend fun setRemoteSourceUrl(id: Long, url: String) = songDao.setSourceUrl(id, url)
    suspend fun setRemoteLocked(id: Long, locked: Boolean) = songDao.setLocked(id, locked)

    private suspend fun findOrCreatePlaylist(name: String): Long =
        playlistDao.getByName(name)?.id ?: playlistDao.insert(Playlist(name = name))

    /** Encode a song as a plain-text file for upload to R2. */
    suspend fun encodeSong(song: Song): String {
        val plName = song.playlistId?.let { playlistDao.getById(it)?.name }
        return SongTextFormat.encode(song, plName)
    }

    /** Insert a brand-new song downloaded from R2. */
    suspend fun importRemoteSong(ro: RemoteObject, text: String) {
        val parsed = SongTextFormat.decode(text)
        val pid = parsed.playlist?.let { findOrCreatePlaylist(it) }
        val pos = if (pid != null) songDao.nextPosition(pid)
                  else songDao.nextPositionUnassigned()
        songDao.insert(
            Song(
                playlistId = pid,
                title = parsed.title,
                artist = parsed.artist,
                genre = parsed.genre,
                content = parsed.content,
                favorite = parsed.favorite,
                locked = parsed.locked,
                capo = parsed.capo,
                sourceUrl = parsed.sourceUrl,
                remoteKey = ro.key,
                remoteEtag = ro.etag,
                remoteUpdatedAt = ro.uploaded,
                dirty = false,
                position = pos
            )
        )
    }

    /** Overwrite an existing local song with the R2 version. */
    suspend fun updateFromRemote(local: Song, ro: RemoteObject, text: String) {
        val parsed = SongTextFormat.decode(text)
        val pid = parsed.playlist?.let { findOrCreatePlaylist(it) } ?: local.playlistId
        songDao.update(
            local.copy(
                playlistId = pid,
                title = parsed.title,
                artist = parsed.artist,
                genre = parsed.genre,
                content = parsed.content,
                favorite = parsed.favorite,
                locked = parsed.locked,
                capo = parsed.capo,
                sourceUrl = parsed.sourceUrl,
                remoteKey = ro.key,
                remoteEtag = ro.etag,
                remoteUpdatedAt = ro.uploaded,
                dirty = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
