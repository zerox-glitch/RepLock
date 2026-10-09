package com.replock.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.replock.ui.components.StatCard
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurface
import com.replock.util.formatUnlockWindow

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val repsToday by viewModel.repsToday.collectAsState()
    val repsWeek by viewModel.repsThisWeek.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val minutesEarned by viewModel.minutesEarned.collectAsState()
    val totalReps by viewModel.totalReps.collectAsState()
    val totalUnlocks by viewModel.totalUnlocks.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Stats", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Reps today", "$repsToday", Modifier.weight(1f))
            StatCard("Streak", "$streak days", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard("Reps this week", "$repsWeek", Modifier.weight(1f))
            StatCard("Time earned", formatMinutes(minutesEarned), Modifier.weight(1f))
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = RepSurface)) {
            Column(Modifier.padding(16.dp)) {
                Text("All time", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Total reps: $totalReps", color = RepGray)
                Text("Total unlocks: $totalUnlocks", color = RepGray)
                Text(
                    "Screen time earned back: ${formatMinutes(minutesEarned)}",
                    color = ElectricGreen,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String = formatUnlockWindow(minutes)
