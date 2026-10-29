package io.github.loeswerkman.language_trainer.storage

import kotlinx.serialization.json.Json

/** Remembers on this device which columns of a table the user hid. Unknown ids are harmless and simply ignored. */
class HiddenColumnsStore(private val key: String, private val settings: Settings = Settings.Device) {

    fun load(): Set<String> = try {
        settings.load(key)?.let { Json.decodeFromString<Set<String>>(it) } ?: emptySet()
    } catch (e: Exception) {
        emptySet()
    }

    fun save(hidden: Set<String>) = settings.save(key, Json.encodeToString(hidden))

    companion object {
        /** Only for the vocabulary table; training doesn't show columns. */
        fun vocabulary(language: String) = HiddenColumnsStore("vocabulary-columns.$language")
    }
}
