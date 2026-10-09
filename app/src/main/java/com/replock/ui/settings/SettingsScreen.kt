package com.replock.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.data.SettingsDataStore
import com.replock.ui.components.GlassCard
import com.replock.ui.components.GlassSurface
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.PremiumGold
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepWhite
import com.replock.util.formatUnlockWindow
import kotlin.math.abs
import kotlin.math.roundToInt

/** Unlock window presets (minutes), 1 minute up to the 3-hour maximum. */
private val WINDOW_PRESETS = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180)

private const val REP_TARGET_MIN = 5
private const val REP_TARGET_MAX = 50
private const val REP_TARGET_STEP = 5

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToPaywall: () -> Unit = {},
) {
    val repTarget by viewModel.repTarget.collectAsState()
    val windowMinutes by viewModel.unlockWindowMinutes.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val context = LocalContext.current

    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggle(key: String) {
        expanded = if (expanded == key) null else key
    }

    val exerciseLabel = when (difficulty) {
        SettingsDataStore.DIFFICULTY_SQUATS -> "Squats"
        SettingsDataStore.DIFFICULTY_BOTH -> "Both"
        else -> "Pushups"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RepBlack),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Settings", color = RepWhite, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ElectricGreen)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("RepLock Athlete", color = RepWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (isPro) "Pro member" else "Free plan",
                            color = if (isPro) PremiumGold else RepGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (!isPro) {
                        Box(
                            Modifier
                                .background(PremiumGold, RoundedCornerShape(50))
                                .clickable(onClick = onNavigateToPaywall)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("Upgrade", color = RepBlack, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            SettingRow(
                icon = Icons.Filled.FitnessCenter,
                title = "Rep target",
                value = "$repTarget reps",
                expanded = expanded == "rep",
                onToggle = { toggle("rep") },
            ) {
                Slider(
                    value = repTarget.toFloat(),
                    onValueChange = { raw ->
                        val snapped = (raw / REP_TARGET_STEP).roundToInt() * REP_TARGET_STEP
                        viewModel.setRepTarget(snapped.coerceIn(REP_TARGET_MIN, REP_TARGET_MAX))
                    },
                    valueRange = REP_TARGET_MIN.toFloat()..REP_TARGET_MAX.toFloat(),
                    steps = (REP_TARGET_MAX - REP_TARGET_MIN) / REP_TARGET_STEP - 1,
                    colors = sliderColors(),
                )
                Text(
                    "Reps you must finish before an app unlocks.",
                    color = RepGray,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            SettingRow(
                icon = Icons.Filled.Timer,
                title = "Unlock window",
                value = formatUnlockWindow(windowMinutes),
                expanded = expanded == "window",
                onToggle = { toggle("window") },
            ) {
                val windowIndex = WINDOW_PRESETS.indices
                    .minByOrNull { abs(WINDOW_PRESETS[it] - windowMinutes) } ?: 0
                Slider(
                    value = windowIndex.toFloat(),
                    onValueChange = { raw ->
                        val index = raw.roundToInt().coerceIn(0, WINDOW_PRESETS.size - 1)
                        viewModel.setUnlockWindowMinutes(WINDOW_PRESETS[index])
                    },
                    valueRange = 0f..(WINDOW_PRESETS.size - 1).toFloat(),
                    steps = WINDOW_PRESETS.size - 2,
                    colors = sliderColors(),
                )
                Text(
                    "How long an app stays open after you finish the reps. Maximum 3 hours.",
                    color = RepGray,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            SettingRow(
                icon = Icons.Filled.DirectionsRun,
                title = "Exercise",
                value = exerciseLabel,
                expanded = expanded == "exercise",
                onToggle = { toggle("exercise") },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Pushups" to SettingsDataStore.DIFFICULTY_PUSHUPS,
                        "Squats" to SettingsDataStore.DIFFICULTY_SQUATS,
                        "Both" to SettingsDataStore.DIFFICULTY_BOTH,
                    ).forEach { (label, value) ->
                        val selected = difficulty == value
                        Box(
                            Modifier
                                .weight(1f)
                                .background(
                                    if (selected) ElectricGreen else GlassSurface,
                                    RoundedCornerShape(50),
                                )
                                .clickable { viewModel.setDifficulty(value) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                color = if (selected) RepBlack else RepGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                value = "Manage",
                expanded = false,
                onToggle = {
                    context.startActivity(
                        Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ) {}
        }

        item {
            SettingRow(
                icon = Icons.Filled.Shield,
                title = "Privacy & security",
                value = "On-device",
                expanded = expanded == "privacy",
                onToggle = { toggle("privacy") },
            ) {
                Text(
                    "Pose detection runs on your phone. Camera frames are not uploaded. " +
                        "Rep and unlock history stays in this app's local database.",
                    color = RepGray,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            SettingRow(
                icon = Icons.Filled.HelpOutline,
                title = "Help & support",
                value = "FAQ",
                expanded = expanded == "help",
                onToggle = { toggle("help") },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Reps not counting? Stand 2 m from the camera, keep your full body in frame, and use good light.",
                        color = RepGray,
                        fontSize = 12.sp,
                    )
                    Text(
                        "Your unlock timer starts the moment you finish the reps.",
                        color = RepGray,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    details: @Composable () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, color = RepWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(value, color = ElectricGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = RepGray,
                )
            }
            if (expanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    details()
                }
            }
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = ElectricGreen,
    activeTrackColor = ElectricGreen,
    inactiveTrackColor = GlassSurface,
)
