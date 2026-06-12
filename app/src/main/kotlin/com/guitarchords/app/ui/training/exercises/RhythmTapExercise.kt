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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.metronome.MetronomeEngine
import com.guitarchords.app.training.RhythmTapSpec
import com.guitarchords.app.training.TapScorer

private const val COUNT_IN_BARS = 2

@Composable
fun RhythmTapExercise(
    spec: RhythmTapSpec,
    onFinish: (score: Int, passed: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val engine = remember { MetronomeEngine() }
    DisposableEffect(Unit) { onDispose { engine.release() } }

    val beat by engine.beat.collectAsStateWithLifecycle()
    var beatsHeard by remember { mutableIntStateOf(0) }
    var scoringStartMs by remember { mutableStateOf(0L) }
    val taps = remember { mutableListOf<Long>() }
    var currentBar by remember { mutableIntStateOf(0) }    // 0 = count-in

    val countInBeats = COUNT_IN_BARS * spec.beatsPerBar
    val totalBeats = countInBeats + spec.bars * spec.beatsPerBar

    // Cuenta beats reales del engine; el primero puntuable fija el origen de tiempos.
    LaunchedEffect(beat) {
        if (beat <= 0) return@LaunchedEffect
        beatsHeard++
        if (beatsHeard == countInBeats + 1) scoringStartMs = System.currentTimeMillis()
        currentBar = (beatsHeard - 1) / spec.beatsPerBar - COUNT_IN_BARS + 1
        if (beatsHeard >= totalBeats + 1) {
            engine.stop()
            val expected = TapScorer.expectedTimes(
                spec.pattern, spec.beatsPerBar, spec.bars, spec.bpm
            )
            val result = TapScorer.score(expected, taps.toList(), spec.toleranceMs)
            onFinish(result.score, result.score >= spec.passPct)
        }
    }

    LaunchedEffect(Unit) {
        engine.bpm = spec.bpm
        engine.beatsPerBar = spec.beatsPerBar
        engine.start(scope)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BeatIndicator(beat, spec.beatsPerBar)
        Spacer(Modifier.height(8.dp))
        Text(
            if (currentBar <= 0) stringResource(R.string.tr_rhythm_count_in)
            else stringResource(R.string.tr_rhythm_bar, currentBar.coerceAtMost(spec.bars), spec.bars),
            style = MaterialTheme.typography.titleMedium,
            color = if (currentBar <= 0) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))

        // Patrón del compás: un punto por golpe esperado, el del pulso actual resaltado.
        Row(horizontalArrangement = Arrangement.Center) {
            for (pos in spec.pattern) {
                val active = beat > 0 && pos.toInt() + 1 == beat
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(if (active) 22.dp else 16.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // Zona de tap: toda la mitad inferior de la pantalla.
        var pressed by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .pointerInput(scoringStartMs) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            if (scoringStartMs > 0L) {
                                taps.add(System.currentTimeMillis() - scoringStartMs)
                            }
                            tryAwaitRelease()
                            pressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.tr_rhythm_tap_here),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
