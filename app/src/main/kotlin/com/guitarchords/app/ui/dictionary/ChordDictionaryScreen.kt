package com.guitarchords.app.ui.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.guitarchords.app.ui.components.ChordModal
import com.guitarchords.app.ui.components.EmptyState
import com.guitarchords.app.ui.responsive.WidthClass
import com.guitarchords.app.ui.responsive.rememberWidthClass

private val QUALITY_LABEL = mapOf(
    "" to "maj",
    "m" to "m",
    "7" to "7",
    "maj7" to "maj7",
    "m7" to "m7",
    "sus2" to "sus2",
    "sus4" to "sus4",
    "dim" to "dim",
    "dim7" to "dim7",
    "m7b5" to "m7b5",
    "aug" to "aug",
    "add9" to "add9",
    "6" to "6",
    "9" to "9",
    "m6" to "m6"
)

private enum class DictTab { CHORDS, CIRCLE, GUIDE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordDictionaryScreen(onBack: () -> Unit) {
    var root by remember { mutableStateOf<String?>(null) }
    var quality by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(DictTab.CHORDS) }

    val all = remember { ChordLibrary.all() }
    val filtered = remember(root, quality, all) {
        all.filter {
            (root == null || it.root == root) &&
                (quality == null || it.quality == quality)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (tab) {
                            DictTab.CHORDS -> stringResource(R.string.dictionary_title)
                            DictTab.CIRCLE -> stringResource(R.string.circle_of_fifths)
                            DictTab.GUIDE -> stringResource(R.string.theory_guide)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (tab != DictTab.CHORDS) tab = DictTab.CHORDS else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    IconButton(onClick = {
                        tab = if (tab == DictTab.GUIDE) DictTab.CHORDS else DictTab.GUIDE
                    }) { Icon(Icons.Default.School, stringResource(R.string.theory_guide)) }
                    IconButton(onClick = {
                        tab = if (tab == DictTab.CIRCLE) DictTab.CHORDS else DictTab.CIRCLE
                    }) { Icon(Icons.Default.Album, stringResource(R.string.circle_of_fifths)) }
                }
            )
        }
    ) { pv ->
        if (tab == DictTab.CIRCLE) {
            Box(Modifier.fillMaxSize().padding(pv)) {
                CircleOfFifthsView(onChordClick = { selected = it })
            }
            return@Scaffold
        }
        if (tab == DictTab.GUIDE) {
            Box(Modifier.fillMaxSize().padding(pv)) {
                TheoryGuideView()
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(pv)) {
            FilterRow(
                label = stringResource(R.string.root),
                options = ChordLibrary.ROOTS,
                optionLabel = { it },
                selected = root,
                onSelect = { root = it }
            )
            FilterRow(
                label = stringResource(R.string.quality),
                options = ChordLibrary.QUALITIES,
                optionLabel = { QUALITY_LABEL[it] ?: it },
                selected = quality,
                onSelect = { quality = it }
            )
            Spacer(Modifier.height(4.dp))
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.MusicOff,
                    title = stringResource(R.string.no_results),
                    subtitle = stringResource(R.string.change_filters)
                )
            } else {
                val tileMin = when (rememberWidthClass()) {
                    WidthClass.COMPACT -> 140.dp
                    WidthClass.MEDIUM -> 170.dp
                    WidthClass.EXPANDED -> 200.dp
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = tileMin),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.name }) { chord ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selected = chord.name }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    chord.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(Modifier.height(4.dp))
                                val first = chord.variations.firstOrNull()
                                if (first != null) {
                                    ChordDiagram(shape = first)
                                } else {
                                    Text(
                                        stringResource(R.string.no_diagram),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { name ->
        ChordModal(chordName = name, onDismiss = { selected = null })
    }
}

@Composable
private fun <T> FilterRow(
    label: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T?,
    onSelect: (T?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp)
        )
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.all_filter)) },
            modifier = Modifier.padding(end = 4.dp)
        )
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(if (selected == opt) null else opt) },
                label = { Text(optionLabel(opt)) },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}
