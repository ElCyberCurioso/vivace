package com.guitarchords.app.tuner

object PitchDetector {

    private const val THRESHOLD = 0.15f

    fun yin(buf: FloatArray, length: Int, sampleRate: Int): Float {
        val size = length / 2
        if (size < 4) return -1f
        val diff = FloatArray(size)
        for (tau in 1 until size) {
            var sum = 0f
            for (i in 0 until size) {
                val d = buf[i] - buf[i + tau]
                sum += d * d
            }
            diff[tau] = sum
        }
        val cmnd = FloatArray(size)
        cmnd[0] = 1f
        var running = 0f
        for (tau in 1 until size) {
            running += diff[tau]
            cmnd[tau] = if (running == 0f) 1f else diff[tau] * tau / running
        }
        var tauEst = -1
        var tau = 2
        while (tau < size) {
            if (cmnd[tau] < THRESHOLD) {
                while (tau + 1 < size && cmnd[tau + 1] < cmnd[tau]) tau++
                tauEst = tau
                break
            }
            tau++
        }
        if (tauEst == -1) return -1f
        val x0 = if (tauEst > 0) tauEst - 1 else tauEst
        val x2 = if (tauEst + 1 < size) tauEst + 1 else tauEst
        val betterTau: Float = when {
            x0 == tauEst -> if (cmnd[tauEst] <= cmnd[x2]) tauEst.toFloat() else x2.toFloat()
            x2 == tauEst -> if (cmnd[tauEst] <= cmnd[x0]) tauEst.toFloat() else x0.toFloat()
            else -> {
                val s0 = cmnd[x0]; val s1 = cmnd[tauEst]; val s2 = cmnd[x2]
                val denom = 2f * (2f * s1 - s2 - s0)
                if (denom == 0f) tauEst.toFloat() else tauEst + (s2 - s0) / denom
            }
        }
        if (betterTau <= 0f) return -1f
        return sampleRate / betterTau
    }
}
