package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.foundation.layout.ColumnScope
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
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.navigation.languageDisplayName
import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.vocabulary.Word
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private data class TrainingData(val words: List<Word>, val session: TrainingSession)

@Composable
fun ColumnScope.TrainingScreen(client: VocabularyClient, language: String) {
    var sessionNumber by remember(language) { mutableStateOf(0) }
    Text("Currently training: ${languageDisplayName(language)}", style = MaterialTheme.typography.headlineMedium)
    Loadable(
        key = language to sessionNumber,
        load = { TrainingData(client.fetchWords(language), client.startSession(language)) },
    ) { (words, session) ->
        if (words.isEmpty()) ErrorMessage("No words found. Add some to vocabulary/$language.yaml.")
        else TrainingSessionScreen(client, words, session, onStartNewSession = { sessionNumber++ })
    }
}

/** Practises [words] and saves every answer in [session]. Finishing the session shows its results. */
@Composable
private fun ColumnScope.TrainingSessionScreen(
    client: VocabularyClient,
    words: List<Word>,
    session: TrainingSession,
    onStartNewSession: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pendingSaves = remember(session) { mutableListOf<Job>() }
    var answeredCount by remember(session) { mutableStateOf(0) }
    var saveError by remember(session) { mutableStateOf<String?>(null) }
    var isFinished by remember(session) { mutableStateOf(false) }

    if (isFinished) {
        Loadable(
            key = session.id,
            // Wait for answers that are still being saved, so they are included in the results.
            load = {
                pendingSaves.joinAll()
                client.fetchSessionResults(session.id)
            },
        ) { results -> SessionResults(results, onStartNewSession) }
        return
    }

    FlashcardScreen(words, onAnswered = { word, direction, answer ->
        answeredCount++
        pendingSaves += scope.launch {
            try {
                client.submitAnswer(session.id, AnswerSubmission(word.id, direction, answer))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveError = "Could not save your answer: ${e.message ?: e}"
            }
        }
    })
    saveError?.let { ErrorMessage(it) }
    if (answeredCount > 0) {
        Link("Finish session", onClick = { isFinished = true }, style = MaterialTheme.typography.bodyLarge)
    }
}
