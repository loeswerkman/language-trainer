package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection.FROM_ENGLISH
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection.TO_ENGLISH
import kotlin.test.Test
import kotlin.test.assertEquals

class AnswerEvaluationTest {

    private val cat = Word(
        id = 1,
        language = "russian",
        word = "cat",
        translation = "кошка",
        translationAlternatives = listOf("кот"),
        category = WordCategory.NOUN,
        tags = emptyList(),
        complexity = 1,
        usefulness = 4,
        notes = null,
    )

    @Test
    fun toEnglishShowsTranslationAndExpectsEnglishWord() {
        assertEquals("кошка", cat.prompt(TO_ENGLISH))
        assertEquals(AnswerResult.CORRECT, evaluateAnswer("cat", cat, TO_ENGLISH))
        assertEquals(AnswerResult.INCORRECT, evaluateAnswer("кошка", cat, TO_ENGLISH))
    }

    @Test
    fun fromEnglishShowsEnglishWordAndExpectsTranslation() {
        assertEquals("cat", cat.prompt(FROM_ENGLISH))
        assertEquals(AnswerResult.CORRECT, evaluateAnswer("кошка", cat, FROM_ENGLISH))
    }

    @Test
    fun alternativesAreAcceptedFromEnglish() {
        assertEquals(AnswerResult.CORRECT, evaluateAnswer("кот", cat, FROM_ENGLISH))
        assertEquals(listOf("кошка", "кот"), cat.acceptedAnswers(FROM_ENGLISH))
        assertEquals(AnswerResult.INCORRECT, evaluateAnswer("кот", cat, TO_ENGLISH))
    }

    @Test
    fun alternativesAreAcceptedToEnglish() {
        val big = cat.copy(word = "big", wordAlternatives = listOf("large"), translation = "большой")

        assertEquals(listOf("big", "large"), big.acceptedAnswers(TO_ENGLISH))
        assertEquals("big", big.prompt(FROM_ENGLISH))
        assertEquals(AnswerResult.CORRECT, evaluateAnswer(" LARGE ", big, TO_ENGLISH))
        assertEquals(AnswerResult.ALMOST_CORRECT, evaluateAnswer("larg", big, TO_ENGLISH))
        assertEquals(AnswerResult.INCORRECT, evaluateAnswer("large", big, FROM_ENGLISH))
    }

    @Test
    fun caseAndWhitespaceAreIgnored() {
        assertEquals(AnswerResult.CORRECT, evaluateAnswer("  Cat ", cat, TO_ENGLISH))
    }

    @Test
    fun oneTypoIsAlmostCorrect() {
        assertEquals(AnswerResult.ALMOST_CORRECT, evaluateAnswer("cut", cat, TO_ENGLISH))
        assertEquals(AnswerResult.ALMOST_CORRECT, evaluateAnswer("кошко", cat, FROM_ENGLISH))
    }

    @Test
    fun moreThanOneTypoIsIncorrect() {
        assertEquals(AnswerResult.INCORRECT, evaluateAnswer("dog", cat, TO_ENGLISH))
    }

    @Test
    fun emptyAnswerIsIncorrect() {
        assertEquals(AnswerResult.INCORRECT, evaluateAnswer("   ", cat, TO_ENGLISH))
    }

    @Test
    fun levenshteinDistanceCountsEdits() {
        assertEquals(0, levenshteinDistance("kitten", "kitten"))
        assertEquals(3, levenshteinDistance("kitten", "sitting"))
        assertEquals(4, levenshteinDistance("", "test"))
    }
}
