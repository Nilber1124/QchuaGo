package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.servicio.AudioService
import com.proyect.qchuago.data.servicio.CursoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val TIPOS_EVALUABLES = setOf("SELECCION", "COMPLETAR", "ORDENAR")

sealed class EstadoLeccion {
    object Cargando : EstadoLeccion()
    data class Error(val mensaje: String) : EstadoLeccion()
    data class EnCurso(
        val actividades: List<Actividad>,
        val indice: Int,
        val correctas: Int,
        val totalEvaluables: Int,
        val respuestaSeleccionada: String? = null,
        val textoIngresado: String = "",
        val elementosDisponibles: List<String> = emptyList(),
        val ordenActual: List<String> = emptyList(),
        val verificada: Boolean = false,
        val esCorrecta: Boolean? = null,
        val reproduciendoAudio: Boolean = false,
        val grabando: Boolean = false,
        val grabacionDisponible: Boolean = false,
    ) : EstadoLeccion() {
        val actividadActual: Actividad get() = actividades[indice]
        val esUltima: Boolean get() = indice == actividades.lastIndex
    }
    data class Finalizada(val correctas: Int, val totalEvaluables: Int) : EstadoLeccion()
}

class LeccionViewModel(
    private val unidadId: String,
    private val leccionId: String,
    private val audioService: AudioService,
    private val cursoService: CursoService = CursoService(),
    private val actividadesEstaticas: List<Actividad>? = null,
) : ViewModel() {

    private val _estadoLeccion = MutableStateFlow<EstadoLeccion>(EstadoLeccion.Cargando)
    val estadoLeccion: StateFlow<EstadoLeccion> = _estadoLeccion.asStateFlow()

    init {
        cargarActividades()
    }

    fun cargarActividades() {
        _estadoLeccion.value = EstadoLeccion.Cargando
        val estaticas = actividadesEstaticas
        if (estaticas != null) {
            iniciarConActividades(estaticas)
            return
        }
        viewModelScope.launch {
            cursoService.obtenerActividades(unidadId, leccionId)
                .onSuccess { actividades -> iniciarConActividades(actividades) }
                .onFailure {
                    _estadoLeccion.value = EstadoLeccion.Error(
                        "No se pudo cargar la lección. Inténtalo nuevamente."
                    )
                }
        }
    }

    private fun iniciarConActividades(actividades: List<Actividad>) {
        if (actividades.isEmpty()) {
            _estadoLeccion.value = EstadoLeccion.Error("Esta lección todavía no tiene contenido.")
            return
        }
        _estadoLeccion.value = EstadoLeccion.EnCurso(
            actividades = actividades,
            indice = 0,
            correctas = 0,
            totalEvaluables = actividades.count { it.tipo in TIPOS_EVALUABLES },
            elementosDisponibles = actividades.first().elementos,
        )
    }

    fun reintentar() = cargarActividades()

    fun seleccionarOpcion(opcion: String) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        _estadoLeccion.value = estado.copy(respuestaSeleccionada = opcion)
    }

    fun actualizarTexto(texto: String) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        _estadoLeccion.value = estado.copy(textoIngresado = texto)
    }

    fun agregarElemento(elemento: String) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        _estadoLeccion.value = estado.copy(
            ordenActual = estado.ordenActual + elemento,
            elementosDisponibles = estado.elementosDisponibles - elemento,
        )
    }

    fun quitarUltimoElemento() {
        val estado = enCursoActual() ?: return
        if (estado.verificada || estado.ordenActual.isEmpty()) return
        val elemento = estado.ordenActual.last()
        _estadoLeccion.value = estado.copy(
            ordenActual = estado.ordenActual.dropLast(1),
            elementosDisponibles = estado.elementosDisponibles + elemento,
        )
    }

    fun comprobar() {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        val actividad = estado.actividadActual

        val esCorrecta = when (actividad.tipo) {
            "SELECCION" -> estado.respuestaSeleccionada == actividad.respuestaCorrecta
            "COMPLETAR" -> estado.textoIngresado.trim()
                .equals(actividad.respuestaCorrecta.trim(), ignoreCase = true)
            "ORDENAR" -> estado.ordenActual == actividad.respuestasCorrectas
            else -> return
        }

        _estadoLeccion.value = estado.copy(
            verificada = true,
            esCorrecta = esCorrecta,
            correctas = if (esCorrecta) estado.correctas + 1 else estado.correctas,
        )
    }

    fun continuar() {
        val estado = enCursoActual() ?: return
        if (estado.esUltima) {
            _estadoLeccion.value = EstadoLeccion.Finalizada(estado.correctas, estado.totalEvaluables)
            return
        }
        val siguienteIndice = estado.indice + 1
        _estadoLeccion.value = estado.copy(
            indice = siguienteIndice,
            respuestaSeleccionada = null,
            textoIngresado = "",
            elementosDisponibles = estado.actividades[siguienteIndice].elementos,
            ordenActual = emptyList(),
            verificada = false,
            esCorrecta = null,
            grabacionDisponible = false,
        )
    }

    fun reproducirAudio() {
        val estado = enCursoActual() ?: return
        val actividad = estado.actividadActual
        val reproduciendo = audioService.reproducir(actividad.audioLocal, actividad.audioUrl) {
            marcarAudioDetenido()
        }
        if (reproduciendo) {
            _estadoLeccion.value = estado.copy(reproduciendoAudio = true)
        }
    }

    fun alternarGrabacion() {
        val estado = enCursoActual() ?: return
        if (estado.grabando) {
            audioService.detenerGrabacion()
            _estadoLeccion.value = estado.copy(grabando = false, grabacionDisponible = true)
        } else {
            audioService.iniciarGrabacion()
            _estadoLeccion.value = estado.copy(grabando = true)
        }
    }

    fun reproducirMiGrabacion() {
        val estado = enCursoActual() ?: return
        val reproduciendo = audioService.reproducirGrabacion { marcarAudioDetenido() }
        if (reproduciendo) {
            _estadoLeccion.value = estado.copy(reproduciendoAudio = true)
        }
    }

    private fun marcarAudioDetenido() {
        val estado = enCursoActual() ?: return
        _estadoLeccion.value = estado.copy(reproduciendoAudio = false)
    }

    private fun enCursoActual(): EstadoLeccion.EnCurso? = _estadoLeccion.value as? EstadoLeccion.EnCurso

    override fun onCleared() {
        super.onCleared()
        audioService.liberar()
    }
}
