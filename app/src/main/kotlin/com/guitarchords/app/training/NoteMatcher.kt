package com.guitarchords.app.training

import kotlin.math.abs
import kotlin.math.log2

/**
 * Valida notas sueltas a partir de la frecuencia del detector YIN (ventanas
 * de ~93 ms del TunerEngine). Una nota se da por buena cuando
 * [windowsNeeded] ventanas consecutivas caen dentro de [toleranceCents] del
 * objetivo. Con [matchOctave]=false compara solo pitch-class (los armónicos
 * de las cuerdas graves provocan errores de octava en YIN).
 *
 * Histéresis anti-doble-conteo: tras validar una nota, no se vuelve a contar
 * hasta que la señal cae por debajo del gate o el pitch se aleja de la nota
 * recién validada (evita que una cuerda que sigue sonando valide la
 * siguiente nota igual).
 */
class NoteMatcher(
    private val matchOctave: Boolean = false,
    private val toleranceCents: Float = 50f,
    private val windowsNeeded: Int = 3,
    private val levelGate: Float = 0.01f
) {
    private var consecutive = 0
    private var armed = true
    private var lastMatchedMidi = Int.MIN_VALUE

    /** Desviación en cents de [frequencyHz] respecto a [targetMidi]. */
    private fun centsOff(frequencyHz: Float, targetMidi: Int): Float {
        val midi = 69.0 + 12.0 * log2(frequencyHz / 440.0)
        var d = midi - targetMidi
        if (!matchOctave) {
            d %= 12.0
            if (d > 6) d -= 12.0
            if (d < -6) d += 12.0
        }
        return (abs(d) * 100.0).toFloat()
    }

    /**
     * Procesa una ventana del detector. Devuelve true cuando la nota
     * [targetMidi] queda validada (y queda desarmado hasta la histéresis).
     */
    fun process(frequencyHz: Float, level: Float, targetMidi: Int): Boolean {
        val hasSignal = frequencyHz > 0f && level >= levelGate
        if (!armed) {
            // Re-arma con silencio o cuando el pitch deja la nota anterior.
            val stillOnPrevious = hasSignal &&
                lastMatchedMidi != Int.MIN_VALUE &&
                centsOff(frequencyHz, lastMatchedMidi) <= toleranceCents
            if (stillOnPrevious) return false
            armed = true
        }
        if (hasSignal && centsOff(frequencyHz, targetMidi) <= toleranceCents) {
            consecutive++
            if (consecutive >= windowsNeeded) {
                consecutive = 0
                armed = false
                lastMatchedMidi = targetMidi
                return true
            }
        } else {
            consecutive = 0
        }
        return false
    }

    /** Reinicia el estado (p. ej. al saltar una nota manualmente). */
    fun reset() {
        consecutive = 0
        armed = true
        lastMatchedMidi = Int.MIN_VALUE
    }
}
