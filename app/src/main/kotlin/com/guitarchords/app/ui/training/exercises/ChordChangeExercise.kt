package com.guitarchords.app.ui.training.exercises

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordDiagram
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.metronome.MetronomeEngine
import com.guitarchords.app.training.ChordChangeSpec
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ChangePhase { RUNNING, COUNT }

/** Indicador de pulso del compás (el acento en grande). */
@Composable
fun BeatIndicator(beat: Int, beatsPerBar: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        for (b in 1..beatsPerBar) {
            val active = b == beat
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (b == 1) 16.dp else 12.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun ChordChangeExercise(
    spec: ChordChangeSpec,
    onFinish: (score: Int, passed: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val engine = remember { MetronomeEngine() }
    DisposableEffect(Unit) { onDispose { engine.release() } }

    var phase by remember { mutableStateOf(ChangePhase.RUNNING) }
    var remaining by remember { mutableIntStateOf(spec.durationSec) }
    var count by remember { mutableIntStateOf(0) }
    val beat by engine.beat.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        engine.bpm = spec.bpm
        engine.beatsPerBar = 4
        engine.start(scope)
        for (t in spec.durationSec downTo 1) {
            remaining = t
            delay(1000)
        }
        engine.stop()
        phase = ChangePhase.COUNT
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (name in listOf(spec.chordA, spec.chordB)) {
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

        when (phase) {
            ChangePhase.RUNNING -> {
                BeatIndicator(beat, 4)
                Spacer(Modifier.height(16.dp))
                Text(
                    "$remaining s",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tr_change_keep_going, spec.bpm),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ChangePhase.COUNT -> {
                Text(
                    stringResource(R.string.tr_change_how_many),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.tr_change_target, spec.targetChanges),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (count > 0) count-- }) {
                        Icon(Icons.Default.Remove, stringResource(R.string.count_down))
                    }
                    Text(
                        "$count",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(onClick = { count++ }) {
                        Icon(Icons.Default.Add, stringResource(R.string.count_up))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val score = min(100, (count * 100f / spec.targetChanges).roundToInt())
                        onFinish(score, score >= 70)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tr_finish))
                }
            }
        }
    }
}
