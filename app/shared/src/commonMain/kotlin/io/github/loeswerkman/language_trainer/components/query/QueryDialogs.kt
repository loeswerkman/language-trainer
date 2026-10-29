package io.github.loeswerkman.language_trainer.components.query

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.loeswerkman.language_trainer.components.Dropdown
import io.github.loeswerkman.language_trainer.query.Field
import io.github.loeswerkman.language_trainer.query.FieldSet
import io.github.loeswerkman.language_trainer.query.Filter
import io.github.loeswerkman.language_trainer.query.OperatorInput
import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey

/** Shows the open dialog of [state], if any. [items] provide the options to filter on, such as the tags in use. */
@Composable
fun <T> QueryDialogs(state: QueryState<T>, items: List<T>) {
    val fields = state.fields
    val query = state.query
    when (val dialog = state.dialog) {
        null -> {}
        is QueryDialog.Filter -> FilterDialog(
            fields = fields,
            items = items,
            initial = FilterDraft.from(fields, query.filter, dialog.fieldId?.let(fields::field)),
            defaultFilter = state.defaultQuery.filter,
            onDismiss = state::closeDialog,
            onConfirm = { state.confirm(query.copy(filter = it)) },
        )
        QueryDialog.Sort -> SortDialog(
            fields = fields,
            initial = SortDraft(query.sort),
            defaultSort = state.defaultQuery.sort,
            onDismiss = state::closeDialog,
            onConfirm = { state.confirm(query.copy(sort = it)) },
        )
    }
}

@Composable
private fun QueryDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    canConfirm: Boolean,
    extraAction: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.padding(24.dp).widthIn(max = 760.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { content() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    extraAction()
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm, enabled = canConfirm) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun <T> FilterDialog(
    fields: FieldSet<T>,
    items: List<T>,
    initial: FilterDraft<T>,
    defaultFilter: Filter,
    onDismiss: () -> Unit,
    onConfirm: (Filter) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    QueryDialogFrame(
        title = "Filter",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(draft.toFilter()) },
        canConfirm = draft.isValid,
        extraAction = {
            TextButton(onClick = { draft = draft.clear() }, enabled = draft.groups.isNotEmpty()) { Text("Clear all") }
            if (!defaultFilter.isEmpty) {
                TextButton(onClick = { draft = FilterDraft.from(fields, defaultFilter) }) { Text("Reset to default") }
            }
        },
    ) {
        if (draft.groups.isEmpty()) {
            Text("No filter: everything is shown.", style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "Every condition in a group must match. When there are several groups, matching any group is enough.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        draft.groups.forEachIndexed { index, group ->
            if (index > 0) OperatorLabel("OR")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.conditions.forEachIndexed { conditionIndex, condition ->
                        if (conditionIndex > 0) OperatorLabel("AND")
                        ConditionEditor(
                            condition = condition,
                            fields = fields,
                            items = items,
                            onChange = { draft = draft.update(it) },
                            onRemove = { draft = draft.remove(condition.id) },
                        )
                    }
                    TextButton(onClick = { draft = draft.addCondition(fields.fields.first(), group.id) }) {
                        Text("+ AND condition")
                    }
                }
            }
        }
        TextButton(onClick = { draft = draft.addCondition(fields.fields.first()) }) {
            Text(if (draft.groups.isEmpty()) "+ Add condition" else "+ OR group")
        }
    }
}

@Composable
private fun OperatorLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ConditionEditor(
    condition: DraftCondition,
    fields: FieldSet<T>,
    items: List<T>,
    onChange: (DraftCondition) -> Unit,
    onRemove: () -> Unit,
) {
    val field = fields.field(condition.field) ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Dropdown(field, fields.fields, Field<T>::label, onSelect = { onChange(condition.copy(field = it.id)) })
            Dropdown(condition.operator, field.operators, { it.label }, onSelect = { onChange(condition.copy(operator = it)) })
            when (condition.operator.input) {
                OperatorInput.TEXT -> OutlinedTextField(
                    value = condition.text,
                    onValueChange = { onChange(condition.copy(text = it)) },
                    singleLine = true,
                    placeholder = { Text("Text") },
                    modifier = Modifier.width(200.dp),
                )
                OperatorInput.NUMBER -> OutlinedTextField(
                    value = condition.number,
                    onValueChange = { onChange(condition.copy(number = it)) },
                    singleLine = true,
                    isError = !condition.isValid,
                    placeholder = { Text("Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                )
                OperatorInput.NONE, OperatorInput.OPTIONS -> {}
            }
            TextButton(onClick = onRemove) { Text("✕ Remove") }
        }
        if (condition.operator.input == OperatorInput.OPTIONS) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                field.options(items).forEach { option ->
                    val selected = option.value in condition.values
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val values = if (selected) condition.values - option.value else condition.values + option.value
                            onChange(condition.copy(values = values))
                        },
                        label = { Text(option.label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> SortDialog(
    fields: FieldSet<T>,
    initial: SortDraft,
    defaultSort: List<SortKey>,
    onDismiss: () -> Unit,
    onConfirm: (List<SortKey>) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val allKeys: List<SortKey> = fields.fields.map { SortKey.ByField(it.id, it.defaultDirection) } + SortKey.Random

    fun label(key: SortKey) = when (key) {
        SortKey.Random -> "Random"
        is SortKey.ByField -> fields.field(key.field)?.label ?: key.field
    }

    fun sameKind(a: SortKey, b: SortKey) = if (a is SortKey.ByField && b is SortKey.ByField) a.field == b.field else a == b
    fun available(except: SortKey?) = allKeys.filter { key -> draft.keys.none { sameKind(it, key) && it != except } }

    QueryDialogFrame(
        title = "Sort",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(draft.keys) },
        canConfirm = true,
        extraAction = { TextButton(onClick = { draft = SortDraft(defaultSort) }, enabled = draft.keys != defaultSort) { Text("Reset to default") } },
    ) {
        Text(
            (if (draft.keys == defaultSort) "This is the default order. " else "") +
                "Later keys only decide between items that are equal on earlier keys.",
            style = MaterialTheme.typography.bodySmall,
        )
        draft.keys.forEachIndexed { index, key ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (index == 0) "Sort by" else "then by", modifier = Modifier.width(64.dp))
                Dropdown(key, available(except = key), ::label, onSelect = { draft = draft.replace(index, it) })
                if (key is SortKey.ByField) {
                    TextButton(onClick = { draft = draft.toggleDirection(index) }) {
                        Text(if (key.direction == SortDirection.ASCENDING) "↑ Ascending" else "↓ Descending")
                    }
                }
                if (index > 0) TextButton(onClick = { draft = draft.moveUp(index) }) { Text("Move up") }
                TextButton(onClick = { draft = draft.remove(index) }) { Text("✕") }
            }
        }
        if (draft.keys.isEmpty()) Text("No sort: items keep their original order.", style = MaterialTheme.typography.bodyMedium)
        available(except = null).firstOrNull()?.let { next ->
            TextButton(onClick = { draft = draft.add(next) }) { Text(if (draft.keys.isEmpty()) "+ Sort by" else "+ Then by") }
        }
    }
}
