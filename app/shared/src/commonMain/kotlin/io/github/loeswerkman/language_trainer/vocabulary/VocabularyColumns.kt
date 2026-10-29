package io.github.loeswerkman.language_trainer.vocabulary

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.components.table.TableColumn
import io.github.loeswerkman.language_trainer.query.Field
import io.github.loeswerkman.language_trainer.theme.AppColors

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** The columns of the vocabulary table: one per vocabulary field. [now] is used to show how long ago a word was answered. */
fun vocabularyColumns(now: Long): List<TableColumn<VocabularyItem>> = with(VocabularyFields) {
    listOf(
        textColumn(word, 150.dp) { it.word.word },
        textColumn(translation, 150.dp) { it.word.translation },
        textColumn(category, 110.dp) { it.word.category.label },
        textColumn(tags, 150.dp) { it.word.tags.joinToString() },
        numberColumn(complexity, 90.dp) { it.word.complexity },
        numberColumn(usefulness, 90.dp) { it.word.usefulness },
        numberColumn(correct, 80.dp, { AppColors.correct }) { it.correct },
        numberColumn(almostCorrect, 90.dp, { AppColors.almostCorrect }) { it.almostCorrect },
        numberColumn(incorrect, 85.dp, { MaterialTheme.colorScheme.error }) { it.incorrect },
        numberColumn(score, 70.dp) { it.score },
        textColumn(lastAnswered, 115.dp) { describeLastAnswered(it.lastAnsweredAt, now) },
    )
}

/** "never", "today", "yesterday" or "N days ago". */
fun describeLastAnswered(at: Long?, now: Long): String {
    if (at == null) return "never"
    return when (val days = (now - at).coerceAtLeast(0) / DAY_MILLIS) {
        0L -> "today"
        1L -> "yesterday"
        else -> "$days days ago"
    }
}

private fun textColumn(field: Field<VocabularyItem>, width: Dp, text: (VocabularyItem) -> String) =
    TableColumn(field.label, width, field) { item: VocabularyItem ->
        Text(text(item), style = MaterialTheme.typography.bodyMedium)
    }

private fun numberColumn(
    field: Field<VocabularyItem>,
    width: Dp,
    colour: @Composable () -> Color = { Color.Unspecified },
    number: (VocabularyItem) -> Int,
) = TableColumn(field.label, width, field, Alignment.End) { item: VocabularyItem ->
    Text(number(item).toString(), style = MaterialTheme.typography.bodyMedium, color = colour())
}
