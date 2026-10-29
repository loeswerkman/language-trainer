package io.github.loeswerkman.language_trainer.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {

    @Test
    fun trainingBreadcrumbsLeadBackThroughLanguageToHome() {
        val training = Route.Training("russian")

        assertEquals(
            listOf(
                Breadcrumb("Home", Route.Home),
                Breadcrumb("Russian", Route.LanguageHome("russian")),
                Breadcrumb("Training", training),
            ),
            training.breadcrumbs,
        )
    }

    @Test
    fun vocabularyBreadcrumbsLeadBackThroughLanguageToHome() {
        val vocabulary = Route.Vocabulary("russian")

        assertEquals(
            listOf(
                Breadcrumb("Home", Route.Home),
                Breadcrumb("Russian", Route.LanguageHome("russian")),
                Breadcrumb("Vocabulary", vocabulary),
            ),
            vocabulary.breadcrumbs,
        )
    }

    @Test
    fun homeOnlyHasItself() {
        assertEquals(listOf(Breadcrumb("Home", Route.Home)), Route.Home.breadcrumbs)
    }

    @Test
    fun everyRouteRoundTripsThroughItsPath() {
        listOf(Route.Home, Route.LanguageHome("russian"), Route.Training("russian"), Route.Vocabulary("russian")).forEach { route ->
            assertEquals(route, Route.fromPath(route.path))
        }
    }

    @Test
    fun pathsMatchThePageStructure() {
        assertEquals("/", Route.Home.path)
        assertEquals("/russian", Route.LanguageHome("russian").path)
        assertEquals("/russian/training", Route.Training("russian").path)
        assertEquals("/russian/vocabulary", Route.Vocabulary("russian").path)
    }

    @Test
    fun unknownPathsLeadHome() {
        assertEquals(Route.Home, Route.fromPath("/russian/unknown"))
        assertEquals(Route.Home, Route.fromPath("/a/b/c"))
    }

    @Test
    fun trailingSlashIsIgnored() {
        assertEquals(Route.Training("russian"), Route.fromPath("/russian/training/"))
    }
}
