package com.replock.ui.stats

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.ui.components.ChartSeries
import com.replock.ui.components.GlassCard
import com.replock.ui.components.LineChart
import com.replock.ui.components.StatCard
import com.replock.ui.theme.ChartViolet
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.util.formatUnlockWindow

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val repsToday by viewModel.repsToday.collectAsState()
    val repsWeek by viewModel.repsThisWeek.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val minutesEarned by viewModel.minutesEarned.collectAsState()
    val totalReps by viewModel.totalReps.collectAsState()
    val totalUnlocks by viewModel.totalUnlocks.collectAsState()
    val repsLast7 by viewModel.repsLast7.collectAsState()
    val unlocksLast7 by viewModel.unlocksLast7.collectAsState()
    val activeLast7 by viewModel.activeDaysLast7.collectAsState()
    val repsLast14 by viewModel.repsLast14.collectAsState()
    val unlocksLast14 by viewModel.unlocksLast14.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Stats", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard("Reps today", "$repsToday", Modifier.weight(1f), sparkline = repsLast7)
            StatCard("Streak", "$streak days", Modifier.weight(1f), sparkline = activeLast7)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatCard("Reps this week", "$repsWeek", Modifier.weight(1f), sparkline = repsLast7)
            StatCard(
                "Time earned",
                formatUnlockWindow(minutesEarned),
                Modifier.weight(1f),
                sparkline = unlocksLast7,
            )
        }

        // All-time: two smooth series over the last 14 days.
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "All time",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    LegendDot("Reps", ElectricGreen)
                    Spacer(Modifier.width(12.dp))
                    LegendDot("Unlocks", ChartViolet)
                }
                Spacer(Modifier.height(4.dp))
                Text("Last 14 days", color = RepGray, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                LineChart(
                    series = listOf(
                        ChartSeries(repsLast14, ElectricGreen),
                        ChartSeries(unlocksLast14, ChartViolet),
                    ),
                    fill = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text("Total reps: $totalReps", color = RepGray)
                Text("Total unlocks: $totalUnlocks", color = RepGray)
                Spacer(Modifier.height(14.dp))
                // Pill highlight, as in the reference design.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(ElectricGreen, RoundedCornerShape(50))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatUnlockWindow(minutesEarned),
                        color = RepBlack,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Screen time\nearned back",
                        color = RepBlack,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50))) {}
        Spacer(Modifier.width(6.dp))
        Text(label, color = RepGray, fontSize = 12.sp)
    }
}
