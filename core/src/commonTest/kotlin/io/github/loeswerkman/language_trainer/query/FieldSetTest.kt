package io.github.loeswerkman.language_trainer.query

import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSetTest {

    private data class Letter(val name: String, val kind: String, val tags: List<String>, val level: Int, val seenAt: Long?)

    private val name = TextField<Letter>("name", "Name") { it.name }
    private val kind = ChoiceField<Letter>("kind", "Kind", listOf(Option("vowel", "Vowel"), Option("consonant", "Consonant"))) { it.kind }
    private val tags = TagsField<Letter>("tags", "Tags") { it.tags }
    private val level = NumberField<Letter>("level", "Level") { it.level }
    private val seen = DateField<Letter>("seen", "Seen") { it.seenAt }
    private val fields = FieldSet(listOf(name, kind, tags, level, seen))

    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day
    private val a = Letter("A", "vowel", listOf("easy"), 1, now - day)
    private val b = Letter("B", "consonant", listOf("easy", "plosive"), 2, now - 10 * day)
    private val c = Letter("C", "consonant", listOf("hard"), 3, null)
    private val letters = listOf(a, b, c)

    private fun filter(vararg groups: List<Condition>) = Filter(groups.map { ConditionGroup(it) })
    private fun matching(filter: Filter) = letters.filter { fields.matches(it, filter, now) }

    @Test
    fun emptyFilterMatchesEverything() {
        assertEquals(letters, matching(Filter()))
    }

    @Test
    fun conditionsInAGroupMustAllMatch() {
        val f = filter(listOf(Condition("kind", Operator.IS_ANY_OF, values = setOf("consonant")), Condition("level", Operator.AT_LEAST, number = 3)))
        assertEquals(listOf(c), matching(f))
    }

    @Test
    fun anyGroupMayMatch() {
        val f = filter(
            listOf(Condition("name", Operator.IS, text = "a")),
            listOf(Condition("level", Operator.EQUALS, number = 3)),
        )
        assertEquals(listOf(a, c), matching(f))
    }

    @Test
    fun textOperatorsIgnoreCaseAndSurroundingSpaces() {
        assertEquals(listOf(b), matching(filter(listOf(Condition("name", Operator.STARTS_WITH, text = " b ")))))
        assertEquals(listOf(a), matching(filter(listOf(Condition("name", Operator.CONTAINS, text = "a")))))
    }

    @Test
    fun choiceAndTagOperators() {
        assertEquals(listOf(a), matching(filter(listOf(Condition("kind", Operator.IS_NONE_OF, values = setOf("consonant"))))))
        assertEquals(listOf(a, b), matching(filter(listOf(Condition("tags", Operator.HAS_ANY_OF, values = setOf("easy", "plosive"))))))
        assertEquals(listOf(b), matching(filter(listOf(Condition("tags", Operator.HAS_ALL_OF, values = setOf("easy", "plosive"))))))
        assertEquals(listOf(c), matching(filter(listOf(Condition("tags", Operator.HAS_NONE_OF, values = setOf("easy"))))))
    }

    @Test
    fun numberOperators() {
        fun levels(operator: Operator) = matching(filter(listOf(Condition("level", operator, number = 2)))).map { it.level }
        assertEquals(listOf(2), levels(Operator.EQUALS))
        assertEquals(listOf(1, 3), levels(Operator.NOT_EQUALS))
        assertEquals(listOf(1), levels(Operator.LESS_THAN))
        assertEquals(listOf(1, 2), levels(Operator.AT_MOST))
        assertEquals(listOf(3), levels(Operator.GREATER_THAN))
        assertEquals(listOf(2, 3), levels(Operator.AT_LEAST))
    }

    @Test
    fun dateOperators() {
        assertEquals(listOf(c), matching(filter(listOf(Condition("seen", Operator.NEVER)))))
        assertEquals(listOf(a, b), matching(filter(listOf(Condition("seen", Operator.ANYTIME)))))
        assertEquals("Seen anytime", fields.describe(filter(listOf(Condition("seen", Operator.ANYTIME)))))
        assertEquals(listOf(a), matching(filter(listOf(Condition("seen", Operator.WITHIN_LAST_DAYS, number = 7)))))
        assertEquals(listOf(b), matching(filter(listOf(Condition("seen", Operator.MORE_THAN_DAYS_AGO, number = 7)))))
    }

    @Test
    fun ignoresUnknownFieldsAndUnsupportedOperators() {
        val f = filter(listOf(Condition("missing", Operator.IS, text = "x"), Condition("level", Operator.CONTAINS, text = "x")))
        assertEquals(letters, matching(f))
    }

    @Test
    fun sortsOnSeveralKeys() {
        val sorted = fields.sort(
            letters,
            listOf(SortKey.ByField("kind", SortDirection.DESCENDING), SortKey.ByField("level", SortDirection.DESCENDING)),
        )
        assertEquals(listOf(c, b, a), sorted)
    }

    @Test
    fun neverComesFirstWhenSortingDatesAscending() {
        assertEquals(listOf(c, b, a), fields.sort(letters, listOf(SortKey.ByField("seen", SortDirection.ASCENDING))))
        assertEquals(listOf(a, b, c), fields.sort(letters, listOf(SortKey.ByField("seen", SortDirection.DESCENDING))))
    }

    @Test
    fun randomKeyShufflesTiesOnly() {
        val many = (1..20).map { Letter("L$it", if (it % 2 == 0) "vowel" else "consonant", emptyList(), it, null) }
        val sorted = fields.sort(many, listOf(SortKey.ByField("kind", SortDirection.ASCENDING), SortKey.Random), Random(42))

        assertEquals(List(10) { "vowel" } + List(10) { "consonant" }, sorted.map { it.kind })
        assertEquals(many.toSet(), sorted.toSet())
        assertTrue(sorted.take(10) != many.filter { it.kind == "vowel" })
    }

    @Test
    fun applyFiltersAndSorts() {
        val query = ListQuery(filter(listOf(Condition("level", Operator.AT_LEAST, number = 2))), listOf(SortKey.ByField("level", SortDirection.DESCENDING)))
        assertEquals(listOf(c, b), fields.apply(letters, query, now))
        assertEquals(letters, fields.apply(letters, ListQuery(), now))
    }

    @Test
    fun clickingAHeaderSortsThenFlipsThenRemovesTheField() {
        val current = listOf(SortKey.ByField("level", SortDirection.DESCENDING), SortKey.Random)

        val first = fields.sortBy(name, current)
        assertEquals(listOf(SortKey.ByField("name", SortDirection.ASCENDING), current[0], SortKey.Random), first)
        val second = fields.sortBy(name, first)
        assertEquals(listOf(SortKey.ByField("name", SortDirection.DESCENDING), current[0], SortKey.Random), second)
        assertEquals(current, fields.sortBy(name, second))
    }

    @Test
    fun removingTheLastKeyFallsBackToTheDefaultSort() {
        val flipped = fields.sortBy(level, fields.sortBy(level, emptyList()))
        assertEquals(listOf(SortKey.ByField("level", SortDirection.ASCENDING)), flipped)
        assertEquals(emptyList(), fields.sortBy(level, flipped))
    }

    @Test
    fun sanitizeDropsWhatTheFieldsCannotUse() {
        val query = ListQuery(
            filter = filter(
                listOf(Condition("missing", Operator.IS)),
                listOf(Condition("level", Operator.AT_LEAST, number = 2), Condition("level", Operator.CONTAINS)),
            ),
            sort = listOf(SortKey.ByField("missing", SortDirection.ASCENDING), SortKey.Random),
        )
        assertEquals(
            ListQuery(filter(listOf(Condition("level", Operator.AT_LEAST, number = 2))), listOf(SortKey.Random)),
            fields.sanitize(query),
        )
    }

    @Test
    fun describesFiltersAndSorts() {
        val f = filter(
            listOf(Condition("kind", Operator.IS_ANY_OF, values = setOf("vowel")), Condition("level", Operator.AT_MOST, number = 2)),
            listOf(Condition("seen", Operator.MORE_THAN_DAYS_AGO, number = 1)),
        )
        assertEquals("(Kind is any of Vowel AND Level ≤ 2) OR (Seen more than 1 day ago)", fields.describe(f))
        assertEquals("Name contains \"x\"", fields.describe(filter(listOf(Condition("name", Operator.CONTAINS, text = "x")))))
        assertEquals("Level ↓, Random", fields.describe(listOf(SortKey.ByField("level", SortDirection.DESCENDING), SortKey.Random)))
    }

    @Test
    fun optionsComeFromTheFieldOrFromTheItems() {
        assertEquals(listOf("vowel", "consonant"), kind.options(letters).map { it.value })
        assertEquals(listOf("easy", "hard", "plosive"), tags.options(letters).map { it.value })
        assertTrue(level.options(letters).isEmpty())
    }

    @Test
    fun savedQueriesUseStableNames() {
        val query = ListQuery(
            filter(listOf(Condition("level", Operator.AT_MOST, number = 2))),
            listOf(SortKey.ByField("level", SortDirection.DESCENDING), SortKey.Random),
        )
        assertEquals(
            """{"filter":{"groups":[{"conditions":[{"field":"level","operator":"lte","number":2}]}]},""" +
                """"sort":[{"type":"field","field":"level","direction":"desc"},{"type":"random"}]}""",
            Json.encodeToString(query),
        )
    }

    @Test
    fun queriesSurviveJsonRoundTrips() {
        val query = ListQuery(
            filter(listOf(Condition("tags", Operator.HAS_ALL_OF, values = setOf("easy")))),
            listOf(SortKey.ByField("level", SortDirection.DESCENDING), SortKey.Random),
        )
        assertEquals(query, Json.decodeFromString<ListQuery>(Json.encodeToString(query)))
    }
}
