package com.guitarchords.app.ui.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.data.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onPlaylistClick: (Long) -> Unit,
    onOpenDictionary: () -> Unit = {},
    onOpenFinder: () -> Unit = {},
    onOpenTuner: () -> Unit = {},
    vm: PlaylistsViewModel = viewModel()
) {
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.import(uri) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listas") },
                actions = {
                    IconButton(onClick = onOpenTuner) {
                        Icon(Icons.Default.GraphicEq, "Afinador")
                    }
                    IconButton(onClick = onOpenFinder) {
                        Icon(Icons.Default.Search, "Buscador de acordes")
                    }
                    IconButton(onClick = onOpenDictionary) {
                        Icon(Icons.Default.MenuBook, "Diccionario")
                    }
                    IconButton(onClick = {
                        importLauncher.launch(arrayOf("application/zip", "*/*"))
                    }) {
                        Icon(Icons.Default.FileUpload, "Importar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, "Nueva lista")
            }
        }
    ) { pv ->
        if (playlists.isEmpty()) {
            EmptyState(pv)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    PlaylistRow(
                        playlist = pl,
                        onClick = { onPlaylistClick(pl.id) },
                        onRename = { renaming = pl },
                        onDelete = { deleting = pl }
                    )
                }
            }
        }
    }

    if (showCreate) {
        TextDialog(
            title = "Nueva lista",
            initial = "",
            onDismiss = { showCreate = false },
            onConfirm = {
                vm.create(it)
                showCreate = false
            }
        )
    }
    renaming?.let { p ->
        TextDialog(
            title = "Renombrar",
            initial = p.name,
            onDismiss = { renaming = null },
            onConfirm = {
                vm.rename(p, it)
                renaming = null
            }
        )
    }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Borrar lista") },
            text = { Text("¿Borrar \"${p.name}\" y todas sus canciones?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(p.id)
                    deleting = null
                }) { Text("Borrar") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun EmptyState(pv: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(pv),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text("Sin listas aún", style = MaterialTheme.typography.titleMedium)
            Text("Pulsa + para crear una", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onRename() }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Icon(Icons.Default.LibraryMusic, null)
            Spacer(Modifier.size(12.dp))
            Text(
                playlist.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, "Renombrar") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Borrar") }
        }
    }
}

@Composable
fun TextDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
