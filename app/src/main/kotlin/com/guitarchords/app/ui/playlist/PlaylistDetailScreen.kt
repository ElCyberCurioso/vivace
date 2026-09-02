package com.guitarchords.app.ui.playlist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.data.Song
import com.guitarchords.app.data.SongSort
import com.guitarchords.app.data.filterByQuery
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.components.MoveSongDialog
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.SongListItem
import com.guitarchords.app.ui.components.SongSearchField
import com.guitarchords.app.ui.components.SongSortMenu
import com.guitarchords.app.ui.components.rememberSongSelection
import com.guitarchords.app.ui.components.rememberTrashedMessage
import com.guitarchords.app.ui.components.rememberUndoSnackbar
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.guitarchords.app.ui.theme.accordioTopBarColors
import com.guitarchords.app.ui.icons.AccordioIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onSongClick: (Long) -> Unit,
    onAddSong: () -> Unit,
    onEditSong: (Long) -> Unit,
    onBack: () -> Unit,
    vm: PlaylistDetailViewModel = viewModel()
) {
    LaunchedEffect(playlistId) { vm.load(playlistId) }
    val playlist by vm.playlist.collectAsState()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val otherPlaylists by vm.otherPlaylists.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var deleting by remember { mutableStateOf<Song?>(null) }
    var moving by remember { mutableStateOf<Song?>(null) }
    val selection = rememberSongSelection()
    var bulkMoving by remember { mutableStateOf(false) }
    var bulkDeleting by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val manualSort = sort == SongSort.MANUAL
    val visibleSongs = remember(songs, query) { songs.filterByQuery(query) }

    // Avisos reversibles: mandar a la papelera y sacar de la carpeta.
    val snackbarHost = remember { SnackbarHostState() }
    val showUndo = rememberUndoSnackbar(snackbarHost)
    val trashedMessage = rememberTrashedMessage()
    val undoLabel = stringResource(R.string.undo)

    BackHandler(enabled = selection.active) { selection.clear() }
    // El back cierra primero la búsqueda activa antes de salir de la carpeta.
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
                    colors = accordioTopBarColors(),
                    title = { Text(playlist?.name ?: "") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) query = ""
                        }) { Icon(AccordioIcons.buscar(), stringResource(R.string.search_songs)) }
                        SongSortMenu(current = sort, onPick = { vm.setSort(it) })
                        IconButton(onClick = {
                            vm.exportZipShare { intent -> ctx.startActivity(intent) }
                        }) { Icon(AccordioIcons.compartir(), stringResource(R.string.share)) }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSong) {
                Icon(AccordioIcons.mas(), stringResource(R.string.new_song))
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
                    icon = AccordioIcons.notas(),
                    title = stringResource(R.string.empty_songs_title),
                    subtitle = stringResource(R.string.empty_songs_subtitle),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // El arrastre solo tiene sentido con el orden manual y sin filtro
                // de búsqueda; en cualquier otro caso la lista es derivada y se
                // oculta el asa.
                val canDrag = manualSort && query.isBlank()
                // Orden local editable por arrastre; se persiste al soltar.
                var localOrder by remember { mutableStateOf(songs) }
                LaunchedEffect(songs) { localOrder = songs }
                val listState = rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(listState) { from, to ->
                    localOrder = localOrder.toMutableList().apply {
                        add(to.index, removeAt(from.index))
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                val listData = if (canDrag) localOrder else visibleSongs
                if (listData.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.no_results),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(listData, key = { it.id }) { s ->
                            ReorderableItem(reorderState, key = s.id) {
                                SongRow(
                                    song = s,
                                    selectionActive = selection.active,
                                    selected = selection.isSelected(s.id),
                                    onClick = { onSongClick(s.id) },
                                    onToggle = { selection.toggle(s.id) },
                                    onLongPress = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selection.start(s.id)
                                    },
                                    onFav = { vm.toggleFavorite(s) },
                                    onEdit = { onEditSong(s.id) },
                                    onDelete = { deleting = s },
                                    onMove = { moving = s },
                                    onSwipeToDefault = {
                                        vm.moveSong(s.id, null)
                                        showUndo(
                                            ctx.getString(R.string.snackbar_removed_from_folder, s.title),
                                            undoLabel
                                        ) { vm.moveSong(s.id, playlistId) }
                                    },
                                    showDragHandle = canDrag,
                                    dragHandle = if (canDrag) Modifier.draggableHandle(
                                        onDragStarted = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            vm.persistOrder(localOrder.map { it.id })
                                        }
                                    ) else Modifier
                                )
                            }
                        }
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
            targets = otherPlaylists,
            includeUnassigned = true,
            onDismiss = { moving = null },
            onPick = { target ->
                vm.moveSong(s.id, target)
                moving = null
            }
        )
    }

    if (bulkMoving) {
        BulkMoveDialog(
            count = selection.count,
            targets = otherPlaylists,
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


@OptIn(ExperimentalMaterial3Api::class)
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
    onMove: () -> Unit,
    onSwipeToDefault: () -> Unit,
    showDragHandle: Boolean = true,
    dragHandle: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    // Deslizar NO borra: saca la partitura de la carpeta y la deja en "Sin lista".
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            if (v == SwipeToDismissBoxValue.EndToStart) onSwipeToDefault()
            false // nunca se asienta: la fila vuelve a su sitio (la lista ya no la incluye)
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = !selectionActive,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DriveFileMove,
                    contentDescription = stringResource(R.string.move_to_unassigned),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
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
                        Icon(AccordioIcons.estrella(), stringResource(R.string.remove_favorite), tint = MaterialTheme.colorScheme.primary)
                    else
                        Icon(AccordioIcons.estrellaHueca(), stringResource(R.string.favorite))
                }
            },
            trailing = {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(AccordioIcons.menu(), stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_to_playlist)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, null) },
                            onClick = { menuOpen = false; onMove() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
                if (showDragHandle) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = stringResource(R.string.drag_reorder),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = dragHandle.padding(start = 4.dp)
                    )
                }
            }
        )
    }
}
