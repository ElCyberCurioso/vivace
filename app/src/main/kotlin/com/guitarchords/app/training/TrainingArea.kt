package com.guitarchords.app.training

/**
 * Áreas del plan de entrenamiento. El nombre del enum es la clave persistida
 * en `area_progress.area` y `exercise_results.area` — no renombrar.
 *
 * La UI muestra un área como "Próximamente" mientras el curriculum no tenga
 * ejercicios suyos (ver `TrainingArea.hasContent()`), sin tocar la DB.
 */
enum class TrainingArea {
    CHORDS,      // acordes: digitación y vocabulario
    CHANGES,     // cambios de acorde
    RHYTHM,      // ritmo / rasgueo
    SCALES,      // escalas y licks
    TECHNIQUE,   // púa alterna, ligados…
    THEORY,      // teoría musical
    EAR          // oído (reconocimiento auditivo)
}
