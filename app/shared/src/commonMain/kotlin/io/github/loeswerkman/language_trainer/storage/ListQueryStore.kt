package io.github.loeswerkman.language_trainer.storage

import io.github.loeswerkman.language_trainer.query.FieldSet
import io.github.loeswerkman.language_trainer.query.ListQuery
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/**
 * Keeps the filter and sort of one view of a list under [key]. Until the user changes it, the view uses [defaultQuery].
 */
class ListQueryStore<T>(
    val key: String,
    val fields: FieldSet<T>,
    val defaultQuery: ListQuery,
    private val settings: Settings = Settings.Device,
) {
    /** The saved query without anything the fields no longer support, or [defaultQuery] if none (valid) is saved. */
    fun load(): ListQuery {
        val saved = settings.load(key) ?: return defaultQuery
        return try {
            fields.sanitize(json.decodeFromString<ListQuery>(saved))
        } catch (e: Exception) {
            defaultQuery
        }
    }

    fun save(query: ListQuery) = settings.save(key, json.encodeToString(query))
}
