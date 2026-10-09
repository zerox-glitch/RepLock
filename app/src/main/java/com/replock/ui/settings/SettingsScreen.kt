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
                Text("Pushups required to unlock an app", color = RepGray, fontSize = 13.sp)
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

        // Unlock window
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Unlock window", color = Color.White, fontWeight = FontWeight.Bold)
                Text("How long an app stays unlocked after you finish your reps", color = RepGray, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text("$windowMinutes min", fontSize = 32.sp, fontWeight = FontWeight.Black, color = ElectricGreen)
                Slider(
                    value = windowMinutes.toFloat(),
                    onValueChange = { viewModel.setUnlockWindowMinutes(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28, // 1, 2, 3, ... 30
                )
            }
        }

        // Difficulty
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("Exercise", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Squats mode lands in v2 — detection is pushups for now", color = RepGray, fontSize = 13.sp)
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
                        label = { Text("Squats (v2)") },
                    )
                    FilterChip(
                        selected = difficulty == SettingsDataStore.DIFFICULTY_BOTH,
                        onClick = { viewModel.setDifficulty(SettingsDataStore.DIFFICULTY_BOTH) },
                        label = { Text("Both (v2)") },
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
                            "custom rep targets, squats mode, full stats history.",
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
