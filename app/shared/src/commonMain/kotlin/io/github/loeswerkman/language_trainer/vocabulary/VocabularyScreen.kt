package io.github.loeswerkman.language_trainer.vocabulary

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.loeswerkman.language_trainer.components.ErrorMessage
import io.github.loeswerkman.language_trainer.components.Loadable
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.navigation.languageDisplayName

/** All words of [language] with their results over every training session, best known words first. */
@Composable
fun ColumnScope.VocabularyScreen(client: VocabularyClient, language: String) {
    Text("${languageDisplayName(language)} vocabulary", style = MaterialTheme.typography.headlineMedium)
    Loadable(key = language, load = { client.fetchVocabulary(language) }) { statistics ->
        if (statistics.isEmpty()) ErrorMessage("No words found. Add some to vocabulary/$language.yaml.")
        else WordStatisticsTable(statistics)
    }
}
