package com.guitarchords.app.ui.metronome

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarchords.app.R
import com.guitarchords.app.metronome.MetronomeEngine
import com.guitarchords.app.ui.theme.VivaceMono
import com.guitarchords.app.ui.theme.accordioTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetronomeScreen(onBack: () -> Unit) {
    val engine = remember { MetronomeEngine() }
    val scope = rememberCoroutineScope()
    var bpm by remember { mutableIntStateOf(100) }
    var beatsPerBar by remember { mutableIntStateOf(4) }
    val running by engine.running.collectAsStateWithLifecycle()
    val beat by engine.beat.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { engine.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = accordioTopBarColors(),
                title = { Text(stringResource(R.string.metronome_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                }
            )
        }
    ) { pv ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pv)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                stringResource(R.string.bpm, bpm),
                // Cifras en la monoespaciada de la marca: el ancho no baila al cambiar de BPM.
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = VivaceMono,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Slider(
                value = bpm.toFloat(),
                onValueChange = {
                    bpm = it.toInt()
                    engine.bpm = bpm
                },
                valueRange = 30f..240f,
                modifier = Modifier.fillMaxWidth()
            )

            // Indicador de pulsos del compás.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (i in 1..beatsPerBar) {
                    val active = running && beat == i
                    val color by animateColorAsState(
                        targetValue = when {
                            active && i == 1 -> MaterialTheme.colorScheme.primary
                            active -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        label = "beatColor"
                    )
                    Box(
                        modifier = Modifier
                            .size(if (i == 1) 22.dp else 18.dp)
                            .background(color, CircleShape)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.beats_per_bar),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 6).forEach { n ->
                        FilterChip(
                            selected = beatsPerBar == n,
                            onClick = {
                                beatsPerBar = n
                                engine.beatsPerBar = n
                            },
                            label = { Text("$n") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LargeFloatingActionButton(
                onClick = {
                    if (running) engine.stop()
                    else {
                        engine.bpm = bpm
                        engine.beatsPerBar = beatsPerBar
                        engine.start(scope)
                    }
                }
            ) {
                Icon(
                    if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (running) stringResource(R.string.stop) else stringResource(R.string.start),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
