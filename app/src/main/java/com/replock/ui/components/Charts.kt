package com.replock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.replock.ui.theme.ElectricGreen

/** One line in a [LineChart]. */
data class ChartSeries(val values: List<Int>, val color: Color)

/** Single-series smooth sparkline with a soft gradient fill underneath. */
@Composable
fun Sparkline(
    values: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = ElectricGreen,
) {
    LineChart(
        series = listOf(ChartSeries(values, color)),
        modifier = modifier,
        fill = true,
    )
}

/**
 * Smooth multi-series line chart. All series share one vertical scale so they
 * can be compared directly. Flat (all-zero) data draws a baseline.
 */
@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val maxValue = series.maxOfOrNull { s -> s.values.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
        val strokePx = 2.5.dp.toPx()
        val top = strokePx
        val usable = size.height - strokePx * 2

        // Baseline so empty charts still read as a chart.
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(0f, size.height - strokePx / 2),
            end = Offset(size.width, size.height - strokePx / 2),
            strokeWidth = 1.dp.toPx(),
        )

        series.forEach { s ->
            if (s.values.size < 2) return@forEach
            val stepX = size.width / (s.values.size - 1)
            val points = s.values.mapIndexed { i, v ->
                Offset(i * stepX, top + usable * (1f - v / maxValue.toFloat()))
            }
            val line = smoothPath(points)

            if (fill) {
                val area = Path().apply {
                    addPath(line)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = area,
                    brush = Brush.verticalGradient(
                        listOf(s.color.copy(alpha = 0.32f), Color.Transparent),
                    ),
                )
            }
            // Soft glow then crisp stroke.
            drawPath(
                path = line,
                color = s.color.copy(alpha = 0.25f),
                style = Stroke(width = strokePx * 3f, cap = StrokeCap.Round),
            )
            drawPath(
                path = line,
                color = s.color,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

/** Catmull-style smoothing via horizontal-midpoint cubic Béziers. */
private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        val midX = (prev.x + cur.x) / 2f
        path.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
    }
    return path
}
