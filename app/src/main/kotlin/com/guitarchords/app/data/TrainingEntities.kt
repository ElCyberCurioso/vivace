package com.guitarchords.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Perfil global de entrenamiento. Fila única (id = 1), sembrada en la
 * migración 9→10 y en onCreate para instalaciones nuevas.
 *
 * El nivel de usuario NO se persiste: se deriva de [xpTotal] con
 * Gamification.levelForXp (una sola fuente de verdad).
 */
@Entity(tableName = "training_profile")
data class TrainingProfile(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "xp_total") val xpTotal: Long = 0,
    @ColumnInfo(name = "streak_current") val streakCurrent: Int = 0,
    @ColumnInfo(name = "streak_best") val streakBest: Int = 0,
    /** LocalDate.toEpochDay() del último día con práctica. 0 = nunca. */
    @ColumnInfo(name = "last_practice_day") val lastPracticeDay: Long = 0,
    @ColumnInfo(name = "placement_done") val placementDone: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/** Progreso por área de entrenamiento. PK = TrainingArea.name. */
@Entity(tableName = "area_progress")
data class AreaProgress(
    @PrimaryKey val area: String,
    @ColumnInfo(name = "unlocked_level") val unlockedLevel: Int = 1,
    @ColumnInfo(name = "xp_area") val xpArea: Long = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Un intento de ejercicio (histórico completo, no solo el mejor: alimenta
 * estadísticas, racha y logros). [exerciseId] referencia el id estable del
 * curriculum en código; sin FK — si un ejercicio desaparece en una versión
 * futura sus resultados huérfanos simplemente no se muestran.
 */
@Entity(
    tableName = "exercise_results",
    indices = [Index("exercise_id"), Index("timestamp")]
)
data class ExerciseResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val area: String,
    /** 0..100. */
    val score: Int,
    val passed: Boolean,
    @ColumnInfo(name = "xp_earned") val xpEarned: Int,
    @ColumnInfo(name = "duration_ms") val durationMs: Long = 0,
    /** Detalles por tipo (bpm, aciertos…), JSON de kotlinx.serialization. */
    @ColumnInfo(name = "details_json") val detailsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/** Logro desbloqueado. PK = id estable del logro (insert con IGNORE). */
@Entity(tableName = "achievements")
data class AchievementUnlock(
    @PrimaryKey @ColumnInfo(name = "achievement_id") val achievementId: String,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long = System.currentTimeMillis()
)
