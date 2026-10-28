package io.github.loeswerkman.language_trainer.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle

/** Clickable text in the accent colour. */
@Composable
fun Link(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    Text(
        text,
        style = style,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick),
    )
}
