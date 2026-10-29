package io.github.loeswerkman.language_trainer.components.table

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Icons are drawn here because the app doesn't depend on an icon library.

/** A funnel. */
val FilterIcon: ImageVector = ImageVector.Builder("Filter", 24.dp, 24.dp, 24f, 24f)
    .path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 4f)
        horizontalLineTo(21f)
        lineTo(14f, 12.5f)
        verticalLineTo(19f)
        lineTo(10f, 21f)
        verticalLineTo(12.5f)
        close()
    }
    .build()

/** An open eye: an outlined almond with a round pupil. */
val EyeIcon: ImageVector = ImageVector.Builder("Eye", 24.dp, 24.dp, 24f, 24f)
    .path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(2f, 12f)
        quadTo(12f, 2f, 22f, 12f)
        quadTo(12f, 22f, 2f, 12f)
        close()
    }
    .path(fill = SolidColor(Color.Black)) {
        moveTo(15f, 12f)
        arcTo(3f, 3f, 0f, true, true, 9f, 12f)
        arcTo(3f, 3f, 0f, true, true, 15f, 12f)
        close()
    }
    .build()
