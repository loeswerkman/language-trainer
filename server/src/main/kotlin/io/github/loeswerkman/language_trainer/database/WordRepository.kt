package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.vocabulary.VocabularyEntry
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyKey
import io.github.loeswerkman.language_trainer.vocabulary.Word
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class SyncResult(val inserted: Int, val updated: Int, val removed: Int, val unchanged: Int)

class WordRepository(private val database: Database) {

    fun findAll(language: String? = null): List<Word> = transaction(database) {
        val query = WordsTable.selectAll()
        if (language != null) query.where { WordsTable.language eq language }
        query.orderBy(WordsTable.id).map { it.toWord() }
    }

    fun findById(id: Long): Word? = transaction(database) {
        WordsTable.selectAll().where { WordsTable.id eq id }.singleOrNull()?.toWord()
    }

    fun findLanguages(): List<String> = transaction(database) {
        WordsTable.select(WordsTable.language)
            .withDistinct()
            .orderBy(WordsTable.language)
            .map { it[WordsTable.language] }
    }

    /**
     * Makes the stored words for [language] match [entries], which come from the YAML source of truth:
     * new entries are inserted, changed ones are updated and words no longer in the YAML are removed.
     */
    fun sync(language: String, entries: List<VocabularyEntry>): SyncResult = transaction(database) {
        val existing = WordsTable.selectAll()
            .where { WordsTable.language eq language }
            .associateBy { VocabularyKey(it[WordsTable.word].lowercase(), it[WordsTable.category]) }

        var inserted = 0
        var updated = 0
        var unchanged = 0
        for (entry in entries) {
            val row = existing[entry.key]
            when {
                row == null -> {
                    WordsTable.insert { it.fill(language, entry) }
                    inserted++
                }
                row.toWord().matches(entry) -> unchanged++
                else -> {
                    WordsTable.update({ WordsTable.id eq row[WordsTable.id] }) { it.fill(language, entry) }
                    updated++
                }
            }
        }

        val keysInYaml = entries.map { it.key }.toSet()
        val idsToRemove = existing.filterKeys { it !in keysInYaml }.values.map { it[WordsTable.id] }
        if (idsToRemove.isNotEmpty()) {
            WordsTable.deleteWhere { WordsTable.id inList idsToRemove }
        }

        SyncResult(inserted, updated, idsToRemove.size, unchanged)
    }

    private fun UpdateBuilder<*>.fill(language: String, entry: VocabularyEntry) {
        this[WordsTable.language] = language
        this[WordsTable.word] = entry.word.trim()
        this[WordsTable.translation] = entry.translation.trim()
        this[WordsTable.category] = entry.category
        this[WordsTable.wordAlternatives] = Json.encodeToString(entry.wordAlternatives)
        this[WordsTable.translationAlternatives] = Json.encodeToString(entry.translationAlternatives)
        this[WordsTable.tags] = Json.encodeToString(entry.tags)
        this[WordsTable.complexity] = entry.complexity
        this[WordsTable.usefulness] = entry.usefulness
        this[WordsTable.notes] = entry.notes
    }

    private fun Word.matches(entry: VocabularyEntry): Boolean =
        word == entry.word.trim() &&
            translation == entry.translation.trim() &&
            wordAlternatives == entry.wordAlternatives &&
            translationAlternatives == entry.translationAlternatives &&
            tags == entry.tags &&
            complexity == entry.complexity &&
            usefulness == entry.usefulness &&
            notes == entry.notes

    private fun ResultRow.toWord() = Word(
        id = this[WordsTable.id],
        language = this[WordsTable.language],
        word = this[WordsTable.word],
        translation = this[WordsTable.translation],
        wordAlternatives = Json.decodeFromString(this[WordsTable.wordAlternatives]),
        translationAlternatives = Json.decodeFromString(this[WordsTable.translationAlternatives]),
        category = this[WordsTable.category],
        tags = Json.decodeFromString(this[WordsTable.tags]),
        complexity = this[WordsTable.complexity],
        usefulness = this[WordsTable.usefulness],
        notes = this[WordsTable.notes],
    )
}
