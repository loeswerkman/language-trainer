package io.github.loeswerkman.language_trainer.database

import io.github.loeswerkman.language_trainer.vocabulary.VocabularyEntry
import io.github.loeswerkman.language_trainer.vocabulary.WordCategory
import java.nio.file.Files
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
            repository.sync("russian", listOf(cat.copy(alternatives = listOf("кот")))),
        )

        val words = repository.findAll("russian")
        assertEquals(listOf("cat"), words.map { it.word })
        assertEquals(listOf("кот"), words.single().alternatives)
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

    private fun entry(word: String, translation: String) = VocabularyEntry(
        word = word,
        translation = translation,
        category = WordCategory.NOUN,
        tags = listOf("animal"),
        complexity = 1,
        usefulness = 3,
    )
}
