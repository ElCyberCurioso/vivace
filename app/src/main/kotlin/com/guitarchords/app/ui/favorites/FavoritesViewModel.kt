package com.guitarchords.app.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoriteItem(val song: Song, val playlistName: String)

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    val favorites = combine(repo.favorites(), repo.playlists()) { songs, playlists ->
        val byId: Map<Long, Playlist> = playlists.associateBy { it.id }
        songs.map { FavoriteItem(it, it.playlistId?.let { pid -> byId[pid]?.name } ?: "") }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }
}
