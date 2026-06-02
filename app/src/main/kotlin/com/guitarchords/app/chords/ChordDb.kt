package com.guitarchords.app.chords

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Chord voicing database backed by the bundled `assets/chords/guitar.json`
 * (tombatossals/chords-db, MIT). Provides multiple real-world positions per
 * chord — frets, fingers and barres — which [ChordLibrary] prefers over its
 * built-in movable templates.
 *
 * Data convention (chords-db):
 *  - `frets`: 6 ints, low E first; -1 muted, 0 open, otherwise relative to
 *    `baseFret` (a value of 1 means `baseFret`). Absolute = v + baseFret - 1.
 *  - `barres`: relative fret numbers that are barred.
 */
object ChordDb {

    @Serializable
    private data class DbFile(val chords: Map<String, List<DbChord>> = emptyMap())

    @Serializable
    private data class DbChord(
        val key: String = "",
        val suffix: String = "",
        val positions: List<DbPos> = emptyList()
    )

    @Serializable
    private data class DbPos(
        val frets: List<Int> = emptyList(),
        val fingers: List<Int> = emptyList(),
        val baseFret: Int = 1,
        val barres: List<Int> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true }

    private var appContext: Context? = null
    @Volatile private var loaded = false
    private var data: Map<String, List<DbChord>> = emptyMap()

    /** Call once at app startup so [shapes] can read the bundled asset. */
    fun init(context: Context) {
        appContext = context.applicationContext
        Thread { ensureLoaded() }.start()   // warm the cache off the main thread
    }

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val ctx = appContext
            if (ctx != null) {
                runCatching {
                    ctx.assets.open("chords/guitar.json").use { ins ->
                        data = json.decodeFromString<DbFile>(ins.readBytes().decodeToString()).chords
                    }
                }
            }
            loaded = true
        }
    }

    /** All known voicings for a chord, or empty if the DB has no entry for it. */
    fun shapes(root: String, quality: String): List<ChordShape> {
        ensureLoaded()
        val dbKey = KEY_MAP[root] ?: root
        val suffix = SUFFIX_MAP[quality] ?: return emptyList()
        val chord = data[dbKey]?.firstOrNull { it.suffix == suffix } ?: return emptyList()
        return chord.positions.mapNotNull { it.toShape() }
    }

    private fun DbPos.toShape(): ChordShape? {
        if (frets.size != 6) return null
        val abs = frets.map { if (it <= 0) it else it + baseFret - 1 }
        val fingers6 = if (fingers.size == 6) fingers else List(6) { 0 }
        val barreList = barres.mapNotNull { rel ->
            val absBr = rel + baseFret - 1
            val idx = abs.indices.filter { abs[it] == absBr }
            if (idx.isEmpty()) null else Barre(absBr, 6 - idx.first(), 6 - idx.last())
        }
        return ChordShape(abs, fingers6, barreList)
    }

    // App root (sharps, see ChordLibrary.ROOTS) -> chords-db key (mixed sharps/flats).
    private val KEY_MAP = mapOf(
        "C" to "C", "C#" to "C#", "D" to "D", "D#" to "Eb", "E" to "E", "F" to "F",
        "F#" to "F#", "G" to "G", "G#" to "Ab", "A" to "A", "A#" to "Bb", "B" to "B"
    )

    // App quality (MusicTheory.FORMULAS) -> chords-db suffix. Unmapped qualities
    // fall through to ChordLibrary's templates / auto-voicing.
    private val SUFFIX_MAP = mapOf(
        "" to "major",
        "m" to "minor",
        "5" to "5",
        "sus2" to "sus2",
        "sus4" to "sus4",
        "dim" to "dim",
        "aug" to "aug",
        "6" to "6",
        "m6" to "m6",
        "7" to "7",
        "maj7" to "maj7",
        "m7" to "m7",
        "mMaj7" to "mmaj7",
        "m7b5" to "m7b5",
        "dim7" to "dim7",
        "aug7" to "aug7",
        "augMaj7" to "maj7#5",
        "7sus4" to "7sus4",
        "add9" to "add9",
        "madd9" to "madd9",
        "add11" to "add11",
        "9" to "9",
        "maj9" to "maj9",
        "m9" to "m9",
        "11" to "11",
        "m11" to "m11",
        "maj11" to "maj11",
        "13" to "13",
        "maj13" to "maj13",
        "7b5" to "7b5",
        "7b9" to "7b9",
        "7#9" to "7#9"
    )
}
