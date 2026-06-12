package com.guitarchords.app.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.training.PlacementTest
import com.guitarchords.app.training.TrainingArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlacementViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GuitarChordsApp).trainingRepo

    private val _step = MutableStateFlow(PlacementTest.Step.EXPERIENCE)
    val step = _step.asStateFlow()

    private val scores = mutableMapOf<TrainingArea, Int>()
    private var experience = PlacementTest.Experience.UNDER_6M

    val computedLevels: Map<TrainingArea, Int>
        get() = PlacementTest.computeLevels(scores)

    fun chooseExperience(e: PlacementTest.Experience) {
        experience = e
        advance()
    }

    /** Resultado de un paso práctico (modo placement: solo guarda el score). */
    fun stepDone(area: TrainingArea, score: Int) {
        scores[area] = score
        advance()
    }

    /** Salta el paso actual sin puntuar (p. ej. escalas sin micrófono). */
    fun skipStep() = advance()

    private fun advance() {
        _step.value = PlacementTest.nextStep(_step.value, experience, scores)
    }

    /** Persiste los niveles calculados y marca el test como hecho. */
    fun confirm(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.completePlacement(computedLevels)
            onDone()
        }
    }
}
