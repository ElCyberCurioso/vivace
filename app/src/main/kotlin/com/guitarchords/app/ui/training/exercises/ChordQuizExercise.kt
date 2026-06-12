package com.guitarchords.app.ui.training.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.chords.ChordDiagram
import com.guitarchords.app.chords.ChordLibrary
import com.guitarchords.app.chords.ChordRecognizer
import com.guitarchords.app.training.ChordQuizSpec
import com.guitarchords.app.ui.components.BaseFretControls
import com.guitarchords.app.ui.components.FretboardInput
import com.guitarchords.app.ui.components.StringStateRow
import com.guitarchords.app.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Fase actual del acorde en curso. */
private enum class Phase { INPUT, CORRECT, SOLUTION }

/** Separa "F#m7" en raíz ("F#") y calidad ("m7"). */
private fun parseChord(name: String): Pair<String, String> {
    val root = if (name.length > 1 && (name[1] == '#' || name[1] == 'b')) name.take(2) else name.take(1)
    return root to name.removePrefix(root)
}

@Composable
fun ChordQuizExercise(
    spec: ChordQuizSpec,
    onFinish: (score: Int, passed: Boolean) -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var attempts by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(Phase.INPUT) }
    val frets = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }
    var baseFret by remember { mutableIntStateOf(1) }

    val target = spec.chords[index]
    val (targetRoot, targetQuality) = parseChord(target)

    fun resetBoard() {
        for (i in 0 until 6) frets[i] = 0
        baseFret = 1
    }

    fun nextOrFinish() {
        if (index + 1 < spec.chords.size) {
            index++
            attempts = 0
            phase = Phase.INPUT
            resetBoard()
        } else {
            val score = (correctCount * 100f / spec.chords.size).roundToInt()
            onFinish(score, score >= spec.passPct)
        }
    }

    // El acierto avanza solo tras un breve feedback.
    LaunchedEffect(phase, index) {
        if (phase == Phase.CORRECT) {
            delay(900)
            nextOrFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.tr_question_progress, index + 1, spec.chords.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { index.toFloat() / spec.chords.size },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                target,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = if (phase == Phase.CORRECT) MaterialTheme.extendedColors.success
                else MaterialTheme.colorScheme.onSurface
            )
            if (phase == Phase.CORRECT) {
                Spacer(Modifier.size(8.dp))
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.extendedColors.success
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        when (phase) {
            Phase.SOLUTION -> {
                Text(
                    stringResource(R.string.tr_chord_solution),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                ChordLibrary.find(target)?.variations?.firstOrNull()?.let { shape ->
                    ChordDiagram(
                        shape = shape,
                        name = target,
                        modifier = Modifier.widthIn(max = 220.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { nextOrFinish() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tr_next_chord))
                }
            }
            else -> {
                StringStateRow(frets) { i, v -> frets[i] = v }
                Spacer(Modifier.height(8.dp))
                FretboardInput(
                    frets = frets.toList(),
                    baseFret = baseFret,
                    onTapFret = { stringIdx, fret ->
                        if (phase == Phase.INPUT) {
                            frets[stringIdx] = if (frets[stringIdx] == fret) 0 else fret
                        }
                    },
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.9f)
                        .aspectRatio(6f / 7f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BaseFretControls(baseFret) { baseFret = it }
                    IconButton(onClick = { resetBoard() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.clear))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (attempts > 0) {
                    Text(
                        stringResource(R.string.tr_chord_try_again),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Button(
                    onClick = {
                        val ok = ChordRecognizer.identify(frets.toList()).any {
                            it.root == targetRoot && it.quality == targetQuality &&
                                (!spec.requireRootBass || it.bassIsRoot)
                        }
                        if (ok) {
                            correctCount++
                            phase = Phase.CORRECT
                        } else {
                            attempts++
                            if (attempts >= 2) phase = Phase.SOLUTION
                        }
                    },
                    enabled = phase == Phase.INPUT,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tr_check))
                }
            }
        }
    }
}
