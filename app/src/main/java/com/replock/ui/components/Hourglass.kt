package com.replock.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.replock.ui.theme.ElectricGreen
import kotlin.math.sin

/**
 * Onboarding hero: a glowing hourglass with sand falling through the neck and
 * floating app badges that bob around it. Pure Canvas, animated.
 */
@Composable
fun HourglassHero(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hourglass")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )

    Canvas(modifier.fillMaxWidth()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val top = h * 0.12f
        val bottom = h * 0.88f
        val mid = h * 0.5f
        val halfTop = w * 0.19f
        val neck = w * 0.028f

        // Soft green glow behind the glass.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricGreen.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(cx, mid),
                radius = w * 0.48f,
            ),
            radius = w * 0.48f,
            center = Offset(cx, mid),
        )

        val glass = Path().apply {
            moveTo(cx - halfTop, top)
            lineTo(cx + halfTop, top)
            lineTo(cx + neck, mid)
            lineTo(cx + halfTop, bottom)
            lineTo(cx - halfTop, bottom)
            lineTo(cx - neck, mid)
            close()
        }
        drawPath(
            path = glass,
            brush = Brush.verticalGradient(
                colors = listOf(ElectricGreen.copy(alpha = 0.10f), ElectricGreen.copy(alpha = 0.03f)),
            ),
        )

        // Sand pile in the bottom bulb.
        val sandTop = bottom - (bottom - mid) * 0.42f
        val pile = Path().apply {
            moveTo(cx - halfTop * 0.84f, bottom)
            lineTo(cx + halfTop * 0.84f, bottom)
            lineTo(cx + halfTop * 0.22f, sandTop)
            lineTo(cx - halfTop * 0.22f, sandTop)
            close()
        }
        drawPath(
            path = pile,
            brush = Brush.verticalGradient(listOf(ElectricGreen, ElectricGreen.copy(alpha = 0.55f))),
        )

        // Sand stream through the neck and the grain currently falling.
        drawLine(
            color = ElectricGreen.copy(alpha = 0.7f),
            start = Offset(cx, mid),
            end = Offset(cx, sandTop),
            strokeWidth = 2.dp.toPx(),
        )
        drawCircle(
            color = ElectricGreen,
            radius = 3.dp.toPx(),
            center = Offset(cx, mid + (sandTop - mid) * phase),
        )

        // Glass edge: a wide faint stroke for glow plus a crisp outline.
        drawPath(path = glass, color = ElectricGreen.copy(alpha = 0.25f), style = Stroke(width = 8.dp.toPx()))
        drawPath(path = glass, color = ElectricGreen, style = Stroke(width = 2.5.dp.toPx()))

        // Caps top and bottom.
        val capWidth = halfTop * 2f + 14.dp.toPx()
        drawRoundRect(
            color = ElectricGreen,
            topLeft = Offset(cx - capWidth / 2f, top - 12.dp.toPx()),
            size = Size(capWidth, 7.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = ElectricGreen,
            topLeft = Offset(cx - capWidth / 2f, bottom + 5.dp.toPx()),
            size = Size(capWidth, 7.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )

        // Floating app badges around the glass.
        val badges = listOf(
            Triple(0.14f, 0.24f, Color(0xFF1877F2)),
            Triple(0.86f, 0.20f, Color(0xFFFF3B30)),
            Triple(0.08f, 0.56f, Color(0xFFB388FF)),
            Triple(0.91f, 0.60f, Color(0xFFFF9500)),
            Triple(0.22f, 0.86f, Color(0xFF5865F2)),
            Triple(0.78f, 0.84f, Color(0xFF00C853)),
        )
        badges.forEachIndexed { index, (fx, fy, color) ->
            val dy = sin((bob + index * 0.17f) * 2f * Math.PI.toFloat()) * 5.dp.toPx()
            val center = Offset(w * fx, h * fy + dy)
            val radius = 17.dp.toPx()
            drawCircle(color = color.copy(alpha = 0.22f), radius = radius + 6.dp.toPx(), center = center)
            drawCircle(color = color, radius = radius, center = center)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.9f),
                topLeft = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
                size = Size(radius * 0.8f, radius * 0.8f),
                cornerRadius = CornerRadius(radius * 0.2f),
            )
        }
    }
}
