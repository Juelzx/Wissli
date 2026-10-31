package com.wissli.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.wissli.app.core.navigation.WissliNavHost
import com.wissli.app.ui.theme.WissliTheme

// AppCompatActivity applies the per-app language below Android 13, see LocalSettingsRepository.
class MainActivity : AppCompatActivity() {
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
