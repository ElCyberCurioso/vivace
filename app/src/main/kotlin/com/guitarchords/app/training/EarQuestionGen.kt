package com.guitarchords.app.training

import kotlin.random.Random

/**
 * Una pregunta de oído ya resuelta: qué notas MIDI hay que tocar, si suenan
 * juntas o seguidas, y cuál de las [options] es la correcta.
 */
data class EarQuestion(
    val midis: List<Int>,
    val together: Boolean,
    /** Valores de opción (semitonos del intervalo, o calidad de acorde). */
    val options: List<Int>,
    val answer: Int
)

/**
 * Generador de preguntas de oído. Puro y determinista con [Random] semillado,
 * para poder testearlo igual que [TheoryQuestionGen].
 */
object EarQuestionGen {

    /** Calidades usadas en [EarMode.CHORD_QUALITY], por su tercera. */
    const val MAJOR = 0
    const val MINOR = 1

    /** Rango cómodo de escucha (La2–Mi5); deja hueco para saltos de una octava. */
    private const val LOW = 45
    private const val HIGH = 76

    /**
     * Nota de partida que deja sitio para moverse [margin] semitonos sin salirse
     * del rango; si el margen no cabe, se usa el centro (nunca un rango vacío).
     */
    private fun rootFor(margin: Int, random: Random): Int {
        val lo = LOW + margin
        val hi = HIGH - margin
        return if (lo < hi) random.nextInt(lo, hi) else (LOW + HIGH) / 2
    }

    fun generate(spec: EarTrainingSpec, random: Random): List<EarQuestion> =
        List(spec.questionCount) { question(spec, random) }

    private fun question(spec: EarTrainingSpec, random: Random): EarQuestion = when (spec.mode) {
        EarMode.INTERVAL -> {
            val semis = spec.choices.random(random)
            val root = rootFor(semis, random)
            EarQuestion(
                midis = listOf(root, root + semis),
                together = false,
                options = spec.choices,
                answer = semis
            )
        }
        EarMode.CHORD_QUALITY -> {
            val quality = spec.choices.random(random)
            val root = rootFor(7, random)   // hueco para la quinta
            val third = if (quality == MINOR) 3 else 4
            EarQuestion(
                midis = listOf(root, root + third, root + 7),
                together = true,
                options = spec.choices,
                answer = quality
            )
        }
        EarMode.DIRECTION -> {
            // +1 sube, -1 baja; el salto se elige entre las opciones de semitonos.
            val up = random.nextBoolean()
            val jump = spec.choices.map { kotlin.math.abs(it) }.filter { it > 0 }
                .ifEmpty { listOf(5) }.random(random)
            val root = rootFor(jump, random)   // cabe subir y bajar el salto
            EarQuestion(
                midis = listOf(root, if (up) root + jump else root - jump),
                together = false,
                options = listOf(1, -1),
                answer = if (up) 1 else -1
            )
        }
    }
}
