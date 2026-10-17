package com.proyect.qchuago.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.servicio.AudioService
import com.proyect.qchuago.ui.viewmodel.EstadoLeccion
import com.proyect.qchuago.ui.viewmodel.LeccionViewModel

private val TIPOS_CONTENIDO = setOf("VOCABULARIO", "PRONUNCIACION")

@Composable
fun LeccionScreen(
    unidadId: String,
    leccionId: String,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
    actividadesEstaticas: List<Actividad>? = null,
) {
    val context = LocalContext.current
    val leccionViewModel: LeccionViewModel = viewModel(
        key = "$unidadId/$leccionId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LeccionViewModel(
                    unidadId = unidadId,
                    leccionId = leccionId,
                    audioService = AudioService(context.applicationContext),
                    actividadesEstaticas = actividadesEstaticas,
                ) as T
            }
        },
    )
    val estado by leccionViewModel.estadoLeccion.collectAsState()

    val lanzadorPermisoAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) leccionViewModel.alternarGrabacion()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        when (val estadoActual = estado) {
            is EstadoLeccion.Cargando -> CargandoLeccion()
            is EstadoLeccion.Error -> ErrorLeccion(
                mensaje = estadoActual.mensaje,
                onReintentar = leccionViewModel::reintentar,
                onSalir = onSalir,
            )
            is EstadoLeccion.EnCurso -> ContenidoLeccion(
                estado = estadoActual,
                onSalir = onSalir,
                onSeleccionarOpcion = leccionViewModel::seleccionarOpcion,
                onActualizarTexto = leccionViewModel::actualizarTexto,
                onAgregarElemento = leccionViewModel::agregarElemento,
                onQuitarUltimoElemento = leccionViewModel::quitarUltimoElemento,
                onComprobar = leccionViewModel::comprobar,
                onContinuar = leccionViewModel::continuar,
                onReproducirAudio = leccionViewModel::reproducirAudio,
                onGrabar = {
                    val permisoConcedido = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (permisoConcedido || estadoActual.grabando) {
                        leccionViewModel.alternarGrabacion()
                    } else {
                        lanzadorPermisoAudio.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onReproducirMiGrabacion = leccionViewModel::reproducirMiGrabacion,
            )
            is EstadoLeccion.Finalizada -> ResultadoLeccion(
                correctas = estadoActual.correctas,
                totalEvaluables = estadoActual.totalEvaluables,
                onVolver = onSalir,
            )
        }
    }
}

@Composable
private fun CargandoLeccion(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Cargando lección...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorLeccion(
    mensaje: String,
    onReintentar: () -> Unit,
    onSalir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onReintentar) { Text("Reintentar") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSalir) { Text("Volver") }
        }
    }
}

@Composable
private fun ContenidoLeccion(
    estado: EstadoLeccion.EnCurso,
    onSalir: () -> Unit,
    onSeleccionarOpcion: (String) -> Unit,
    onActualizarTexto: (String) -> Unit,
    onAgregarElemento: (String) -> Unit,
    onQuitarUltimoElemento: () -> Unit,
    onComprobar: () -> Unit,
    onContinuar: () -> Unit,
    onReproducirAudio: () -> Unit,
    onGrabar: () -> Unit,
    onReproducirMiGrabacion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actividad = estado.actividadActual
    val esContenido = actividad.tipo in TIPOS_CONTENIDO

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onSalir)
                    .padding(end = 12.dp),
            )
            BarraProgresoLeccion(
                total = estado.actividades.size,
                indiceActual = estado.indice,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (actividad.tipo) {
                "SELECCION" -> TarjetaSeleccion(
                    actividad = actividad,
                    seleccionada = estado.respuestaSeleccionada,
                    verificada = estado.verificada,
                    onSeleccionar = onSeleccionarOpcion,
                )
                "COMPLETAR" -> TarjetaCompletar(
                    actividad = actividad,
                    texto = estado.textoIngresado,
                    verificada = estado.verificada,
                    onCambiarTexto = onActualizarTexto,
                )
                "ORDENAR" -> TarjetaOrdenar(
                    actividad = actividad,
                    ordenActual = estado.ordenActual,
                    elementosDisponibles = estado.elementosDisponibles,
                    verificada = estado.verificada,
                    onAgregarElemento = onAgregarElemento,
                    onQuitarUltimoElemento = onQuitarUltimoElemento,
                )
                else -> TarjetaContenido(
                    actividad = actividad,
                    reproduciendoAudio = estado.reproduciendoAudio,
                    grabando = estado.grabando,
                    grabacionDisponible = estado.grabacionDisponible,
                    onReproducirAudio = onReproducirAudio,
                    onGrabar = onGrabar,
                    onReproducirMiGrabacion = onReproducirMiGrabacion,
                )
            }
        }

        if (estado.verificada) {
            PanelRetroalimentacion(actividad = actividad, esCorrecta = estado.esCorrecta == true)
            Spacer(Modifier.height(12.dp))
        }

        BotonInferior(
            estado = estado,
            esContenido = esContenido,
            onComprobar = onComprobar,
            onContinuar = onContinuar,
        )
    }
}

@Composable
private fun BarraProgresoLeccion(
    total: Int,
    indiceActual: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (i <= indiceActual) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}

@Composable
private fun TarjetaContenido(
    actividad: Actividad,
    reproduciendoAudio: Boolean,
    grabando: Boolean,
    grabacionDisponible: Boolean,
    onReproducirAudio: () -> Unit,
    onGrabar: () -> Unit,
    onReproducirMiGrabacion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hayAudio = actividad.audioLocal.isNotBlank() || actividad.audioUrl.isNotBlank()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = actividad.quechua,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = actividad.traduccion,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actividad.explicacion.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = actividad.explicacion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        if (hayAudio) {
            Button(onClick = onReproducirAudio) {
                Text(if (reproduciendoAudio) "⏸ Reproduciendo..." else "▶ Escuchar")
            }
            Spacer(Modifier.height(8.dp))
        }

        if (actividad.tipo == "PRONUNCIACION") {
            OutlinedButton(onClick = onGrabar) {
                Text(if (grabando) "⏹ Detener grabación" else "🎙 Grabar mi voz")
            }
            if (grabacionDisponible) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onReproducirMiGrabacion) {
                    Text("▶ Escuchar mi grabación")
                }
            }
        }
    }
}

@Composable
private fun TarjetaSeleccion(
    actividad: Actividad,
    seleccionada: String?,
    verificada: Boolean,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = actividad.enunciado,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            actividad.opciones.forEach { opcion ->
                OpcionTarjeta(
                    texto = opcion,
                    seleccionada = seleccionada == opcion,
                    habilitada = !verificada,
                    onClick = { onSeleccionar(opcion) },
                )
            }
        }
    }
}

@Composable
private fun OpcionTarjeta(
    texto: String,
    seleccionada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (seleccionada) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = habilitada, onClick = onClick),
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(16.dp),
            color = if (seleccionada) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun TarjetaCompletar(
    actividad: Actividad,
    texto: String,
    verificada: Boolean,
    onCambiarTexto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = actividad.enunciado,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = texto,
            onValueChange = onCambiarTexto,
            enabled = !verificada,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TarjetaOrdenar(
    actividad: Actividad,
    ordenActual: List<String>,
    elementosDisponibles: List<String>,
    verificada: Boolean,
    onAgregarElemento: (String) -> Unit,
    onQuitarUltimoElemento: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = actividad.enunciado,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Tu respuesta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ordenActual) { elemento ->
                Chip(texto = elemento, habilitada = !verificada, onClick = onQuitarUltimoElemento)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Elementos disponibles",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(elementosDisponibles) { elemento ->
                Chip(texto = elemento, habilitada = !verificada, onClick = { onAgregarElemento(elemento) })
            }
        }
    }
}

@Composable
private fun Chip(
    texto: String,
    habilitada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(enabled = habilitada, onClick = onClick),
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PanelRetroalimentacion(
    actividad: Actividad,
    esCorrecta: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (esCorrecta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val respuestaCorrecta = if (actividad.respuestasCorrectas.isNotEmpty()) {
        actividad.respuestasCorrectas.joinToString(" ")
    } else {
        actividad.respuestaCorrecta
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (esCorrecta) "¡Correcto!" else "No es correcto. La respuesta era: $respuestaCorrecta",
                fontWeight = FontWeight.Bold,
                color = color,
            )
            if (actividad.explicacion.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = actividad.explicacion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BotonInferior(
    estado: EstadoLeccion.EnCurso,
    esContenido: Boolean,
    onComprobar: () -> Unit,
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actividad = estado.actividadActual

    if (esContenido) {
        Button(onClick = onContinuar, modifier = modifier.fillMaxWidth()) {
            Text(if (estado.esUltima) "Finalizar" else "Continuar")
        }
        return
    }

    if (!estado.verificada) {
        val hayRespuesta = when (actividad.tipo) {
            "SELECCION" -> estado.respuestaSeleccionada != null
            "COMPLETAR" -> estado.textoIngresado.isNotBlank()
            "ORDENAR" -> estado.ordenActual.isNotEmpty()
            else -> false
        }
        Button(
            onClick = onComprobar,
            enabled = hayRespuesta,
            modifier = modifier.fillMaxWidth(),
        ) { Text("Comprobar") }
    } else {
        Button(onClick = onContinuar, modifier = modifier.fillMaxWidth()) {
            Text(if (estado.esUltima) "Ver resultado" else "Continuar")
        }
    }
}

@Composable
private fun ResultadoLeccion(
    correctas: Int,
    totalEvaluables: Int,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⭐", fontSize = 44.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "¡Lección completada!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            if (totalEvaluables > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$correctas de $totalEvaluables respuestas correctas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onVolver,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) { Text("Volver") }
        }
    }
}
