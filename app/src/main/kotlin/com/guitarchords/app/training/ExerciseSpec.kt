package com.guitarchords.app.training

/**
 * Definición estática de un ejercicio del curriculum (en código, no en DB).
 * La DB solo persiste resultados referenciando [id], que debe ser estable
 * entre versiones: prefijo de área + nivel + slug ("chords_l1_open_minor").
 */
sealed interface ExerciseSpec {
    val id: String
    val area: TrainingArea
    /** Nivel de dificultad 1..3 en Fase 1. */
    val level: Int
    /** XP base del ejercicio (típicamente 10/20/35 según nivel). */
    val xpBase: Int
    /** Recurso de string del título (plantilla, puede llevar [titleArgs]). */
    val titleRes: Int
    val titleArgs: List<String>
    /** Recurso de string con la descripción/instrucciones. */
    val descRes: Int
}

/** Quiz de digitación: marcar cada acorde en el mástil táctil. */
data class ChordQuizSpec(
    override val id: String,
    override val area: TrainingArea,
    override val level: Int,
    override val xpBase: Int,
    override val titleRes: Int,
    override val titleArgs: List<String> = emptyList(),
    override val descRes: Int,
    /** Nombres de acorde a pedir, p. ej. ["Em", "Am", "C"]. */
    val chords: List<String>,
    val passPct: Int = 70,
    /** N1-2 exigen posición fundamental (bajo = raíz); N3 acepta inversiones. */
    val requireRootBass: Boolean = true
) : ExerciseSpec

/** Cambios entre dos acordes contra metrónomo, con conteo autoevaluado. */
data class ChordChangeSpec(
    override val id: String,
    override val area: TrainingArea,
    override val level: Int,
    override val xpBase: Int,
    override val titleRes: Int,
    override val titleArgs: List<String> = emptyList(),
    override val descRes: Int,
    val chordA: String,
    val chordB: String,
    val bpm: Int,
    val durationSec: Int = 60,
    /** Cambios limpios para el 100 % (método "one-minute changes"). */
    val targetChanges: Int
) : ExerciseSpec

/** Una nota objetivo sobre el mástil. [midi] = nota MIDI absoluta. */
data class TargetNote(val stringIdx: Int, val fret: Int, val midi: Int)

/** Escala/lick nota a nota validado con el detector de pitch (tempo libre). */
data class ScaleNotesSpec(
    override val id: String,
    override val area: TrainingArea,
    override val level: Int,
    override val xpBase: Int,
    override val titleRes: Int,
    override val titleArgs: List<String> = emptyList(),
    override val descRes: Int,
    val notes: List<TargetNote>,
    /** false = compara solo pitch-class (tolera errores de octava del detector). */
    val matchOctave: Boolean = false,
    val passPct: Int = 70
) : ExerciseSpec

/** Temas que puede cubrir el generador de preguntas de teoría. */
enum class TheoryTopic { NOTES_OF_CHORD, INTERVAL_BETWEEN, FORMULA_TO_CHORD, CHORD_TO_FORMULA, LATIN_NOTATION }

/** Quiz de teoría autogenerado desde MusicTheory. */
data class TheoryQuizSpec(
    override val id: String,
    override val area: TrainingArea,
    override val level: Int,
    override val xpBase: Int,
    override val titleRes: Int,
    override val titleArgs: List<String> = emptyList(),
    override val descRes: Int,
    val topics: List<TheoryTopic>,
    val questionCount: Int = 8,
    val passPct: Int = 70,
    /** Acordes/raíces candidatos acotados por nivel (vacío = todos). */
    val qualities: List<String> = emptyList()
) : ExerciseSpec

/** Golpes sobre el patrón del compás contra metrónomo. */
data class RhythmTapSpec(
    override val id: String,
    override val area: TrainingArea,
    override val level: Int,
    override val xpBase: Int,
    override val titleRes: Int,
    override val titleArgs: List<String> = emptyList(),
    override val descRes: Int,
    val bpm: Int,
    val beatsPerBar: Int = 4,
    val bars: Int = 8,
    /** Posiciones de golpe dentro del compás en beats: 0f,1f,2f,3f = negras; 0f,0.5f… = corcheas. */
    val pattern: List<Float>,
    val toleranceMs: Int,
    val passPct: Int = 70
) : ExerciseSpec
