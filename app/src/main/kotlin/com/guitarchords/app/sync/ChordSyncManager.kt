package com.guitarchords.app.sync

import com.guitarchords.app.chords.CustomChords
import com.guitarchords.app.data.CustomChord
import com.guitarchords.app.data.CustomChordDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Sincronización automática y convergente de los acordes personalizados con el
 * Worker R2, mediante un único blob JSON ([CHORDS_KEY]) fuera del flujo de
 * partituras. Resolución último-en-modificar-gana por uuid (ver [ChordMerge]),
 * con propagación de borrados vía tombstones.
 *
 * Disparadores: cambios locales (observa [CustomChords.revision] con debounce),
 * arranque y reconexión de red (la App llama a [sync]). [Mutex] evita solapes.
 */
class ChordSyncManager(
    private val dao: CustomChordDao,
    private val prefs: SyncPrefs
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    data class Outcome(val applied: Int, val pushed: Boolean)

    /** Arranca los disparadores automáticos en el scope dado (App). */
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch { runCatching { sync() } }          // intento inicial
        scope.launch {
            // Cada cambio local (alta/edición/borrado) bumpea revision.
            CustomChords.revision
                .drop(1)
                .debounce(1500)
                .collect { runCatching { sync() } }
        }
    }

    /** Pull → merge → aplica en local → push si procede. Mejor esfuerzo. */
    suspend fun sync(): Outcome = mutex.withLock {
        val base = prefs.baseUrl.trim()
        val token = prefs.authToken.trim()
        // Sin sesión no hay nada que sincronizar: los acordes van por cuenta.
        if (base.isBlank() || token.isBlank()) return@withLock Outcome(0, false)

        val client = VivaceClient(base, token)

        // 1) Descargar el blob de la cuenta (vacío la primera vez).
        val remote: List<ChordRecord> = runCatching {
            json.decodeFromString<ChordBundle>(client.getChords()).chords
        }.getOrDefault(emptyList())

        // 2) Estado local (incluye tombstones).
        val local = dao.allForSync().map { it.toRecord() }

        // 3) Fusionar.
        val result = ChordMerge.merge(local, remote)

        // 4) Aplicar en la DB lo que viene del servidor (queda dirty = 0).
        for (rec in result.toApplyLocally) applyRemote(rec)

        // 5) Subir si el merge difiere del remoto.
        var pushed = false
        if (result.needsPush) {
            val bundle = ChordBundle(updatedAt = System.currentTimeMillis(), chords = result.merged)
            client.putChords(json.encodeToString(ChordBundle.serializer(), bundle))
            pushed = true
        }

        // Tras un sync correcto no quedan cambios locales sin subir.
        dao.clearDirtyAll()
        prefs.chordsLastSync = System.currentTimeMillis()

        // 6) Refrescar la caché para que los diagramas se actualicen en vivo.
        if (result.toApplyLocally.isNotEmpty()) CustomChords.refresh()

        Outcome(result.toApplyLocally.size, pushed)
    }

    private suspend fun applyRemote(rec: ChordRecord) {
        val deletedAt = if (rec.deleted) rec.updatedAt.coerceAtLeast(1) else 0L
        val existing = dao.getByUuid(rec.uuid)
        if (existing == null) {
            dao.insertIgnore(
                CustomChord(
                    chordKey = rec.key,
                    frets = rec.frets,
                    position = rec.position,
                    uuid = rec.uuid,
                    deletedAt = deletedAt,
                    dirty = false,
                    updatedAt = rec.updatedAt
                )
            )
        } else {
            dao.applyRemote(rec.uuid, rec.key, rec.frets, rec.position, deletedAt, rec.updatedAt)
        }
    }

    private fun CustomChord.toRecord() = ChordRecord(
        uuid = uuid,
        key = chordKey,
        frets = frets,
        position = position,
        updatedAt = updatedAt,
        deleted = deletedAt > 0
    )
}
