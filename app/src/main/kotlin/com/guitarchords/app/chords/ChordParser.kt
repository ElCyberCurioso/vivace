package com.guitarchords.app.chords

data class ChordToken(val chord: String, val position: Int)

data class RenderedLine(
    val lyric: String,
    val chords: List<ChordToken>
) {
    val isEmpty: Boolean get() = lyric.isBlank() && chords.isEmpty()
    val chordOnly: Boolean get() = lyric.isBlank() && chords.isNotEmpty()
}

sealed class ContentBlock {
    data class Lyric(val line: RenderedLine) : ContentBlock()
    data class Tab(val rows: List<String>) : ContentBlock()
}

object ChordParser {

    const val TAB_OPEN = "{tab}"
    const val TAB_CLOSE = "{/tab}"

    fun parse(content: String): List<RenderedLine> =
        parseBlocks(content).mapNotNull { (it as? ContentBlock.Lyric)?.line }

    fun parseBlocks(content: String): List<ContentBlock> {
        val out = mutableListOf<ContentBlock>()
        val buf = mutableListOf<String>()
        var inTab = false
        for (raw in content.split('\n')) {
            val t = raw.trim()
            when {
                t.equals(TAB_OPEN, ignoreCase = true) -> {
                    inTab = true
                    buf.clear()
                }
                t.equals(TAB_CLOSE, ignoreCase = true) -> {
                    inTab = false
                    out += ContentBlock.Tab(buf.toList())
                    buf.clear()
                }
                inTab -> buf += raw
                else -> out += ContentBlock.Lyric(parseLine(raw))
            }
        }
        if (inTab && buf.isNotEmpty()) out += ContentBlock.Tab(buf.toList())
        return out
    }

    fun tabTemplate(): String = buildString {
        append(TAB_OPEN).append('\n')
        append("e|--------------------|\n")
        append("B|--------------------|\n")
        append("G|--------------------|\n")
        append("D|--------------------|\n")
        append("A|--------------------|\n")
        append("E|--------------------|\n")
        append(TAB_CLOSE)
    }

    fun parseLine(line: String): RenderedLine {
        val lyric = StringBuilder()
        val chords = mutableListOf<ChordToken>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '{') {
                val end = line.indexOf('}', i + 1)
                if (end > i) {
                    val name = line.substring(i + 1, end).trim()
                    if (name.isNotEmpty() &&
                        !name.equals("tab", ignoreCase = true) &&
                        !name.equals("/tab", ignoreCase = true)
                    ) {
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
        lines[line] = l.substring(0, col) + "{$chord}" + l.substring(col)
        return lines.joinToString("\n")
    }
}
