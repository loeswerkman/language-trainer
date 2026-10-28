package io.github.loeswerkman.language_trainer.vocabulary

enum class AnswerResult { CORRECT, ALMOST_CORRECT, INCORRECT }

/**
 * Compares an answer with the accepted answers for [direction], ignoring case and surrounding whitespace.
 * An answer within a Levenshtein distance of 1 of an accepted answer counts as almost correct.
 */
fun evaluateAnswer(answer: String, word: Word, direction: TranslationDirection): AnswerResult {
    val normalisedAnswer = normalise(answer)
    if (normalisedAnswer.isEmpty()) return AnswerResult.INCORRECT

    val bestDistance = word.acceptedAnswers(direction)
        .minOf { levenshteinDistance(normalisedAnswer, normalise(it)) }

    return when (bestDistance) {
        0 -> AnswerResult.CORRECT
        1 -> AnswerResult.ALMOST_CORRECT
        else -> AnswerResult.INCORRECT
    }
}

fun levenshteinDistance(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val current = IntArray(b.length + 1)
        current[0] = i
        for (j in 1..b.length) {
            val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + substitutionCost,
            )
        }
        previous = current
    }
    return previous[b.length]
}

private fun normalise(text: String): String = text.trim().lowercase()
