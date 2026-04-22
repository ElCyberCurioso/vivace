package com.guitarchords.app.ui.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.AppDatabase
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SongEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo
    private val songDao = AppDatabase.get(app).songDao()

    private val _song = MutableStateFlow(Song(playlistId = 0L, title = ""))
    val song = _song.asStateFlow()

    private var loaded = false

    fun load(songId: Long, playlistId: Long) {
        if (loaded) return
        loaded = true
        if (songId == 0L) {
            _song.value = Song(playlistId = playlistId, title = "")
            return
        }
        viewModelScope.launch {
            songDao.getById(songId)?.let { _song.value = it }
        }
    }

    fun updateTitle(v: String) { _song.value = _song.value.copy(title = v) }
    fun updateArtist(v: String) { _song.value = _song.value.copy(artist = v) }
    fun updateContent(v: String) { _song.value = _song.value.copy(content = v) }

    fun save(onDone: (Long) -> Unit) = viewModelScope.launch {
        val current = _song.value.copy(
            updatedAt = System.currentTimeMillis(),
            title = _song.value.title.ifBlank { "Sin título" }
        )
        val id = repo.upsertSong(current)
        onDone(id)
    }
}
