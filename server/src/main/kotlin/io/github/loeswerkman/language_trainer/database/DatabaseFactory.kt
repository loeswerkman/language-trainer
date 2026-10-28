package io.github.loeswerkman.language_trainer.database

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories

/**
 * Connects to the SQLite database file, creating it if needed.
 * Creates missing tables and adds English alternatives to databases created with the original schema.
 */
fun connectDatabase(file: Path): Database {
    file.absolute().parent?.createDirectories()
    val database = Database.connect("jdbc:sqlite:${file.absolute()}", driver = "org.sqlite.JDBC")
    transaction(database) {
        SchemaUtils.create(WordsTable)
        val columns = exec("PRAGMA table_info(words)") { result ->
            buildSet<String> {
                while (result.next()) add(result.getString("name"))
            }
        } ?: error("Could not read the words table schema")
        if ("word_alternatives" !in columns) {
            exec("ALTER TABLE words ADD COLUMN word_alternatives TEXT NOT NULL DEFAULT '[]'")
        }
    }
    return database
}
