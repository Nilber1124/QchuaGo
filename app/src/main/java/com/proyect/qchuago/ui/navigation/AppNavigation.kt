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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.proyect.qchuago.R
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.ui.screens.BienvenidaScreen
import com.proyect.qchuago.ui.screens.ElegirNivelScreen
import com.proyect.qchuago.ui.screens.EvaluacionScreen
import com.proyect.qchuago.ui.screens.HomeScreen
import com.proyect.qchuago.ui.screens.LeccionScreen
import com.proyect.qchuago.ui.screens.LoginScreen
import com.proyect.qchuago.ui.screens.RegisterScreen
import com.proyect.qchuago.ui.viewmodel.ActividadesPruebaNivel
import com.proyect.qchuago.ui.viewmodel.AuthViewModel

object Rutas {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val BIENVENIDA = "bienvenida"
    const val ELEGIR_NIVEL = "elegir_nivel"
    const val PRUEBA_NIVEL = "prueba_nivel"
    const val HOME = "home"
    const val LECCION = "leccion"
    const val EVALUACION = "evaluacion"

    /** Patrón que registra el NavHost (los {} son los argumentos). */
    const val LECCION_PATRON = "$LECCION/{unidadId}/{leccionId}"
    const val EVALUACION_PATRON = "$EVALUACION/{unidadId}"

    /** Ruta concreta para abrir una lección: `navController.navigate(Rutas.leccion(u, l))`. */
    fun leccion(unidadId: String, leccionId: String): String = "$LECCION/$unidadId/$leccionId"

    fun evaluacion(unidadId: String): String = "$EVALUACION/$unidadId"

    /** Decide a dónde navegar tras autenticar: bienvenida la primera vez, Home después. */
    fun destinoTrasAutenticar(usuario: Usuario): String =
        if (usuario.onboardingCompletado) HOME else BIENVENIDA
}

/**
 * Contenedor de navegación. Las pantallas reciben [navController] directamente y
 * navegan por sí mismas (`navController.navigate(...)`/`popBackStack()`); aquí solo
 * se registran las rutas, sin mapear lambdas `onIrAX` para cada una.
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val sesionInicializada by authViewModel.sesionInicializada.collectAsState()
    val usuario by authViewModel.usuario.collectAsState()

    // Cuando se comprueba la sesión al arrancar, salta a Login, Bienvenida o Home.
    LaunchedEffect(sesionInicializada) {
        if (sesionInicializada) {
            val destino = usuario?.let { Rutas.destinoTrasAutenticar(it) } ?: Rutas.LOGIN
            navController.navigate(destino) {
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
        composable(Rutas.LOGIN) { LoginScreen(navController, authViewModel) }
        composable(Rutas.REGISTRO) { RegisterScreen(navController, authViewModel) }
        composable(Rutas.BIENVENIDA) { BienvenidaScreen(navController, authViewModel) }
        composable(Rutas.ELEGIR_NIVEL) { ElegirNivelScreen(navController, authViewModel) }
        composable(Rutas.PRUEBA_NIVEL) {
            LeccionScreen(
                navController = navController,
                authViewModel = authViewModel,
                unidadId = "prueba-nivel",
                leccionId = "demo",
                actividadesEstaticas = ActividadesPruebaNivel.lista,
            )
        }
        composable(Rutas.HOME) { HomeScreen(navController, authViewModel) }
        composable(
            route = Rutas.LECCION_PATRON,
            arguments = listOf(
                navArgument("unidadId") { type = NavType.StringType },
                navArgument("leccionId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val unidadId = backStackEntry.arguments?.getString("unidadId") ?: return@composable
            val leccionId = backStackEntry.arguments?.getString("leccionId") ?: return@composable
            LeccionScreen(navController, authViewModel, unidadId, leccionId)
        }
        composable(
            route = Rutas.EVALUACION_PATRON,
            arguments = listOf(navArgument("unidadId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val unidadId = backStackEntry.arguments?.getString("unidadId") ?: return@composable
            EvaluacionScreen(navController, authViewModel, unidadId)
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
