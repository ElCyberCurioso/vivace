package com.guitarchords.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.guitarchords.app.chords.ChordAudio
import com.guitarchords.app.training.ToneEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Reproductor de acordes para las pantallas donde se consultan digitaciones
 * (modal de acorde, diccionario y buscador): rasguea la posición completa o
 * suena una cuerda concreta.
 *
 * Cada nueva reproducción cancela la anterior, así pulsar varias cuerdas
 * seguidas no acumula sonidos.
 */
@Stable
class ChordPlayerState internal constructor(
    private val tone: ToneEngine,
    private val scope: kotlinx.coroutines.CoroutineScope
) {
    private var job: Job? = null

    /** Rasguea la digitación completa (ignora las cuerdas mudas). */
    fun strum(frets: List<Int>) {
        val midis = ChordAudio.midisOf(frets)
        if (midis.isEmpty()) return
        job?.cancel()
        job = scope.launch { tone.playStrum(midis) }
    }

    /** Toca una sola cuerda, si no está muda. */
    fun pluck(stringIdx: Int, frets: List<Int>) {
        val fret = frets.getOrNull(stringIdx) ?: return
        val midi = ChordAudio.midiOf(stringIdx, fret) ?: return
        job?.cancel()
        job = scope.launch { tone.play(midi, durationMs = 1100) }
    }

    internal fun stop() { job?.cancel() }
}

@Composable
fun rememberChordPlayer(): ChordPlayerState {
    val tone = remember { ToneEngine() }
    val scope = rememberCoroutineScope()
    val state = remember(tone, scope) { ChordPlayerState(tone, scope) }
    DisposableEffect(state) { onDispose { state.stop() } }
    return state
}
