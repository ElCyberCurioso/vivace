package com.guitarchords.app.ui.training.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.training.EarMode
import com.guitarchords.app.training.EarQuestionGen
import com.guitarchords.app.training.EarTrainingSpec
import com.guitarchords.app.training.ToneEngine
import com.guitarchords.app.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

/** Nombre de cada opción según el modo del ejercicio. */
@Composable
private fun optionLabel(mode: EarMode, value: Int): String = when (mode) {
    EarMode.CHORD_QUALITY ->
        if (value == EarQuestionGen.MINOR) stringResource(R.string.tr_ear_minor)
        else stringResource(R.string.tr_ear_major)
    EarMode.DIRECTION ->
        if (value > 0) stringResource(R.string.tr_ear_up) else stringResource(R.string.tr_ear_down)
    EarMode.INTERVAL -> intervalName(value)
}

@Composable
private fun intervalName(semitones: Int): String {
    val res = when (semitones) {
        1 -> R.string.tr_int_m2
        2 -> R.string.tr_int_M2
        3 -> R.string.tr_int_m3
        4 -> R.string.tr_int_M3
        5 -> R.string.tr_int_P4
        6 -> R.string.tr_int_TT
        7 -> R.string.tr_int_P5
        8 -> R.string.tr_int_m6
        9 -> R.string.tr_int_M6
        10 -> R.string.tr_int_m7
        11 -> R.string.tr_int_M7
        else -> R.string.tr_int_P8
    }
    return stringResource(res)
}

/**
 * Ejercicio de oído: la app toca (intervalo, acorde o dirección) y el usuario
 * elige la respuesta. El sonido se sintetiza con [ToneEngine]; se puede repetir
 * la escucha tantas veces como haga falta antes de responder.
 */
@Composable
fun EarTrainingExercise(
    spec: EarTrainingSpec,
    onFinish: (score: Int, passed: Boolean) -> Unit
) {
    val questions = remember(spec.id) { EarQuestionGen.generate(spec, Random(Random.nextLong())) }
    val tone = remember { ToneEngine() }
    val scope = rememberCoroutineScope()

    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var playing by remember { mutableStateOf(false) }

    val question = questions[index]

    fun playCurrent() {
        if (playing) return
        playing = true
        scope.launch {
            if (question.together) tone.playTogether(question.midis)
            else tone.playSequence(question.midis)
            playing = false
        }
    }

    // Suena automáticamente al llegar a cada pregunta.
    LaunchedEffect(index) { playCurrent() }
    DisposableEffect(Unit) { onDispose { } }

    // Tras responder: feedback breve y avance (o fin del ejercicio).
    LaunchedEffect(selected, index) {
        val answer = selected ?: return@LaunchedEffect
        if (answer == question.answer) correct++
        delay(900)
        if (index + 1 < questions.size) {
            selected = null
            index++
        } else {
            val score = (correct * 100f / questions.size).roundToInt()
            onFinish(score, score >= spec.passPct)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { (index + 1f) / questions.size },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tr_question_progress, index + 1, questions.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        Text(
            stringResource(
                when (spec.mode) {
                    EarMode.INTERVAL -> R.string.tr_ear_q_interval
                    EarMode.CHORD_QUALITY -> R.string.tr_ear_q_quality
                    EarMode.DIRECTION -> R.string.tr_ear_q_direction
                }
            ),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        Button(onClick = { playCurrent() }, enabled = !playing) {
            Icon(if (playing) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Refresh, null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(if (playing) R.string.tr_ear_playing else R.string.tr_ear_replay))
        }
        Spacer(Modifier.height(24.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            question.options.forEach { option ->
                val isAnswer = option == question.answer
                val chosen = selected == option
                val colors = when {
                    selected == null -> ButtonDefaults.outlinedButtonColors()
                    isAnswer -> ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.extendedColors.success.copy(alpha = 0.2f)
                    )
                    chosen -> ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                    else -> ButtonDefaults.outlinedButtonColors()
                }
                OutlinedButton(
                    onClick = { if (selected == null) selected = option },
                    enabled = selected == null,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(optionLabel(spec.mode, option))
                }
            }
        }
    }
}
