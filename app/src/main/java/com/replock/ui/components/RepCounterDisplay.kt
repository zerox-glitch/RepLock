package com.replock.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepGray

/** Huge, satisfying rep counter with an electric-green progress ring. */
@Composable
fun RepCounterDisplay(reps: Int, target: Int, modifier: Modifier = Modifier) {
    val progress = (reps.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)

    // The ring eases to the new value instead of jumping.
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "ring",
    )

    // The number pops on every counted rep.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(reps) {
        if (reps > 0) {
            pop.snapTo(1.22f)
            pop.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp)) {
            val stroke = 14.dp.toPx()
            drawCircle(color = Color(0xFF1E1E1E), style = Stroke(stroke))
            drawArc(
                color = ElectricGreen,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$reps",
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 110.sp,
                color = if (reps >= target) ElectricGreen else Color.White,
                modifier = Modifier.graphicsLayer(scaleX = pop.value, scaleY = pop.value),
            )
            Text(
                text = "of $target",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = RepGray,
            )
        }
    }
}
