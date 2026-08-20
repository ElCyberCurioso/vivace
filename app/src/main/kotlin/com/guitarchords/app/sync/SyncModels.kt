package com.guitarchords.app.sync

import kotlinx.serialization.Serializable

/** An object stored in the R2 bucket, as reported by the Worker. */
@Serializable
data class RemoteObject(
    val key: String,
    val etag: String = "",
    val size: Long = 0,
    val uploaded: Long = 0,
    /** Indexed metadata from the Worker (R2 customMetadata) so it flows without a body download. */
    val title: String = "",
    val artist: String = "",
    val capo: String = "",
    val url: String = "",
    /** "true" si la partitura está bloqueada en el Worker. */
    val locked: String = ""
)

/** Body + metadata of a single downloaded R2 object. */
data class RemoteContent(
    val key: String,
    val text: String,
    val etag: String,
    val uploaded: Long
)

/** A song changed on BOTH the device and the bucket since the last sync. */
data class SyncConflict(
    val songId: Long,
    val title: String,
    val remoteKey: String,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long
)

/** A dirty local song waiting for the user to confirm its upload. */
data class PendingUpload(
    val songId: Long,
    val title: String,
    val isNew: Boolean
)

/** Outcome of a sync run. Uploads only happen later, via an explicit push. */
data class SyncResult(
    val downloaded: Int,
    val uploaded: Int,
    val pendingUploads: List<PendingUpload>,
    val conflicts: List<SyncConflict>
)
