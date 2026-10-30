package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult.ALMOST_CORRECT
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult.CORRECT
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult.INCORRECT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LearningProgressTest {

    @Test
    fun countsCorrectAnswersInARow() {
        assertEquals(0, correctStreak(emptyList()))
        assertEquals(3, correctStreak(listOf(CORRECT, CORRECT, CORRECT)))
    }

    @Test
    fun anIncorrectAnswerResetsTheStreak() {
        assertEquals(1, correctStreak(listOf(CORRECT, CORRECT, CORRECT, CORRECT, INCORRECT, CORRECT)))
        assertEquals(0, correctStreak(listOf(CORRECT, CORRECT, INCORRECT)))
    }

    @Test
    fun anAlmostCorrectAnswerNeitherCountsNorResets() {
        assertEquals(2, correctStreak(listOf(CORRECT, ALMOST_CORRECT, CORRECT, ALMOST_CORRECT)))
    }

    @Test
    fun aWordIsLearnedAfterFiveCorrectAnswersInARow() {
        val word = Word(1, "russian", "кот", "cat", emptyList(), emptyList(), WordCategory.NOUN, emptyList(), 1, 1, null)
        assertFalse(VocabularyItem(word, correctStreak = correctStreak(List(4) { CORRECT })).learned)
        assertTrue(VocabularyItem(word, correctStreak = correctStreak(List(5) { CORRECT } + ALMOST_CORRECT)).learned)
        assertFalse(VocabularyItem(word, correctStreak = correctStreak(List(7) { CORRECT } + INCORRECT)).learned)
    }
}
