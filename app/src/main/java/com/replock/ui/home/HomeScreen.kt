package com.replock.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.replock.ui.components.AppIconImage
import com.replock.ui.components.GlassCard
import com.replock.ui.components.GlassSurface
import com.replock.ui.components.PillBadge
import com.replock.ui.components.ProgressRing
import com.replock.ui.components.Sparkline
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.PremiumGold
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepWhite
import com.replock.util.formatUnlockWindow
import kotlin.math.roundToInt

/** Free-tier unlock allowance per day (shown as "x/3" on the ring). */
private const val FREE_UNLOCKS_PER_DAY = 3

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenAppPicker: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
) {
    val blockingEnabled by viewModel.blockingEnabled.collectAsState()
    val repsToday by viewModel.repsToday.collectAsState()
    val unlocksToday by viewModel.unlocksToday.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val windowMinutes by viewModel.unlockWindowMinutes.collectAsState()
    val repTarget by viewModel.repTarget.collectAsState()
    val repsLast7 by viewModel.repsLast7.collectAsState()
    val blockedPackages by viewModel.blockedPackages.collectAsState()

    val yesterdayReps = repsLast7.getOrNull(repsLast7.size - 2) ?: 0
    val delta = repsToday - yesterdayReps
    val deltaText = when {
        repsLast7.size < 2 -> "Start your first set"
        delta > 0 -> "+$delta vs yesterday"
        delta < 0 -> "$delta vs yesterday"
        else -> "Same as yesterday"
    }
    val goalFraction = repsToday.toFloat() / repTarget.coerceAtLeast(1)
    val goalPercent = (goalFraction.coerceIn(0f, 1f) * 100).roundToInt()
    val minutesEarned = unlocksToday * windowMinutes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RepBlack),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("REP", color = RepWhite, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("LOCK", color = ElectricGreen, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        "FOCUS · EARN · GROW",
                        color = RepGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .background(GlassSurface, CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Settings", tint = ElectricGreen)
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = ElectricGreen)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (blockingEnabled) "Blocking Active" else "Blocking Paused",
                            color = RepWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (blockingEnabled) "Distractions are locked. Keep going!" else "Turn blocking on to start earning",
                            color = RepGray,
                            fontSize = 12.sp,
                        )
                    }
                    Switch(
                        checked = blockingEnabled,
                        onCheckedChange = viewModel::setBlockingEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RepBlack,
                            checkedTrackColor = ElectricGreen,
                            uncheckedTrackColor = GlassSurface,
                        ),
                    )
                }
            }
        }

        item {
            Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                val unlockFraction = if (isPro) 1f else unlocksToday.toFloat() / FREE_UNLOCKS_PER_DAY
                ProgressRing(
                    progress = unlockFraction,
                    modifier = Modifier.size(210.dp),
                    strokeWidth = 12.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = ElectricGreen, modifier = Modifier.size(22.dp))
                        Text(
                            if (isPro) "$unlocksToday" else "$unlocksToday/$FREE_UNLOCKS_PER_DAY",
                            color = RepWhite,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(
                            if (isPro) "UNLOCKS · PRO" else "UNLOCKS",
                            color = RepGray,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassCard(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("REPS TODAY", color = RepGray, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text(
                            "$repsToday",
                            color = ElectricGreen,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Text(deltaText, color = RepGray, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Sparkline(
                            values = repsLast7,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        )
                    }
                }
                GlassCard(Modifier.weight(1f)) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProgressRing(
                            progress = goalFraction,
                            modifier = Modifier.size(84.dp),
                            strokeWidth = 8.dp,
                        ) {
                            Text("$goalPercent%", color = RepWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${formatUnlockWindow(minutesEarned)} earned",
                            color = ElectricGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "of $repTarget reps goal",
                            color = RepGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Blocked Apps",
                            color = RepWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(ElectricGreen, CircleShape)
                                .clickable(onClick = onOpenAppPicker),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add app", tint = RepBlack)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (blockedPackages.isEmpty()) {
                        Text(
                            "Nothing blocked yet. Tap + to choose apps.",
                            color = RepGray,
                            fontSize = 12.sp,
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            blockedPackages.take(4).forEach { packageName ->
                                AppIconImage(packageName, size = 48.dp)
                            }
                        }
                    }
                }
            }
        }

        item {
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAppPicker),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = ElectricGreen)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Manage Blocklist", color = ElectricGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${blockedPackages.size} app(s) locked behind your reps",
                            color = RepGray,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = RepGray)
                }
            }
        }

        if (!isPro) {
            item {
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPaywall),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Free plan · $FREE_UNLOCKS_PER_DAY unlocks a day, 1 app",
                            color = RepGray,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        PillBadge("GO PRO", PremiumGold)
                    }
                }
            }
        }
    }
}
