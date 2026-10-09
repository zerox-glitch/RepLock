package com.replock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.ui.theme.ElectricGreen

/** Dark, green-tinted glass surface used for every card in the app. */
val GlassSurface = Color(0xFF0D1613)

/** Hairline green border that gives the cards their "glass" edge. */
val GlassBorder = ElectricGreen.copy(alpha = 0.16f)

/** Standard card: rounded, green hairline border, no elevation. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier.border(1.dp, GlassBorder, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = GlassSurface),
    ) {
        content()
    }
}

/** Small rounded status pill, e.g. "ACTIVE", "MISSING", "PRO UNLIMITED". */
@Composable
fun PillBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Circular progress ring, used for "unlocks used today". */
@Composable
fun MiniRing(progress: Float, modifier: Modifier = Modifier, color: Color = ElectricGreen) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.13f
        val inset = stroke / 2f
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = (size.minDimension - stroke) / 2f,
            center = center,
            style = Stroke(stroke),
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}
