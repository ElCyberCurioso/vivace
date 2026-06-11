package com.guitarchords.app.chords

import com.guitarchords.app.data.CustomChord
import com.guitarchords.app.data.CustomChordDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Caché en memoria de las digitaciones definidas por el usuario. Se carga al
 * arrancar y se mantiene al día tras cada alta/edición/borrado, de modo que
 * [ChordLibrary] puede consultarla de forma síncrona al construir diagramas.
 *
 * [revision] se incrementa con cada cambio: la UI lo observa para volver a
 * resolver el acorde y refrescar el modal sin reiniciar.
 */
object CustomChords {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dao: CustomChordDao? = null

    @Volatile
    private var byKey: Map<String, List<ChordShape>> = emptyMap()

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision

    fun init(dao: CustomChordDao) {
        this.dao = dao
        reload()
    }

    /** Digitaciones del usuario para un acorde ("Bm", "F#maj7"), en orden. */
    fun shapesFor(key: String): List<ChordShape> = byKey[key].orEmpty()

    fun add(key: String, frets: List<Int>) {
        val d = dao ?: return
        scope.launch {
            d.insert(
                CustomChord(
                    chordKey = key,
                    frets = encodeFrets(frets),
                    position = d.nextPosition(key)
                )
            )
            reloadBlocking()
        }
    }

    fun update(id: Long, key: String, frets: List<Int>) {
        val d = dao ?: return
        scope.launch {
            d.update(
                CustomChord(
                    id = id,
                    chordKey = key,
                    frets = encodeFrets(frets),
                    updatedAt = System.currentTimeMillis()
                )
            )
            reloadBlocking()
        }
    }

    fun delete(id: Long) {
        val d = dao ?: return
        scope.launch {
            d.deleteById(id)
            reloadBlocking()
        }
    }

    private fun reload() = scope.launch { reloadBlocking() }

    private suspend fun reloadBlocking() {
        val d = dao ?: return
        byKey = d.all()
            .groupBy { it.chordKey }
            .mapValues { (_, rows) -> rows.mapNotNull { it.toShape() } }
        _revision.value = _revision.value + 1
    }

    private fun encodeFrets(frets: List<Int>): String = frets.joinToString(",")

    private fun CustomChord.toShape(): ChordShape? {
        val parsed = frets.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (parsed.size != 6) return null
        return ChordShape(parsed, List(6) { 0 }, autoBarres(parsed), customId = id)
    }

    /**
     * Deducción de cejilla: si no hay cuerdas al aire y dos o más cuerdas pisan
     * el traste mínimo, se dibuja cejilla en ese traste entre la primera y la
     * última de ellas (x24432 → cejilla 5ª..1ª en el traste 2).
     */
    private fun autoBarres(frets: List<Int>): List<Barre> {
        if (frets.any { it == 0 }) return emptyList()
        val positives = frets.filter { it > 0 }
        val min = positives.minOrNull() ?: return emptyList()
        val idx = frets.indices.filter { frets[it] == min }
        if (idx.size < 2) return emptyList()
        return listOf(Barre(min, 6 - idx.first(), 6 - idx.last()))
    }
}
