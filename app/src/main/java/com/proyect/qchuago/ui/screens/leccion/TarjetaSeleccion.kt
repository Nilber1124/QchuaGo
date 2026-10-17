package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.qchuago.data.entidad.Actividad

/** Ejercicio de selección múltiple: elegir una de varias opciones. */
@Composable
internal fun TarjetaSeleccion(
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
