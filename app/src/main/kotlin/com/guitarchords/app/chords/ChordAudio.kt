package com.guitarchords.app.chords

/**
 * Traduce digitaciones a notas MIDI para poder escuchar los acordes.
 *
 * Las digitaciones ([ChordShape.frets]) llevan un traste absoluto por cuerda,
 * empezando por la que se dibuja a la izquierda (6.ª en guitarra, 4.ª en
 * ukelele); `-1` = cuerda muda, `0` = al aire. El instrumento se deduce del
 * número de trastes, así que un diagrama de ukelele suena a ukelele sin que
 * quien llama tenga que decirlo.
 */
object ChordAudio {

    /** Afinación estándar de guitarra, Mi grave primero: E2 A2 D3 G3 B3 E4. */
    val OPEN_STRING_MIDI = Instrument.GUITAR.openMidi

    /** Nota de una cuerda pisada en [fret]; null si la cuerda está muda. */
    fun midiOf(
        stringIdx: Int,
        fret: Int,
        instrument: Instrument = Instrument.DEFAULT
    ): Int? {
        val abiertas = instrument.openMidi
        if (stringIdx !in abiertas.indices || fret < 0) return null
        return abiertas[stringIdx] + fret
    }

    /** Notas que suenan en una digitación, de la más grave a la más aguda. */
    fun midisOf(frets: List<Int>): List<Int> {
        val instrument = Instrument.forStringCount(frets.size)
        return frets.take(instrument.strings)
            .mapIndexedNotNull { i, fret -> midiOf(i, fret, instrument) }
    }

    fun midisOf(shape: ChordShape): List<Int> = midisOf(shape.frets)
}
