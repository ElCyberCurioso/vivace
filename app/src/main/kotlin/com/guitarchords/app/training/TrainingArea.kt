package com.guitarchords.app.training

/**
 * Áreas del plan de entrenamiento. El nombre del enum es la clave persistida
 * en `area_progress.area` y `exercise_results.area` — no renombrar.
 *
 * EAR existe desde el día 1 (evita migrar datos después) pero no tiene
 * ejercicios en Fase 1: la UI la muestra como "Próximamente".
 */
enum class TrainingArea {
    CHORDS,      // acordes: digitación y vocabulario
    CHANGES,     // cambios de acorde
    RHYTHM,      // ritmo / rasgueo
    SCALES,      // escalas y licks
    TECHNIQUE,   // púa alterna, ligados…
    THEORY,      // teoría musical
    EAR          // oído (Fase 2)
}
