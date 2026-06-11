package com.guitarchords.app.sync

import com.guitarchords.app.data.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongTextFormatTest {

    @Test
    fun `encode then decode roundtrips`() {
        val song = Song(
            title = "Wonderwall",
            artist = "Oasis",
            genre = "Rock",
            capo = 2,
            favorite = true,
            sourceUrl = "https://tabs.example.com/wonderwall",
            content = "{Em7}Today is gonna be the day"
        )
        val text = SongTextFormat.encode(song, "Favoritas")
        val parsed = SongTextFormat.decode(text)
        assertEquals("Wonderwall", parsed.title)
        assertEquals("Oasis", parsed.artist)
        assertEquals("Rock", parsed.genre)
        assertEquals(2, parsed.capo)
        assertTrue(parsed.favorite)
        assertEquals("Favoritas", parsed.playlist)
        assertEquals("https://tabs.example.com/wonderwall", parsed.sourceUrl)
        assertEquals("{Em7}Today is gonna be the day", parsed.content)
    }

    @Test
    fun `decode without separator stops header at first plain line`() {
        val parsed = SongTextFormat.decode("#title: Test\nLínea de letra\nOtra")
        assertEquals("Test", parsed.title)
        assertEquals("Línea de letra\nOtra", parsed.content)
    }

    @Test
    fun `decode clamps capo and defaults title`() {
        val parsed = SongTextFormat.decode("#capo: 99\n---\nbody")
        assertEquals(12, parsed.capo)
        assertEquals("Sin título", parsed.title)
        assertNull(parsed.playlist)
    }
}
