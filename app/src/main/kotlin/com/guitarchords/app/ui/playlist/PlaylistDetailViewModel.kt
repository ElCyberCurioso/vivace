package com.guitarchords.app.ui.playlist

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import com.guitarchords.app.data.ZipManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: Repository = (app as GuitarChordsApp).repo

    private val playlistIdFlow = MutableStateFlow(0L)
    val playlistId: Long get() = playlistIdFlow.value

    private val playlistFlow = MutableStateFlow<Playlist?>(null)
    val playlist = playlistFlow

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val songs = playlistIdFlow
        .flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.songs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: Long) {
        if (playlistIdFlow.value == id) return
        playlistIdFlow.value = id
        viewModelScope.launch { playlistFlow.value = repo.playlist(id) }
    }

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }

    fun deleteSong(id: Long) = viewModelScope.launch { repo.deleteSong(id) }

    fun exportZipShare(onReady: (Intent) -> Unit) = viewModelScope.launch {
        val id = playlistIdFlow.value
        val exp = repo.exportPlaylist(id) ?: return@launch
        val ctx: Context = getApplication()
        val uri: Uri = ZipManager.exportToZip(ctx, exp)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_SUBJECT, exp.name)
        }
        onReady(Intent.createChooser(send, "Compartir lista"))
    }
}
