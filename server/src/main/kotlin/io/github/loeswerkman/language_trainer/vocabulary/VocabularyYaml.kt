package io.github.loeswerkman.language_trainer.vocabulary

import com.charleskorn.kaml.Yaml
import io.github.loeswerkman.language_trainer.vocabulary.VocabularyIssue.Severity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** One entry in a `vocabulary/<language>.yaml` file. */
@Serializable
data class VocabularyEntry(
    val word: String,
    val translation: String,
    val category: WordCategory,
    val alternatives: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val complexity: Int,
    val usefulness: Int,
    val notes: String? = null,
) {
    /** Entries are unique per word and category, so e.g. "run" can exist as both a noun and a verb. */
    val key: VocabularyKey get() = VocabularyKey(word.trim().lowercase(), category)
}

data class VocabularyKey(val word: String, val category: WordCategory)

data class VocabularyIssue(val severity: Severity, val message: String) {
    enum class Severity { WARNING, ERROR }
}

data class ValidatedVocabulary(
    val entries: List<VocabularyEntry>,
    val issues: List<VocabularyIssue>,
)

fun parseVocabularyYaml(yaml: String): List<VocabularyEntry> =
    Yaml.default.decodeFromString(ListSerializer(VocabularyEntry.serializer()), yaml)

/**
 * Validates parsed entries. Invalid entries and duplicates (same word and category) are reported as errors
 * and left out; only the first occurrence of a duplicate is kept. Issues that don't block loading,
 * such as the same translation used for several words, are reported as warnings.
 */
fun validateVocabulary(entries: List<VocabularyEntry>): ValidatedVocabulary {
    val issues = mutableListOf<VocabularyIssue>()
    val seenKeys = mutableSetOf<VocabularyKey>()
    val validEntries = mutableListOf<VocabularyEntry>()

    for (entry in entries) {
        val label = "'${entry.word}' (${entry.category.label})"
        val errors = buildList {
            if (entry.word.isBlank()) add("word is empty")
            if (entry.translation.isBlank()) add("translation is empty")
            if (entry.complexity !in RATING_RANGE) add("complexity must be between 1 and 5, was ${entry.complexity}")
            if (entry.usefulness !in RATING_RANGE) add("usefulness must be between 1 and 5, was ${entry.usefulness}")
        }
        when {
            errors.isNotEmpty() ->
                errors.forEach { issues += VocabularyIssue(Severity.ERROR, "$label skipped: $it") }
            !seenKeys.add(entry.key) ->
                issues += VocabularyIssue(Severity.ERROR, "$label skipped: duplicate of an earlier entry")
            else -> validEntries += entry
        }
    }

    validEntries
        .groupBy { it.translation.trim().lowercase() }
        .filterValues { it.size > 1 }
        .forEach { (translation, sharing) ->
            issues += VocabularyIssue(
                Severity.WARNING,
                "translation '$translation' is used by several words: ${sharing.joinToString { "'${it.word}'" }}",
            )
        }

    return ValidatedVocabulary(validEntries, issues)
}

private val RATING_RANGE = 1..5
