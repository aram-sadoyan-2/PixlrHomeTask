package com.armsd.pixrhometask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.armsd.pixrhometask.ui.screen.EnhancementScreen
import com.armsd.pixrhometask.ui.theme.PixlrHomeTaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixlrHomeTaskTheme {
                EnhancementScreen()
            }
        }
    }
}
