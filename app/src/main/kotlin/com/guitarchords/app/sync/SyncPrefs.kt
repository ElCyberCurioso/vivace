package com.guitarchords.app.sync

import android.content.Context

/** Persists the Worker endpoint, auth token and last-sync timestamp. */
class SyncPrefs(context: Context) {

    private val sp = context.getSharedPreferences("guitarchords_sync", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString(KEY_URL, "").orEmpty()
        set(value) { sp.edit().putString(KEY_URL, value).apply() }

    var token: String
        get() = sp.getString(KEY_TOKEN, "").orEmpty()
        set(value) { sp.edit().putString(KEY_TOKEN, value).apply() }

    var lastSync: Long
        get() = sp.getLong(KEY_LAST, 0L)
        set(value) { sp.edit().putLong(KEY_LAST, value).apply() }

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_TOKEN = "token"
        const val KEY_LAST = "last_sync"
    }
}
