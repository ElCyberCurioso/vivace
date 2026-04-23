package com.guitarchords.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    fun observeAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    @Update
    suspend fun update(playlist: Playlist)

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE playlist_id = :playlistId ORDER BY position ASC, id ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE playlist_id = :playlistId ORDER BY position ASC, id ASC")
    suspend fun getByPlaylist(playlistId: Long): List<Song>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun observeById(id: Long): Flow<Song?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song): Long

    @Update
    suspend fun update(song: Song)

    @Delete
    suspend fun delete(song: Song)

    @Query("UPDATE songs SET favorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM songs WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<Song>>

    @Query(
        """
        SELECT * FROM songs
        WHERE title LIKE '%' || :q || '%' COLLATE NOCASE
           OR artist LIKE '%' || :q || '%' COLLATE NOCASE
           OR genre LIKE '%' || :q || '%' COLLATE NOCASE
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun search(q: String): Flow<List<Song>>

    @Query("UPDATE songs SET playlist_id = :newPlaylistId, position = :position WHERE id = :id")
    suspend fun moveSong(id: Long, newPlaylistId: Long?, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM songs WHERE playlist_id = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM songs WHERE playlist_id IS NULL")
    suspend fun nextPositionUnassigned(): Int

    @Query("SELECT * FROM songs WHERE playlist_id IS NULL ORDER BY position ASC, id ASC")
    fun observeUnassigned(): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM songs WHERE playlist_id IS NULL")
    fun countUnassigned(): Flow<Int>
}
