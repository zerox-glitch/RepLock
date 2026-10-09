package com.replock.ui.stats

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.ui.components.BarChart
import com.replock.ui.components.ChartSeries
import com.replock.ui.components.GlassCard
import com.replock.ui.components.LineChart
import com.replock.ui.components.SegmentedTabs
import com.replock.ui.theme.ChartBlue
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepWhite
import com.replock.util.formatUnlockWindow

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {
    val ui by viewModel.ui.collectAsState()
    val range by viewModel.range.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RepBlack),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Stats", color = RepWhite, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Your progress, measured in reps", color = RepGray, fontSize = 13.sp)
            }
        }

        item {
            SegmentedTabs(
                items = StatsRange.entries,
                selected = range,
                label = { it.label },
                onSelect = viewModel::setRange,
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("TOTAL REPS", color = RepGray, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text(
                            "${ui.reps}",
                            color = ElectricGreen,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                        )
                        DeltaText(ui.repsDelta, unit = "reps", range = ui.range)
                        Spacer(Modifier.height(10.dp))
                        BarChart(
                            values = ui.last7Reps,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        )
                    }
                }
                GlassCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("TIME EARNED", color = RepGray, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text(
                            formatUnlockWindow(ui.minutesEarned),
                            color = ElectricGreen,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                        )
                        DeltaText(ui.minutesDelta, unit = "min", range = ui.range)
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Timer, contentDescription = null, tint = ElectricGreen)
                        }
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Reps & Unlocks",
                            color = RepWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        LegendDot("Reps", ElectricGreen)
                        Spacer(Modifier.width(12.dp))
                        LegendDot("Unlocks", ChartBlue)
                    }
                    Spacer(Modifier.height(12.dp))
                    LineChart(
                        series = listOf(
                            ChartSeries(ui.repSeries, ElectricGreen),
                            ChartSeries(ui.unlockSeries, ChartBlue),
                        ),
                        modifier = Modifier.fillMaxWidth().height(170.dp),
                        fill = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Last ${ui.range.days} days",
                        color = RepGray,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "\u201COne more set today is one less distraction tomorrow.\u201D",
                        color = RepWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Screen time earned back", color = ElectricGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DeltaText(delta: Int, unit: String, range: StatsRange) {
    val previous = "vs previous ${range.days} days"
    val (text, color) = when {
        delta > 0 -> "↑ +$delta $unit $previous" to ElectricGreen
        delta < 0 -> "↓ $delta $unit $previous" to LockedRed
        else -> "No change $previous" to RepGray
    }
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = RepGray, fontSize = 11.sp)
    }
}
