package com.guitarchords.app.update

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Comprobación silenciosa de actualizaciones al arrancar (como mucho una vez al
 * día). Si hay versión nueva, [available] la publica y la pantalla de inicio
 * muestra un aviso discreto; nada se descarga sin que el usuario lo pida.
 */
object UpdateController {

    private const val PREFS = "updates"
    private const val KEY_LAST_CHECK_DAY = "last_check_day"

    private val _available = MutableStateFlow<UpdateInfo?>(null)
    val available: StateFlow<UpdateInfo?> = _available

    /** Comprueba si toca (un intento al día) y guarda el resultado. Silencioso. */
    suspend fun checkSilently(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = System.currentTimeMillis() / DAY_MS
        if (prefs.getLong(KEY_LAST_CHECK_DAY, 0) == today) return
        if (UpdateManager.baseUrl(context).isBlank()) return
        runCatching { UpdateManager.check(context) }
            .onSuccess { info ->
                prefs.edit().putLong(KEY_LAST_CHECK_DAY, today).apply()
                _available.value = info
            }
        // Si falla (sin red, servidor caído) no se marca el día: se reintenta.
    }

    /** El usuario ya vio el aviso; no volver a mostrarlo en esta sesión. */
    fun dismiss() { _available.value = null }

    private const val DAY_MS = 24L * 60 * 60 * 1000
}
