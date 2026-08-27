package com.guitarchords.app.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists the Worker endpoint, auth token and last-sync timestamp.
 *
 * El token se guarda en [EncryptedSharedPreferences] (AES-256). Si el keystore
 * falla (p. ej. tras una restauración de backup corrupta) se recurre a prefs
 * normales para no romper la sincronización, regenerando el fichero.
 */
class SyncPrefs(context: Context) {

    private val sp: SharedPreferences = createPrefs(context)

    private fun createPrefs(context: Context): SharedPreferences =
        runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "guitarchords_sync_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse {
            context.getSharedPreferences("guitarchords_sync", Context.MODE_PRIVATE)
        }.also { secure ->
            // Migración única desde las prefs antiguas en texto plano.
            val legacy = context.getSharedPreferences("guitarchords_sync", Context.MODE_PRIVATE)
            if (secure !== legacy && legacy.contains(KEY_TOKEN) && !secure.contains(KEY_TOKEN)) {
                secure.edit()
                    .putString(KEY_URL, legacy.getString(KEY_URL, ""))
                    .putString(KEY_TOKEN, legacy.getString(KEY_TOKEN, ""))
                    .putLong(KEY_LAST, legacy.getLong(KEY_LAST, 0L))
                    .apply()
                legacy.edit().clear().apply()
            }
        }

    var baseUrl: String
        get() = sp.getString(KEY_URL, "").orEmpty()
        set(value) { sp.edit().putString(KEY_URL, value).apply() }

    var token: String
        get() = sp.getString(KEY_TOKEN, "").orEmpty()
        set(value) { sp.edit().putString(KEY_TOKEN, value).apply() }

    var lastSync: Long
        get() = sp.getLong(KEY_LAST, 0L)
        set(value) { sp.edit().putLong(KEY_LAST, value).apply() }

    // ---- sesión de usuario (sustituye al token compartido) ----
    /** JWT devuelto por /auth/login; vacío = sin sesión. */
    var authToken: String
        get() = sp.getString(KEY_AUTH, "").orEmpty()
        set(value) { sp.edit().putString(KEY_AUTH, value).apply() }

    var userEmail: String
        get() = sp.getString(KEY_EMAIL, "").orEmpty()
        set(value) { sp.edit().putString(KEY_EMAIL, value).apply() }

    var userName: String
        get() = sp.getString(KEY_NAME, "").orEmpty()
        set(value) { sp.edit().putString(KEY_NAME, value).apply() }

    val isLoggedIn: Boolean get() = authToken.isNotBlank() && baseUrl.isNotBlank()

    /**
     * Cierra la sesión (no borra las partituras del dispositivo).
     * También olvida los cursores: al volver a entrar —quizá con otra cuenta—
     * hay que rehacer la foto completa, no seguir por donde iba la anterior.
     */
    fun clearSession() {
        sp.edit()
            .remove(KEY_AUTH).remove(KEY_EMAIL).remove(KEY_NAME).remove(KEY_ROLE)
            .remove(KEY_CUR_PLAYLISTS).remove(KEY_CUR_SONGS).remove(KEY_CUR_VERSIONS)
            .apply()
    }

    /** Rol del usuario ('user' | 'editor' | 'admin'), para saber qué ofrecer. */
    var userRole: String
        get() = sp.getString(KEY_ROLE, "user").orEmpty().ifBlank { "user" }
        set(value) { sp.edit().putString(KEY_ROLE, value).apply() }

    /** Último error de sincronización, para poder avisar sin abrir la pantalla. */
    var lastSyncError: String
        get() = sp.getString(KEY_LAST_ERROR, "").orEmpty()
        set(value) { sp.edit().putString(KEY_LAST_ERROR, value).apply() }

    /**
     * Cursores del feed de cambios, uno por flujo. Guardarlos es lo que hace que
     * una sincronización pida solo lo nuevo en vez de el catálogo entero.
     */
    fun cursors() = com.guitarchords.app.sync.SyncCursors(
        playlists = sp.getString(KEY_CUR_PLAYLISTS, "").orEmpty(),
        songs = sp.getString(KEY_CUR_SONGS, "").orEmpty(),
        versions = sp.getString(KEY_CUR_VERSIONS, "").orEmpty()
    )

    fun saveCursors(cursors: com.guitarchords.app.sync.SyncCursors) {
        sp.edit()
            .putString(KEY_CUR_PLAYLISTS, cursors.playlists)
            .putString(KEY_CUR_SONGS, cursors.songs)
            .putString(KEY_CUR_VERSIONS, cursors.versions)
            .apply()
    }

    /** ETag del último blob de acordes sincronizado (concurrencia optimista). */
    var chordsEtag: String
        get() = sp.getString(KEY_CHORDS_ETAG, "").orEmpty()
        set(value) { sp.edit().putString(KEY_CHORDS_ETAG, value).apply() }

    var chordsLastSync: Long
        get() = sp.getLong(KEY_CHORDS_LAST, 0L)
        set(value) { sp.edit().putLong(KEY_CHORDS_LAST, value).apply() }

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_LAST = "last_sync"
        const val KEY_CHORDS_ETAG = "chords_etag"
        const val KEY_CHORDS_LAST = "chords_last_sync"
        const val KEY_AUTH = "auth_token"
        const val KEY_EMAIL = "user_email"
        const val KEY_NAME = "user_name"
        const val KEY_ROLE = "user_role"
        const val KEY_LAST_ERROR = "last_sync_error"
        const val KEY_CUR_PLAYLISTS = "cursor_playlists"
        const val KEY_CUR_SONGS = "cursor_songs"
        const val KEY_CUR_VERSIONS = "cursor_versions"
    }
}
