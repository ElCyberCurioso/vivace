package com.guitarchords.app.chords

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    private val warmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Call once at app startup so [shapes] can read the bundled asset. */
    fun init(context: Context) {
        appContext = context.applicationContext
        warmScope.launch { ensureLoaded() }   // warm the cache off the main thread
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

    // Pitch class de las cuerdas al aire (Mi grave .. mi agudo).
    private val OPEN_STRING_PC = intArrayOf(4, 9, 2, 7, 11, 4)
    private val NOTE_PC = mapOf(
        "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4, "F" to 5,
        "F#" to 6, "G" to 7, "G#" to 8, "A" to 9, "A#" to 10, "B" to 11
    )

    /** All known voicings for a chord, or empty if the DB has no entry for it. */
    fun shapes(root: String, quality: String): List<ChordShape> {
        ensureLoaded()
        val dbKey = KEY_MAP[root] ?: root
        val suffix = SUFFIX_MAP[quality] ?: return emptyList()
        val chord = data[dbKey]?.firstOrNull { it.suffix == suffix } ?: return emptyList()
        val rootPc = NOTE_PC[root] ?: return chord.positions.mapNotNull { it.toShape(null) }
        return chord.positions.mapNotNull { it.toShape(rootPc) }
    }

    private fun DbPos.toShape(rootPc: Int?): ChordShape? {
        if (frets.size != 6) return null
        val abs = frets.map { if (it <= 0) it else it + baseFret - 1 }.toMutableList()
        val fingers6 = (if (fingers.size == 6) fingers else List(6) { 0 }).toMutableList()

        // chords-db trae voicings de cejilla completa con la 5ª en el bajo
        // (Bm = 224432). Para el diagrama estándar silenciamos las cuerdas
        // graves que no suenan la fundamental cuando esta aparece justo
        // después (Bm → x24432, Cm → x35543).
        if (rootPc != null) {
            repeat(2) {
                val lo = abs.indexOfFirst { it >= 0 }
                if (lo !in 0..1) return@repeat
                val loPc = (OPEN_STRING_PC[lo] + abs[lo]) % 12
                if (loPc == rootPc) return@repeat
                val nextRoot = (lo + 1..2).any { s ->
                    abs[s] >= 0 && (OPEN_STRING_PC[s] + abs[s]) % 12 == rootPc
                }
                if (!nextRoot) return@repeat
                abs[lo] = -1
                fingers6[lo] = 0
            }
        }

        // La cejilla solo abarca desde la cuerda sonada más grave hasta la más
        // aguda que pisa ese traste — nunca cuerdas silenciadas.
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
