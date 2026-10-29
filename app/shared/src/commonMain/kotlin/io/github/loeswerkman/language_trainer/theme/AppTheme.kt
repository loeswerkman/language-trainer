package io.github.loeswerkman.language_trainer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The app's colours. Change [accent] to recolour links, the answer field and buttons. */
object AppColors {
    val accent = Color(0xFFD0BCFF)
    val background = Color.Black
    val text = Color.White
    val correct = Color(0xFF66BB6A)
    val almostCorrect = Color(0xFFFFA726)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AppColors.accent,
            background = AppColors.background,
            surface = AppColors.background,
            onBackground = AppColors.text,
            onSurface = AppColors.text,
        ),
        content = content,
    )
}
