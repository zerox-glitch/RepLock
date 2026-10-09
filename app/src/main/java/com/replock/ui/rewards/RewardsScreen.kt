package com.replock.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.ui.components.GlassCard
import com.replock.ui.components.PillBadge
import com.replock.ui.components.ProgressRing
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.PremiumGold
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepWhite

@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = viewModel(),
    onOpenPaywall: () -> Unit = {},
) {
    val ui by viewModel.ui.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RepBlack),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Rewards", color = RepWhite, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Every rep and unlock levels you up", color = RepGray, fontSize = 13.sp)
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressRing(
                        progress = ui.xpIntoLevel.toFloat() / ui.xpPerLevel,
                        modifier = Modifier.size(150.dp),
                        strokeWidth = 12.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = PremiumGold, modifier = Modifier.size(26.dp))
                            Text("LV ${ui.level}", color = RepWhite, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(ui.levelTitle, color = RepWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${ui.totalXp} XP total · ${ui.xpPerLevel - ui.xpIntoLevel} XP to level ${ui.level + 1}",
                        color = RepGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("RepLock Pro", color = PremiumGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            if (ui.isPro) PillBadge("ACTIVE", PremiumGold)
                        }
                        Text(
                            if (ui.isPro) "Unlimited apps and unlocks" else "Unlimited apps and unlocks, no daily cap",
                            color = RepGray,
                            fontSize = 12.sp,
                        )
                    }
                    if (!ui.isPro) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PremiumGold, RoundedCornerShape(50))
                                .clickable(onClick = onOpenPaywall)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("Go Pro", color = RepBlack, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "MILESTONES",
                color = RepGray,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items(ui.milestones) { milestone ->
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(
                                if (milestone.done) ElectricGreen else Color.White.copy(alpha = 0.06f),
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (milestone.done) Icons.Filled.Check else Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (milestone.done) RepBlack else RepGray,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(milestone.title, color = RepWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(milestone.subtitle, color = RepGray, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(milestone.progress)
                                    .fillMaxHeight()
                                    .background(ElectricGreen),
                            )
                        }
                    }
                    if (milestone.done) {
                        Spacer(Modifier.width(10.dp))
                        PillBadge("DONE", ElectricGreen)
                    }
                }
            }
        }
    }
}
