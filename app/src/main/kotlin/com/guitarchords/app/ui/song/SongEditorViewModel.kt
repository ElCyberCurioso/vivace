package com.guitarchords.app.ui.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SongEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo
    private val songDao = AppDatabase.get(app).songDao()

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
            songDao.getById(songId)?.let { _song.value = it }
        }
    }

    fun updateTitle(v: String) { _song.value = _song.value.copy(title = v) }
    fun updateArtist(v: String) { _song.value = _song.value.copy(artist = v) }
    fun updateGenre(v: String) { _song.value = _song.value.copy(genre = v) }
    fun updateContent(v: String) { _song.value = _song.value.copy(content = v) }
    fun updatePlaylist(id: Long?) { _song.value = _song.value.copy(playlistId = id) }

    fun save(onDone: (Long) -> Unit) = viewModelScope.launch {
        val current = _song.value.copy(
            updatedAt = System.currentTimeMillis(),
            title = _song.value.title.ifBlank { "Sin título" }
        )
        val id = repo.upsertSong(current)
        onDone(id)
    }
}
