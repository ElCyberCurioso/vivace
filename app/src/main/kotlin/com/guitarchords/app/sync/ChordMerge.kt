package com.guitarchords.app.sync

/**
 * Fusión convergente de acordes personalizados entre dos dispositivos vía el
 * blob de R2. Función pura (testeable): para cada `uuid`, gana el registro con
 * mayor `updatedAt`; en empate se prefiere el vivo sobre el tombstone y, si aún
 * empatan, el remoto (estable). Los borrados viajan como tombstones, de modo
 * que también se propagan.
 */
object ChordMerge {

    fun merge(local: List<ChordRecord>, remote: List<ChordRecord>): ChordMergeResult {
        val localByUuid = local.associateBy { it.uuid }
        val remoteByUuid = remote.associateBy { it.uuid }

        val merged = (localByUuid.keys + remoteByUuid.keys).map { uuid ->
            val l = localByUuid[uuid]
            val r = remoteByUuid[uuid]
            when {
                l == null -> r!!
                r == null -> l
                else -> pick(l, r)
            }
        }

        val toApplyLocally = merged.filter { it != localByUuid[it.uuid] }

        // Hace falta subir si el merge no coincide exactamente con el remoto.
        val mergedByUuid = merged.associateBy { it.uuid }
        val needsPush = mergedByUuid.size != remoteByUuid.size ||
            merged.any { it != remoteByUuid[it.uuid] }

        return ChordMergeResult(merged, toApplyLocally, needsPush)
    }

    private fun pick(a: ChordRecord, b: ChordRecord): ChordRecord = when {
        a.updatedAt != b.updatedAt -> if (a.updatedAt > b.updatedAt) a else b
        a.deleted != b.deleted -> if (!a.deleted) a else b
        else -> b // empate total: el remoto, para converger de forma estable
    }
}
