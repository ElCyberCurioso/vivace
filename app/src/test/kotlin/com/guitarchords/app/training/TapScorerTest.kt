package com.guitarchords.app.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TapScorerTest {

    @Test
    fun `taps perfectos dan 100`() {
        val expected = listOf(0L, 500L, 1000L, 1500L)
        val r = TapScorer.score(expected, expected, toleranceMs = 100)
        assertEquals(100, r.score)
        assertEquals(4, r.hits)
        assertEquals(0, r.extraTaps)
    }

    @Test
    fun `latencia sistematica se compensa con la mediana`() {
        val expected = listOf(0L, 500L, 1000L, 1500L)
        val taps = expected.map { it + 80 }    // 80 ms tarde, regular
        val r = TapScorer.score(expected, taps, toleranceMs = 50)
        assertEquals(100, r.score)             // sin compensación serían todos fallo
        assertEquals(80L, r.medianOffsetMs)
    }

    @Test
    fun `tap fuera de tolerancia no puntua`() {
        val expected = listOf(0L, 500L, 1000L, 1500L)
        val taps = listOf(0L, 500L, 1000L, 1800L)   // último muy tarde
        val r = TapScorer.score(expected, taps, toleranceMs = 100)
        assertEquals(3, r.hits)
        assertTrue(r.score < 100)
    }

    @Test
    fun `taps de mas penalizan`() {
        val expected = listOf(0L, 500L, 1000L, 1500L)
        val taps = listOf(0L, 250L, 500L, 750L, 1000L, 1250L, 1500L)  // dobles entre medias
        val r = TapScorer.score(expected, taps, toleranceMs = 100)
        assertEquals(4, r.hits)
        assertEquals(3, r.extraTaps)
        assertTrue(r.score < 100)
    }

    @Test
    fun `sin taps da 0`() {
        val r = TapScorer.score(listOf(0L, 500L), emptyList(), 100)
        assertEquals(0, r.score)
    }

    @Test
    fun `tiempos esperados de negras`() {
        // 4 golpes por compás, 2 compases a 60 bpm → cada beat 1000 ms
        val t = TapScorer.expectedTimes(listOf(0f, 1f, 2f, 3f), beatsPerBar = 4, bars = 2, bpm = 60)
        assertEquals(listOf(0L, 1000L, 2000L, 3000L, 4000L, 5000L, 6000L, 7000L), t)
    }

    @Test
    fun `tiempos esperados de corcheas`() {
        val t = TapScorer.expectedTimes(listOf(0f, 0.5f, 1f, 1.5f), beatsPerBar = 2, bars = 1, bpm = 120)
        assertEquals(listOf(0L, 250L, 500L, 750L), t)
    }
}
