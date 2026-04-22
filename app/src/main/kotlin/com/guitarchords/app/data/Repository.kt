package com.guitarchords.app.data

import kotlinx.coroutines.flow.Flow

class Repository(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) {
    fun playlists(): Flow<List<Playlist>> = playlistDao.observeAll()
    fun songs(playlistId: Long): Flow<List<Song>> = songDao.observeByPlaylist(playlistId)
    fun song(id: Long): Flow<Song?> = songDao.observeById(id)

    suspend fun playlist(id: Long): Playlist? = playlistDao.getById(id)
    suspend fun songsOf(playlistId: Long): List<Song> = songDao.getByPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insert(Playlist(name = name))

    suspend fun renamePlaylist(p: Playlist, newName: String) =
        playlistDao.update(p.copy(name = newName))

    suspend fun deletePlaylist(id: Long) = playlistDao.deleteById(id)

    suspend fun upsertSong(song: Song): Long =
        if (song.id == 0L) songDao.insert(song)
        else { songDao.update(song); song.id }

    suspend fun deleteSong(id: Long) = songDao.deleteById(id)

    suspend fun toggleFavorite(song: Song) =
        songDao.setFavorite(song.id, !song.favorite)

    suspend fun importPlaylist(exp: PlaylistExport): Long {
        val pid = playlistDao.insert(Playlist(name = exp.name))
        exp.songs.forEachIndexed { idx, s ->
            songDao.insert(
                Song(
                    playlistId = pid,
                    title = s.title,
                    artist = s.artist,
                    content = s.content,
                    favorite = s.favorite,
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
                    content = it.content,
                    favorite = it.favorite,
                    position = it.position
                )
            }
        )
    }
}
