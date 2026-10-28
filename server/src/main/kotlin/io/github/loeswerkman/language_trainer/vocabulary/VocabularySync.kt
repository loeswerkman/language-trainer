package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.database.SyncResult
import io.github.loeswerkman.language_trainer.database.WordRepository
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

data class LanguageSyncReport(
    val language: String,
    val result: SyncResult,
    val issues: List<VocabularyIssue>,
)

/**
 * Loads every `<vocabularyDir>/<language>.yaml` file into the database.
 * The file name is used as the language, e.g. `vocabulary/russian.yaml` becomes "russian".
 */
class VocabularySync(
    private val vocabularyDir: Path,
    private val repository: WordRepository,
) {
    private val logger = LoggerFactory.getLogger(VocabularySync::class.java)

    fun syncAll(): List<LanguageSyncReport> {
        if (!vocabularyDir.isDirectory()) {
            logger.warn("Vocabulary directory {} not found, no vocabulary loaded", vocabularyDir.toAbsolutePath())
            return emptyList()
        }
        return vocabularyDir.listDirectoryEntries("*.yaml")
            .filter { it.isRegularFile() }
            .sortedBy { it.nameWithoutExtension }
            .map { syncLanguage(it.nameWithoutExtension, it) }
    }

    private fun syncLanguage(language: String, file: Path): LanguageSyncReport {
        val validated = validateVocabulary(parseVocabularyYaml(file.readText()))
        validated.issues.forEach {
            when (it.severity) {
                VocabularyIssue.Severity.ERROR -> logger.error("[{}] {}", language, it.message)
                VocabularyIssue.Severity.WARNING -> logger.warn("[{}] {}", language, it.message)
            }
        }
        val result = repository.sync(language, validated.entries)
        logger.info(
            "[{}] Synced {} words: {} inserted, {} updated, {} removed, {} unchanged",
            language, validated.entries.size, result.inserted, result.updated, result.removed, result.unchanged,
        )
        return LanguageSyncReport(language, result, validated.issues)
    }
}
