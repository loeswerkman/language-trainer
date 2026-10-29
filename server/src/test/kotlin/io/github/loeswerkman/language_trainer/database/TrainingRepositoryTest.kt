package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection.FROM_ENGLISH
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection.TO_ENGLISH
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyEntry
import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import io.github.loeswerkman.language_trainer.vocabulary.WordStatistics
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalPathApi::class)
class TrainingRepositoryTest {

    private val tempDir = Files.createTempDirectory("training-repository-test")
    private val database = connectDatabase(tempDir.resolve("test.db"))
    private val words = WordRepository(database)
    private val training = TrainingRepository(database, words)

    @AfterTest
    fun cleanUp() {
        tempDir.deleteRecursively()
    }

    @Test
    fun evaluatesAndSavesAnswers() {
        words.sync("russian", listOf(entry("cat", "кошка")))
        val session = training.startSession("russian")
        val cat = words.findAll().single()

        assertEquals(AnswerResult.CORRECT, training.recordAnswer(session, AnswerSubmission(cat.id, TO_ENGLISH, "Cat")))
        assertEquals(AnswerResult.ALMOST_CORRECT, training.recordAnswer(session, AnswerSubmission(cat.id, FROM_ENGLISH, "кошко")))
        assertEquals(AnswerResult.INCORRECT, training.recordAnswer(session, AnswerSubmission(cat.id, TO_ENGLISH, "dog")))

        assertEquals(listOf(WordStatistics(cat.id, "cat", "кошка", 1, 1, 1)), training.sessionStatistics(session.id))
    }

    @Test
    fun rejectsUnknownWordsAndWordsOfOtherLanguages() {
        words.sync("dutch", listOf(entry("cat", "kat")))
        val session = training.startSession("russian")

        assertNull(training.recordAnswer(session, AnswerSubmission(words.findAll().single().id, TO_ENGLISH, "cat")))
        assertNull(training.recordAnswer(session, AnswerSubmission(999, TO_ENGLISH, "cat")))
        assertEquals(emptyList(), training.sessionStatistics(session.id))
    }

    @Test
    fun sessionStatisticsOnlyCountThatSession() {
        words.sync("russian", listOf(entry("cat", "кошка"), entry("dog", "собака")))
        val (cat, dog) = words.findAll()
        val first = training.startSession("russian")
        val second = training.startSession("russian")
        training.recordAnswer(first, AnswerSubmission(cat.id, TO_ENGLISH, "cat"))
        training.recordAnswer(second, AnswerSubmission(dog.id, TO_ENGLISH, "wrong"))

        assertEquals(listOf("cat"), training.sessionStatistics(first.id).map { it.word })
        assertEquals(listOf(WordStatistics(dog.id, "dog", "собака", 0, 0, 1)), training.sessionStatistics(second.id))
    }

    @Test
    fun vocabularyStatisticsCountAllSessionsAndIncludeUnansweredWords() {
        words.sync("russian", listOf(entry("cat", "кошка"), entry("dog", "собака"), entry("bird", "птица")))
        words.sync("dutch", listOf(entry("cat", "kat")))
        val (cat, dog) = words.findAll("russian")
        repeat(2) {
            val session = training.startSession("russian")
            training.recordAnswer(session, AnswerSubmission(cat.id, TO_ENGLISH, "cat"))
            training.recordAnswer(session, AnswerSubmission(dog.id, TO_ENGLISH, "wrong"))
        }

        val statistics = training.vocabularyStatistics("russian")

        assertEquals(listOf("cat", "bird", "dog"), statistics.map { it.word })
        assertEquals(WordStatistics(cat.id, "cat", "кошка", 2, 0, 0), statistics[0])
        assertEquals(0, statistics[1].correct + statistics[1].almostCorrect + statistics[1].incorrect)
        assertEquals(2, statistics[2].incorrect)
    }

    @Test
    fun keepsAnswersWhenWordsAreUpdated() {
        val cat = entry("cat", "кошка")
        words.sync("russian", listOf(cat))
        val session = training.startSession("russian")
        training.recordAnswer(session, AnswerSubmission(words.findAll().single().id, TO_ENGLISH, "cat"))

        words.sync("russian", listOf(cat.copy(translationAlternatives = listOf("кот"))))

        assertEquals(1, training.vocabularyStatistics("russian").single().correct)
    }

    private fun entry(word: String, translation: String) = VocabularyEntry(
        word = word,
        translation = translation,
        category = WordCategory.NOUN,
        tags = emptyList(),
        complexity = 1,
        usefulness = 3,
    )
}
