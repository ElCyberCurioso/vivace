package com.guitarchords.app.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TheoryQuestionGenTest {

    private fun spec(vararg topics: TheoryTopic, qualities: List<String> = emptyList()) =
        TheoryQuizSpec(
            id = "t", area = TrainingArea.THEORY, level = 1, xpBase = 10,
            titleRes = 0, descRes = 0,
            topics = topics.toList(), questionCount = 20, qualities = qualities
        )

    @Test
    fun `genera el numero pedido de preguntas validas`() {
        val qs = TheoryQuestionGen.generate(spec(*TheoryTopic.entries.toTypedArray()), seed = 42)
        assertEquals(20, qs.size)
        for (q in qs) {
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.toSet().size)              // únicas
            assertTrue(q.correctIndex in 0..3)
            assertTrue(q.args.isNotEmpty())
        }
    }

    @Test
    fun `misma semilla produce las mismas preguntas`() {
        val a = TheoryQuestionGen.generate(spec(TheoryTopic.NOTES_OF_CHORD), seed = 7)
        val b = TheoryQuestionGen.generate(spec(TheoryTopic.NOTES_OF_CHORD), seed = 7)
        assertEquals(a, b)
    }

    @Test
    fun `respeta las calidades acotadas`() {
        val qs = TheoryQuestionGen.generate(
            spec(TheoryTopic.NOTES_OF_CHORD, qualities = listOf("", "m")), seed = 3
        )
        // Los enunciados solo llevan acordes mayores o menores (X o Xm).
        for (q in qs) {
            val name = q.args[0]
            assertTrue(name, Regex("^[A-G]#?m?$").matches(name))
        }
    }

    @Test
    fun `solo usa los topics del spec`() {
        val qs = TheoryQuestionGen.generate(spec(TheoryTopic.INTERVAL_BETWEEN), seed = 11)
        assertTrue(qs.all { it.topic == TheoryTopic.INTERVAL_BETWEEN })
    }
}
