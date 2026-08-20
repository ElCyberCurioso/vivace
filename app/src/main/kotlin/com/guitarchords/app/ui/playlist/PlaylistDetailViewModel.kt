package com.guitarchords.app.ui.playlist

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.R
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Repository
import com.guitarchords.app.data.Song
import com.guitarchords.app.data.SongSort
import com.guitarchords.app.data.SongSortPrefs
import com.guitarchords.app.data.ZipManager
import com.guitarchords.app.data.applySort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val sortPrefs = SongSortPrefs(app)
    private val sortFlow = MutableStateFlow(sortPrefs.sort)
    val sort = sortFlow.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val songs = combine(
        playlistIdFlow.flatMapLatest { if (it == 0L) flowOf(emptyList()) else repo.songs(it) },
        sortFlow
    ) { list, sortMode -> list.applySort(sortMode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSort(sortMode: SongSort) {
        sortFlow.value = sortMode
        sortPrefs.sort = sortMode
    }

    fun load(id: Long) {
        if (playlistIdFlow.value == id) return
        playlistIdFlow.value = id
        viewModelScope.launch { playlistFlow.value = repo.playlist(id) }
    }

    val otherPlaylists = combine(repo.playlists(), playlistIdFlow) { all, currentId ->
        all.filter { it.id != currentId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(song: Song) = viewModelScope.launch { repo.toggleFavorite(song) }

    fun deleteSong(id: Long) = viewModelScope.launch { repo.deleteSong(id) }

    fun moveSong(songId: Long, targetPlaylistId: Long?) = viewModelScope.launch {
        repo.moveSong(songId, targetPlaylistId)
    }

    fun persistOrder(orderedIds: List<Long>) = viewModelScope.launch { repo.reorderSongs(orderedIds) }

    fun deleteSelected(ids: Set<Long>) = viewModelScope.launch { repo.deleteSongs(ids) }

    /** Deshacer el envío a la papelera (Snackbar "Deshacer"). */
    fun undoTrash(ids: Collection<Long>) = viewModelScope.launch { repo.restoreFromTrash(ids) }
    fun moveSelected(ids: Set<Long>, target: Long?) = viewModelScope.launch { repo.moveSongs(ids, target) }
    fun favoriteSelected(ids: Set<Long>, fav: Boolean) = viewModelScope.launch { repo.setFavoriteFor(ids, fav) }

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
        onReady(Intent.createChooser(send, ctx.getString(R.string.share_playlist)))
    }
}
