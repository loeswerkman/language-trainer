package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.TranslationDirection
import org.jetbrains.exposed.v1.core.Table

object TrainingSessionsTable : Table("training_sessions") {
    val id = long("id").autoIncrement()
    val language = varchar("language", 50)
    val startedAt = long("started_at")

    override val primaryKey = PrimaryKey(id)
}

/** Every answer ever given. The per-word counts are calculated from these rows, never stored. */
object AnswersTable : Table("answers") {
    val id = long("id").autoIncrement()
    val sessionId = long("session_id").references(TrainingSessionsTable.id).index()

    // No foreign key: answers are kept when a word is removed from the YAML, so the history isn't lost.
    // Word IDs are never reused (AUTOINCREMENT), so such answers simply stop showing up.
    val wordId = long("word_id").index()
    val direction = enumerationByName<TranslationDirection>("direction", 20)
    val answer = text("answer")
    val result = enumerationByName<AnswerResult>("result", 20)
    val answeredAt = long("answered_at")

    override val primaryKey = PrimaryKey(id)
}
