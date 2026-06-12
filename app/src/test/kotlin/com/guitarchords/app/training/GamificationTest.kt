package com.guitarchords.app.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationTest {

    @Test
    fun `xp base por nivel de dificultad`() {
        assertEquals(10, Gamification.xpBaseForLevel(1))
        assertEquals(20, Gamification.xpBaseForLevel(2))
        assertEquals(35, Gamification.xpBaseForLevel(3))
    }

    @Test
    fun `primer clear duplica la xp`() {
        val xp = Gamification.xpForAttempt(10, 100, passed = true, firstClear = true, alreadyPassed = false)
        assertEquals(20, xp)
    }

    @Test
    fun `repaso de ejercicio superado da la mitad`() {
        val xp = Gamification.xpForAttempt(10, 100, passed = true, firstClear = false, alreadyPassed = true)
        assertEquals(5, xp)
    }

    @Test
    fun `fallar da el 25 por ciento de la base`() {
        assertEquals(3, Gamification.xpForAttempt(10, 30, passed = false, firstClear = false, alreadyPassed = false))
        assertEquals(9, Gamification.xpForAttempt(35, 0, passed = false, firstClear = false, alreadyPassed = false))
    }

    @Test
    fun `score bajo acota el factor a 0_5`() {
        // passed con score 10 → factor 0.5
        assertEquals(5, Gamification.xpForAttempt(10, 10, passed = true, firstClear = false, alreadyPassed = false))
    }

    @Test
    fun `curva de niveles`() {
        assertEquals(0, Gamification.xpToReachLevel(1))
        assertEquals(300, Gamification.xpToReachLevel(2))
        assertEquals(600, Gamification.xpToReachLevel(3))
        assertEquals(1500, Gamification.xpToReachLevel(5))
        assertEquals(5500, Gamification.xpToReachLevel(10))

        assertEquals(1, Gamification.levelForXp(0))
        assertEquals(1, Gamification.levelForXp(299))
        assertEquals(2, Gamification.levelForXp(300))
        assertEquals(2, Gamification.levelForXp(599))
        assertEquals(3, Gamification.levelForXp(600))
        assertEquals(10, Gamification.levelForXp(5500))
    }

    @Test
    fun `progreso dentro del nivel`() {
        assertEquals(0f, Gamification.progressInLevel(0), 0.001f)
        assertEquals(0.5f, Gamification.progressInLevel(150), 0.001f)   // mitad de 0..300
        assertEquals(0f, Gamification.progressInLevel(300), 0.001f)    // recién subido a N2
    }

    @Test
    fun `racha en el mismo dia no cambia`() {
        val s = Gamification.updateStreak(current = 3, best = 5, lastPracticeDay = 100, todayEpochDay = 100)
        assertEquals(3, s.current)
        assertEquals(5, s.best)
        assertFalse(s.extended)
    }

    @Test
    fun `racha al dia siguiente suma`() {
        val s = Gamification.updateStreak(current = 3, best = 3, lastPracticeDay = 100, todayEpochDay = 101)
        assertEquals(4, s.current)
        assertEquals(4, s.best)
        assertTrue(s.extended)
    }

    @Test
    fun `racha rota se reinicia a 1`() {
        val s = Gamification.updateStreak(current = 7, best = 9, lastPracticeDay = 100, todayEpochDay = 103)
        assertEquals(1, s.current)
        assertEquals(9, s.best)
        assertTrue(s.extended)
    }

    @Test
    fun `primera practica arranca la racha`() {
        val s = Gamification.updateStreak(current = 0, best = 0, lastPracticeDay = 0, todayEpochDay = 20_000)
        assertEquals(1, s.current)
        assertEquals(1, s.best)
    }
}
