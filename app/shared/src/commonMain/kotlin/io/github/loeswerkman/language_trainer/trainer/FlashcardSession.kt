package io.github.loeswerkman.language_trainer.trainer

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.evaluateAnswer

typealias Shuffle = (List<Word>) -> List<Word>

private val randomShuffle: Shuffle = { it.shuffled() }

/** Immutable state of a round of flashcards. Every action returns a new session. */
@ConsistentCopyVisibility
data class FlashcardSession private constructor(
    private val words: List<Word>,
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

    /** Moves to the next card, or starts a new shuffled round after the last card. */
    fun next(shuffle: Shuffle = randomShuffle): FlashcardSession {
        check(isAnswered) { "Answer the current card first" }
        return if (isRoundFinished) start(words, direction, shuffle) else copy(position = position + 1, lastResult = null)
    }

    companion object {
        fun start(
            words: List<Word>,
            direction: TranslationDirection = TranslationDirection.TO_ENGLISH,
            shuffle: Shuffle = randomShuffle,
        ): FlashcardSession {
            require(words.isNotEmpty()) { "A session needs at least one word" }
            return FlashcardSession(words, shuffle(words), direction)
        }
    }
}
