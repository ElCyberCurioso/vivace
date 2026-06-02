package com.guitarchords.app.chords

object ChordRecognizer {

    private val OPEN_STRINGS = intArrayOf(40, 45, 50, 55, 59, 64)

    /** Pitch-class intervals (mod-12) per quality, derived from [MusicTheory.FORMULAS]. */
    val QUALITY_INTERVALS: Map<String, Set<Int>> = linkedMapOf<String, Set<Int>>().apply {
        MusicTheory.FORMULAS.forEach { f ->
            put(f.quality, f.semitones.map { ((it % 12) + 12) % 12 }.toSet())
        }
    }

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
