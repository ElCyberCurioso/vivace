package com.guitarchords.app.chords

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nota: en JVM ChordDb no tiene assets, así que estas pruebas ejercitan las
 * plantillas/acordes abiertos de ChordLibrary y la lógica de bajos slash.
 */
class ChordLibraryTest {

    @Test
    fun `D slash F sharp marks the bass on the 6th string 2nd fret`() {
        val chord = ChordLibrary.find("D/F#")!!
        val open = chord.variations.first()
        assertEquals(listOf(2, -1, 0, 2, 3, 2), open.frets)
    }

    @Test
    fun `G slash B mutes the low E and uses B on the 5th string`() {
        val chord = ChordLibrary.find("G/B")!!
        val open = chord.variations.first()
        assertEquals(listOf(-1, 2, 0, 0, 0, 3), open.frets)
    }

    @Test
    fun `C slash G adds G on the 6th string`() {
        val chord = ChordLibrary.find("C/G")!!
        val open = chord.variations.first()
        assertEquals(listOf(3, 3, 2, 0, 1, 0), open.frets)
    }

    @Test
    fun `Am slash G adds G bass`() {
        val chord = ChordLibrary.find("Am/G")!!
        val open = chord.variations.first()
        assertEquals(listOf(3, 0, 2, 2, 1, 0), open.frets)
    }

    @Test
    fun `slash with same note as root leaves shape untouched`() {
        val plain = ChordLibrary.find("D")!!.variations.first()
        val slash = ChordLibrary.find("D/D")!!.variations.first()
        assertEquals(plain.frets, slash.frets)
    }

    @Test
    fun `Bm A-shape barre spans five strings not six`() {
        val chord = ChordLibrary.find("Bm")!!
        val aShape = chord.variations.first { it.frets == listOf(-1, 2, 4, 4, 3, 2) }
        assertEquals(1, aShape.barres.size)
        val barre = aShape.barres.single()
        assertEquals(2, barre.fret)
        assertEquals(5, maxOf(barre.fromString, barre.toString))
        assertEquals(1, minOf(barre.fromString, barre.toString))
    }

    @Test
    fun `full barre is kept where it belongs (F major E-shape)`() {
        val chord = ChordLibrary.find("F")!!
        val eShape = chord.variations.first { it.frets == listOf(1, 3, 3, 2, 1, 1) }
        val barre = eShape.barres.single()
        assertEquals(1, barre.fret)
        assertEquals(6, maxOf(barre.fromString, barre.toString))
    }
}
