package io.github.loeswerkman.language_trainer.query

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

data class Option(val value: String, val label: String)

/**
 * A property of a list item that can be shown, filtered and sorted on.
 * Each kind of field decides which operators it supports, how conditions match and how items compare.
 */
sealed interface Field<T> {
    val id: String
    val label: String
    val operators: List<Operator>

    /** The direction used when the user first sorts on this field. */
    val defaultDirection: SortDirection

    /** Whether [item] passes [condition]. [now] is in epoch milliseconds, for date conditions. */
    fun matches(item: T, condition: Condition, now: Long): Boolean
    fun comparator(): Comparator<T>

    /** The values a condition on this field can choose from, for operators that take options. */
    fun options(items: List<T>): List<Option> = emptyList()

    /** The value part of a condition in a summary, e.g. `"кот"` or `noun, verb`. */
    fun describeValue(condition: Condition): String = when (condition.operator.input) {
        OperatorInput.NONE -> ""
        OperatorInput.TEXT -> "\"${condition.text}\""
        OperatorInput.NUMBER -> condition.number.toString()
        OperatorInput.OPTIONS -> condition.values.sorted().joinToString()
    }

    fun describe(condition: Condition): String =
        listOf(label, condition.operator.label, describeValue(condition)).filter { it.isNotEmpty() }.joinToString(" ")
}

class TextField<T>(override val id: String, override val label: String, val get: (T) -> String) : Field<T> {
    override val operators = listOf(Operator.CONTAINS, Operator.IS, Operator.STARTS_WITH)
    override val defaultDirection = SortDirection.ASCENDING

    override fun matches(item: T, condition: Condition, now: Long): Boolean {
        val value = get(item).lowercase()
        val text = condition.text.trim().lowercase()
        return when (condition.operator) {
            Operator.CONTAINS -> value.contains(text)
            Operator.IS -> value == text
            Operator.STARTS_WITH -> value.startsWith(text)
            else -> true
        }
    }

    override fun comparator(): Comparator<T> = compareBy { get(it).lowercase() }
}

/** A field with one value out of a fixed list of [options], such as a word category. */
class ChoiceField<T>(
    override val id: String,
    override val label: String,
    val options: List<Option>,
    val get: (T) -> String,
) : Field<T> {
    override val operators = listOf(Operator.IS_ANY_OF, Operator.IS_NONE_OF)
    override val defaultDirection = SortDirection.ASCENDING

    override fun matches(item: T, condition: Condition, now: Long): Boolean = when (condition.operator) {
        Operator.IS_ANY_OF -> get(item) in condition.values
        Operator.IS_NONE_OF -> get(item) !in condition.values
        else -> true
    }

    override fun comparator(): Comparator<T> = compareBy { item -> options.indexOfFirst { it.value == get(item) } }

    override fun options(items: List<T>): List<Option> = options

    override fun describeValue(condition: Condition): String =
        options.filter { it.value in condition.values }.joinToString { it.label }
}

/** A field with any number of free-form values, such as tags. */
class TagsField<T>(override val id: String, override val label: String, val get: (T) -> List<String>) : Field<T> {
    override val operators = listOf(Operator.HAS_ANY_OF, Operator.HAS_ALL_OF, Operator.HAS_NONE_OF)
    override val defaultDirection = SortDirection.ASCENDING

    override fun matches(item: T, condition: Condition, now: Long): Boolean {
        val tags = get(item).toSet()
        return when (condition.operator) {
            Operator.HAS_ANY_OF -> condition.values.any { it in tags }
            Operator.HAS_ALL_OF -> tags.containsAll(condition.values)
            Operator.HAS_NONE_OF -> condition.values.none { it in tags }
            else -> true
        }
    }

    override fun comparator(): Comparator<T> = compareBy { get(it).sorted().joinToString() }

    /** The tags used by [items]. */
    override fun options(items: List<T>): List<Option> = items.flatMap(get).distinct().sorted().map { Option(it, it) }
}

class NumberField<T>(override val id: String, override val label: String, val get: (T) -> Int) : Field<T> {
    override val operators = listOf(
        Operator.EQUALS, Operator.NOT_EQUALS, Operator.LESS_THAN, Operator.AT_MOST, Operator.GREATER_THAN, Operator.AT_LEAST,
    )
    override val defaultDirection = SortDirection.DESCENDING

    override fun matches(item: T, condition: Condition, now: Long): Boolean {
        val value = get(item)
        return when (condition.operator) {
            Operator.EQUALS -> value == condition.number
            Operator.NOT_EQUALS -> value != condition.number
            Operator.LESS_THAN -> value < condition.number
            Operator.AT_MOST -> value <= condition.number
            Operator.GREATER_THAN -> value > condition.number
            Operator.AT_LEAST -> value >= condition.number
            else -> true
        }
    }

    override fun comparator(): Comparator<T> = compareBy { get(it) }
}

/**
 * A moment in epoch milliseconds, or null if it never happened (e.g. a word that was never answered).
 * "anytime" matches every item for which it did happen.
 */
class DateField<T>(override val id: String, override val label: String, val get: (T) -> Long?) : Field<T> {
    override val operators = listOf(Operator.NEVER, Operator.ANYTIME, Operator.WITHIN_LAST_DAYS, Operator.MORE_THAN_DAYS_AGO)
    override val defaultDirection = SortDirection.DESCENDING

    override fun matches(item: T, condition: Condition, now: Long): Boolean {
        val at = get(item)
        return when (condition.operator) {
            Operator.NEVER -> at == null
            Operator.ANYTIME -> at != null
            Operator.WITHIN_LAST_DAYS -> at != null && now - at <= condition.number * DAY_MILLIS
            Operator.MORE_THAN_DAYS_AGO -> at != null && now - at > condition.number * DAY_MILLIS
            else -> true
        }
    }

    /** Never comes first when ascending (oldest first) and last when descending. */
    override fun comparator(): Comparator<T> = Comparator { a, b -> compareValues(get(a), get(b)) }

    override fun describe(condition: Condition): String = when (condition.operator) {
        Operator.WITHIN_LAST_DAYS -> "$label within the last ${days(condition.number)}"
        Operator.MORE_THAN_DAYS_AGO -> "$label more than ${days(condition.number)} ago"
        else -> "$label ${condition.operator.label}"
    }

    private fun days(count: Int) = if (count == 1) "1 day" else "$count days"
}
