package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.vocabulary.VocabularyEntry
import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.io.path.deleteRecursively
import kotlin.io.path.ExperimentalPathApi
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalPathApi::class)
class WordRepositoryTest {

    private val tempDir = Files.createTempDirectory("word-repository-test")
    private val repository = WordRepository(connectDatabase(tempDir.resolve("test.db")))

    @AfterTest
    fun cleanUp() {
        tempDir.deleteRecursively()
    }

    @Test
    fun insertsUpdatesAndRemovesWords() {
        val cat = entry("cat", "кошка")
        val dog = entry("dog", "собака")

        assertEquals(SyncResult(inserted = 2, updated = 0, removed = 0, unchanged = 0), repository.sync("russian", listOf(cat, dog)))
        assertEquals(
            SyncResult(inserted = 0, updated = 1, removed = 1, unchanged = 0),
            repository.sync("russian", listOf(cat.copy(translationAlternatives = listOf("кот")))),
        )

        val words = repository.findAll("russian")
        assertEquals(listOf("cat"), words.map { it.word })
        assertEquals(listOf("кот"), words.single().translationAlternatives)
        assertEquals(emptyList(), words.single().wordAlternatives)
    }

    @Test
    fun syncsAlternativesForBothDirections() {
        val big = entry("big", "большой").copy(
            wordAlternatives = listOf("large"),
            translationAlternatives = listOf("крупный"),
        )
        repository.sync("russian", listOf(big))

        assertEquals(listOf("large"), repository.findAll().single().wordAlternatives)
        assertEquals(listOf("крупный"), repository.findAll().single().translationAlternatives)
        assertEquals(SyncResult(0, 0, 0, 1), repository.sync("russian", listOf(big)))

        assertEquals(SyncResult(0, 1, 0, 0), repository.sync("russian", listOf(big.copy(wordAlternatives = emptyList()))))
        assertEquals(emptyList(), repository.findAll().single().wordAlternatives)
        assertEquals(listOf("крупный"), repository.findAll().single().translationAlternatives)
    }

    @Test
    fun syncingTheSameEntriesTwiceChangesNothing() {
        val entries = listOf(entry("cat", "кошка"))
        repository.sync("russian", entries)

        assertEquals(SyncResult(inserted = 0, updated = 0, removed = 0, unchanged = 1), repository.sync("russian", entries))
    }

    @Test
    fun keepsLanguagesSeparate() {
        repository.sync("russian", listOf(entry("cat", "кошка")))
        repository.sync("dutch", listOf(entry("cat", "kat")))

        assertEquals(listOf("кошка"), repository.findAll("russian").map { it.translation })
        assertEquals(2, repository.findAll().size)
        assertEquals(listOf("dutch", "russian"), repository.findLanguages())
    }

    @Test
    fun upgradesExistingDatabaseWithoutLosingWordsOrAlternatives() {
        val file = tempDir.resolve("legacy.db")
        DriverManager.getConnection("jdbc:sqlite:$file").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE words (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        language VARCHAR(50) NOT NULL,
                        word VARCHAR(255) NOT NULL,
                        translation VARCHAR(255) NOT NULL,
                        category VARCHAR(20) NOT NULL,
                        alternatives TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        complexity INTEGER NOT NULL,
                        usefulness INTEGER NOT NULL,
                        notes TEXT
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    INSERT INTO words
                        (id, language, word, translation, category, alternatives, tags, complexity, usefulness)
                    VALUES (42, 'russian', 'big', 'большой', 'ADJECTIVE', '["крупный"]', '[]', 1, 3)
                    """.trimIndent(),
                )
            }
        }

        val upgraded = WordRepository(connectDatabase(file))
        val word = upgraded.findAll().single()
        assertEquals(42L, word.id)
        assertEquals(emptyList(), word.wordAlternatives)
        assertEquals(listOf("крупный"), word.translationAlternatives)

        val big = entry("big", "большой").copy(
            category = WordCategory.ADJECTIVE,
            wordAlternatives = listOf("large"),
            translationAlternatives = listOf("крупный"),
        )
        assertEquals(SyncResult(0, 1, 0, 0), upgraded.sync("russian", listOf(big)))

        val reopened = WordRepository(connectDatabase(file)).findAll().single()
        assertEquals(42L, reopened.id)
        assertEquals(listOf("large"), reopened.wordAlternatives)
        assertEquals(listOf("крупный"), reopened.translationAlternatives)
    }

    private fun entry(word: String, translation: String) = VocabularyEntry(
        word = word,
        translation = translation,
        category = WordCategory.NOUN,
        tags = listOf("animal"),
        complexity = 1,
        usefulness = 3,
    )
}
