package com.replock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.replock.ui.RepLockRoot
import com.replock.ui.theme.RepLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RepLockApp
        setContent {
            RepLockTheme {
                Surface {
                    RepLockRoot(settings = app.settings)
                }
            }
        }
    }
}
