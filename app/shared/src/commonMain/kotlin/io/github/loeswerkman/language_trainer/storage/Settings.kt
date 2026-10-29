package io.github.loeswerkman.language_trainer.storage

/** Reads a setting saved on this device, or null if there is none or storage is unavailable. */
expect fun loadSetting(key: String): String?

/** Saves a setting on this device. Does nothing if storage is unavailable. */
expect fun saveSetting(key: String, value: String)

/** Key-value settings. [Device] keeps them in the browser; tests can use [InMemory]. */
interface Settings {
    fun load(key: String): String?
    fun save(key: String, value: String)

    object Device : Settings {
        override fun load(key: String) = loadSetting(key)
        override fun save(key: String, value: String) = saveSetting(key, value)
    }

    class InMemory(val values: MutableMap<String, String> = mutableMapOf()) : Settings {
        override fun load(key: String) = values[key]
        override fun save(key: String, value: String) {
            values[key] = value
        }
    }
}
