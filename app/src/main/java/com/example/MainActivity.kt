package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.sokotz.ui.navigation.SokoTzApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.sokotz.data.util.FirebaseInitHelper.ensureInitialized(this)
        ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                SokoTzApp()
            }
        }
    }
}
