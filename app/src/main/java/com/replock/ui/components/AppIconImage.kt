package com.replock.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.replock.ui.theme.RepGray
import com.replock.ui.theme.RepSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The launcher icon of [packageName], rendered rounded. Falls back to the app's initial. */
@Composable
fun AppIconImage(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val px = 128
                val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                val canvas = AndroidCanvas(bitmap)
                drawable.setBounds(0, 0, px, px)
                drawable.draw(canvas)
                bitmap.asImageBitmap()
            }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(size * 0.26f)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(RepSurfaceVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = icon
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                packageName.take(1).uppercase(),
                color = RepGray,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}
