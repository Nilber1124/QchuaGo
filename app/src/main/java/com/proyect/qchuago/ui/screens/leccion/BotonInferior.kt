package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.proyect.qchuago.data.entidad.TipoActividad
import com.proyect.qchuago.ui.viewmodel.EstadoLeccion

/** Botón inferior de la lección: "Continuar" para contenido, "Comprobar"/"Continuar" para ejercicios. */
@Composable
internal fun BotonInferior(
    estado: EstadoLeccion.EnCurso,
    esContenido: Boolean,
    onComprobar: () -> Unit,
    onContinuar: () -> Unit,
    onReintentarPronunciacion: () -> Unit,
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
            TipoActividad.SELECCION -> estado.respuestaSeleccionada != null
            TipoActividad.COMPLETAR -> estado.textoIngresado.isNotBlank()
            TipoActividad.ORDENAR -> estado.ordenActual.isNotEmpty()
            TipoActividad.ASOCIAR -> actividad.elementos.isNotEmpty() && estado.pares.size == actividad.elementos.size
            TipoActividad.ESCUCHA -> estado.respuestaSeleccionada != null
            TipoActividad.PRONUNCIACION -> estado.grabacionDisponible
            TipoActividad.VOCABULARIO -> false
        }
        Button(
            onClick = onComprobar,
            enabled = hayRespuesta && !estado.evaluandoPronunciacion,
            modifier = modifier.fillMaxWidth(),
        ) { Text(if (estado.evaluandoPronunciacion) "Evaluando..." else "Comprobar") }
    } else if (actividad.tipo == TipoActividad.PRONUNCIACION) {
        Column(modifier = modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onReintentarPronunciacion, modifier = Modifier.fillMaxWidth()) {
                Text("🔄 Intentar de nuevo")
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth()) {
                Text(if (estado.esUltima) "Ver resultado" else "Continuar")
            }
        }
    } else {
        Button(onClick = onContinuar, modifier = modifier.fillMaxWidth()) {
            Text(if (estado.esUltima) "Ver resultado" else "Continuar")
        }
    }
}
