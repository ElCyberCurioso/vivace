package com.guitarchords.app.tuner

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class TunerEngine {
    private val sampleRate = 22050
    private val windowSize = 2048
    private var recorder: AudioRecord? = null
    private var job: Job? = null

    private val _frequency = MutableStateFlow(0f)
    val frequency: StateFlow<Float> = _frequency

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (recorder != null) return
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(windowSize * 4)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        recorder = rec
        rec.startRecording()
        _running.value = true

        val shortBuf = ShortArray(windowSize)
        val floatBuf = FloatArray(windowSize)

        job = scope.launch(Dispatchers.Default) {
            try {
                while (isActive) {
                    val read = rec.read(shortBuf, 0, windowSize)
                    if (read <= 0) continue
                    var sumSq = 0.0
                    for (i in 0 until read) {
                        val f = shortBuf[i] / 32768f
                        floatBuf[i] = f
                        sumSq += f * f
                    }
                    val rms = sqrt(sumSq / read).toFloat()
                    _level.value = rms
                    if (rms > 0.01f) {
                        val freq = PitchDetector.yin(floatBuf, read, sampleRate)
                        if (freq in 60f..1000f) _frequency.value = freq
                    }
                }
            } finally {
                _running.value = false
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        recorder?.let {
            try { it.stop() } catch (_: Throwable) {}
            it.release()
        }
        recorder = null
        _running.value = false
    }
}
