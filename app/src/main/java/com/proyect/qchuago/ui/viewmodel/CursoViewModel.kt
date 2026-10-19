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

/**
 * Modelo simple que agrupa los cálculos de avance del curso para que
 * las pantallas (Progreso, Perfil e Inicio) no tengan que repetir lógica de colecciones.
 */
data class ResumenProgresoCurso(
    val totalLecciones: Int = 0,
    val leccionesCompletadas: Int = 0,
    val unidadesCompletadas: Int = 0,
    val siguienteLeccion: Pair<Unidad, Leccion>? = null,
) {
    /** Porcentaje de 0 a 100 para mostrar en barras de progreso. */
    val porcentaje: Int
        get() = if (totalLecciones > 0) ((leccionesCompletadas.toFloat() / totalLecciones) * 100).toInt() else 0

    /** Fracción de 0.0 a 1.0 para el LinearProgressIndicator de Compose. */
    val progresoFraccion: Float
        get() = if (totalLecciones > 0) leccionesCompletadas.toFloat() / totalLecciones else 0f
}

class CursoViewModel(
    private val cursoService: CursoService = CursoService(),
) : ViewModel() {

    private val _estadoCurso = MutableStateFlow<EstadoCurso>(EstadoCurso.Cargando)
    val estadoCurso: StateFlow<EstadoCurso> = _estadoCurso.asStateFlow()

    init {
        cargarCurso()
    }

    /**
     * Carga todas las unidades del curso desde Firestore.
     */
    fun cargarCurso() {
        _estadoCurso.value = EstadoCurso.Cargando
        viewModelScope.launch {
            cursoService.obtenerUnidades()
                .onSuccess { _estadoCurso.value = EstadoCurso.Exito(it) }
                .onFailure {
                    _estadoCurso.value = EstadoCurso.Error(
                        "No se pudieron cargar las unidades. Inténtalo nuevamente."
                    )
                }
        }
    }

    fun reintentar() = cargarCurso()

    /**
     * Calcula métricas agregadas del curso en una sola llamada.
     * Facilita el entendimiento del código para principiantes y evita duplicar código en las pantallas.
     */
    fun calcularResumenProgreso(
        unidades: List<Unidad>,
        leccionesCompletadasIds: List<String>,
    ): ResumenProgresoCurso {
        val totalLecciones = unidades.sumOf { it.lecciones.size }

        val leccionesCompletadas = unidades.sumOf { unidad ->
            unidad.lecciones.count { estaCompletada(unidad, it, leccionesCompletadasIds) }
        }

        val unidadesCompletadas = unidades.count { unidad ->
            unidad.lecciones.isNotEmpty() && unidad.lecciones.all {
                estaCompletada(unidad, it, leccionesCompletadasIds)
            }
        }

        // La siguiente lección pendiente es la primera que aún no esté completada
        val siguienteLeccion = unidades
            .flatMap { unidad -> unidad.lecciones.map { unidad to it } }
            .firstOrNull { (unidad, leccion) ->
                !estaCompletada(unidad, leccion, leccionesCompletadasIds)
            }

        return ResumenProgresoCurso(
            totalLecciones = totalLecciones,
            leccionesCompletadas = leccionesCompletadas,
            unidadesCompletadas = unidadesCompletadas,
            siguienteLeccion = siguienteLeccion,
        )
    }

    /**
     * Una lección está desbloqueada si:
     * 1. Todas las unidades anteriores están completas.
     * 2. Todas las lecciones anteriores dentro de la misma unidad están completas.
     */
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

        val unidadesPreviasListas = unidadesPrevias.all { anterior ->
            anterior.lecciones.all { estaCompletada(anterior, it, leccionesCompletadasIds) }
        }
        val leccionesPreviasListas = leccionesPrevias.all {
            estaCompletada(unidadActual, it, leccionesCompletadasIds)
        }

        return unidadesPreviasListas && leccionesPreviasListas
    }

    /**
     * Una unidad está desbloqueada si todas las lecciones de las unidades anteriores están completas.
     */
    fun estaDesbloqueadaUnidad(unidad: Unidad, leccionesCompletadasIds: List<String>): Boolean {
        val estado = _estadoCurso.value as? EstadoCurso.Exito ?: return false
        val unidadesPrevias = estado.unidades.takeWhile { it.id != unidad.id }
        return unidadesPrevias.all { anterior ->
            anterior.lecciones.all { estaCompletada(anterior, it, leccionesCompletadasIds) }
        }
    }

    /**
     * Comprueba si una lección específica ya fue completada por el usuario.
     */
    fun estaCompletada(
        unidad: Unidad,
        leccion: Leccion,
        leccionesCompletadasIds: List<String>,
    ): Boolean = leccion.claveEn(unidad.id) in leccionesCompletadasIds
}
