package com.replock.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.replock.data.SettingsDataStore
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurfaceVariant
import com.replock.util.PermissionUtils
import kotlinx.coroutines.launch

private enum class OnboardingStep {
    Welcome, Camera, UsageStats, Overlay, Notifications, Done,
}

/**
 * Explains the mechanic, then requests permissions in sequence, each with a
 * clear rationale: Camera → Usage Stats (special) → Overlay (special) →
 * Notifications.
 */
@Composable
fun OnboardingScreen(settings: SettingsDataStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(OnboardingStep.Welcome) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasUsageStats by remember { mutableStateOf(PermissionUtils.hasUsageStatsAccess(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionUtils.hasOverlayPermission(context)) }
    var hasNotifications by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // Re-check special permissions every time we come back to the foreground
    // (i.e. after the user toggles them in system settings).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = PermissionUtils.hasUsageStatsAccess(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                hasNotifications =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifications = granted }

    fun openAppSettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
        )
    }

    Surface(color = RepBlack) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Icon(
                Icons.Filled.FitnessCenter,
                null,
                tint = ElectricGreen,
                modifier = Modifier.size(64.dp),
            )
            Text("RepLock", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Earn your screen time. Literally.", fontSize = 16.sp, color = RepGray)
            Spacer(Modifier.height(32.dp))

            AnimatedContent(targetState = step, label = "onboarding") { current ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (current) {
                        OnboardingStep.Welcome -> WelcomeStep(onNext = { step = OnboardingStep.Camera })

                        OnboardingStep.Camera -> PermissionStep(
                            icon = Icons.Filled.Videocam,
                            title = "Camera access",
                            rationale = "RepLock uses the front camera to count your pushups in real time. " +
                                "Video never leaves your phone — pose detection runs entirely on-device.",
                            granted = hasCameraPermission,
                            buttonLabel = "Grant camera access",
                            onGrant = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                            onOpenAppSettings = ::openAppSettings,
                            onNext = { step = OnboardingStep.UsageStats },
                        )

                        OnboardingStep.UsageStats -> SpecialPermissionStep(
                            icon = Icons.Filled.Visibility,
                            title = "Usage access",
                            rationale = "RepLock needs Usage Stats access to detect when you open a " +
                                "blocked app. This is how the lock screen knows when to appear.",
                            granted = hasUsageStats,
                            buttonLabel = "Open usage access settings",
                            onOpen = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            onNext = { step = OnboardingStep.Overlay },
                        )

                        OnboardingStep.Overlay -> SpecialPermissionStep(
                            icon = Icons.Filled.Layers,
                            title = "Display over other apps",
                            rationale = "RepLock draws a full-screen lock over blocked apps. " +
                                "Android requires the \"Display over other apps\" permission for this.",
                            granted = hasOverlay,
                            buttonLabel = "Open overlay settings",
                            onOpen = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                )
                            },
                            onNext = {
                                step = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    OnboardingStep.Notifications
                                } else {
                                    OnboardingStep.Done
                                }
                            },
                        )

                        OnboardingStep.Notifications -> PermissionStep(
                            icon = Icons.Filled.Notifications,
                            title = "Notifications",
                            rationale = "RepLock shows a persistent notification while blocking is " +
                                "active, so Android doesn't kill the service.",
                            granted = hasNotifications,
                            buttonLabel = "Grant notification access",
                            onGrant = {
                                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onOpenAppSettings = ::openAppSettings,
                            onNext = { step = OnboardingStep.Done },
                            canSkip = true,
                        )

                        OnboardingStep.Done -> DoneStep(
                            allGranted = hasCameraPermission && hasUsageStats && hasOverlay,
                            onFinish = {
                                scope.launch { settings.setOnboardingDone(true) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Text("How it works", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(Modifier.height(16.dp))
    listOf(
        "1" to "Pick the apps that steal your time.",
        "2" to "When you open one, RepLock locks it.",
        "3" to "Do your pushups in front of the camera.",
        "4" to "The app unlocks for a few minutes. Then it locks again.",
    ).forEach { (n, text) ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(n, color = ElectricGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text(text, color = RepGray, fontSize = 16.sp)
        }
    }
    Spacer(Modifier.height(32.dp))
    BigButton(label = "Get started", onClick = onNext)
}

@Composable
private fun PermissionStep(
    icon: ImageVector,
    title: String,
    rationale: String,
    granted: Boolean,
    buttonLabel: String,
    onGrant: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onNext: () -> Unit,
    canSkip: Boolean = false,
) {
    PermissionStepLayout(icon, title, rationale) {
        BigButton(
            label = if (granted) "Granted ✓" else buttonLabel,
            onClick = onGrant,
            enabled = !granted,
        )
        if (!granted) {
            TextButton(onClick = onOpenAppSettings) {
                Text("Open app settings", color = RepGray)
            }
        }
        Spacer(Modifier.height(8.dp))
        BigButton(
            label = "Continue",
            onClick = onNext,
            enabled = granted,
            color = if (granted) ElectricGreen else RepSurfaceVariant,
        )
        if (canSkip) {
            TextButton(onClick = onNext) { Text("Skip for now", color = RepGray) }
        }
    }
}

@Composable
private fun SpecialPermissionStep(
    icon: ImageVector,
    title: String,
    rationale: String,
    granted: Boolean,
    buttonLabel: String,
    onOpen: () -> Unit,
    onNext: () -> Unit,
) {
    PermissionStepLayout(icon, title, rationale) {
        BigButton(
            label = if (granted) "Granted ✓" else buttonLabel,
            onClick = onOpen,
            enabled = !granted,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "After toggling it on in system settings, come back here.",
            color = RepGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        BigButton(
            label = "Continue",
            onClick = onNext,
            enabled = granted,
            color = if (granted) ElectricGreen else RepSurfaceVariant,
        )
    }
}

@Composable
private fun PermissionStepLayout(
    icon: ImageVector,
    title: String,
    rationale: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Icon(icon, null, tint = ElectricGreen, modifier = Modifier.size(56.dp))
    Spacer(Modifier.height(16.dp))
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
    Spacer(Modifier.height(12.dp))
    Text(rationale, color = RepGray, fontSize = 15.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    content()
}

@Composable
private fun DoneStep(allGranted: Boolean, onFinish: () -> Unit) {
    Icon(
        if (allGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
        null,
        tint = if (allGranted) ElectricGreen else LockedRedColor,
        modifier = Modifier.size(64.dp),
    )
    Spacer(Modifier.height(16.dp))
    Text(
        if (allGranted) "You're all set" else "Almost there",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        if (allGranted) "Next: pick the apps you want to block."
        else "Some permissions are still missing — you can fix them later in Settings.",
        color = RepGray,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    BigButton(label = "Start blocking", onClick = onFinish)
}

private val LockedRedColor: Color
    @Composable get() = com.replock.ui.theme.LockedRed

@Composable
private fun BigButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = ElectricGreen,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == ElectricGreen) RepBlack else Color.White,
            disabledContainerColor = RepSurfaceVariant,
            disabledContentColor = RepGray,
        ),
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
