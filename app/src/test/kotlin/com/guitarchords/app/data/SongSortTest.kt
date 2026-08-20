package com.guitarchords.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SongSortTest {

    private fun song(
        id: Long,
        title: String = "",
        artist: String = "",
        genre: String = "",
        created: Long = 0,
        updated: Long = 0
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        genre = genre,
        createdAt = created,
        updatedAt = updated
    )

    private val list = listOf(
        song(1, title = "beta", artist = "Zaz", created = 100, updated = 300),
        song(2, title = "Alfa", artist = "aria", created = 300, updated = 100),
        song(3, title = "Gamma", artist = "", created = 200, updated = 200)
    )

    @Test
    fun `manual conserva el orden de la consulta`() {
        assertEquals(listOf(1L, 2L, 3L), list.applySort(SongSort.MANUAL).map { it.id })
    }

    @Test
    fun `titulo ordena sin distinguir mayusculas`() {
        assertEquals(listOf(2L, 1L, 3L), list.applySort(SongSort.TITLE).map { it.id })
    }

    @Test
    fun `artista deja los vacios al final`() {
        assertEquals(listOf(2L, 1L, 3L), list.applySort(SongSort.ARTIST).map { it.id })
    }

    @Test
    fun `creacion ordena de mas reciente a mas antigua`() {
        assertEquals(listOf(2L, 3L, 1L), list.applySort(SongSort.CREATED).map { it.id })
    }

    @Test
    fun `modificacion ordena de mas reciente a mas antigua`() {
        assertEquals(listOf(1L, 3L, 2L), list.applySort(SongSort.UPDATED).map { it.id })
    }

    @Test
    fun `consulta vacia no filtra`() {
        assertEquals(3, list.filterByQuery("   ").size)
    }

    @Test
    fun `filtra por titulo artista o genero sin distinguir mayusculas`() {
        assertEquals(listOf(1L), list.filterByQuery("BET").map { it.id })
        assertEquals(listOf(2L), list.filterByQuery("aRi").map { it.id })
        val conGenero = list + song(4, title = "otra", genre = "Rock")
        assertEquals(listOf(4L), conGenero.filterByQuery("rock").map { it.id })
    }

    @Test
    fun `sin coincidencias devuelve vacio`() {
        assertEquals(emptyList<Long>(), list.filterByQuery("zzz").map { it.id })
    }
}
