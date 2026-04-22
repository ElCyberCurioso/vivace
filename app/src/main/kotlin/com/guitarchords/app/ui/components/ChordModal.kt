package com.guitarchords.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.chords.ChordDiagram

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordModal(
    chordName: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val chord = remember(chordName) { ChordLibrary.find(chordName) }
    var index by remember(chordName) { mutableIntStateOf(0) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (chord == null || chord.variations.isEmpty()) {
                Text("Acorde desconocido: $chordName", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
            } else {
                Text(chord.name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                val total = chord.variations.size
                val safe = index.coerceIn(0, total - 1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (safe > 0) index = safe - 1 },
                        enabled = safe > 0
                    ) { Icon(Icons.Default.ChevronLeft, "Anterior") }

                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        ChordDiagram(shape = chord.variations[safe])
                    }

                    IconButton(
                        onClick = { if (safe < total - 1) index = safe + 1 },
                        enabled = safe < total - 1
                    ) { Icon(Icons.Default.ChevronRight, "Siguiente") }
                }
                Spacer(Modifier.height(8.dp))
                Text("Variación ${safe + 1} / $total", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
