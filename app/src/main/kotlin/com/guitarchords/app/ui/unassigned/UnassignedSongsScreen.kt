package com.guitarchords.app.ui.unassigned

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.data.Song
import com.guitarchords.app.data.filterByQuery
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.MoveSongDialog
import com.guitarchords.app.ui.components.SongListItem
import com.guitarchords.app.ui.components.SongSearchField
import com.guitarchords.app.ui.components.SongSortMenu
import com.guitarchords.app.ui.components.rememberSongSelection
import com.guitarchords.app.ui.components.rememberTrashedMessage
import com.guitarchords.app.ui.components.rememberUndoSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnassignedSongsScreen(
    onSongClick: (Long) -> Unit,
    onEditSong: (Long) -> Unit,
    onAddSong: () -> Unit,
    onBack: () -> Unit,
    vm: UnassignedSongsViewModel = viewModel()
) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Song?>(null) }
    var moving by remember { mutableStateOf<Song?>(null) }
    val selection = rememberSongSelection()
    var bulkMoving by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val visibleSongs = remember(songs, query) { songs.filterByQuery(query) }

    val snackbarHost = remember { SnackbarHostState() }
    val showUndo = rememberUndoSnackbar(snackbarHost)
    val trashedMessage = rememberTrashedMessage()
    val undoLabel = stringResource(R.string.undo)

    BackHandler(enabled = selection.active) { selection.clear() }
    BackHandler(enabled = !selection.active && searchOpen) { searchOpen = false; query = "" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            if (selection.active) {
                SelectionTopBar(
                    count = selection.count,
                    onClose = { selection.clear() },
                    onSelectAll = { selection.setAll(visibleSongs.map { it.id }) },
                    onFavorite = { vm.favoriteSelected(selection.selected, true); selection.clear() },
                    onMove = { bulkMoving = true },
                    onDelete = { bulkDeleting = true }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.unassigned_songs)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) query = ""
                        }) { Icon(Icons.Default.Search, stringResource(R.string.search_songs)) }
                        SongSortMenu(current = sort, onPick = { vm.setSort(it) })
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSong) {
                Icon(Icons.Default.Add, stringResource(R.string.new_song))
            }
        }
    ) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv)) {
            if (searchOpen) {
                SongSearchField(
                    query = query,
                    onQuery = { query = it },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (songs.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.MusicNote,
                    title = stringResource(R.string.empty_unassigned_title),
                    subtitle = stringResource(R.string.empty_unassigned_subtitle),
                    modifier = Modifier.fillMaxSize()
                )
            } else if (visibleSongs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_results),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visibleSongs, key = { it.id }) { s ->
                        SongRow(
                            song = s,
                            selectionActive = selection.active,
                            selected = selection.isSelected(s.id),
                            onClick = { onSongClick(s.id) },
                            onToggle = { selection.toggle(s.id) },
                            onLongPress = { selection.start(s.id) },
                            onFav = { vm.toggleFavorite(s) },
                            onEdit = { onEditSong(s.id) },
                            onDelete = { deleting = s },
                            onMove = { moving = s }
                        )
                    }
                }
            }
        }
    }

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_song_title)) },
            text = { Text(stringResource(R.string.delete_song_msg, s.title)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSong(s.id)
                    deleting = null
                    showUndo(trashedMessage(1), undoLabel) { vm.undoTrash(listOf(s.id)) }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    moving?.let { s ->
        MoveSongDialog(
            songTitle = s.title,
            targets = playlists,
            includeUnassigned = false,   // ya está en "Sin lista"
            onDismiss = { moving = null },
            onPick = { target ->
                if (target != null) vm.moveSong(s.id, target)
                moving = null
            }
        )
    }

    if (bulkMoving) {
        BulkMoveDialog(
            count = selection.count,
            targets = playlists,
            includeUnassigned = false,
            onDismiss = { bulkMoving = false },
            onPick = { target ->
                vm.moveSelected(selection.selected, target)
                bulkMoving = false
                selection.clear()
            }
        )
    }
    if (bulkDeleting) {
        BulkDeleteDialog(
            count = selection.count,
            onConfirm = {
                val ids = selection.selected.toList()
                vm.deleteSelected(selection.selected)
                bulkDeleting = false
                selection.clear()
                showUndo(trashedMessage(ids.size), undoLabel) { vm.undoTrash(ids) }
            },
            onDismiss = { bulkDeleting = false }
        )
    }
}

@Composable
private fun SongRow(
    song: Song,
    selectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    onFav: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    SongListItem(
        song = song,
        selectionActive = selectionActive,
        selected = selected,
        onClick = onClick,
        onToggle = onToggle,
        onLongPress = onLongPress,
        leading = {
            IconButton(onClick = onFav) {
                if (song.favorite)
                    Icon(Icons.Default.Star, stringResource(R.string.remove_favorite), tint = MaterialTheme.colorScheme.primary)
                else
                    Icon(Icons.Outlined.StarOutline, stringResource(R.string.favorite))
            }
        },
        trailing = {
            IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.assign_to_playlist)) }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
        }
    )
}
