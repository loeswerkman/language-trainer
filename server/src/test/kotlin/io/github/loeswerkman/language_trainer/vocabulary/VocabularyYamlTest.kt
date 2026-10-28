package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.vocabulary.VocabularyIssue.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyYamlTest {

    @Test
    fun currentRussianVocabularyHasValidAlternatives() {
        val yaml = checkNotNull(javaClass.getResource("/russian.yaml")).readText()
        val entries = parseVocabularyYaml(yaml)
        val validated = validateVocabulary(entries)

        assertTrue(entries.isNotEmpty())
        assertEquals(entries, validated.entries)
        assertTrue(validated.issues.isEmpty(), validated.issues.toString())
        for (entry in entries) {
            for (answers in listOf(
                listOf(entry.word) + entry.wordAlternatives,
                listOf(entry.translation) + entry.translationAlternatives,
            )) {
                assertTrue(answers.all { it.isNotBlank() }, "Blank answer for ${entry.word}")
                assertEquals(answers.size, answers.map { it.trim().lowercase() }.toSet().size)
            }
        }
        val big = entries.single { it.word == "big" }
        assertEquals(listOf("large"), big.wordAlternatives)
        assertEquals(listOf("крупный"), big.translationAlternatives)
    }

    @Test
    fun parsesAllFieldsAndDefaults() {
        val entries = parseVocabularyYaml(
            """
            - word: "hello"
              wordAlternatives: ["hi"]
              translation: "привет"
              category: "interjection"
              translationAlternatives:
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
        assertEquals(listOf("hi"), entries[0].wordAlternatives)
        assertEquals(listOf("здравствуйте"), entries[0].translationAlternatives)
        assertEquals("Informal", entries[0].notes)
        assertEquals(emptyList(), entries[1].wordAlternatives)
        assertEquals(emptyList(), entries[1].translationAlternatives)
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
