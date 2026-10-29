package io.github.loeswerkman.language_trainer.components.query

import io.github.loeswerkman.language_trainer.query.ListQuery
import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey
import io.github.loeswerkman.language_trainer.storage.Settings
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyFields
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyQueries
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueryStateTest {

    private val settings = Settings.InMemory()
    private val changes = mutableListOf<ListQuery>()
    private fun state() = QueryState(VocabularyQueries.tableStore("russian", settings), onChange = { changes += it })

    @Test
    fun startsWithTheDefaultQuery() {
        assertEquals(VocabularyQueries.tableDefault, state().query)
    }

    @Test
    fun confirmingSavesReportsAndClosesTheDialog() {
        val state = state()
        state.openSort()
        val byScore = ListQuery(sort = listOf(SortKey.ByField("score", SortDirection.DESCENDING)))

        state.confirm(byScore)

        assertNull(state.dialog)
        assertEquals(listOf(byScore), changes)
        assertEquals(byScore, state().query)
    }

    @Test
    fun theDefaultSortCanBeTurnedOffAndStaysOff() {
        val state = state()
        state.sortBy(VocabularyFields.word)
        assertEquals(listOf(SortKey.ByField("word", SortDirection.DESCENDING)), state.query.sort)
        state.sortBy(VocabularyFields.word)
        assertEquals(emptyList(), state.query.sort)

        assertEquals(emptyList(), state().query.sort)
    }

    @Test
    fun ignoresBrokenSavedQueries() {
        settings.save("vocabulary-query.russian", "{not json")
        assertEquals(VocabularyQueries.tableDefault, state().query)
    }

    @Test
    fun theTableAndTrainingKeepSeparateQueries() {
        state().confirm(ListQuery())

        assertEquals(setOf("vocabulary-query.russian"), settings.values.keys)
        assertEquals(VocabularyQueries.trainingDefault, VocabularyQueries.trainingStore("russian", settings).load())
    }
}
