package com.guitarchords.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.SongListItem
import com.guitarchords.app.ui.components.SongSortMenu
import com.guitarchords.app.ui.components.rememberSongSelection
import com.guitarchords.app.ui.components.rememberTrashedMessage
import com.guitarchords.app.ui.components.rememberUndoSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSearchScreen(
    onSongClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: SongSearchViewModel = viewModel()
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val hits by vm.results.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val selection = rememberSongSelection()
    var bulkMoving by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val showUndo = rememberUndoSnackbar(snackbarHost)
    val trashedMessage = rememberTrashedMessage()
    val undoLabel = stringResource(R.string.undo)

    BackHandler(enabled = selection.active) { selection.clear() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            if (selection.active) {
                SelectionTopBar(
                    count = selection.count,
                    onClose = { selection.clear() },
                    onSelectAll = { selection.setAll(hits.map { it.song.id }) },
                    onFavorite = { vm.favoriteSelected(selection.selected, true); selection.clear() },
                    onMove = { bulkMoving = true },
                    onDelete = { bulkDeleting = true }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.search_songs)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    },
                    actions = { SongSortMenu(current = sort, onPick = { vm.setSort(it) }) }
                )
            }
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, stringResource(R.string.clear))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            when {
                query.isBlank() -> EmptyState(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.search_hint_title),
                    subtitle = stringResource(R.string.search_hint_subtitle)
                )
                hits.isEmpty() -> EmptyState(
                    icon = Icons.Default.Search,
                    title = stringResource(R.string.no_results),
                    subtitle = stringResource(R.string.try_another_search)
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(hits, key = { it.song.id }) { hit ->
                        HitRow(
                            hit = hit,
                            selectionActive = selection.active,
                            selected = selection.isSelected(hit.song.id),
                            onClick = { onSongClick(hit.song.id) },
                            onToggle = { selection.toggle(hit.song.id) },
                            onLongPress = { selection.start(hit.song.id) }
                        )
                    }
                }
            }
        }
    }

    if (bulkMoving) {
        BulkMoveDialog(
            count = selection.count,
            targets = playlists,
            includeUnassigned = true,
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
private fun HitRow(
    hit: SongHit,
    selectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    SongListItem(
        song = hit.song,
        selectionActive = selectionActive,
        selected = selected,
        onClick = onClick,
        onToggle = onToggle,
        onLongPress = onLongPress,
        playlistName = hit.playlistName,
        leading = {
            // Aquí la estrella es solo un indicador (no se puede alternar).
            if (hit.song.favorite) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.padding(horizontal = 4.dp))
            }
        }
    )
}
