package com.proyect.qchuago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyect.qchuago.ui.navigation.AppNavigation
import com.proyect.qchuago.ui.theme.QchuaGOTheme
import com.proyect.qchuago.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val temaOscuro by authViewModel.temaOscuro.collectAsState()

            QchuaGOTheme(darkTheme = temaOscuro) {
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }
}
