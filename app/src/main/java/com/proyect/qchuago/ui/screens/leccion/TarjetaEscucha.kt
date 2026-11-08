package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.qchuago.data.entidad.Actividad

/**
 * Ejercicio de escucha: el estudiante oye un audio y elige, entre varias opciones,
 * la que corresponde a lo que escuchó. A diferencia de [TarjetaContenido], nunca se
 * muestra el texto en quechua ni su traducción, para que la respuesta dependa
 * únicamente del audio.
 */
@Composable
internal fun TarjetaEscucha(
    actividad: Actividad,
    seleccionada: String?,
    verificada: Boolean,
    reproduciendoAudio: Boolean,
    onSeleccionar: (String) -> Unit,
    onReproducirAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(actividad.id) { onReproducirAudio() }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = actividad.enunciado,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onReproducirAudio) {
            Text(if (reproduciendoAudio) "⏸ Reproduciendo..." else "▶ Escuchar de nuevo")
        }
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
