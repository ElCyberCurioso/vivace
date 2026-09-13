package com.guitarchords.app.tuner

import com.guitarchords.app.chords.Instrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Afinaciones del afinador. Lo que se prueba no es la lista —esa se lee— sino
 * las dos cosas que pueden hacer que el afinador mienta: que la frecuencia
 * calculada a partir del nombre de la nota sea la correcta, y que la cuerda que
 * elige sea la que de verdad se está tocando.
 */
class TuningsTest {

    private fun tuning(id: String): Tuning {
        val t = Tunings.byId(id)
        assertNotNull("no existe la afinación $id", t)
        return t!!
    }

    @Test
    fun `la4 son 440 hercios y do4 el midi 60`() {
        assertEquals(440f, Tunings.frecuenciaDe("A4"), 0.001f)
        assertEquals(60, Tunings.midiDe("C4"))
        assertEquals(69, Tunings.midiDe("A4"))
        // Enarmónicos: la misma tecla por dos nombres.
        assertEquals(Tunings.midiDe("D#2"), Tunings.midiDe("Eb2"))
    }

    @Test
    fun `la estandar de guitarra da las frecuencias de siempre`() {
        val estandar = tuning("guitar_standard").targets
        val esperadas = listOf(82.41f, 110.00f, 146.83f, 196.00f, 246.94f, 329.63f)
        estandar.forEachIndexed { i, cuerda ->
            assertTrue(
                "${cuerda.name}: ${cuerda.freq} != ${esperadas[i]}",
                abs(cuerda.freq - esperadas[i]) < 0.05f
            )
        }
    }

    @Test
    fun `drop D solo baja la sexta`() {
        val estandar = tuning("guitar_standard").notes
        val dropD = tuning("guitar_drop_d").notes
        assertEquals("D2", dropD.first())
        assertEquals(estandar.drop(1), dropD.drop(1))
    }

    @Test
    fun `el ukelele estandar es reentrante y suena en el orden del mastil`() {
        val uke = tuning("uke_standard")
        assertEquals(listOf("G4", "C4", "E4", "A4"), uke.notes)
        assertEquals(Instrument.UKULELE, uke.instrument)
        // Reentrante de verdad: la primera cuerda que se dibuja suena MÁS AGUDA
        // que la segunda. Si alguien "ordena" esta lista, rompe el afinador.
        assertTrue(uke.targets[0].freq > uke.targets[1].freq)
    }

    @Test
    fun `cada instrumento trae sus afinaciones y su estandar por delante`() {
        assertEquals("guitar_standard", Tunings.defaultFor(Instrument.GUITAR).id)
        assertEquals("uke_standard", Tunings.defaultFor(Instrument.UKULELE).id)
        Tunings.forInstrument(Instrument.GUITAR).forEach { assertEquals(6, it.notes.size) }
        Tunings.forInstrument(Instrument.UKULELE).forEach { assertEquals(4, it.notes.size) }
    }

    @Test
    fun `elige la cuerda que se esta tocando, tambien en drop C`() {
        val dropC = tuning("guitar_drop_c")
        // Do2 grave: la sexta, no la quinta.
        assertEquals("C2", Tunings.nearest(dropC, 65.4f)?.name)
        assertEquals("G2", Tunings.nearest(dropC, 97.5f)?.name)
        // Y en el ukelele, el sol agudo de la cuarta cuerda.
        assertEquals("G4", Tunings.nearest(tuning("uke_standard"), 391f)?.name)
        assertEquals(null, Tunings.nearest(dropC, 0f))
    }

    @Test
    fun `los cents dicen cuanto falta y hacia donde`() {
        val la = StringTarget("A4", 440f)
        assertEquals(0f, Tunings.cents(440f, la), 0.01f)
        // Un semitono son cien cents, arriba y abajo.
        assertEquals(100f, Tunings.cents(466.164f, la), 0.5f)
        assertEquals(-100f, Tunings.cents(415.305f, la), 0.5f)
    }
}
