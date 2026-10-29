package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.training.AnswerSubmission
import io.github.loeswerkman.language_trainer.training.TrainingSession
import io.github.loeswerkman.language_trainer.vocabulary.AnswerResult
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyItem
import io.github.loeswerkman.language_trainer.vocabulary.WordStatistics
import io.github.loeswerkman.language_trainer.vocabulary.evaluateAnswer
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/** Saves training sessions and their answers, and calculates per-word statistics from the saved answers. */
class TrainingRepository(
    private val database: Database,
    private val words: WordRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun startSession(language: String): TrainingSession = transaction(database) {
        val id = TrainingSessionsTable.insert {
            it[TrainingSessionsTable.language] = language
            it[startedAt] = clock()
        }[TrainingSessionsTable.id]
        TrainingSession(id, language)
    }

    fun findSession(id: Long): TrainingSession? = transaction(database) {
        TrainingSessionsTable.selectAll()
            .where { TrainingSessionsTable.id eq id }
            .singleOrNull()
            ?.let { TrainingSession(it[TrainingSessionsTable.id], it[TrainingSessionsTable.language]) }
    }

    /** Evaluates and saves the answer. Returns null if the word doesn't exist in the session's language. */
    fun recordAnswer(session: TrainingSession, submission: AnswerSubmission): AnswerResult? {
        val word = words.findById(submission.wordId)?.takeIf { it.language == session.language } ?: return null
        val result = evaluateAnswer(submission.answer, word, submission.direction)
        transaction(database) {
            AnswersTable.insert {
                it[sessionId] = session.id
                it[wordId] = word.id
                it[direction] = submission.direction
                it[answer] = submission.answer
                it[AnswersTable.result] = result
                it[answeredAt] = clock()
            }
        }
        return result
    }

    /** Statistics for the words answered in one session. */
    fun sessionStatistics(sessionId: Long): List<WordStatistics> = transaction(database) {
        val correct = countOf(AnswerResult.CORRECT)
        val almostCorrect = countOf(AnswerResult.ALMOST_CORRECT)
        val incorrect = countOf(AnswerResult.INCORRECT)

        WordsTable.join(AnswersTable, JoinType.INNER, WordsTable.id, AnswersTable.wordId)
            .select(WordsTable.id, WordsTable.word, WordsTable.translation, correct, almostCorrect, incorrect)
            .where { AnswersTable.sessionId eq sessionId }
            .groupBy(WordsTable.id, WordsTable.word, WordsTable.translation)
            .map {
                WordStatistics(
                    wordId = it[WordsTable.id],
                    word = it[WordsTable.word],
                    translation = it[WordsTable.translation],
                    correct = it[correct] ?: 0,
                    almostCorrect = it[almostCorrect] ?: 0,
                    incorrect = it[incorrect] ?: 0,
                )
            }
            .sortedWith(WordStatistics.byScore)
    }

    /** Every word of [language] with its results over all sessions, including words that were never answered. */
    fun vocabulary(language: String): List<VocabularyItem> {
        val correct = countOf(AnswerResult.CORRECT)
        val almostCorrect = countOf(AnswerResult.ALMOST_CORRECT)
        val incorrect = countOf(AnswerResult.INCORRECT)
        val lastAnsweredAt = AnswersTable.answeredAt.max()

        val answered = transaction(database) {
            AnswersTable.join(WordsTable, JoinType.INNER, AnswersTable.wordId, WordsTable.id)
                .select(AnswersTable.wordId, correct, almostCorrect, incorrect, lastAnsweredAt)
                .where { WordsTable.language eq language }
                .groupBy(AnswersTable.wordId)
                .associateBy { it[AnswersTable.wordId] }
        }
        return words.findAll(language).map { word ->
            val row = answered[word.id] ?: return@map VocabularyItem(word)
            VocabularyItem(
                word = word,
                correct = row[correct] ?: 0,
                almostCorrect = row[almostCorrect] ?: 0,
                incorrect = row[incorrect] ?: 0,
                lastAnsweredAt = row[lastAnsweredAt],
            )
        }
    }

    /** SUM(CASE WHEN result = [result] THEN 1 ELSE 0 END): the number of answers per word with that result. */
    private fun countOf(result: AnswerResult) =
        case().When(AnswersTable.result eq result, intLiteral(1)).Else(intLiteral(0)).sum()
}
