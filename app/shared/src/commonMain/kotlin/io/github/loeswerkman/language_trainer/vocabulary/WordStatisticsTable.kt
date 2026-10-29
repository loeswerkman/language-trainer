package io.github.loeswerkman.language_trainer.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.theme.AppColors

private val countWidth = 72.dp

/** A scrollable table of words with how often each was answered correctly, almost correctly and incorrectly. */
@Composable
fun ColumnScope.WordStatisticsTable(statistics: List<WordStatistics>) {
    Column(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
        StatisticsRow(
            word = "Word",
            translation = "Translation",
            counts = listOf("Correct", "Almost", "Incorrect"),
            style = MaterialTheme.typography.labelMedium,
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
            items(statistics, key = { it.wordId }) {
                StatisticsRow(
                    word = it.word,
                    translation = it.translation,
                    counts = listOf(it.correct, it.almostCorrect, it.incorrect).map(Int::toString),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StatisticsRow(word: String, translation: String, counts: List<String>, style: TextStyle) {
    val countColours = listOf(AppColors.correct, AppColors.almostCorrect, MaterialTheme.colorScheme.error)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(word, style = style, modifier = Modifier.weight(1f))
        Text(translation, style = style, modifier = Modifier.weight(1f))
        counts.zip(countColours).forEach { (count, colour) -> Count(count, colour, style) }
    }
}

@Composable
private fun Count(text: String, colour: Color, style: TextStyle) {
    Text(text, style = style, color = colour, textAlign = TextAlign.End, modifier = Modifier.width(countWidth))
}
