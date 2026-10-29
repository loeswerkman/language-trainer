package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.query.ChoiceField
import io.github.loeswerkman.language_trainer.query.DateField
import io.github.loeswerkman.language_trainer.query.FieldSet
import io.github.loeswerkman.language_trainer.query.NumberField
import io.github.loeswerkman.language_trainer.query.Option
import io.github.loeswerkman.language_trainer.query.TagsField
import io.github.loeswerkman.language_trainer.query.TextField
import kotlinx.serialization.Serializable

/** A word together with how it was answered over all training sessions. */
@Serializable
data class VocabularyItem(
    val word: Word,
    val correct: Int = 0,
    val almostCorrect: Int = 0,
    val incorrect: Int = 0,
    /** Epoch milliseconds of the most recent answer, or null if the word was never answered. */
    val lastAnsweredAt: Long? = null,
) {
    val score: Int get() = correct - incorrect
}

/** The fields of the vocabulary list, used by the vocabulary table and training. Ids are saved in queries: keep them stable. */
object VocabularyFields {
    val word = TextField<VocabularyItem>("word", "Word") { it.word.word }
    val translation = TextField<VocabularyItem>("translation", "Translation") { it.word.translation }
    val category = ChoiceField<VocabularyItem>(
        "category",
        "Category",
        WordCategory.entries.map { Option(it.label, it.label) },
    ) { it.word.category.label }
    val tags = TagsField<VocabularyItem>("tags", "Tags") { it.word.tags }
    val complexity = NumberField<VocabularyItem>("complexity", "Complexity") { it.word.complexity }
    val usefulness = NumberField<VocabularyItem>("usefulness", "Usefulness") { it.word.usefulness }
    val correct = NumberField<VocabularyItem>("correct", "Correct") { it.correct }
    val almostCorrect = NumberField<VocabularyItem>("almostCorrect", "Almost correct") { it.almostCorrect }
    val incorrect = NumberField<VocabularyItem>("incorrect", "Incorrect") { it.incorrect }
    val score = NumberField<VocabularyItem>("score", "Score") { it.score }
    val lastAnswered = DateField<VocabularyItem>("lastAnswered", "Last answered") { it.lastAnsweredAt }

    val all = FieldSet(
        listOf(word, translation, category, tags, complexity, usefulness, correct, almostCorrect, incorrect, score, lastAnswered),
    )
}
