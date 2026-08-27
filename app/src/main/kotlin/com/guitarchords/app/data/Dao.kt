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
    @Query("SELECT * FROM playlists WHERE deleted_at = 0 ORDER BY position ASC, created_at DESC")
    fun observeAll(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE name = :name AND deleted_at = 0 LIMIT 1")
    suspend fun getByName(name: String): Playlist?

    // ---- sincronización ----
    @Query("SELECT * FROM playlists WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): Playlist?

    @Query("SELECT * FROM playlists WHERE dirty = 1")
    suspend fun dirtyPlaylists(): List<Playlist>

    @Query("SELECT COUNT(*) FROM playlists WHERE dirty = 1")
    suspend fun countDirtyOnce(): Int

    @Query("UPDATE playlists SET remote_id = :remoteId, dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    /** Papelera lógica: la lista se conserva hasta confirmar el borrado remoto. */
    @Query("UPDATE playlists SET deleted_at = :ts, dirty = 1, updated_at = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long)

    /** Borrado ya comunicado: deja de estar sucio y lo barre `purgeSyncedDeletions`. */
    @Query("UPDATE playlists SET dirty = 0 WHERE id = :id")
    suspend fun markDeletionSynced(id: Long)

    @Query("DELETE FROM playlists WHERE deleted_at > 0 AND dirty = 0")
    suspend fun purgeSyncedDeletions()

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
    @Query("SELECT * FROM songs WHERE playlist_id = :playlistId AND deleted_at = 0 ORDER BY position ASC, id ASC")
    fun observeByPlaylist(playlistId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE playlist_id = :playlistId AND deleted_at = 0 ORDER BY position ASC, id ASC")
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

    // ---- Operaciones en lote (una sola sentencia = atómicas) ----
    @Query("UPDATE songs SET favorite = :fav WHERE id IN (:ids)")
    suspend fun setFavoriteFor(ids: Collection<Long>, fav: Boolean)

    @Query("UPDATE songs SET deleted_at = :ts WHERE id IN (:ids)")
    suspend fun setDeletedAtFor(ids: Collection<Long>, ts: Long)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: Collection<Long>)

    @Query("SELECT * FROM songs WHERE favorite = 1 AND deleted_at = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<Song>>

    @Query(
        """
        SELECT * FROM songs
        WHERE deleted_at = 0 AND (
              title LIKE '%' || :q || '%' COLLATE NOCASE
           OR artist LIKE '%' || :q || '%' COLLATE NOCASE
           OR genre LIKE '%' || :q || '%' COLLATE NOCASE
           OR content LIKE '%' || :q || '%' COLLATE NOCASE
        )
        ORDER BY title COLLATE NOCASE ASC
        """
    )
    fun search(q: String): Flow<List<Song>>

    @Query("UPDATE songs SET playlist_id = :newPlaylistId, position = :position WHERE id = :id")
    suspend fun moveSong(id: Long, newPlaylistId: Long?, position: Int)

    /** Saca todas las partituras de una lista a "Sin lista" sin borrarlas. */
    @Query("UPDATE songs SET playlist_id = NULL WHERE playlist_id = :playlistId")
    suspend fun unassignAllFromPlaylist(playlistId: Long)

    @Query("UPDATE songs SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM songs WHERE playlist_id = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM songs WHERE playlist_id IS NULL")
    suspend fun nextPositionUnassigned(): Int

    @Query("SELECT * FROM songs WHERE playlist_id IS NULL AND deleted_at = 0 ORDER BY position ASC, id ASC")
    fun observeUnassigned(): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM songs WHERE playlist_id IS NULL AND deleted_at = 0")
    fun countUnassigned(): Flow<Int>

    // ---- sincronización con cuenta de usuario ----
    /**
     * Deliberadamente NO filtra `deleted_at`: una partitura en la papelera tiene
     * que seguir "ocupando" su identidad remota hasta que el borrado llegue al
     * servidor; si aquí se escondiera, la siguiente pasada la reimportaría como
     * nueva. Quien decide qué hacer con ella es [com.guitarchords.app.sync.SyncPlan].
     */
    @Query("SELECT * FROM songs WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): Song?

    @Query("SELECT * FROM songs WHERE remote_id IS NOT NULL AND deleted_at = 0")
    suspend fun songsWithRemoteId(): List<Song>

    /** Ya estaban subidas con el token compartido pero aún sin id de cuenta. */
    @Query("SELECT * FROM songs WHERE remote_id IS NULL AND remote_key IS NOT NULL AND deleted_at = 0")
    suspend fun songsPendingRelink(): List<Song>

    @Query(
        "UPDATE songs SET remote_id = :remoteId, remote_updated_at = :updated, " +
            "remote_rev = :rev, dirty = :dirty WHERE id = :id"
    )
    suspend fun markAccountSynced(id: Long, remoteId: String, updated: Long, rev: Int, dirty: Boolean)

    /**
     * Marca cambios locales pendientes de subir. Es el único sitio que enciende
     * `dirty` para los cambios "sueltos" (favorito, carpeta, orden, papelera),
     * que antes escribían directos en el DAO y por eso NUNCA se sincronizaban.
     */
    @Query("UPDATE songs SET dirty = 1, updated_at = :ts WHERE id = :id")
    suspend fun touch(id: Long, ts: Long)

    @Query("UPDATE songs SET dirty = 1, updated_at = :ts WHERE id IN (:ids)")
    suspend fun touchAll(ids: Collection<Long>, ts: Long)

    @Query("UPDATE songs SET remote_id = NULL, remote_updated_at = 0 WHERE id = :id")
    suspend fun clearAccountLink(id: Long)

    @Query("UPDATE songs SET visibility = :visibility WHERE id = :id")
    suspend fun setVisibility(id: Long, visibility: String)

    /**
     * Todo lo que falta por subir, PAPELERA INCLUIDA: una partitura borrada aquí
     * tiene que llegar al servidor como borrada. Al filtrar `deleted_at = 0` los
     * borrados no salían nunca del dispositivo y la partitura volvía a bajarse.
     */
    @Query("SELECT * FROM songs WHERE dirty = 1")
    suspend fun dirtySongs(): List<Song>

    @Query("SELECT COUNT(*) FROM songs WHERE dirty = 1")
    fun countDirty(): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE dirty = 1")
    suspend fun countDirtyOnce(): Int

    /** El candado lo manda el Worker; se aplica sin tocar content/dirty. */
    @Query("UPDATE songs SET locked = :locked WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean)

    // ---- Papelera de reciclaje ----
    @Query("SELECT * FROM songs WHERE deleted_at > 0 ORDER BY deleted_at DESC")
    fun observeTrash(): Flow<List<Song>>

    @Query("SELECT COUNT(*) FROM songs WHERE deleted_at > 0")
    fun countTrash(): Flow<Int>

    /** Mueve a la papelera (ts>0) o restaura (ts=0). */
    @Query("UPDATE songs SET deleted_at = :ts WHERE id = :id")
    suspend fun setDeletedAt(id: Long, ts: Long)

    /**
     * Purga definitiva de lo que lleva en la papelera más del límite.
     *
     * Solo purga lo que YA está sincronizado (`dirty = 0`): si se borrara la
     * fila con el borrado aún sin comunicar, el servidor conservaría la
     * partitura y la siguiente sincronización la bajaría otra vez como nueva.
     */
    @Query("DELETE FROM songs WHERE deleted_at > 0 AND deleted_at < :cutoff AND dirty = 0")
    suspend fun purgeTrashOlderThan(cutoff: Long)

    /** Partituras ya borradas en el servidor: la fila local puede irse. */
    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun purgeRow(id: Long)
}

@Dao
interface CustomChordDao {
    /** Solo acordes vivos (sin tombstones): alimenta la caché de diagramas. */
    @Query("SELECT * FROM custom_chords WHERE deleted_at = 0 ORDER BY chord_key ASC, position ASC, id ASC")
    suspend fun all(): List<CustomChord>

    /** Todo, incluidos tombstones: para construir el blob de sincronización. */
    @Query("SELECT * FROM custom_chords ORDER BY chord_key ASC, position ASC, id ASC")
    suspend fun allForSync(): List<CustomChord>

    @Query("SELECT * FROM custom_chords WHERE uuid = :uuid LIMIT 1")
    suspend fun getByUuid(uuid: String): CustomChord?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM custom_chords WHERE chord_key = :key AND deleted_at = 0")
    suspend fun nextPosition(key: String): Int

    @Insert
    suspend fun insert(chord: CustomChord): Long

    /** Edición local de la digitación: marca dirty para subir el cambio. */
    @Query("UPDATE custom_chords SET frets = :frets, dirty = 1, updated_at = :ts WHERE id = :id")
    suspend fun updateFrets(id: Long, frets: String, ts: Long)

    /** Upsert por uuid al aplicar la fusión del servidor (no marca dirty). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(chord: CustomChord): Long

    @Query(
        "UPDATE custom_chords SET chord_key = :key, frets = :frets, position = :position, " +
            "deleted_at = :deletedAt, updated_at = :updatedAt, dirty = 0 WHERE uuid = :uuid"
    )
    suspend fun applyRemote(
        uuid: String, key: String, frets: String, position: Int,
        deletedAt: Long, updatedAt: Long
    )

    /** Borrado lógico: deja un tombstone que se propaga al sincronizar. */
    @Query("UPDATE custom_chords SET deleted_at = :ts, dirty = 1, updated_at = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long)

    /**
     * Limpia la marca SOLO de los uuid dados. Antes se limpiaba todo de golpe,
     * lo que se llevaba por delante las ediciones hechas mientras corría la
     * sincronización: quedaban como subidas sin haberse subido.
     */
    @Query("UPDATE custom_chords SET dirty = 0 WHERE uuid IN (:uuids)")
    suspend fun clearDirtyFor(uuids: Collection<String>)

    @Query("SELECT COUNT(*) FROM custom_chords WHERE dirty = 1")
    fun countDirty(): Flow<Int>
}

@Dao
interface SongVersionDao {
    @Query("SELECT * FROM song_versions WHERE song_id = :songId AND deleted_at = 0 ORDER BY position ASC, id ASC")
    fun observeBySong(songId: Long): Flow<List<SongVersion>>

    @Query("SELECT * FROM song_versions WHERE id = :id")
    suspend fun getById(id: Long): SongVersion?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM song_versions WHERE song_id = :songId")
    suspend fun nextPosition(songId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: SongVersion): Long

    @Update
    suspend fun update(version: SongVersion)

    @Query("DELETE FROM song_versions WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ---- sincronización ----
    @Query("SELECT * FROM song_versions WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): SongVersion?

    @Query("SELECT * FROM song_versions WHERE dirty = 1")
    suspend fun dirtyVersions(): List<SongVersion>

    @Query("SELECT COUNT(*) FROM song_versions WHERE dirty = 1")
    suspend fun countDirtyOnce(): Int

    @Query("UPDATE song_versions SET remote_id = :remoteId, dirty = 0 WHERE id = :id")
    suspend fun markSynced(id: Long, remoteId: String)

    /** Borrado lógico: la lápida tiene que llegar al servidor antes de purgar. */
    @Query("UPDATE song_versions SET deleted_at = :ts, dirty = 1, updated_at = :ts WHERE id = :id")
    suspend fun softDelete(id: Long, ts: Long)

    @Query("UPDATE song_versions SET dirty = 0 WHERE id = :id")
    suspend fun markDeletionSynced(id: Long)

    @Query("DELETE FROM song_versions WHERE deleted_at > 0 AND dirty = 0")
    suspend fun purgeSyncedDeletions()
}

/**
 * Cola de borrados pendientes. Ver [PendingDelete]: sin esto, borrar del todo
 * una partitura ya sincronizada la dejaba viva en el servidor y volvía a bajar.
 */
@Dao
interface PendingDeleteDao {
    @Query("SELECT * FROM pending_deletes ORDER BY created_at ASC")
    suspend fun all(): List<PendingDelete>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(item: PendingDelete)

    @Query("DELETE FROM pending_deletes WHERE remote_id = :remoteId")
    suspend fun clear(remoteId: String)

    @Query("SELECT COUNT(*) FROM pending_deletes")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_deletes")
    suspend fun countOnce(): Int
}
