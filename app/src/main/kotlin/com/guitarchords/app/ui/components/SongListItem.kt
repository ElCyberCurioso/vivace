package com.guitarchords.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.data.Song

/**
 * Fila de canción compartida por todas las listas (carpeta, sin lista,
 * favoritas y búsqueda): tarjeta seleccionable con título + estado de
 * sincronización, artista · género y, opcionalmente, la lista a la que
 * pertenece.
 *
 * Lo que cambia entre pantallas entra por los huecos [leading] (estrella,
 * normalmente) y [trailing] (menú, botones o asa de arrastre); mientras hay una
 * selección activa, [leading] se sustituye por la casilla y [trailing] se
 * oculta, que es el comportamiento que ya tenían las cuatro pantallas.
 */
@Composable
fun SongListItem(
    song: Song,
    selectionActive: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    playlistName: String = "",
    leading: @Composable (RowScope.() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Card(
        colors = if (selected)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else CardDefaults.cardColors(),
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(selectionActive) {
                detectTapGestures(
                    onTap = { if (selectionActive) onToggle() else onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            if (selectionActive) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
            } else {
                leading?.invoke(this)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.size(6.dp))
                    SyncBadge(synced = song.remoteKey != null)
                }
                val sub = buildString {
                    if (song.artist.isNotBlank()) append(song.artist)
                    if (song.genre.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(song.genre)
                    }
                }
                if (sub.isNotEmpty()) {
                    Text(sub, style = MaterialTheme.typography.bodySmall)
                }
                if (playlistName.isNotBlank()) {
                    Text(
                        stringResource(R.string.in_playlist, playlistName),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!selectionActive) trailing?.invoke(this)
        }
    }
}
