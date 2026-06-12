package com.guitarchords.app.training

import org.junit.Assert.assertEquals
import org.junit.Test

class PlacementTestTest {

    @Test
    fun `umbrales de nivel`() {
        assertEquals(1, PlacementTest.levelForScore(null))
        assertEquals(1, PlacementTest.levelForScore(0))
        assertEquals(1, PlacementTest.levelForScore(39))
        assertEquals(2, PlacementTest.levelForScore(40))
        assertEquals(2, PlacementTest.levelForScore(74))
        assertEquals(3, PlacementTest.levelForScore(75))
        assertEquals(3, PlacementTest.levelForScore(100))
    }

    @Test
    fun `technique hereda el promedio de scales y changes`() {
        val levels = PlacementTest.computeLevels(
            mapOf(
                TrainingArea.SCALES to 80,
                TrainingArea.CHANGES to 70
            )
        )
        assertEquals(3, levels[TrainingArea.SCALES])
        assertEquals(2, levels[TrainingArea.CHANGES])
        assertEquals(3, levels[TrainingArea.TECHNIQUE])   // promedio 75
        assertEquals(1, levels[TrainingArea.EAR])
        assertEquals(1, levels[TrainingArea.CHORDS])      // sin datos
    }

    @Test
    fun `sin puntuaciones todo queda a 1`() {
        val levels = PlacementTest.computeLevels(emptyMap())
        TrainingArea.entries.forEach { assertEquals(1, levels[it]) }
    }

    @Test
    fun `principiante salta directo al resumen`() {
        assertEquals(
            PlacementTest.Step.SUMMARY,
            PlacementTest.nextStep(
                PlacementTest.Step.EXPERIENCE, PlacementTest.Experience.NONE, emptyMap()
            )
        )
    }

    @Test
    fun `fallar acordes salta cambios y escalas`() {
        val scores = mapOf(TrainingArea.CHORDS to 10)
        assertEquals(
            PlacementTest.Step.RHYTHM,
            PlacementTest.nextStep(PlacementTest.Step.CHORDS, PlacementTest.Experience.OVER_2Y, scores)
        )
        assertEquals(
            PlacementTest.Step.SUMMARY,
            PlacementTest.nextStep(PlacementTest.Step.RHYTHM, PlacementTest.Experience.OVER_2Y, scores)
        )
    }

    @Test
    fun `flujo completo con experiencia`() {
        val scores = mapOf(TrainingArea.CHORDS to 70)
        var step = PlacementTest.Step.EXPERIENCE
        val visited = mutableListOf(step)
        repeat(10) {
            step = PlacementTest.nextStep(step, PlacementTest.Experience.OVER_2Y, scores)
            if (visited.last() != step) visited.add(step)
        }
        assertEquals(
            listOf(
                PlacementTest.Step.EXPERIENCE, PlacementTest.Step.THEORY,
                PlacementTest.Step.CHORDS, PlacementTest.Step.CHANGES,
                PlacementTest.Step.RHYTHM, PlacementTest.Step.SCALES,
                PlacementTest.Step.SUMMARY
            ),
            visited
        )
    }
}
