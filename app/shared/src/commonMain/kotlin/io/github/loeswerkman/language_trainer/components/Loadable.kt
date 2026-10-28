package io.github.loeswerkman.language_trainer.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign

private sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

/** Runs [load] once per [key], showing a spinner while loading and an error message if it fails. */
@Composable
fun <T> Loadable(key: Any?, load: suspend () -> T, content: @Composable (T) -> Unit) {
    var state by remember(key) { mutableStateOf<LoadState<T>>(LoadState.Loading) }
    LaunchedEffect(key) {
        state = try {
            LoadState.Loaded(load())
        } catch (e: Exception) {
            LoadState.Failed(e.message ?: e.toString())
        }
    }
    when (val current = state) {
        LoadState.Loading -> CircularProgressIndicator()
        is LoadState.Failed -> ErrorMessage("Could not reach the server. Is it running?\n${current.message}")
        is LoadState.Loaded -> content(current.value)
    }
}

@Composable
fun ErrorMessage(text: String) {
    Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
}
