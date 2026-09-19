package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.qchuago.R
import com.proyect.qchuago.data.entidad.Usuario

@Composable
fun PerfilScreen(
    usuario: Usuario?, error: String?, temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit, onCargarPerfil: () -> Unit,
    onActualizarNombre: (String, () -> Unit) -> Unit,
    onCambiarContrasena: (String, String, () -> Unit) -> Unit,
    onCerrarSesion: () -> Unit, modifier: Modifier = Modifier,
) {
    var mostrarEditarPerfil by remember { mutableStateOf(false) }
    var mostrarCambiarContrasena by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { onCargarPerfil() }

    val perfil = usuario ?: Usuario()
    val nombre = perfil.nombre.ifBlank { "Usuario" }
    val correo = perfil.correo.ifBlank { "Correo no disponible" }
    val iniciales = nombre.take(2).uppercase().ifEmpty { "US" }
    // Calculamos el porcentaje de avance una sola vez.
    val progreso = if (perfil.totalLecciones > 0) perfil.leccionesCompletadas.toFloat() / perfil.totalLecciones else 0f
    val porcentaje = (progreso * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EncabezadoPerfil(
            nombre = nombre,
            correo = correo,
            iniciales = iniciales,
            nivel = perfil.nivel
        ) {
            nuevoNombre = perfil.nombre
            mostrarEditarPerfil = true
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            TituloSeccion(
                texto = "ESTADÍSTICAS",
                modifier = Modifier.padding(vertical = 8.dp)
            )
            EstadisticasPerfil(perfil)

            Spacer(Modifier.height(16.dp))
            TarjetaProgreso(perfil, progreso, porcentaje)

            Spacer(Modifier.height(24.dp))
            TituloSeccion(
                texto = "PREFERENCIAS",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            PreferenciasPerfil(temaOscuro, onCambiarTema)

            Spacer(Modifier.height(24.dp))
            OpcionPerfil("Cambiar contraseña") { mostrarCambiarContrasena = true }

            Spacer(Modifier.height(12.dp))
            BotonCerrarSesion(onCerrarSesion)

            Spacer(Modifier.height(20.dp))
            Text(
                text = "QchuaGo · Aprendizaje del Quechua · v1.0",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (mostrarEditarPerfil) {
        DialogoEditarPerfil(
            nombre = nuevoNombre,
            error = error,
            onNombreChange = { nuevoNombre = it },
            onGuardar = {
                onActualizarNombre(nuevoNombre) { mostrarEditarPerfil = false }
            },
            onCancelar = { mostrarEditarPerfil = false }
        )
    }

    if (mostrarCambiarContrasena) {
        DialogoCambiarContrasena(
            nueva = nuevaContrasena,
            confirmar = confirmarContrasena,
            error = error,
            onNuevaChange = { nuevaContrasena = it },
            onConfirmarChange = { confirmarContrasena = it },
            onGuardar = {
                onCambiarContrasena(nuevaContrasena, confirmarContrasena) {
                    mostrarCambiarContrasena = false
                    nuevaContrasena = ""
                    confirmarContrasena = ""
                }
            },
            onCancelar = {
                mostrarCambiarContrasena = false
                nuevaContrasena = ""
                confirmarContrasena = ""
            }
        )
    }
}

@Composable
private fun EncabezadoPerfil(
    nombre: String,
    correo: String,
    iniciales: String,
    nivel: String,
    onEditar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iniciales,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = nombre,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onEditar),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_perfil_edit),
                    contentDescription = "Editar perfil",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(Modifier.height(4.dp))
        
        Text(
            text = correo,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(12.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_perfil_nivel),
                    contentDescription = "Nivel Principiante",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = nivel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun TituloSeccion(
    texto: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = texto,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = modifier
    )
}

@Composable
private fun EstadisticasPerfil(usuario: Usuario) {
    val racha = if (usuario.racha == 1) "1 día" else "${usuario.racha} días"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TarjetaEstadistica(
            icono = R.drawable.ic_perfil_xp,
            valor = usuario.xp.toString(),
            etiqueta = "Puntos XP",
            colorValor = Color(0xFFD97706),
            modifier = Modifier.weight(1f)
        )
        TarjetaEstadistica(
            icono = R.drawable.ic_perfil_racha,
            valor = racha,
            etiqueta = "Racha de días",
            colorValor = Color(0xFFC2410C),
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TarjetaEstadistica(
            icono = R.drawable.ic_perfil_lecciones,
            valor = "${usuario.leccionesCompletadas}/${usuario.totalLecciones}",
            etiqueta = "Lecciones",
            colorValor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        TarjetaEstadistica(
            icono = R.drawable.ic_perfil_vidas,
            valor = usuario.vidas.toString(),
            etiqueta = "Vidas restantes",
            colorValor = Color(0xFFDC2626),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaEstadistica(
    icono: Int,
    valor: String,
    etiqueta: String,
    colorValor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(icono),
                contentDescription = etiqueta,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = valor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorValor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = etiqueta,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaProgreso(
    usuario: Usuario,
    progreso: Float,
    porcentaje: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progreso Total",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$porcentaje%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(10.dp))
            
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "${usuario.leccionesCompletadas} de ${usuario.totalLecciones} lecciones completadas",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PreferenciasPerfil(
    temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_perfil_tema),
                    contentDescription = "Tema de la app",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tema de la app",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (temaOscuro) "Modo Oscuro" else "Modo Claro",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = temaOscuro,
                onCheckedChange = onCambiarTema,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun OpcionPerfil(
    texto: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = texto,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
    }
}

@Composable
private fun BotonCerrarSesion(onCerrarSesion: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onCerrarSesion)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cerrar Sesión",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DialogoEditarPerfil(
    nombre: String,
    error: String?,
    onNombreChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar perfil") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre") },
                    singleLine = true
                )
                MensajeError(error)
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DialogoCambiarContrasena(
    nueva: String,
    confirmar: String,
    error: String?,
    onNuevaChange: (String) -> Unit,
    onConfirmarChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Cambiar contraseña") },
        text = {
            Column {
                OutlinedTextField(
                    value = nueva,
                    onValueChange = onNuevaChange,
                    label = { Text("Nueva contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmar,
                    onValueChange = onConfirmarChange,
                    label = { Text("Confirmar contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                MensajeError(error)
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MensajeError(error: String?) {
    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
