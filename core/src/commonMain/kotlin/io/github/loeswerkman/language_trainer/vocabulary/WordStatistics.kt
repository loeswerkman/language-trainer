package io.github.loeswerkman.language_trainer.vocabulary

import kotlinx.serialization.Serializable

/** How often a word was answered correctly, almost correctly and incorrectly, counted from the saved answers. */
@Serializable
data class WordStatistics(
    val wordId: Long,
    val word: String,
    val translation: String,
    val correct: Int,
    val almostCorrect: Int,
    val incorrect: Int,
) {
    val score: Int get() = correct - incorrect

    companion object {
        /** Highest [score] first; ties are sorted alphabetically. */
        val byScore: Comparator<WordStatistics> =
            compareByDescending<WordStatistics> { it.score }.thenBy { it.word.lowercase() }
    }
}
