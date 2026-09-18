package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.proyect.qchuago.R
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.ui.navigation.Rutas

@Composable
fun LoginScreen(
    navController: NavController,
    cargando: Boolean,
    error: String?,
    usuario: Usuario?,
    onIniciarSesion: (correo: String, contrasena: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var correo by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    val errorVisible = mensajeError ?: error

    LaunchedEffect(usuario) {
        if (usuario != null) {
            navController.navigate(Rutas.HOME) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_app),
            contentDescription = "Logo de QchuaGO",
            modifier = Modifier.size(110.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("QchuaGO", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Aprende quechua de forma sencilla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(36.dp))
        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        errorVisible?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = {
                if (correo.isBlank() || contrasena.isBlank()) {
                    mensajeError = "Ingresa tu correo y contraseña."
                } else {
                    mensajeError = null
                    onIniciarSesion(correo, contrasena)
                }
            },
            enabled = !cargando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Iniciar sesión")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { navController.navigate(Rutas.REGISTRO) }) {
                Text("Registrarse")
            }
        }
    }
}