package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.loeswerkman.language_trainer.components.ErrorMessage
import io.github.loeswerkman.language_trainer.components.Loadable
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.navigation.languageDisplayName

@Composable
fun TrainingScreen(client: VocabularyClient, language: String) {
    Text("Currently training: ${languageDisplayName(language)}", style = MaterialTheme.typography.headlineMedium)
    Loadable(key = language, load = { client.fetchWords(language) }) { words ->
        if (words.isEmpty()) ErrorMessage("No words found. Add some to vocabulary/$language.yaml.")
        else FlashcardScreen(words)
    }
}
