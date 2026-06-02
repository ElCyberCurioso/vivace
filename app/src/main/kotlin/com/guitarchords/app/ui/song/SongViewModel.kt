package com.guitarchords.app.ui.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SongViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo
    private val songIdFlow = MutableStateFlow(0L)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val song = songIdFlow
        .flatMapLatest { if (it == 0L) flowOf<Song?>(null) else repo.song(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val versions = songIdFlow
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.versions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: Long) { songIdFlow.value = id }

    fun toggleFavorite() = viewModelScope.launch {
        song.value?.let { repo.toggleFavorite(it) }
    }

    /** Create a new version seeded with the current song's content/capo. */
    fun addVersion(name: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        val s = song.value ?: return@launch
        val id = repo.addVersion(s.id, name.trim().ifBlank { "Versión" }, s.content, s.capo)
        onCreated(id)
    }

    fun deleteVersion(id: Long) = viewModelScope.launch { repo.deleteVersion(id) }
}
