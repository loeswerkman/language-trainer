package io.github.loeswerkman.language_trainer.home

import androidx.compose.runtime.Composable
import io.github.loeswerkman.language_trainer.components.Link
import io.github.loeswerkman.language_trainer.components.Loadable
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.navigation.languageDisplayName

@Composable
fun HomeScreen(client: VocabularyClient, onLanguageSelected: (String) -> Unit) {
    Loadable(key = Unit, load = { client.fetchLanguages() }) { languages ->
        languages.forEach { language ->
            Link(languageDisplayName(language), onClick = { onLanguageSelected(language) })
        }
    }
}
