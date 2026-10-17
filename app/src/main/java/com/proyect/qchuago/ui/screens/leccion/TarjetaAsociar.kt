package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/**
 * Ejercicio de asociación de palabras: emparejar cada elemento de la columna izquierda
 * (quechua) con su traducción en la columna derecha (mezclada). El emparejamiento se
 * modela por índice en [pares] (índice de [Actividad.elementos] -> índice de [Actividad.opciones])
 * para no depender de que las traducciones sean únicas como texto.
 */
@Composable
internal fun TarjetaAsociar(
    actividad: Actividad,
    indiceElementoSeleccionado: Int?,
    pares: Map<Int, Int>,
    verificada: Boolean,
    onSeleccionarElemento: (Int) -> Unit,
    onSeleccionarOpcion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = actividad.enunciado,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actividad.elementos.forEachIndexed { indice, elemento ->
                    OpcionTarjeta(
                        texto = elemento,
                        seleccionada = pares.containsKey(indice) || indiceElementoSeleccionado == indice,
                        habilitada = !verificada,
                        onClick = { onSeleccionarElemento(indice) },
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                actividad.opciones.forEachIndexed { indice, opcion ->
                    OpcionTarjeta(
                        texto = opcion,
                        seleccionada = pares.containsValue(indice),
                        habilitada = !verificada,
                        onClick = { onSeleccionarOpcion(indice) },
                    )
                }
            }
        }
    }
}
