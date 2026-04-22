package com.guitarchords.app.chords

data class ChordToken(val chord: String, val position: Int)

data class RenderedLine(
    val lyric: String,
    val chords: List<ChordToken>
) {
    val isEmpty: Boolean get() = lyric.isBlank() && chords.isEmpty()
    val chordOnly: Boolean get() = lyric.isBlank() && chords.isNotEmpty()
}

object ChordParser {

    fun parse(content: String): List<RenderedLine> =
        content.split('\n').map { parseLine(it) }

    fun parseLine(line: String): RenderedLine {
        val lyric = StringBuilder()
        val chords = mutableListOf<ChordToken>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '[') {
                val end = line.indexOf(']', i + 1)
                if (end > i) {
                    val name = line.substring(i + 1, end).trim()
                    if (name.isNotEmpty()) {
                        chords += ChordToken(name, lyric.length)
                    }
                    i = end + 1
                    continue
                }
            }
            lyric.append(c)
            i++
        }
        return RenderedLine(lyric.toString(), chords)
    }

    fun uniqueChords(content: String): List<String> {
        val seen = linkedSetOf<String>()
        parse(content).forEach { line ->
            line.chords.forEach { seen += it.chord }
        }
        return seen.toList()
    }

    fun insertChordAt(content: String, line: Int, column: Int, chord: String): String {
        val lines = content.split('\n').toMutableList()
        if (line !in lines.indices) return content
        val l = lines[line]
        val col = column.coerceIn(0, l.length)
        lines[line] = l.substring(0, col) + "[$chord]" + l.substring(col)
        return lines.joinToString("\n")
    }
}
