package com.guitarchords.app.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarchords.app.R
import com.guitarchords.app.training.ChordChangeSpec
import com.guitarchords.app.training.ChordQuizSpec
import com.guitarchords.app.training.EarTrainingSpec
import com.guitarchords.app.training.ExerciseSpec
import com.guitarchords.app.training.RhythmTapSpec
import com.guitarchords.app.training.ScaleNotesSpec
import com.guitarchords.app.training.TheoryQuizSpec
import com.guitarchords.app.ui.training.components.LevelBadge
import com.guitarchords.app.ui.training.exercises.ChordChangeExercise
import com.guitarchords.app.ui.training.exercises.ChordQuizExercise
import com.guitarchords.app.ui.training.exercises.EarTrainingExercise
import com.guitarchords.app.ui.training.exercises.RhythmTapExercise
import com.guitarchords.app.ui.training.exercises.ScaleNotesExercise
import com.guitarchords.app.ui.training.exercises.TheoryQuizExercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseRunnerScreen(
    exerciseId: String,
    onBack: () -> Unit,
    vm: ExerciseRunnerViewModel = viewModel()
) {
    LaunchedEffect(exerciseId) { vm.load(exerciseId) }
    val state by vm.state.collectAsStateWithLifecycle()

    val spec: ExerciseSpec? = when (val s = state) {
        is RunnerState.Intro -> s.spec
        is RunnerState.Running -> s.spec
        is RunnerState.Saving -> s.spec
        is RunnerState.Finished -> s.spec
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        spec?.let { stringResource(it.titleRes, *it.titleArgs.toTypedArray()) }
                            ?: stringResource(R.string.training_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { pv ->
        when (val s = state) {
            RunnerState.Loading, is RunnerState.Saving -> Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            RunnerState.NotFound -> Box(
                Modifier.fillMaxSize().padding(pv),
                contentAlignment = Alignment.Center
            ) { Text(stringResource(R.string.tr_exercise_not_found)) }

            is RunnerState.Intro -> ExerciseIntro(
                spec = s.spec,
                onStart = { vm.start() },
                modifier = Modifier.fillMaxSize().padding(pv)
            )

            is RunnerState.Running -> Box(Modifier.fillMaxSize().padding(pv)) {
                ExerciseContent(spec = s.spec, vm = vm)
            }

            is RunnerState.Finished -> ResultCelebration(
                summary = s.summary,
                area = s.spec.area,
                hasNext = s.nextExerciseId != null,
                onRetry = { vm.retry() },
                onNext = { s.nextExerciseId?.let { vm.loadNext(it) } },
                onBack = onBack,
                modifier = Modifier.padding(pv)
            )
        }
    }
}

@Composable
private fun ExerciseIntro(
    spec: ExerciseSpec,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LevelBadge(spec.level)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(spec.titleRes, *spec.titleArgs.toTypedArray()),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(spec.descRes),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow, null)
            Text(stringResource(R.string.training_start))
        }
    }
}

/** Despacha al composable de cada tipo de ejercicio. */
@Composable
private fun ExerciseContent(spec: ExerciseSpec, vm: ExerciseRunnerViewModel) {
    when (spec) {
        is TheoryQuizSpec -> TheoryQuizExercise(
            spec = spec,
            onFinish = { score, passed -> vm.finish(score, passed) }
        )
        is ChordQuizSpec -> ChordQuizExercise(
            spec = spec,
            onFinish = { score, passed -> vm.finish(score, passed) }
        )
        is ChordChangeSpec -> ChordChangeExercise(
            spec = spec,
            onFinish = { score, passed -> vm.finish(score, passed) }
        )
        is RhythmTapSpec -> RhythmTapExercise(
            spec = spec,
            onFinish = { score, passed -> vm.finish(score, passed) }
        )
        is ScaleNotesSpec -> ScaleNotesExercise(
            spec = spec,
            onFinish = { score, passed, mic -> vm.finish(score, passed, micValidated = mic) }
        )
        is EarTrainingSpec -> EarTrainingExercise(
            spec = spec,
            onFinish = { score, passed -> vm.finish(score, passed) }
        )
    }
}
