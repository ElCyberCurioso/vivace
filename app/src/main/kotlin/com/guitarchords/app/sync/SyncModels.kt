package com.guitarchords.app.sync

/**
 * Resultado de una pasada de sincronización.
 *
 * Ya no hay "pendientes de confirmar": la subida es automática. Lo que sí se
 * cuenta son los conflictos resueltos, porque el usuario merece enterarse de
 * que su versión se guardó aparte en vez de perderse.
 */
data class SyncResult(
    val downloaded: Int = 0,
    val uploaded: Int = 0,
    val deleted: Int = 0,
    val conflicts: List<ResolvedConflict> = emptyList()
) {
    val changed: Boolean get() = downloaded > 0 || uploaded > 0 || deleted > 0
}

/**
 * Una partitura que cambió a la vez aquí y en el servidor. No se pierde nada:
 * gana la del servidor como Original y la copia local se guarda como versión
 * alternativa con este nombre.
 */
data class ResolvedConflict(
    val songId: Long,
    val title: String,
    val versionName: String
)

/** Por qué falló una sincronización, para poder enseñarlo y decidir si reintentar. */
sealed interface SyncFailure {
    /** Sin red o el servidor no responde: se reintenta solo. */
    data class Network(val message: String) : SyncFailure
    /** La sesión ya no vale: hay que volver a entrar, reintentar no arregla nada. */
    data class Unauthorized(val message: String) : SyncFailure
    /** Cualquier otra cosa. */
    data class Other(val message: String) : SyncFailure
}
