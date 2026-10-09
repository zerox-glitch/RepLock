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
import androidx.compose.ui.graphics.StrokeCap
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

/** Joint marker dot color — bright, reads well on the dark overlay. */
private val JointDot = Color(0xFFEAFFF4)

/**
 * Animated exercise demo drawn on a Canvas.
 *
 * The figure lives in a 100x100 design space (side view, floor at y=90) and is
 * lerped by a depth parameter: 0 = top position (arms straight / standing),
 * 1 = bottom position (chest down / deep squat). Pass `depth = null` for a
 * looping one-rep animation (hold top → lower → hold bottom → rise), or a
 * fixed value for a static pose.
 *
 * The working joint (elbow for pushups, knee for squats) gets a pulsing ring,
 * an arc spanning the live joint angle, and a degree readout, so the demo
 * teaches the actual angle RepLock counts. [showPhone] adds a phone glyph and
 * viewfinder brackets to illustrate camera placement.
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
                durationMillis = 2200
                0f at 0     // hold at the top
                0f at 350
                1f at 1050  // lower down
                1f at 1300  // hold at the bottom
                0f at 2200  // push back up
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "depth",
    )
    val d = (depth ?: animated).coerceIn(0f, 1f)

    // Gentle pulse on the working joint so the eye finds the angle being counted.
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
        val scaleX = size.width / 100f
        val scaleY = size.height / 100f
        val s = minOf(scaleX, scaleY)
        fun p(x: Float, y: Float) = Offset(x * scaleX, y * scaleY)

        val joints = stickFigure(exercise, d)
        val color = ElectricGreen

        // Floor line + soft ground shadow.
        drawLine(
            color = RepGray.copy(alpha = 0.30f),
            start = p(6f, 90f),
            end = p(94f, 90f),
            strokeWidth = 1f * s,
            cap = StrokeCap.Round,
        )
        val shadowCx = (joints.wrist.x + joints.ankle.x) / 2f
        drawOval(
            color = color.copy(alpha = 0.10f),
            topLeft = p(shadowCx - 22f, 89.1f),
            size = Size(44f * scaleX, 2.6f * scaleY),
        )

        // Bones: glow halo, then a gradient limb.
        val limbW = 6.5f
        val bones = listOf(
            Bone(joints.shoulder, joints.hip, 10f),           // torso
            Bone(joints.hip, joints.knee, limbW),
            Bone(joints.knee, joints.ankle, limbW),
            Bone(joints.shoulder, joints.elbow, limbW),
            Bone(joints.elbow, joints.wrist, limbW),
            Bone(joints.shoulder, joints.head, limbW * 0.8f), // neck
        )
        bones.forEach { bone ->
            val a = p(bone.a.x, bone.a.y)
            val b = p(bone.b.x, bone.b.y)
            val w = bone.width * s
            drawLine(color.copy(alpha = 0.10f), a, b, strokeWidth = w * 2.6f, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = 0.16f), a, b, strokeWidth = w * 1.6f, cap = StrokeCap.Round)
            drawLine(
                brush = Brush.linearGradient(listOf(color.copy(alpha = 0.55f), color), start = a, end = b),
                start = a,
                end = b,
                strokeWidth = w,
                cap = StrokeCap.Round,
            )
        }

        // Head: glow, skull, highlight.
        val headR = 5.5f
        val headC = p(joints.head.x, joints.head.y)
        drawCircle(color.copy(alpha = 0.14f), radius = headR * 2.2f * s, center = headC)
        drawCircle(color, radius = headR * s, center = headC)
        drawCircle(
            Color.White.copy(alpha = 0.35f),
            radius = headR * 0.38f * s,
            center = headC + Offset(-headR * 0.35f * s, -headR * 0.35f * s),
        )

        // Joint dots.
        val jointR = 2.3f
        listOf(joints.shoulder, joints.hip, joints.knee, joints.elbow, joints.ankle, joints.wrist)
            .forEach { j ->
                val c = p(j.x, j.y)
                drawCircle(color.copy(alpha = 0.18f), radius = jointR * 2.4f * s, center = c)
                drawCircle(JointDot, radius = jointR * s, center = c)
            }

        // Live angle arc + degree readout at the working joint(s).
        fun drawAngle(joint: FigPoint, na: FigPoint, nb: FigPoint) {
            val j = p(joint.x, joint.y)
            val pa = p(na.x, na.y)
            val pb = p(nb.x, nb.y)
            val a1 = (atan2((pa.y - j.y).toDouble(), (pa.x - j.x).toDouble()) * 180.0 / PI).toFloat()
            val a2 = (atan2((pb.y - j.y).toDouble(), (pb.x - j.x).toDouble()) * 180.0 / PI).toFloat()
            var delta = (a2 - a1) % 360f
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            var start = if (delta >= 0f) a1 else a2
            val sweep = abs(delta)
            if (sweep > 172f) {
                // Nearly straight limb: pick the semicircle that bulges away from the body.
                val midRad = (start + sweep / 2f) * PI / 180.0
                val bx = cos(midRad).toFloat()
                val by = sin(midRad).toFloat()
                val toBody = Offset(55f * scaleX - j.x, 65f * scaleY - j.y)
                if (bx * toBody.x + by * toBody.y > 0f) start += sweep
            }
            // Pulsing ring on the joint.
            val ringR = (jointR + 1.5f + 2.2f * pulse) * s
            drawCircle(
                color.copy(alpha = 0.15f + 0.45f * (1f - pulse)),
                radius = ringR,
                center = j,
                style = Stroke(1.6f * s),
            )
            if (sweep > 4f) {
                val r = 15f * s
                drawArc(
                    color.copy(alpha = 0.9f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(j.x - r, j.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(2.2f * s, cap = StrokeCap.Round),
                )
                val midRad = (start + sweep / 2f) * PI / 180.0
                val labelC = j + Offset(
                    (r + 5.5f * s) * cos(midRad).toFloat(),
                    (r + 5.5f * s) * sin(midRad).toFloat(),
                )
                val text = "${sweep.roundToInt()}°"
                val layout = textMeasurer.measure(
                    text,
                    TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold, color = color),
                )
                drawText(layout, topLeft = labelC - Offset(layout.size.width / 2f, layout.size.height / 2f))
            }
        }

        when (exercise) {
            ExerciseMode.Pushups ->
                drawAngle(joints.elbow, joints.shoulder, joints.wrist)
            ExerciseMode.Squats ->
                drawAngle(joints.knee, joints.hip, joints.ankle)
            ExerciseMode.Both -> {
                drawAngle(joints.elbow, joints.shoulder, joints.wrist)
                drawAngle(joints.knee, joints.hip, joints.ankle)
            }
        }

        if (showPhone) {
            // Viewfinder brackets — this is what the camera sees.
            val inset = 2.5f
            val arm = 9f
            val w = 1.6f * s
            val c = RepGray.copy(alpha = 0.65f)
            val min = p(inset, inset)
            val max = p(100f - inset, 100f - inset)
            val tl = min
            val tr = Offset(max.x, min.y)
            val bl = Offset(min.x, max.y)
            val br = max
            listOf(
                tl to listOf(Offset(arm * scaleX, 0f), Offset(0f, arm * scaleY)),
                tr to listOf(Offset(-arm * scaleX, 0f), Offset(0f, arm * scaleY)),
                bl to listOf(Offset(arm * scaleX, 0f), Offset(0f, -arm * scaleY)),
                br to listOf(Offset(-arm * scaleX, 0f), Offset(0f, -arm * scaleY)),
            ).forEach { (corner, arms) ->
                arms.forEach { armVec ->
                    drawLine(c, corner, corner + armVec, strokeWidth = w, cap = StrokeCap.Round)
                }
            }
            // Phone glyph lying at the bottom-left, camera dot facing the figure.
            drawRoundRect(
                color = RepGray.copy(alpha = 0.9f),
                topLeft = p(3f, 70f),
                size = Size(11f * scaleX, 20f * scaleY),
                cornerRadius = CornerRadius(2.2f * s, 2.2f * s),
                style = Stroke(1.8f * s),
            )
            val cam = p(8.5f, 75f)
            drawCircle(ElectricGreen.copy(alpha = 0.25f), radius = 3.4f * s, center = cam)
            drawCircle(ElectricGreen, radius = 1.7f * s, center = cam)
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

private data class Bone(val a: FigPoint, val b: FigPoint, val width: Float)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun lerpPoint(a: FigPoint, b: FigPoint, t: Float) =
    FigPoint(lerp(a.x, b.x, t), lerp(a.y, b.y, t))

/**
 * Side-view figure for [exercise], interpolated by [depth] (0 = top/standing,
 * 1 = bottom). Schematic, not biomechanically exact — it exists to show the
 * motion, the camera angle, and the joint angle RepLock counts.
 */
private fun stickFigure(exercise: ExerciseMode, depth: Float): Joints {
    val d = depth.coerceIn(0f, 1f)
    return when (exercise) {
        ExerciseMode.Pushups, ExerciseMode.Both -> {
            // Side-view plank, facing left. Floor at y=89; hands (wrist) and feet (ankle) fixed.
            val wrist = FigPoint(40f, 89f)
            val ankle = FigPoint(80f, 89f)
            val shoulderY = lerp(50f, 66f, d) // high (arms straight) -> low (chest down)
            val shoulder = FigPoint(44f, shoulderY)
            // Hip and knee stay on the straight shoulder->ankle body line.
            val hip = FigPoint(65.6f, shoulderY + 0.6f * (89f - shoulderY))
            val knee = FigPoint(72.8f, shoulderY + 0.8f * (89f - shoulderY))
            Joints(
                head = FigPoint(36f, shoulderY - 6f),
                shoulder = shoulder,
                elbow = lerpPoint(FigPoint(42f, 69.5f), FigPoint(57.5f, 80.2f), d), // straight 180° -> bent ~73°
                wrist = wrist,
                hip = hip,
                knee = knee,
                ankle = ankle,
            )
        }
        ExerciseMode.Squats -> {
            // Side-view squat, facing left. Floor at y=89; feet (ankle) fixed.
            Joints(
                head = lerpPoint(FigPoint(52f, 20f), FigPoint(42f, 33f), d),
                shoulder = lerpPoint(FigPoint(56f, 30f), FigPoint(46f, 42f), d),
                elbow = lerpPoint(FigPoint(55f, 43f), FigPoint(38f, 50f), d),
                wrist = lerpPoint(FigPoint(54f, 55f), FigPoint(32f, 58f), d),
                hip = lerpPoint(FigPoint(60f, 44f), FigPoint(66f, 60f), d),
                knee = lerpPoint(FigPoint(58f, 66f), FigPoint(46f, 70f), d), // standing ~165° -> deep ~77°
                ankle = FigPoint(62f, 89f),
            )
        }
    }
}
