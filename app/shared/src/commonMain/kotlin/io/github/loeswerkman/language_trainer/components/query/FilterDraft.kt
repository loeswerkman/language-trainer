package io.github.loeswerkman.language_trainer.components.query

import io.github.loeswerkman.language_trainer.query.Condition
import io.github.loeswerkman.language_trainer.query.ConditionGroup
import io.github.loeswerkman.language_trainer.query.Field
import io.github.loeswerkman.language_trainer.query.FieldSet
import io.github.loeswerkman.language_trainer.query.Filter
import io.github.loeswerkman.language_trainer.query.Operator
import io.github.loeswerkman.language_trainer.query.OperatorInput

/** A condition while it is being edited: the number is kept as typed, so it can be invalid. */
data class DraftCondition(
    val id: Int,
    val field: String,
    val operator: Operator,
    val text: String = "",
    val number: String = "",
    val values: Set<String> = emptySet(),
) {
    val isValid: Boolean
        get() = when (operator.input) {
            OperatorInput.NONE -> true
            OperatorInput.TEXT -> text.isNotBlank()
            OperatorInput.NUMBER -> number.trim().toIntOrNull() != null
            OperatorInput.OPTIONS -> values.isNotEmpty()
        }

    fun toCondition() = Condition(field, operator, text.trim(), number.trim().toIntOrNull() ?: 0, values)
}

data class DraftGroup(val id: Int, val conditions: List<DraftCondition>)

/** The filter dialog's state. Every change returns a new draft; ids keep the UI rows stable. */
data class FilterDraft<T>(val fields: FieldSet<T>, val groups: List<DraftGroup>, private val nextId: Int) {

    val isValid: Boolean get() = groups.all { group -> group.conditions.all { it.isValid } }

    fun toFilter() = Filter(
        groups.map { group -> ConditionGroup(group.conditions.map { it.toCondition() }) }.filter { it.conditions.isNotEmpty() },
    )

    /** Adds a condition on [field] (AND) to [groupId], or to a new group (OR) if [groupId] is null. */
    fun addCondition(field: Field<T>, groupId: Int? = null): FilterDraft<T> {
        val condition = DraftCondition(nextId, field.id, field.operators.first())
        return if (groupId == null) {
            copy(groups = groups + DraftGroup(nextId + 1, listOf(condition)), nextId = nextId + 2)
        } else {
            copy(
                groups = groups.map { if (it.id == groupId) it.copy(conditions = it.conditions + condition) else it },
                nextId = nextId + 1,
            )
        }
    }

    /** Replaces the condition with the same id. Changing its field starts the condition afresh. */
    fun update(condition: DraftCondition): FilterDraft<T> {
        val old = groups.flatMap { it.conditions }.firstOrNull { it.id == condition.id } ?: return this
        val field = fields.field(condition.field) ?: return this
        val updated = if (old.field == condition.field) condition else DraftCondition(condition.id, field.id, field.operators.first())
        return copy(
            groups = groups.map { group ->
                group.copy(conditions = group.conditions.map { if (it.id == updated.id) updated else it })
            },
        )
    }

    /** Removes the condition, and its group if that becomes empty. */
    fun remove(conditionId: Int): FilterDraft<T> = copy(
        groups = groups
            .map { group -> group.copy(conditions = group.conditions.filterNot { it.id == conditionId }) }
            .filter { it.conditions.isNotEmpty() },
    )

    fun clear(): FilterDraft<T> = copy(groups = emptyList())

    companion object {
        /** Starts editing [filter]. With a [preselected] field, a condition on it is added to the last group. */
        fun <T> from(fields: FieldSet<T>, filter: Filter, preselected: Field<T>? = null): FilterDraft<T> {
            var id = 0
            val groups = filter.groups.map { group ->
                val conditions = group.conditions.map {
                    val number = if (it.operator.input == OperatorInput.NUMBER) it.number.toString() else ""
                    DraftCondition(id++, it.field, it.operator, it.text, number, it.values)
                }
                DraftGroup(id++, conditions)
            }
            val draft = FilterDraft(fields, groups, id)
            return if (preselected == null) draft else draft.addCondition(preselected, groups.lastOrNull()?.id)
        }
    }
}
