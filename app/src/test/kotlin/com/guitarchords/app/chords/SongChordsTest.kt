package com.guitarchords.app.chords

import org.junit.Assert.assertEquals
import org.junit.Test

class SongChordsTest {

    @Test
    fun `extrae acordes distintos en orden de aparicion`() {
        val content = "{Am}Hola {C}que tal\n{Am}otra vez {G}fin"
        assertEquals(listOf("Am", "C", "G"), SongChords.distinctChords(content))
    }

    @Test
    fun `normaliza bemoles y quita el bajo`() {
        val content = "{Bb}uno {Bm/F#}dos"
        assertEquals(listOf("A#", "Bm"), SongChords.distinctChords(content))
    }

    @Test
    fun `ignora los bloques de tablatura`() {
        val content = "{C}letra\n{tab}\ne|--0--{X}--\n{/tab}\n{G}mas letra"
        assertEquals(listOf("C", "G"), SongChords.distinctChords(content))
    }

    @Test
    fun `sin acordes devuelve vacio`() {
        assertEquals(emptyList<String>(), SongChords.distinctChords("solo letra"))
    }

    @Test
    fun `dos acordes generan un unico par`() {
        assertEquals(listOf("Am" to "C"), SongChords.changePairs(listOf("Am", "C")))
    }

    @Test
    fun `tres acordes cierran el ciclo`() {
        assertEquals(
            listOf("Am" to "C", "C" to "G", "G" to "Am"),
            SongChords.changePairs(listOf("Am", "C", "G"))
        )
    }

    @Test
    fun `con menos de dos acordes no hay cambios`() {
        assertEquals(emptyList<Pair<String, String>>(), SongChords.changePairs(listOf("Am")))
        assertEquals(emptyList<Pair<String, String>>(), SongChords.changePairs(emptyList()))
    }
}
