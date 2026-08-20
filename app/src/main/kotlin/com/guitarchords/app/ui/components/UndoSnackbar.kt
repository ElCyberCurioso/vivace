package com.guitarchords.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.guitarchords.app.R
import kotlinx.coroutines.launch

/**
 * Devuelve una función para avisar de una acción reversible: muestra un
 * Snackbar con botón de deshacer y, si el usuario lo pulsa, ejecuta [onUndo].
 *
 * Si ya había un aviso en pantalla se descarta, de modo que al borrar varias
 * canciones seguidas siempre se puede deshacer la última.
 */
@Composable
fun rememberUndoSnackbar(
    host: SnackbarHostState
): (message: String, undoLabel: String, onUndo: () -> Unit) -> Unit {
    val scope = rememberCoroutineScope()
    return remember(host, scope) {
        { message, undoLabel, onUndo ->
            scope.launch {
                host.currentSnackbarData?.dismiss()
                val result = host.showSnackbar(
                    message = message,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) onUndo()
            }
        }
    }
}

/**
 * Texto "N partitura(s) movida(s) a la papelera", resoluble fuera de un
 * composable (los avisos se lanzan desde callbacks de botones y gestos).
 */
@Composable
fun rememberTrashedMessage(): (Int) -> String {
    val res = LocalContext.current.resources
    return remember(res) {
        { count -> res.getQuantityString(R.plurals.snackbar_trashed, count, count) }
    }
}
