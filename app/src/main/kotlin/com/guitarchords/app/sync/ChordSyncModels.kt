package com.guitarchords.app.sync

import kotlinx.serialization.Serializable

/** Clave única del blob de acordes en R2 (fuera del prefijo songs/). */
const val CHORDS_KEY = "chords/custom-chords.json"

/** Un acorde personalizado tal y como viaja en el blob de sincronización. */
@Serializable
data class ChordRecord(
    val uuid: String,
    val key: String,
    val frets: String,
    val position: Int = 0,
    val updatedAt: Long = 0,
    /** Tombstone: el acorde se borró en algún dispositivo. */
    val deleted: Boolean = false
)

/** Documento completo de acordes personalizados (todos, incluidos tombstones). */
@Serializable
data class ChordBundle(
    val version: Int = 1,
    val updatedAt: Long = 0,
    val chords: List<ChordRecord> = emptyList()
)

/** Resultado de fusionar el estado local con el remoto. */
data class ChordMergeResult(
    /** Conjunto resuelto por uuid (lo que debería quedar en ambos lados). */
    val merged: List<ChordRecord>,
    /** Registros del merge que difieren del local: hay que aplicarlos en la DB. */
    val toApplyLocally: List<ChordRecord>,
    /** El merge difiere del remoto: hay que reescribir el blob. */
    val needsPush: Boolean
)
