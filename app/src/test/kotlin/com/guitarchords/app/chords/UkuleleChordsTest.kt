package com.guitarchords.app.chords

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acordes de ukelele. En la JVM no hay assets, así que aquí se ejercita justo lo
 * que va a usar la aplicación mientras no exista el diccionario empaquetado: el
 * generador de digitaciones sobre la afinación sol-do-mi-la.
 *
 * Las cuatro posturas que se comprueban son las primeras que aprende cualquiera,
 * y son la prueba de que la afinación reentrante está bien tratada: si se le
 * exigiera —como en la guitarra— que la cuerda de más a la izquierda llevara la
 * fundamental, saldrían posturas altas en vez de estas.
 */
class UkuleleChordsTest {

    private fun frets(nombre: String): List<Int> =
        ChordLibrary.find(nombre, Instrument.UKULELE)!!.variations.first().frets

    @Test
    fun `Do mayor es la cuerda de la en el tercer traste`() {
        assertEquals(listOf(0, 0, 0, 3), frets("C"))
    }

    @Test
    fun `La menor es un solo dedo en la cuarta cuerda`() {
        assertEquals(listOf(2, 0, 0, 0), frets("Am"))
    }

    @Test
    fun `Fa mayor son dos dedos`() {
        assertEquals(listOf(2, 0, 1, 0), frets("F"))
    }

    @Test
    fun `Sol mayor es el triangulo`() {
        assertEquals(listOf(0, 2, 3, 2), frets("G"))
    }

    @Test
    fun `las digitaciones traen cuatro trastes, no seis`() {
        for (nombre in listOf("C", "Am", "F", "G", "D7", "Bm", "Emaj7")) {
            val acorde = ChordLibrary.find(nombre, Instrument.UKULELE)
            assertTrue("$nombre sin digitaciones", acorde!!.variations.isNotEmpty())
            acorde.variations.forEach {
                assertEquals("$nombre con ${it.frets.size} cuerdas", 4, it.frets.size)
            }
        }
    }

    @Test
    fun `el mismo acorde en guitarra sigue teniendo seis cuerdas`() {
        val guitarra = ChordLibrary.find("C", Instrument.GUITAR)!!.variations.first()
        assertEquals(6, guitarra.frets.size)
    }

    @Test
    fun `la fundamental suena en todas las posturas generadas`() {
        val abiertas = Instrument.UKULELE.openPitchClasses
        for (raiz in ChordLibrary.ROOTS) {
            val acorde = ChordLibrary.find(raiz, Instrument.UKULELE) ?: continue
            val forma = acorde.variations.firstOrNull() ?: continue
            val raizPc = ChordLibrary.ROOTS.indexOf(raiz)
            val suenan = forma.frets.mapIndexedNotNull { i, f ->
                if (f < 0) null else (abiertas[i] + f) % 12
            }
            assertTrue("$raiz sin fundamental: ${forma.frets}", raizPc in suenan)
        }
    }

    @Test
    fun `el audio de una digitacion de ukelele usa sus cuerdas`() {
        // Do al aire: sol4 do4 mi4 y la4 en el tercer traste (do5).
        assertEquals(listOf(67, 60, 64, 72), ChordAudio.midisOf(listOf(0, 0, 0, 3)))
        // Y la guitarra sigue sonando a guitarra: mi2 la2 re3 sol3 si3 mi4.
        assertEquals(
            listOf(40, 45, 50, 55, 59, 64),
            ChordAudio.midisOf(listOf(0, 0, 0, 0, 0, 0))
        )
    }
}
