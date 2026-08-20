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
 * Tipografía de interfaz del paquete de marca: Space Grotesk.
 * Va empaquetada (OFL); ver `app/licenses/`.
 */
val VivaceSans = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Normal),
    variableFont(R.font.space_grotesk, FontWeight.Medium),
    variableFont(R.font.space_grotesk, FontWeight.SemiBold),
    variableFont(R.font.space_grotesk, FontWeight.Bold)
)

/**
 * Monoespaciada de la marca (JetBrains Mono), **solo para cifras**: BPM,
 * trastes, compases, hercios del afinador, XP. El resto va en [VivaceSans].
 */
val VivaceMono = FontFamily(
    variableFont(R.font.jetbrains_mono, FontWeight.Normal),
    variableFont(R.font.jetbrains_mono, FontWeight.SemiBold)
)

/**
 * Escala tipográfica de la app. Parte de la escala Material 3 y ajusta pesos
 * y tracking a la marca: títulos con tracking negativo, etiquetas en 500.
 */
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-1.1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VivaceSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
