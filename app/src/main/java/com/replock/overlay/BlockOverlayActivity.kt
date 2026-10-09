package com.replock.overlay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.replock.domain.ExerciseMode
import com.replock.ui.components.ExerciseDemo
import com.replock.ui.components.RepCounterDisplay
import com.replock.ui.paywall.PaywallScreen
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepLockTheme
import com.replock.ui.theme.RepSurface
import com.replock.ui.theme.RepSurfaceVariant
import com.replock.util.Haptics
import com.replock.util.formatUnlockWindow
import kotlinx.coroutines.delay

/**
 * Full-screen lock shown over blocked apps. Shows the front camera, counts
 * pushups/squats with ML Kit pose detection, and dismisses once the rep target
 * is hit (granting a limited unlock window). TYPE_APPLICATION_OVERLAY-style
 * behaviour comes from the SYSTEM_ALERT_WINDOW grant — this is a normal
 * activity launched with FLAG_ACTIVITY_NEW_TASK on top of the blocked app.
 */
class BlockOverlayActivity : ComponentActivity() {

    private val viewModel: OverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).hide(
            WindowInsetsCompat.Type.systemBars()
        )
        setContent {
            RepLockTheme {
                OverlayRoute(viewModel = viewModel, onFinished = { finish() })
            }
        }
    }

    override fun onDestroy() {
        if (activeOverlayPackage == viewModel.packageName) activeOverlayPackage = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "com.replock.extra.PACKAGE_NAME"
        const val EXTRA_APP_NAME = "com.replock.extra.APP_NAME"

        /** Package the overlay is currently responsible for (prevents duplicate launches). */
        @Volatile
        var activeOverlayPackage: String? = null
    }
}

@Composable
private fun OverlayRoute(viewModel: OverlayViewModel, onFinished: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val target by viewModel.target.collectAsState()
    val reps by viewModel.reps.collectAsState()
    val pose by viewModel.pose.collectAsState()
    val exerciseMode by viewModel.exerciseMode.collectAsState()
    val windowMinutes by viewModel.unlockWindowMinutes.collectAsState()

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val analyzer = remember { PoseAnalyzer { result -> viewModel.onPoseResult(result) } }
    DisposableEffect(Unit) {
        onDispose { analyzer.close() }
    }

    // Haptic tick on every counted rep.
    LaunchedEffect(Unit) {
        viewModel.repHaptics.collect { Haptics.repTick(context) }
    }

    // When unlocked: celebrate, then dismiss the overlay so the blocked app is revealed.
    LaunchedEffect(state) {
        if (state == OverlayState.Unlocked) {
            Haptics.success(context)
            delay(1200)
            onFinished()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(onFrame = { imageProxy -> analyzer.analyze(imageProxy) })
            // Scrim: dim the camera feed top & bottom so the UI stays readable,
            // keep the middle (the user's body) visible.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.78f),
                            0.30f to Color.Black.copy(alpha = 0.10f),
                            0.62f to Color.Black.copy(alpha = 0.18f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        )
                    )
            )
            SkeletonCanvas(pose = pose, modifier = Modifier.fillMaxSize())

            if (state != OverlayState.Paywall) {
                Column(Modifier.fillMaxSize()) {
                    // Header: locked app + exercise chip
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = LockedRed, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${viewModel.appName} is locked",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            color = LockedRed.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = exerciseMode.label.uppercase(),
                                color = LockedRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Which angle should face the camera
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        CameraAngleGuide(exerciseMode)
                    }

                    Spacer(Modifier.weight(1f))

                    // Big rep counter
                    RepCounterDisplay(
                        reps = reps,
                        target = target,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Spacer(Modifier.weight(1f))

                    // Animated exercise demo
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DemoCard(exerciseMode)
                    }
                    Spacer(Modifier.height(8.dp))

                    // Contextual hint
                    Text(
                        text = when {
                            !pose.inFrame -> when (exerciseMode) {
                                ExerciseMode.Pushups ->
                                    "Get in frame — your upper body (side view) must be visible"
                                ExerciseMode.Squats ->
                                    "Get in frame — your legs (side view) must be visible"
                                ExerciseMode.Both ->
                                    "Get in frame — your full body (side view) must be visible"
                            }
                            !pose.formOk -> when (exerciseMode) {
                                ExerciseMode.Pushups -> "Use BOTH arms — keep them symmetric"
                                ExerciseMode.Squats -> "Use BOTH legs — keep them symmetric"
                                ExerciseMode.Both -> "Keep both sides symmetric"
                            }
                            else -> when (exerciseMode) {
                                ExerciseMode.Pushups ->
                                    "Do $target pushups. Straight arms → chest down → straight arms."
                                ExerciseMode.Squats ->
                                    "Do $target squats. Stand tall → thighs parallel → stand tall."
                                ExerciseMode.Both ->
                                    "Do $target reps — pushups or squats. Watch the demo."
                            }
                        },
                        color = if (pose.inFrame && pose.formOk) RepGray else LockedRed,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Camera permission needed",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    )
                    context.startActivity(intent)
                }) {
                    Text("Open settings")
                }
            }
        }

        if (state == OverlayState.Paywall) {
            PaywallScreen(
                onSubscribed = { viewModel.purchasePro() },
                onDismiss = onFinished,
            )
        }

        if (state == OverlayState.Unlocked) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f))) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.LockOpen,
                        null,
                        tint = ElectricGreen,
                        modifier = Modifier.size(72.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "UNLOCKED",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = ElectricGreen,
                    )
                    Text(
                        "${viewModel.appName} is open for ${formatUnlockWindow(windowMinutes)}",
                        color = RepGray,
                    )
                }
            }
        }
    }
}

/** Compact card explaining which body angle should face the camera, with a schematic. */
@Composable
private fun CameraAngleGuide(mode: ExerciseMode) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RepSurfaceVariant.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                // The standing figure shows the full body — the right schematic for "full body in frame".
                val figureMode = if (mode == ExerciseMode.Both) ExerciseMode.Squats else mode
                ExerciseDemo(figureMode, Modifier.size(60.dp), depth = 0f, showPhone = true)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "CAMERA ANGLE · SIDE VIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricGreen,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = when (mode) {
                        ExerciseMode.Pushups ->
                            "Plank side-on: your profile faces the camera. " +
                                "Phone on the floor at head height."
                        ExerciseMode.Squats ->
                            "Stand side-on: your profile faces the camera, " +
                                "full body in frame."
                        ExerciseMode.Both ->
                            "Side view for both: full body in frame, profile facing the camera."
                    },
                    fontSize = 11.sp,
                    color = RepGray,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

/** Collapsible card with a looping animated demo of the selected exercise. */
@Composable
private fun DemoCard(mode: ExerciseMode) {
    var visible by remember { mutableStateOf(true) }
    Card(
        colors = CardDefaults.cardColors(containerColor = RepSurface.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("DEMO — WATCH THE ANGLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RepGray)
                IconButton(onClick = { visible = !visible }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Toggle demo",
                        tint = RepGray,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (visible) {
                if (mode == ExerciseMode.Both) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ExerciseDemo(ExerciseMode.Pushups, Modifier.size(64.dp))
                            Text("Pushup", fontSize = 10.sp, color = RepGray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            ExerciseDemo(ExerciseMode.Squats, Modifier.size(64.dp))
                            Text("Squat", fontSize = 10.sp, color = RepGray)
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ExerciseDemo(mode, Modifier.size(96.dp))
                        Text(mode.label, fontSize = 10.sp, color = RepGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(onFrame: (ImageProxy) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    LaunchedEffect(lifecycleOwner) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).await()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            onFrame(imageProxy)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis,
            )
        } catch (e: Exception) {
            // Camera unavailable (in use by another app, hardware missing, ...)
        }
    }
}

@Composable
private fun SkeletonCanvas(pose: PoseUiState, modifier: Modifier = Modifier) {
    val color = if (pose.inFrame && pose.formOk) ElectricGreen else LockedRed
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun pt(o: PoseAnalyzer.Point) = Offset((1f - o.x) * w, o.y * h)
        pose.skeleton.forEach { (a, b) ->
            // PreviewView mirrors the front-camera preview, so flip x to match.
            val start = pt(a)
            val end = pt(b)
            drawLine(color.copy(alpha = 0.12f), start, end, strokeWidth = 18f, cap = StrokeCap.Round)
            drawLine(color.copy(alpha = 0.20f), start, end, strokeWidth = 12f, cap = StrokeCap.Round)
            drawLine(
                brush = Brush.linearGradient(listOf(color.copy(alpha = 0.6f), color), start, end),
                start = start,
                end = end,
                strokeWidth = 8f,
                cap = StrokeCap.Round,
            )
        }
        // Joint dots at every unique endpoint.
        val seen = HashSet<String>()
        pose.skeleton.forEach { (a, b) ->
            listOf(a, b).forEach { o ->
                val key = "${(o.x * 1000).toInt()}_${(o.y * 1000).toInt()}"
                if (seen.add(key)) {
                    val c = pt(o)
                    drawCircle(color.copy(alpha = 0.25f), radius = 9f, center = c)
                    drawCircle(Color.White.copy(alpha = 0.85f), radius = 4.5f, center = c)
                }
            }
        }
    }
}
