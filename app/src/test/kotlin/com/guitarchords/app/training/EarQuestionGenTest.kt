package com.guitarchords.app.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class EarQuestionGenTest {

    private fun spec(mode: EarMode, choices: List<Int>, count: Int = 8) = EarTrainingSpec(
        id = "test", area = TrainingArea.EAR, level = 1, xpBase = 10,
        titleRes = 0, descRes = 0, mode = mode, choices = choices, questionCount = count
    )

    @Test
    fun `genera tantas preguntas como pide el spec`() {
        val qs = EarQuestionGen.generate(spec(EarMode.INTERVAL, listOf(7, 12)), Random(1))
        assertEquals(8, qs.size)
    }

    @Test
    fun `el intervalo suena en secuencia y coincide con la respuesta`() {
        val qs = EarQuestionGen.generate(spec(EarMode.INTERVAL, listOf(3, 4, 7)), Random(2))
        for (q in qs) {
            assertEquals(2, q.midis.size)
            assertTrue(!q.together)
            assertEquals(q.answer, q.midis[1] - q.midis[0])
            assertTrue(q.answer in listOf(3, 4, 7))
        }
    }

    @Test
    fun `la calidad del acorde suena junta y la tercera la distingue`() {
        val choices = listOf(EarQuestionGen.MAJOR, EarQuestionGen.MINOR)
        val qs = EarQuestionGen.generate(spec(EarMode.CHORD_QUALITY, choices), Random(3))
        for (q in qs) {
            assertEquals(3, q.midis.size)
            assertTrue(q.together)
            val third = q.midis[1] - q.midis[0]
            assertEquals(7, q.midis[2] - q.midis[0])   // la quinta no cambia
            assertEquals(if (q.answer == EarQuestionGen.MINOR) 3 else 4, third)
        }
    }

    @Test
    fun `la direccion responde al sentido real del salto`() {
        val qs = EarQuestionGen.generate(spec(EarMode.DIRECTION, listOf(5, 7, 12)), Random(4))
        for (q in qs) {
            val delta = q.midis[1] - q.midis[0]
            assertEquals(if (delta > 0) 1 else -1, q.answer)
            assertTrue(abs(delta) in listOf(5, 7, 12))
            assertEquals(listOf(1, -1), q.options)
        }
    }

    @Test
    fun `las notas caen en un rango audible razonable`() {
        val modes = listOf(
            spec(EarMode.INTERVAL, listOf(12)),
            spec(EarMode.CHORD_QUALITY, listOf(EarQuestionGen.MAJOR)),
            spec(EarMode.DIRECTION, listOf(12))
        )
        for (s in modes) {
            for (q in EarQuestionGen.generate(s, Random(5))) {
                assertTrue(q.midis.all { it in 36..96 })
            }
        }
    }

    @Test
    fun `misma semilla produce las mismas preguntas`() {
        val s = spec(EarMode.INTERVAL, listOf(3, 4, 5, 7))
        assertEquals(
            EarQuestionGen.generate(s, Random(42)),
            EarQuestionGen.generate(s, Random(42))
        )
    }
}
