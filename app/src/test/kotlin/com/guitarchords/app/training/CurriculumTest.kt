package com.guitarchords.app.training

import com.guitarchords.app.chords.ChordLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurriculumTest {

    @Test
    fun `ids unicos y niveles en rango`() {
        val ids = Curriculum.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(Curriculum.all.all { it.level in 1..Curriculum.MAX_LEVEL })
    }

    @Test
    fun `los ids llevan el prefijo de su area`() {
        val prefix = mapOf(
            TrainingArea.CHORDS to "chords_", TrainingArea.CHANGES to "changes_",
            TrainingArea.RHYTHM to "rhythm_", TrainingArea.SCALES to "scales_",
            TrainingArea.TECHNIQUE to "technique_", TrainingArea.THEORY to "theory_",
            TrainingArea.EAR to "ear_"
        )
        for (spec in Curriculum.all) {
            assertTrue(spec.id, spec.id.startsWith(prefix.getValue(spec.area)))
        }
    }

    @Test
    fun `todos los acordes del curriculum existen en la libreria`() {
        val names = Curriculum.all.flatMap {
            when (it) {
                is ChordQuizSpec -> it.chords
                is ChordChangeSpec -> listOf(it.chordA, it.chordB)
                else -> emptyList()
            }
        }
        for (name in names.distinct()) {
            assertNotNull("Acorde sin digitación: $name", ChordLibrary.find(name))
        }
    }

    @Test
    fun `las notas de escala estan dentro del mastil`() {
        for (spec in Curriculum.all.filterIsInstance<ScaleNotesSpec>()) {
            for (n in spec.notes) {
                assertTrue(n.stringIdx in 0..5)
                assertTrue(n.fret in 0..15)
                // MIDI coherente con cuerda+traste en afinación estándar (E2=40).
                val open = listOf(40, 45, 50, 55, 59, 64)
                assertEquals(spec.id, open[n.stringIdx] + n.fret, n.midi)
            }
        }
    }

    @Test
    fun `levelComplete exige el 80 por ciento`() {
        val l1 = Curriculum.levelExercises(TrainingArea.CHORDS, 1).map { it.id }
        assertFalse(Curriculum.levelComplete(TrainingArea.CHORDS, 1, emptySet()))
        assertTrue(Curriculum.levelComplete(TrainingArea.CHORDS, 1, l1.toSet()))
    }

    @Test
    fun `recommended elige nivel accesible no superado`() {
        val unlocked = TrainingArea.entries.associateWith { 1 }
        val rec = Curriculum.recommended(unlocked, emptySet())
        assertNotNull(rec)
        assertEquals(1, rec!!.level)

        // Si todo está superado, no recomienda nada.
        val allIds = Curriculum.all.map { it.id }.toSet()
        val maxUnlocked = TrainingArea.entries.associateWith { Curriculum.MAX_LEVEL }
        assertNull(Curriculum.recommended(maxUnlocked, allIds))
    }

    @Test
    fun `recommended reparte entre areas`() {
        val unlocked = TrainingArea.entries.associateWith { 1 }
        // Con un área ya practicada, recomienda otra con menos superados.
        val chordsIds = Curriculum.levelExercises(TrainingArea.CHORDS, 1).map { it.id }.toSet()
        val rec = Curriculum.recommended(unlocked, chordsIds)
        assertNotNull(rec)
        assertTrue(rec!!.area != TrainingArea.CHORDS)
    }
}
