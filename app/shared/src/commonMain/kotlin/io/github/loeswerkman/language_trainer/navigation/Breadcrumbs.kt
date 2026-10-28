package io.github.loeswerkman.language_trainer.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.loeswerkman.language_trainer.components.Link
import io.github.loeswerkman.language_trainer.theme.AppColors

/** Shows the trail to [current]. Earlier pages are links; the current page is plain text. */
@Composable
fun Breadcrumbs(current: Route, onNavigate: (Route) -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyLarge
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        current.breadcrumbs.forEachIndexed { index, crumb ->
            if (index > 0) Text(">", style = style, color = AppColors.text)
            if (crumb.route == current) {
                Text(crumb.label, style = style, color = AppColors.text)
            } else {
                Link(crumb.label, onClick = { onNavigate(crumb.route) }, style = style)
            }
        }
    }
}
