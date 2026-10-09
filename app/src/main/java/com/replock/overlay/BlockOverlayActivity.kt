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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.replock.ui.components.RepCounterDisplay
import com.replock.ui.paywall.PaywallScreen
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepLockTheme
import com.replock.util.Haptics
import kotlinx.coroutines.delay

/**
 * Full-screen lock shown over blocked apps. Shows the front camera, counts
 * pushups with ML Kit pose detection, and dismisses once the rep target is
 * hit (granting a limited unlock window). TYPE_APPLICATION_OVERLAY-style
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
            SkeletonCanvas(pose = pose, modifier = Modifier.fillMaxSize())

            if (state != OverlayState.Paywall) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = LockedRed, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${viewModel.appName} is locked",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    RepCounterDisplay(
                        reps = reps,
                        target = target,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = when {
                            !pose.inFrame -> "Get in frame — put your phone down so the camera can see your whole body"
                            !pose.formOk -> "Use BOTH arms — keep them symmetric"
                            else -> "Do $target pushups. Straight arms → chest down → straight arms."
                        },
                        color = if (pose.inFrame && pose.formOk) RepGray else LockedRed,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                    )
                    Spacer(Modifier.height(48.dp))
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
                        "${viewModel.appName} is open for a limited window",
                        color = RepGray,
                    )
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
        pose.skeleton.forEach { (a, b) ->
            // PreviewView mirrors the front-camera preview, so flip x to match.
            drawLine(
                color = color,
                start = Offset((1f - a.x) * w, a.y * h),
                end = Offset((1f - b.x) * w, b.y * h),
                strokeWidth = 8f,
                cap = StrokeCap.Round,
            )
        }
    }
}
