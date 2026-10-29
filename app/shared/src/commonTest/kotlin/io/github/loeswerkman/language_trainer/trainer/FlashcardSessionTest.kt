package io.github.loeswerkman.language_trainer.trainer

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FlashcardSessionTest {

    private val cat = word(1, "cat", "кошка")
    private val dog = word(2, "dog", "собака")
    private val bird = word(3, "bird", "птица")
    private val words = listOf(cat, dog)

    @Test
    fun countsCorrectAnswersInTheGivenOrder() {
        val first = FlashcardSession.start(words).submit("cat")
        assertEquals(AnswerResult.CORRECT, first.lastResult)

        val second = first.next().submit("cat")
        assertEquals(dog, second.currentWord)
        assertEquals(AnswerResult.INCORRECT, second.lastResult)
        assertEquals(1, second.correctCount)
        assertTrue(second.isRoundFinished)
    }

    @Test
    fun canPractiseFromEnglish() {
        val session = FlashcardSession.start(words, TranslationDirection.FROM_ENGLISH).submit("кошка")
        assertEquals(AnswerResult.CORRECT, session.lastResult)
    }

    @Test
    fun cannotSkipAnswerTwiceOrGoPastTheEnd() {
        val session = FlashcardSession.start(words)
        assertFailsWith<IllegalStateException> { session.next() }
        val answered = session.submit("cat")
        assertFailsWith<IllegalStateException> { answered.submit("cat") }
        assertFailsWith<IllegalStateException> { answered.next().submit("dog").next() }
    }

    @Test
    fun replacesTheUpcomingCardsButKeepsAnsweredOnes() {
        val session = FlashcardSession.start(listOf(cat, dog, bird)).submit("cat").next()

        val changed = session.withRemaining(listOf(bird, cat))!!

        assertEquals(1, changed.position)
        assertEquals(bird, changed.currentWord)
        assertEquals(2, changed.size)
        assertEquals(1, changed.correctCount)
    }

    @Test
    fun keepsTheAnsweredCurrentCardAndItsFeedback() {
        val session = FlashcardSession.start(listOf(cat, dog)).submit("cat")

        val changed = session.withRemaining(listOf(bird))!!

        assertEquals(cat, changed.currentWord)
        assertEquals(AnswerResult.CORRECT, changed.lastResult)
        assertEquals(bird, changed.next().currentWord)

        val finished = session.withRemaining(listOf(cat))!!
        assertTrue(finished.isRoundFinished)
    }

    @Test
    fun returnsNullWhenNoUnansweredCardIsLeft() {
        val session = FlashcardSession.start(listOf(cat, dog)).submit("cat").next()
        assertNull(session.withRemaining(listOf(cat)))
        assertFalse(session.withRemaining(listOf(dog))!!.isAnswered)
    }

    private fun word(id: Long, word: String, translation: String) = Word(
        id = id,
        language = "russian",
        word = word,
        translation = translation,
        wordAlternatives = emptyList(),
        translationAlternatives = emptyList(),
        category = WordCategory.NOUN,
        tags = emptyList(),
        complexity = 1,
        usefulness = 3,
        notes = null,
    )
}
