package com.guitarchords.app.chords

import org.junit.Assert.assertEquals
import org.junit.Test

class ChordTransposerTest {

    @Test
    fun `transposes simple major chords`() {
        assertEquals("D", ChordTransposer.transposeChord("C", 2))
        assertEquals("A", ChordTransposer.transposeChord("G", 2))
        assertEquals("B", ChordTransposer.transposeChord("C", -1))
    }

    @Test
    fun `keeps suffix intact`() {
        assertEquals("Bm7", ChordTransposer.transposeChord("Am7", 2))
        assertEquals("C#maj7", ChordTransposer.transposeChord("Cmaj7", 1))
        assertEquals("F#sus4", ChordTransposer.transposeChord("Esus4", 2))
    }

    @Test
    fun `normalizes flats to sharps by default`() {
        assertEquals("C#", ChordTransposer.transposeChord("Db", 12))
        assertEquals("A#", ChordTransposer.transposeChord("Ab", 2))
    }

    @Test
    fun `prefers flats when asked`() {
        assertEquals("Bb", ChordTransposer.transposeChord("A", 1, preferFlats = true))
        assertEquals("Eb7", ChordTransposer.transposeChord("D7", 1, preferFlats = true))
        // Sin transponer pero con bemoles: reescribe el sostenido.
        assertEquals("Db", ChordTransposer.transposeChord("C#", 0, preferFlats = true))
    }

    @Test
    fun `handles slash chords`() {
        assertEquals("D/F#", ChordTransposer.transposeChord("C/E", 2))
        assertEquals("Bb/D", ChordTransposer.transposeChord("A/C#", 1, preferFlats = true))
    }

    @Test
    fun `wraps around the octave`() {
        assertEquals("C", ChordTransposer.transposeChord("C", 12))
        assertEquals("B", ChordTransposer.transposeChord("C", -13))
    }

    @Test
    fun `unknown roots are left untouched`() {
        assertEquals("?", ChordTransposer.transposeChord("?", 3))
    }

    @Test
    fun `transposes braces in content but not tab blocks`() {
        val content = "{Am} hola {C}\n{tab}\ne|--0--{X}--|\n{/tab}\n{G}"
        val out = ChordTransposer.transposeContent(content, 2)
        assertEquals("{Bm} hola {D}\n{tab}\ne|--0--{X}--|\n{/tab}\n{A}", out)
    }

    @Test
    fun `content untouched with zero semitones and sharps`() {
        val content = "{Am} hola"
        assertEquals(content, ChordTransposer.transposeContent(content, 0))
    }
}
