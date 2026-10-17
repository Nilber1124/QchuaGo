package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Nivel
import com.proyect.qchuago.data.entidad.Unidad
import com.proyect.qchuago.data.servicio.CursoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EstadoCurso {
    object Cargando : EstadoCurso()
    data class Exito(val nivel: Nivel, val unidades: List<Unidad>) : EstadoCurso()
    data class Error(val mensaje: String) : EstadoCurso()
}

class CursoViewModel(
    private val cursoService: CursoService = CursoService(),
) : ViewModel() {

    private val _estadoCurso = MutableStateFlow<EstadoCurso>(EstadoCurso.Cargando)
    val estadoCurso: StateFlow<EstadoCurso> = _estadoCurso.asStateFlow()

    init {
        cargarCurso()
    }

    fun cargarCurso() {
        _estadoCurso.value = EstadoCurso.Cargando
        viewModelScope.launch {
            cursoService.obtenerNiveles()
                .onSuccess { niveles ->
                    val nivel = niveles.firstOrNull()
                    if (nivel == null) {
                        _estadoCurso.value = EstadoCurso.Error("No hay niveles disponibles.")
                        return@onSuccess
                    }
                    cursoService.obtenerUnidades(nivel.id)
                        .onSuccess { _estadoCurso.value = EstadoCurso.Exito(nivel, it) }
                        .onFailure {
                            _estadoCurso.value = EstadoCurso.Error(
                                "No se pudieron cargar las unidades. Inténtalo nuevamente."
                            )
                        }
                }
                .onFailure {
                    _estadoCurso.value = EstadoCurso.Error(
                        "No se pudieron cargar los niveles. Inténtalo nuevamente."
                    )
                }
        }
    }

    fun reintentar() = cargarCurso()

    fun estaDesbloqueada(
        unidad: Unidad,
        leccion: Leccion,
        leccionesCompletadasIds: List<String>,
    ): Boolean {
        val estado = _estadoCurso.value as? EstadoCurso.Exito ?: return false
        val unidadActual = estado.unidades.firstOrNull { it.id == unidad.id } ?: return false
        val indice = unidadActual.lecciones.indexOfFirst { it.id == leccion.id }
        if (indice == -1) return false

        val unidadesPrevias = estado.unidades.takeWhile { it.id != unidadActual.id }
        val leccionesPrevias = unidadActual.lecciones.take(indice)
        return unidadesPrevias.all { anterior ->
            anterior.lecciones.all { estaCompletada(anterior, it, leccionesCompletadasIds) }
        } && leccionesPrevias.all {
            estaCompletada(unidadActual, it, leccionesCompletadasIds)
        }
    }

    fun estaDesbloqueadaUnidad(unidad: Unidad, leccionesCompletadasIds: List<String>): Boolean {
        val estado = _estadoCurso.value as? EstadoCurso.Exito ?: return false
        val unidadesPrevias = estado.unidades.takeWhile { it.id != unidad.id }
        return unidadesPrevias.all { anterior ->
            anterior.lecciones.all { estaCompletada(anterior, it, leccionesCompletadasIds) }
        }
    }

    fun estaCompletada(
        unidad: Unidad,
        leccion: Leccion,
        leccionesCompletadasIds: List<String>,
    ): Boolean = leccion.claveEn(unidad.id) in leccionesCompletadasIds
}
