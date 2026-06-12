package com.guitarchords.app.training

import com.guitarchords.app.R
import kotlin.math.ceil

/**
 * Catálogo estático del plan de entrenamiento. Los ids son estables entre
 * versiones (la DB los referencia); añadir ejercicios nunca requiere migración.
 *
 * Regla de desbloqueo: el nivel N+1 de un área se abre al superar el 80 % de
 * los ejercicios del nivel N de esa área (o directamente por el test de
 * nivel). Lo desbloqueado nunca se pierde.
 */
object Curriculum {

    const val MAX_LEVEL = 3

    private val QUARTERS = listOf(0f, 1f, 2f, 3f)
    private val EIGHTHS = listOf(0f, 0.5f, 1f, 1.5f, 2f, 2.5f, 3f, 3.5f)
    private val OFFBEATS = listOf(0.5f, 1.5f, 2.5f, 3.5f)

    private fun chordQuiz(
        id: String, level: Int, chords: List<String>, requireRootBass: Boolean = true
    ) = ChordQuizSpec(
        id = id, area = TrainingArea.CHORDS, level = level,
        xpBase = Gamification.xpBaseForLevel(level),
        titleRes = R.string.tr_title_chord_quiz, titleArgs = listOf(chords.joinToString(" · ")),
        descRes = R.string.tr_desc_chord_quiz,
        chords = chords, requireRootBass = requireRootBass
    )

    private fun change(
        id: String, level: Int, a: String, b: String, bpm: Int, target: Int
    ) = ChordChangeSpec(
        id = id, area = TrainingArea.CHANGES, level = level,
        xpBase = Gamification.xpBaseForLevel(level),
        titleRes = R.string.tr_title_change, titleArgs = listOf(a, b),
        descRes = R.string.tr_desc_change,
        chordA = a, chordB = b, bpm = bpm, targetChanges = target
    )

    private fun rhythm(
        id: String, level: Int, titleRes: Int, bpm: Int, pattern: List<Float>,
        toleranceMs: Int, area: TrainingArea = TrainingArea.RHYTHM
    ) = RhythmTapSpec(
        id = id, area = area, level = level,
        xpBase = Gamification.xpBaseForLevel(level),
        titleRes = titleRes, descRes = R.string.tr_desc_rhythm,
        bpm = bpm, pattern = pattern, toleranceMs = toleranceMs
    )

    private fun scale(
        id: String, level: Int, titleRes: Int, notes: List<TargetNote>,
        area: TrainingArea = TrainingArea.SCALES, descRes: Int = R.string.tr_desc_scale
    ) = ScaleNotesSpec(
        id = id, area = area, level = level,
        xpBase = Gamification.xpBaseForLevel(level),
        titleRes = titleRes, descRes = descRes, notes = notes
    )

    private fun theory(
        id: String, level: Int, titleRes: Int, topics: List<TheoryTopic>,
        qualities: List<String> = emptyList(), questionCount: Int = 8
    ) = TheoryQuizSpec(
        id = id, area = TrainingArea.THEORY, level = level,
        xpBase = Gamification.xpBaseForLevel(level),
        titleRes = titleRes, descRes = R.string.tr_desc_theory,
        topics = topics, qualities = qualities, questionCount = questionCount
    )

    // Em pentatónica posición abierta, Mi grave primero (stringIdx 0 = 6ª cuerda, MIDI E2=40).
    private val EM_PENT_OPEN = listOf(
        TargetNote(0, 0, 40), TargetNote(0, 3, 43),   // E2  G2
        TargetNote(1, 0, 45), TargetNote(1, 2, 47),   // A2  B2
        TargetNote(2, 0, 50), TargetNote(2, 2, 52),   // D3  E3
        TargetNote(3, 0, 55), TargetNote(3, 2, 57),   // G3  A3
        TargetNote(4, 0, 59), TargetNote(4, 3, 62),   // B3  D4
        TargetNote(5, 0, 64), TargetNote(5, 3, 67)    // E4  G4
    )

    val all: List<ExerciseSpec> = buildList {
        // ---------------- ACORDES (4 + 4 + 3) ----------------
        add(chordQuiz("chords_l1_first_minors", 1, listOf("Em", "Am", "Dm")))
        add(chordQuiz("chords_l1_majors_1", 1, listOf("E", "A", "D")))
        add(chordQuiz("chords_l1_majors_2", 1, listOf("G", "C", "D")))
        add(chordQuiz("chords_l1_open_review", 1, listOf("Em", "Am", "E", "A", "D", "C")))
        add(chordQuiz("chords_l2_barre", 2, listOf("F", "Bm")))
        add(chordQuiz("chords_l2_sevenths", 2, listOf("E7", "A7", "D7", "B7")))
        add(chordQuiz("chords_l2_maj7_m7", 2, listOf("Cmaj7", "Am7", "Dm7", "Gmaj7")))
        add(chordQuiz("chords_l2_sus", 2, listOf("Dsus4", "Asus2", "Esus4")))
        add(chordQuiz("chords_l3_barre_shapes", 3, listOf("F#m", "G#m", "C#m"), requireRootBass = false))
        add(chordQuiz("chords_l3_add9", 3, listOf("Cadd9", "Aadd9", "Dadd9"), requireRootBass = false))
        add(chordQuiz("chords_l3_dim_aug", 3, listOf("Bdim", "Caug", "F#dim"), requireRootBass = false))

        // ---------------- CAMBIOS (3 + 3 + 3) ----------------
        add(change("changes_l1_em_am", 1, "Em", "Am", 60, 25))
        add(change("changes_l1_am_e", 1, "Am", "E", 60, 25))
        add(change("changes_l1_g_c", 1, "G", "C", 60, 22))
        add(change("changes_l2_c_f", 2, "C", "F", 70, 28))
        add(change("changes_l2_g_bm", 2, "G", "Bm", 75, 28))
        add(change("changes_l2_am_f", 2, "Am", "F", 80, 30))
        add(change("changes_l3_f_bm", 3, "F", "Bm", 100, 35))
        add(change("changes_l3_b7_e", 3, "B7", "E", 100, 35))
        add(change("changes_l3_cmaj7_fmaj7", 3, "Cmaj7", "Fmaj7", 110, 40))

        // ---------------- RITMO (3 + 3 + 3) ----------------
        add(rhythm("rhythm_l1_quarters_60", 1, R.string.tr_title_rhythm_quarters_60, 60, QUARTERS, 120))
        add(rhythm("rhythm_l1_quarters_80", 1, R.string.tr_title_rhythm_quarters_80, 80, QUARTERS, 120))
        add(rhythm("rhythm_l1_halves_60", 1, R.string.tr_title_rhythm_halves_60, 60, listOf(0f, 2f), 120))
        add(rhythm("rhythm_l2_eighths_70", 2, R.string.tr_title_rhythm_eighths_70, 70, EIGHTHS, 90))
        add(rhythm("rhythm_l2_offbeat_80", 2, R.string.tr_title_rhythm_offbeat_80, 80, OFFBEATS, 90))
        add(rhythm("rhythm_l2_quarters_110", 2, R.string.tr_title_rhythm_quarters_110, 110, QUARTERS, 90))
        add(rhythm("rhythm_l3_sync_90", 3, R.string.tr_title_rhythm_sync_90, 90, listOf(0f, 1.5f, 2f, 3.5f), 70))
        add(rhythm("rhythm_l3_eighths_120", 3, R.string.tr_title_rhythm_eighths_120, 120, EIGHTHS, 70))
        add(rhythm("rhythm_l3_mixed_100", 3, R.string.tr_title_rhythm_mixed_100, 100, listOf(0f, 0.5f, 1f, 2f, 2.5f, 3f), 70))

        // ---------------- ESCALAS (3 + 3 + 3) ----------------
        add(scale("scales_l1_pent_em_low", 1, R.string.tr_title_scale_pent_em_low, EM_PENT_OPEN.take(6)))
        add(scale("scales_l1_pent_em_high", 1, R.string.tr_title_scale_pent_em_high, EM_PENT_OPEN.drop(6)))
        add(scale("scales_l1_pent_em_full", 1, R.string.tr_title_scale_pent_em_full, EM_PENT_OPEN))
        add(
            scale(
                "scales_l2_major_c", 2, R.string.tr_title_scale_major_c,
                listOf(
                    TargetNote(1, 3, 48), TargetNote(2, 0, 50), TargetNote(2, 2, 52),
                    TargetNote(2, 3, 53), TargetNote(3, 0, 55), TargetNote(3, 2, 57),
                    TargetNote(4, 0, 59), TargetNote(4, 1, 60)
                )
            )
        )
        add(
            scale(
                "scales_l2_pent_am", 2, R.string.tr_title_scale_pent_am,
                listOf(
                    TargetNote(0, 5, 45), TargetNote(0, 8, 48), TargetNote(1, 5, 50),
                    TargetNote(1, 7, 52), TargetNote(2, 5, 55), TargetNote(2, 7, 57),
                    TargetNote(3, 5, 60), TargetNote(3, 7, 62), TargetNote(4, 5, 64),
                    TargetNote(4, 8, 67), TargetNote(5, 5, 69), TargetNote(5, 8, 72)
                )
            )
        )
        add(
            scale(
                "scales_l2_major_g", 2, R.string.tr_title_scale_major_g,
                listOf(
                    TargetNote(0, 3, 43), TargetNote(1, 0, 45), TargetNote(1, 2, 47),
                    TargetNote(1, 3, 48), TargetNote(2, 0, 50), TargetNote(2, 2, 52),
                    TargetNote(2, 4, 54), TargetNote(3, 0, 55)
                )
            )
        )
        add(
            scale(
                "scales_l3_pent_em_pos2", 3, R.string.tr_title_scale_pent_em_pos2,
                listOf(
                    TargetNote(0, 3, 43), TargetNote(0, 5, 45), TargetNote(1, 2, 47),
                    TargetNote(1, 5, 50), TargetNote(2, 2, 52), TargetNote(2, 5, 55),
                    TargetNote(3, 2, 57), TargetNote(3, 4, 59), TargetNote(4, 3, 62),
                    TargetNote(4, 5, 64), TargetNote(5, 3, 67), TargetNote(5, 5, 69)
                )
            )
        )
        add(
            scale(
                "scales_l3_lick_blues", 3, R.string.tr_title_scale_lick_blues,
                listOf(
                    TargetNote(2, 2, 52), TargetNote(3, 2, 57), TargetNote(3, 4, 59),
                    TargetNote(4, 3, 62), TargetNote(4, 5, 64), TargetNote(5, 3, 67),
                    TargetNote(5, 5, 69), TargetNote(5, 3, 67)
                )
            )
        )
        add(
            scale(
                "scales_l3_lick_desc", 3, R.string.tr_title_scale_lick_desc,
                listOf(
                    TargetNote(5, 8, 72), TargetNote(5, 5, 69), TargetNote(4, 8, 67),
                    TargetNote(4, 5, 64), TargetNote(3, 7, 62), TargetNote(3, 5, 60),
                    TargetNote(2, 7, 57), TargetNote(2, 5, 55)
                )
            )
        )

        // ---------------- TÉCNICA (1 + 2 + 2) — reutiliza escalas y ritmo ----------------
        add(
            scale(
                "technique_l1_alt_open", 1, R.string.tr_title_tech_alt_open,
                listOf(
                    TargetNote(0, 0, 40), TargetNote(1, 0, 45), TargetNote(2, 0, 50),
                    TargetNote(3, 0, 55), TargetNote(4, 0, 59), TargetNote(5, 0, 64)
                ),
                area = TrainingArea.TECHNIQUE, descRes = R.string.tr_desc_technique
            )
        )
        add(
            scale(
                "technique_l2_chromatic", 2, R.string.tr_title_tech_chromatic,
                listOf(
                    TargetNote(0, 1, 41), TargetNote(0, 2, 42), TargetNote(0, 3, 43),
                    TargetNote(0, 4, 44), TargetNote(1, 1, 46), TargetNote(1, 2, 47),
                    TargetNote(1, 3, 48), TargetNote(1, 4, 49)
                ),
                area = TrainingArea.TECHNIQUE, descRes = R.string.tr_desc_technique
            )
        )
        add(
            rhythm(
                "technique_l2_eighths", 2, R.string.tr_title_tech_eighths, 70, EIGHTHS, 100,
                area = TrainingArea.TECHNIQUE
            )
        )
        add(
            scale(
                "technique_l3_string_skip", 3, R.string.tr_title_tech_skip,
                listOf(
                    TargetNote(0, 0, 40), TargetNote(2, 0, 50), TargetNote(1, 0, 45),
                    TargetNote(3, 0, 55), TargetNote(2, 0, 50), TargetNote(4, 0, 59),
                    TargetNote(3, 0, 55), TargetNote(5, 0, 64)
                ),
                area = TrainingArea.TECHNIQUE, descRes = R.string.tr_desc_technique
            )
        )
        add(
            rhythm(
                "technique_l3_gallop", 3, R.string.tr_title_tech_gallop, 100,
                listOf(0f, 0.5f, 0.75f, 1f, 1.5f, 1.75f, 2f, 2.5f, 2.75f, 3f, 3.5f, 3.75f),
                70, area = TrainingArea.TECHNIQUE
            )
        )

        // ---------------- TEORÍA (4 + 4 + 4) ----------------
        add(
            theory(
                "theory_l1_notes", 1, R.string.tr_title_theory_notes,
                listOf(TheoryTopic.NOTES_OF_CHORD, TheoryTopic.LATIN_NOTATION),
                qualities = listOf("", "m")
            )
        )
        add(theory("theory_l1_intervals", 1, R.string.tr_title_theory_intervals, listOf(TheoryTopic.INTERVAL_BETWEEN)))
        add(theory("theory_l1_latin", 1, R.string.tr_title_theory_latin, listOf(TheoryTopic.LATIN_NOTATION), questionCount = 6))
        add(
            theory(
                "theory_l1_triads", 1, R.string.tr_title_theory_triads,
                listOf(TheoryTopic.FORMULA_TO_CHORD, TheoryTopic.CHORD_TO_FORMULA),
                qualities = listOf("", "m", "5", "sus2", "sus4")
            )
        )
        add(
            theory(
                "theory_l2_sevenths", 2, R.string.tr_title_theory_sevenths,
                listOf(TheoryTopic.NOTES_OF_CHORD),
                qualities = listOf("7", "maj7", "m7")
            )
        )
        add(theory("theory_l2_intervals", 2, R.string.tr_title_theory_intervals2, listOf(TheoryTopic.INTERVAL_BETWEEN), questionCount = 10))
        add(
            theory(
                "theory_l2_formulas", 2, R.string.tr_title_theory_formulas2,
                listOf(TheoryTopic.FORMULA_TO_CHORD, TheoryTopic.CHORD_TO_FORMULA),
                qualities = listOf("7", "maj7", "m7", "dim", "aug")
            )
        )
        add(
            theory(
                "theory_l2_mixed", 2, R.string.tr_title_theory_mixed,
                TheoryTopic.entries.toList(),
                qualities = listOf("", "m", "7", "maj7", "m7")
            )
        )
        add(
            theory(
                "theory_l3_extended", 3, R.string.tr_title_theory_extended,
                listOf(TheoryTopic.NOTES_OF_CHORD, TheoryTopic.CHORD_TO_FORMULA),
                qualities = listOf("9", "add9", "m9", "11", "13")
            )
        )
        add(
            theory(
                "theory_l3_sus_alt", 3, R.string.tr_title_theory_sus_alt,
                listOf(TheoryTopic.NOTES_OF_CHORD, TheoryTopic.FORMULA_TO_CHORD),
                qualities = listOf("sus2", "sus4", "7sus4", "m7b5", "dim7")
            )
        )
        add(
            theory(
                "theory_l3_formulas", 3, R.string.tr_title_theory_formulas3,
                listOf(TheoryTopic.FORMULA_TO_CHORD, TheoryTopic.CHORD_TO_FORMULA),
                qualities = listOf("m7b5", "dim7", "aug7", "mMaj7", "6", "m6")
            )
        )
        add(
            theory(
                "theory_l3_master", 3, R.string.tr_title_theory_master,
                TheoryTopic.entries.toList(),
                qualities = listOf("", "m", "7", "maj7", "m7", "dim", "aug", "sus4", "9", "add9"),
                questionCount = 12
            )
        )
    }

    init {
        val ids = all.map { it.id }
        require(ids.size == ids.toSet().size) { "Curriculum con ids duplicados" }
        require(all.all { it.level in 1..MAX_LEVEL }) { "Nivel fuera de rango" }
    }

    fun byId(id: String): ExerciseSpec? = all.firstOrNull { it.id == id }

    fun byArea(area: TrainingArea, maxLevel: Int = MAX_LEVEL): List<ExerciseSpec> =
        all.filter { it.area == area && it.level <= maxLevel }

    fun levelExercises(area: TrainingArea, level: Int): List<ExerciseSpec> =
        all.filter { it.area == area && it.level == level }

    /** ¿Está superado el 80 % (redondeo hacia arriba) del nivel del área? */
    fun levelComplete(area: TrainingArea, level: Int, passedIds: Set<String>): Boolean {
        val specs = levelExercises(area, level)
        if (specs.isEmpty()) return false
        val needed = ceil(specs.size * 0.8).toInt()
        return specs.count { it.id in passedIds } >= needed
    }

    /**
     * Ejercicio recomendado: no superado, del nivel más bajo accesible, en el
     * área con menos ejercicios superados (reparte el entrenamiento).
     */
    fun recommended(
        unlockedLevels: Map<TrainingArea, Int>,
        passedIds: Set<String>
    ): ExerciseSpec? {
        val candidates = all.filter {
            it.id !in passedIds && it.level <= (unlockedLevels[it.area] ?: 1)
        }
        if (candidates.isEmpty()) return null
        val passedPerArea = all.filter { it.id in passedIds }.groupingBy { it.area }.eachCount()
        val minLevel = candidates.minOf { it.level }
        return candidates.filter { it.level == minLevel }
            .minByOrNull { passedPerArea[it.area] ?: 0 }
    }
}
