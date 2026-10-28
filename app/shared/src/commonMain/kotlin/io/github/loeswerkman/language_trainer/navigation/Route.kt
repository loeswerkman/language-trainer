package io.github.loeswerkman.language_trainer.navigation

/** The pages of the app. Each page has its own URL path and knows its place in the breadcrumb trail. */
sealed interface Route {
    val path: String
    val breadcrumbs: List<Breadcrumb>

    data object Home : Route {
        override val path = "/"
        override val breadcrumbs = listOf(Breadcrumb("Home", Home))
    }

    data class LanguageHome(val language: String) : Route {
        override val path = "/$language"
        override val breadcrumbs = Home.breadcrumbs + Breadcrumb(languageDisplayName(language), this)
    }

    data class Training(val language: String) : Route {
        override val path = "/$language/training"
        override val breadcrumbs = LanguageHome(language).breadcrumbs + Breadcrumb("Training", this)
    }

    companion object {
        /** Maps a URL path back to its page. Unknown paths lead to [Home]. */
        fun fromPath(path: String): Route {
            val segments = path.split("/").filter { it.isNotEmpty() }
            return when {
                segments.isEmpty() -> Home
                segments.size == 1 -> LanguageHome(segments[0])
                segments.size == 2 && segments[1] == "training" -> Training(segments[0])
                else -> Home
            }
        }
    }
}

data class Breadcrumb(val label: String, val route: Route)

/** Turns a language key such as "russian" into a display name such as "Russian". */
fun languageDisplayName(language: String): String = language.replaceFirstChar { it.uppercase() }
