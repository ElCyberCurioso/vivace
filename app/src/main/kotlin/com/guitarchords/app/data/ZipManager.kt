package com.guitarchords.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun exportToZip(context: Context, exp: PlaylistExport): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = exp.name.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "playlist" }
        val file = File(dir, "$safeName.gtrlist")
        ZipOutputStream(file.outputStream().buffered()).use { zos ->
            val payload = json.encodeToString(exp).toByteArray(Charsets.UTF_8)
            zos.putNextEntry(ZipEntry("playlist.json"))
            zos.write(payload)
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("META-INF/format.txt"))
            zos.write("guitarchords-playlist;v1".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun importFromStream(input: InputStream): PlaylistExport? {
        ZipInputStream(input.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "playlist.json") {
                    val bytes = zis.readBytes()
                    return json.decodeFromString(PlaylistExport.serializer(), String(bytes, Charsets.UTF_8))
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    fun importFromUri(context: Context, uri: Uri): PlaylistExport? {
        context.contentResolver.openInputStream(uri)?.use { return importFromStream(it) }
        return null
    }
}
