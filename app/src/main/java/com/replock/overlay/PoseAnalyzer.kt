package com.replock.overlay

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Thin wrapper around ML Kit Pose Detection running in STREAM_MODE.
 *
 * Battery saving: only every [sampleEveryNthFrame]-th camera frame is sent to
 * ML Kit, and a frame is dropped while a previous detection is still in flight.
 *
 * Video frames never leave the device — pose detection runs fully on-device.
 */
class PoseAnalyzer(
    private val sampleEveryNthFrame: Int = 3,
    private val onResult: (PoseFrameResult) -> Unit,
) {
    /** Normalized (0..1) point in upright-image space. */
    data class Point(val x: Float, val y: Float)

    data class PoseFrameResult(
        val inFrame: Boolean,
        val formOk: Boolean,
        val leftElbowAngle: Float,
        val rightElbowAngle: Float,
        val skeleton: List<Pair<Point, Point>>,
    )

    private val detector: PoseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private val isProcessing = AtomicBoolean(false)
    private var frameIndex = 0

    @SuppressLint("UnsafeOptInUsageError")
    fun analyze(imageProxy: ImageProxy) {
        frameIndex++
        if (frameIndex % sampleEveryNthFrame != 0 || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            isProcessing.set(false)
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { pose -> handlePose(pose) }
            .addOnFailureListener { /* transient detection failure: drop the frame */ }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    private fun handlePose(pose: Pose) {
        fun landmark(type: Int): PoseLandmark? = pose.getPoseLandmark(type)

        val requiredTypes = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
        )
        val outOfFrame = requiredTypes.any { landmark(it)?.inFrameLikelihood ?: 0f < MIN_CONFIDENCE }
        if (outOfFrame) {
            onResult(
                PoseFrameResult(
                    inFrame = false,
                    formOk = false,
                    leftElbowAngle = 0f,
                    rightElbowAngle = 0f,
                    skeleton = skeletonEdges(pose),
                )
            )
            return
        }

        val leftAngle = elbowAngle(
            landmark(PoseLandmark.LEFT_SHOULDER)!!,
            landmark(PoseLandmark.LEFT_ELBOW)!!,
            landmark(PoseLandmark.LEFT_WRIST)!!,
        )
        val rightAngle = elbowAngle(
            landmark(PoseLandmark.RIGHT_SHOULDER)!!,
            landmark(PoseLandmark.RIGHT_ELBOW)!!,
            landmark(PoseLandmark.RIGHT_WRIST)!!,
        )

        onResult(
            PoseFrameResult(
                inFrame = true,
                formOk = abs(leftAngle - rightAngle) <= MAX_ASYMMETRY_DEG,
                leftElbowAngle = leftAngle,
                rightElbowAngle = rightAngle,
                skeleton = skeletonEdges(pose),
            )
        )
    }

    /** Angle at the elbow formed by shoulder–elbow–wrist, in degrees (0..180). */
    private fun elbowAngle(shoulder: PoseLandmark, elbow: PoseLandmark, wrist: PoseLandmark): Float {
        val radians = atan2(wrist.position.y - elbow.position.y, wrist.position.x - elbow.position.x) -
            atan2(shoulder.position.y - elbow.position.y, shoulder.position.x - elbow.position.x)
        var angle = abs(radians * 180.0 / Math.PI).toFloat()
        if (angle > 180f) angle = 360f - angle
        return angle
    }

    private fun skeletonEdges(pose: Pose): List<Pair<Point, Point>> {
        val connections = listOf(
            PoseLandmark.NOSE to PoseLandmark.LEFT_EYE,
            PoseLandmark.NOSE to PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EYE to PoseLandmark.LEFT_EAR,
            PoseLandmark.RIGHT_EYE to PoseLandmark.RIGHT_EAR,
            PoseLandmark.LEFT_EAR to PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_EAR to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_PINKY,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_INDEX,
            PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_THUMB,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_PINKY,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_INDEX,
            PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_THUMB,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
            PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_HEEL,
            PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_HEEL,
            PoseLandmark.LEFT_HEEL to PoseLandmark.LEFT_FOOT_INDEX,
            PoseLandmark.RIGHT_HEEL to PoseLandmark.RIGHT_FOOT_INDEX,
        )
        return connections.mapNotNull { (a, b) ->
            val la = pose.getPoseLandmark(a) ?: return@mapNotNull null
            val lb = pose.getPoseLandmark(b) ?: return@mapNotNull null
            if (la.inFrameLikelihood < MIN_CONFIDENCE || lb.inFrameLikelihood < MIN_CONFIDENCE) {
                return@mapNotNull null
            }
            // PoseLandmark.position is a PointF3D with normalized x/y (0..1).
            Point(la.position.x, la.position.y) to Point(lb.position.x, lb.position.y)
        }
    }

    fun close() = detector.close()

    companion object {
        private const val MIN_CONFIDENCE = 0.5f
        private const val MAX_ASYMMETRY_DEG = 30f
    }
}
