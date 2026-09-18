package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proyect.qchuago.R
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.ui.navigation.Rutas

@Composable
fun HomeScreen(
    navController: NavController,
    usuario: Usuario?,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(usuario) {
        if (usuario == null) {
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_app),
            contentDescription = "Logo de QchuaGO",
            modifier = Modifier.size(96.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "¡Hola, ${usuario?.nombre ?: "amigo"}!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Has iniciado sesión correctamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onCerrarSesion) {
            Text("Cerrar sesión")
        }
    }
}