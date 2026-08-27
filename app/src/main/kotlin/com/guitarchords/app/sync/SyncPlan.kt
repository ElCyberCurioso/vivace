package com.guitarchords.app.sync

import com.guitarchords.app.data.Song

/**
 * Reglas de sincronización, sin Room ni red, para poder probarlas solas.
 *
 * Sustituye a la antigua `SyncPolicy`, que solo cubría el flujo del token
 * compartido; el de cuenta llevaba las reglas escritas a mano dentro del
 * gestor y sin un solo test.
 */
object SyncPlan {

    /** Qué hacer con una partitura que llega del servidor. */
    enum class PullAction {
        /** No está en el dispositivo: se importa. */
        IMPORT,
        /** Cambió fuera y aquí no: se baja tal cual. */
        DOWNLOAD,
        /** Cambió en los dos sitios: gana el servidor y lo local se guarda aparte. */
        CONFLICT,
        /** El servidor la ha borrado: se manda a la papelera local. */
        DELETE_LOCAL,
        /** Ya está al día: no se toca. */
        UP_TO_DATE,
        /** El servidor la borró y aquí ya no existe (o ya estaba borrada). */
        IGNORE
    }

    /**
     * Decide qué hacer con una partitura remota.
     *
     * @param local copia en el dispositivo, o null si no la hay.
     * @param remoteRev revisión que trae el servidor.
     * @param remoteDeleted true si el servidor la da por borrada.
     *
     * Reglas, en orden:
     *  - Lo borrado en el servidor manda sobre lo que no se ha tocado aquí. Si
     *    aquí SÍ hay cambios sin subir, no se borra: subirán y la resucitarán,
     *    que es lo que el usuario acaba de pedir al editarla.
     *  - Si la revisión no ha cambiado desde la última vez, no hay nada que
     *    bajar aunque la fila esté sucia (lo sucio se sube, no se baja).
     *  - Si cambió fuera y también aquí, es conflicto.
     */
    fun decidePull(local: Song?, remoteRev: Int, remoteDeleted: Boolean): PullAction = when {
        remoteDeleted && local == null -> PullAction.IGNORE
        remoteDeleted && local!!.deletedAt > 0 -> PullAction.IGNORE
        remoteDeleted && local!!.dirty -> PullAction.UP_TO_DATE   // lo local se subirá
        remoteDeleted -> PullAction.DELETE_LOCAL
        local == null -> PullAction.IMPORT
        remoteRev == local.remoteRev -> PullAction.UP_TO_DATE
        local.dirty -> PullAction.CONFLICT
        else -> PullAction.DOWNLOAD
    }

    /**
     * `baseRev` que se manda al subir. 0 significa "no sé cuál era": el servidor
     * lo trata como "no compruebes", que es lo correcto para un alta y para las
     * partituras que se enlazaron antes de que existiera el contador.
     */
    fun baseRevFor(song: Song): Int = if (song.remoteId == null) 0 else song.remoteRev

    /**
     * Nombre de la versión donde se guarda la copia local al chocar. Lleva la
     * fecha para que dos conflictos seguidos no se pisen ni se confundan.
     */
    fun conflictVersionName(timestampIso: String): String = "Conflicto · $timestampIso"

    /**
     * Orden de subida dentro de una tanda: primero las altas y luego el resto.
     *
     * Una partitura nueva que además está en la papelera no se sube: nació y
     * murió sin salir del dispositivo, y mandarla solo para borrarla acto
     * seguido es tráfico y una fila basura en el servidor.
     */
    fun shouldPush(song: Song): Boolean = !(song.remoteId == null && song.deletedAt > 0)
}
