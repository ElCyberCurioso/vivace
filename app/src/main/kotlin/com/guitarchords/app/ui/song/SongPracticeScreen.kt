package com.guitarchords.app.ui.song

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordDiagram
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.chords.SongChords
import com.guitarchords.app.metronome.MetronomeEngine
import com.guitarchords.app.ui.components.EmptyState

/**
 * Práctica de los cambios de acorde de una canción concreta: coge los acordes
 * que realmente aparecen en la partitura y los va emparejando contra el
 * metrónomo. Es una herramienta libre (no puntúa ni da XP): el entrenamiento
 * con progreso vive en el curriculum.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPracticeScreen(
    songId: Long,
    onBack: () -> Unit,
    vm: SongViewModel = viewModel()
) {
    LaunchedEffect(songId) { vm.load(songId) }
    val song by vm.song.collectAsStateWithLifecycle()

    val chords = remember(song?.content) {
        song?.content?.let { SongChords.distinctChords(it) }.orEmpty()
    }
    val pairs = remember(chords) { SongChords.changePairs(chords) }

    val engine = remember { MetronomeEngine() }
    val scope = rememberCoroutineScope()
    val running by engine.running.collectAsStateWithLifecycle()
    val beat by engine.beat.collectAsStateWithLifecycle()
    var bpm by remember { mutableIntStateOf(60) }
    var index by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) { onDispose { engine.release() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.practice_changes_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { pv ->
        if (pairs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PlayArrow,
                title = stringResource(R.string.practice_no_chords_title),
                subtitle = stringResource(R.string.practice_no_chords_subtitle),
                modifier = Modifier.padding(pv)
            )
            return@Scaffold
        }

        val pair = pairs[index.coerceIn(0, pairs.lastIndex)]
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.practice_pair_of, index + 1, pairs.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (name in listOf(pair.first, pair.second)) {
                    ChordLibrary.find(name)?.variations?.firstOrNull()?.let { shape ->
                        ChordDiagram(
                            shape = shape,
                            name = name,
                            modifier = Modifier.widthIn(max = 150.dp).weight(1f).padding(8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Pulso del compás (4/4), como en los ejercicios de entrenamiento.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 1..4) {
                    val active = running && beat == i
                    val color = when {
                        active && i == 1 -> MaterialTheme.colorScheme.primary
                        active -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(if (i == 1) 22.dp else 18.dp)
                            .background(color, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.bpm, bpm),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Slider(
                value = bpm.toFloat(),
                onValueChange = { bpm = it.toInt(); engine.bpm = bpm },
                valueRange = 30f..200f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.practice_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(onClick = {
                    if (running) engine.stop() else {
                        engine.bpm = bpm
                        engine.beatsPerBar = 4
                        engine.start(scope)
                    }
                }) {
                    Icon(
                        if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (running) stringResource(R.string.stop) else stringResource(R.string.start)
                    )
                }
                FilledTonalButton(onClick = { index = (index + 1) % pairs.size }) {
                    Icon(Icons.Default.SkipNext, null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.practice_next_pair))
                }
            }
        }
    }
}
