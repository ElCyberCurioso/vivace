package com.guitarchords.app.data

import com.guitarchords.app.R

/** Criterio de ordenación de la lista de canciones de una carpeta. */
enum class SongSort(val labelRes: Int) {
    /** Orden manual definido por arrastre (campo position). Valor por defecto. */
    MANUAL(R.string.sort_manual),
    TITLE(R.string.sort_title),
    ARTIST(R.string.sort_artist),
    CREATED(R.string.sort_created),
    UPDATED(R.string.sort_updated)
}

/**
 * Aplica el criterio sobre una lista que YA viene ordenada por position (orden
 * manual de la consulta). Para [SongSort.MANUAL] se devuelve sin tocar.
 * Las fechas ordenan de más reciente a más antigua; texto sin distinguir
 * mayúsculas y dejando los valores vacíos al final.
 */
fun List<Song>.applySort(sort: SongSort): List<Song> = when (sort) {
    SongSort.MANUAL -> this
    SongSort.TITLE -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    SongSort.ARTIST -> sortedWith(
        compareBy<Song> { it.artist.isBlank() }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    )
    SongSort.CREATED -> sortedByDescending { it.createdAt }
    SongSort.UPDATED -> sortedByDescending { it.updatedAt }
}
