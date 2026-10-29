package io.github.loeswerkman.language_trainer.trainer

import io.github.loeswerkman.language_trainer.query.ListQuery
import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey
import io.github.loeswerkman.language_trainer.storage.Settings
import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.RecordedAnswer
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyItem
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyQueries
import io.github.loeswerkman.language_trainer.vocabulary.Word
import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrainingControllerTest {

    private val dog = VocabularyItem(word(1, "dog", complexity = 1))
    private val cat = VocabularyItem(word(2, "cat", complexity = 2))
    private val bird = VocabularyItem(word(3, "bird", complexity = 1), correct = 1, lastAnsweredAt = 1_000)
    private val fish = VocabularyItem(word(4, "fish", complexity = 1), correct = 5, lastAnsweredAt = 2_000)

    private class FakeApi(var items: List<VocabularyItem>) : TrainingApi {
        val submissions = mutableListOf<AnswerSubmission>()
        var failSaves = false

        override suspend fun fetchVocabulary(language: String) = items

        override suspend fun submitAnswer(sessionId: Long, submission: AnswerSubmission): RecordedAnswer {
            if (failSaves) error("offline")
            submissions += submission
            return RecordedAnswer(AnswerResult.CORRECT)
        }
    }

    private val settings = Settings.InMemory()

    private fun TestScope.controller(api: FakeApi) = TrainingController(
        api = api,
        session = TrainingSession(1, "russian"),
        initialItems = api.items,
        store = VocabularyQueries.trainingStore("russian", settings),
        scope = this,
        now = { 1_000_000 },
    )

    private val TrainingController.card get() = assertNotNull(visibleRound).currentWord.word

    @Test
    fun trainsUnlearnedWordsNeverAnsweredFirstThenEasiest() = runTest {
        val controller = controller(FakeApi(listOf(fish, bird, cat, dog)))

        assertEquals(3, controller.matchCount)
        assertEquals(3, controller.visibleRound?.size)
        assertEquals("dog", controller.card)
    }

    @Test
    fun savesAnswersAndMovesOn() = runTest {
        val api = FakeApi(listOf(bird, cat, dog))
        val controller = controller(api)

        controller.submit("dog")
        advanceUntilIdle()
        controller.next()

        assertEquals(listOf(AnswerSubmission(1, TranslationDirection.TO_ENGLISH, "dog")), api.submissions)
        assertEquals(1, controller.answeredCount)
        assertEquals("cat", controller.card)
    }

    @Test
    fun aNewQueryReordersTheRemainingCardsWithFreshStatistics() = runTest {
        val api = FakeApi(listOf(bird, cat, dog, fish))
        val controller = controller(api)
        controller.submit("dog")
        api.items = listOf(bird, cat.copy(correct = 5), dog, fish)

        controller.query.confirm(controller.query.query.copy(sort = listOf(SortKey.ByField("word", SortDirection.ASCENDING))))
        advanceUntilIdle()

        assertEquals("dog", controller.card)
        assertTrue(assertNotNull(controller.visibleRound).isAnswered)
        controller.next()
        assertEquals("bird", controller.card)
        assertTrue(!assertNotNull(controller.visibleRound).isAnswered)
        assertEquals(2, controller.matchCount)
        assertTrue(settings.values.containsKey("training-query.russian"))
    }

    @Test
    fun aNewRoundStopsWhenNoWordsMatchAnymore() = runTest {
        val api = FakeApi(listOf(dog))
        val controller = controller(api)
        controller.submit("dog")
        api.items = listOf(dog.copy(correct = 5))

        controller.next()
        assertTrue(controller.isReloading)
        assertNull(controller.visibleRound)
        advanceUntilIdle()

        assertTrue(!controller.isReloading)
        assertNull(controller.visibleRound)
        assertEquals(0, controller.matchCount)
    }

    @Test
    fun clearingTheFilterStartsTrainingAgain() = runTest {
        val controller = controller(FakeApi(listOf(fish)))
        assertNull(controller.visibleRound)

        controller.query.confirm(ListQuery())
        advanceUntilIdle()

        assertEquals("fish", controller.card)
    }

    @Test
    fun reportsAnswersThatCouldNotBeSaved() = runTest {
        val api = FakeApi(listOf(dog)).apply { failSaves = true }
        val controller = controller(api)

        controller.submit("dog")
        advanceUntilIdle()

        assertEquals("Could not save your answer: offline", controller.error)
    }

    private fun word(id: Long, word: String, complexity: Int) = Word(
        id = id,
        language = "russian",
        word = word,
        translation = "$word-ru",
        wordAlternatives = emptyList(),
        translationAlternatives = emptyList(),
        category = WordCategory.NOUN,
        tags = emptyList(),
        complexity = complexity,
        usefulness = 3,
        notes = null,
    )
}
