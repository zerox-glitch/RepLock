package com.replock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.replock.ui.theme.ElectricGreen
import com.replock.ui.theme.RepGray

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Text(value, fontSize = 30.sp, fontWeight = FontWeight.Black, color = ElectricGreen)
            Spacer(Modifier.height(2.dp))
            Text(label, color = RepGray, fontSize = 13.sp)
        }
    }
}
