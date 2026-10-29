package io.github.loeswerkman.language_trainer.query

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What the user typed or picked for a condition, and so which input the filter dialog shows. */
enum class OperatorInput { NONE, TEXT, NUMBER, OPTIONS }

/** Saved in the browser: keep the [SerialName]s stable when renaming entries. */
@Serializable
enum class Operator(val label: String, val input: OperatorInput) {
    @SerialName("contains")
    CONTAINS("contains", OperatorInput.TEXT),
    @SerialName("is")
    IS("is", OperatorInput.TEXT),
    @SerialName("startsWith")
    STARTS_WITH("starts with", OperatorInput.TEXT),

    @SerialName("isAnyOf")
    IS_ANY_OF("is any of", OperatorInput.OPTIONS),
    @SerialName("isNoneOf")
    IS_NONE_OF("is none of", OperatorInput.OPTIONS),

    @SerialName("hasAnyOf")
    HAS_ANY_OF("has any of", OperatorInput.OPTIONS),
    @SerialName("hasAllOf")
    HAS_ALL_OF("has all of", OperatorInput.OPTIONS),
    @SerialName("hasNoneOf")
    HAS_NONE_OF("has none of", OperatorInput.OPTIONS),

    @SerialName("eq")
    EQUALS("=", OperatorInput.NUMBER),
    @SerialName("ne")
    NOT_EQUALS("≠", OperatorInput.NUMBER),
    @SerialName("lt")
    LESS_THAN("<", OperatorInput.NUMBER),
    @SerialName("lte")
    AT_MOST("≤", OperatorInput.NUMBER),
    @SerialName("gt")
    GREATER_THAN(">", OperatorInput.NUMBER),
    @SerialName("gte")
    AT_LEAST("≥", OperatorInput.NUMBER),

    @SerialName("never")
    NEVER("never", OperatorInput.NONE),
    @SerialName("anytime")
    ANYTIME("anytime", OperatorInput.NONE),
    @SerialName("withinLastDays")
    WITHIN_LAST_DAYS("within the last … days", OperatorInput.NUMBER),
    @SerialName("moreThanDaysAgo")
    MORE_THAN_DAYS_AGO("more than … days ago", OperatorInput.NUMBER),
}

/**
 * One check on a single field, e.g. "complexity ≤ 2". Only the value matching the operator's [Operator.input] is used:
 * [text] for text, [number] for numbers and days, [values] for options.
 */
@Serializable
data class Condition(
    val field: String,
    val operator: Operator,
    val text: String = "",
    val number: Int = 0,
    val values: Set<String> = emptySet(),
)

/** Conditions that must all match (AND). */
@Serializable
data class ConditionGroup(val conditions: List<Condition>)

/** Matches items that match any of the [groups] (OR). Without groups, everything matches. */
@Serializable
data class Filter(val groups: List<ConditionGroup> = emptyList()) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

@Serializable
enum class SortDirection {
    @SerialName("asc")
    ASCENDING,

    @SerialName("desc")
    DESCENDING,
}

@Serializable
sealed interface SortKey {
    @Serializable
    @SerialName("field")
    data class ByField(val field: String, val direction: SortDirection) : SortKey

    /** Shuffles the items. As a later key it shuffles items that are equal on the earlier keys. */
    @Serializable
    @SerialName("random")
    data object Random : SortKey
}

/**
 * The filter and sort of a list. An empty [sort] keeps the items in their original order.
 * Queries are saved in the browser and refer to fields by id, so field ids must stay stable too.
 */
@Serializable
data class ListQuery(val filter: Filter = Filter(), val sort: List<SortKey> = emptyList())
