package io.github.loeswerkman.language_trainer.vocabulary

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.loeswerkman.language_trainer.components.ErrorMessage
import io.github.loeswerkman.language_trainer.components.Loadable
import io.github.loeswerkman.language_trainer.components.query.QueryDialogs
import io.github.loeswerkman.language_trainer.components.query.QueryState
import io.github.loeswerkman.language_trainer.components.query.QueryToolbar
import io.github.loeswerkman.language_trainer.components.table.ColumnsButton
import io.github.loeswerkman.language_trainer.components.table.DataTable
import io.github.loeswerkman.language_trainer.currentTimeMillis
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.navigation.languageDisplayName
import io.github.loeswerkman.language_trainer.storage.HiddenColumnsStore

/** All words of [language] with their results over every training session, filtered and sorted as the user chose. */
@Composable
fun ColumnScope.VocabularyScreen(client: VocabularyClient, language: String) {
    Text("${languageDisplayName(language)} vocabulary", style = MaterialTheme.typography.headlineMedium)
    Loadable(key = language, load = { client.fetchVocabulary(language) }) { items ->
        if (items.isEmpty()) ErrorMessage("No words found. Add some to vocabulary/$language.yaml.")
        else VocabularyTable(items, language)
    }
}

@Composable
private fun ColumnScope.VocabularyTable(items: List<VocabularyItem>, language: String) {
    val state = remember(language) { QueryState(VocabularyQueries.tableStore(language)) }
    val columnsStore = remember(language) { HiddenColumnsStore.vocabulary(language) }
    var hiddenColumns by remember(columnsStore) { mutableStateOf(columnsStore.load()) }
    val now = remember(items) { currentTimeMillis() }
    val columns = remember(now) { vocabularyColumns(now) }
    val shown = remember(items, state.query) { state.fields.apply(items, state.query, now) }

    val learnedCount = remember(items) { items.count { it.learned } }
    QueryToolbar(state, summary = "Showing ${shown.size} of ${items.size} words · $learnedCount learned") {
        ColumnsButton(columns, hiddenColumns, onChange = {
            hiddenColumns = it
            columnsStore.save(it)
        })
    }
    if (shown.isEmpty()) {
        ErrorMessage("No words match the filter.")
    } else {
        DataTable(
            items = shown,
            columns = columns.filter { it.id !in hiddenColumns },
            key = { it.word.id },
            sort = state.query.sort,
            filteredFieldIds = state.filteredFieldIds,
            onSort = state::sortBy,
            onFilter = state::openFilter,
        )
    }
    QueryDialogs(state, items)
}
