package com.replock.ui.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray

private val BarSurface = Color(0xFF0B1410)

/**
 * Bottom navigation: Home, Stats, a raised centre lock button that opens
 * Blocked Apps, Rewards, Settings.
 */
@Composable
fun RepLockBottomBar(currentRoute: String?, onNavigate: (Screen) -> Unit) {
    Box(Modifier.fillMaxWidth().height(92.dp)) {
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(70.dp)
                .background(BarSurface, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarItem("Home", Icons.Filled.Home, currentRoute == Screen.Home.route) { onNavigate(Screen.Home) }
            BarItem("Stats", Icons.Filled.ShowChart, currentRoute == Screen.Stats.route) { onNavigate(Screen.Stats) }
            Spacer(Modifier.width(72.dp))
            BarItem("Rewards", Icons.Filled.EmojiEvents, currentRoute == Screen.Rewards.route) { onNavigate(Screen.Rewards) }
            BarItem("Settings", Icons.Filled.Settings, currentRoute == Screen.Settings.route) { onNavigate(Screen.Settings) }
        }

        val lockSelected = currentRoute == Screen.AppPicker.route
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(70.dp)
                .background(
                    Brush.verticalGradient(listOf(ElectricGreen, Color(0xFF00C46A))),
                    CircleShape,
                )
                .clickable { onNavigate(Screen.AppPicker) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Blocked apps",
                tint = if (lockSelected) RepBlack else RepBlack,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun RowScope.BarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) ElectricGreen else RepGray
    Column(
        Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
