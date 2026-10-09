package com.replock.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.replock.domain.ExerciseMode
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepGray
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Bright highlight used for the head sheen and the joint-angle label. */
private val Highlight = Color(0xFFEAFFF4)

/**
 * Animated exercise demo: a filled, anatomically-proportioned side-view figure
 * (tapered limbs, solid torso and head) that performs one full rep on loop
 * (hold top → lower → hold bottom → rise).
 *
 * The working joint (elbow for pushups, knee for squats) gets a pulsing ring,
 * an arc spanning the live joint angle, and a labelled degree readout, so the
 * demo shows exactly which angle RepLock counts. Pass a fixed [depth] (0 = top,
 * 1 = bottom) for a static pose; [showPhone] adds viewfinder brackets and a
 * phone glyph to illustrate camera placement.
 */
@Composable
fun ExerciseDemo(
    exercise: ExerciseMode,
    modifier: Modifier = Modifier,
    depth: Float? = null,
    showPhone: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "exercise-demo")
    val animated by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2400
                0f at 0      // hold at the top
                0f at 400
                1f at 1200   // lower
                1f at 1450   // hold at the bottom
                0f at 2400   // press back up
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "depth",
    )
    val d = (depth ?: animated).coerceIn(0f, 1f)

    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        // Uniform scale into a 100x100 design space, centred in the canvas.
        val s = minOf(size.width, size.height) / 100f
        val ox = (size.width - 100f * s) / 2f
        val oy = (size.height - 100f * s) / 2f
        fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)

        val j = stickFigure(exercise, d)
        val body = ElectricGreen

        // Floor + soft contact shadow.
        drawLine(
            color = RepGray.copy(alpha = 0.35f),
            start = p(4f, 91f),
            end = p(96f, 91f),
            strokeWidth = 1.2f * s,
            cap = StrokeCap.Round,
        )
        val shadowCx = (j.wrist.x + j.ankle.x) / 2f
        drawOval(
            color = body.copy(alpha = 0.12f),
            topLeft = p(shadowCx - 24f, 89.6f),
            size = Size(48f * s, 3f * s),
        )

        // Far-side limbs first (dimmer, nudged for depth), then the near side.
        val nudge = FigPoint(2.2f, -1.2f)
        fun nudged(pt: FigPoint) = FigPoint(pt.x + nudge.x, pt.y + nudge.y)
        val farColor = body.copy(alpha = 0.42f)
        drawLimb(nudged(j.shoulder), nudged(j.elbow), 4.4f, 3.4f, farColor, s, ::p)
        drawLimb(nudged(j.elbow), nudged(j.wrist), 3.4f, 2.6f, farColor, s, ::p)
        drawLimb(nudged(j.hip), nudged(j.knee), 5.6f, 4.2f, farColor, s, ::p)
        drawLimb(nudged(j.knee), nudged(j.ankle), 4.0f, 2.8f, farColor, s, ::p)

        // Near side: torso, neck, legs, arm.
        drawLimb(j.shoulder, j.hip, 7.6f, 8.6f, body, s, ::p)
        drawLimb(j.head, j.shoulder, 3.6f, 4.2f, body, s, ::p)
        drawLimb(j.hip, j.knee, 5.6f, 4.2f, body, s, ::p)
        drawLimb(j.knee, j.ankle, 4.0f, 2.8f, body, s, ::p)
        drawLimb(j.shoulder, j.elbow, 4.4f, 3.4f, body, s, ::p)
        drawLimb(j.elbow, j.wrist, 3.4f, 2.6f, body, s, ::p)
        drawCircle(body, radius = 2.8f * s, center = p(j.wrist.x, j.wrist.y))

        // Head with a soft glow and a sheen highlight.
        val headC = p(j.head.x, j.head.y)
        drawCircle(body.copy(alpha = 0.16f), radius = 10f * s, center = headC)
        drawCircle(body, radius = 6.2f * s, center = headC)
        drawCircle(Highlight.copy(alpha = 0.45f), radius = 1.8f * s, center = headC + Offset(-2.2f * s, -2.4f * s))

        // Working-joint angle: pulsing ring, arc over the live angle, and a label.
        fun angleAt(joint: FigPoint, a: FigPoint, b: FigPoint, label: String) {
            val c = p(joint.x, joint.y)
            val pa = p(a.x, a.y)
            val pb = p(b.x, b.y)
            val a1 = (atan2((pa.y - c.y).toDouble(), (pa.x - c.x).toDouble()) * 180.0 / PI).toFloat()
            val a2 = (atan2((pb.y - c.y).toDouble(), (pb.x - c.x).toDouble()) * 180.0 / PI).toFloat()
            var delta = (a2 - a1) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            val start = if (delta >= 0f) a1 else a2
            val sweep = abs(delta)
            val midDeg = start + sweep / 2f
            val radius = 12.5f * s

            // Pulsing ring on the joint.
            drawCircle(
                color = Highlight.copy(alpha = 0.15f + 0.45f * (1f - pulse)),
                radius = (3.6f + 2.4f * pulse) * s,
                center = c,
                style = Stroke(1.4f * s),
            )
            if (sweep > 4f) {
                drawArc(
                    color = Highlight.copy(alpha = 0.95f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(c.x - radius, c.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(1.8f * s, cap = StrokeCap.Round),
                )
                val rad = midDeg * PI / 180.0
                val labelC = c + Offset(
                    (radius + 9f * s) * cos(rad).toFloat(),
                    (radius + 9f * s) * sin(rad).toFloat(),
                )
                val layout = textMeasurer.measure(
                    text = "$label ${sweep.roundToInt()}°",
                    style = TextStyle(fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Highlight),
                )
                drawText(
                    layout,
                    topLeft = labelC - Offset(layout.size.width / 2f, layout.size.height / 2f),
                )
            }
        }

        when (exercise) {
            ExerciseMode.Pushups -> angleAt(j.elbow, j.shoulder, j.wrist, "ELBOW")
            ExerciseMode.Squats -> angleAt(j.knee, j.hip, j.ankle, "KNEE")
            ExerciseMode.Both -> {
                angleAt(j.elbow, j.shoulder, j.wrist, "ELBOW")
                angleAt(j.knee, j.hip, j.ankle, "KNEE")
            }
        }

        if (showPhone) {
            // Viewfinder corners: what the camera sees.
            val inset = 3f
            val arm = 10f
            val w = 1.6f * s
            val c = RepGray.copy(alpha = 0.7f)
            val tl = p(inset, inset)
            val tr = p(100f - inset, inset)
            val bl = p(inset, 100f - inset)
            val br = p(100f - inset, 100f - inset)
            drawLine(c, tl, tl + Offset(arm * s, 0f), w, StrokeCap.Round)
            drawLine(c, tl, tl + Offset(0f, arm * s), w, StrokeCap.Round)
            drawLine(c, tr, tr + Offset(-arm * s, 0f), w, StrokeCap.Round)
            drawLine(c, tr, tr + Offset(0f, arm * s), w, StrokeCap.Round)
            drawLine(c, bl, bl + Offset(arm * s, 0f), w, StrokeCap.Round)
            drawLine(c, bl, bl + Offset(0f, -arm * s), w, StrokeCap.Round)
            drawLine(c, br, br + Offset(-arm * s, 0f), w, StrokeCap.Round)
            drawLine(c, br, br + Offset(0f, -arm * s), w, StrokeCap.Round)
            // Phone glyph with its lens facing the figure.
            drawRoundRect(
                color = RepGray.copy(alpha = 0.9f),
                topLeft = p(4f, 66f),
                size = Size(10f * s, 18f * s),
                cornerRadius = CornerRadius(2.2f * s, 2.2f * s),
                style = Stroke(1.6f * s),
            )
            val lens = p(9f, 70f)
            drawCircle(ElectricGreen.copy(alpha = 0.25f), radius = 3.2f * s, center = lens)
            drawCircle(ElectricGreen, radius = 1.6f * s, center = lens)
        }
    }
}

/** A tapered, round-capped limb from [a] to [b] (radius [ra] at a, [rb] at b). */
private fun DrawScope.drawLimb(
    a: FigPoint,
    b: FigPoint,
    ra: Float,
    rb: Float,
    color: Color,
    s: Float,
    p: (Float, Float) -> Offset,
) {
    val pa = p(a.x, a.y)
    val pb = p(b.x, b.y)
    val dx = pb.x - pa.x
    val dy = pb.y - pa.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val nx = -dy / len
    val ny = dx / len
    val radA = ra * s
    val radB = rb * s
    val path = Path().apply {
        moveTo(pa.x + nx * radA, pa.y + ny * radA)
        lineTo(pb.x + nx * radB, pb.y + ny * radB)
        lineTo(pb.x - nx * radB, pb.y - ny * radB)
        lineTo(pa.x - nx * radA, pa.y - ny * radA)
        close()
    }
    drawPath(path, color)
    drawCircle(color, radius = radA, center = pa)
    drawCircle(color, radius = radB, center = pb)
}

private data class FigPoint(val x: Float, val y: Float)

private data class Joints(
    val head: FigPoint,
    val shoulder: FigPoint,
    val elbow: FigPoint,
    val wrist: FigPoint,
    val hip: FigPoint,
    val knee: FigPoint,
    val ankle: FigPoint,
)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun lerpPoint(a: FigPoint, b: FigPoint, t: Float) =
    FigPoint(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

/**
 * Side-view figure for [exercise], interpolated by [depth] (0 = top/standing,
 * 1 = bottom). Proportions are roughly human; the poses are schematic but
 * show the true joint geometry RepLock measures.
 */
private fun stickFigure(exercise: ExerciseMode, depth: Float): Joints {
    val d = depth.coerceIn(0f, 1f)
    return when (exercise) {
        ExerciseMode.Pushups, ExerciseMode.Both -> {
            // Plank, facing left. Hands and toes on the floor (y = 88).
            val wrist = FigPoint(38f, 88f)
            val ankle = FigPoint(84f, 88f)
            val shoulderY = lerp(46f, 64f, d)
            val shoulder = FigPoint(42f, shoulderY)
            // Hip and knee sit on the straight shoulder→ankle line.
            val t = { f: Float -> FigPoint(lerp(shoulder.x, ankle.x, f), lerp(shoulder.y, ankle.y, f)) }
            Joints(
                head = FigPoint(33f, shoulderY - 7f),
                shoulder = shoulder,
                elbow = lerpPoint(FigPoint(41f, 66f), FigPoint(54f, 76f), d), // straight arm → bent ≈ 75°
                wrist = wrist,
                hip = t(0.58f),
                knee = t(0.80f),
                ankle = ankle,
            )
        }
        ExerciseMode.Squats -> {
            // Squat, facing left. Feet flat on the floor (y = 88).
            Joints(
                head = lerpPoint(FigPoint(50f, 14f), FigPoint(42f, 29f), d),
                shoulder = lerpPoint(FigPoint(54f, 26f), FigPoint(46f, 41f), d),
                elbow = lerpPoint(FigPoint(50f, 42f), FigPoint(40f, 52f), d),
                wrist = lerpPoint(FigPoint(48f, 56f), FigPoint(44f, 64f), d),
                hip = lerpPoint(FigPoint(58f, 46f), FigPoint(63f, 62f), d),
                knee = lerpPoint(FigPoint(60f, 68f), FigPoint(48f, 70f), d), // standing ≈165° → deep ≈80°
                ankle = FigPoint(64f, 88f),
            )
        }
    }
}
