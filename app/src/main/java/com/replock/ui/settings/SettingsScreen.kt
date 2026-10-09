package com.replock.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.data.SettingsDataStore
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurface
import com.replock.util.formatUnlockWindow
import kotlin.math.abs

/** Unlock-window slider presets, in minutes. Max is 3 hours (180 min). */
private val WINDOW_PRESETS = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToPaywall: () -> Unit = {},
) {
    val repTarget by viewModel.repTarget.collectAsState()
    val windowMinutes by viewModel.unlockWindowMinutes.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        // Rep target
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Rep target", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Reps required to unlock an app", color = RepGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("$repTarget reps", fontSize = 32.sp, fontWeight = FontWeight.Black, color = ElectricGreen)
                Slider(
                    value = repTarget.toFloat(),
                    onValueChange = { viewModel.setRepTarget(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 8, // 5, 10, 15, ... 50
                )
            }
        }

        // Unlock window (up to 3 hours)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Unlock window", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "How long an app stays unlocked after you finish your reps (max 3 hr)",
                    color = RepGray,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    formatUnlockWindow(windowMinutes),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = ElectricGreen,
                )
                val windowIndex = WINDOW_PRESETS.indices
                    .minByOrNull { abs(WINDOW_PRESETS[it] - windowMinutes) }
                    ?: 0
                Slider(
                    value = windowIndex.toFloat(),
                    onValueChange = { index ->
                        viewModel.setUnlockWindowMinutes(
                            WINDOW_PRESETS[index.toInt().coerceIn(0, WINDOW_PRESETS.size - 1)]
                        )
                    },
                    valueRange = 0f..(WINDOW_PRESETS.size - 1).toFloat(),
                    steps = WINDOW_PRESETS.size - 2, // one stop per preset
                )
            }
        }

        // Exercise
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Exercise", color = Color.White, fontWeight = FontWeight.Bold)
                Text("What RepLock counts to unlock apps", color = RepGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = difficulty == SettingsDataStore.DIFFICULTY_PUSHUPS,
                        onClick = { viewModel.setDifficulty(SettingsDataStore.DIFFICULTY_PUSHUPS) },
                        label = { Text("Pushups") },
                    )
                    FilterChip(
                        selected = difficulty == SettingsDataStore.DIFFICULTY_SQUATS,
                        onClick = { viewModel.setDifficulty(SettingsDataStore.DIFFICULTY_SQUATS) },
                        label = { Text("Squats") },
                    )
                    FilterChip(
                        selected = difficulty == SettingsDataStore.DIFFICULTY_BOTH,
                        onClick = { viewModel.setDifficulty(SettingsDataStore.DIFFICULTY_BOTH) },
                        label = { Text("Both") },
                    )
                }
            }
        }

        // Pro
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("RepLock Pro", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                if (isPro) {
                    Text(
                        "Pro is active (stub — no real billing wired up yet).",
                        color = ElectricGreen,
                        fontSize = 14.sp,
                    )
                } else {
                    Text(
                        "$9.99/month or $29.99/year — unlimited unlocks, unlimited blocked apps, " +
                            "custom rep targets, squat mode, full stats history.",
                        color = RepGray,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onNavigateToPaywall, modifier = Modifier.fillMaxWidth()) {
                        Text("Go Pro")
                    }
                }
            }
        }
    }
}
