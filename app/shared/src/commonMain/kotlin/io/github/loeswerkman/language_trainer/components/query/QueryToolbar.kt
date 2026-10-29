package io.github.loeswerkman.language_trainer.components.query

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.components.table.FilterIcon

/**
 * A summary of the current filter and sort with buttons to change them, e.g. "12 of 80 words · Filter: …".
 * [extraActions] adds view-specific buttons after Filter and Sort.
 */
@Composable
fun <T> QueryToolbar(
    state: QueryState<T>,
    summary: String,
    modifier: Modifier = Modifier,
    extraActions: @Composable RowScope.() -> Unit = {},
) {
    val query = state.query
    val default = state.defaultQuery
    fun suffix(isDefault: Boolean) = if (isDefault) " (default)" else ""
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(summary, style = MaterialTheme.typography.titleSmall)
            Text(
                (if (query.filter.isEmpty) "No filter" else "Filter: ${state.fields.describe(query.filter)}") +
                    suffix(query.filter == default.filter),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                (if (query.sort.isEmpty()) "Not sorted" else "Sort: ${state.fields.describe(query.sort)}") +
                    suffix(query.sort == default.sort),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = { state.openFilter() }) {
            Icon(FilterIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(" Filter")
        }
        OutlinedButton(onClick = state::openSort) { Text("↑↓ Sort") }
        extraActions()
    }
}
