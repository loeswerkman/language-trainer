package io.github.loeswerkman.language_trainer.storage

import kotlinx.browser.window

// localStorage throws when the browser blocks it (e.g. some private modes); settings are then simply not kept.
actual fun loadSetting(key: String): String? = try {
    window.localStorage.getItem(key)
} catch (e: Throwable) {
    null
}

actual fun saveSetting(key: String, value: String) {
    try {
        window.localStorage.setItem(key, value)
    } catch (e: Throwable) {
        // Not saved; the setting still applies until the page is reloaded.
    }
}
