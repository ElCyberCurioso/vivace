package com.guitarchords.app.chords

object ChordTransposer {
    private val CHROMA = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    private val CHROMA_FLAT = listOf("C","Db","D","Eb","E","F","Gb","G","Ab","A","Bb","B")
    private val FLAT = mapOf(
        "Db" to "C#", "Eb" to "D#", "Gb" to "F#",
        "Ab" to "G#", "Bb" to "A#", "Cb" to "B", "Fb" to "E"
    )

    /**
     * Transpone un acorde [semitones] semitonos. Con [preferFlats] los
     * resultados alterados se escriben con bemoles (Bb) en vez de sostenidos (A#).
     */
    fun transposeChord(name: String, semitones: Int, preferFlats: Boolean = false): String {
        if ((semitones == 0 && !preferFlats) || name.isEmpty()) return name
        val slash = name.indexOf('/')
        if (slash >= 0) {
            val main = transposeChord(name.substring(0, slash), semitones, preferFlats)
            val bass = transposeChord(name.substring(slash + 1), semitones, preferFlats)
            return "$main/$bass"
        }
        val root = when {
            name.length >= 2 && (name[1] == '#' || name[1] == 'b') -> name.substring(0, 2)
            else -> name.substring(0, 1)
        }
        val suffix = name.substring(root.length)
        val normalized = FLAT[root] ?: root
        val idx = CHROMA.indexOf(normalized)
        if (idx < 0) return name
        val shifted = ((idx + semitones) % 12 + 12) % 12
        val scale = if (preferFlats) CHROMA_FLAT else CHROMA
        return scale[shifted] + suffix
    }

    fun transposeContent(content: String, semitones: Int, preferFlats: Boolean = false): String {
        if (semitones == 0 && !preferFlats) return content
        val sb = StringBuilder()
        var i = 0
        var inTab = false
        while (i < content.length) {
            val c = content[i]
            if (c == '{') {
                val end = content.indexOf('}', i + 1)
                if (end > i) {
                    val inner = content.substring(i + 1, end).trim()
                    when {
                        inner.equals("tab", ignoreCase = true) -> {
                            inTab = true
                            sb.append(content, i, end + 1)
                        }
                        inner.equals("/tab", ignoreCase = true) -> {
                            inTab = false
                            sb.append(content, i, end + 1)
                        }
                        inTab -> sb.append(content, i, end + 1)
                        else -> sb.append('{').append(transposeChord(inner, semitones, preferFlats)).append('}')
                    }
                    i = end + 1
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }
}
