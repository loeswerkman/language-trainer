package io.github.loeswerkman.language_trainer.vocabulary

/** A word is learned once it has been answered correctly this many times in a row. */
const val LEARNED_STREAK = 5

/**
 * The number of correct answers in a row at the end of [results] (oldest first). An incorrect answer resets it to 0;
 * an almost correct answer neither counts nor resets it.
 */
fun correctStreak(results: Iterable<AnswerResult>): Int = results.fold(0) { streak, result ->
    when (result) {
        AnswerResult.CORRECT -> streak + 1
        AnswerResult.ALMOST_CORRECT -> streak
        AnswerResult.INCORRECT -> 0
    }
}
