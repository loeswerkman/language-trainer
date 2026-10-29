package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.loeswerkman.language_trainer.components.ErrorMessage
import io.github.loeswerkman.language_trainer.vocabulary.WordStatistics
import io.github.loeswerkman.language_trainer.vocabulary.WordStatisticsTable

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
        WordStatisticsTable(results)
    }
    Button(onClick = onStartNewSession) { Text("Start new session") }
}
