package com.guitarchords.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Radios del paquete Accordio (regla 3: «nada cuadrado»). Son los mismos
 * `--ac-radius-*` que gasta la web: 8 / 12 / 20 px, y la píldora para los
 * controles.
 *
 * Material 3 usa `extraSmall`..`extraLarge` para cosas distintas de las del kit,
 * así que se mapean por USO y no por nombre: los chips y campos pequeños caen en
 * small (12, el radio de control del kit) y las tarjetas en large (20).
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),    // ac-radius-sm
    small = RoundedCornerShape(12.dp),        // ac-radius-md · controles
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),        // ac-radius-lg · tarjetas
    extraLarge = RoundedCornerShape(28.dp)    // ac-radius-xl · hojas y diálogos
)

/** Píldora: los botones del kit son redondos por los lados, no rectángulos. */
val PillShape = RoundedCornerShape(percent = 50)
