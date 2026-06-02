package com.guitarchords.app.ui.search

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.ui.components.BulkDeleteDialog
import com.guitarchords.app.ui.components.BulkMoveDialog
import com.guitarchords.app.ui.components.SelectionTopBar
import com.guitarchords.app.ui.components.rememberSongSelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSearchScreen(
    onSongClick: (Long) -> Unit,
    onBack: () -> Unit,
    vm: SongSearchViewModel = viewModel()
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val hits by vm.results.collectAsStateWithLifecycle()
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
                    onSelectAll = { selection.setAll(hits.map { it.song.id }) },
                    onFavorite = { vm.favoriteSelected(selection.selected, true); selection.clear() },
                    onMove = { bulkMoving = true },
                    onDelete = { bulkDeleting = true }
                )
            } else {
                TopAppBar(
                    title = { Text("Buscar canciones") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                    }
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
                placeholder = { Text("Título, artista o género…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            when {
                query.isBlank() -> HintMessage("Escribe para buscar")
                hits.isEmpty() -> HintMessage("Sin resultados")
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
                vm.deleteSelected(selection.selected)
                bulkDeleting = false
                selection.clear()
            },
            onDismiss = { bulkDeleting = false }
        )
    }
}

@Composable
private fun HintMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionActive) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
                Spacer(Modifier.padding(horizontal = 4.dp))
            } else if (hit.song.favorite) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(hit.song.title, style = MaterialTheme.typography.titleMedium)
                val sub = buildString {
                    if (hit.song.artist.isNotBlank()) append(hit.song.artist)
                    if (hit.song.genre.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(hit.song.genre)
                    }
                }
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall)
                }
                if (hit.playlistName.isNotBlank()) {
                    Text(
                        "en ${hit.playlistName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
