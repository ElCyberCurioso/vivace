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
    /** Id de la lista en la API de Vivace; null = aún no subida. */
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    @ColumnInfo(name = "position") val position: Int = 0,
    /** Hay cambios locales sin subir. */
    val dirty: Boolean = true,
    /** Borrado lógico: se propaga al servidor y luego se purga. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
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
    // remote_key único: una clave del bucket no puede apuntar a dos canciones
    // (en SQLite los NULL no colisionan, así que las locales no se ven afectadas).
    // `dirty` y `deleted_at` se filtran en cada pasada de sincronización y al
    // pintar la papelera: con la sincronización automática dejan de ser
    // consultas ocasionales.
    indices = [
        Index("playlist_id"), Index(value = ["remote_key"], unique = true),
        Index("dirty"), Index("deleted_at")
    ]
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
    /** Clave del objeto en R2 (flujo antiguo con token compartido). */
    @ColumnInfo(name = "remote_key") val remoteKey: String? = null,
    /** Id de la partitura en la API de Vivace (sincronización con cuenta). */
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    /** 'private' o 'public': quién puede verla en la web. */
    val visibility: String = "private",
    @ColumnInfo(name = "remote_etag") val remoteEtag: String? = null,
    @ColumnInfo(name = "remote_updated_at") val remoteUpdatedAt: Long = 0,
    /**
     * Revisión que el SERVIDOR tenía la última vez que se sincronizó. Viaja de
     * vuelta como `baseRev` al subir: si no coincide con la del servidor es que
     * alguien más la tocó y hay conflicto. `updated_at` no vale para esto,
     * porque dos relojes distintos no se pueden comparar.
     */
    @ColumnInfo(name = "remote_rev") val remoteRev: Int = 0,
    val dirty: Boolean = false,
    /** Bloqueo anti-edición accidental. Solo se fija/quita desde el Worker;
     * en el dispositivo viaja vía la cabecera #locked y se respeta (confirmar
     * antes de editar), nunca se cambia localmente. */
    val locked: Boolean = false,
    @ColumnInfo(name = "position") val position: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    /** Papelera: 0 = activa; >0 = momento del borrado (se purga a los 90 días). */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long = 0
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
    /** Id de la versión en la API; null = aún no subida. */
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    val dirty: Boolean = true,
    /** Borrado lógico: sin esto, borrar una versión aquí no llegaba al servidor. */
    @ColumnInfo(name = "deleted_at") val deletedAt: Long = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Cola de borrados pendientes de comunicar al servidor.
 *
 * Hace falta porque un borrado definitivo se lleva la fila: sin dejar rastro,
 * la partitura seguía viva en el servidor y **volvía a bajarse como nueva** en
 * la siguiente sincronización. Aquí queda la lápida hasta que el servidor la
 * confirma.
 */
@Entity(tableName = "pending_deletes", indices = [Index(value = ["remote_id"], unique = true)])
data class PendingDelete(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Id remoto de lo que hay que borrar. */
    @ColumnInfo(name = "remote_id") val remoteId: String,
    /** "song", "version" o "playlist". */
    val kind: String,
    /** true = borrado definitivo (se va la fila y el objeto); false = papelera. */
    val purge: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
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
