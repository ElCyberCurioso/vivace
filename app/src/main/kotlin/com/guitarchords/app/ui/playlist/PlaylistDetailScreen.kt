package com.guitarchords.app.ui.playlist

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.data.Song

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
    val ctx = LocalContext.current
    var deleting by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton(onClick = {
                        vm.exportZipShare { intent -> ctx.startActivity(intent) }
                    }) { Icon(Icons.Default.Share, "Compartir") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSong) {
                Icon(Icons.Default.Add, "Nueva canción")
            }
        }
    ) { pv ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(72.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Sin canciones")
                }
            }
        } else {
            val sorted = remember(songs) {
                songs.sortedWith(compareByDescending<Song> { it.favorite }.thenBy { it.position }.thenBy { it.title })
            }
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(pv)
            ) {
                items(sorted, key = { it.id }) { s ->
                    SongRow(
                        song = s,
                        onClick = { onSongClick(s.id) },
                        onFav = { vm.toggleFavorite(s) },
                        onEdit = { onEditSong(s.id) },
                        onDelete = { deleting = s }
                    )
                }
            }
        }
    }

    deleting?.let { s ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Borrar canción") },
            text = { Text("¿Borrar \"${s.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteSong(s.id)
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
private fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onFav: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onEdit() })
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            IconButton(onClick = onFav) {
                if (song.favorite)
                    Icon(Icons.Default.Star, "Quitar favorita", tint = MaterialTheme.colorScheme.primary)
                else
                    Icon(Icons.Outlined.StarOutline, "Favorita")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                if (song.artist.isNotBlank())
                    Text(song.artist, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Borrar") }
        }
    }
}
