package io.github.loeswerkman.language_trainer.vocabulary

import kotlin.test.Test
import kotlin.test.assertEquals

class WordStatisticsTest {

    @Test
    fun scoreIsCorrectMinusIncorrect() {
        assertEquals(1, stats("cat", correct = 3, almostCorrect = 5, incorrect = 2).score)
    }

    @Test
    fun sortsByScoreThenAlphabetically() {
        val sorted = listOf(
            stats("dog", correct = 1, incorrect = 3),
            stats("Bird", correct = 2, incorrect = 0),
            stats("ant", correct = 0, incorrect = 0),
            stats("cat", correct = 4, incorrect = 2),
        ).sortedWith(WordStatistics.byScore)

        assertEquals(listOf("Bird", "cat", "ant", "dog"), sorted.map { it.word })
    }

    private fun stats(word: String, correct: Int = 0, almostCorrect: Int = 0, incorrect: Int = 0) =
        WordStatistics(word.length.toLong(), word, "", correct, almostCorrect, incorrect)
}
