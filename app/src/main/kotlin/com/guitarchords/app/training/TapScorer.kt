package com.guitarchords.app.training

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Puntúa una tanda de taps contra los tiempos esperados del patrón.
 *
 * La latencia táctil/de audio varía por dispositivo (±40-100 ms), así que se
 * estima un offset sistemático (mediana de las desviaciones) y se puntúa la
 * desviación RESPECTO A ESA MEDIANA: se premia la regularidad, no la
 * posición absoluta.
 */
object TapScorer {

    data class Result(
        val score: Int,          // 0..100
        val hits: Int,
        val total: Int,
        val extraTaps: Int,
        val medianOffsetMs: Long
    )

    fun score(expectedMs: List<Long>, tapsMs: List<Long>, toleranceMs: Int): Result {
        if (expectedMs.isEmpty()) return Result(0, 0, 0, tapsMs.size, 0)
        if (tapsMs.isEmpty()) return Result(0, 0, expectedMs.size, 0, 0)

        // Offset sistemático: mediana de la desviación de cada tap a su
        // golpe esperado más cercano.
        val deltas = tapsMs.map { tap -> tap - expectedMs.minBy { abs(it - tap) } }.sorted()
        val median = deltas[deltas.size / 2]

        // Asignación golosa: cada golpe esperado toma el tap libre más
        // cercano (corregido por el offset) si entra en tolerancia.
        val used = BooleanArray(tapsMs.size)
        var hits = 0
        for (expected in expectedMs) {
            var bestIdx = -1
            var bestDelta = Long.MAX_VALUE
            for (i in tapsMs.indices) {
                if (used[i]) continue
                val d = abs(tapsMs[i] - median - expected)
                if (d < bestDelta) {
                    bestDelta = d
                    bestIdx = i
                }
            }
            if (bestIdx >= 0 && bestDelta <= toleranceMs) {
                used[bestIdx] = true
                hits++
            }
        }
        val extra = used.count { !it }
        // Los taps sobrantes (golpes de más) restan medio acierto cada uno.
        val effective = max(0f, hits - extra * 0.5f)
        val score = (effective * 100f / expectedMs.size).roundToInt().coerceIn(0, 100)
        return Result(score, hits, expectedMs.size, extra, median)
    }

    /**
     * Tiempos esperados (ms desde el primer beat puntuable) de un patrón de
     * compás repetido [bars] veces a [bpm]. [pattern] son posiciones en
     * beats dentro del compás (0f, 1f… negras; 0.5f… corcheas).
     */
    fun expectedTimes(pattern: List<Float>, beatsPerBar: Int, bars: Int, bpm: Int): List<Long> {
        val beatMs = 60_000f / bpm
        return buildList {
            repeat(bars) { bar ->
                for (pos in pattern) {
                    add(((bar * beatsPerBar + pos) * beatMs).toLong())
                }
            }
        }
    }
}
