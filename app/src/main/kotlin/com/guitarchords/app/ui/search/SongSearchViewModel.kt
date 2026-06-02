package com.guitarchords.app.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SongHit(val song: Song, val playlistName: String)

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class SongSearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    private val _query = MutableStateFlow("")
    val query = _query

    private val playlistsFlow = repo.playlists()

    val results = _query
        .debounce(150)
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(emptyList())
            else combine(repo.search(q.trim()), playlistsFlow) { songs, playlists ->
                val byId: Map<Long, Playlist> = playlists.associateBy { it.id }
                songs.map { SongHit(it, it.playlistId?.let { pid -> byId[pid]?.name } ?: "") }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists = playlistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(v: String) { _query.value = v }

    fun deleteSelected(ids: Set<Long>) = viewModelScope.launch { repo.deleteSongs(ids) }
    fun moveSelected(ids: Set<Long>, target: Long?) = viewModelScope.launch { repo.moveSongs(ids, target) }
    fun favoriteSelected(ids: Set<Long>, fav: Boolean) = viewModelScope.launch { repo.setFavoriteFor(ids, fav) }
}
