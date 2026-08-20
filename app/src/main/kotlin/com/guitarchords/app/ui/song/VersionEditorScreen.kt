package com.guitarchords.app.ui.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordParser
import com.guitarchords.app.ui.components.ChordPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionEditorScreen(
    versionId: Long,
    onDone: () -> Unit,
    vm: VersionEditorViewModel = viewModel()
) {
    LaunchedEffect(versionId) { vm.load(versionId) }
    val version by vm.version.collectAsStateWithLifecycle()
    // El candado de la canción padre también protege sus versiones.
    val parentLocked by vm.parentLocked.collectAsStateWithLifecycle()

    var contentField by remember { mutableStateOf(TextFieldValue("")) }
    var initialized by remember { mutableStateOf(false) }
    var chordPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var lockAck by remember { mutableStateOf(false) }

    LaunchedEffect(version?.id) {
        val v = version
        if (!initialized && v != null) {
            contentField = TextFieldValue(v.content, TextRange(v.content.length))
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_version)) },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete_version))
                    }
                    IconButton(onClick = {
                        vm.updateContent(contentField.text)
                        vm.save { onDone() }
                    }) { Icon(Icons.Default.Check, stringResource(R.string.save)) }
                }
            )
        }
    ) { pv ->
        val v = version
        if (v == null) {
            Box(Modifier.fillMaxSize().padding(pv), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .imePadding()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (parentLocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(R.string.locked_song_banner),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = v.name,
                onValueChange = vm::updateName,
                label = { Text(stringResource(R.string.version_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = v.sourceUrl,
                onValueChange = vm::updateSourceUrl,
                label = { Text(stringResource(R.string.source_url_label)) },
                placeholder = { Text("https://…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                leadingIcon = { Icon(Icons.Default.Link, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.capo_label), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = { vm.updateCapo(v.capo - 1) }, enabled = v.capo > 0) {
                    Icon(Icons.Default.Remove, stringResource(R.string.capo_down))
                }
                Text(
                    if (v.capo == 0) stringResource(R.string.no_capo)
                    else stringResource(R.string.fret_n, v.capo),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = { vm.updateCapo(v.capo + 1) }, enabled = v.capo < 12) {
                    Icon(Icons.Default.Add, stringResource(R.string.capo_up))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                AssistChip(
                    onClick = { chordPicker = true },
                    label = { Text(stringResource(R.string.insert_chord)) },
                    leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                )
                AssistChip(
                    onClick = {
                        val cursor = contentField.selection.start
                            .coerceIn(0, contentField.text.length)
                        val before = contentField.text.substring(0, cursor)
                        val after = contentField.text.substring(cursor)
                        val prefix = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
                        val suffix = if (after.startsWith("\n") || after.isEmpty()) "" else "\n"
                        val inserted = prefix + ChordParser.tabTemplate() + suffix
                        val newText = before + inserted + after
                        contentField = TextFieldValue(newText, TextRange(cursor + inserted.length))
                        vm.updateContent(newText)
                    },
                    label = { Text(stringResource(R.string.insert_tab)) },
                    leadingIcon = { Icon(Icons.Default.GraphicEq, null) }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lyrics_and_chords), style = MaterialTheme.typography.labelMedium)
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
                placeholder = { Text(stringResource(R.string.write_here)) }
            )
        }
    }

    if (chordPicker) {
        ChordPickerDialog(
            onDismiss = { chordPicker = false },
            onPick = { chordName ->
                val cursor = contentField.selection.start.coerceIn(0, contentField.text.length)
                val before = contentField.text.substring(0, cursor)
                val after = contentField.text.substring(cursor)
                val inserted = "{$chordName}"
                val newText = before + inserted + after
                contentField = TextFieldValue(newText, TextRange(cursor + inserted.length))
                vm.updateContent(newText)
                chordPicker = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_version)) },
            text = { Text(stringResource(R.string.delete_version_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete { onDone() }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Mismo aviso que en el editor de la partitura: si la canción está
    // bloqueada hay que confirmar antes de tocar cualquiera de sus versiones.
    if (parentLocked && !lockAck) {
        AlertDialog(
            onDismissRequest = { onDone() },
            icon = { Icon(Icons.Default.Lock, null) },
            title = { Text(stringResource(R.string.locked_dialog_title)) },
            text = { Text(stringResource(R.string.locked_dialog_msg)) },
            confirmButton = {
                TextButton(onClick = { lockAck = true }) {
                    Text(stringResource(R.string.locked_dialog_edit))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDone() }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
