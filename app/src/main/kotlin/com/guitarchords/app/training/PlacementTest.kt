package com.guitarchords.app.training

import com.guitarchords.app.R

/**
 * Test de nivel inicial: pasos, specs de cada prueba y mapeo de puntuaciones
 * a niveles de partida por área. Los pasos prácticos reutilizan los mismos
 * composables de ejercicio en "modo placement" (sin XP ni pass/fail).
 */
object PlacementTest {

    /** Pasos del wizard, en orden. */
    enum class Step { EXPERIENCE, THEORY, CHORDS, CHANGES, RHYTHM, SCALES, SUMMARY }

    /** Autoinforme inicial. */
    enum class Experience { NONE, UNDER_6M, UNDER_2Y, OVER_2Y }

    // Specs internas del test (ids con prefijo placement_, nunca se persisten
    // como resultados de ejercicio).
    val theorySpec = TheoryQuizSpec(
        id = "placement_theory", area = TrainingArea.THEORY, level = 1, xpBase = 0,
        titleRes = R.string.training_placement_invite_title, descRes = R.string.tr_desc_theory,
        topics = listOf(
            TheoryTopic.LATIN_NOTATION, TheoryTopic.NOTES_OF_CHORD,
            TheoryTopic.INTERVAL_BETWEEN, TheoryTopic.FORMULA_TO_CHORD
        ),
        questionCount = 6
    )

    val chordsSpec = ChordQuizSpec(
        id = "placement_chords", area = TrainingArea.CHORDS, level = 1, xpBase = 0,
        titleRes = R.string.training_area_chords, descRes = R.string.tr_desc_chord_quiz,
        chords = listOf("Em", "F", "Amaj7")
    )

    val changesSpec = ChordChangeSpec(
        id = "placement_changes", area = TrainingArea.CHANGES, level = 1, xpBase = 0,
        titleRes = R.string.training_area_changes, descRes = R.string.tr_desc_change,
        chordA = "Am", chordB = "E", bpm = 60, durationSec = 30, targetChanges = 12
    )

    val rhythmSpec = RhythmTapSpec(
        id = "placement_rhythm", area = TrainingArea.RHYTHM, level = 1, xpBase = 0,
        titleRes = R.string.training_area_rhythm, descRes = R.string.tr_desc_rhythm,
        bpm = 80, pattern = listOf(0f, 1f, 2f, 3f), bars = 4, toleranceMs = 120
    )

    val scalesSpec = ScaleNotesSpec(
        id = "placement_scales", area = TrainingArea.SCALES, level = 1, xpBase = 0,
        titleRes = R.string.training_area_scales, descRes = R.string.tr_desc_scale,
        notes = listOf(
            TargetNote(0, 0, 40), TargetNote(0, 3, 43),
            TargetNote(1, 0, 45), TargetNote(1, 2, 47),
            TargetNote(2, 0, 50)
        )
    )

    /** Nivel de partida que corresponde a una puntuación (null = sin datos). */
    fun levelForScore(score: Int?): Int = when {
        score == null -> 1
        score < 40 -> 1
        score < 75 -> 2
        else -> 3
    }

    /**
     * Mapea las puntuaciones (0..100 por área evaluada) a nivel desbloqueado
     * por área. TECHNIQUE hereda el promedio de SCALES y CHANGES; EAR queda
     * a 1 (sin ejercicios en Fase 1).
     */
    fun computeLevels(scores: Map<TrainingArea, Int>): Map<TrainingArea, Int> {
        val technique = listOfNotNull(scores[TrainingArea.SCALES], scores[TrainingArea.CHANGES])
            .takeIf { it.isNotEmpty() }?.average()?.toInt()
        return mapOf(
            TrainingArea.CHORDS to levelForScore(scores[TrainingArea.CHORDS]),
            TrainingArea.CHANGES to levelForScore(scores[TrainingArea.CHANGES]),
            TrainingArea.RHYTHM to levelForScore(scores[TrainingArea.RHYTHM]),
            TrainingArea.SCALES to levelForScore(scores[TrainingArea.SCALES]),
            TrainingArea.THEORY to levelForScore(scores[TrainingArea.THEORY]),
            TrainingArea.TECHNIQUE to levelForScore(technique),
            TrainingArea.EAR to 1
        )
    }

    /**
     * Pasos a ejecutar según el autoinforme y los resultados parciales:
     * - Principiante total: ninguno (todo a nivel 1).
     * - Si en acordes no marcó ni el primero (score < 30), se saltan cambios
     *   y escalas: no tiene sentido cronometrar cambios que no conoce.
     * - Escalas solo con ≥ 6 meses de experiencia (y es saltable en la UI).
     */
    fun nextStep(current: Step, experience: Experience, scores: Map<TrainingArea, Int>): Step {
        return when (current) {
            Step.EXPERIENCE -> if (experience == Experience.NONE) Step.SUMMARY else Step.THEORY
            Step.THEORY -> Step.CHORDS
            Step.CHORDS ->
                if ((scores[TrainingArea.CHORDS] ?: 0) < 30) Step.RHYTHM else Step.CHANGES
            Step.CHANGES -> Step.RHYTHM
            Step.RHYTHM ->
                if (experience != Experience.UNDER_6M && experience != Experience.NONE &&
                    (scores[TrainingArea.CHORDS] ?: 0) >= 30
                ) Step.SCALES else Step.SUMMARY
            Step.SCALES -> Step.SUMMARY
            Step.SUMMARY -> Step.SUMMARY
        }
    }
}
