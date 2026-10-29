package io.github.loeswerkman.language_trainer.trainer

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.evaluateAnswer

/**
 * Immutable state of a round of flashcards, shown in the given order. Every action returns a new session.
 * Starting the next round is up to the caller, so it can pick (and order) the words again.
 */
@ConsistentCopyVisibility
data class FlashcardSession private constructor(
    private val round: List<Word>,
    val direction: TranslationDirection,
    val position: Int = 0,
    val correctCount: Int = 0,
    val lastResult: AnswerResult? = null,
) {
    val size: Int get() = round.size
    val currentWord: Word get() = round[position]
    val isAnswered: Boolean get() = lastResult != null
    val isRoundFinished: Boolean get() = isAnswered && position == round.lastIndex

    fun submit(answer: String): FlashcardSession {
        check(!isAnswered) { "The current card has already been answered" }
        val result = evaluateAnswer(answer, currentWord, direction)
        return copy(
            lastResult = result,
            correctCount = if (result == AnswerResult.CORRECT) correctCount + 1 else correctCount,
        )
    }

    fun next(): FlashcardSession {
        check(isAnswered) { "Answer the current card first" }
        check(!isRoundFinished) { "The round is finished; start a new one" }
        return copy(position = position + 1, lastResult = null)
    }

    /**
     * Replaces the cards still to come with [words], skipping words already answered in this round.
     * An answered current card stays, so its feedback remains visible. Returns null if no cards would be left to show.
     */
    fun withRemaining(words: List<Word>): FlashcardSession? {
        val done = round.take(if (isAnswered) position + 1 else position)
        val doneIds = done.map { it.id }.toSet()
        val rest = words.filter { it.id !in doneIds }
        if (!isAnswered && rest.isEmpty()) return null
        return copy(round = done + rest)
    }

    companion object {
        fun start(words: List<Word>, direction: TranslationDirection = TranslationDirection.TO_ENGLISH): FlashcardSession {
            require(words.isNotEmpty()) { "A session needs at least one word" }
            return FlashcardSession(words, direction)
        }
    }
}
