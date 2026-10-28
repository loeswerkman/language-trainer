package io.github.loeswerkman.language_trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.data.VocabularyClient
import io.github.loeswerkman.language_trainer.home.HomeScreen
import io.github.loeswerkman.language_trainer.language.LanguageHomeScreen
import io.github.loeswerkman.language_trainer.navigation.Breadcrumbs
import io.github.loeswerkman.language_trainer.navigation.Route
import io.github.loeswerkman.language_trainer.navigation.currentPath
import io.github.loeswerkman.language_trainer.navigation.onPathChanged
import io.github.loeswerkman.language_trainer.navigation.pushPath
import io.github.loeswerkman.language_trainer.theme.AppTheme
import io.github.loeswerkman.language_trainer.trainer.TrainingScreen

@Composable
@Preview
fun App() {
    AppTheme {
        val client = remember { VocabularyClient() }
        var route by remember { mutableStateOf(Route.fromPath(currentPath())) }
        val navigate = { next: Route ->
            pushPath(next.path)
            route = next
        }
        DisposableEffect(Unit) {
            onPathChanged { route = Route.fromPath(it) }.let { stop -> onDispose { stop() } }
        }

        // Surface sets a light content colour so text stays readable on the black background.
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.safeContentPadding().fillMaxSize().padding(24.dp)) {
                Breadcrumbs(route, onNavigate = navigate, modifier = Modifier.align(Alignment.TopStart))
                Column(
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 480.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (val current = route) {
                        Route.Home -> HomeScreen(client, onLanguageSelected = { navigate(Route.LanguageHome(it)) })
                        is Route.LanguageHome ->
                            LanguageHomeScreen(onTrainingSelected = { navigate(Route.Training(current.language)) })
                        is Route.Training -> TrainingScreen(client, current.language)
                    }
                }
            }
        }
    }
}
