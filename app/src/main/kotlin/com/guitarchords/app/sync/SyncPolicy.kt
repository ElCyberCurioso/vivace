package com.guitarchords.app.sync

import com.guitarchords.app.data.Song

/** Qué hacer con un objeto remoto durante la fase PULL. */
enum class PullAction {
    /** No existe en el dispositivo: se importa como canción nueva. */
    IMPORT,
    /** Cambió en el servidor y no hay ediciones locales: se descarga. */
    DOWNLOAD,
    /** Cambió en ambos lados: lo resuelve el usuario canción a canción. */
    CONFLICT,
    /** Mismo ETag: solo puede fluir la metadata (título, capo, candado…). */
    METADATA_ONLY,
    /** Está en la papelera: la papelera manda y no se toca nada. */
    SKIP_TRASHED
}

/**
 * Reglas puras de sincronización (sin Room ni red), para poder testearlas.
 */
object SyncPolicy {

    /**
     * Decide qué hacer con una clave remota según su copia local.
     *
     * Una canción en la papelera se ignora por completo: si se descargara
     * "resucitaría" sin que el usuario lo pida, y si generara conflicto pediría
     * resolver algo que ya está borrado. Sigue conservando su `remoteKey`, así
     * que tampoco se reimporta como canción nueva.
     */
    fun decidePull(local: Song?, remoteEtag: String): PullAction = when {
        local == null -> PullAction.IMPORT
        local.deletedAt > 0 -> PullAction.SKIP_TRASHED
        local.remoteEtag != remoteEtag -> if (local.dirty) PullAction.CONFLICT else PullAction.DOWNLOAD
        else -> PullAction.METADATA_ONLY
    }

    /**
     * Canciones locales cuyo objeto remoto ya no existe en el bucket: se les
     * quita el enlace remoto y pasan a ser "solo locales".
     *
     * Si la lista remota llega vacía no se toca nada: no se puede distinguir
     * un bucket vacío de una URL mal configurada, y desenlazar todo sería
     * destructivo.
     */
    fun orphanIds(localsWithRemote: List<Song>, remoteKeys: Set<String>): List<Long> {
        if (remoteKeys.isEmpty()) return emptyList()
        return localsWithRemote
            .filter { it.remoteKey != null && it.remoteKey !in remoteKeys }
            .map { it.id }
    }
}
