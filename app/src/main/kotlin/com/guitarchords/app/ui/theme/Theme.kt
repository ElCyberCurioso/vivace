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
 * Paleta Accordio · el mismo paquete de marca que la web (accordio-web-kit).
 *
 * Los valores salen tal cual de `tokens.css` y `tokens.dark.css`, y son los
 * mismos que gastan `worker/src/web-html.js` y `res/values/colors.xml`. Si el
 * kit cambia, se cambian los tres sitios.
 *
 * Lo importante del kit no son los colores sueltos, son los ROLES, porque son
 * los que cambian de tema: `action` es la acción principal, `highlight` lo que
 * llama la atención, `active` lo seleccionado y `pending` el estado. La marca no
 * se invierte entre modos: lo que cambia es quién hace de acción. En claro el
 * teal; en oscuro el teal no contrasta contra el fondo, pasa a titular y la
 * acción la toma el turquesa.
 *
 * Material 3 pide más roles de los que define el paquete (contenedores, niveles
 * de superficie…). Los que no son token se DERIVAN de uno que sí lo es —el color
 * de rol tintado sobre el fondo— y se marcan como derivados.
 *
 * Reglas del paquete que se respetan aquí:
 *  - Un solo acento por bloque: coral llama a la acción, turquesa indica estado.
 *  - El amarillo es SOLO estado (capo, valoración, pendiente de revisión), nunca
 *    decoración; entra por `ExtendedColors.pending`.
 *  - Texto corrido sobre coral o amarillo, jamás: para leer van las rampas
 *    `coral-700` / `yellow-900`, que es lo que hay en `onXxxContainer`.
 */

// ---- rampas de marca (iguales en los dos modos) ----
private val Primary       = Color(0xFF1A535C)
private val Primary100    = Color(0xFFE3EEF0)
private val Primary200    = Color(0xFFBCD6DA)
private val Primary600    = Color(0xFF154650)
private val Primary700    = Color(0xFF113941)
private val Primary800    = Color(0xFF0D2B31)
private val Primary900    = Color(0xFF081D21)
private val Coral         = Color(0xFFFF6B6B)
private val Coral200      = Color(0xFFFFCCCC)
private val Coral600      = Color(0xFFEE5253)
private val Coral700      = Color(0xFFC93B3C)
private val Turquoise     = Color(0xFF4ECDC4)
private val Turquoise200  = Color(0xFFC2F0EB)
private val Turquoise300  = Color(0xFF94E4DD)
private val Turquoise400  = Color(0xFF6DD9D0)
private val Turquoise600  = Color(0xFF37AFA6)
private val Turquoise700  = Color(0xFF2A8A83)
private val Turquoise900  = Color(0xFF164742)
private val Yellow        = Color(0xFFFFE66D)
private val Yellow100     = Color(0xFFFFFBE6)
private val Yellow900     = Color(0xFF6B550A)

// ---- superficies y texto ----
private val LightBg       = Color(0xFFF7EFE3)
private val LightSurface  = Color(0xFFF2FAF6)
private val LightSurface2 = Color(0xFFFFFFFF)
private val LightInk      = Color(0xFF12363D)
private val LightBody     = Color(0xFF3F5257)
/*
 * El `--ac-muted` del kit (#7B8E92) da 3,2:1 sobre la tarjeta clara: vale para
 * un rótulo grande, no para el artista de una tarjeta ni un pie de texto. Sube
 * el ROL de texto secundario hasta 4,5:1 conservando el tono; el token de marca
 * no se toca, igual que en el modo oscuro y en la web.
 */
private val LightMuted    = Color(0xFF667579)
private val LightLine     = Color(0xFFDCE8E5)

private val DarkBg        = Color(0xFF0F2429)
private val DarkBgAlt     = Color(0xFF122E34)
private val DarkSurface   = Color(0xFF163A41)
private val DarkSurface2  = Color(0xFF1C464E)
private val DarkInk       = Color(0xFFF2FAF6)
private val DarkLine      = Color(0xFF26545C)
private val DarkOnAction  = Color(0xFF08262B)
private val DarkOnCoral   = Color(0xFF3B0E0E)
private val DarkNav       = Color(0xFF0B1D21)

/*
 * El gris secundario del kit (#8CA6AA) se queda en 3,6:1 sobre la tarjeta
 * oscura: el artista y los pies de tarjeta costaban de leer. Sube el ROL, igual
 * que en la web; los tokens de marca no se tocan.
 */
private val DarkBody      = Color(0xFFD2E4E2)
private val DarkMuted     = Color(0xFFA9C3C6)

private val LightScheme = lightColorScheme(
    primary = Primary,                            // action
    onPrimary = LightSurface,                     // on-action
    primaryContainer = Primary100,                // relleno tintado del kit
    onPrimaryContainer = Primary700,
    secondary = Turquoise700,                     // active, en la rampa que se lee
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Turquoise200,
    onSecondaryContainer = Turquoise900,
    tertiary = Coral700,                          // highlight legible como texto
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Coral200,
    onTertiaryContainer = Color(0xFF701F1F),
    background = LightBg,
    onBackground = LightBody,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = Color(0xFFF3E3CD),           // bg-alt del kit
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = LightSurface2,       // derivados: escalón de superficie
    surfaceContainerLow = Color(0xFFF7FCFA),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = Color(0xFFEAF5F0),
    surfaceContainerHighest = Color(0xFFE3F0EB),
    surfaceDim = Color(0xFFEDE4D8),
    surfaceBright = LightSurface2,
    outline = Primary200,                         // border-strong
    outlineVariant = LightLine,                   // border
    error = Coral700,                             // danger legible
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE9E9),
    onErrorContainer = Color(0xFF701F1F),
    inverseSurface = LightInk,
    inverseOnSurface = LightSurface,
    inversePrimary = Turquoise,                   // la acción del modo oscuro
    scrim = Primary900
)

private val DarkScheme = darkColorScheme(
    primary = Turquoise,                          // action (en oscuro, turquesa)
    onPrimary = DarkOnAction,
    primaryContainer = Color(0xFF1E5058),         // derivado: turquesa 16 % sobre bg
    onPrimaryContainer = Turquoise300,
    secondary = Turquoise400,                     // active
    onSecondary = Primary800,
    secondaryContainer = Color(0xFF215A5C),
    onSecondaryContainer = Turquoise200,
    tertiary = Color(0xFFFF8A8A),                 // highlight aclarado del kit
    onTertiary = DarkOnCoral,
    tertiaryContainer = Color(0xFF52272B),        // derivado: coral 16 % sobre bg
    onTertiaryContainer = Color(0xFFFFCCCC),
    background = DarkBg,
    onBackground = DarkBody,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkBgAlt,
    onSurfaceVariant = DarkMuted,
    surfaceContainerLowest = DarkNav,             // derivados: escalón de superficie
    surfaceContainerLow = DarkBgAlt,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface2,
    surfaceContainerHighest = Color(0xFF215158),
    surfaceDim = DarkBg,
    surfaceBright = DarkSurface2,
    outline = Color(0xFF2F636B),                  // border-strong (rol, ver arriba)
    outlineVariant = DarkLine,
    error = Color(0xFFFF8A8A),
    onError = DarkOnCoral,
    errorContainer = Color(0xFF52272B),
    onErrorContainer = Color(0xFFFFCCCC),
    inverseSurface = DarkInk,
    inverseOnSurface = DarkSurface,
    inversePrimary = Primary,                     // la acción del modo claro
    scrim = Color(0xFF000000)
)

/**
 * Roles del kit que Material 3 no tiene. Se adaptan al tema y hay que usarlos en
 * lugar de colores a mano.
 *
 *  - [success] / [onSuccess]: turquesa de estado positivo — afinador en tono,
 *    ejercicio superado, partitura publicada.
 *  - [pending] / [onPending]: el AMARILLO del kit, y solo como estado: capo,
 *    valoración, propuesta a la espera. Nunca de adorno (regla 2).
 *  - [chord]: el color de los acordes sobre la hoja. Coral, en la rampa que se
 *    puede leer: el coral de marca sobre crema da 2,4:1 (regla 5).
 *  - [nav] / [onNav]: la barra superior, teal macizo como en la web.
 *  - [beat]: el pulso del metrónomo, que en la web es el amarillo de estado.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val pending: Color,
    val onPending: Color,
    val chord: Color,
    val nav: Color,
    val onNav: Color,
    val beat: Color
) {
    /** Nombre anterior de [pending]; se mantiene para no romper llamadas. */
    val warning: Color get() = pending
    val onWarning: Color get() = onPending
}

private val LightExtended = ExtendedColors(
    success = Turquoise700,
    onSuccess = Color(0xFFFFFFFF),
    pending = Yellow,
    onPending = Yellow900,
    chord = Coral700,
    nav = Primary,
    onNav = LightSurface,
    beat = Yellow
)

private val DarkExtended = ExtendedColors(
    success = Turquoise400,
    onSuccess = Primary800,
    pending = Yellow,
    onPending = Yellow900,
    chord = Color(0xFFFF8A8A),
    nav = DarkNav,
    onNav = DarkInk,
    beat = Yellow
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

/** Acceso cómodo: `MaterialTheme.extendedColors.success`. */
val MaterialTheme.extendedColors: ExtendedColors
    @Composable get() = LocalExtendedColors.current

@Composable
fun GuitarChordsTheme(
    dark: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = false,
    content: @Composable () -> Unit
) {
    /*
     * `dynamic` viene apagado a propósito. El color dinámico de Android 12+ pinta
     * la app con el fondo de pantalla del móvil, y entonces la app deja de ser
     * Accordio: la marca es justo lo que se quiere que se reconozca entre la web
     * y el móvil. Se deja el parámetro por si alguna pantalla de pruebas lo pide.
     */
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
            shapes = AppShapes,
            content = content
        )
    }
}
