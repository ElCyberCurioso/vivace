package com.guitarchords.app.ui.song

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SongViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo
    private val songIdFlow = MutableStateFlow(0L)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val song = songIdFlow
        .flatMapLatest { if (it == 0L) flowOf<Song?>(null) else repo.song(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * true cuando la consulta ya respondió y la canción no existe (id inválido
     * o borrada definitivamente): así el visor distingue "cargando" de "no está".
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val notFound = songIdFlow
        .flatMapLatest { id ->
            if (id == 0L) flowOf(false) else repo.song(id).map { it == null }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val versions = songIdFlow
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.versions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Canciones de la carpeta desde la que se abrió el visor, en su orden, para
     * poder encadenarlas sin volver al listado (modo concierto). Vacío si se
     * abrió desde favoritas, búsqueda o un enlace suelto.
     */
    private val _siblings = MutableStateFlow<List<Long>>(emptyList())
    val siblings = _siblings.asStateFlow()

    fun load(id: Long) { songIdFlow.value = id }

    fun loadSiblings(playlistId: Long) {
        if (playlistId <= 0L || _siblings.value.isNotEmpty()) return
        viewModelScope.launch { _siblings.value = repo.songsOf(playlistId).map { it.id } }
    }

    /** Salta a la canción anterior/siguiente de la carpeta, si la hay. */
    fun step(delta: Int): Boolean {
        val ids = _siblings.value
        val i = ids.indexOf(songIdFlow.value)
        val next = i + delta
        if (i < 0 || next !in ids.indices) return false
        songIdFlow.value = ids[next]
        return true
    }

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
