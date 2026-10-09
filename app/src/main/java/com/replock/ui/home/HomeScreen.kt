package com.replock.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.replock.ui.components.StatCard
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurface
import com.replock.ui.theme.RepSurfaceVariant
import com.replock.util.PermissionUtils

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Status card
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (blockingEnabled) "BLOCKING ACTIVE" else "BLOCKING PAUSED",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (blockingEnabled) ElectricGreen else LockedRed,
                    )
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
                        checkedThumbColor = ElectricGreen,
                        checkedTrackColor = ElectricGreen.copy(alpha = 0.5f),
                    ),
                )
            }
        }

        // Permissions
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Permissions", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                PermissionChip("Camera", hasCamera)
                PermissionChip("Usage access", hasUsage)
                PermissionChip("Draw over apps", hasOverlay)
                PermissionChip("Notifications", hasNotifications)
            }
        }

        // Today
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Reps today", "$repsToday", Modifier.weight(1f))
            StatCard(
                "Unlocks",
                if (isPro) "$unlocksToday ∞" else "$unlocksToday / 3",
                Modifier.weight(1f),
            )
        }

        // Blocklist
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Blocked apps", fontWeight = FontWeight.Bold, color = Color.White)
                Text("$blockedCount app(s) on your blocklist", color = RepGray)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenAppPicker, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Block, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manage blocklist")
                }
            }
        }

        // How to use
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = RepSurfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("How to use", fontWeight = FontWeight.Bold, color = ElectricGreen)
                Spacer(Modifier.height(4.dp))
                Text(
                    "1. Add apps to your blocklist.\n" +
                        "2. Open a blocked app — RepLock locks it.\n" +
                        "3. Do your pushups in front of the front camera.\n" +
                        "4. The app unlocks for a few minutes, then locks again.",
                    color = RepGray,
                    fontSize = 14.sp,
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onOpenStats, modifier = Modifier.weight(1f)) {
                Text("Stats")
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text("Settings")
            }
        }
    }
}

@Composable
private fun PermissionChip(label: String, granted: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Filled.Error,
            null,
            tint = if (granted) ElectricGreen else LockedRed,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Text(
            if (granted) "OK" else "Missing",
            color = if (granted) ElectricGreen else LockedRed,
            fontSize = 12.sp,
        )
    }
}
