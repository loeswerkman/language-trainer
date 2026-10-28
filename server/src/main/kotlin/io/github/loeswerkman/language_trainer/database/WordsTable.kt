package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import org.jetbrains.exposed.v1.core.Table

object WordsTable : Table("words") {
    val id = long("id").autoIncrement()
    val language = varchar("language", 50)
    val word = varchar("word", 255)
    val translation = varchar("translation", 255)
    val category = enumerationByName<WordCategory>("category", 20)

    /** Stored as JSON arrays to keep the schema simple while the model is still evolving. */
    val alternatives = text("alternatives")
    val tags = text("tags")

    val complexity = integer("complexity")
    val usefulness = integer("usefulness")
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("words_language_word_category", language, word, category)
    }
}
