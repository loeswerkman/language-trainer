package io.github.loeswerkman.language_trainer.components.query

import io.github.loeswerkman.language_trainer.query.Condition
import io.github.loeswerkman.language_trainer.query.ConditionGroup
import io.github.loeswerkman.language_trainer.query.Filter
import io.github.loeswerkman.language_trainer.query.Operator
import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyFields
import io.github.loeswerkman.language_trainer.vocabulary.describeLastAnswered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueryDraftTest {

    private val fields = VocabularyFields.all
    private val complexityAtMost2 = Condition("complexity", Operator.AT_MOST, number = 2)

    @Test
    fun editsConditionsIntoGroups() {
        var draft = FilterDraft.from(fields, Filter())
        draft = draft.addCondition(VocabularyFields.category)
        val group = draft.groups.single()
        draft = draft.update(group.conditions.single().copy(values = setOf("noun")))
        draft = draft.addCondition(VocabularyFields.complexity, group.id)
        draft = draft.update(draft.groups.single().conditions[1].copy(operator = Operator.AT_MOST, number = " 2 "))
        draft = draft.addCondition(VocabularyFields.lastAnswered)

        assertTrue(draft.isValid)
        assertEquals(
            Filter(
                listOf(
                    ConditionGroup(listOf(Condition("category", Operator.IS_ANY_OF, values = setOf("noun")), complexityAtMost2)),
                    ConditionGroup(listOf(Condition("lastAnswered", Operator.NEVER))),
                ),
            ),
            draft.toFilter(),
        )
    }

    @Test
    fun invalidValuesBlockConfirming() {
        val draft = FilterDraft.from(fields, Filter()).addCondition(VocabularyFields.complexity)
        assertFalse(draft.isValid)
        assertFalse(draft.update(draft.groups.single().conditions.single().copy(number = "two")).isValid)
        assertTrue(draft.update(draft.groups.single().conditions.single().copy(number = "2")).isValid)
    }

    @Test
    fun aPreselectedFieldIsAddedToTheLastGroup() {
        val draft = FilterDraft.from(fields, Filter(listOf(ConditionGroup(listOf(complexityAtMost2)))), VocabularyFields.word)

        val conditions = draft.groups.single().conditions
        assertEquals(listOf("complexity", "word"), conditions.map { it.field })
        assertEquals("2", conditions[0].number)
        assertEquals(2, conditions.map { it.id }.toSet().size)
    }

    @Test
    fun changingTheFieldStartsTheConditionAfresh() {
        val draft = FilterDraft.from(fields, Filter(listOf(ConditionGroup(listOf(complexityAtMost2)))))
        val changed = draft.update(draft.groups.single().conditions.single().copy(field = "word"))
        assertEquals(Operator.CONTAINS, changed.groups.single().conditions.single().operator)
        assertEquals("", changed.groups.single().conditions.single().number)
    }

    @Test
    fun removingTheLastConditionRemovesItsGroup() {
        val draft = FilterDraft.from(fields, Filter(listOf(ConditionGroup(listOf(complexityAtMost2)))))
        assertEquals(Filter(), draft.remove(draft.groups.single().conditions.single().id).toFilter())
    }

    @Test
    fun editsSortKeys() {
        val draft = SortDraft(listOf(SortKey.ByField("score", SortDirection.DESCENDING), SortKey.ByField("word", SortDirection.ASCENDING)))

        val edited = draft.toggleDirection(1).add(SortKey.Random).moveUp(2)
        assertEquals(
            listOf(
                SortKey.ByField("score", SortDirection.DESCENDING),
                SortKey.Random,
                SortKey.ByField("word", SortDirection.DESCENDING),
            ),
            edited.keys,
        )
        assertEquals(emptyList(), edited.remove(0).remove(0).remove(0).keys)
    }

    @Test
    fun describesWhenAWordWasLastAnswered() {
        val day = 24L * 60 * 60 * 1000
        assertEquals("never", describeLastAnswered(null, 10 * day))
        assertEquals("today", describeLastAnswered(10 * day - 1, 10 * day))
        assertEquals("yesterday", describeLastAnswered(9 * day, 10 * day))
        assertEquals("3 days ago", describeLastAnswered(7 * day, 10 * day))
    }
}
