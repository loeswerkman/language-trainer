package io.github.loeswerkman.language_trainer.components.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Hides or shows the column [id]. The last visible column of [allIds] can't be hidden, so the table never disappears. */
fun toggleHidden(hidden: Set<String>, id: String, allIds: List<String>): Set<String> = when {
    id in hidden -> hidden - id
    allIds.none { it != id && it !in hidden } -> hidden
    else -> hidden + id
}

/** A "Columns" button with a checklist to show or hide each of [columns]. */
@Composable
fun <T> ColumnsButton(columns: List<TableColumn<T>>, hidden: Set<String>, onChange: (Set<String>) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val ids = columns.map { it.id }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(EyeIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(" Columns")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            columns.forEach { column ->
                val visible = column.id !in hidden
                val toggle = { onChange(toggleHidden(hidden, column.id, ids)) }
                DropdownMenuItem(
                    text = { Text(column.label) },
                    leadingIcon = { Checkbox(checked = visible, onCheckedChange = { toggle() }) },
                    onClick = toggle,
                )
            }
        }
    }
}
