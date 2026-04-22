package com.guitarchords.app.chords

object ChordRecognizer {

    private val OPEN_STRINGS = intArrayOf(40, 45, 50, 55, 59, 64)

    private val QUALITY_INTERVALS: Map<String, Set<Int>> = linkedMapOf(
        "" to setOf(0, 4, 7),
        "m" to setOf(0, 3, 7),
        "sus2" to setOf(0, 2, 7),
        "sus4" to setOf(0, 5, 7),
        "dim" to setOf(0, 3, 6),
        "aug" to setOf(0, 4, 8),
        "7" to setOf(0, 4, 7, 10),
        "maj7" to setOf(0, 4, 7, 11),
        "m7" to setOf(0, 3, 7, 10),
        "m7b5" to setOf(0, 3, 6, 10),
        "dim7" to setOf(0, 3, 6, 9),
        "6" to setOf(0, 4, 7, 9),
        "m6" to setOf(0, 3, 7, 9),
        "add9" to setOf(0, 2, 4, 7),
        "9" to setOf(0, 2, 4, 7, 10)
    )

    data class Match(
        val name: String,
        val root: String,
        val quality: String,
        val bassIsRoot: Boolean,
        val slashBass: String? = null
    )

    fun identify(frets: List<Int>): List<Match> {
        if (frets.size != 6) return emptyList()
        val pitches = frets.mapIndexedNotNull { i, f ->
            if (f >= 0) OPEN_STRINGS[i] + f else null
        }
        if (pitches.isEmpty()) return emptyList()
        val pcSet = pitches.map { ((it % 12) + 12) % 12 }.toSet()
        val bassPc = (pitches.min() % 12 + 12) % 12

        val result = mutableListOf<Match>()
        for (root in 0..11) {
            for ((q, intervals) in QUALITY_INTERVALS) {
                val target = intervals.map { (it + root) % 12 }.toSet()
                if (target == pcSet) {
                    val rootName = ChordLibrary.ROOTS[root]
                    val bassName = ChordLibrary.ROOTS[bassPc]
                    val isBassRoot = bassPc == root
                    val display = if (isBassRoot) rootName + q else "$rootName$q/$bassName"
                    result += Match(
                        name = display,
                        root = rootName,
                        quality = q,
                        bassIsRoot = isBassRoot,
                        slashBass = if (isBassRoot) null else bassName
                    )
                }
            }
        }

        result.sortWith(
            compareByDescending<Match> { it.bassIsRoot }
                .thenBy {
                    val i = ChordLibrary.QUALITIES.indexOf(it.quality)
                    if (i < 0) 99 else i
                }
        )
        return result
    }
}
