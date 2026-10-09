package com.replock.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.ui.components.GlassCard
import com.replock.ui.components.MiniRing
import com.replock.ui.components.PillBadge
import com.replock.ui.components.StatCard
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.util.PermissionUtils
import com.replock.util.formatUnlockWindow

/** Gold used for the PRO badge (matches the premium accent in the design). */
private val ProGold = Color(0xFFFFC857)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenAppPicker: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenStats: () -> Unit = {},
) {
    val context = LocalContext.current
    val blockingEnabled by viewModel.blockingEnabled.collectAsState()
    val repsToday by viewModel.repsToday.collectAsState()
    val unlocksToday by viewModel.unlocksToday.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val blockedCount by viewModel.enabledBlockedCount.collectAsState()
    val windowMinutes by viewModel.unlockWindowMinutes.collectAsState()

    val hasCamera = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    val hasUsage = PermissionUtils.hasUsageStatsAccess(context)
    val hasOverlay = PermissionUtils.hasOverlayPermission(context)
    val hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header: wordmark + plan badge
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "REPLOCK",
                color = ElectricGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            if (isPro) {
                PillBadge("PRO", ProGold)
            } else {
                PillBadge("FREE PLAN", RepGray)
            }
        }

        // Status card: big on/off state + switch
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (blockingEnabled) "BLOCKING ACTIVE" else "BLOCKING PAUSED",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (blockingEnabled) ElectricGreen else LockedRed,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (blockingEnabled) "Blocked apps are locked" else "Blocked apps are open",
                        color = RepGray,
                        fontSize = 14.sp,
                    )
                }
                Switch(
                    checked = blockingEnabled,
                    onCheckedChange = { viewModel.setBlockingEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RepBlack,
                        checkedTrackColor = ElectricGreen,
                        uncheckedTrackColor = Color(0xFF2A2A2A),
                    ),
                )
            }
        }

        // Permissions with status pills
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Permissions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(10.dp))
                PermissionRow("Camera", hasCamera)
                PermissionRow("Usage access", hasUsage)
                PermissionRow("Draw over apps", hasOverlay)
                PermissionRow("Notifications", hasNotifications)
            }
        }

        // Today: reps + unlocks (with a ring showing free-tier usage)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard("Reps today", "$repsToday", Modifier.weight(1f))
            GlassCard(Modifier.weight(1f)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isPro) "$unlocksToday" else "$unlocksToday/3",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = ElectricGreen,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(if (isPro) "Unlocks (Pro)" else "Unlocks", color = RepGray, fontSize = 13.sp)
                    }
                    MiniRing(
                        progress = if (isPro) 1f else unlocksToday / 3f,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        // Blocklist + window
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("Blocked apps", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$blockedCount app(s) on your blocklist · unlock for ${formatUnlockWindow(windowMinutes)}",
                    color = RepGray,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onOpenAppPicker,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricGreen,
                        contentColor = RepBlack,
                    ),
                ) {
                    Icon(Icons.Filled.Block, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage blocklist", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // How to use
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("How it works", fontWeight = FontWeight.Bold, color = ElectricGreen, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "1. Add apps to your blocklist.\n" +
                        "2. Open a blocked app — RepLock locks it.\n" +
                        "3. Do your reps in front of the front camera.\n" +
                        "4. The app unlocks for ${formatUnlockWindow(windowMinutes)}, then locks again.",
                    color = RepGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedButton(
                onClick = onOpenStats,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
            ) {
                Text("Stats")
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(50),
            ) {
                Text("Settings")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
            null,
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
