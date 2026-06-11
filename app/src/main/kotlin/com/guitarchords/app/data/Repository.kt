package com.guitarchords.app.data

import com.guitarchords.app.sync.RemoteObject
import com.guitarchords.app.sync.SongTextFormat
import kotlinx.coroutines.flow.Flow

class Repository(
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

    suspend fun deletePlaylist(id: Long) = playlistDao.deleteById(id)

    suspend fun upsertSong(song: Song): Long {
        // Local edits flag the song dirty so the next sync uploads it.
        val s = song.copy(dirty = true, updatedAt = System.currentTimeMillis())
        return if (s.id == 0L) songDao.insert(s)
               else { songDao.update(s); s.id }
    }

    suspend fun deleteSong(id: Long) = songDao.deleteById(id)

    suspend fun toggleFavorite(song: Song) =
        songDao.setFavorite(song.id, !song.favorite)

    // ---- Bulk operations (multi-select) ----
    suspend fun deleteSongs(ids: Collection<Long>) = ids.forEach { songDao.deleteById(it) }

    suspend fun moveSongs(ids: Collection<Long>, targetPlaylistId: Long?) =
        ids.forEach { moveSong(it, targetPlaylistId) }

    /** Persiste un orden manual: la posición pasa a ser el índice en [orderedIds]. */
    suspend fun reorderSongs(orderedIds: List<Long>) =
        orderedIds.forEachIndexed { index, id -> songDao.setPosition(id, index) }

    suspend fun setFavoriteFor(ids: Collection<Long>, fav: Boolean) =
        ids.forEach { songDao.setFavorite(it, fav) }

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

    suspend fun markSynced(id: Long, key: String, etag: String, updated: Long) =
        songDao.markSynced(id, key, etag, updated)

    /** Pull metadata from the Worker onto a local song without touching content or dirty flag. */
    suspend fun setRemoteTitle(id: Long, title: String) = songDao.setTitle(id, title)
    suspend fun setRemoteArtist(id: Long, artist: String) = songDao.setArtist(id, artist)
    suspend fun setRemoteCapo(id: Long, capo: Int) = songDao.setCapo(id, capo)
    suspend fun setRemoteSourceUrl(id: Long, url: String) = songDao.setSourceUrl(id, url)

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
