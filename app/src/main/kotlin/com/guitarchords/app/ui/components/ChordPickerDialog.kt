package com.guitarchords.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarchords.app.chords.ChordLibrary

/** Searchable grid of chord names. Returns the picked chord name via [onPick]. */
@Composable
fun ChordPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
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
