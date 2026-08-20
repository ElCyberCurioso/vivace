package com.guitarchords.app.update

import kotlinx.serialization.Serializable

/**
 * Metadatos de la última versión publicada, servidos por el Worker en
 * `GET /update` (leídos de `app/latest.json` en R2).
 *
 * Ejemplo de `app/latest.json`:
 * ```json
 * { "versionCode": 2, "versionName": "1.1", "notes": "Novedades…", "apkUrl": "/update/apk" }
 * ```
 */
@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String = "",
    val notes: String = "",
    /** Relativa al Worker ("/update/apk") o absoluta ("https://…"). */
    val apkUrl: String = "/update/apk"
)
