package com.guitarchords.app.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Talks to the Cloudflare Worker that fronts the R2 bucket.
 *
 * Endpoints (all require `Authorization: Bearer <token>`):
 *  - GET  /list                 -> JSON array of [RemoteObject]
 *  - GET  /object?key=<key>     -> raw text body, headers ETag + X-Uploaded
 *  - PUT  /object?key=<key>     -> stores body, returns [RemoteObject] JSON
 */
class R2Client(baseUrl: String, private val token: String) {

    private val base = baseUrl.trim().trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun list(): List<RemoteObject> = withContext(Dispatchers.IO) {
        val conn = open("/list", "GET")
        try {
            json.decodeFromString<List<RemoteObject>>(conn.readBody())
        } finally {
            conn.disconnect()
        }
    }

    suspend fun get(key: String): RemoteContent = withContext(Dispatchers.IO) {
        val conn = open("/object?key=" + enc(key), "GET")
        try {
            val text = conn.readBody()
            val etag = conn.getHeaderField("ETag").orEmpty()
            val uploaded = conn.getHeaderField("X-Uploaded")?.toLongOrNull() ?: 0L
            RemoteContent(key, text, etag, uploaded)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun put(key: String, body: String): RemoteObject = withContext(Dispatchers.IO) {
        val conn = open("/object?key=" + enc(key), "PUT")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            json.decodeFromString<RemoteObject>(conn.readBody())
        } finally {
            conn.disconnect()
        }
    }

    private fun open(path: String, method: String): HttpURLConnection {
        if (base.isBlank()) throw IOException("URL del servidor no configurada")
        val conn = URL(base + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        return conn
    }

    private fun HttpURLConnection.readBody(): String {
        val code = responseCode
        if (code !in 200..299) {
            val err = errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IOException("HTTP $code ${responseMessage.orEmpty()} ${err.take(200)}".trim())
        }
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
