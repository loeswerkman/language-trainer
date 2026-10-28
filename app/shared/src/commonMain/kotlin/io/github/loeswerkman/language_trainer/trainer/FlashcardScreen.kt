package io.github.loeswerkman.language_trainer.trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import io.github.loeswerkman.language_trainer.vocabulary.acceptedAnswers
import io.github.loeswerkman.language_trainer.vocabulary.prompt
import io.github.loeswerkman.language_trainer.vocabulary.Word

@Composable
fun FlashcardScreen(words: List<Word>) {
    var session by remember(words) { mutableStateOf(FlashcardSession.start(words)) }
    var answer by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    fun submitOrContinue() {
        if (session.isAnswered) {
            session = session.next()
            answer = ""
        } else {
            session = session.submit(answer)
        }
    }

    LaunchedEffect(session.position) { focusRequester.requestFocus() }

    val word = session.currentWord
    Text(
        "Card ${session.position + 1} of ${session.size} · ${session.correctCount} correct",
        style = MaterialTheme.typography.bodyMedium,
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(word.prompt(session.direction), style = MaterialTheme.typography.displaySmall)
        }
    }

    OutlinedTextField(
        value = answer,
        onValueChange = { if (!session.isAnswered) answer = it },
        label = {
            Text(
                when (session.direction) {
                    TranslationDirection.TO_ENGLISH -> "English translation"
                    TranslationDirection.FROM_ENGLISH -> "Translation"
                },
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submitOrContinue() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )

    session.lastResult?.let { Feedback(it, word.acceptedAnswers(session.direction), word.notes) }

    Button(onClick = ::submitOrContinue) {
        Text(
            when {
                !session.isAnswered -> "Check"
                session.isRoundFinished -> "Start new round"
                else -> "Next"
            },
        )
    }

    if (session.isRoundFinished) {
        Text("Round finished: ${session.correctCount} of ${session.size} correct")
    }
}

@Composable
private fun Feedback(result: AnswerResult, acceptedAnswers: List<String>, notes: String?) {
    val answer = acceptedAnswers.first()
    val (title, color) = when (result) {
        AnswerResult.CORRECT -> "Correct!" to Color(0xFF66BB6A)
        AnswerResult.ALMOST_CORRECT -> "Almost! The answer is $answer" to Color(0xFFFFA726)
        AnswerResult.INCORRECT -> "Incorrect. The answer is $answer" to MaterialTheme.colorScheme.error
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = color, style = MaterialTheme.typography.titleMedium)
        if (acceptedAnswers.size > 1) {
            Text("Also accepted: ${acceptedAnswers.drop(1).joinToString()}", style = MaterialTheme.typography.bodyMedium)
        }
        notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
