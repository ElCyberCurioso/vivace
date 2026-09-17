package com.guitarchords.app.chords

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Elección de digitación por partitura. Es un formato COMPARTIDO con la web y
 * con el servidor, así que lo que se prueba aquí no es un capricho de la app:
 * si estas reglas se separan de las de `varianteDe` en la web, la misma
 * partitura se dibujaría distinta en el móvil y en el navegador.
 */
class ChordVariantsTest {

    @Test
    fun `sin elecciones se pinta la primera`() {
        assertEquals(0, ChordVariants.indexFor("", Instrument.GUITAR, "F", 4))
        assertEquals(0, ChordVariants.indexFor(null, Instrument.GUITAR, "F", 4))
    }

    @Test
    fun `la eleccion guardada manda`() {
        val json = """{"guitarra":{"F":2}}"""
        assertEquals(2, ChordVariants.indexFor(json, Instrument.GUITAR, "F", 4))
    }

    @Test
    fun `cada instrumento va por su lado`() {
        val json = """{"guitarra":{"F":2},"ukelele":{"F":1}}"""
        assertEquals(2, ChordVariants.indexFor(json, Instrument.GUITAR, "F", 4))
        assertEquals(1, ChordVariants.indexFor(json, Instrument.UKULELE, "F", 4))
        // Un acorde elegido en guitarra no dice nada del ukelele.
        assertEquals(
            0,
            ChordVariants.indexFor("""{"guitarra":{"Bb":3}}""", Instrument.UKULELE, "Bb", 4)
        )
    }

    @Test
    fun `un indice que se sale vuelve a la primera`() {
        // El diccionario cambió y aquella cuarta postura ya no existe: mejor la
        // primera que un hueco en blanco.
        val json = """{"guitarra":{"F":9}}"""
        assertEquals(0, ChordVariants.indexFor(json, Instrument.GUITAR, "F", 3))
        assertEquals(0, ChordVariants.indexFor(json, Instrument.GUITAR, "F", 0))
    }

    @Test
    fun `un JSON roto no rompe el visor`() {
        for (basura in listOf("{", "no es json", "[]", """{"guitarra":"dos"}""")) {
            assertEquals(0, ChordVariants.indexFor(basura, Instrument.GUITAR, "F", 4))
        }
    }

    @Test
    fun `elegir una postura la guarda y elegir la primera la retira`() {
        val conEleccion = ChordVariants.withChoice("", Instrument.GUITAR, "F", 2)
        assertEquals(2, ChordVariants.indexFor(conEleccion, Instrument.GUITAR, "F", 4))

        val vuelta = ChordVariants.withChoice(conEleccion, Instrument.GUITAR, "F", 0)
        assertEquals("", vuelta)
    }

    @Test
    fun `sin elecciones se guarda cadena vacia, no un objeto vacio`() {
        // Importa: la columna del servidor nace con cadena vacía, y mandar "{}"
        // sería decir que hay elecciones cuando no las hay.
        assertEquals("", ChordVariants.encode(emptyMap()))
        assertEquals("", ChordVariants.encode(mapOf("guitarra" to emptyMap())))
        assertEquals("", ChordVariants.encode(mapOf("guitarra" to mapOf("F" to 0))))
    }

    @Test
    fun `lo guardado se vuelve a leer igual`() {
        val ida = ChordVariants.withChoice(
            ChordVariants.withChoice("", Instrument.GUITAR, "F", 2),
            Instrument.UKULELE, "Bb", 1
        )
        val vuelta = ChordVariants.decode(ida)
        assertEquals(mapOf("F" to 2), vuelta["guitarra"])
        assertEquals(mapOf("Bb" to 1), vuelta["ukelele"])
    }

    @Test
    fun `las claves son las mismas que usan la web y el servidor`() {
        // "guitarra" y "ukelele", en español: son las que escribe el servidor en
        // `songs.chord_variants` y las que lee la web. Cambiarlas aquí dejaría
        // las elecciones del navegador invisibles en el móvil.
        assertEquals("guitarra", Instrument.GUITAR.id)
        assertEquals("ukelele", Instrument.UKULELE.id)
        assertTrue(ChordVariants.withChoice("", Instrument.GUITAR, "F", 2).contains("\"guitarra\""))
    }
}
