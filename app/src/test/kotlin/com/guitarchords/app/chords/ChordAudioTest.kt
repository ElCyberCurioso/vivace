package com.guitarchords.app.chords

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChordAudioTest {

    @Test
    fun `cuerdas al aire en afinacion estandar`() {
        assertEquals(listOf(40, 45, 50, 55, 59, 64), ChordAudio.midisOf(listOf(0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun `el traste suma semitonos`() {
        assertEquals(45, ChordAudio.midiOf(0, 5))    // Mi grave, traste 5 = La
        assertEquals(64, ChordAudio.midiOf(5, 0))    // Mi agudo al aire
    }

    @Test
    fun `las cuerdas mudas no suenan`() {
        assertNull(ChordAudio.midiOf(0, -1))
        // Do mayor abierto: x32010 → suenan 5 cuerdas.
        val c = ChordAudio.midisOf(listOf(-1, 3, 2, 0, 1, 0))
        assertEquals(listOf(48, 52, 55, 60, 64), c)
    }

    @Test
    fun `una cuerda fuera de rango no rompe`() {
        assertNull(ChordAudio.midiOf(9, 0))
        assertNull(ChordAudio.midiOf(-1, 0))
    }

    @Test
    fun `las notas salen de grave a agudo`() {
        val em = ChordAudio.midisOf(listOf(0, 2, 2, 0, 0, 0))   // Mi menor
        assertEquals(em.sorted(), em)
    }
}
