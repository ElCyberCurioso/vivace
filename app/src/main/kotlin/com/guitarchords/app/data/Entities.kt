package com.guitarchords.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "songs",
    foreignKeys = [ForeignKey(
        entity = Playlist::class,
        parentColumns = ["id"],
        childColumns = ["playlist_id"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("playlist_id")]
)
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "playlist_id") val playlistId: Long? = null,
    val title: String,
    val artist: String = "",
    val genre: String = "",
    val content: String = "",
    val favorite: Boolean = false,
    val capo: Int = 0,
    /** URL de la web donde está la canción original (botón "Link" en el visor). */
    @ColumnInfo(name = "source_url") val sourceUrl: String = "",
    @ColumnInfo(name = "remote_key") val remoteKey: String? = null,
    @ColumnInfo(name = "remote_etag") val remoteEtag: String? = null,
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: Long = 0,
    val dirty: Boolean = false,
    @ColumnInfo(name = "position") val position: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * An alternate version of a [Song] (different arrangement, key, capo, tab…).
 * The song's own `content`/`capo` is the "Original"; extra versions live here.
 */
@Entity(
    tableName = "song_versions",
    foreignKeys = [ForeignKey(
        entity = Song::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("song_id")]
)
data class SongVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "song_id") val songId: Long,
    val name: String,
    val content: String = "",
    val capo: Int = 0,
    /** URL de la web de origen de esta versión concreta (botón "Link"). */
    @ColumnInfo(name = "source_url") val sourceUrl: String = "",
    @ColumnInfo(name = "position") val position: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Digitación de acorde definida por el usuario. Tiene prioridad sobre las
 * posiciones de chords-db/plantillas al mostrar diagramas.
 */
@Entity(
    tableName = "custom_chords",
    indices = [Index("chord_key"), Index(value = ["uuid"], unique = true)]
)
data class CustomChord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nombre normalizado raíz+calidad, p. ej. "Bm" o "F#maj7". */
    @ColumnInfo(name = "chord_key") val chordKey: String,
    /** 6 trastes absolutos separados por comas, Mi grave primero; -1 = muda. */
    val frets: String,
    @ColumnInfo(name = "position") val position: Int = 0,
    /** Id estable entre dispositivos para sincronizar (UUID). */
    @ColumnInfo(name = "uuid") val uuid: String = "",
    /** Borrado lógico: 0 = vivo, >0 = tombstone (se propaga al sincronizar). */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long = 0,
    /** Hay cambios locales sin subir al Worker. */
    val dirty: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class PlaylistExport(
    val name: String,
    val songs: List<SongExport>,
    val version: Int = 1
)

@Serializable
data class SongExport(
    val title: String,
    val artist: String = "",
    val genre: String = "",
    val content: String = "",
    val favorite: Boolean = false,
    val capo: Int = 0,
    val sourceUrl: String = "",
    val position: Int = 0
)
