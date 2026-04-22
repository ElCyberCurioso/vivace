package com.guitarchords.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5D4037),
    onPrimary = Color.White,
    secondary = Color(0xFF795548),
    background = Color(0xFFFFFBF8),
    surface = Color(0xFFFFF8F3)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFD7A587),
    onPrimary = Color(0xFF3E2723),
    secondary = Color(0xFFBCAAA4),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1B18)
)

@Composable
fun GuitarChordsTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
