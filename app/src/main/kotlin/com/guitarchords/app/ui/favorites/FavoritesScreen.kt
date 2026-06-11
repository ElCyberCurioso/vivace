package com.guitarchords.app.ui.favorites

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.rememberSongSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onSongClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: FavoritesViewModel = viewModel()
) {
    val items by vm.favorites.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
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
                    }
                )
            }
        }
    ) { pv ->
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.StarOutline,
                title = stringResource(R.string.empty_favorites_title),
                subtitle = stringResource(R.string.empty_favorites_subtitle),
                modifier = Modifier.padding(pv)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(pv)
            ) {
                items(items, key = { it.song.id }) { fav ->
                    val selected = selection.isSelected(fav.song.id)
                    Card(
                        colors = if (selected)
                            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        else CardDefaults.cardColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(selection.active) {
                                detectTapGestures(
                                    onTap = {
                                        if (selection.active) selection.toggle(fav.song.id)
                                        else onSongClick(fav.song.id)
                                    },
                                    onLongPress = { selection.start(fav.song.id) }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selection.active) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { selection.toggle(fav.song.id) }
                                )
                            } else {
                                IconButton(onClick = { vm.toggleFavorite(fav.song) }) {
                                    Icon(
                                        Icons.Default.Star,
                                        stringResource(R.string.remove_favorite),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    fav.song.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val sub = buildString {
                                    if (fav.song.artist.isNotBlank()) append(fav.song.artist)
                                    if (fav.song.genre.isNotBlank()) {
                                        if (isNotEmpty()) append(" · ")
                                        append(fav.song.genre)
                                    }
                                }
                                if (sub.isNotEmpty()) {
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (fav.playlistName.isNotBlank()) {
                                    Text(
                                        stringResource(R.string.in_playlist, fav.playlistName),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
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
                vm.deleteSelected(selection.selected)
                bulkDeleting = false
                selection.clear()
            },
            onDismiss = { bulkDeleting = false }
        )
    }
}
