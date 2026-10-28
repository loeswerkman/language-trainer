package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.vocabulary.VocabularyIssue.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyYamlTest {

    @Test
    fun parsesAllFieldsAndDefaults() {
        val entries = parseVocabularyYaml(
            """
            - word: "hello"
              translation: "привет"
              category: "interjection"
              alternatives:
                - "здравствуйте"
              tags: ["greeting"]
              complexity: 1
              usefulness: 5
              notes: "Informal"
            - word: "cat"
              translation: "кошка"
              category: "noun"
              complexity: 1
              usefulness: 4
            """.trimIndent(),
        )

        assertEquals(2, entries.size)
        assertEquals(WordCategory.INTERJECTION, entries[0].category)
        assertEquals(listOf("здравствуйте"), entries[0].alternatives)
        assertEquals("Informal", entries[0].notes)
        assertEquals(emptyList(), entries[1].alternatives)
        assertNull(entries[1].notes)
    }

    @Test
    fun keepsFirstOccurrenceOfDuplicateAndReportsError() {
        val result = validateVocabulary(
            listOf(
                entry("cat", "кошка"),
                entry("Cat ", "кот"),
            ),
        )

        assertEquals(listOf("кошка"), result.entries.map { it.translation })
        assertEquals(1, result.issues.size)
        assertEquals(Severity.ERROR, result.issues.single().severity)
        assertTrue("duplicate" in result.issues.single().message)
    }

    @Test
    fun sameWordWithDifferentCategoryIsNotADuplicate() {
        val result = validateVocabulary(
            listOf(
                entry("run", "бег", WordCategory.NOUN),
                entry("run", "бегать", WordCategory.VERB),
            ),
        )

        assertEquals(2, result.entries.size)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun skipsEntriesWithInvalidRatings() {
        val result = validateVocabulary(listOf(entry("cat", "кошка", complexity = 7)))

        assertTrue(result.entries.isEmpty())
        assertEquals(Severity.ERROR, result.issues.single().severity)
    }

    @Test
    fun warnsAboutSharedTranslations() {
        val result = validateVocabulary(
            listOf(
                entry("hi", "привет", WordCategory.INTERJECTION),
                entry("hello", "привет", WordCategory.INTERJECTION),
            ),
        )

        assertEquals(2, result.entries.size)
        assertEquals(Severity.WARNING, result.issues.single().severity)
    }

    private fun entry(
        word: String,
        translation: String,
        category: WordCategory = WordCategory.NOUN,
        complexity: Int = 1,
    ) = VocabularyEntry(
        word = word,
        translation = translation,
        category = category,
        complexity = complexity,
        usefulness = 3,
    )
}
