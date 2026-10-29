package io.github.loeswerkman.language_trainer.language

import androidx.compose.runtime.Composable
import io.github.loeswerkman.language_trainer.components.Link

@Composable
fun LanguageHomeScreen(onTrainingSelected: () -> Unit, onVocabularySelected: () -> Unit) {
    Link("Training", onClick = onTrainingSelected)
    Link("Vocabulary", onClick = onVocabularySelected)
}
