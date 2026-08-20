package com.guitarchords.app.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Preferencia de tema elegida por el usuario. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Estado global del tema (claro/oscuro/sistema), persistido y observable.
 * Se inicializa en [com.guitarchords.app.GuitarChordsApp]; MainActivity observa
 * [mode] para recomponer la app al cambiarlo.
 */
object ThemeController {
    private var prefs: android.content.SharedPreferences? = null

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode

    fun init(context: Context) {
        val sp = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
        prefs = sp
        _mode.value = runCatching {
            ThemeMode.valueOf(sp.getString(KEY, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setMode(mode: ThemeMode) {
        _mode.value = mode
        prefs?.edit()?.putString(KEY, mode.name)?.apply()
    }

    private const val KEY = "mode"
}
