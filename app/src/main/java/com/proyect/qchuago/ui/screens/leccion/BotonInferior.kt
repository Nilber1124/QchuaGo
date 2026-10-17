package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.proyect.qchuago.data.entidad.TipoActividad
import com.proyect.qchuago.ui.viewmodel.EstadoLeccion

/** Botón inferior de la lección: "Continuar" para contenido, "Comprobar"/"Continuar" para ejercicios. */
@Composable
internal fun BotonInferior(
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
            TipoActividad.SELECCION -> estado.respuestaSeleccionada != null
            TipoActividad.COMPLETAR -> estado.textoIngresado.isNotBlank()
            TipoActividad.ORDENAR -> estado.ordenActual.isNotEmpty()
            TipoActividad.ASOCIAR -> actividad.elementos.isNotEmpty() && estado.pares.size == actividad.elementos.size
            TipoActividad.VOCABULARIO, TipoActividad.PRONUNCIACION -> false
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
