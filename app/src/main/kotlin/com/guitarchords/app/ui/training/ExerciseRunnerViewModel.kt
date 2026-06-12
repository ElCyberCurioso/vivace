package com.guitarchords.app.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.training.Curriculum
import com.guitarchords.app.training.ExerciseSpec
import com.guitarchords.app.training.ResultSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados del runner. El resultado es un estado del mismo destino de
 * navegación (no una ruta aparte): evita serializar resúmenes por nav-args.
 */
sealed interface RunnerState {
    data object Loading : RunnerState
    data object NotFound : RunnerState
    data class Intro(val spec: ExerciseSpec) : RunnerState
    data class Running(val spec: ExerciseSpec) : RunnerState
    data class Saving(val spec: ExerciseSpec) : RunnerState
    data class Finished(
        val spec: ExerciseSpec,
        val summary: ResultSummary,
        val nextExerciseId: String?
    ) : RunnerState
}

class ExerciseRunnerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GuitarChordsApp).trainingRepo

    private val _state = MutableStateFlow<RunnerState>(RunnerState.Loading)
    val state = _state.asStateFlow()

    private var startedAtMs = 0L

    fun load(exerciseId: String) {
        if (_state.value != RunnerState.Loading) return
        val spec = Curriculum.byId(exerciseId)
        _state.value = if (spec == null) RunnerState.NotFound else RunnerState.Intro(spec)
    }

    fun start() {
        val spec = (_state.value as? RunnerState.Intro)?.spec
            ?: (_state.value as? RunnerState.Finished)?.spec ?: return
        startedAtMs = System.currentTimeMillis()
        _state.value = RunnerState.Running(spec)
    }

    fun finish(score: Int, passed: Boolean, detailsJson: String = "", micValidated: Boolean = false) {
        val spec = (_state.value as? RunnerState.Running)?.spec ?: return
        _state.value = RunnerState.Saving(spec)
        viewModelScope.launch {
            val summary = repo.recordResult(
                spec = spec,
                score = score.coerceIn(0, 100),
                passed = passed,
                durationMs = System.currentTimeMillis() - startedAtMs,
                detailsJson = detailsJson,
                micValidated = micValidated
            )
            _state.value = RunnerState.Finished(spec, summary, repo.nextRecommended()?.id)
        }
    }

    /** Repetir el mismo ejercicio desde la celebración. */
    fun retry() {
        val spec = (_state.value as? RunnerState.Finished)?.spec ?: return
        startedAtMs = System.currentTimeMillis()
        _state.value = RunnerState.Running(spec)
    }

    /** Encadena el siguiente ejercicio sin salir del runner. */
    fun loadNext(exerciseId: String) {
        val spec = Curriculum.byId(exerciseId) ?: return
        _state.value = RunnerState.Intro(spec)
    }
}
