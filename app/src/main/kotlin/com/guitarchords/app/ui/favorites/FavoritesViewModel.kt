package com.guitarchords.app.ui.favorites

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

data class FavoriteItem(val song: Song, val playlistName: String)

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    private val sortPrefs = SongSortPrefs(app)
    private val sortFlow = MutableStateFlow(sortPrefs.sort)
    val sort = sortFlow.asStateFlow()

    val favorites = combine(
        repo.favorites(), repo.playlists(), sortFlow
    ) { songs, playlists, sortMode ->
        val byId: Map<Long, Playlist> = playlists.associateBy { it.id }
        // Sin orden manual (aquí no hay arrastre) la lista llega ya por título.
        songs.applySort(sortMode)
            .map { FavoriteItem(it, it.playlistId?.let { pid -> byId[pid]?.name } ?: "") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSort(sortMode: SongSort) {
        sortFlow.value = sortMode
        sortPrefs.sort = sortMode
    }

    val playlists = repo.playlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }

    fun deleteSelected(ids: Set<Long>) = viewModelScope.launch { repo.deleteSongs(ids) }

    /** Deshacer el envío a la papelera (Snackbar "Deshacer"). */
    fun undoTrash(ids: Collection<Long>) = viewModelScope.launch { repo.restoreFromTrash(ids) }
    fun moveSelected(ids: Set<Long>, target: Long?) = viewModelScope.launch { repo.moveSongs(ids, target) }
    fun favoriteSelected(ids: Set<Long>, fav: Boolean) = viewModelScope.launch { repo.setFavoriteFor(ids, fav) }
}
