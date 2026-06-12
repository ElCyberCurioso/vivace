package com.guitarchords.app.data

import android.content.Context

/** Recuerda (global) el criterio de ordenación elegido para las carpetas. */
class SongSortPrefs(context: Context) {
    private val sp = context.getSharedPreferences("song_sort", Context.MODE_PRIVATE)

    var sort: SongSort
        get() = runCatching {
            SongSort.valueOf(sp.getString(KEY, null) ?: SongSort.MANUAL.name)
        }.getOrDefault(SongSort.MANUAL)
        set(value) { sp.edit().putString(KEY, value.name).apply() }

    private companion object {
        const val KEY = "sort_mode"
    }
}
