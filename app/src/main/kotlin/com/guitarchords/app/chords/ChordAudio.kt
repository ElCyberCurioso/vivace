package com.guitarchords.app.chords

/**
 * Traduce digitaciones a notas MIDI para poder escuchar los acordes.
 *
 * Las digitaciones ([ChordShape.frets]) llevan 6 trastes absolutos empezando
 * por la 6.ª cuerda (Mi grave); `-1` = cuerda muda, `0` = al aire.
 */
object ChordAudio {

    /** Afinación estándar, Mi grave primero: E2 A2 D3 G3 B3 E4. */
    val OPEN_STRING_MIDI = intArrayOf(40, 45, 50, 55, 59, 64)

    /** Nota de una cuerda pisada en [fret]; null si la cuerda está muda. */
    fun midiOf(stringIdx: Int, fret: Int): Int? {
        if (stringIdx !in OPEN_STRING_MIDI.indices || fret < 0) return null
        return OPEN_STRING_MIDI[stringIdx] + fret
    }

    /** Notas que suenan en una digitación, de la más grave a la más aguda. */
    fun midisOf(frets: List<Int>): List<Int> =
        frets.take(OPEN_STRING_MIDI.size)
            .mapIndexedNotNull { i, fret -> midiOf(i, fret) }

    fun midisOf(shape: ChordShape): List<Int> = midisOf(shape.frets)
}
