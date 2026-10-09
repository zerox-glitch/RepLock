package com.replock.ui.onboarding

import androidx.compose.material.icons.filled.FitnessCenter
import com.replock.ui.components.HourglassHero
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.replock.data.SettingsDataStore
import com.replock.domain.ExerciseMode
import com.replock.service.LockMonitorService
import com.replock.ui.components.ExerciseDemo
import com.replock.ui.components.GlassBorder
import com.replock.ui.components.GlassCard
import com.replock.ui.components.GlassSurface
import com.replock.ui.components.PillBadge
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.PremiumGold
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.util.PermissionUtils
import kotlinx.coroutines.launch

private enum class OnboardingStep {
    Welcome, Camera, UsageStats, Overlay, Notifications, Done,
}

/**
 * First-run flow: a short value pitch, then one screen per permission (camera,
 * usage access, overlay, notifications) explaining exactly why it's needed,
 * then a checklist and "Start blocking". Permission state is re-read whenever
 * the user returns from system settings.
 */
@Composable
fun OnboardingScreen(settings: SettingsDataStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val needsNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val steps = remember(needsNotifications) {
        OnboardingStep.entries.filter { needsNotifications || it != OnboardingStep.Notifications }
    }
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
            !needsNotifications ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // Re-check special permissions whenever we come back to the app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                hasUsageStats = PermissionUtils.hasUsageStatsAccess(context)
                hasOverlay = PermissionUtils.hasOverlayPermission(context)
                hasNotifications = !needsNotifications ||
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

    val index = steps.indexOf(step).coerceAtLeast(0)
    val nextAfter: (OnboardingStep) -> OnboardingStep = { current ->
        steps.getOrNull(steps.indexOf(current) + 1) ?: OnboardingStep.Done
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A2016), RepBlack),
                    startY = 0f,
                    endY = 1400f,
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            val previous = steps.getOrNull(index - 1)
            ProgressHeader(
                index = index,
                total = steps.size,
                onBack = previous?.let { prev -> { step = prev } },
            )

            AnimatedContent(
                targetState = step,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    (slideInHorizontally(animationSpec = tween(380)) { it / 6 } +
                        fadeIn(animationSpec = tween(380))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(380)) { -it / 6 } +
                            fadeOut(animationSpec = tween(200)))
                },
                label = "onboarding",
            ) { current ->
                run {
                    when (current) {
                        OnboardingStep.Welcome -> WelcomePage(
                            onNext = { step = nextAfter(OnboardingStep.Welcome) },
                        )

                        OnboardingStep.Camera -> PermissionPage(
                            icon = Icons.Filled.Videocam,
                            title = "Camera access",
                            subtitle = "RepLock uses the front camera to count your reps. " +
                                "Frames are analysed on your phone and never uploaded.",
                            bullets = listOf(
                                "Counts pushups and squats in real time",
                                "Works best with your side profile to the camera",
                                "Nothing is recorded or stored",
                            ),
                            granted = hasCameraPermission,
                            primaryLabel = "Allow camera",
                            onPrimary = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                            secondaryLabel = "Open app settings",
                            onSecondary = ::openAppSettings,
                            nextLabel = "Continue",
                            nextEnabled = hasCameraPermission,
                            onNext = { step = nextAfter(OnboardingStep.Camera) },
                        )

                        OnboardingStep.UsageStats -> PermissionPage(
                            icon = Icons.Filled.Visibility,
                            title = "Usage access",
                            subtitle = "Lets RepLock see which app is in front, so it knows when " +
                                "to show the lock. It can't read what happens inside apps.",
                            bullets = listOf(
                                "Detects when a blocked app opens",
                                "Re-locks the moment your unlock window ends",
                                "Reads only app open and close events",
                            ),
                            granted = hasUsageStats,
                            primaryLabel = "Open usage access",
                            onPrimary = {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            },
                            footnote = "Turn RepLock on in the list, then come back here.",
                            nextLabel = "Continue",
                            nextEnabled = hasUsageStats,
                            onNext = { step = nextAfter(OnboardingStep.UsageStats) },
                        )

                        OnboardingStep.Overlay -> PermissionPage(
                            icon = Icons.Filled.Layers,
                            title = "Display over other apps",
                            subtitle = "Needed to draw the full-screen lock on top of the apps " +
                                "you chose to block.",
                            bullets = listOf(
                                "Shows your rep counter over the blocked app",
                                "Only appears for apps on your blocklist",
                            ),
                            granted = hasOverlay,
                            primaryLabel = "Open overlay settings",
                            onPrimary = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}"),
                                    )
                                )
                            },
                            footnote = "Switch on \"Allow display over other apps\", then come back.",
                            nextLabel = "Continue",
                            nextEnabled = hasOverlay,
                            onNext = { step = nextAfter(OnboardingStep.Overlay) },
                        )

                        OnboardingStep.Notifications -> PermissionPage(
                            icon = Icons.Filled.Notifications,
                            title = "Notifications",
                            subtitle = "A quiet, persistent notification keeps the blocker running " +
                                "so Android doesn't shut it down in the background.",
                            bullets = listOf(
                                "Shows a \"Blocking active\" status",
                                "No marketing, no spam",
                            ),
                            granted = hasNotifications,
                            primaryLabel = "Allow notifications",
                            onPrimary = {
                                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            nextLabel = if (hasNotifications) "Continue" else "Skip for now",
                            nextEnabled = true,
                            onNext = { step = nextAfter(OnboardingStep.Notifications) },
                        )

                        OnboardingStep.Done -> DonePage(
                            cameraOk = hasCameraPermission,
                            usageOk = hasUsageStats,
                            overlayOk = hasOverlay,
                            notificationsOk = hasNotifications,
                            notificationsRequired = needsNotifications,
                            onFinish = {
                                scope.launch {
                                    settings.setOnboardingDone(true)
                                    // Start blocking right away so the service is
                                    // running the moment the user lands on Home.
                                    if (PermissionUtils.hasAllBlockingPermissions(context)) {
                                        ContextCompat.startForegroundService(
                                            context,
                                            Intent(context, LockMonitorService::class.java),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pages
// ---------------------------------------------------------------------------

@Composable
private fun ProgressHeader(index: Int, total: Int, onBack: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = RepGray)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Row(
            Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (i <= index) ElectricGreen else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(50),
                        )
                ) {}
            }
        }
        Text(
            "${index + 1}/$total",
            color = RepGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Standard page layout: the body scrolls if it's taller than the screen, and
 * the action buttons are pinned to the bottom so they're always reachable.
 */
@Composable
private fun OnboardingPage(
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            body()
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            actions()
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    OnboardingPage(
        body = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(
                    "EARN YOUR",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "SCREEN TIME",
                    color = ElectricGreen,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 44.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Less distractions.",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "More progress.",
                    color = RepGray,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            HourglassHero(Modifier.fillMaxWidth().height(260.dp))
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StepBadge("1", "Pick your apps", Icons.Filled.Lock)
                StepBadge("2", "Do your reps", Icons.Filled.FitnessCenter)
                StepBadge("3", "Earn screen time", Icons.Filled.LockOpen)
            }
        },
        actions = {
            BigButton(label = "Get Started  \u2192", onClick = onNext)
            Spacer(Modifier.height(10.dp))
            Text(
                "Your focus = Your power",
                color = RepGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        },
    )
}

@Composable
private fun StepBadge(index: String, label: String, icon: ImageVector) {
    Column(
        Modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(58.dp)
                .background(ElectricGreen.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, ElectricGreen.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "$index. $label",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionPage(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bullets: List<String>,
    granted: Boolean,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String,
    nextEnabled: Boolean,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    footnote: String? = null,
) {
    val accent = if (granted) ElectricGreen else PremiumGold
    OnboardingPage(
        body = {
            GlowHero(size = 170.dp, color = accent) {
                Box(
                    Modifier
                        .size(100.dp)
                        .background(GlassSurface, CircleShape)
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            PillBadge(
                text = if (granted) "GRANTED" else "NEEDS ACCESS",
                color = accent,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = RepGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    bullets.forEach { BulletRow(it) }
                }
            }
        },
        actions = {
            if (footnote != null && !granted) {
                Text(
                    footnote,
                    color = RepGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
            }
            BigButton(
                label = if (granted) "Granted" else primaryLabel,
                onClick = onPrimary,
                enabled = !granted,
            )
            if (!granted && secondaryLabel != null && onSecondary != null) {
                TextButton(onClick = onSecondary) {
                    Text(secondaryLabel, color = RepGray)
                }
            }
            Spacer(Modifier.height(8.dp))
            BigButton(
                label = nextLabel,
                onClick = onNext,
                enabled = nextEnabled,
                color = if (nextEnabled && granted) ElectricGreen else Color.White.copy(alpha = 0.10f),
                contentColor = if (nextEnabled && granted) RepBlack else Color.White,
            )
        },
    )
}

@Composable
private fun DonePage(
    cameraOk: Boolean,
    usageOk: Boolean,
    overlayOk: Boolean,
    notificationsOk: Boolean,
    notificationsRequired: Boolean,
    onFinish: () -> Unit,
) {
    val allRequiredGranted = cameraOk && usageOk && overlayOk
    val accent = if (allRequiredGranted) ElectricGreen else PremiumGold
    OnboardingPage(
        body = {
            GlowHero(size = 170.dp, color = accent) {
                Icon(
                    if (allRequiredGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(84.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (allRequiredGranted) "You're all set" else "Almost there",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (allRequiredGranted) {
                    "Next, pick the apps you want to lock. Blocking starts as soon as you finish."
                } else {
                    "Some permissions are still missing. Blocking won't work until they're on. " +
                        "You can fix them later in Settings."
                },
                color = RepGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    ChecklistRow("Camera", cameraOk)
                    ChecklistRow("Usage access", usageOk)
                    ChecklistRow("Display over apps", overlayOk)
                    if (notificationsRequired) ChecklistRow("Notifications", notificationsOk)
                }
            }
        },
        actions = {
            BigButton(label = "Start blocking", onClick = onFinish)
        },
    )
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

/** Soft radial glow behind a hero element. */
@Composable
private fun GlowHero(
    size: Dp,
    color: Color = ElectricGreen,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset.Unspecified,
                    radius = 420f,
                ),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = RepGray, fontSize = 13.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = ElectricGreen,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 1.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun ChecklistRow(label: String, granted: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (granted) ElectricGreen else LockedRed,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        PillBadge(
            text = if (granted) "ACTIVE" else "MISSING",
            color = if (granted) ElectricGreen else LockedRed,
        )
    }
}

@Composable
private fun BigButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = ElectricGreen,
    contentColor: Color = RepBlack,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = Color.White.copy(alpha = 0.08f),
            disabledContentColor = RepGray,
        ),
    ) {
        Text(label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
