package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.qchuago.data.entidad.Actividad

/** Ejercicio de ordenar palabras para formar una frase, tocando los chips en orden. */
@Composable
internal fun TarjetaOrdenar(
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
