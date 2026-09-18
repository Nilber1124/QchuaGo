package com.proyect.qchuago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.proyect.qchuago.ui.navigation.AppNavigation
import com.proyect.qchuago.ui.theme.QchuaGOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QchuaGOTheme {
                AppNavigation()
            }
        }
    }
}