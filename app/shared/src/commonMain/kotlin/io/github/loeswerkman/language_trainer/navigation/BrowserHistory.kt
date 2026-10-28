package io.github.loeswerkman.language_trainer.navigation

/** The path of the current browser URL, e.g. "/russian/training". */
expect fun currentPath(): String

/** Changes the browser URL to [path] without reloading the page. */
expect fun pushPath(path: String)

/** Calls [listener] with the new path when the user presses back/forward. Returns a function that stops listening. */
expect fun onPathChanged(listener: (String) -> Unit): () -> Unit
