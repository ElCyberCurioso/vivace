package com.guitarchords.app.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Mejor resultado por ejercicio (derivado, no persistido). */
data class BestResult(
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val best: Int,
    val passed: Boolean
)

/** Agregado de XP/intentos por área para estadísticas. */
data class AreaStat(
    val area: String,
    val xp: Long,
    val attempts: Int,
    val passed: Int
)

@Dao
interface TrainingDao {

    // ---- perfil ----
    @Query("SELECT * FROM training_profile WHERE id = 1")
    fun observeProfile(): Flow<TrainingProfile?>

    @Query("SELECT * FROM training_profile WHERE id = 1")
    suspend fun getProfile(): TrainingProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: TrainingProfile)

    // ---- progreso por área ----
    @Query("SELECT * FROM area_progress")
    fun observeAreas(): Flow<List<AreaProgress>>

    @Query("SELECT * FROM area_progress")
    suspend fun getAreas(): List<AreaProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArea(area: AreaProgress)

    // ---- resultados ----
    @Insert
    suspend fun insertResult(result: ExerciseResult): Long

    @Query(
        """
        SELECT exercise_id, MAX(score) AS best, MAX(passed) AS passed
        FROM exercise_results GROUP BY exercise_id
        """
    )
    fun observeBestResults(): Flow<List<BestResult>>

    @Query(
        """
        SELECT exercise_id, MAX(score) AS best, MAX(passed) AS passed
        FROM exercise_results GROUP BY exercise_id
        """
    )
    suspend fun getBestResults(): List<BestResult>

    @Query("SELECT COUNT(*) FROM exercise_results WHERE passed = 1")
    suspend fun countPassed(): Int

    @Query("SELECT COUNT(DISTINCT exercise_id) FROM exercise_results WHERE passed = 1")
    suspend fun countDistinctPassed(): Int

    @Query("SELECT COUNT(DISTINCT area) FROM exercise_results WHERE passed = 1")
    suspend fun countAreasWithPass(): Int

    @Query("SELECT MAX(score) FROM exercise_results")
    suspend fun maxScore(): Int?

    @Query("SELECT * FROM exercise_results ORDER BY timestamp DESC LIMIT :n")
    fun observeRecent(n: Int): Flow<List<ExerciseResult>>

    @Query(
        """
        SELECT area, SUM(xp_earned) AS xp, COUNT(*) AS attempts,
               SUM(CASE WHEN passed = 1 THEN 1 ELSE 0 END) AS passed
        FROM exercise_results GROUP BY area
        """
    )
    fun observeAreaStats(): Flow<List<AreaStat>>

    // ---- logros ----
    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementUnlock>>

    @Query("SELECT achievement_id FROM achievements")
    suspend fun unlockedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun unlock(achievement: AchievementUnlock): Long
}
