package com.guitarchords.app.chords

object ChordLibrary {

    val ROOTS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FLAT = mapOf(
        "Db" to "C#", "Eb" to "D#", "Gb" to "F#",
        "Ab" to "G#", "Bb" to "A#", "Cb" to "B", "Fb" to "E"
    )

    /** Order kept in sync with [MusicTheory.FORMULAS] (priority for recognition). */
    val QUALITIES: List<String> = MusicTheory.FORMULAS.map { it.quality }

    private data class Template(
        val offsets: List<Int>,
        val barreFingers: List<Int>,
        val openFingers: List<Int>,
        val barreString: Int,
        val extraBarre: Barre? = null,
        val barre: Boolean = true
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
        ),
        "5" to Template(
            listOf(0, 2, 2, -1, -1, -1),
            listOf(1, 3, 4, 0, 0, 0),
            listOf(0, 2, 3, 0, 0, 0), 6, barre = false
        ),
        "7sus4" to Template(
            listOf(0, 2, 0, 2, 0, 0),
            listOf(1, 3, 1, 4, 1, 1),
            listOf(0, 2, 0, 3, 0, 0), 6
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
        ),
        "5" to Template(
            listOf(-1, 0, 2, 2, -1, -1),
            listOf(0, 1, 3, 4, 0, 0),
            listOf(0, 0, 1, 2, 0, 0), 5, barre = false
        ),
        "7sus4" to Template(
            listOf(-1, 0, 2, 0, 3, 3),
            listOf(0, 1, 3, 1, 4, 4),
            listOf(0, 0, 2, 0, 3, 4), 5
        )
    )

    // D-shape movable voicings (root on the 4th string). No full barre.
    private val dTemplates = mapOf(
        "" to Template(
            listOf(-1, -1, 0, 2, 3, 2),
            listOf(0, 0, 1, 2, 4, 3),
            listOf(0, 0, 1, 2, 4, 3), 4, barre = false
        ),
        "m" to Template(
            listOf(-1, -1, 0, 2, 3, 1),
            listOf(0, 0, 1, 3, 4, 2),
            listOf(0, 0, 1, 3, 4, 2), 4, barre = false
        ),
        "7" to Template(
            listOf(-1, -1, 0, 2, 1, 2),
            listOf(0, 0, 1, 3, 2, 4),
            listOf(0, 0, 1, 3, 2, 4), 4, barre = false
        ),
        "maj7" to Template(
            listOf(-1, -1, 0, 2, 2, 2),
            listOf(0, 0, 1, 2, 3, 4),
            listOf(0, 0, 1, 2, 3, 4), 4, barre = false
        ),
        "m7" to Template(
            listOf(-1, -1, 0, 2, 1, 1),
            listOf(0, 0, 1, 3, 2, 2),
            listOf(0, 0, 1, 3, 2, 2), 4, barre = false
        ),
        "sus2" to Template(
            listOf(-1, -1, 0, 2, 3, 0),
            listOf(0, 0, 1, 2, 3, 0),
            listOf(0, 0, 1, 2, 3, 0), 4, barre = false
        ),
        "sus4" to Template(
            listOf(-1, -1, 0, 2, 3, 3),
            listOf(0, 0, 1, 2, 3, 4),
            listOf(0, 0, 1, 2, 3, 4), 4, barre = false
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

    /** Nota de bajo de un acorde slash ("D/F#" → "F#"), o null si no la hay. */
    private fun parseBass(name: String): String? {
        val bass = name.trim().substringAfter("/", "").trim()
        if (bass.isEmpty()) return null
        val m = Regex("^([A-G])([#b]?)$").find(bass) ?: return null
        return normalize(m.value)
    }

    /**
     * Digitaciones de un acorde para [instrument]. La guitarra tiene plantillas
     * escritas a mano (las formas de Mi, La y Re) además del diccionario; el
     * resto de instrumentos van con el diccionario si lo hay y, si no, con el
     * generador automático, que solo necesita saber cómo están afinadas las
     * cuerdas al aire.
     */
    fun find(name: String, instrument: Instrument = Instrument.DEFAULT): Chord? {
        val (root, qual) = parseName(name) ?: return null
        return build(root, qual, name, parseBass(name), instrument)
    }

    private fun build(
        root: String,
        quality: String,
        displayName: String = root + quality,
        bass: String? = null,
        instrument: Instrument = Instrument.DEFAULT
    ): Chord? {
        val rIdx = rootIndex(root)
        if (rIdx < 0) return null
        val bassIdx = bass?.let { rootIndex(it) }?.takeIf { it >= 0 && it != rIdx }

        // Las digitaciones del usuario son de guitarra: se guardan con seis
        // trastes y no dicen de qué instrumento son.
        val custom = if (instrument == Instrument.GUITAR) {
            CustomChords.shapesFor(root + quality)
        } else emptyList()

        // Prefer the bundled chords-db voicings when available.
        val dbShapes = ChordDb.shapes(root, quality, instrument)
        if (dbShapes.isNotEmpty()) {
            return Chord(displayName, root, quality, applyBass(custom + dbShapes, bassIdx, instrument))
        }

        val variations = mutableListOf<ChordShape>()
        val fullName = root + quality

        /*
         * Las plantillas y los acordes abiertos son formas de GUITARRA: seis
         * cuerdas afinadas mi-la-re-sol-si-mi. Para otro instrumento no valen
         * ni trasladadas, así que se va directo al generador.
         */
        if (instrument != Instrument.GUITAR) {
            val intervals = ChordRecognizer.QUALITY_INTERVALS[quality]
            if (intervals != null) {
                autoVoicing(rIdx, intervals, instrument)?.let { variations += it }
                // Una segunda postura más arriba del mástil, si la hay: con una
                // sola el carrusel de digitaciones no tiene nada que enseñar.
                autoVoicing(rIdx, intervals, instrument, desde = 3)
                    ?.takeIf { otra -> variations.none { it.frets == otra.frets } }
                    ?.let { variations += it }
            }
            return Chord(displayName, root, quality, applyBass(custom + variations, bassIdx, instrument))
        }

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

        val dRootFret = ((rIdx - 2 + 12) % 12)
        dTemplates[quality]?.let { tpl ->
            val rf = if (dRootFret == 0) 12 else dRootFret
            val shape = buildFromTemplate(tpl, rf, 4)
            if (!variations.any { it.frets == shape.frets }) variations += shape
        }

        if (variations.isEmpty()) {
            // Last-resort: try to auto-build a voicing from the interval set.
            val intervals = ChordRecognizer.QUALITY_INTERVALS[quality]
            if (intervals != null) {
                autoVoicing(rIdx, intervals)?.let { variations += it }
            }
        }

        return Chord(displayName, root, quality, applyBass(custom + variations, bassIdx, instrument))
    }

    /**
     * Adapta los voicings de un acorde slash para que la nota más grave sea el
     * bajo indicado (D/F# → marca F# en la 6ª cuerda, traste 2). Si en alguna
     * variación no hay forma razonable de colocar el bajo, se deja tal cual.
     */
    private fun applyBass(
        shapes: List<ChordShape>,
        bassIdx: Int?,
        instrument: Instrument = Instrument.DEFAULT
    ): List<ChordShape> {
        if (bassIdx == null) return shapes
        val bassPc = ((bassIdx % 12) + 12) % 12
        return shapes.map { withBass(it, bassPc, instrument) ?: it }
    }

    private fun withBass(
        shape: ChordShape,
        bassPc: Int,
        instrument: Instrument = Instrument.DEFAULT
    ): ChordShape? {
        val openPc = instrument.openPitchClasses
        val lowest = shape.frets.indexOfFirst { it >= 0 }
        if (lowest < 0) return null
        val lowestPc = (openPc[lowest] + shape.frets[lowest]) % 12
        if (lowestPc == bassPc) return shape

        // Trastes alcanzables sin salir de la posición de la mano.
        val positives = shape.frets.filter { it > 0 }
        val minFret = positives.minOrNull() ?: 0
        val maxFret = positives.maxOrNull() ?: 0
        val candidates = buildList {
            add(0)
            val from = maxOf(1, minFret - 1)
            val to = maxOf(maxFret + 1, 4)
            addAll(from..to)
        }

        // El bajo debe quedar en una de las cuerdas graves y pasar a ser la nota
        // más grave del acorde (las cuerdas por debajo se silencian). En un
        // instrumento de cuatro cuerdas "las graves" son dos, no tres.
        val gravas = if (instrument.strings >= 6) 2 else instrument.strings / 2 - 1
        for (s in 0..gravas) {
            val f = candidates.firstOrNull { (openPc[s] + it) % 12 == bassPc } ?: continue
            val newFrets = shape.frets.toMutableList()
            for (i in 0 until s) newFrets[i] = -1
            newFrets[s] = f
            // Debe seguir sonando algo del acorde por encima del bajo.
            if (newFrets.drop(s + 1).none { it >= 0 }) continue
            val newFingers = shape.fingers.toMutableList().also {
                for (i in 0..s) if (i < it.size) it[i] = 0
            }
            // Las cejillas que quedaran colgando sobre cuerdas mudas se descartan.
            val newBarres = shape.barres.filter { b ->
                val hi = maxOf(b.fromString, b.toString)
                instrument.strings - hi >= s
            }
            return ChordShape(newFrets, newFingers, newBarres, shape.customId)
        }
        return null
    }

    /**
     * Greedy chord-voicing generator used when no manual template exists.
     *
     * Slides a 4-fret window from fret 0 upward. For each string it picks the
     * lowest fret in the window that hits a target pitch class, preferring
     * notes not yet covered. The lowest sounded string must be the root.
     *
     * Permits omitting the 5th if every other tone is covered (common practice
     * for extended chords on a 6-string guitar).
     */
    private fun autoVoicing(
        rootIdx: Int,
        intervals: Set<Int>,
        instrument: Instrument = Instrument.DEFAULT,
        desde: Int = 0
    ): ChordShape? {
        val openPc = instrument.openPitchClasses
        val cuerdas = instrument.strings
        val targetPcs = intervals.map { ((rootIdx + it) % 12 + 12) % 12 }.toSet()
        val rootPc = rootIdx % 12
        val fifthPc = ((rootIdx + 7) % 12 + 12) % 12
        /*
         * Que la cuerda más grave lleve la fundamental es una regla de la
         * GUITARRA. En el ukelele la afinación estándar es reentrante —la cuarta
         * cuerda suena más aguda que la tercera—, así que ahí no hay "bajo" que
         * valga: exigirlo devolvía posturas altísimas en vez del Do al aire que
         * toca todo el mundo.
         */
        val exigirRaizEnElBajo = instrument == Instrument.GUITAR
        // Con cuatro cuerdas no caben todas las notas de un acorde extendido:
        // se pide una menos que cuerdas hay, y nunca menos de tres.
        val minimoSonando = minOf(3, cuerdas)

        for (baseFret in desde..12) {
            val low = if (baseFret <= 4) 0 else baseFret
            val high = baseFret + 4
            val frets = IntArray(cuerdas) { -1 }
            val pcsUsed = mutableSetOf<Int>()
            var bassAssigned = false

            for (s in 0 until cuerdas) {
                val open = openPc[s]
                val range = (low..high).toList()
                if (exigirRaizEnElBajo && !bassAssigned) {
                    val f = range.firstOrNull { ((open + it) % 12) == rootPc }
                    if (f != null) {
                        frets[s] = f
                        pcsUsed += rootPc
                        bassAssigned = true
                    }
                    continue
                }
                val missing = targetPcs - pcsUsed
                val matchMissing = range.firstOrNull { ((open + it) % 12) in missing }
                if (matchMissing != null) {
                    frets[s] = matchMissing
                    pcsUsed += (open + matchMissing) % 12
                    continue
                }
                val anyTarget = range.firstOrNull { ((open + it) % 12) in targetPcs }
                if (anyTarget != null) {
                    frets[s] = anyTarget
                    pcsUsed += (open + anyTarget) % 12
                }
            }

            if (exigirRaizEnElBajo && !bassAssigned) continue
            val sounded = frets.count { it >= 0 }
            if (sounded < minimoSonando) continue

            val essential = targetPcs - setOf(fifthPc)
            if (!pcsUsed.containsAll(essential)) continue
            // La fundamental tiene que sonar en alguna parte, aunque no sea abajo.
            if (rootPc !in pcsUsed) continue
            if (exigirRaizEnElBajo) {
                val lowestIdx = frets.indexOfFirst { it >= 0 }
                val bassPc = (openPc[lowestIdx] + frets[lowestIdx]) % 12
                if (bassPc != rootPc) continue
            }

            return ChordShape(frets.toList())
        }
        return null
    }

    private fun buildFromTemplate(tpl: Template, rootFret: Int, barreString: Int): ChordShape {
        val frets = tpl.offsets.map { if (it < 0) -1 else it + rootFret }
        // A real barre only exists when two or more strings sit on the root fret
        // (offset 0). Shapes like dim7/m7b5/6/aug touch the root fret on a single
        // string, so a full barre there is spurious and misdraws the fingering.
        val barredStrings = tpl.offsets.count { it == 0 }
        val barres = if (tpl.barre && barredStrings >= 2 && rootFret in 1..12) {
            val list = mutableListOf(Barre(rootFret, barreString, 1))
            tpl.extraBarre?.let { list += it.copy(fret = it.fret + rootFret) }
            list
        } else emptyList()
        val fingers = tpl.barreFingers
        return ChordShape(frets, fingers, barres)
    }

    fun all(instrument: Instrument = Instrument.DEFAULT): List<Chord> {
        val list = mutableListOf<Chord>()
        for (r in ROOTS) {
            for (q in QUALITIES) {
                build(r, q, r + q, null, instrument)?.let { list += it }
            }
        }
        return list
    }
}
