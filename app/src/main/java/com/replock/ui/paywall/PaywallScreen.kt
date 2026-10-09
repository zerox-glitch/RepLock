package com.replock.ui.paywall

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.LockedRed
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurface

private enum class Plan { Monthly, Yearly }

/**
 * Hardcoded paywall stub (MVP). Shown after the 3rd free unlock of the day, or
 * when a free user tries to block a second app. Tapping "Start free trial"
 * flips the stub Pro flag — wire RevenueCat / Play Billing here for real.
 */
@Composable
fun PaywallScreen(
    onSubscribed: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPlan by remember { mutableStateOf(Plan.Yearly) }

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Filled.Lock, null, tint = LockedRed, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Out of free unlocks",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Free: 3 unlocks per day and 1 blocked app. Go Pro for unlimited everything.",
            color = RepGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        listOf(
            "Unlimited unlocks every day",
            "Block unlimited apps",
            "Custom rep targets",
            "Squats mode (v2)",
            "Full stats history",
        ).forEach { feature ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Check, null, tint = ElectricGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(feature, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
        PlanCard(
            title = "Monthly",
            price = "$9.99 / month",
            selected = selectedPlan == Plan.Monthly,
            onClick = { selectedPlan = Plan.Monthly },
        )
        Spacer(Modifier.height(8.dp))
        PlanCard(
            title = "Yearly",
            price = "$29.99 / year",
            selected = selectedPlan == Plan.Yearly,
            badge = "2 months free",
            onClick = { selectedPlan = Plan.Yearly },
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubscribed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricGreen,
                contentColor = RepBlack,
            ),
        ) {
            Text("Start 3-day free trial", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "STUB: no real billing yet — tapping subscribes instantly.",
            color = RepGray,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onDismiss) { Text("Not now", color = RepGray) }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    selected: Boolean,
    badge: String? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ElectricGreen.copy(alpha = 0.12f) else RepSurface,
        ),
        border = if (selected) BorderStroke(2.dp, ElectricGreen) else null,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(price, color = RepGray)
            }
            if (badge != null) {
                Surface(color = ElectricGreen, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        badge,
                        color = RepBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
