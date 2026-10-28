package io.github.loeswerkman.language_trainer.database

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories

/**
 * Connects to the SQLite database file, creating it if needed.
 * There is no migration system yet: missing tables are created, but existing tables are not altered.
 */
fun connectDatabase(file: Path): Database {
    file.absolute().parent?.createDirectories()
    val database = Database.connect("jdbc:sqlite:${file.absolute()}", driver = "org.sqlite.JDBC")
    transaction(database) {
        SchemaUtils.create(WordsTable)
    }
    return database
}
