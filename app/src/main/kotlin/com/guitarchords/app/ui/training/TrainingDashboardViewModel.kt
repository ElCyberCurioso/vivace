package com.guitarchords.app.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guitarchords.app.GuitarChordsApp
import com.guitarchords.app.data.AreaProgress
import com.guitarchords.app.data.BestResult
import com.guitarchords.app.training.Curriculum
import com.guitarchords.app.training.ExerciseSpec
import com.guitarchords.app.training.TrainingArea
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrainingDashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as GuitarChordsApp).trainingRepo

    val profile = repo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val areas = repo.areas
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bestResults = repo.bestResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recommended = combine(repo.areas, repo.bestResults) { areas, bests ->
        Curriculum.recommended(unlockedLevels(areas), passedIds(bests))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- estadísticas (StatsScreen comparte este VM) ----
    val areaStats = repo.areaStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val achievements = repo.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentResults = repo.recentResults(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Progreso del nivel desbloqueado de un área para su AreaCard. */
    fun areaCardData(
        area: TrainingArea,
        areas: List<AreaProgress>,
        bests: List<BestResult>
    ): Triple<Int, Int, Int> {
        val unlocked = areas.firstOrNull { it.area == area.name }?.unlockedLevel ?: 1
        val specs = Curriculum.levelExercises(area, unlocked)
        val passed = passedIds(bests)
        return Triple(unlocked, specs.count { it.id in passed }, specs.size)
    }

    /** "Soy principiante total": salta el test dejando todo a nivel 1. */
    fun skipPlacement() {
        viewModelScope.launch { repo.completePlacement(emptyMap()) }
    }

    companion object {
        fun unlockedLevels(areas: List<AreaProgress>): Map<TrainingArea, Int> =
            TrainingArea.entries.associateWith { area ->
                areas.firstOrNull { it.area == area.name }?.unlockedLevel ?: 1
            }

        fun passedIds(bests: List<BestResult>): Set<String> =
            bests.filter { it.passed }.mapTo(mutableSetOf()) { it.exerciseId }
    }
}

/** ¿Tiene el área ejercicios en el curriculum actual? */
fun TrainingArea.hasContent(): Boolean = Curriculum.byArea(this).isNotEmpty()

/** Spec accesible para la card de recomendado. */
typealias RecommendedSpec = ExerciseSpec?
