package com.guitarchords.app.training

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Sintetiza notas para los ejercicios de oído. Genera un timbre con algunos
 * armónicos y una envolvente tipo cuerda pulsada (ataque corto, caída suave),
 * más cercano a una guitarra que un seno puro y mucho más fácil de reconocer.
 *
 * No depende de ficheros de audio: todo se calcula en memoria, igual que hace
 * [com.guitarchords.app.metronome.MetronomeEngine] con los clics.
 */
class ToneEngine {

    private val sampleRate = 44100

    /** Reproduce [midi] durante [durationMs] y espera a que termine. */
    suspend fun play(midi: Int, durationMs: Int = 900) {
        playTogether(listOf(midi), durationMs)
    }

    /** Reproduce varias notas a la vez (acorde) y espera a que termine. */
    suspend fun playTogether(midis: List<Int>, durationMs: Int = 1200) {
        if (midis.isEmpty()) return
        val track = buildTrack(midis, durationMs)
        try {
            track.play()
            delay(durationMs.toLong())
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** Reproduce notas una detrás de otra (intervalo o arpegio). */
    suspend fun playSequence(midis: List<Int>, noteMs: Int = 750, gapMs: Int = 120) {
        for (m in midis) {
            play(m, noteMs)
            if (gapMs > 0) delay(gapMs.toLong())
        }
    }

    /**
     * Rasguea las notas: entran escalonadas [strumMs] milisegundos pero siguen
     * sonando juntas, que es como suena una guitarra al pasar la púa.
     */
    suspend fun playStrum(midis: List<Int>, strumMs: Int = 32, durationMs: Int = 1800) {
        if (midis.isEmpty()) return
        val track = buildTrack(midis, durationMs, strumMs)
        try {
            track.play()
            delay(durationMs.toLong())
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /**
     * Sintetiza [midis] en un único buffer. Con [strumMs] > 0 cada nota entra
     * escalonada (rasgueo); con 0 suenan todas a la vez (acorde en bloque).
     */
    private fun buildTrack(midis: List<Int>, durationMs: Int, strumMs: Int = 0): AudioTrack {
        val n = sampleRate * durationMs / 1000
        val samples = ShortArray(n)
        // Armónicos: fundamental + 2.º y 3.º más suaves (timbre con cuerpo).
        val partials = listOf(1.0 to 1.0, 2.0 to 0.35, 3.0 to 0.15)
        val onsets = midis.indices.map { it * strumMs * sampleRate / 1000 }
        for (i in 0 until n) {
            var v = 0.0
            for ((idx, midi) in midis.withIndex()) {
                val start = onsets[idx]
                if (i < start) continue                    // esta cuerda aún no ha sonado
                val t = (i - start).toDouble() / sampleRate
                val progress = (i - start).toDouble() / (n - start).coerceAtLeast(1)
                // Ataque rápido (5 ms) y caída exponencial, como una cuerda.
                val attack = (t / 0.005).coerceAtMost(1.0)
                val decay = exp(-3.0 * progress)
                val f0 = midiToHz(midi)
                var note = 0.0
                for ((mult, amp) in partials) note += sin(2.0 * PI * f0 * mult * t) * amp
                v += note * attack * decay
            }
            // Normaliza por nº de notas para que un acorde no sature.
            v /= (midis.size * 1.5)
            samples[i] = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            samples.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(samples, 0, samples.size)
        return track
    }

    private companion object {
        fun midiToHz(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)
    }
}
