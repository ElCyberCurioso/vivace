package com.guitarchords.app.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Metrónomo por software: genera los clics sintetizando un seno corto con
 * envolvente y los reproduce con [AudioTrack] en modo estático. El primer
 * pulso de cada compás suena más agudo (acento).
 */
class MetronomeEngine {

    private val sampleRate = 44100
    private var job: Job? = null
    private var accentTrack: AudioTrack? = null
    private var tickTrack: AudioTrack? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    private val _beat = MutableStateFlow(0)   // 1-based; 0 = sin pulso aún
    val beat: StateFlow<Int> = _beat

    var bpm: Int = 100
        set(value) { field = value.coerceIn(30, 240) }

    var beatsPerBar: Int = 4
        set(value) { field = value.coerceIn(1, 12) }

    private fun buildClick(freq: Double): AudioTrack {
        val durMs = 45
        val n = sampleRate * durMs / 1000
        val samples = ShortArray(n)
        for (i in 0 until n) {
            // Envolvente de caída rápida para un "click" seco.
            val env = (1.0 - i.toDouble() / n).let { it * it }
            val v = sin(2.0 * PI * freq * i / sampleRate) * env
            samples[i] = (v * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

    fun start(scope: CoroutineScope) {
        if (_running.value) return
        if (accentTrack == null) accentTrack = buildClick(1760.0)
        if (tickTrack == null) tickTrack = buildClick(1175.0)
        _running.value = true
        _beat.value = 0
        job = scope.launch(Dispatchers.Default) {
            var count = 0
            var next = System.nanoTime()
            while (isActive && _running.value) {
                val current = count % beatsPerBar + 1
                _beat.value = current
                val track = if (current == 1) accentTrack else tickTrack
                track?.let {
                    it.stop()
                    it.reloadStaticData()
                    it.play()
                }
                count++
                val intervalNs = 60_000_000_000L / bpm
                next += intervalNs
                val waitMs = (next - System.nanoTime()) / 1_000_000
                if (waitMs > 0) delay(waitMs) else next = System.nanoTime()
            }
        }
    }

    fun stop() {
        _running.value = false
        job?.cancel()
        job = null
        _beat.value = 0
    }

    fun release() {
        stop()
        accentTrack?.release(); accentTrack = null
        tickTrack?.release(); tickTrack = null
    }
}
