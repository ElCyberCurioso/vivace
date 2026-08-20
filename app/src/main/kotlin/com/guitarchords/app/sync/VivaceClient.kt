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
    val locked: Boolean = false,
    val visibility: String = "private",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
private data class SongsResponse(val songs: List<RemoteSong> = emptyList())

@Serializable
data class SongDetail(val song: RemoteSong, val content: String = "")

@Serializable
private data class SongResponse(val song: RemoteSong)

@Serializable
private data class ErrorResponse(val error: String = "")

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class RegisterRequest(val email: String, val password: String, val name: String)

/** Cuerpo de creación/actualización de partitura. */
@Serializable
private data class SongPayload(
    val title: String,
    val artist: String,
    val genre: String,
    val capo: Int,
    val sourceUrl: String,
    val locked: Boolean,
    val visibility: String,
    val content: String
)

/** El servidor rechazó la sesión (token caducado o revocado). */
class UnauthorizedException(message: String) : IOException(message)

/**
 * Cliente de la API de Vivace: autenticación por email/contraseña y acceso a
 * las partituras del usuario. Sustituye al token compartido de [R2Client].
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
    suspend fun listSongs(): List<RemoteSong> =
        json.decodeFromString<SongsResponse>(request("GET", "/api/songs")).songs

    suspend fun getSong(id: String): SongDetail =
        json.decodeFromString(request("GET", "/api/songs/$id"))

    suspend fun createSong(
        title: String, artist: String, genre: String, capo: Int,
        sourceUrl: String, locked: Boolean, visibility: String, content: String
    ): RemoteSong = json.decodeFromString<SongResponse>(
        request("POST", "/api/songs", payload(title, artist, genre, capo, sourceUrl, locked, visibility, content))
    ).song

    suspend fun updateSong(
        id: String, title: String, artist: String, genre: String, capo: Int,
        sourceUrl: String, locked: Boolean, visibility: String, content: String
    ): RemoteSong = json.decodeFromString<SongResponse>(
        request("PUT", "/api/songs/$id", payload(title, artist, genre, capo, sourceUrl, locked, visibility, content))
    ).song

    // ---- acordes personalizados (un blob por usuario) ----
    suspend fun getChords(): String = request("GET", "/api/chords")

    suspend fun putChords(body: String) { request("PUT", "/api/chords", body, "application/json") }

    private fun payload(
        title: String, artist: String, genre: String, capo: Int,
        sourceUrl: String, locked: Boolean, visibility: String, content: String
    ) = json.encodeToString(
        SongPayload.serializer(),
        SongPayload(title, artist, genre, capo, sourceUrl, locked, visibility, content)
    )

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
