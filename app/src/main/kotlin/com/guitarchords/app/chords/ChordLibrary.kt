package com.guitarchords.app.chords

object ChordLibrary {

    val ROOTS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLAT = mapOf(
        "Db" to "C#", "Eb" to "D#", "Gb" to "F#",
        "Ab" to "G#", "Bb" to "A#", "Cb" to "B", "Fb" to "E"
    )

    val QUALITIES = listOf(
        "", "m", "7", "maj7", "m7", "sus2", "sus4",
        "dim", "dim7", "m7b5", "aug", "add9", "6", "9", "m6"
    )

    private data class Template(
        val offsets: List<Int>,
        val barreFingers: List<Int>,
        val openFingers: List<Int>,
        val barreString: Int,
        val extraBarre: Barre? = null
    )

    private val eTemplates = mapOf(
        "" to Template(
            listOf(0, 2, 2, 1, 0, 0),
            listOf(1, 3, 4, 2, 1, 1),
            listOf(0, 2, 3, 1, 0, 0), 6
        ),
        "m" to Template(
            listOf(0, 2, 2, 0, 0, 0),
            listOf(1, 3, 4, 1, 1, 1),
            listOf(0, 2, 3, 0, 0, 0), 6
        ),
        "7" to Template(
            listOf(0, 2, 0, 1, 0, 0),
            listOf(1, 3, 1, 2, 1, 1),
            listOf(0, 2, 0, 1, 0, 0), 6
        ),
        "maj7" to Template(
            listOf(0, 2, 1, 1, 0, 0),
            listOf(1, 3, 2, 2, 1, 1),
            listOf(0, 3, 1, 2, 0, 0), 6
        ),
        "m7" to Template(
            listOf(0, 2, 0, 0, 0, 0),
            listOf(1, 3, 1, 1, 1, 1),
            listOf(0, 2, 0, 0, 0, 0), 6
        ),
        "sus4" to Template(
            listOf(0, 2, 2, 2, 0, 0),
            listOf(1, 2, 3, 4, 1, 1),
            listOf(0, 1, 2, 3, 0, 0), 6
        )
    )

    private val aTemplates = mapOf(
        "" to Template(
            listOf(-1, 0, 2, 2, 2, 0),
            listOf(0, 1, 3, 3, 3, 1),
            listOf(0, 0, 1, 2, 3, 0), 5
        ),
        "m" to Template(
            listOf(-1, 0, 2, 2, 1, 0),
            listOf(0, 1, 3, 4, 2, 1),
            listOf(0, 0, 2, 3, 1, 0), 5
        ),
        "7" to Template(
            listOf(-1, 0, 2, 0, 2, 0),
            listOf(0, 1, 3, 1, 4, 1),
            listOf(0, 0, 2, 0, 3, 0), 5
        ),
        "maj7" to Template(
            listOf(-1, 0, 2, 1, 2, 0),
            listOf(0, 1, 3, 2, 4, 1),
            listOf(0, 0, 2, 1, 3, 0), 5
        ),
        "m7" to Template(
            listOf(-1, 0, 2, 0, 1, 0),
            listOf(0, 1, 3, 1, 2, 1),
            listOf(0, 0, 2, 0, 1, 0), 5
        ),
        "sus2" to Template(
            listOf(-1, 0, 2, 2, 0, 0),
            listOf(0, 1, 3, 4, 1, 1),
            listOf(0, 0, 1, 2, 0, 0), 5
        ),
        "sus4" to Template(
            listOf(-1, 0, 2, 2, 3, 0),
            listOf(0, 1, 2, 3, 4, 1),
            listOf(0, 0, 1, 2, 3, 0), 5
        ),
        "dim" to Template(
            listOf(-1, 0, 1, 2, 1, -1),
            listOf(0, 1, 2, 4, 3, 0),
            listOf(0, 0, 1, 3, 2, 0), 5
        ),
        "dim7" to Template(
            listOf(-1, 0, 1, 2, 1, 2),
            listOf(0, 1, 2, 4, 2, 3),
            listOf(0, 0, 1, 3, 2, 4), 5
        ),
        "m7b5" to Template(
            listOf(-1, 0, 1, 2, 1, 3),
            listOf(0, 1, 2, 4, 2, 4),
            listOf(0, 0, 1, 3, 2, 4), 5
        ),
        "aug" to Template(
            listOf(-1, 0, 3, 2, 2, 1),
            listOf(0, 1, 4, 3, 3, 2),
            listOf(0, 0, 4, 3, 2, 1), 5
        ),
        "6" to Template(
            listOf(-1, 0, 2, 2, 2, 2),
            listOf(0, 1, 2, 2, 2, 2),
            listOf(0, 0, 1, 1, 1, 1), 5
        ),
        "add9" to Template(
            listOf(-1, 0, 2, 4, 2, 0),
            listOf(0, 1, 2, 4, 3, 1),
            listOf(0, 0, 1, 4, 2, 0), 5
        ),
        "9" to Template(
            listOf(-1, 0, 2, 4, 2, 3),
            listOf(0, 1, 2, 4, 3, 4),
            listOf(0, 0, 1, 4, 2, 3), 5
        ),
        "m6" to Template(
            listOf(-1, 0, 2, 2, 1, 2),
            listOf(0, 1, 3, 4, 2, 4),
            listOf(0, 0, 2, 3, 1, 4), 5
        )
    )

    private val openChords: Map<String, ChordShape> = mapOf(
        "C" to ChordShape(listOf(-1, 3, 2, 0, 1, 0), listOf(0, 3, 2, 0, 1, 0)),
        "C7" to ChordShape(listOf(-1, 3, 2, 3, 1, 0), listOf(0, 3, 2, 4, 1, 0)),
        "Cmaj7" to ChordShape(listOf(-1, 3, 2, 0, 0, 0), listOf(0, 3, 2, 0, 0, 0)),
        "Cadd9" to ChordShape(listOf(-1, 3, 2, 0, 3, 0), listOf(0, 2, 1, 0, 3, 0)),
        "Csus4" to ChordShape(listOf(-1, 3, 3, 0, 1, 1), listOf(0, 3, 4, 0, 1, 2)),
        "D" to ChordShape(listOf(-1, -1, 0, 2, 3, 2), listOf(0, 0, 0, 1, 3, 2)),
        "Dm" to ChordShape(listOf(-1, -1, 0, 2, 3, 1), listOf(0, 0, 0, 2, 3, 1)),
        "D7" to ChordShape(listOf(-1, -1, 0, 2, 1, 2), listOf(0, 0, 0, 2, 1, 3)),
        "Dmaj7" to ChordShape(listOf(-1, -1, 0, 2, 2, 2), listOf(0, 0, 0, 1, 1, 1)),
        "Dm7" to ChordShape(listOf(-1, -1, 0, 2, 1, 1), listOf(0, 0, 0, 2, 1, 1)),
        "Dsus2" to ChordShape(listOf(-1, -1, 0, 2, 3, 0), listOf(0, 0, 0, 1, 3, 0)),
        "Dsus4" to ChordShape(listOf(-1, -1, 0, 2, 3, 3), listOf(0, 0, 0, 1, 2, 3)),
        "E" to ChordShape(listOf(0, 2, 2, 1, 0, 0), listOf(0, 2, 3, 1, 0, 0)),
        "Em" to ChordShape(listOf(0, 2, 2, 0, 0, 0), listOf(0, 2, 3, 0, 0, 0)),
        "E7" to ChordShape(listOf(0, 2, 0, 1, 0, 0), listOf(0, 2, 0, 1, 0, 0)),
        "Emaj7" to ChordShape(listOf(0, 2, 1, 1, 0, 0), listOf(0, 3, 1, 2, 0, 0)),
        "Em7" to ChordShape(listOf(0, 2, 0, 0, 0, 0), listOf(0, 2, 0, 0, 0, 0)),
        "Esus4" to ChordShape(listOf(0, 2, 2, 2, 0, 0), listOf(0, 1, 2, 3, 0, 0)),
        "Fmaj7" to ChordShape(listOf(-1, -1, 3, 2, 1, 0), listOf(0, 0, 3, 2, 1, 0)),
        "G" to ChordShape(listOf(3, 2, 0, 0, 0, 3), listOf(3, 2, 0, 0, 0, 4)),
        "G7" to ChordShape(listOf(3, 2, 0, 0, 0, 1), listOf(3, 2, 0, 0, 0, 1)),
        "Gmaj7" to ChordShape(listOf(3, 2, 0, 0, 0, 2), listOf(3, 1, 0, 0, 0, 2)),
        "Gsus4" to ChordShape(listOf(3, 3, 0, 0, 1, 3), listOf(2, 3, 0, 0, 1, 4)),
        "A" to ChordShape(listOf(-1, 0, 2, 2, 2, 0), listOf(0, 0, 1, 2, 3, 0)),
        "Am" to ChordShape(listOf(-1, 0, 2, 2, 1, 0), listOf(0, 0, 2, 3, 1, 0)),
        "A7" to ChordShape(listOf(-1, 0, 2, 0, 2, 0), listOf(0, 0, 2, 0, 3, 0)),
        "Amaj7" to ChordShape(listOf(-1, 0, 2, 1, 2, 0), listOf(0, 0, 2, 1, 3, 0)),
        "Am7" to ChordShape(listOf(-1, 0, 2, 0, 1, 0), listOf(0, 0, 2, 0, 1, 0)),
        "Asus2" to ChordShape(listOf(-1, 0, 2, 2, 0, 0), listOf(0, 0, 1, 2, 0, 0)),
        "Asus4" to ChordShape(listOf(-1, 0, 2, 2, 3, 0), listOf(0, 0, 1, 2, 3, 0)),
        "B7" to ChordShape(listOf(-1, 2, 1, 2, 0, 2), listOf(0, 2, 1, 3, 0, 4))
    )

    private fun normalize(root: String): String = FLAT[root] ?: root

    private fun rootIndex(root: String): Int = ROOTS.indexOf(normalize(root))

    fun parseName(name: String): Pair<String, String>? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val m = Regex("^([A-G])([#b]?)").find(trimmed) ?: return null
        val root = normalize(m.value)
        val rest = trimmed.substring(m.value.length)
        val cleanQual = rest.substringBefore("/").trim()
        return root to cleanQual
    }

    fun find(name: String): Chord? {
        val (root, qual) = parseName(name) ?: return null
        return build(root, qual, name)
    }

    private fun build(root: String, quality: String, displayName: String = root + quality): Chord? {
        val rIdx = rootIndex(root)
        if (rIdx < 0) return null
        val variations = mutableListOf<ChordShape>()
        val fullName = root + quality

        openChords[fullName]?.let { variations += it }

        val eRootFret = ((rIdx - 4 + 12) % 12)
        val aRootFret = ((rIdx - 9 + 12) % 12)

        eTemplates[quality]?.let { tpl ->
            val rf = if (eRootFret == 0) 12 else eRootFret
            val shape = buildFromTemplate(tpl, rf, 6)
            if (!variations.any { it.frets == shape.frets }) variations += shape
        }

        aTemplates[quality]?.let { tpl ->
            val rf = if (aRootFret == 0) 12 else aRootFret
            val shape = buildFromTemplate(tpl, rf, 5)
            if (!variations.any { it.frets == shape.frets }) variations += shape
        }

        if (variations.isEmpty()) {
            eTemplates[""]?.let { tpl ->
                val rf = if (eRootFret == 0) 12 else eRootFret
                variations += buildFromTemplate(tpl, rf, 6)
            }
        }

        return Chord(displayName, root, quality, variations)
    }

    private fun buildFromTemplate(tpl: Template, rootFret: Int, barreString: Int): ChordShape {
        val frets = tpl.offsets.map { if (it < 0) -1 else it + rootFret }
        val barres = if (rootFret >= 1 && rootFret <= 12) {
            val list = mutableListOf(Barre(rootFret, barreString, 1))
            tpl.extraBarre?.let { list += it.copy(fret = it.fret + rootFret) }
            list
        } else emptyList()
        val fingers = tpl.barreFingers
        return ChordShape(frets, fingers, barres)
    }

    fun all(): List<Chord> {
        val list = mutableListOf<Chord>()
        for (r in ROOTS) {
            for (q in QUALITIES) {
                build(r, q, r + q)?.let { list += it }
            }
        }
        return list
    }
}
