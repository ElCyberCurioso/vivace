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
    }
}
