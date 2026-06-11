package com.guitarchords.app.chords

data class Barre(val fret: Int, val fromString: Int, val toString: Int)

data class ChordShape(
    val frets: List<Int>,
    val fingers: List<Int> = List(6) { 0 },
    val barres: List<Barre> = emptyList(),
    /** Id en custom_chords si la digitación la definió el usuario. */
    val customId: Long? = null
) {
    val baseFret: Int
        get() {
            val positive = frets.filter { it > 0 }
            if (positive.isEmpty()) return 1
            val min = positive.min()
            val max = positive.max()
            return if (max <= 4) 1 else maxOf(1, min)
        }
}

data class Chord(
    val name: String,
    val root: String,
    val quality: String,
    val variations: List<ChordShape>
)
