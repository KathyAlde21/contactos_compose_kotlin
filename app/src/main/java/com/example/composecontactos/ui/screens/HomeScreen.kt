package com.example.composecontactos.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.composecontactos.ui.navigation.NavigationWrapper
import com.example.composecontactos.ui.theme.ComposeContactosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeContactosTheme {
                NavigationWrapper()
            }
        }
    }
}

