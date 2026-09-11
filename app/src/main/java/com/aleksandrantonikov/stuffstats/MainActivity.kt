package com.aleksandrantonikov.stuffstats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aleksandrantonikov.stuffstats.navigation.StuffStatsApp
import com.aleksandrantonikov.stuffstats.ui.theme.StuffStatsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StuffStatsTheme {
                StuffStatsApp()
            }
        }
    }
}
