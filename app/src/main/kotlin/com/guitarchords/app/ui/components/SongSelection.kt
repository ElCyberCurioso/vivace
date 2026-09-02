package com.guitarchords.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.data.Playlist
import com.guitarchords.app.ui.icons.AccordioIcons

/**
 * Holds the set of selected song ids for a list screen. Long-press starts a
 * selection; tapping while [active] toggles members; the contextual app bar
 * and bulk dialogs below drive the actions.
 */
@Stable
class SongSelectionState {
    var selected by mutableStateOf<Set<Long>>(emptySet())
        private set

    val active: Boolean get() = selected.isNotEmpty()
    val count: Int get() = selected.size

    fun isSelected(id: Long) = id in selected
    fun toggle(id: Long) { selected = if (id in selected) selected - id else selected + id }
    fun start(id: Long) { selected = selected + id }
    fun setAll(ids: List<Long>) { selected = ids.toSet() }
    fun clear() { selected = emptySet() }
}

@Composable
fun rememberSongSelection(): SongSelectionState = remember { SongSelectionState() }

/** Contextual top bar shown while a selection is active. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onFavorite: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    favoriteIsRemove: Boolean = false
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        title = { Text(pluralStringResource(R.plurals.selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.cancel_selection)) }
        },
        actions = {
            IconButton(onClick = onSelectAll) { Icon(Icons.Default.SelectAll, stringResource(R.string.select_all)) }
            IconButton(onClick = onFavorite) {
                if (favoriteIsRemove) Icon(AccordioIcons.estrellaHueca(), stringResource(R.string.remove_from_favorites))
                else Icon(AccordioIcons.estrella(), stringResource(R.string.mark_favorite))
            }
            IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.move_to_list)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
        }
    )
}

/** Bulk move/assign dialog. [onPick] receives the target playlist id, or null for "Sin lista". */
@Composable
fun BulkMoveDialog(
    count: Int,
    targets: List<Playlist>,
    includeUnassigned: Boolean,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pluralStringResource(R.plurals.bulk_move_title, count, count)) },
        text = {
            if (targets.isEmpty() && !includeUnassigned) {
                Text(stringResource(R.string.no_lists_yet))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (includeUnassigned) {
                        item {
                            PickRow(icon = AccordioIcons.notas(), label = stringResource(R.string.no_playlist)) { onPick(null) }
                        }
                    }
                    items(targets, key = { it.id }) { p ->
                        PickRow(icon = AccordioIcons.partitura(), label = p.name) { onPick(p.id) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

/**
 * Mover UNA canción a otra lista. Mismo selector que [BulkMoveDialog]; con
 * [includeUnassigned] se ofrece además la carpeta "Sin lista".
 */
@Composable
fun MoveSongDialog(
    songTitle: String,
    targets: List<Playlist>,
    includeUnassigned: Boolean,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_song_title, songTitle)) },
        text = {
            if (targets.isEmpty() && !includeUnassigned) {
                Text(stringResource(R.string.no_lists_yet))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (includeUnassigned) {
                        item {
                            PickRow(icon = AccordioIcons.notas(), label = stringResource(R.string.no_playlist)) { onPick(null) }
                        }
                    }
                    items(targets, key = { it.id }) { p ->
                        PickRow(icon = AccordioIcons.partitura(), label = p.name) { onPick(p.id) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
private fun PickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null)
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun BulkDeleteDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bulk_delete_title)) },
        text = { Text(pluralStringResource(R.plurals.bulk_delete_msg, count, count)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
