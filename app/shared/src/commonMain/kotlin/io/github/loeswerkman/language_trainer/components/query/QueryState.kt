package io.github.loeswerkman.language_trainer.components.query

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.loeswerkman.language_trainer.query.Field
import io.github.loeswerkman.language_trainer.query.FieldSet
import io.github.loeswerkman.language_trainer.query.ListQuery
import io.github.loeswerkman.language_trainer.storage.ListQueryStore

/** Which query dialog is open. A filter dialog can start with a condition on one field, e.g. from a column header. */
sealed interface QueryDialog {
    data class Filter(val fieldId: String? = null) : QueryDialog
    data object Sort : QueryDialog
}

/**
 * The filter and sort of one view, and which of its dialogs is open. Every change is saved in [store] and reported to
 * [onChange]. Plain state without UI, so views share the same behaviour and it can be tested.
 */
@Stable
class QueryState<T>(private val store: ListQueryStore<T>, private val onChange: (ListQuery) -> Unit = {}) {
    val fields: FieldSet<T> get() = store.fields
    val defaultQuery: ListQuery get() = store.defaultQuery

    var query: ListQuery by mutableStateOf(store.load())
        private set
    var dialog: QueryDialog? by mutableStateOf(null)
        private set

    val filteredFieldIds: Set<String> get() = fields.filteredFieldIds(query.filter)

    fun openFilter(field: Field<T>? = null) {
        dialog = QueryDialog.Filter(field?.id)
    }

    fun openSort() {
        dialog = QueryDialog.Sort
    }

    fun closeDialog() {
        dialog = null
    }

    /** Applies the query confirmed in a dialog. */
    fun confirm(next: ListQuery) {
        dialog = null
        update(next)
    }

    /** A click on [field]'s column header. Removing the last sort key leaves the list unsorted, also after reloading. */
    fun sortBy(field: Field<T>) = update(query.copy(sort = fields.sortBy(field, query.sort)))

    private fun update(next: ListQuery) {
        if (next == query) return
        query = next
        store.save(next)
        onChange(next)
    }
}
