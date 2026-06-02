package com.guitarchords.app.chords

/**
 * Static music-theory data used by the chord-dictionary guide:
 * note names, intervals and the interval formula of every chord quality.
 *
 * This file is also the single source of truth for the list of chord
 * qualities the app knows about: [ChordLibrary.QUALITIES] and
 * [ChordRecognizer.QUALITY_INTERVALS] are derived from [FORMULAS].
 */
object MusicTheory {

    val NOTES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** Cifrado americano <-> latino. */
    val LETTER_TO_LATIN = listOf(
        "C" to "Do", "D" to "Re", "E" to "Mi", "F" to "Fa",
        "G" to "Sol", "A" to "La", "B" to "Si"
    )

    data class Interval(val semitones: Int, val name: String, val short: String)

    val INTERVALS = listOf(
        Interval(0, "Unísono", "1"),
        Interval(1, "2ª menor", "b2"),
        Interval(2, "2ª mayor", "2"),
        Interval(3, "3ª menor", "b3"),
        Interval(4, "3ª mayor", "3"),
        Interval(5, "4ª justa", "4"),
        Interval(6, "Tritono (5ª disminuida)", "b5"),
        Interval(7, "5ª justa", "5"),
        Interval(8, "6ª menor (5ª aumentada)", "#5"),
        Interval(9, "6ª mayor", "6"),
        Interval(10, "7ª menor", "b7"),
        Interval(11, "7ª mayor", "7"),
        Interval(12, "8ª (octava)", "8")
    )

    data class ChordFormula(
        val quality: String,        // "" "m" "7" ...
        val symbol: String,         // shown next to the root
        val displayName: String,    // "Mayor", "Menor"...
        val degrees: String,        // "1 · 3 · 5"
        val semitones: List<Int>,
        val description: String
    )

    /** Master list. Order = priority for recognition + listing. */
    val FORMULAS: List<ChordFormula> = listOf(
        ChordFormula("", "—", "Mayor",
            "1 · 3 · 5", listOf(0, 4, 7),
            "Tónica + 3ª mayor + 5ª justa. Sonido alegre y estable."),
        ChordFormula("m", "m", "Menor",
            "1 · b3 · 5", listOf(0, 3, 7),
            "Tónica + 3ª menor + 5ª justa. Sonido triste o melancólico."),
        ChordFormula("5", "5", "Power chord",
            "1 · 5", listOf(0, 7),
            "Sin 3ª: no es mayor ni menor. Muy usado en rock."),
        ChordFormula("sus2", "sus2", "Suspendida 2ª",
            "1 · 2 · 5", listOf(0, 2, 7),
            "La 3ª se sustituye por la 2ª."),
        ChordFormula("sus4", "sus4", "Suspendida 4ª",
            "1 · 4 · 5", listOf(0, 5, 7),
            "La 3ª se sustituye por la 4ª."),
        ChordFormula("dim", "dim", "Disminuido",
            "1 · b3 · b5", listOf(0, 3, 6),
            "3ª y 5ª disminuidas. Sonido inestable."),
        ChordFormula("aug", "aug", "Aumentado",
            "1 · 3 · #5", listOf(0, 4, 8),
            "5ª aumentada. Sonido brillante y suspendido."),
        ChordFormula("6", "6", "Sexta",
            "1 · 3 · 5 · 6", listOf(0, 4, 7, 9),
            "Acorde mayor + 6ª mayor."),
        ChordFormula("m6", "m6", "Menor sexta",
            "1 · b3 · 5 · 6", listOf(0, 3, 7, 9),
            "Acorde menor + 6ª mayor."),
        ChordFormula("7", "7", "Séptima (dominante)",
            "1 · 3 · 5 · b7", listOf(0, 4, 7, 10),
            "Mayor + 7ª menor. Pide resolución."),
        ChordFormula("maj7", "maj7", "Séptima mayor",
            "1 · 3 · 5 · 7", listOf(0, 4, 7, 11),
            "Mayor + 7ª mayor. Sonido suave, jazz."),
        ChordFormula("m7", "m7", "Menor séptima",
            "1 · b3 · 5 · b7", listOf(0, 3, 7, 10),
            "Menor + 7ª menor."),
        ChordFormula("mMaj7", "mMaj7", "Menor con 7ª mayor",
            "1 · b3 · 5 · 7", listOf(0, 3, 7, 11),
            "Menor + 7ª mayor. Sonido cinematográfico."),
        ChordFormula("m7b5", "m7b5", "Semidisminuido (ø)",
            "1 · b3 · b5 · b7", listOf(0, 3, 6, 10),
            "Menor con 5ª disminuida."),
        ChordFormula("dim7", "dim7", "Séptima disminuida",
            "1 · b3 · b5 · 6", listOf(0, 3, 6, 9),
            "Disminuido + 7ª disminuida. Muy tenso, simétrico."),
        ChordFormula("aug7", "aug7", "Séptima aumentada",
            "1 · 3 · #5 · b7", listOf(0, 4, 8, 10),
            "Aumentado + 7ª menor."),
        ChordFormula("augMaj7", "augMaj7", "Séptima mayor aumentada",
            "1 · 3 · #5 · 7", listOf(0, 4, 8, 11),
            "Aumentado + 7ª mayor."),
        ChordFormula("7sus2", "7sus2", "Séptima sus2",
            "1 · 2 · 5 · b7", listOf(0, 2, 7, 10),
            "Séptima con 2ª en lugar de 3ª."),
        ChordFormula("7sus4", "7sus4", "Séptima sus4",
            "1 · 4 · 5 · b7", listOf(0, 5, 7, 10),
            "Séptima con 4ª en lugar de 3ª."),
        ChordFormula("add9", "add9", "Con novena añadida",
            "1 · 3 · 5 · 9", listOf(0, 4, 7, 2),
            "Mayor + 9ª, sin 7ª."),
        ChordFormula("madd9", "m(add9)", "Menor con novena añadida",
            "1 · b3 · 5 · 9", listOf(0, 3, 7, 2),
            "Menor + 9ª, sin 7ª."),
        ChordFormula("add11", "add11", "Con oncena añadida",
            "1 · 3 · 5 · 11", listOf(0, 4, 7, 5),
            "Mayor + 11ª, sin 7ª ni 9ª."),
        ChordFormula("9", "9", "Novena",
            "1 · 3 · 5 · b7 · 9", listOf(0, 4, 7, 10, 2),
            "Séptima + 9ª. Color jazz/funk."),
        ChordFormula("maj9", "maj9", "Novena mayor",
            "1 · 3 · 5 · 7 · 9", listOf(0, 4, 7, 11, 2),
            "Séptima mayor + 9ª."),
        ChordFormula("m9", "m9", "Menor novena",
            "1 · b3 · 5 · b7 · 9", listOf(0, 3, 7, 10, 2),
            "Menor séptima + 9ª."),
        ChordFormula("9sus4", "9sus4", "Novena sus4",
            "1 · 4 · 5 · b7 · 9", listOf(0, 5, 7, 10, 2),
            "Sus4 con 7ª y 9ª."),
        ChordFormula("11", "11", "Oncena",
            "1 · (3) · 5 · b7 · 9 · 11", listOf(0, 4, 7, 10, 2, 5),
            "Novena + 11ª. La 3ª suele omitirse en guitarra."),
        ChordFormula("m11", "m11", "Menor oncena",
            "1 · b3 · 5 · b7 · 9 · 11", listOf(0, 3, 7, 10, 2, 5),
            "Menor novena + 11ª."),
        ChordFormula("maj11", "maj11", "Oncena mayor",
            "1 · 3 · 5 · 7 · 9 · 11", listOf(0, 4, 7, 11, 2, 5),
            "Novena mayor + 11ª."),
        ChordFormula("13", "13", "Trecena",
            "1 · 3 · 5 · b7 · 9 · 13", listOf(0, 4, 7, 10, 2, 9),
            "Séptima dominante + 9ª + 13ª. La 11 se omite."),
        ChordFormula("m13", "m13", "Menor trecena",
            "1 · b3 · 5 · b7 · 9 · 13", listOf(0, 3, 7, 10, 2, 9),
            "Menor novena + 13ª."),
        ChordFormula("maj13", "maj13", "Trecena mayor",
            "1 · 3 · 5 · 7 · 9 · 13", listOf(0, 4, 7, 11, 2, 9),
            "Novena mayor + 13ª."),
        ChordFormula("7b5", "7b5", "Séptima con 5ª disminuida",
            "1 · 3 · b5 · b7", listOf(0, 4, 6, 10),
            "Acorde dominante alterado."),
        ChordFormula("7b9", "7b9", "Séptima con 9ª menor",
            "1 · 3 · 5 · b7 · b9", listOf(0, 4, 7, 10, 1),
            "Dominante alterado. Tensión característica del jazz."),
        ChordFormula("7#9", "7#9", "Séptima con 9ª aumentada",
            "1 · 3 · 5 · b7 · #9", listOf(0, 4, 7, 10, 3),
            "También llamado «acorde de Hendrix»."),
        ChordFormula("7#11", "7#11", "Séptima con 11ª aumentada",
            "1 · 3 · 5 · b7 · #11", listOf(0, 4, 7, 10, 6),
            "Lidio dominante. Color brillante.")
    )

    /** Notes of [semis] applied from [rootIndex] (0 = C). */
    fun notesFor(rootIndex: Int, semis: List<Int>): List<String> =
        semis.map { NOTES[((rootIndex + it) % 12 + 12) % 12] }
}
