package io.github.loeswerkman.language_trainer.trainer

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlashcardSessionTest {

    private val words = listOf(word(1, "cat", "кошка"), word(2, "dog", "собака"))
    private val keepOrder: Shuffle = { it }

    @Test
    fun countsCorrectAnswers() {
        val first = FlashcardSession.start(words, shuffle = keepOrder).submit("cat")
        assertEquals(AnswerResult.CORRECT, first.lastResult)

        val second = first.next(keepOrder).submit("cat")
        assertEquals(AnswerResult.INCORRECT, second.lastResult)
        assertEquals(1, second.correctCount)
        assertTrue(second.isRoundFinished)
    }

    @Test
    fun canPractiseFromEnglish() {
        val session = FlashcardSession.start(words, TranslationDirection.FROM_ENGLISH, keepOrder).submit("кошка")
        assertEquals(AnswerResult.CORRECT, session.lastResult)
    }

    @Test
    fun startsNewRoundAfterLastCard() {
        val session = FlashcardSession.start(words, shuffle = keepOrder)
            .submit("cat").next(keepOrder)
            .submit("dog").next(keepOrder)

        assertEquals(0, session.position)
        assertEquals(0, session.correctCount)
        assertFalse(session.isAnswered)
    }

    @Test
    fun cannotSkipOrAnswerTwice() {
        val session = FlashcardSession.start(words, shuffle = keepOrder)
        assertFailsWith<IllegalStateException> { session.next(keepOrder) }
        val answered = session.submit("cat")
        assertFailsWith<IllegalStateException> { answered.submit("cat") }
    }

    private fun word(id: Long, word: String, translation: String) = Word(
        id = id,
        language = "russian",
        word = word,
        translation = translation,
        alternatives = emptyList(),
        category = WordCategory.NOUN,
        tags = emptyList(),
        complexity = 1,
        usefulness = 3,
        notes = null,
    )
}
