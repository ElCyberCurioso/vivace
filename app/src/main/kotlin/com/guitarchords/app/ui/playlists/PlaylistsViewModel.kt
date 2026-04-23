package com.guitarchords.app.ui.playlists

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.ZipManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    val playlists = repo.playlists().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val unassignedCount = repo.unassignedCount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0
    )

    fun create(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repo.createPlaylist(name.trim())
    }

    fun rename(p: Playlist, newName: String) = viewModelScope.launch {
        if (newName.isNotBlank()) repo.renamePlaylist(p, newName.trim())
    }

    fun delete(id: Long) = viewModelScope.launch { repo.deletePlaylist(id) }

    fun import(uri: Uri, onDone: (Long?) -> Unit) = viewModelScope.launch {
        val ctx: Context = getApplication()
        val exp = ZipManager.importFromUri(ctx, uri)
        val id = exp?.let { repo.importPlaylist(it) }
        onDone(id)
    }
}
