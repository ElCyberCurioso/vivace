package com.guitarchords.app.training

import com.guitarchords.app.data.AchievementUnlock
import com.guitarchords.app.data.AreaProgress
import com.guitarchords.app.data.ExerciseResult
import com.guitarchords.app.data.TrainingDao
import com.guitarchords.app.data.TrainingProfile
import java.time.LocalDate

/** Resumen que consume la pantalla de celebración tras un intento. */
data class ResultSummary(
    val xpEarned: Int,
    val score: Int,
    val passed: Boolean,
    /** Nivel de usuario nuevo si subió con este intento. */
    val newUserLevel: Int?,
    /** Nivel de área recién desbloqueado, si lo hubo. */
    val unlockedAreaLevel: Int?,
    val newAchievements: List<String>,
    val streakCurrent: Int,
    val streakExtended: Boolean
)

/**
 * Lógica de progreso del entrenamiento (separada del [com.guitarchords.app.data.Repository]
 * de canciones). Centraliza la transacción resultado → XP/racha → desbloqueo
 * de nivel de área → logros.
 */
class TrainingRepository(private val dao: TrainingDao) {

    val profile = dao.observeProfile()
    val areas = dao.observeAreas()
    val bestResults = dao.observeBestResults()
    val areaStats = dao.observeAreaStats()
    val achievements = dao.observeAchievements()
    fun recentResults(n: Int) = dao.observeRecent(n)

    suspend fun recordResult(
        spec: ExerciseSpec,
        score: Int,
        passed: Boolean,
        durationMs: Long,
        detailsJson: String = "",
        micValidated: Boolean = false,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): ResultSummary {
        val now = System.currentTimeMillis()
        val bests = dao.getBestResults().associateBy { it.exerciseId }
        val alreadyPassed = bests[spec.id]?.passed == true
        val firstClear = passed && !alreadyPassed
        val xp = Gamification.xpForAttempt(spec.xpBase, score, passed, firstClear, alreadyPassed)

        dao.insertResult(
            ExerciseResult(
                exerciseId = spec.id, area = spec.area.name, score = score,
                passed = passed, xpEarned = xp, durationMs = durationMs,
                detailsJson = detailsJson, timestamp = now
            )
        )

        // Perfil: XP total + racha (cualquier intento mantiene la racha).
        val profile = dao.getProfile() ?: TrainingProfile()
        val streak = Gamification.updateStreak(
            profile.streakCurrent, profile.streakBest, profile.lastPracticeDay, todayEpochDay
        )
        val newXpTotal = profile.xpTotal + xp
        val levelBefore = Gamification.levelForXp(profile.xpTotal)
        val levelAfter = Gamification.levelForXp(newXpTotal)
        dao.upsertProfile(
            profile.copy(
                xpTotal = newXpTotal,
                streakCurrent = streak.current,
                streakBest = streak.best,
                lastPracticeDay = todayEpochDay
            )
        )

        // Área: XP y desbloqueo del nivel siguiente al completar el 80 %.
        val passedIds = buildSet {
            bests.values.filter { it.passed }.forEach { add(it.exerciseId) }
            if (passed) add(spec.id)
        }
        val areaRow = dao.getAreas().firstOrNull { it.area == spec.area.name }
            ?: AreaProgress(spec.area.name)
        var unlockedAreaLevel: Int? = null
        var newUnlocked = areaRow.unlockedLevel
        if (passed && spec.level == areaRow.unlockedLevel && spec.level < Curriculum.MAX_LEVEL &&
            Curriculum.levelComplete(spec.area, spec.level, passedIds)
        ) {
            newUnlocked = spec.level + 1
            unlockedAreaLevel = newUnlocked
        }
        dao.upsertArea(
            areaRow.copy(unlockedLevel = newUnlocked, xpArea = areaRow.xpArea + xp, updatedAt = now)
        )

        // Logros.
        val snapshot = Achievements.Snapshot(
            xpTotal = newXpTotal,
            streakCurrent = streak.current,
            totalPassed = dao.countDistinctPassed(),
            areasWithPass = dao.countAreasWithPass(),
            maxScore = dao.maxScore() ?: 0,
            placementDone = profile.placementDone,
            lastWasMicScale = micValidated && passed,
            chordsLevel1Complete = Curriculum.levelComplete(TrainingArea.CHORDS, 1, passedIds)
        )
        val newAchievements = Achievements.evaluate(snapshot, dao.unlockedIds().toSet())
        newAchievements.forEach { dao.unlock(AchievementUnlock(it)) }

        return ResultSummary(
            xpEarned = xp,
            score = score,
            passed = passed,
            newUserLevel = levelAfter.takeIf { it > levelBefore },
            unlockedAreaLevel = unlockedAreaLevel,
            newAchievements = newAchievements,
            streakCurrent = streak.current,
            streakExtended = streak.extended
        )
    }

    /** Siguiente ejercicio recomendado según el estado actual. */
    suspend fun nextRecommended(): ExerciseSpec? {
        val areas = dao.getAreas()
        val unlocked = TrainingArea.entries.associateWith { area ->
            areas.firstOrNull { it.area == area.name }?.unlockedLevel ?: 1
        }
        val passed = dao.getBestResults().filter { it.passed }
            .mapTo(mutableSetOf()) { it.exerciseId }
        return Curriculum.recommended(unlocked, passed)
    }

    /**
     * Aplica el resultado del test de nivel. Solo puede SUBIR niveles
     * (recalibrar nunca degrada) y marca el test como hecho.
     */
    suspend fun completePlacement(levels: Map<TrainingArea, Int>) {
        val now = System.currentTimeMillis()
        val rows = dao.getAreas().associateBy { it.area }
        for ((area, level) in levels) {
            val row = rows[area.name] ?: AreaProgress(area.name)
            if (level > row.unlockedLevel) {
                dao.upsertArea(row.copy(unlockedLevel = level.coerceIn(1, Curriculum.MAX_LEVEL), updatedAt = now))
            }
        }
        val profile = dao.getProfile() ?: TrainingProfile()
        dao.upsertProfile(profile.copy(placementDone = true))
        dao.unlock(AchievementUnlock("placement_done"))
    }
}
