package com.guitarchords.app.ui.trash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.data.Song
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.trash.TrashViewModel.Companion.TRASH_MAX_AGE_MILLIS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    vm: TrashViewModel = viewModel()
) {
    val items by vm.items.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf<Song?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { confirmEmpty = true }) {
                            Icon(Icons.Default.DeleteForever, stringResource(R.string.trash_empty))
                        }
                    }
                }
            )
        }
    ) { pv ->
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.trash_empty_title),
                subtitle = stringResource(R.string.trash_empty_subtitle),
                modifier = Modifier.padding(pv)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(pv)) {
                Text(
                    stringResource(R.string.trash_retention_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { s ->
                        TrashRow(
                            song = s,
                            onRestore = { vm.restore(s.id) },
                            onDelete = { confirmDelete = s }
                        )
                    }
                }
            }
        }
    }

    confirmDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.trash_delete_title)) },
            text = { Text(stringResource(R.string.trash_delete_msg, s.title)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteForever(s.id)
                    confirmDelete = null
                }) { Text(stringResource(R.string.delete_forever)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text(stringResource(R.string.trash_empty_title_confirm)) },
            text = { Text(stringResource(R.string.trash_empty_msg, items.size)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.emptyTrash()
                    confirmEmpty = false
                }) { Text(stringResource(R.string.delete_forever)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun TrashRow(
    song: Song,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium)
                if (song.artist.isNotBlank()) {
                    Text(song.artist, style = MaterialTheme.typography.bodySmall)
                }
                val dayMs = 24L * 60 * 60 * 1000
                val daysLeft = ((song.deletedAt + TRASH_MAX_AGE_MILLIS - System.currentTimeMillis()) /
                    dayMs).coerceAtLeast(0).toInt()
                Text(
                    stringResource(R.string.trash_days_left, daysLeft),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.RestoreFromTrash, stringResource(R.string.trash_restore))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteForever,
                    stringResource(R.string.delete_forever),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
