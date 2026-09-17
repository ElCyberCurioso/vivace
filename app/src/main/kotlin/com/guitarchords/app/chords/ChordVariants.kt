package com.guitarchords.app.chords

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Qué digitación usa ESTA partitura para cada acorde, por instrumento.
 *
 * El problema que resuelve: un acorde tiene varias posturas y el diccionario
 * devuelve siempre la primera. Para media biblioteca esa primera es una postura
 * alta que no es la que toca quien escribió la partitura, así que la elección se
 * guarda CON LA PARTITURA y no como preferencia de quien lee.
 *
 * Formato, el mismo que ya usan la web y el servidor (`songs.chord_variants`):
 *
 *     {"guitarra":{"F":2},"ukelele":{"Bb":1}}
 *
 * instrumento -> acorde -> índice dentro de la lista de digitaciones. Lo que no
 * está vale 0, que es la primera; por eso elegir la primera BORRA la entrada en
 * vez de guardar un cero, y una partitura sin elecciones se guarda como cadena
 * vacía en lugar de como `{}`.
 *
 * Todo aquí es puro: ni Room, ni red, ni Android. Lo que decide qué se pinta
 * merece poder probarse solo.
 */
object ChordVariants {

    private val json = Json { ignoreUnknownKeys = true }

    /** Mapa vacío si la cadena está vacía o no es el JSON que se espera. */
    fun decode(texto: String?): Map<String, Map<String, Int>> {
        val limpio = texto?.trim().orEmpty()
        if (limpio.isEmpty()) return emptyMap()
        return runCatching {
            val raiz = json.parseToJsonElement(limpio).jsonObject
            raiz.mapValues { (_, porAcorde) ->
                porAcorde.jsonObject.mapNotNull { (acorde, indice) ->
                    indice.jsonPrimitive.intOrNull?.let { acorde to it }
                }.toMap()
            }.filterValues { it.isNotEmpty() }
        }.getOrDefault(emptyMap())
    }

    /** Cadena vacía cuando no hay ninguna elección: así no se guarda ruido. */
    fun encode(variantes: Map<String, Map<String, Int>>): String {
        val utiles = variantes.mapValues { (_, porAcorde) -> porAcorde.filterValues { it > 0 } }
            .filterValues { it.isNotEmpty() }
        if (utiles.isEmpty()) return ""
        val raiz = JsonObject(
            utiles.mapValues { (_, porAcorde) ->
                JsonObject(porAcorde.mapValues { (_, indice) -> JsonPrimitive(indice) })
            }
        )
        return raiz.toString()
    }

    /**
     * Digitación que toca pintar para este acorde, entre las `total` que hay.
     *
     * Un índice que se salga vuelve a la primera en lugar de dejar el hueco en
     * blanco: el diccionario puede haber cambiado desde que se eligió, y una
     * partitura vieja no debe quedarse sin diagrama por eso. Es la misma regla
     * que aplica la web en `varianteDe`.
     */
    fun indexFor(texto: String?, instrument: Instrument, chord: String, total: Int): Int {
        if (total <= 0) return 0
        val elegido = decode(texto)[instrument.id]?.get(chord) ?: return 0
        return if (elegido in 0 until total) elegido else 0
    }

    /**
     * Devuelve la cadena con la elección puesta, lista para guardar. Elegir la
     * primera retira la entrada, que es como se dice «lo de siempre».
     */
    fun withChoice(texto: String?, instrument: Instrument, chord: String, index: Int): String {
        val actual = decode(texto).mapValues { (_, porAcorde) -> porAcorde.toMutableMap() }
            .toMutableMap()
        val delInstrumento = actual.getOrPut(instrument.id) { mutableMapOf() }
        if (index > 0) delInstrumento[chord] = index else delInstrumento.remove(chord)
        return encode(actual)
    }
}
