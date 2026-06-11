package com.guitarchords.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5D4037),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF795548),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFDBD2),
    onSecondaryContainer = Color(0xFF2B1A14),
    tertiary = Color(0xFF7A5900),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA3),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFFBF8),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF0DFD7),
    onSurfaceVariant = Color(0xFF52443D)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFD7A587),
    onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF5D4037),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFBCAAA4),
    onSecondary = Color(0xFF2B1A14),
    secondaryContainer = Color(0xFF5D4037),
    onSecondaryContainer = Color(0xFFEFDBD2),
    tertiary = Color(0xFFF2BE48),
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDEA3),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF1E1B18),
    onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C2B9)
)

/**
 * Colores semánticos que Material 3 no expone (éxito/aviso). Se adaptan al
 * tema claro/oscuro y deben usarse en lugar de valores hardcodeados — p. ej.
 * el indicador "en tono" del afinador.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color
)

private val LightExtended = ExtendedColors(
    success = Color(0xFF2E7D32),
    onSuccess = Color.White,
    warning = Color(0xFFB26A00),
    onWarning = Color.White
)

private val DarkExtended = ExtendedColors(
    success = Color(0xFF8BC98E),
    onSuccess = Color(0xFF00390C),
    warning = Color(0xFFFFC75A),
    onWarning = Color(0xFF422C00)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

/** Acceso cómodo: `MaterialTheme.extendedColors.success`. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current

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
    val extended = if (dark) DarkExtended else LightExtended
    CompositionLocalProvider(LocalExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
