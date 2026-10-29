package io.github.loeswerkman.language_trainer.query

import kotlin.random.Random

/**
 * The fields of one kind of list (e.g. vocabulary). Filters and sorts that list with a [ListQuery].
 * Conditions and sort keys that refer to unknown fields or unsupported operators are ignored, so a stored query
 * keeps working when fields change.
 */
class FieldSet<T>(val fields: List<Field<T>>) {

    init {
        require(fields.map { it.id }.toSet().size == fields.size) { "Field ids must be unique" }
    }

    fun field(id: String): Field<T>? = fields.firstOrNull { it.id == id }

    fun matches(item: T, filter: Filter, now: Long): Boolean =
        filter.groups.isEmpty() || filter.groups.any { group -> group.conditions.all { matches(item, it, now) } }

    private fun matches(item: T, condition: Condition, now: Long): Boolean {
        val field = field(condition.field)?.takeIf { condition.operator in it.operators } ?: return true
        return field.matches(item, condition, now)
    }

    fun sort(items: List<T>, keys: List<SortKey>, random: Random = Random.Default): List<T> {
        val comparators = keys.mapNotNull { comparator(it, items, random) }
        if (comparators.isEmpty()) return items
        val combined = comparators.reduce { first, next -> first.then(next) }
        return items.indices.sortedWith(combined).map { items[it] }
    }

    /** Compares item indices, so random keys can give each item one fixed random number. */
    private fun comparator(key: SortKey, items: List<T>, random: Random): Comparator<Int>? = when (key) {
        SortKey.Random -> {
            val numbers = DoubleArray(items.size) { random.nextDouble() }
            compareBy { numbers[it] }
        }
        is SortKey.ByField -> field(key.field)?.comparator()?.let { byItem ->
            val byIndex = Comparator<Int> { a, b -> byItem.compare(items[a], items[b]) }
            if (key.direction == SortDirection.DESCENDING) byIndex.reversed() else byIndex
        }
    }

    fun apply(items: List<T>, query: ListQuery, now: Long, random: Random = Random.Default): List<T> =
        sort(items.filter { matches(it, query.filter, now) }, query.sort, random)

    /** Removes conditions, groups and sort keys this field set can't use. */
    fun sanitize(query: ListQuery): ListQuery = ListQuery(
        filter = Filter(
            query.filter.groups
                .map { group -> ConditionGroup(group.conditions.filter { isValid(it) }) }
                .filter { it.conditions.isNotEmpty() },
        ),
        sort = query.sort.filter { it is SortKey.Random || (it is SortKey.ByField && field(it.field) != null) },
    )

    private fun isValid(condition: Condition): Boolean =
        field(condition.field)?.let { condition.operator in it.operators } ?: false

    /**
     * What clicking [field]'s column header does to [current]: the first click makes it the main sort key in its
     * default direction, the second flips the direction, the third removes it. Other keys remain as tie-breakers.
     * When no keys are left the list keeps its original order.
     */
    fun sortBy(field: Field<T>, current: List<SortKey>): List<SortKey> {
        val first = current.firstOrNull()
        val rest = current.filterNot { it is SortKey.ByField && it.field == field.id }
        if (first !is SortKey.ByField || first.field != field.id) {
            return listOf(SortKey.ByField(field.id, field.defaultDirection)) + rest
        }
        if (first.direction == field.defaultDirection) {
            return listOf(first.copy(direction = first.direction.flipped())) + rest
        }
        return rest
    }

    /** A readable summary, e.g. `(Category is any of noun AND Complexity ≤ 2) OR Incorrect ≥ 3`. */
    fun describe(filter: Filter): String {
        val groups = filter.groups.map { group -> group.conditions.joinToString(" AND ") { describe(it) } }
        return if (groups.size == 1) groups.single() else groups.joinToString(" OR ") { "($it)" }
    }

    fun describe(condition: Condition): String = field(condition.field)?.describe(condition) ?: condition.field

    fun describe(sort: List<SortKey>): String = sort.joinToString { key ->
        when (key) {
            SortKey.Random -> "Random"
            is SortKey.ByField -> "${field(key.field)?.label ?: key.field} ${if (key.direction == SortDirection.ASCENDING) "↑" else "↓"}"
        }
    }

    /** Ids of the fields used in [filter], e.g. to highlight their columns. */
    fun filteredFieldIds(filter: Filter): Set<String> = filter.groups.flatMap { g -> g.conditions.map { it.field } }.toSet()
}

private fun SortDirection.flipped() =
    if (this == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
