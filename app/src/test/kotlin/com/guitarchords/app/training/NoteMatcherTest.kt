package com.guitarchords.app.training

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMatcherTest {

    /** Frecuencia de una nota MIDI. */
    private fun hz(midi: Int): Float =
        (440.0 * Math.pow(2.0, (midi - 69) / 12.0)).toFloat()

    @Test
    fun `tres ventanas consecutivas validan la nota`() {
        val m = NoteMatcher(matchOctave = true)
        val a4 = hz(69)
        assertFalse(m.process(a4, 0.5f, 69))
        assertFalse(m.process(a4, 0.5f, 69))
        assertTrue(m.process(a4, 0.5f, 69))
    }

    @Test
    fun `una ventana desafinada reinicia el contador`() {
        val m = NoteMatcher(matchOctave = true)
        val a4 = hz(69)
        assertFalse(m.process(a4, 0.5f, 69))
        assertFalse(m.process(hz(71), 0.5f, 69))   // se va de nota
        assertFalse(m.process(a4, 0.5f, 69))
        assertFalse(m.process(a4, 0.5f, 69))
        assertTrue(m.process(a4, 0.5f, 69))
    }

    @Test
    fun `senal por debajo del gate no cuenta`() {
        val m = NoteMatcher(matchOctave = true, levelGate = 0.05f)
        val a4 = hz(69)
        repeat(5) { assertFalse(m.process(a4, 0.01f, 69)) }
    }

    @Test
    fun `sin octava acepta el error de octava tipico de yin`() {
        val m = NoteMatcher(matchOctave = false)
        val e3 = hz(52)     // detector da E3 cuando se toca E2 (2º armónico)
        assertFalse(m.process(e3, 0.5f, 40))
        assertFalse(m.process(e3, 0.5f, 40))
        assertTrue(m.process(e3, 0.5f, 40))   // targetMidi E2=40, pitch-class coincide
    }

    @Test
    fun `con octava exigida el armonico no valida`() {
        val m = NoteMatcher(matchOctave = true)
        val e3 = hz(52)
        repeat(5) { assertFalse(m.process(e3, 0.5f, 40)) }
    }

    @Test
    fun `histeresis impide validar dos veces con la cuerda sonando`() {
        val m = NoteMatcher(matchOctave = true)
        val a4 = hz(69)
        repeat(2) { m.process(a4, 0.5f, 69) }
        assertTrue(m.process(a4, 0.5f, 69))           // validada
        // La misma nota sigue sonando y el siguiente objetivo es también A4:
        repeat(5) { assertFalse(m.process(a4, 0.5f, 69)) }
        // Silencio → re-arma → puede validar de nuevo
        assertFalse(m.process(0f, 0f, 69))
        assertFalse(m.process(a4, 0.5f, 69))
        assertFalse(m.process(a4, 0.5f, 69))
        assertTrue(m.process(a4, 0.5f, 69))
    }

    @Test
    fun `histeresis no bloquea una nota objetivo distinta`() {
        val m = NoteMatcher(matchOctave = true)
        val a4 = hz(69)
        val b4 = hz(71)
        repeat(2) { m.process(a4, 0.5f, 69) }
        assertTrue(m.process(a4, 0.5f, 69))           // A4 validada
        // El usuario pasa a B4 sin silencio: pitch deja la nota anterior → re-arma
        assertFalse(m.process(b4, 0.5f, 71))
        assertFalse(m.process(b4, 0.5f, 71))
        assertTrue(m.process(b4, 0.5f, 71))
    }
}
