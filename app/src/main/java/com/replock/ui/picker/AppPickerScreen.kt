package com.replock.ui.picker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.data.model.InstalledApp
import com.replock.ui.components.GlassCard
import com.replock.ui.components.PillBadge
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.PremiumGold
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which rows the list shows. */
private enum class ListFilter(val label: String) { All("All"), Blocked("Blocked"), Open("Open") }

@Composable
fun AppPickerScreen(
    viewModel: AppPickerViewModel = viewModel(),
    onNavigateToPaywall: () -> Unit = {},
) {
    val installedApps by viewModel.installedApps.collectAsState()
    val blockedApps by viewModel.blockedApps.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ListFilter.All) }

    LaunchedEffect(Unit) {
        viewModel.paywallPrompt.collect { onNavigateToPaywall() }
    }

    val blockedMap = remember(blockedApps) { blockedApps.associateBy { it.packageName } }
    val blockedCount = blockedApps.count { it.isEnabled }
    val filtered = remember(installedApps, query, filter, blockedMap) {
        installedApps
            .filter { app ->
                val blocked = blockedMap[app.packageName]?.isEnabled == true
                when (filter) {
                    ListFilter.All -> true
                    ListFilter.Blocked -> blocked
                    ListFilter.Open -> !blocked
                }
            }
            .filter {
                query.isBlank() ||
                    it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Title + plan badge
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Blocked Apps",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (isPro) {
                PillBadge("PRO UNLIMITED", PremiumGold)
            } else {
                PillBadge("FREE · $blockedCount/1", RepGray)
            }
        }
        Text(
            if (isPro) "Pro: unlimited blocked apps" else "Free: 1 blocked app · Pro: unlimited",
            color = RepGray,
            fontSize = 13.sp,
        )

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search apps", color = RepGray) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = RepGray) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricGreen.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                    focusedContainerColor = RepSurfaceVariant,
                    unfocusedContainerColor = RepSurfaceVariant,
                    cursorColor = ElectricGreen,
                ),
            )
            Spacer(Modifier.width(10.dp))
            // Cycles All → Blocked → Open. Current filter is shown as a small label.
            Column(
                Modifier
                    .size(56.dp)
                    .background(RepSurfaceVariant, RoundedCornerShape(28.dp))
                    .clickable {
                        filter = ListFilter.entries[(filter.ordinal + 1) % ListFilter.entries.size]
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Filter apps",
                    tint = if (filter == ListFilter.All) RepGray else ElectricGreen,
                )
                Text(filter.label, color = RepGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filtered, key = { it.packageName }) { app ->
                val blocked = blockedMap[app.packageName]?.isEnabled == true
                AppRow(
                    app = app,
                    blocked = blocked,
                    onToggle = { viewModel.toggle(app, it) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, blocked: Boolean, onToggle: (Boolean) -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app.packageName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    app.packageName,
                    color = RepGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Switch(
                checked = blocked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RepBlack,
                    checkedTrackColor = ElectricGreen,
                    uncheckedTrackColor = Color(0xFF2A2A2A),
                ),
            )
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val size = 96
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }
    if (icon != null) {
        Image(
            bitmap = icon!!,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
    } else {
        Box(
            Modifier
                .size(44.dp)
                .background(RepSurfaceVariant, RoundedCornerShape(10.dp)),
        )
    }
}
