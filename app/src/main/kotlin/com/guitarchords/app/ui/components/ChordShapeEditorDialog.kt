package com.guitarchords.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R

/**
 * Editor de una digitación personalizada: mástil interactivo + fila X/O +
 * traste base. [initialFrets] nulo crea una digitación nueva.
 */
@Composable
fun ChordShapeEditorDialog(
    chordName: String,
    initialFrets: List<Int>?,
    onSave: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val frets = remember {
        mutableStateListOf<Int>().apply { addAll(initialFrets ?: List(6) { 0 }) }
    }
    var baseFret by remember {
        val positives = (initialFrets ?: emptyList()).filter { it > 0 }
        val start = when {
            positives.isEmpty() -> 1
            positives.max() <= 5 -> 1
            else -> positives.min()
        }
        mutableIntStateOf(start.coerceIn(1, 12))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(chordName) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.custom_shape_hint),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                StringStateRow(frets) { i, v -> frets[i] = v }
                Spacer(Modifier.height(8.dp))
                FretboardInput(
                    frets = frets.toList(),
                    baseFret = baseFret,
                    onTapFret = { stringIdx, fret ->
                        frets[stringIdx] = if (frets[stringIdx] == fret) 0 else fret
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(6f / 7f)
                )
                Spacer(Modifier.height(4.dp))
                BaseFretControls(baseFret) { baseFret = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(frets.toList()) },
                enabled = frets.any { it >= 0 }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
