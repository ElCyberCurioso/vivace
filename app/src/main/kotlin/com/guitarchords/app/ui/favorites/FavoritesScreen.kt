package com.guitarchords.app.ui.favorites

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.data.filterByQuery
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.SongListItem
import com.guitarchords.app.ui.components.SongSearchField
import com.guitarchords.app.ui.components.SongSortMenu
import com.guitarchords.app.ui.components.rememberSongSelection
import com.guitarchords.app.ui.components.rememberTrashedMessage
import com.guitarchords.app.ui.components.rememberUndoSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onSongClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: FavoritesViewModel = viewModel()
) {
    val allItems by vm.favorites.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val selection = rememberSongSelection()
    var bulkMoving by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    // Mismo filtro que en las carpetas, aplicado sobre la canción de cada favorito.
    val items = remember(allItems, query) {
        if (query.isBlank()) allItems
        else allItems.filter { fav -> listOf(fav.song).filterByQuery(query).isNotEmpty() }
    }

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
                    onSelectAll = { selection.setAll(items.map { it.song.id }) },
                    onFavorite = { vm.favoriteSelected(selection.selected, false); selection.clear() },
                    onMove = { bulkMoving = true },
                    onDelete = { bulkDeleting = true },
                    favoriteIsRemove = true
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.favorites_title)) },
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
            if (allItems.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.StarOutline,
                    title = stringResource(R.string.empty_favorites_title),
                    subtitle = stringResource(R.string.empty_favorites_subtitle),
                    modifier = Modifier.fillMaxSize()
                )
            } else if (items.isEmpty()) {
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
                    items(items, key = { it.song.id }) { fav ->
                        SongListItem(
                            song = fav.song,
                            selectionActive = selection.active,
                            selected = selection.isSelected(fav.song.id),
                            onClick = { onSongClick(fav.song.id) },
                            onToggle = { selection.toggle(fav.song.id) },
                            onLongPress = { selection.start(fav.song.id) },
                            playlistName = fav.playlistName,
                            leading = {
                                IconButton(onClick = { vm.toggleFavorite(fav.song) }) {
                                    Icon(
                                        Icons.Default.Star,
                                        stringResource(R.string.remove_favorite),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
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
