package com.guitarchords.app.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Respuesta de /auth/login y /auth/register. */
@Serializable
data class AuthResponse(val token: String, val user: RemoteUser)

@Serializable
data class RemoteUser(
    val id: String,
    val email: String,
    val name: String = "",
    val role: String = "user"
)

/** Ficha de partitura tal y como la devuelve la API. */
@Serializable
data class RemoteSong(
    val id: String,
    /** Clave interna en R2; solo llega si la partitura es tuya. */
    val r2Key: String? = null,
    val ownerId: String = "",
    val ownerName: String? = null,
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val capo: Int = 0,
    val sourceUrl: String = "",
    val youtubeUrl: String = "",
    val locked: Boolean = false,
    val visibility: String = "private",
    /** Carpeta, favorito y orden: ya son campos, no cabeceras del texto. */
    val favorite: Boolean = false,
    val position: Int = 0,
    val playlistId: String? = null,
    /** Revisión del servidor; se devuelve como `baseRev` al subir. */
    val rev: Int = 1,
    /** >0 = el servidor la da por borrada (lápida). */
    val deletedAt: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    /** Texto de la partitura, incrustado por el feed de cambios. */
    val content: String = ""
)

@Serializable
data class RemotePlaylist(
    val id: String,
    val name: String = "",
    val position: Int = 0,
    val deletedAt: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class RemoteVersion(
    val id: String,
    val songId: String = "",
    val name: String = "",
    val capo: Int = 0,
    val sourceUrl: String = "",
    val position: Int = 0,
    val rev: Int = 1,
    val deletedAt: Long = 0,
    val updatedAt: Long = 0,
    val content: String = ""
)

/** Una tanda del feed de cambios, con su cursor para pedir la siguiente. */
@Serializable
data class ChangeSet<T>(
    val items: List<T> = emptyList(),
    val cursor: String = "",
    val more: Boolean = false
)

@Serializable
data class ChangesResponse(
    val serverTime: Long = 0,
    val playlists: ChangeSet<RemotePlaylist> = ChangeSet(),
    val songs: ChangeSet<RemoteSong> = ChangeSet(),
    val versions: ChangeSet<RemoteVersion> = ChangeSet(),
    val more: Boolean = false
)

/** Cursores guardados entre sincronizaciones (uno por flujo). */
data class SyncCursors(
    val playlists: String = "",
    val songs: String = "",
    val versions: String = ""
)

/* ---- lo que se sube ---- */

@Serializable
data class PushPlaylist(
    val clientId: String,
    val id: String? = null,
    val name: String = "",
    val position: Int = 0,
    val deleted: Boolean = false
)

@Serializable
data class PushSong(
    val clientId: String,
    val id: String? = null,
    /** Revisión que el cliente creía tener; 0 = no comprobar. */
    val baseRev: Int = 0,
    val title: String = "",
    val artist: String = "",
    val genre: String = "",
    val capo: Int = 0,
    val sourceUrl: String = "",
    val locked: Boolean = false,
    val favorite: Boolean = false,
    val position: Int = 0,
    val playlistId: String? = null,
    /** Si la carpeta se crea en este mismo lote, se referencia por su clientId. */
    val playlistClientId: String? = null,
    val content: String = "",
    val deleted: Boolean = false,
    /** true = borrado definitivo (se va la fila y el objeto). */
    val purge: Boolean = false
)

@Serializable
data class PushVersion(
    val clientId: String,
    val id: String? = null,
    val songId: String? = null,
    val songClientId: String? = null,
    val name: String = "",
    val capo: Int = 0,
    val sourceUrl: String = "",
    val position: Int = 0,
    val content: String = "",
    val deleted: Boolean = false
)

@Serializable
data class PushRequest(
    val playlists: List<PushPlaylist> = emptyList(),
    val songs: List<PushSong> = emptyList(),
    val versions: List<PushVersion> = emptyList()
)

/** Resultado de UN elemento del lote. */
@Serializable
data class PushOutcome(
    val clientId: String? = null,
    val ok: Boolean = false,
    val id: String? = null,
    val rev: Int = 0,
    val updatedAt: Long = 0,
    val deleted: Boolean = false,
    val purged: Boolean = false,
    val skipped: Boolean = false,
    /** El servidor cambió mientras tanto: viene su copia para no perder nada. */
    val conflict: Boolean = false,
    /** La partitura ya no existe en el servidor. */
    val gone: Boolean = false,
    val server: RemoteSong? = null,
    val error: String = ""
)

@Serializable
data class PushResponse(
    val serverTime: Long = 0,
    val playlists: List<PushOutcome> = emptyList(),
    val songs: List<PushOutcome> = emptyList(),
    val versions: List<PushOutcome> = emptyList()
)

@Serializable
private data class ErrorResponse(val error: String = "")

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class RegisterRequest(val email: String, val password: String, val name: String)

/** El servidor rechazó la sesión (token caducado o revocado). */
class UnauthorizedException(message: String) : IOException(message)

/**
 * Cliente de la API de Vivace: autenticación por email/contraseña y acceso a
 * las partituras del usuario. Sustituyó al token compartido, ya retirado.
 */
class VivaceClient(baseUrl: String, private val token: String = "") {

    private val base = baseUrl.trim().trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ---- autenticación ----
    suspend fun login(email: String, password: String): AuthResponse = json.decodeFromString(
        request(
            "POST", "/auth/login",
            json.encodeToString(LoginRequest.serializer(), LoginRequest(email, password))
        )
    )

    suspend fun register(email: String, password: String, name: String): AuthResponse =
        json.decodeFromString(
            request(
                "POST", "/auth/register",
                json.encodeToString(RegisterRequest.serializer(), RegisterRequest(email, password, name))
            )
        )

    suspend fun me(): RemoteUser =
        json.decodeFromString<Map<String, RemoteUser>>(request("GET", "/auth/me"))["user"]
            ?: throw IOException("respuesta inesperada")

    // ---- partituras ----
    /**
     * Todo lo que cambió desde los cursores dados, con el texto ya incrustado.
     * Sustituye al "listar todo y pedir cada canción aparte" de antes.
     */
    suspend fun changes(cursors: SyncCursors, limit: Int = 20): ChangesResponse {
        val q = buildString {
            append("?limit=").append(limit)
            if (cursors.playlists.isNotBlank()) append("&playlists=").append(encode(cursors.playlists))
            if (cursors.songs.isNotBlank()) append("&songs=").append(encode(cursors.songs))
            if (cursors.versions.isNotBlank()) append("&versions=").append(encode(cursors.versions))
        }
        return json.decodeFromString(request("GET", "/api/sync/changes$q"))
    }

    /** Sube una tanda. Cada elemento responde por separado (puede haber conflictos). */
    suspend fun push(body: PushRequest): PushResponse = json.decodeFromString(
        request("POST", "/api/sync/push", json.encodeToString(PushRequest.serializer(), body))
    )

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    // ---- acordes personalizados (un blob por usuario) ----
    suspend fun getChords(): String = request("GET", "/api/chords")

    suspend fun putChords(body: String) { request("PUT", "/api/chords", body, "application/json") }

    private suspend fun request(
        method: String,
        path: String,
        body: String? = null,
        contentType: String = "application/json; charset=utf-8"
    ): String = withContext(Dispatchers.IO) {
        if (base.isBlank()) throw IOException("Configura la dirección del servidor")
        val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                return@withContext conn.inputStream.bufferedReader().use { it.readText() }
            }
            val raw = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val message = runCatching {
                json.decodeFromString<ErrorResponse>(raw).error
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "HTTP $code"
            if (code == 401) throw UnauthorizedException(message)
            throw IOException(message)
        } finally {
            conn.disconnect()
        }
    }
}
