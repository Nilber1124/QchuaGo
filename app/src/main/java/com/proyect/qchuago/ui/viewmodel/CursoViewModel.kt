package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Unidad
import com.proyect.qchuago.data.servicio.CursoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EstadoCurso {
    object Cargando : EstadoCurso()
    data class Exito(val unidades: List<Unidad>) : EstadoCurso()
    data class Error(val mensaje: String) : EstadoCurso()
}

class CursoViewModel(
    private val cursoService: CursoService = CursoService(),
) : ViewModel() {

    private val _estadoCurso = MutableStateFlow<EstadoCurso>(EstadoCurso.Cargando)
    val estadoCurso: StateFlow<EstadoCurso> = _estadoCurso.asStateFlow()

    init {
        cargarUnidades()
    }

    fun cargarUnidades() {
        _estadoCurso.value = EstadoCurso.Cargando
        viewModelScope.launch {
            cursoService.obtenerUnidades()
                .onSuccess { _estadoCurso.value = EstadoCurso.Exito(it) }
                .onFailure { _estadoCurso.value = EstadoCurso.Error("No se pudo cargar el curso. Inténtalo nuevamente.") }
        }
    }

    fun estaDesbloqueada(leccion: Leccion, leccionesCompletadasIds: List<String>): Boolean {
        val estado = _estadoCurso.value as? EstadoCurso.Exito ?: return false
        for (unidad in estado.unidades) {
            val indice = unidad.lecciones.indexOfFirst { it.id == leccion.id }
            if (indice == -1) continue
            // Todas las lecciones anteriores en esta unidad deben estar completadas
            val leccionesPrevias = unidad.lecciones.take(indice)
            val unidadesPrevias = estado.unidades.takeWhile { it.id != unidad.id }
            val todasLasPreviasCompletadas =
                unidadesPrevias.all { u -> u.lecciones.all { it.id in leccionesCompletadasIds } } &&
                leccionesPrevias.all { it.id in leccionesCompletadasIds }
            return todasLasPreviasCompletadas
        }
        return false
    }

    fun estaDesbloqueadaUnidad(unidad: Unidad, leccionesCompletadasIds: List<String>): Boolean {
        val estado = _estadoCurso.value as? EstadoCurso.Exito ?: return false
        val unidadesPrevias = estado.unidades.takeWhile { it.id != unidad.id }
        return unidadesPrevias.all { u -> u.lecciones.all { it.id in leccionesCompletadasIds } }
    }
}
