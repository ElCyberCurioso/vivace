package com.guitarchords.app.sync

import com.guitarchords.app.data.Song

/**
 * Plain-text (.txt) file format for a single song stored in R2.
 *
 * A header block of `#key: value` lines, an optional `---` separator,
 * then the song body (lyrics + {chords} + {tab} blocks).
 *
 * Example:
 * ```
 * #title: Wonderwall
 * #artist: Oasis
 * #genre: Rock
 * #capo: 2
 * #playlist: Favoritas
 * ---
 * {Em7}Today is gonna be the day...
 * ```
 */
object SongTextFormat {

    private val HEADER = Regex("^#([A-Za-z]+):[ \\t]?(.*)$")

    /**
     * Título por defecto de una canción sin nombre. Es un valor PERSISTIDO (va
     * a la base de datos y al fichero .txt del bucket), así que no se localiza:
     * la sincronización lo compara para saber si el título aún no se ha puesto.
     */
    const val UNTITLED = "Sin título"

    data class Parsed(
        val title: String,
        val artist: String,
        val genre: String,
        val capo: Int,
        val favorite: Boolean,
        val locked: Boolean,
        val playlist: String?,
        val sourceUrl: String,
        val content: String
    )

    /**
     * Serializa la partitura para el bucket.
     *
     * Ya NO se escriben `#favorite:` ni `#playlist:`: el favorito y la carpeta
     * son campos de la API desde que la base los conoce. Se dejaban ahí porque
     * el servidor no tenía dónde guardarlos, y el efecto era que la web no podía
     * enseñarlos y que cualquier edición desde el navegador podía perderlos.
     * [decode] los sigue leyendo para los ficheros antiguos que aún los llevan.
     */
    fun encode(song: Song): String = buildString {
        append("#title: ").append(song.title).append('\n')
        if (song.artist.isNotBlank()) append("#artist: ").append(song.artist).append('\n')
        if (song.genre.isNotBlank()) append("#genre: ").append(song.genre).append('\n')
        if (song.capo > 0) append("#capo: ").append(song.capo).append('\n')
        if (song.locked) append("#locked: true\n")
        if (song.sourceUrl.isNotBlank()) append("#url: ").append(song.sourceUrl).append('\n')
        append("---\n")
        append(song.content)
    }

    fun decode(text: String): Parsed {
        val lines = text.replace("\r\n", "\n").split('\n')
        var title = ""
        var artist = ""
        var genre = ""
        var capo = 0
        var favorite = false
        var locked = false
        var playlist: String? = null
        var sourceUrl = ""
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (line.trim() == "---") { i++; break }
            val m = HEADER.find(line)
            if (m == null) {
                // No explicit separator: header ends at the first non-#key line.
                break
            }
            val key = m.groupValues[1].lowercase()
            val value = m.groupValues[2].trim()
            when (key) {
                "title" -> title = value
                "artist" -> artist = value
                "genre" -> genre = value
                "capo" -> capo = value.toIntOrNull()?.coerceIn(0, 12) ?: 0
                "favorite" -> favorite = value.equals("true", ignoreCase = true)
                "locked" -> locked = value.equals("true", ignoreCase = true)
                "playlist" -> playlist = value.ifBlank { null }
                "url" -> sourceUrl = value
            }
            i++
        }
        val content = lines.drop(i).joinToString("\n")
        return Parsed(
            title = title.ifBlank { UNTITLED },
            artist = artist,
            genre = genre,
            capo = capo,
            favorite = favorite,
            locked = locked,
            playlist = playlist,
            sourceUrl = sourceUrl,
            content = content
        )
    }
}
