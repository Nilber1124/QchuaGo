package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.TipoActividad

/** Contenido de vocabulario/pronunciación: muestra la palabra, su audio y, para PRONUNCIACION, grabación. */
@Composable
internal fun TarjetaContenido(
    actividad: Actividad,
    reproduciendoAudio: Boolean,
    grabando: Boolean,
    grabacionDisponible: Boolean,
    onReproducirAudio: () -> Unit,
    onGrabar: () -> Unit,
    onReproducirMiGrabacion: () -> Unit,
    modifier: Modifier = Modifier,
    evaluando: Boolean = false,
    errorEvaluacion: String? = null,
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

        if (actividad.tipo == TipoActividad.PRONUNCIACION) {
            OutlinedButton(onClick = onGrabar, enabled = !evaluando) {
                Text(if (grabando) "⏹ Detener grabación" else "🎙 Grabar mi voz")
            }
            if (grabacionDisponible) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onReproducirMiGrabacion, enabled = !evaluando) {
                    Text("▶ Escuchar mi grabación")
                }
            }
            if (evaluando) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.width(18.dp).height(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Evaluando pronunciación...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (errorEvaluacion != null) {
                Spacer(Modifier.height(12.dp))
                Text(text = errorEvaluacion, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
