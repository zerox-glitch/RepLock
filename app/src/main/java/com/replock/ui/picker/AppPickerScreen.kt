package com.replock.ui.picker

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.replock.data.model.AppCategory
import com.replock.data.model.InstalledApp
import com.replock.ui.components.AppIconImage
import com.replock.ui.components.GlassCard
import com.replock.ui.components.GlassSurface
import com.replock.ui.components.PillBadge
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.PremiumGold
import com.replock.ui.theme.RepBlack
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepWhite

@Composable
fun AppPickerScreen(
    viewModel: AppPickerViewModel = viewModel(),
    onNavigateToPaywall: () -> Unit = {},
) {
    val installed by viewModel.installedApps.collectAsState()
    val blocked by viewModel.blockedApps.collectAsState()
    val isPro by viewModel.isPro.collectAsState()

    var category by remember { mutableStateOf<AppCategory?>(null) }
    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.paywallPrompt.collect { onNavigateToPaywall() }
    }

    val blockedPackages = blocked.filter { it.isEnabled }.map { it.packageName }.toSet()
    val filtered = installed.filter { app ->
        (category == null || app.category == category) &&
            (query.isBlank() || app.label.contains(query.trim(), ignoreCase = true))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(RepBlack)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Blocked Apps", color = RepWhite, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Apps that open only after your reps", color = RepGray, fontSize = 13.sp)
            }
            if (isPro) {
                PillBadge("PRO · UNLIMITED", PremiumGold)
            } else {
                PillBadge(
                    "${blockedPackages.size}/${AppPickerViewModel.FREE_BLOCKED_APPS} FREE",
                    ElectricGreen,
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocus),
            placeholder = { Text("Search apps", color = RepGray) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = RepGray) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RepWhite,
                unfocusedTextColor = RepWhite,
                focusedBorderColor = ElectricGreen,
                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                cursorColor = ElectricGreen,
                focusedContainerColor = GlassSurface,
                unfocusedContainerColor = GlassSurface,
            ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryPill("All", selected = category == null) { category = null }
            AppCategory.entries.forEach { option ->
                CategoryPill(option.label, selected = category == option) { category = option }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(filtered, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    checked = app.packageName in blockedPackages,
                    onToggle = { enabled -> viewModel.toggle(app, enabled) },
                )
            }
            item {
                GlassCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { searchFocus.requestFocus() },
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .background(ElectricGreen.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = ElectricGreen)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Add App", color = ElectricGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Search any installed app and block it", color = RepGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) ElectricGreen else GlassSurface,
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) RepBlack else RepGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(app.packageName, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    color = RepWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(app.category.label, color = RepGray, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RepBlack,
                    checkedTrackColor = ElectricGreen,
                    uncheckedTrackColor = GlassSurface,
                ),
            )
        }
    }
}
