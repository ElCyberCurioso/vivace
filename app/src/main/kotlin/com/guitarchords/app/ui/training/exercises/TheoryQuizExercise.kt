package com.guitarchords.app.ui.training.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guitarchords.app.R
import com.guitarchords.app.training.TheoryQuestionGen
import com.guitarchords.app.training.TheoryQuizSpec
import com.guitarchords.app.training.TheoryTopic
import com.guitarchords.app.ui.theme.extendedColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

/** Plantilla de enunciado de cada tipo de pregunta. */
private fun promptRes(topic: TheoryTopic): Int = when (topic) {
    TheoryTopic.NOTES_OF_CHORD -> R.string.tr_q_notes_of_chord
    TheoryTopic.INTERVAL_BETWEEN -> R.string.tr_q_interval
    TheoryTopic.FORMULA_TO_CHORD -> R.string.tr_q_formula_to_chord
    TheoryTopic.CHORD_TO_FORMULA -> R.string.tr_q_chord_to_formula
    TheoryTopic.LATIN_NOTATION -> R.string.tr_q_latin
}

@Composable
fun TheoryQuizExercise(
    spec: TheoryQuizSpec,
    onFinish: (score: Int, passed: Boolean) -> Unit
) {
    val questions = remember(spec.id) {
        TheoryQuestionGen.generate(spec, seed = Random.nextLong())
    }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }

    val question = questions[index]

    // Tras responder: breve feedback y avance automático (o fin).
    LaunchedEffect(selected, index) {
        val sel = selected ?: return@LaunchedEffect
        if (sel == question.correctIndex) correct++
        delay(900)
        if (index + 1 < questions.size) {
            index++
            selected = null
        } else {
            val score = (correct * 100f / questions.size).roundToInt()
            onFinish(score, score >= spec.passPct)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.tr_question_progress, index + 1, questions.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { index.toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(promptRes(question.topic), *question.args.toTypedArray()),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        question.options.forEachIndexed { i, option ->
            val showFeedback = selected != null
            val isCorrect = i == question.correctIndex
            val isSelected = i == selected
            val colors = when {
                showFeedback && isCorrect -> ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.extendedColors.success.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.extendedColors.success
                )
                showFeedback && isSelected -> ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.error
                )
                else -> ButtonDefaults.outlinedButtonColors()
            }
            OutlinedButton(
                onClick = { if (selected == null) selected = i },
                enabled = selected == null || showFeedback,
                colors = colors,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(option, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
