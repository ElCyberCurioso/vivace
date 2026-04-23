package com.guitarchords.app.ui.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.data.Playlist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditorScreen(
    songId: Long,
    playlistId: Long,
    onDone: () -> Unit,
    vm: SongEditorViewModel = viewModel()
) {
    LaunchedEffect(songId, playlistId) { vm.load(songId, playlistId) }
    val song by vm.song.collectAsState()
    var contentField by remember { mutableStateOf(TextFieldValue("")) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(song.id, song.content) {
        if (!initialized && song.content.isNotEmpty()) {
            contentField = TextFieldValue(song.content, TextRange(song.content.length))
            initialized = true
        } else if (!initialized && songId == 0L) {
            initialized = true
        }
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var playlistPickerOpen by remember { mutableStateOf(false) }
    val playlists by vm.playlists.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (songId == 0L) "Nueva canción" else "Editar") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                actions = {
                    IconButton(onClick = {
                        vm.updateContent(contentField.text)
                        vm.save { onDone() }
                    }) { Icon(Icons.Default.Check, "Guardar") }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = song.title,
                onValueChange = vm::updateTitle,
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = song.artist,
                onValueChange = vm::updateArtist,
                label = { Text("Artista") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = song.genre,
                onValueChange = vm::updateGenre,
                label = { Text("Género") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            val plName = song.playlistId?.let { pid -> playlists.firstOrNull { it.id == pid }?.name } ?: "Sin lista"
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AssistChip(
                    onClick = { playlistPickerOpen = true },
                    label = { Text("Lista: $plName") },
                    leadingIcon = { Icon(Icons.Default.LibraryMusic, null) }
                )
                AssistChip(
                    onClick = { pickerOpen = true },
                    label = { Text("Insertar acorde") },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Letra y acordes", style = MaterialTheme.typography.labelMedium)
            Text(
                "Usa [Nombre] para acordes — ej: [Am] Casa",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = contentField,
                onValueChange = {
                    contentField = it
                    vm.updateContent(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                placeholder = { Text("Escribe aquí…") }
            )
        }
    }

    if (playlistPickerOpen) {
        PlaylistPickerDialog(
            playlists = playlists,
            currentId = song.playlistId,
            onDismiss = { playlistPickerOpen = false },
            onPick = {
                vm.updatePlaylist(it)
                playlistPickerOpen = false
            }
        )
    }

    if (pickerOpen) {
        ChordPickerDialog(
            onDismiss = { pickerOpen = false },
            onPick = { chordName ->
                val cursor = contentField.selection.start.coerceIn(0, contentField.text.length)
                val before = contentField.text.substring(0, cursor)
                val after = contentField.text.substring(cursor)
                val inserted = "[$chordName]"
                val newText = before + inserted + after
                val newCursor = cursor + inserted.length
                contentField = TextFieldValue(newText, TextRange(newCursor))
                vm.updateContent(newText)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<Playlist>,
    currentId: Long?,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige lista") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(null) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, null)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Sin lista" + if (currentId == null) " ✓" else "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                items(playlists) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(p.id) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.LibraryMusic, null)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            p.name + if (currentId == p.id) " ✓" else "",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun ChordPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val all = remember { ChordLibrary.all().map { it.name } }
    val filtered = remember(query) {
        if (query.isBlank()) all else all.filter { it.startsWith(query, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige un acorde") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Buscar (Am, F#m7…)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 72.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { name ->
                        AssistChip(onClick = { onPick(name) }, label = { Text(name) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
