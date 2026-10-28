package io.github.loeswerkman.language_trainer.navigation

import kotlinx.browser.window
import org.w3c.dom.events.Event

actual fun currentPath(): String = window.location.pathname

actual fun pushPath(path: String) {
    if (path != currentPath()) window.history.pushState(null, "", path)
}

actual fun onPathChanged(listener: (String) -> Unit): () -> Unit {
    val callback: (Event) -> Unit = { listener(currentPath()) }
    window.addEventListener("popstate", callback)
    return { window.removeEventListener("popstate", callback) }
}
