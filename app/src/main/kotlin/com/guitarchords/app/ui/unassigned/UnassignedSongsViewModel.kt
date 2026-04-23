package com.guitarchords.app.ui.unassigned

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UnassignedSongsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    val songs = repo.unassigned().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Song>()
    )

    val playlists = repo.playlists().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Playlist>()
    )

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }
    fun deleteSong(id: Long) = viewModelScope.launch { repo.deleteSong(id) }
    fun moveSong(songId: Long, targetPlaylistId: Long) = viewModelScope.launch {
        repo.moveSong(songId, targetPlaylistId)
    }
}
