package io.github.loeswerkman.language_trainer.vocabulary

import kotlinx.serialization.Serializable

/** A vocabulary word as stored in the database and exposed by the API. */
@Serializable
data class Word(
    val id: Long,
    val language: String,
    val word: String,
    val translation: String,
    val wordAlternatives: List<String> = emptyList(),
    val translationAlternatives: List<String> = emptyList(),
    val category: WordCategory,
    val tags: List<String>,
    val complexity: Int,
    val usefulness: Int,
    val notes: String?,
)
