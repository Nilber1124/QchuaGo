package com.proyect.qchuago.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proyect.qchuago.R
import com.proyect.qchuago.ui.screens.HomeScreen
import com.proyect.qchuago.ui.screens.LoginScreen
import com.proyect.qchuago.ui.screens.RegisterScreen
import com.proyect.qchuago.ui.viewmodel.AuthViewModel

object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val HOME = "home"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val sesionInicializada by authViewModel.sesionInicializada.collectAsState()
    val usuario by authViewModel.usuario.collectAsState()
    val cargando by authViewModel.cargando.collectAsState()
    val error by authViewModel.error.collectAsState()

    // Cuando se comprueba la sesión al arrancar, salta a Login o Home.
    LaunchedEffect(sesionInicializada) {
        if (sesionInicializada) {
            navController.navigate(if (usuario != null) Rutas.HOME else Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.SPLASH,
        modifier = modifier,
    ) {
        composable(Rutas.SPLASH) { PantallaCargaInicial() }
        composable(Rutas.LOGIN) {
            LoginScreen(
                navController = navController,
                cargando = cargando,
                error = error,
                usuario = usuario,
                onIniciarSesion = authViewModel::iniciarSesion,
            )
        }
        composable(Rutas.REGISTRO) {
            RegisterScreen(
                navController = navController,
                cargando = cargando,
                error = error,
                usuario = usuario,
                onRegistrar = authViewModel::registrar,
            )
        }
        composable(Rutas.HOME) {
            val temaOscuro by authViewModel.temaOscuro.collectAsState()
            HomeScreen(
                navController = navController,
                usuario = usuario,
                error = error,
                temaOscuro = temaOscuro,
                onCambiarTema = authViewModel::cambiarTema,
                onCargarPerfil = authViewModel::recargarPerfil,
                onActualizarNombre = authViewModel::actualizarNombre,
                onCambiarContrasena = authViewModel::cambiarContrasena,
                onCerrarSesion = authViewModel::cerrarSesion,
            )
        }
    }
}

@Composable
private fun PantallaCargaInicial(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_app),
            contentDescription = "Logo de QchuaGO",
            modifier = Modifier.size(112.dp),
        )
        Text(
            text = "QchuaGO",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.size(24.dp))
        CircularProgressIndicator()
    }
}
