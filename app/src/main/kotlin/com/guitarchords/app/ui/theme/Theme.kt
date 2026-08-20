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

/*
 * Paleta Vivace · estilo "Nocturno" (paquete de marca).
 *
 * Los tokens de marca son estos y se llaman igual en la web
 * (`--vv-*`) y en `res/values/colors.xml` (`vv_*`):
 *
 *   bg · surface · surface-alt · border · border-strong
 *   text · text-muted · text-subtle
 *   accent · accent-strong · on-accent · beat · danger
 *
 * Material 3 pide más roles de los que define el paquete (contenedores,
 * niveles de superficie…). Los que no son token se DERIVAN de uno que sí
 * lo es —normalmente el acento al 16 % (oscuro) o al 14 % (claro) aplanado
 * sobre el fondo, que es justo `accent-soft`— y se marcan como derivados.
 *
 * Reglas del paquete que se respetan aquí:
 *  - El ámbar cambia de tono entre modos: #E8B04B en oscuro, #B8791F en
 *    claro. Nunca el ámbar claro sobre fondo claro.
 *  - El verde `beat` es exclusivo de tempo y estado positivo (afinador en
 *    tono, ejercicio superado): entra por `ExtendedColors.success`, no como
 *    color decorativo.
 */

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE8B04B),                 // accent
    onPrimary = Color(0xFF0F1113),               // on-accent
    primaryContainer = Color(0xFF322A1C),        // derivado: accent-soft sobre bg
    onPrimaryContainer = Color(0xFFE8B04B),
    secondary = Color(0xFF7FB2A0),               // beat
    onSecondary = Color(0xFF0F1113),
    secondaryContainer = Color(0xFF212B2A),      // derivado: beat al 16 % sobre bg
    onSecondaryContainer = Color(0xFF7FB2A0),
    tertiary = Color(0xFFC2762B),                // accent-strong
    onTertiary = Color(0xFF0F1113),
    tertiaryContainer = Color(0xFF2C2117),       // derivado: accent-strong al 16 %
    onTertiaryContainer = Color(0xFFE8B04B),
    background = Color(0xFF0F1113),              // bg
    onBackground = Color(0xFFF2EFE9),            // text
    surface = Color(0xFF17191C),                 // surface
    onSurface = Color(0xFFF2EFE9),
    surfaceVariant = Color(0xFF1E2124),          // surface-alt
    onSurfaceVariant = Color(0xFFA7ABB2),        // text-muted
    surfaceContainerLowest = Color(0xFF0B0D0F),  // derivados: escalón de superficie
    surfaceContainerLow = Color(0xFF131518),
    surfaceContainer = Color(0xFF17191C),
    surfaceContainerHigh = Color(0xFF1E2124),
    surfaceContainerHighest = Color(0xFF24272B),
    surfaceDim = Color(0xFF0F1113),
    surfaceBright = Color(0xFF2B2D31),
    outline = Color(0xFF3A3D42),                 // border-strong
    outlineVariant = Color(0xFF2B2D31),          // border
    error = Color(0xFFE0654B),                   // danger
    onError = Color(0xFF0F1113),
    errorContainer = Color(0xFF301E1C),          // derivado: danger al 16 % sobre bg
    onErrorContainer = Color(0xFFE0654B),
    inverseSurface = Color(0xFFF2EFE9),
    inverseOnSurface = Color(0xFF17191C),
    inversePrimary = Color(0xFFB8791F),          // el ámbar del modo claro
    scrim = Color(0xFF000000)
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFB8791F),                 // accent (claro)
    onPrimary = Color(0xFFFFFFFF),               // on-accent
    primaryContainer = Color(0xFFEDE3D2),        // derivado: accent-soft sobre bg
    onPrimaryContainer = Color(0xFF8F5A12),
    secondary = Color(0xFF2F6B5B),               // beat
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE1DA),      // derivado: beat al 14 % sobre bg
    onSecondaryContainer = Color(0xFF234F43),
    tertiary = Color(0xFF8F5A12),                // accent-strong
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEFE0CC),       // derivado
    onTertiaryContainer = Color(0xFF5E3A0B),
    background = Color(0xFFF6F4EF),              // bg
    onBackground = Color(0xFF16181A),            // text
    surface = Color(0xFFFFFFFF),                 // surface
    onSurface = Color(0xFF16181A),
    surfaceVariant = Color(0xFFEFEDE7),          // surface-alt
    onSurfaceVariant = Color(0xFF4A4D52),        // text-muted
    surfaceContainerLowest = Color(0xFFFFFFFF),  // derivados: escalón de superficie
    surfaceContainerLow = Color(0xFFFBF9F5),
    surfaceContainer = Color(0xFFF2F0EA),
    surfaceContainerHigh = Color(0xFFEFEDE7),
    surfaceContainerHighest = Color(0xFFE9E6DE),
    surfaceDim = Color(0xFFE7E4DC),
    surfaceBright = Color(0xFFFFFFFF),
    outline = Color(0xFFD5D0C5),                 // border-strong
    outlineVariant = Color(0xFFE2DED5),          // border
    error = Color(0xFFB33F26),                   // danger
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF6DED6),          // derivado
    onErrorContainer = Color(0xFF7A2A19),
    inverseSurface = Color(0xFF16181A),
    inverseOnSurface = Color(0xFFF6F4EF),
    inversePrimary = Color(0xFFE8B04B),          // el ámbar del modo oscuro
    scrim = Color(0xFF000000)
)

/**
 * Colores semánticos que Material 3 no expone (éxito/aviso). Se adaptan al
 * tema claro/oscuro y deben usarse en lugar de valores hardcodeados — p. ej.
 * el indicador "en tono" del afinador.
 *
 * `success` es el verde `beat` del paquete de marca, reservado a tempo y
 * estado positivo; `warning` es el ámbar del acento.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color
)

private val LightExtended = ExtendedColors(
    success = Color(0xFF2F6B5B),                 // beat (claro)
    onSuccess = Color(0xFFFFFFFF),
    warning = Color(0xFFB8791F),                 // accent (claro)
    onWarning = Color(0xFFFFFFFF)
)

private val DarkExtended = ExtendedColors(
    success = Color(0xFF7FB2A0),                 // beat (oscuro)
    onSuccess = Color(0xFF0F1113),
    warning = Color(0xFFE8B04B),                 // accent (oscuro)
    onWarning = Color(0xFF0F1113)
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
