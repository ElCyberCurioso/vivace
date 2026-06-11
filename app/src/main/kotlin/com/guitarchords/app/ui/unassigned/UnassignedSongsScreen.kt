package com.guitarchords.app.ui.unassigned

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.data.Song
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.rememberSongSelection

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
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<Song?>(null) }
    var moving by remember { mutableStateOf<Song?>(null) }
    val selection = rememberSongSelection()
    var bulkMoving by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }

    BackHandler(enabled = selection.active) { selection.clear() }

    Scaffold(
        topBar = {
            if (selection.active) {
                SelectionTopBar(
                    count = selection.count,
                    onClose = { selection.clear() },
                    onSelectAll = { selection.setAll(songs.map { it.id }) },
                    onFavorite = { vm.favoriteSelected(selection.selected, true); selection.clear() },
                    onMove = { bulkMoving = true },
                    onDelete = { bulkDeleting = true }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.unassigned_songs)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
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
        if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.MusicNote,
                title = stringResource(R.string.empty_unassigned_title),
                subtitle = stringResource(R.string.empty_unassigned_subtitle),
                modifier = Modifier.padding(pv)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(pv)
            ) {
                items(songs, key = { it.id }) { s ->
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

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_song_title)) },
            text = { Text(stringResource(R.string.delete_song_msg, s.title)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSong(s.id)
                    deleting = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    moving?.let { s ->
        AssignPlaylistDialog(
            songTitle = s.title,
            targets = playlists,
            onDismiss = { moving = null },
            onPick = { target ->
                vm.moveSong(s.id, target.id)
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
                vm.deleteSelected(selection.selected)
                bulkDeleting = false
                selection.clear()
            },
            onDismiss = { bulkDeleting = false }
        )
    }
}

@Composable
private fun AssignPlaylistDialog(
    songTitle: String,
    targets: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_song_title, songTitle)) },
        text = {
            if (targets.isEmpty()) {
                Text(stringResource(R.string.no_lists_yet))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(targets, key = { it.id }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(p) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LibraryMusic, null)
                            Spacer(Modifier.size(12.dp))
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
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
    Card(
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else CardDefaults.cardColors(),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(selectionActive) {
                detectTapGestures(
                    onTap = { if (selectionActive) onToggle() else onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            if (selectionActive) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
            } else {
                IconButton(onClick = onFav) {
                    if (song.favorite)
                        Icon(Icons.Default.Star, stringResource(R.string.remove_favorite), tint = MaterialTheme.colorScheme.primary)
                    else
                        Icon(Icons.Outlined.StarOutline, stringResource(R.string.favorite))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                val sub = buildString {
                    if (song.artist.isNotBlank()) append(song.artist)
                    if (song.genre.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(song.genre)
                    }
                }
                if (sub.isNotEmpty())
                    Text(sub, style = MaterialTheme.typography.bodySmall)
            }
            if (!selectionActive) {
                IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.assign_to_playlist)) }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
            }
        }
    }
}
