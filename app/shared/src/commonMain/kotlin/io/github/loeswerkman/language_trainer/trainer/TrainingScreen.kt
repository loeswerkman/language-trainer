package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.loeswerkman.language_trainer.components.ErrorMessage
import io.github.loeswerkman.language_trainer.components.Link
import io.github.loeswerkman.language_trainer.components.Loadable
import io.github.loeswerkman.language_trainer.components.query.QueryDialogs
import io.github.loeswerkman.language_trainer.components.query.QueryToolbar
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.navigation.languageDisplayName
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyItem
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyQueries

private data class TrainingData(val items: List<VocabularyItem>, val session: TrainingSession)

@Composable
fun ColumnScope.TrainingScreen(client: VocabularyClient, language: String) {
    var sessionNumber by remember(language) { mutableStateOf(0) }
    Text("Currently training: ${languageDisplayName(language)}", style = MaterialTheme.typography.headlineMedium)
    Loadable(
        key = language to sessionNumber,
        load = { TrainingData(client.fetchVocabulary(language), client.startSession(language)) },
    ) { (items, session) ->
        if (items.isEmpty()) ErrorMessage("No words found. Add some to vocabulary/$language.yaml.")
        else TrainingSessionScreen(client, items, session, onStartNewSession = { sessionNumber++ })
    }
}

/** Shows a [TrainingController]: the training query, the current card, and the results once finished. */
@Composable
private fun ColumnScope.TrainingSessionScreen(
    client: VocabularyClient,
    items: List<VocabularyItem>,
    session: TrainingSession,
    onStartNewSession: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val controller = remember(session) {
        TrainingController(client, session, items, VocabularyQueries.trainingStore(session.language), scope)
    }
    var isFinished by remember(session) { mutableStateOf(false) }

    if (isFinished) {
        Loadable(
            key = session.id,
            // Wait for answers that are still being saved, so they are included in the results.
            load = {
                controller.awaitPendingSaves()
                client.fetchSessionResults(session.id)
            },
        ) { results -> SessionResults(results, onStartNewSession) }
        return
    }

    QueryToolbar(controller.query, summary = "Training ${controller.matchCount} of ${controller.items.size} words")
    val round = controller.visibleRound
    when {
        round != null -> FlashcardScreen(round, onSubmit = controller::submit, onNext = controller::next)
        controller.isReloading -> CircularProgressIndicator()
        else -> ErrorMessage("No words match the filter. Change it to keep training.")
    }
    controller.error?.let { ErrorMessage(it) }
    if (controller.answeredCount > 0) {
        Link("Finish session", onClick = { isFinished = true }, style = MaterialTheme.typography.bodyLarge)
    }
    QueryDialogs(controller.query, controller.items)
}
