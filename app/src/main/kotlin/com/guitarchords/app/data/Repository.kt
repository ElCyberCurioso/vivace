package com.guitarchords.app.data

import androidx.room.withTransaction
import com.guitarchords.app.sync.RemoteSong
import com.guitarchords.app.sync.SongTextFormat
import kotlinx.coroutines.flow.Flow

class Repository(
    private val db: AppDatabase,
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val songVersionDao: SongVersionDao,
    private val pendingDeleteDao: PendingDeleteDao
) {
    private fun now() = System.currentTimeMillis()

    /**
     * Aviso de "aquí ha cambiado algo que hay que subir". Lo rellena la clase
     * Application para encolar el SyncWorker; en los tests se queda a null.
     *
     * Es lo que convierte la sincronización en automática: antes solo se subía
     * si el usuario entraba en Ajustes y pulsaba un botón.
     */
    var onLocalChange: (() -> Unit)? = null

    private fun notifyChange() { onLocalChange?.invoke() }
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
        songDao.touch(songId, now())
        notifyChange()
    }

    suspend fun playlist(id: Long): Playlist? = playlistDao.getById(id)
    suspend fun songsOf(playlistId: Long): List<Song> = songDao.getByPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insert(Playlist(name = name, dirty = true, updatedAt = now()))
            .also { notifyChange() }

    suspend fun renamePlaylist(p: Playlist, newName: String) {
        playlistDao.update(p.copy(name = newName, dirty = true, updatedAt = now()))
        notifyChange()
    }

    /**
     * Borra la lista pero NO sus partituras: estas pasan a la carpeta por
     * defecto "Sin lista" (playlist_id = NULL). Las canciones solo se borran
     * definitivamente desde la papelera.
     *
     * El borrado es lógico y las partituras quedan `dirty`: la lista sigue en la
     * tabla hasta que el servidor confirma el borrado, y las canciones tienen
     * que subir su nueva carpeta (ninguna).
     */
    suspend fun deletePlaylist(id: Long) = db.withTransaction {
        val afectadas = songDao.getByPlaylist(id).map { it.id }
        songDao.unassignAllFromPlaylist(id)
        if (afectadas.isNotEmpty()) songDao.touchAll(afectadas, now())
        val lista = playlistDao.getById(id)
        if (lista?.remoteId == null) playlistDao.deleteById(id)   // nunca salió de aquí
        else playlistDao.softDelete(id, now())
        notifyChange()
    }

    suspend fun upsertSong(song: Song): Long {
        // Local edits flag the song dirty so the next sync uploads it.
        val s = song.copy(dirty = true, updatedAt = System.currentTimeMillis())
        val id = if (s.id == 0L) songDao.insert(s) else { songDao.update(s); s.id }
        notifyChange()
        return id
    }

    /** Borrado "normal": va a la papelera, no se elimina de la base de datos. */
    suspend fun deleteSong(id: Long) = db.withTransaction {
        songDao.setDeletedAt(id, now())
        songDao.touch(id, now())      // la papelera también hay que contarla arriba
        notifyChange()
    }

    /**
     * Marcar favorito, mover de carpeta o reordenar son ediciones como
     * cualquier otra: sin este `touch` escribían directas en el DAO, la fila no
     * quedaba `dirty` y el cambio no se subía jamás.
     */
    suspend fun toggleFavorite(song: Song) = db.withTransaction {
        songDao.setFavorite(song.id, !song.favorite)
        songDao.touch(song.id, now())
        notifyChange()
    }

    // ---- Papelera de reciclaje ----
    fun trash(): Flow<List<Song>> = songDao.observeTrash()
    fun trashCount(): Flow<Int> = songDao.countTrash()
    suspend fun restoreFromTrash(id: Long) = db.withTransaction {
        songDao.setDeletedAt(id, 0)
        songDao.touch(id, now())
        notifyChange()
    }
    /** Deshacer un envío a la papelera (una o varias de golpe). */
    suspend fun restoreFromTrash(ids: Collection<Long>) = db.withTransaction {
        songDao.setDeletedAtFor(ids, 0)
        songDao.touchAll(ids, now())
        notifyChange()
    }

    /**
     * Borrado definitivo (desde la papelera, con confirmación en la UI).
     *
     * Antes de que desaparezca la fila se deja la lápida en [PendingDelete]: sin
     * ella el servidor conservaba la partitura y la siguiente sincronización la
     * bajaba otra vez como nueva.
     */
    suspend fun deleteForever(id: Long) = deleteForever(listOf(id))

    suspend fun deleteForever(ids: Collection<Long>) = db.withTransaction {
        for (id in ids) {
            val song = songDao.getById(id) ?: continue
            song.remoteId?.let {
                pendingDeleteDao.enqueue(PendingDelete(remoteId = it, kind = "song", purge = true))
            }
        }
        songDao.deleteByIds(ids)
        notifyChange()
    }
    /** Purga las partituras que llevan en la papelera más de [maxAgeMillis]. */
    suspend fun purgeExpiredTrash(maxAgeMillis: Long) =
        songDao.purgeTrashOlderThan(System.currentTimeMillis() - maxAgeMillis)

    // ---- Bulk operations (multi-select) ----
    // Una sola sentencia SQL por operación (o una transacción cuando hay que
    // escribir valores distintos por fila): si algo falla no queda a medias.
    suspend fun deleteSongs(ids: Collection<Long>) = db.withTransaction {
        songDao.setDeletedAtFor(ids, now())
        songDao.touchAll(ids, now())
        notifyChange()
    }

    suspend fun moveSongs(ids: Collection<Long>, targetPlaylistId: Long?) =
        db.withTransaction { ids.forEach { moveSong(it, targetPlaylistId) } }

    /** Persiste un orden manual: la posición pasa a ser el índice en [orderedIds]. */
    suspend fun reorderSongs(orderedIds: List<Long>) = db.withTransaction {
        orderedIds.forEachIndexed { index, id -> songDao.setPosition(id, index) }
        songDao.touchAll(orderedIds, now())
        notifyChange()
    }

    suspend fun setFavoriteFor(ids: Collection<Long>, fav: Boolean) = db.withTransaction {
        songDao.setFavoriteFor(ids, fav)
        songDao.touchAll(ids, now())
        notifyChange()
    }

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
                position = pos,
                dirty = true
            )
        ).also { notifyChange() }
    }

    suspend fun updateVersion(version: SongVersion) {
        songVersionDao.update(version.copy(dirty = true, updatedAt = System.currentTimeMillis()))
        notifyChange()
    }

    /**
     * Borrado lógico: si la versión ya existe en el servidor hay que decírselo,
     * y para eso la fila tiene que sobrevivir hasta que se confirme.
     */
    suspend fun deleteVersion(id: Long) = db.withTransaction {
        val version = songVersionDao.getById(id)
        if (version?.remoteId == null) songVersionDao.deleteById(id)
        else songVersionDao.softDelete(id, now())
        notifyChange()
    }

    // ---- soporte de sincronización de listas y versiones ----
    suspend fun dirtyPlaylists(): List<Playlist> = playlistDao.dirtyPlaylists()
    suspend fun dirtyVersions(): List<SongVersion> = songVersionDao.dirtyVersions()
    suspend fun playlistById(id: Long): Playlist? = playlistDao.getById(id)
    suspend fun playlistByRemoteId(remoteId: String): Playlist? = playlistDao.getByRemoteId(remoteId)
    suspend fun versionByRemoteId(remoteId: String): SongVersion? = songVersionDao.getByRemoteId(remoteId)
    suspend fun markPlaylistSynced(id: Long, remoteId: String) = playlistDao.markSynced(id, remoteId)
    suspend fun markVersionSynced(id: Long, remoteId: String) = songVersionDao.markSynced(id, remoteId)
    suspend fun upsertPlaylist(playlist: Playlist): Long = playlistDao.insert(playlist)
    suspend fun upsertVersion(version: SongVersion): Long = songVersionDao.insert(version)
    suspend fun playlistByName(name: String): Playlist? = playlistDao.getByName(name)
    /** Quita la fila de la lista y deja sus partituras en "Sin lista". */
    suspend fun deletePlaylistRow(id: Long) = db.withTransaction {
        songDao.unassignAllFromPlaylist(id)
        playlistDao.deleteById(id)
    }
    suspend fun deleteVersionRow(id: Long) = songVersionDao.deleteById(id)
    suspend fun markPlaylistDeletionSynced(id: Long) = playlistDao.markDeletionSynced(id)
    suspend fun markVersionDeletionSynced(id: Long) = songVersionDao.markDeletionSynced(id)
    suspend fun purgeSyncedDeletions() {
        playlistDao.purgeSyncedDeletions()
        songVersionDao.purgeSyncedDeletions()
    }

    /**
     * ¿Queda algo por subir? Lo consulta el SyncWorker al terminar: si alguien
     * editó mientras corría la pasada, pide otra en vez de dejar el cambio
     * esperando a la sincronización periódica.
     */
    suspend fun hasPendingChanges(): Boolean =
        songDao.countDirtyOnce() > 0 || playlistDao.countDirtyOnce() > 0 ||
            songVersionDao.countDirtyOnce() > 0 || pendingDeleteDao.countOnce() > 0

    // ---- cola de borrados ----
    suspend fun pendingDeletes(): List<PendingDelete> = pendingDeleteDao.all()
    suspend fun clearPendingDelete(remoteId: String) = pendingDeleteDao.clear(remoteId)
    fun pendingDeleteCount(): Flow<Int> = pendingDeleteDao.count()
    suspend fun purgeRow(id: Long) = songDao.purgeRow(id)

    // ---- soporte de sincronización ----

    suspend fun songOnce(id: Long): Song? = songDao.getById(id)
    suspend fun dirtySongs(): List<Song> = songDao.dirtySongs()

    // ---- sincronización con cuenta de usuario (API de Vivace) ----
    suspend fun songByRemoteId(remoteId: String): Song? = songDao.getByRemoteId(remoteId)
    suspend fun songsWithRemoteId(): List<Song> = songDao.songsWithRemoteId()
    suspend fun songsPendingRelink(): List<Song> = songDao.songsPendingRelink()
    /** La partitura ya no está en la cuenta: pasa a ser solo local. */
    suspend fun clearAccountLink(id: Long) = songDao.clearAccountLink(id)

    suspend fun markAccountSynced(
        id: Long, remoteId: String, updatedAt: Long, rev: Int, keepDirty: Boolean = false
    ) = songDao.markAccountSynced(id, remoteId, updatedAt, rev, keepDirty)

    /**
     * Alta local de una partitura que llega de la cuenta.
     *
     * Título, artista, carpeta y favorito vienen ya como CAMPOS de la API. Las
     * cabeceras del texto solo se leen como respaldo, para los ficheros que
     * siguen en R2 desde antes de que la base conociera estos datos.
     */
    suspend fun importAccountSong(remote: RemoteSong, content: String) {
        val parsed = SongTextFormat.decode(content)
        songDao.insert(
            Song(
                playlistId = localPlaylistId(remote.playlistId),
                title = remote.title.ifBlank { parsed.title },
                artist = remote.artist.ifBlank { parsed.artist },
                genre = remote.genre.ifBlank { parsed.genre },
                content = parsed.content,
                favorite = remote.favorite || parsed.favorite,
                locked = remote.locked || parsed.locked,
                capo = if (remote.capo > 0) remote.capo else parsed.capo,
                sourceUrl = remote.sourceUrl.ifBlank { parsed.sourceUrl },
                remoteId = remote.id,
                remoteKey = remote.r2Key,
                remoteUpdatedAt = remote.updatedAt,
                remoteRev = remote.rev,
                visibility = remote.visibility,
                dirty = false,
                position = remote.position
            )
        )
    }

    /** Sobrescribe la copia local con la del servidor. */
    suspend fun updateFromAccount(local: Song, remote: RemoteSong, content: String) {
        val parsed = SongTextFormat.decode(content)
        songDao.update(
            local.copy(
                playlistId = localPlaylistId(remote.playlistId) ?: local.playlistId,
                title = remote.title.ifBlank { parsed.title },
                artist = remote.artist.ifBlank { parsed.artist },
                genre = remote.genre.ifBlank { parsed.genre },
                content = parsed.content,
                favorite = remote.favorite,
                locked = remote.locked || parsed.locked,
                capo = if (remote.capo > 0) remote.capo else parsed.capo,
                sourceUrl = remote.sourceUrl.ifBlank { parsed.sourceUrl },
                remoteId = remote.id,
                remoteUpdatedAt = remote.updatedAt,
                remoteRev = remote.rev,
                visibility = remote.visibility,
                position = remote.position,
                deletedAt = 0,
                dirty = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Manda a la papelera local algo que el servidor dice que está borrado. */
    suspend fun applyRemoteDeletion(local: Song) {
        songDao.update(local.copy(deletedAt = System.currentTimeMillis(), dirty = false))
    }

    /** Aplica candado y visibilidad cuando el contenido no cambió. */
    suspend fun applyRemoteFlags(local: Song, remote: RemoteSong) {
        if (remote.locked != local.locked) songDao.setLocked(local.id, remote.locked)
        if (remote.visibility != local.visibility) songDao.setVisibility(local.id, remote.visibility)
    }

    /** Cambia quién puede ver la partitura (se subirá en el próximo push). */
    suspend fun setVisibility(id: Long, visibility: String) = db.withTransaction {
        songDao.setVisibility(id, visibility)
        songDao.touch(id, System.currentTimeMillis())
        notifyChange()
    }

    /** Id local de una lista a partir de su id remoto (null si no hay o no existe). */
    private suspend fun localPlaylistId(remoteId: String?): Long? =
        remoteId?.let { playlistDao.getByRemoteId(it)?.id }

    /** Encode a song as a plain-text file for upload to R2. */
    suspend fun encodeSong(song: Song): String = SongTextFormat.encode(song)
}
