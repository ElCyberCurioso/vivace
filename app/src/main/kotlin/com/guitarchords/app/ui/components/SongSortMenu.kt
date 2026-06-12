package com.guitarchords.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.data.SongSort

/** Acción de barra superior: abre un menú para elegir el orden de la carpeta. */
@Composable
fun SongSortMenu(current: SongSort, onPick: (SongSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_by))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SongSort.entries.forEach { s ->
                DropdownMenuItem(
                    text = { Text(stringResource(s.labelRes)) },
                    leadingIcon = {
                        if (s == current) Icon(Icons.Default.Check, null)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = { open = false; onPick(s) }
                )
            }
        }
    }
}
