package com.guitarchords.app.ui.training

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.training.PlacementTest
import com.guitarchords.app.training.TrainingArea
import com.guitarchords.app.ui.training.components.LevelBadge
import com.guitarchords.app.ui.training.components.icon
import com.guitarchords.app.ui.training.components.titleRes
import com.guitarchords.app.ui.training.exercises.ChordChangeExercise
import com.guitarchords.app.ui.training.exercises.ChordQuizExercise
import com.guitarchords.app.ui.training.exercises.RhythmTapExercise
import com.guitarchords.app.ui.training.exercises.ScaleNotesExercise
import com.guitarchords.app.ui.training.exercises.TheoryQuizExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    vm: PlacementViewModel = viewModel()
) {
    val step by vm.step.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.training_placement_invite_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { pv ->
        Column(Modifier.fillMaxSize().padding(pv)) {
            LinearProgressIndicator(
                progress = { step.ordinal.toFloat() / PlacementTest.Step.SUMMARY.ordinal },
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
            // El paso usa key() para que cada prueba arranque con estado limpio.
            androidx.compose.runtime.key(step) {
                when (step) {
                    PlacementTest.Step.EXPERIENCE -> ExperienceStep(
                        onChoose = { vm.chooseExperience(it) }
                    )
                    PlacementTest.Step.THEORY -> TheoryQuizExercise(
                        spec = PlacementTest.theorySpec,
                        onFinish = { score, _ -> vm.stepDone(TrainingArea.THEORY, score) }
                    )
                    PlacementTest.Step.CHORDS -> ChordQuizExercise(
                        spec = PlacementTest.chordsSpec,
                        onFinish = { score, _ -> vm.stepDone(TrainingArea.CHORDS, score) }
                    )
                    PlacementTest.Step.CHANGES -> ChordChangeExercise(
                        spec = PlacementTest.changesSpec,
                        onFinish = { score, _ -> vm.stepDone(TrainingArea.CHANGES, score) }
                    )
                    PlacementTest.Step.RHYTHM -> RhythmTapExercise(
                        spec = PlacementTest.rhythmSpec,
                        onFinish = { score, _ -> vm.stepDone(TrainingArea.RHYTHM, score) }
                    )
                    PlacementTest.Step.SCALES -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            ScaleNotesExercise(
                                spec = PlacementTest.scalesSpec,
                                onFinish = { score, _, _ -> vm.stepDone(TrainingArea.SCALES, score) }
                            )
                        }
                        TextButton(
                            onClick = { vm.skipStep() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(stringResource(R.string.placement_skip_step))
                        }
                    }
                    PlacementTest.Step.SUMMARY -> SummaryStep(
                        levels = vm.computedLevels,
                        onConfirm = { vm.confirm(onDone) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperienceStep(onChoose: (PlacementTest.Experience) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.placement_exp_q),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        val options = listOf(
            PlacementTest.Experience.NONE to R.string.placement_exp_none,
            PlacementTest.Experience.UNDER_6M to R.string.placement_exp_under6m,
            PlacementTest.Experience.UNDER_2Y to R.string.placement_exp_under2y,
            PlacementTest.Experience.OVER_2Y to R.string.placement_exp_over2y
        )
        options.forEach { (exp, labelRes) ->
            OutlinedButton(
                onClick = { onChoose(exp) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SummaryStep(
    levels: Map<TrainingArea, Int>,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.placement_summary_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.placement_summary_msg),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                TrainingArea.entries.forEach { area ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Icon(
                            area.icon, null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            stringResource(area.titleRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        LevelBadge(levels[area] ?: 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.placement_confirm))
        }
    }
}
