package com.guitarchords.app.training

/**
 * Logros de Fase 1. El dominio solo conoce ids estables y predicados; la UI
 * mapea cada id a sus strings e icono (mantiene este fichero testeable en JVM
 * puro, sin referencias a recursos Android).
 */
object Achievements {

    /** Foto del estado tras registrar un resultado, para evaluar predicados. */
    data class Snapshot(
        val xpTotal: Long,
        val streakCurrent: Int,
        val totalPassed: Int,
        val areasWithPass: Int,
        val maxScore: Int,
        val placementDone: Boolean,
        /** El resultado recién registrado era una escala validada por micrófono. */
        val lastWasMicScale: Boolean,
        /** Todos los ejercicios de Acordes N1 del curriculum están superados. */
        val chordsLevel1Complete: Boolean
    )

    data class Def(val id: String, val predicate: (Snapshot) -> Boolean)

    val all: List<Def> = listOf(
        Def("first_steps") { it.totalPassed >= 1 },
        Def("placement_done") { it.placementDone },
        Def("streak_3") { it.streakCurrent >= 3 },
        Def("streak_7") { it.streakCurrent >= 7 },
        Def("ten_exercises") { it.totalPassed >= 10 },
        Def("chord_apprentice") { it.chordsLevel1Complete },
        Def("perfect_score") { it.maxScore >= 100 },
        Def("xp_1000") { it.xpTotal >= 1000 },
        Def("first_mic") { it.lastWasMicScale },
        Def("all_areas") { it.areasWithPass >= 5 }
    )

    /** Ids recién desbloqueados: cumplen el predicado y no estaban ya. */
    fun evaluate(snapshot: Snapshot, alreadyUnlocked: Set<String>): List<String> =
        all.filter { it.id !in alreadyUnlocked && it.predicate(snapshot) }.map { it.id }
}
