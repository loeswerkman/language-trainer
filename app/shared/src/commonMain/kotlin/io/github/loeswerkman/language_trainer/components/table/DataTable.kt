package io.github.loeswerkman.language_trainer.components.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.query.Field
import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey

/**
 * A column of a [DataTable]. [width] is the minimum width; spare room is shared out in proportion to it.
 * Columns with a [field] can be sorted and filtered on.
 */
class TableColumn<T>(
    val label: String,
    val width: Dp,
    val field: Field<T>? = null,
    val alignment: Alignment.Horizontal = Alignment.Start,
    val cell: @Composable (T) -> Unit,
) {
    /** Identifies the column, e.g. to remember whether it is hidden. */
    val id: String = field?.id ?: label
}

/**
 * A scrollable table that knows nothing about the items it shows: the columns decide what is shown.
 * Clicking a sortable header calls [onSort]; its funnel icon calls [onFilter].
 * [sort] and [filteredFieldIds] are only used to mark the headers.
 */
@Composable
fun <T> ColumnScope.DataTable(
    items: List<T>,
    columns: List<TableColumn<T>>,
    key: (T) -> Any,
    sort: List<SortKey> = emptyList(),
    filteredFieldIds: Set<String> = emptySet(),
    onSort: ((Field<T>) -> Unit)? = null,
    onFilter: ((Field<T>) -> Unit)? = null,
) {
    BoxWithConstraints(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
        val minWidth = columns.fold(0.dp) { total, column -> total + column.width }
        val scale = if (maxWidth > minWidth) maxWidth / minWidth else 1f
        val widths = columns.map { it.width * scale }

        Column(modifier = Modifier.horizontalScroll(rememberScrollState()).width(minWidth * scale)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                columns.forEachIndexed { index, column ->
                    HeaderCell(column, widths[index], sort, filteredFieldIds, onSort, onFilter)
                }
            }
            HorizontalDivider()
            // A keyed lazy list keeps its first visible item in view when items move, which would scroll a re-sorted
            // table away from its new first rows. A new filter or sort therefore starts at the top.
            val listState = remember(items) { LazyListState() }
            LazyColumn(state = listState) {
                items(items, key = key) { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        columns.forEachIndexed { index, column ->
                            Box(
                                modifier = Modifier.width(widths[index]).padding(horizontal = 8.dp, vertical = 6.dp),
                                contentAlignment = if (column.alignment == Alignment.End) Alignment.CenterEnd else Alignment.CenterStart,
                            ) { column.cell(item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> HeaderCell(
    column: TableColumn<T>,
    width: Dp,
    sort: List<SortKey>,
    filteredFieldIds: Set<String>,
    onSort: ((Field<T>) -> Unit)?,
    onFilter: ((Field<T>) -> Unit)?,
) {
    val field = column.field
    val sortIndex = sort.indexOfFirst { it is SortKey.ByField && it.field == field?.id }
    val sortKey = sort.getOrNull(sortIndex) as SortKey.ByField?
    val isFiltered = field != null && field.id in filteredFieldIds
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Row(
        modifier = Modifier.width(width).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, column.alignment),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val sortable = if (field != null && onSort != null) {
            Modifier.pointerHoverIcon(PointerIcon.Hand).clickable { onSort(field) }
        } else {
            Modifier
        }
        val arrow = when (sortKey?.direction) {
            null -> ""
            SortDirection.ASCENDING -> " ↑"
            SortDirection.DESCENDING -> " ↓"
        }
        val position = if (sortKey != null && sort.size > 1) "${sortIndex + 1}" else ""
        Text(
            column.label + arrow + position,
            style = MaterialTheme.typography.labelMedium,
            color = if (sortKey != null) accent else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = sortable.weight(1f, fill = false),
        )
        if (field != null && onFilter != null) {
            Icon(
                FilterIcon,
                contentDescription = "Filter on ${column.label}",
                tint = if (isFiltered) accent else muted,
                modifier = Modifier.size(16.dp).pointerHoverIcon(PointerIcon.Hand).clickable { onFilter(field) },
            )
        }
    }
}
