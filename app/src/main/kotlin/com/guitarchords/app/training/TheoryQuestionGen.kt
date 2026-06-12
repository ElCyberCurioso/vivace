package com.guitarchords.app.training

import com.guitarchords.app.chords.MusicTheory
import kotlin.random.Random

/**
 * Pregunta de teoría autogenerada. El enunciado se construye en la UI a
 * partir de [topic] (plantilla de string) y [args]; el dominio no toca
 * recursos Android para seguir siendo testeable en JVM puro.
 */
data class TheoryQuestion(
    val topic: TheoryTopic,
    val args: List<String>,
    val options: List<String>,
    val correctIndex: Int
)

/** Genera preguntas desde los datos de [MusicTheory] (sin contenido manual). */
object TheoryQuestionGen {

    /** Calidades por defecto cuando el spec no acota (las de tríadas/séptimas comunes). */
    private val DEFAULT_QUALITIES = listOf("", "m", "7", "maj7", "m7", "dim", "aug", "sus2", "sus4")

    fun generate(spec: TheoryQuizSpec, seed: Long): List<TheoryQuestion> {
        val rng = Random(seed)
        val qualities = spec.qualities.ifEmpty { DEFAULT_QUALITIES }
            .filter { q -> MusicTheory.FORMULAS.any { it.quality == q } }
            .ifEmpty { DEFAULT_QUALITIES }
        return List(spec.questionCount) {
            when (spec.topics[rng.nextInt(spec.topics.size)]) {
                TheoryTopic.NOTES_OF_CHORD -> notesOfChord(rng, qualities)
                TheoryTopic.INTERVAL_BETWEEN -> intervalBetween(rng)
                TheoryTopic.FORMULA_TO_CHORD -> formulaToChord(rng, qualities)
                TheoryTopic.CHORD_TO_FORMULA -> chordToFormula(rng, qualities)
                TheoryTopic.LATIN_NOTATION -> latinNotation(rng)
            }
        }
    }

    private fun formulaFor(quality: String): MusicTheory.ChordFormula =
        MusicTheory.FORMULAS.first { it.quality == quality }

    private fun chordName(rootIdx: Int, quality: String): String =
        MusicTheory.NOTES[rootIdx] + quality

    private fun notesOfChord(rng: Random, qualities: List<String>): TheoryQuestion {
        val rootIdx = rng.nextInt(12)
        val quality = qualities[rng.nextInt(qualities.size)]
        val formula = formulaFor(quality)
        val correct = MusicTheory.notesFor(rootIdx, formula.semitones).joinToString(" · ")
        // Distractores: las notas del mismo acorde desde otras raíces.
        val options = buildOptions(rng, correct) {
            val r = rng.nextInt(12)
            MusicTheory.notesFor(r, formula.semitones).joinToString(" · ")
        }
        return TheoryQuestion(
            TheoryTopic.NOTES_OF_CHORD,
            args = listOf(chordName(rootIdx, quality)),
            options = options.first,
            correctIndex = options.second
        )
    }

    private fun intervalBetween(rng: Random): TheoryQuestion {
        val fromIdx = rng.nextInt(12)
        val semis = 1 + rng.nextInt(11)              // 1..11, sin unísono ni octava
        val toIdx = (fromIdx + semis) % 12
        val correct = MusicTheory.INTERVALS.first { it.semitones == semis }.name
        val options = buildOptions(rng, correct) {
            MusicTheory.INTERVALS[1 + rng.nextInt(11)].name
        }
        return TheoryQuestion(
            TheoryTopic.INTERVAL_BETWEEN,
            args = listOf(MusicTheory.NOTES[fromIdx], MusicTheory.NOTES[toIdx]),
            options = options.first,
            correctIndex = options.second
        )
    }

    private fun formulaToChord(rng: Random, qualities: List<String>): TheoryQuestion {
        val quality = qualities[rng.nextInt(qualities.size)]
        val formula = formulaFor(quality)
        val options = buildOptions(rng, formula.displayName) {
            formulaFor(qualities[rng.nextInt(qualities.size)]).displayName
        }
        return TheoryQuestion(
            TheoryTopic.FORMULA_TO_CHORD,
            args = listOf(formula.degrees),
            options = options.first,
            correctIndex = options.second
        )
    }

    private fun chordToFormula(rng: Random, qualities: List<String>): TheoryQuestion {
        val quality = qualities[rng.nextInt(qualities.size)]
        val formula = formulaFor(quality)
        val options = buildOptions(rng, formula.degrees) {
            formulaFor(qualities[rng.nextInt(qualities.size)]).degrees
        }
        return TheoryQuestion(
            TheoryTopic.CHORD_TO_FORMULA,
            args = listOf(formula.displayName),
            options = options.first,
            correctIndex = options.second
        )
    }

    private fun latinNotation(rng: Random): TheoryQuestion {
        val pair = MusicTheory.LETTER_TO_LATIN[rng.nextInt(MusicTheory.LETTER_TO_LATIN.size)]
        val correct = pair.second
        val options = buildOptions(rng, correct) {
            MusicTheory.LETTER_TO_LATIN[rng.nextInt(MusicTheory.LETTER_TO_LATIN.size)].second
        }
        return TheoryQuestion(
            TheoryTopic.LATIN_NOTATION,
            args = listOf(pair.first),
            options = options.first,
            correctIndex = options.second
        )
    }

    /**
     * 4 opciones únicas (la correcta + 3 distractores de [candidate]),
     * barajadas. Devuelve (opciones, índice de la correcta).
     */
    private fun buildOptions(
        rng: Random,
        correct: String,
        candidate: () -> String
    ): Pair<List<String>, Int> {
        val set = linkedSetOf(correct)
        var guard = 0
        while (set.size < 4 && guard++ < 200) set.add(candidate())
        val options = set.toMutableList()
        // Si el banco no da para 4 distractores únicos, rellena variando texto.
        while (options.size < 4) options.add(options.last() + " ")
        options.shuffle(rng)
        return options to options.indexOf(correct)
    }
}
