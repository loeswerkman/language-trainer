package io.github.loeswerkman.language_trainer.vocabulary

import io.github.loeswerkman.language_trainer.query.Condition
import io.github.loeswerkman.language_trainer.query.ConditionGroup
import io.github.loeswerkman.language_trainer.query.Filter
import io.github.loeswerkman.language_trainer.query.ListQuery
import io.github.loeswerkman.language_trainer.query.Operator
import io.github.loeswerkman.language_trainer.query.SortDirection
import io.github.loeswerkman.language_trainer.query.SortKey
import io.github.loeswerkman.language_trainer.storage.ListQueryStore
import io.github.loeswerkman.language_trainer.storage.Settings

/**
 * The vocabulary table and training each have their own filter and sort, saved separately per language,
 * so browsing the vocabulary never changes what is trained.
 */
object VocabularyQueries {

    /** Every word: learned words first, then alphabetically. */
    val tableDefault = ListQuery(
        sort = listOf(
            SortKey.ByField(VocabularyFields.learned.id, SortDirection.DESCENDING),
            SortKey.ByField(VocabularyFields.word.id, SortDirection.ASCENDING),
        ),
    )

    /**
     * Words not yet learned; words never (or longest ago) answered first, then the easiest.
     */
    val trainingDefault = ListQuery(
        filter = Filter(listOf(ConditionGroup(listOf(Condition(VocabularyFields.learned.id, Operator.IS_NO))))),
        sort = listOf(
            SortKey.ByField(VocabularyFields.lastAnswered.id, SortDirection.ASCENDING),
            SortKey.ByField(VocabularyFields.complexity.id, SortDirection.ASCENDING),
        ),
    )

    fun tableStore(language: String, settings: Settings = Settings.Device) =
        ListQueryStore("vocabulary-query.$language", VocabularyFields.all, tableDefault, settings)

    fun trainingStore(language: String, settings: Settings = Settings.Device) =
        ListQueryStore("training-query.$language", VocabularyFields.all, trainingDefault, settings)
}
