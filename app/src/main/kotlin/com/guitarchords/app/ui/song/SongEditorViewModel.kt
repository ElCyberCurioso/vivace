package com.guitarchords.app.ui.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import com.guitarchords.app.sync.SongTextFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SongEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    private val _song = MutableStateFlow(Song(playlistId = null, title = ""))
    val song = _song.asStateFlow()

    val playlists = repo.playlists().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Playlist>()
    )

    private var loaded = false

    fun load(songId: Long, playlistId: Long) {
        if (loaded) return
        loaded = true
        if (songId == 0L) {
            val pid = if (playlistId <= 0L) null else playlistId
            _song.value = Song(playlistId = pid, title = "")
            return
        }
        viewModelScope.launch {
            repo.songOnce(songId)?.let { _song.value = it }
        }
    }

    fun updateTitle(v: String) { _song.value = _song.value.copy(title = v) }
    fun updateArtist(v: String) { _song.value = _song.value.copy(artist = v) }
    fun updateGenre(v: String) { _song.value = _song.value.copy(genre = v) }
    fun updateSourceUrl(v: String) { _song.value = _song.value.copy(sourceUrl = v.trim()) }
    fun updateContent(v: String) { _song.value = _song.value.copy(content = v) }
    fun updatePlaylist(id: Long?) { _song.value = _song.value.copy(playlistId = id) }
    fun updateCapo(v: Int) { _song.value = _song.value.copy(capo = v.coerceIn(0, 12)) }

    fun save(onDone: (Long) -> Unit) = viewModelScope.launch {
        val current = _song.value.copy(
            updatedAt = System.currentTimeMillis(),
            title = _song.value.title.ifBlank { SongTextFormat.UNTITLED }
        )
        val id = repo.upsertSong(current)
        onDone(id)
    }
}
