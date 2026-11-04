package com.proyect.qchuago.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.TipoActividad
import com.proyect.qchuago.ui.screens.leccion.BarraProgresoLeccion
import com.proyect.qchuago.ui.screens.leccion.BotonInferior
import com.proyect.qchuago.ui.screens.leccion.PanelRetroalimentacion
import com.proyect.qchuago.ui.screens.leccion.ResultadoLeccion
import com.proyect.qchuago.ui.screens.leccion.TarjetaAsociar
import com.proyect.qchuago.ui.screens.leccion.TarjetaCompletar
import com.proyect.qchuago.ui.screens.leccion.TarjetaContenido
import com.proyect.qchuago.ui.screens.leccion.TarjetaOrdenar
import com.proyect.qchuago.ui.screens.leccion.TarjetaSeleccion
import com.proyect.qchuago.ui.viewmodel.EstadoLeccion
import com.proyect.qchuago.ui.viewmodel.LeccionViewModel

private val TIPOS_CONTENIDO = setOf(TipoActividad.VOCABULARIO, TipoActividad.PRONUNCIACION)

@Composable
fun LeccionScreen(
    unidadId: String,
    leccionId: String,
    onSalir: (completada: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    actividadesEstaticas: List<Actividad>? = null,
    uid: String? = null,
) {
    val context = LocalContext.current
    val leccionViewModel: LeccionViewModel = viewModel(
        key = "$unidadId/$leccionId",
        factory = viewModelFactory {
            initializer {
                LeccionViewModel(
                    unidadId = unidadId,
                    leccionId = leccionId,
                    context = context.applicationContext,
                    actividadesEstaticas = actividadesEstaticas,
                    uid = uid,
                )
            }
        },
    )
    val estado by leccionViewModel.estadoLeccion.collectAsState()

    val lanzadorPermisoAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) leccionViewModel.alternarGrabacion()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp),
    ) {
        when (val estadoActual = estado) {
            is EstadoLeccion.Cargando -> PantallaCargando(texto = "Cargando lección...")
            is EstadoLeccion.Error -> PantallaError(
                mensaje = estadoActual.mensaje,
                onReintentar = leccionViewModel::reintentar,
                onVolver = { onSalir(false) },
            )
            is EstadoLeccion.EnCurso -> ContenidoLeccion(
                estado = estadoActual,
                onSalir = { onSalir(false) },
                onSeleccionarOpcion = leccionViewModel::seleccionarOpcion,
                onActualizarTexto = leccionViewModel::actualizarTexto,
                onAgregarElemento = leccionViewModel::agregarElemento,
                onQuitarUltimoElemento = leccionViewModel::quitarUltimoElemento,
                onSeleccionarElementoAsociar = leccionViewModel::seleccionarElementoAsociar,
                onSeleccionarOpcionAsociar = leccionViewModel::seleccionarOpcionAsociar,
                onComprobar = leccionViewModel::comprobar,
                onContinuar = leccionViewModel::continuar,
                onReproducirAudio = leccionViewModel::reproducirAudio,
                onGrabar = {
                    val permisoConcedido = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (permisoConcedido || estadoActual.grabando) {
                        leccionViewModel.alternarGrabacion()
                    } else {
                        lanzadorPermisoAudio.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onReproducirMiGrabacion = leccionViewModel::reproducirMiGrabacion,
            )
            is EstadoLeccion.Finalizada -> ResultadoLeccion(
                correctas = estadoActual.correctas,
                totalEvaluables = estadoActual.totalEvaluables,
                onVolver = { onSalir(true) },
            )
        }
    }
}

@Composable
private fun ContenidoLeccion(
    estado: EstadoLeccion.EnCurso,
    onSalir: () -> Unit,
    onSeleccionarOpcion: (String) -> Unit,
    onActualizarTexto: (String) -> Unit,
    onAgregarElemento: (String) -> Unit,
    onQuitarUltimoElemento: () -> Unit,
    onSeleccionarElementoAsociar: (Int) -> Unit,
    onSeleccionarOpcionAsociar: (Int) -> Unit,
    onComprobar: () -> Unit,
    onContinuar: () -> Unit,
    onReproducirAudio: () -> Unit,
    onGrabar: () -> Unit,
    onReproducirMiGrabacion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actividad = estado.actividadActual
    val esContenido = actividad.tipo in TIPOS_CONTENIDO

    Column(modifier = modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "✕",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onSalir)
                    .padding(end = 12.dp),
            )
            BarraProgresoLeccion(
                total = estado.actividades.size,
                indiceActual = estado.indice,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (actividad.tipo) {
                TipoActividad.SELECCION -> TarjetaSeleccion(
                    actividad = actividad,
                    seleccionada = estado.respuestaSeleccionada,
                    verificada = estado.verificada,
                    onSeleccionar = onSeleccionarOpcion,
                )
                TipoActividad.COMPLETAR -> TarjetaCompletar(
                    actividad = actividad,
                    texto = estado.textoIngresado,
                    verificada = estado.verificada,
                    onCambiarTexto = onActualizarTexto,
                )
                TipoActividad.ORDENAR -> TarjetaOrdenar(
                    actividad = actividad,
                    ordenActual = estado.ordenActual,
                    elementosDisponibles = estado.elementosDisponibles,
                    verificada = estado.verificada,
                    onAgregarElemento = onAgregarElemento,
                    onQuitarUltimoElemento = onQuitarUltimoElemento,
                )
                TipoActividad.ASOCIAR -> TarjetaAsociar(
                    actividad = actividad,
                    indiceElementoSeleccionado = estado.indiceElementoSeleccionado,
                    pares = estado.pares,
                    verificada = estado.verificada,
                    onSeleccionarElemento = onSeleccionarElementoAsociar,
                    onSeleccionarOpcion = onSeleccionarOpcionAsociar,
                )
                TipoActividad.VOCABULARIO, TipoActividad.PRONUNCIACION -> TarjetaContenido(
                    actividad = actividad,
                    reproduciendoAudio = estado.reproduciendoAudio,
                    grabando = estado.grabando,
                    grabacionDisponible = estado.grabacionDisponible,
                    onReproducirAudio = onReproducirAudio,
                    onGrabar = onGrabar,
                    onReproducirMiGrabacion = onReproducirMiGrabacion,
                )
            }
        }

        if (estado.verificada) {
            PanelRetroalimentacion(actividad = actividad, esCorrecta = estado.esCorrecta == true)
            Spacer(Modifier.height(12.dp))
        }

        BotonInferior(
            estado = estado,
            esContenido = esContenido,
            onComprobar = onComprobar,
            onContinuar = onContinuar,
        )
    }
}
