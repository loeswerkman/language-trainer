package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.components.ErrorMessage
import io.github.loeswerkman.language_trainer.components.table.DataTable
import io.github.loeswerkman.language_trainer.components.table.TableColumn
import io.github.loeswerkman.language_trainer.theme.AppColors
import io.github.loeswerkman.language_trainer.vocabulary.WordStatistics

/** The words answered in a finished session, with how often each was answered correctly, almost or incorrectly. */
@Composable
fun ColumnScope.SessionResults(results: List<WordStatistics>, onStartNewSession: () -> Unit) {
    Text("Session finished", style = MaterialTheme.typography.headlineSmall)
    if (results.isEmpty()) {
        ErrorMessage("No answers were saved in this session.")
    } else {
        Text(
            "${results.sumOf { it.correct }} correct · ${results.sumOf { it.almostCorrect }} almost correct · " +
                "${results.sumOf { it.incorrect }} incorrect",
            style = MaterialTheme.typography.bodyLarge,
        )
        DataTable(results, resultColumns, key = { it.wordId })
    }
    Button(onClick = onStartNewSession) { Text("Start new session") }
}

private val resultColumns: List<TableColumn<WordStatistics>> = listOf(
    textColumn("Word") { it.word },
    textColumn("Translation") { it.translation },
    countColumn("Correct", { AppColors.correct }) { it.correct },
    countColumn("Almost", { AppColors.almostCorrect }) { it.almostCorrect },
    countColumn("Incorrect", { MaterialTheme.colorScheme.error }) { it.incorrect },
)

private fun textColumn(label: String, text: (WordStatistics) -> String) =
    TableColumn<WordStatistics>(label, 150.dp) { Text(text(it), style = MaterialTheme.typography.bodyMedium) }

private fun countColumn(label: String, colour: @Composable () -> Color, count: (WordStatistics) -> Int) =
    TableColumn<WordStatistics>(label, 80.dp, alignment = Alignment.End) {
        Text(count(it).toString(), style = MaterialTheme.typography.bodyMedium, color = colour())
    }
