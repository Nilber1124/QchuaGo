package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.TipoActividad

/** Retroalimentación inmediata tras comprobar una respuesta: correcta/incorrecta + solución + explicación. */
@Composable
internal fun PanelRetroalimentacion(
    actividad: Actividad,
    esCorrecta: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (esCorrecta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val respuestaCorrecta = when {
        actividad.tipo == TipoActividad.ASOCIAR -> actividad.elementos
            .zip(actividad.respuestasCorrectas)
            .joinToString("; ") { (elemento, traduccion) -> "$elemento = $traduccion" }
        actividad.respuestasCorrectas.isNotEmpty() -> actividad.respuestasCorrectas.joinToString(" ")
        else -> actividad.respuestaCorrecta
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
