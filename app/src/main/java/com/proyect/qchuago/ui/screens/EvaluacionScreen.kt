package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proyect.qchuago.ui.screens.leccion.BarraProgresoLeccion
import com.proyect.qchuago.ui.screens.leccion.TarjetaSeleccion
import com.proyect.qchuago.ui.viewmodel.EstadoEvaluacion
import com.proyect.qchuago.ui.viewmodel.EvaluacionViewModel

@Composable
fun EvaluacionScreen(
    unidadId: String,
    uid: String?,
    onResultadoGuardado: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val evaluacionViewModel: EvaluacionViewModel = viewModel(
        key = "evaluacion/$unidadId",
        factory = viewModelFactory {
            initializer { EvaluacionViewModel(unidadId, uid) }
        },
    )
    val estado by evaluacionViewModel.estado.collectAsState()

    LaunchedEffect(estado) {
        val resultado = estado as? EstadoEvaluacion.Resultado
        if (resultado != null && !resultado.guardando && resultado.error == null) {
            onResultadoGuardado()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
    ) {
        when (val actual = estado) {
            is EstadoEvaluacion.Cargando -> PantallaCargando(texto = "Cargando evaluación...")
            is EstadoEvaluacion.Error -> PantallaError(
                mensaje = actual.mensaje,
                onReintentar = evaluacionViewModel::cargarPreguntas,
                onVolver = onVolver,
            )
            is EstadoEvaluacion.EnCurso -> ContenidoEvaluacion(
                estado = actual,
                onSeleccionar = evaluacionViewModel::seleccionar,
                onContinuar = evaluacionViewModel::continuar,
                onVolver = onVolver,
            )
            is EstadoEvaluacion.Resultado -> ResultadoEvaluacion(
                estado = actual,
                onRepetir = evaluacionViewModel::repetir,
                onReintentar = evaluacionViewModel::reintentarGuardado,
                onVolver = onVolver,
            )
        }
    }
}

@Composable
private fun ContenidoEvaluacion(
    estado: EstadoEvaluacion.EnCurso,
    onSeleccionar: (String) -> Unit,
    onContinuar: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(onClick = onVolver)
                    .padding(end = 12.dp),
            )
            BarraProgresoLeccion(
                total = estado.preguntas.size,
                indiceActual = estado.indice,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = estado.nombreUnidad,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Pregunta ${estado.indice + 1} de ${estado.preguntas.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.weight(1f)) {
            TarjetaSeleccion(
                actividad = estado.preguntaActual,
                seleccionada = estado.seleccionada,
                verificada = false,
                onSeleccionar = onSeleccionar,
            )
        }
        Button(
            onClick = onContinuar,
            enabled = estado.seleccionada != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (estado.esUltima) "Finalizar" else "Continuar")
        }
    }
}

@Composable
private fun ResultadoEvaluacion(
    estado: EstadoEvaluacion.Resultado,
    onRepetir: () -> Unit,
    onReintentar: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (estado.aprobada) "✅" else "📚", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (estado.aprobada) "¡Evaluación aprobada!" else "Sigue practicando",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${estado.porcentaje}%",
            style = MaterialTheme.typography.displaySmall,
            color = if (estado.aprobada) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = "${estado.correctas} de ${estado.total} respuestas correctas",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        if (estado.guardando) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Guardando resultado...")
        } else if (estado.error != null) {
            Text(
                text = estado.error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onReintentar) { Text("Reintentar guardado") }
        } else if (estado.aprobada) {
            Button(onClick = onVolver) { Text("Volver al inicio") }
        } else {
            Button(onClick = onRepetir) { Text("Repetir evaluación") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onVolver) { Text("Volver al inicio") }
        }
    }
}
