package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proyect.qchuago.data.entidad.EntradaDiccionario
import com.proyect.qchuago.ui.screens.leccion.OpcionTarjeta
import com.proyect.qchuago.ui.viewmodel.DesafioViewModel
import com.proyect.qchuago.ui.viewmodel.DireccionTraduccion

/**
 * Desafío de traducción con el diccionario local: ventana flotante sobre
 * [DiccionarioScreen] (no reemplaza la pantalla ni navega a ninguna ruta nueva).
 */
@Composable
fun DesafioDiccionario(
    entradas: List<EntradaDiccionario>,
    onCerrar: () -> Unit,
) {
    val viewModel: DesafioViewModel = viewModel(
        factory = viewModelFactory { initializer { DesafioViewModel(entradas) } },
    )
    val estado by viewModel.estado.collectAsState()

    Dialog(onDismissRequest = onCerrar) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎯 Desafío",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "✕",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onCerrar),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Aciertos: ${estado.aciertos}/${estado.intentos}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = if (estado.direccion == DireccionTraduccion.QUECHUA_A_ESPANOL) {
                        "Traduce al español:"
                    } else {
                        "Traduce al quechua:"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = estado.enunciado,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    estado.opciones.forEach { opcion ->
                        OpcionTarjeta(
                            texto = opcion,
                            seleccionada = estado.seleccionada == opcion,
                            habilitada = !estado.verificada,
                            onClick = { viewModel.seleccionar(opcion) },
                        )
                    }
                }
                if (estado.verificada) {
                    Spacer(Modifier.height(16.dp))
                    val color = if (estado.esCorrecta) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = if (estado.esCorrecta) {
                            "¡Correcto!"
                        } else {
                            "No era. La respuesta era: ${estado.respuestaCorrecta}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = color,
                    )
                    if (estado.entrada.nota.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "💡 ${estado.entrada.nota}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { if (estado.verificada) viewModel.siguiente() else viewModel.comprobar() },
                    enabled = estado.verificada || estado.seleccionada != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (estado.verificada) "Siguiente" else "Comprobar")
                }
            }
        }
    }
}
