package com.replock.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.replock.domain.ExerciseMode
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepGray

/**
 * Stick-figure demo of an exercise, drawn on a Canvas.
 *
 * The figure lives in a 100x100 design space and is lerped by a depth parameter:
 * 0 = top position (arms straight / standing), 1 = bottom position (chest down /
 * deep squat). Pass `depth = null` for a looping animation (one rep cycle), or a
 * fixed value for a static pose. [showPhone] draws a small phone glyph to
 * illustrate camera placement.
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
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "depth",
    )
    val d = (depth ?: animated).coerceIn(0f, 1f)

    Canvas(modifier = modifier) {
        val scaleX = size.width / 100f
        val scaleY = size.height / 100f
        fun p(x: Float, y: Float) = Offset(x * scaleX, y * scaleY)

        val joints = stickFigure(exercise, d)
        val bones = listOf(
            joints.head to joints.shoulder,
            joints.shoulder to joints.hip,
            joints.hip to joints.knee,
            joints.knee to joints.ankle,
            joints.shoulder to joints.elbow,
            joints.elbow to joints.wrist,
        )
        val lineWidth = 3f * scaleX
        bones.forEach { (a, b) ->
            drawLine(
                color = ElectricGreen,
                start = p(a.x, a.y),
                end = p(b.x, b.y),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round,
            )
        }
        // joints
        listOf(joints.shoulder, joints.hip, joints.knee, joints.elbow, joints.ankle, joints.wrist)
            .forEach { j ->
                drawCircle(ElectricGreen, radius = 2.2f * scaleX, center = p(j.x, j.y))
            }
        // head
        drawCircle(
            color = ElectricGreen,
            radius = 5f * scaleX,
            center = p(joints.head.x, joints.head.y),
            style = Stroke(lineWidth),
        )

        if (showPhone) {
            // little phone glyph, bottom-left, with a camera dot
            drawRoundRect(
                color = RepGray,
                topLeft = p(2f, 68f),
                size = Size(12f * scaleX, 22f * scaleY),
                cornerRadius = CornerRadius(2f * scaleX, 2f * scaleX),
                style = Stroke(1.5f * scaleX),
            )
            drawCircle(RepGray, radius = 1.6f * scaleX, center = p(8f, 73f))
        }
    }
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
 * Side-view stick figure for [exercise], interpolated by [depth] (0 = top/standing,
 * 1 = bottom). Schematic, not biomechanically exact — it exists to show the motion
 * and the camera angle.
 */
private fun stickFigure(exercise: ExerciseMode, depth: Float): Joints {
    val d = depth.coerceIn(0f, 1f)
    return when (exercise) {
        ExerciseMode.Pushups, ExerciseMode.Both -> {
            // Side-view plank, facing left. Floor at y=90. Feet (ankle) and hands (wrist) fixed.
            val ankle = FigPoint(82f, 88f)
            val wrist = FigPoint(28f, 88f)
            val shoulderY = lerp(46f, 68f, d) // high (arms straight) -> low (chest down)
            val shoulder = FigPoint(46f, shoulderY)
            Joints(
                head = FigPoint(35f, shoulderY - 7f),
                shoulder = shoulder,
                elbow = lerpPoint(FigPoint(37f, 67f), FigPoint(40.6f, 62f), d), // bends up as arms bend
                wrist = wrist,
                hip = FigPoint(65.8f, lerp(69.1f, 79f, d)),
                knee = FigPoint(56.1f, lerp(57.8f, 73.6f, d)),
                ankle = ankle,
            )
        }
        ExerciseMode.Squats -> {
            // Side-view squat, facing left. Floor at y=90. Feet fixed.
            Joints(
                head = lerpPoint(FigPoint(50f, 15f), FigPoint(45f, 24f), d),
                shoulder = lerpPoint(FigPoint(58f, 24f), FigPoint(52f, 32f), d),
                elbow = lerpPoint(FigPoint(52f, 42f), FigPoint(56f, 48f), d),
                wrist = lerpPoint(FigPoint(48f, 58f), FigPoint(58f, 64f), d),
                hip = lerpPoint(FigPoint(58f, 46f), FigPoint(66f, 63f), d),
                knee = lerpPoint(FigPoint(58f, 67f), FigPoint(76f, 70f), d),
                ankle = FigPoint(72f, 88f),
            )
        }
    }
}
