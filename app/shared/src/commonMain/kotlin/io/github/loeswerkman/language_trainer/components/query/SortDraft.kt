package io.github.loeswerkman.language_trainer.components.query

import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey

/** The sort dialog's state. */
data class SortDraft(val keys: List<SortKey>) {

    fun replace(index: Int, key: SortKey) = edit { set(index, key) }

    fun toggleDirection(index: Int): SortDraft {
        val key = keys[index] as? SortKey.ByField ?: return this
        val flipped = if (key.direction == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
        return replace(index, key.copy(direction = flipped))
    }

    fun moveUp(index: Int) = if (index <= 0) this else edit { add(index - 1, removeAt(index)) }

    fun remove(index: Int) = edit { removeAt(index) }

    fun add(key: SortKey) = edit { add(key) }

    private fun edit(change: MutableList<SortKey>.() -> Unit) = SortDraft(keys.toMutableList().apply(change))
}
