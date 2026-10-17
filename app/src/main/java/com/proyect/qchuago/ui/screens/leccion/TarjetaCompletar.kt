package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.proyect.qchuago.data.entidad.Actividad

/**
 * Ejercicio de completar frases con texto libre. Si la actividad trae [Actividad.opciones],
 * se muestra además un "banco de palabras" tocable que escribe la palabra completa en el campo.
 */
@Composable
internal fun TarjetaCompletar(
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
        if (actividad.opciones.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Banco de palabras",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(actividad.opciones) { opcion ->
                    Chip(texto = opcion, habilitada = !verificada, onClick = { onCambiarTexto(opcion) })
                }
            }
        }
    }
}
