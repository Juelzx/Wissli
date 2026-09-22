package com.wissli.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wissli.app.core.navigation.WissliNavHost
import com.wissli.app.ui.theme.WissliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WissliTheme {
                WissliNavHost()
            }
        }
    }
}
