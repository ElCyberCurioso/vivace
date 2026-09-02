package com.guitarchords.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.guitarchords.app.R

/**
 * Un peso de una fuente **variable**: el mismo fichero sirve para todos, y el
 * peso se pide por el eje `wght`. Sin `variationSettings` el sistema daría
 * siempre el peso por defecto del fichero.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/**
 * Titulares: Montserrat, la del paquete Accordio. Es variable, así que un solo
 * fichero da todos los pesos. Va empaquetada (OFL); ver `app/licenses/`.
 */
val AccordioHeading = FontFamily(
    variableFont(R.font.montserrat, FontWeight.SemiBold),
    variableFont(R.font.montserrat, FontWeight.Bold),
    variableFont(R.font.montserrat, FontWeight.ExtraBold)
)

/**
 * Texto: Poppins. Aquí NO hay fichero variable —Poppins se distribuye en pesos
 * sueltos—, así que van los tres que gasta la interfaz y ninguno más: cada peso
 * son 160 KB en el APK.
 */
val AccordioBody = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

/** Nombre anterior de la familia de texto; se mantiene para no romper llamadas. */
val VivaceSans = AccordioBody

/**
 * Monoespaciada (JetBrains Mono) para la HOJA de partitura y las cifras: BPM,
 * trastes, compases, hercios del afinador, XP.
 *
 * No sale del paquete Accordio, que no trae monoespaciada, y es la única
 * concesión: sin ancho fijo los acordes dejan de caer sobre su sílaba. Es la
 * misma decisión que en la web.
 */
val VivaceMono = FontFamily(
    variableFont(R.font.jetbrains_mono, FontWeight.Normal),
    variableFont(R.font.jetbrains_mono, FontWeight.SemiBold)
)

/**
 * Escala tipográfica de la app. Parte de la escala Material 3 y la ajusta al
 * paquete: titulares en Montserrat 700 con tracking −0,02 em, texto en Poppins
 * 400/500. La regla 4 del kit es no mezclarlas, así que aquí no hay ningún
 * estilo a medio camino.
 */
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = AccordioHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1.14).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AccordioHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.56).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AccordioHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.48).sp
    ),
    titleLarge = TextStyle(
        fontFamily = AccordioHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.44).sp
    ),
    titleMedium = TextStyle(
        fontFamily = AccordioHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.16).sp
    ),
    titleSmall = TextStyle(
        fontFamily = AccordioHeading,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AccordioBody,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AccordioBody,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AccordioBody,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AccordioBody,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AccordioBody,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AccordioBody,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
