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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordDiagram
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.chords.ChordVariants
import com.guitarchords.app.chords.Instrument
import com.guitarchords.app.chords.InstrumentPrefs
import com.guitarchords.app.chords.CustomChords
import com.guitarchords.app.chords.MusicTheory
import com.guitarchords.app.ui.icons.AccordioIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordModal(
    chordName: String,
    onDismiss: () -> Unit,
    /**
     * Elecciones de la partitura que se está mirando (`songs.chord_variants`).
     * Se pasa el JSON entero y no un índice ya resuelto porque quien sabe cuántas
     * posturas hay —y qué instrumento se está mirando, que aquí se puede cambiar—
     * es este modal. El diccionario y el buscador no pasan ninguna y entran por
     * la primera, que es lo de siempre.
     */
    variantsJson: String? = null,
    /**
     * Fija esta postura para la partitura que se está mirando. Null donde no hay
     * partitura de por medio (diccionario, buscador): allí no habría dónde
     * guardarla, y el botón ni aparece.
     */
    onChoose: ((Int) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()
    // Las digitaciones personalizadas pueden cambiar mientras el modal está
    // abierto (alta/edición/borrado): la revisión fuerza re-resolver el acorde.
    val revision by CustomChords.revision.collectAsState()
    /*
     * El acorde se resuelve para el instrumento que se esté mirando: el mismo
     * nombre, otra digitación. La elección se comparte con el diccionario, así
     * que quien la cambia aquí la encuentra puesta allí.
     */
    val instrument by InstrumentPrefs.current.collectAsState()
    val chord = remember(chordName, revision, instrument) {
        ChordLibrary.find(chordName, instrument)
    }
    /*
     * Las digitaciones propias se guardan con seis trastes y sin decir de qué
     * instrumento son, así que el editor solo se ofrece en guitarra: en ukelele
     * dibujaría un mástil de seis cuerdas y guardaría algo que nadie sabría leer.
     */
    val chordKey = remember(chordName, instrument) {
        if (instrument != Instrument.GUITAR) null
        else ChordLibrary.parseName(chordName)?.let { (root, qual) -> root + qual }
    }
    /*
     * Se abre por la postura que ESTA partitura tenga elegida para este
     * instrumento, no siempre por la primera. Al cambiar de instrumento dentro
     * del modal se recalcula: cada uno guarda la suya.
     */
    var index by remember(chordName, instrument, chord) {
        mutableIntStateOf(
            ChordVariants.indexFor(variantsJson, instrument, chordName, chord?.variations?.size ?: 0)
        )
    }
    val player = rememberChordPlayer()

    // null = cerrado; (customId?, fretsIniciales?) = editor abierto.
    var editing by remember { mutableStateOf<Pair<Long?, List<Int>?>?>(null) }
    var confirmDelete by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Instrument.entries.forEach { opcion ->
                    FilterChip(
                        selected = instrument == opcion,
                        onClick = { InstrumentPrefs.set(opcion) },
                        label = { Text(stringResource(etiquetaDe(opcion))) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (chord == null) {
                Text(stringResource(R.string.unknown_chord, chordName), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
            } else if (chord.variations.isEmpty()) {
                Text(
                    chord.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                val formula = MusicTheory.FORMULAS.firstOrNull { it.quality == chord.quality }
                if (formula != null) {
                    Text(
                        stringResource(R.string.degrees, formula.degrees),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(
                            R.string.notes,
                            MusicTheory.notesFor(
                                ChordLibrary.ROOTS.indexOf(chord.root).coerceAtLeast(0),
                                formula.semitones
                            ).joinToString(" · ")
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    stringResource(R.string.no_diagram_position),
                    style = MaterialTheme.typography.bodySmall
                )
                if (chordKey != null) {
                    Spacer(Modifier.height(8.dp))
                    AssistChip(
                        onClick = { editing = null to null },
                        label = { Text(stringResource(R.string.add_custom_shape)) },
                        leadingIcon = { Icon(AccordioIcons.mas(), null) }
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Text(chord.name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                val total = chord.variations.size
                val safe = index.coerceIn(0, total - 1)
                val shape = chord.variations[safe]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (safe > 0) index = safe - 1 },
                        enabled = safe > 0
                    ) { Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous)) }

                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        // Tocar una cuerda del diagrama la hace sonar suelta.
                        ChordDiagram(
                            shape = shape,
                            onStringTap = { s -> player.pluck(s, shape.frets) }
                        )
                    }

                    IconButton(
                        onClick = { if (safe < total - 1) index = safe + 1 },
                        enabled = safe < total - 1
                    ) { Icon(AccordioIcons.siguiente(), stringResource(R.string.next)) }
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = { player.strum(shape.frets) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.listen_chord))
                }
                Text(
                    stringResource(R.string.listen_strings_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.variation_n, safe + 1, total),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (shape.customId != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.custom_shape)) }
                        )
                    }
                }
                /*
                 * Fijar la postura para ESTA partitura. Solo aparece con una
                 * partitura detrás (el visor) y cuando hay más de una donde
                 * elegir: en el diccionario no habría dónde guardarla.
                 */
                if (onChoose != null && total > 1) {
                    Spacer(Modifier.height(8.dp))
                    val yaElegida = safe == ChordVariants.indexFor(
                        variantsJson, instrument, chordName, total
                    )
                    if (yaElegida) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.shape_used_in_song)) }
                        )
                    } else {
                        AssistChip(
                            onClick = { onChoose(safe) },
                            label = { Text(stringResource(R.string.use_shape_in_song)) }
                        )
                    }
                }
                if (chordKey != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { editing = null to null },
                            label = { Text(stringResource(R.string.add_custom_shape)) },
                            leadingIcon = { Icon(AccordioIcons.mas(), null) }
                        )
                        if (shape.customId != null) {
                            AssistChip(
                                onClick = { editing = shape.customId to shape.frets },
                                label = { Text(stringResource(R.string.edit)) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            AssistChip(
                                onClick = { confirmDelete = shape.customId },
                                label = { Text(stringResource(R.string.delete)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    editing?.let { (editId, initialFrets) ->
        if (chordKey != null) {
            ChordShapeEditorDialog(
                chordName = chordKey,
                initialFrets = initialFrets,
                onSave = { frets ->
                    if (editId == null) {
                        CustomChords.add(chordKey, frets)
                        index = 0   // la nueva digitación aparece la primera
                    } else {
                        CustomChords.update(editId, chordKey, frets)
                    }
                    editing = null
                },
                onDismiss = { editing = null }
            )
        }
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.delete_custom_shape)) },
            text = { Text(stringResource(R.string.delete_custom_shape_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    CustomChords.delete(id)
                    confirmDelete = null
                    index = 0
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

private fun etiquetaDe(instrumento: Instrument): Int = when (instrumento) {
    Instrument.GUITAR -> R.string.instrument_guitar
    Instrument.UKULELE -> R.string.instrument_ukulele
}
