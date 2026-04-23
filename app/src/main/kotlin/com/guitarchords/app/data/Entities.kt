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
    @ColumnInfo(name = "position") val position: Int = 0,
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
    val position: Int = 0
)
