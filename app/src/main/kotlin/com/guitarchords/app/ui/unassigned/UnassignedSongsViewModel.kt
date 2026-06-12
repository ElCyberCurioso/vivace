package com.guitarchords.app.ui.unassigned

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import com.guitarchords.app.data.SongSort
import com.guitarchords.app.data.SongSortPrefs
import com.guitarchords.app.data.applySort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UnassignedSongsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    private val sortPrefs = SongSortPrefs(app)
    private val sortFlow = MutableStateFlow(sortPrefs.sort)
    val sort = sortFlow.asStateFlow()

    val songs = combine(repo.unassigned(), sortFlow) { list, sortMode ->
        list.applySort(sortMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Song>())

    fun setSort(sortMode: SongSort) {
        sortFlow.value = sortMode
        sortPrefs.sort = sortMode
    }

    val playlists = repo.playlists().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Playlist>()
    )

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }
    fun deleteSong(id: Long) = viewModelScope.launch { repo.deleteSong(id) }
    fun moveSong(songId: Long, targetPlaylistId: Long) = viewModelScope.launch {
        repo.moveSong(songId, targetPlaylistId)
    }

    fun deleteSelected(ids: Set<Long>) = viewModelScope.launch { repo.deleteSongs(ids) }
    fun moveSelected(ids: Set<Long>, target: Long?) = viewModelScope.launch { repo.moveSongs(ids, target) }
    fun favoriteSelected(ids: Set<Long>, fav: Boolean) = viewModelScope.launch { repo.setFavoriteFor(ids, fav) }
}
