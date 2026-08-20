package com.guitarchords.app.chords

/**
 * Extrae los acordes reales que usa una partitura, para poder practicarlos
 * (ver "practicar los cambios de esta canción" en el visor).
 */
object SongChords {

    /**
     * Acordes distintos de [content], en orden de aparición y normalizados
     * (bemoles a sostenidos, sin bajo tras la barra: `Bm/F#` cuenta como `Bm`).
     * Los bloques `{tab}` se ignoran, igual que en el visor.
     */
    fun distinctChords(content: String): List<String> {
        val out = LinkedHashSet<String>()
        for (block in ChordParser.parseBlocks(content)) {
            val line = (block as? ContentBlock.Lyric)?.line ?: continue
            for (token in line.chords) {
                val (root, quality) = ChordLibrary.parseName(token.chord) ?: continue
                out += root + quality
            }
        }
        return out.toList()
    }

    /**
     * Pares consecutivos para practicar los cambios: (1→2), (2→3)… y el cierre
     * (último→primero) cuando hay más de dos, que es como suenan en bucle.
     * Con menos de dos acordes no hay cambio que practicar.
     */
    fun changePairs(chords: List<String>): List<Pair<String, String>> {
        if (chords.size < 2) return emptyList()
        val pairs = chords.zipWithNext().toMutableList()
        if (chords.size > 2) pairs += chords.last() to chords.first()
        return pairs
    }
}
