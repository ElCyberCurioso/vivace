package com.guitarchords.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.guitarchords.app.BuildConfig
import com.guitarchords.app.sync.SyncPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Auto-actualización contra el Worker R2 (endpoints públicos `/update` y
 * `/update/apk`). Flujo: comprobar → descargar a la caché → lanzar el
 * instalador del sistema → limpiar el APK. La instalación no es silenciosa:
 * Android muestra su diálogo y el usuario confirma.
 */
object UpdateManager {

    private val json = Json { ignoreUnknownKeys = true }

    /** URL base del Worker: campo de build si está, si no la de sincronización. */
    fun baseUrl(context: Context): String {
        val configured = BuildConfig.UPDATE_BASE_URL.ifBlank { SyncPrefs(context).baseUrl }
        return configured.trim().trimEnd('/')
    }

    /** Devuelve la actualización disponible, o null si ya está al día. */
    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val base = baseUrl(context)
        if (base.isBlank()) throw IOException("Configura la URL del servidor primero")
        val info = json.decodeFromString<UpdateInfo>(httpGet("$base/update"))
        if (info.versionCode > BuildConfig.VERSION_CODE) info else null
    }

    /** Descarga el APK a la caché informando del progreso (0f..1f). */
    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val base = baseUrl(context)
        val url = if (info.apkUrl.startsWith("http")) info.apkUrl else base + info.apkUrl
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Limpia descargas previas antes de empezar.
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "app-${info.versionCode}.apk")

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 60_000
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code al descargar el APK")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            onProgress(1f)
            file
        } finally {
            conn.disconnect()
        }
    }

    /** En Android 8+ el usuario debe permitir "instalar apps de esta fuente". */
    fun canInstall(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Lanza el instalador del sistema sobre el APK descargado. */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /** Borra los APK descargados (se llama al arrancar y antes de descargar). */
    fun cleanup(context: Context) {
        runCatching {
            File(context.cacheDir, "updates").listFiles()?.forEach { it.delete() }
        }
    }

    private fun httpGet(spec: String): String {
        val conn = (URL(spec).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
