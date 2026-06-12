package com.guitarchords.app.training

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reglas puras de XP, niveles de usuario y racha diaria. Sin estado: el
 * nivel se deriva siempre de la XP total (única fuente de verdad).
 */
object Gamification {

    /** XP base recomendada por nivel de dificultad del ejercicio (1..3). */
    fun xpBaseForLevel(level: Int): Int = when (level.coerceIn(1, 3)) {
        1 -> 10
        2 -> 20
        else -> 35
    }

    /**
     * XP de un intento.
     * - Superado: base × max(0.5, score/100); ×2 la primera vez que se supera
     *   ese ejercicio; ×0.5 si ya estaba superado (repaso).
     * - No superado: 25 % de la base — el esfuerzo cuenta.
     */
    fun xpForAttempt(
        xpBase: Int,
        score: Int,
        passed: Boolean,
        firstClear: Boolean,
        alreadyPassed: Boolean
    ): Int {
        if (!passed) return max(1, (xpBase * 0.25f).roundToInt())
        var xp = xpBase * max(0.5f, score.coerceIn(0, 100) / 100f)
        if (firstClear) xp *= 2f
        else if (alreadyPassed) xp *= 0.5f
        return max(1, xp.roundToInt())
    }

    /** XP total acumulada necesaria para ALCANZAR el nivel [level]. */
    fun xpToReachLevel(level: Int): Long =
        if (level <= 1) 0 else 50L * level * (level + 1)   // N2=300, N3=600, N5=1500, N10=5500

    /** Nivel de usuario (≥1) que corresponde a una XP total. */
    fun levelForXp(xp: Long): Int {
        var level = 1
        while (xpToReachLevel(level + 1) <= xp) level++
        return level
    }

    /** Progreso 0..1 dentro del nivel actual (para la barra de XP). */
    fun progressInLevel(xp: Long): Float {
        val level = levelForXp(xp)
        val floor = xpToReachLevel(level)
        val ceil = xpToReachLevel(level + 1)
        return ((xp - floor).toFloat() / (ceil - floor)).coerceIn(0f, 1f)
    }

    data class StreakUpdate(val current: Int, val best: Int, val extended: Boolean)

    /**
     * Actualiza la racha al registrar práctica en [todayEpochDay].
     * Mismo día → sin cambio; día siguiente → +1; salto → reinicia a 1.
     * Cualquier ejercicio completado (aunque se falle) mantiene la racha.
     */
    fun updateStreak(current: Int, best: Int, lastPracticeDay: Long, todayEpochDay: Long): StreakUpdate {
        val newCurrent = when {
            lastPracticeDay == todayEpochDay -> current
            lastPracticeDay == todayEpochDay - 1 && current > 0 -> current + 1
            else -> 1
        }
        return StreakUpdate(
            current = newCurrent,
            best = max(best, newCurrent),
            extended = newCurrent != current || lastPracticeDay != todayEpochDay
        )
    }
}
