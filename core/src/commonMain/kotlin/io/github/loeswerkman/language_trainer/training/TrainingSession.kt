package io.github.loeswerkman.language_trainer.training

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import kotlinx.serialization.Serializable

/** One sitting of training. Every answer given during the sitting is saved with the session's [id]. */
@Serializable
data class TrainingSession(val id: Long, val language: String)

@Serializable
data class StartSessionRequest(val language: String)

/** An answer typed by the user. The server evaluates it, so the saved result doesn't depend on the client. */
@Serializable
data class AnswerSubmission(val wordId: Long, val direction: TranslationDirection, val answer: String)

@Serializable
data class RecordedAnswer(val result: AnswerResult)
