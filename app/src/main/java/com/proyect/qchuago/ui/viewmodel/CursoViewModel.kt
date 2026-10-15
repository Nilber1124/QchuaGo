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
    data class SeleccionNivel(val niveles: List<Nivel>) : EstadoCurso()
    data class Exito(val nivel: Nivel, val unidades: List<Unidad>) : EstadoCurso()
    data class Error(val mensaje: String, val nivel: Nivel? = null) : EstadoCurso()
}

class CursoViewModel(
    private val cursoService: CursoService = CursoService(),
) : ViewModel() {

    private val _estadoCurso = MutableStateFlow<EstadoCurso>(EstadoCurso.Cargando)
    val estadoCurso: StateFlow<EstadoCurso> = _estadoCurso.asStateFlow()
    private var niveles: List<Nivel> = emptyList()

    init {
        cargarNiveles()
    }

    fun cargarNiveles() {
        _estadoCurso.value = EstadoCurso.Cargando
        viewModelScope.launch {
            cursoService.obtenerNiveles()
                .onSuccess {
                    niveles = it
                    _estadoCurso.value = EstadoCurso.SeleccionNivel(it)
                }
                .onFailure {
                    _estadoCurso.value = EstadoCurso.Error(
                        "No se pudieron cargar los niveles. Inténtalo nuevamente."
                    )
                }
        }
    }

    fun seleccionarNivel(nivel: Nivel) {
        if (!estaDesbloqueado(nivel)) return

        _estadoCurso.value = EstadoCurso.Cargando
        viewModelScope.launch {
            cursoService.obtenerUnidades(nivel.id)
                .onSuccess { _estadoCurso.value = EstadoCurso.Exito(nivel, it) }
                .onFailure {
                    _estadoCurso.value = EstadoCurso.Error(
                        mensaje = "No se pudieron cargar las unidades. Inténtalo nuevamente.",
                        nivel = nivel,
                    )
                }
        }
    }

    fun volverANiveles() {
        _estadoCurso.value = EstadoCurso.SeleccionNivel(niveles)
    }

    fun reintentar() {
        val nivel = (_estadoCurso.value as? EstadoCurso.Error)?.nivel
        if (nivel == null) cargarNiveles() else seleccionarNivel(nivel)
    }

    fun estaDesbloqueado(nivel: Nivel): Boolean = niveles.firstOrNull()?.id == nivel.id

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
