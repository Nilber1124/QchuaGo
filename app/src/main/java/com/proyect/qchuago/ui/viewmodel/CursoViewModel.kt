package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Unidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Modelo simple que agrupa los cálculos de avance del curso para que
 * las pantallas (Progreso, Perfil e Inicio) no tengan que repetir lógica de colecciones.
 */
data class ResumenProgresoCurso(
    val totalLecciones: Int = 0,
    val leccionesCompletadas: Int = 0,
    val unidadesCompletadas: Int = 0,
    val siguienteLeccion: Pair<Unidad, Leccion>? = null,
    val siguienteEvaluacion: Unidad? = null,
) {
    /** Porcentaje de 0 a 100 para mostrar en barras de progreso. */
    val porcentaje: Int
        get() = if (totalLecciones > 0) ((leccionesCompletadas.toFloat() / totalLecciones) * 100).toInt() else 0

    /** Fracción de 0.0 a 1.0 para el LinearProgressIndicator de Compose. */
    val progresoFraccion: Float
        get() = if (totalLecciones > 0) leccionesCompletadas.toFloat() / totalLecciones else 0f
}

class CursoViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val unidadesCollection = firestore.collection("unidades")

    private val _estadoCurso = MutableStateFlow<Recurso<List<Unidad>>>(Recurso.Cargando)
    val estadoCurso: StateFlow<Recurso<List<Unidad>>> = _estadoCurso.asStateFlow()

    init {
        cargarCurso()
    }

    /**
     * Carga todas las unidades del curso desde Firestore.
     */
    fun cargarCurso() {
        _estadoCurso.value = Recurso.Cargando
        viewModelScope.launch {
            try {
                // 1) Todas las unidades, ordenadas por "orden".
                val documentosUnidades = unidadesCollection
                    .orderBy("orden", Query.Direction.ASCENDING)
                    .get().await()
                    .documents

                // 2) Para cada unidad, su subcolección "lecciones" (también ordenada).
                val unidades = documentosUnidades.map { documento ->
                    val unidad = documento.toObject(Unidad::class.java) ?: Unidad(id = documento.id)
                    val lecciones = documento.reference.collection("lecciones")
                        .orderBy("orden", Query.Direction.ASCENDING)
                        .get().await()
                        .toObjects(Leccion::class.java)
                    unidad.copy(lecciones = lecciones)
                }
                _estadoCurso.value = Recurso.Exito(unidades)
            } catch (e: Exception) {
                _estadoCurso.value = Recurso.Error(
                    "No se pudieron cargar las unidades. Inténtalo nuevamente."
                )
            }
        }
    }

    fun reintentar() = cargarCurso()

    /**
     * Resumen del avance del usuario en el curso (porcentaje, completadas, siguiente lección).
     * Si el curso todavía no cargó, devuelve un resumen vacío (todo en 0).
     *
     * Recibe el progreso de lecciones y las evaluaciones de unidad aprobadas.
     */
    fun resumen(
        leccionesCompletadasIds: List<String>,
        evaluacionesAprobadasIds: List<String>,
    ): ResumenProgresoCurso {
        val unidades = (_estadoCurso.value as? Recurso.Exito)?.datos ?: return ResumenProgresoCurso()
        return calcularResumenProgreso(unidades, leccionesCompletadasIds, evaluacionesAprobadasIds)
    }

    private fun calcularResumenProgreso(
        unidades: List<Unidad>,
        leccionesCompletadasIds: List<String>,
        evaluacionesAprobadasIds: List<String>,
    ): ResumenProgresoCurso {
        val totalLecciones = unidades.sumOf { it.lecciones.size }

        val leccionesCompletadas = unidades.sumOf { unidad ->
            unidad.lecciones.count { estaCompletada(unidad, it, leccionesCompletadasIds) }
        }

        val unidadesCompletadas = unidades.count { it.id in evaluacionesAprobadasIds }

        // La siguiente lección pendiente es la primera que aún no esté completada
        val siguienteLeccion = unidades
            .flatMap { unidad -> unidad.lecciones.map { unidad to it } }
            .firstOrNull { (unidad, leccion) ->
                !estaCompletada(unidad, leccion, leccionesCompletadasIds) &&
                    estaDesbloqueada(
                        unidad, leccion, leccionesCompletadasIds, evaluacionesAprobadasIds
                    )
            }

        val siguienteEvaluacion = unidades.firstOrNull { unidad ->
            unidad.id !in evaluacionesAprobadasIds &&
                puedeEvaluar(unidad, leccionesCompletadasIds) &&
                estaDesbloqueadaUnidad(unidad, evaluacionesAprobadasIds)
        }

        return ResumenProgresoCurso(
            totalLecciones = totalLecciones,
            leccionesCompletadas = leccionesCompletadas,
            unidadesCompletadas = unidadesCompletadas,
            siguienteLeccion = siguienteLeccion,
            siguienteEvaluacion = siguienteEvaluacion,
        )
    }

    /**
     * Una lección está desbloqueada si:
     * 1. Las evaluaciones de todas las unidades anteriores están aprobadas.
     * 2. Todas las lecciones anteriores dentro de la misma unidad están completas.
     */
    fun estaDesbloqueada(
        unidad: Unidad,
        leccion: Leccion,
        leccionesCompletadasIds: List<String>,
        evaluacionesAprobadasIds: List<String>,
    ): Boolean {
        val estado = _estadoCurso.value as? Recurso.Exito<List<Unidad>> ?: return false
        val unidadActual = estado.datos.firstOrNull { it.id == unidad.id } ?: return false
        val indice = unidadActual.lecciones.indexOfFirst { it.id == leccion.id }
        if (indice == -1) return false

        val unidadesPrevias = estado.datos.takeWhile { it.id != unidadActual.id }
        val leccionesPrevias = unidadActual.lecciones.take(indice)

        val unidadesPreviasListas = unidadesPrevias.all { it.id in evaluacionesAprobadasIds }
        val leccionesPreviasListas = leccionesPrevias.all {
            estaCompletada(unidadActual, it, leccionesCompletadasIds)
        }

        return unidadesPreviasListas && leccionesPreviasListas
    }

    /**
     * Una unidad está desbloqueada si las evaluaciones anteriores están aprobadas.
     */
    fun estaDesbloqueadaUnidad(
        unidad: Unidad,
        evaluacionesAprobadasIds: List<String>,
    ): Boolean {
        val estado = _estadoCurso.value as? Recurso.Exito<List<Unidad>> ?: return false
        val unidadesPrevias = estado.datos.takeWhile { it.id != unidad.id }
        return unidadesPrevias.all { it.id in evaluacionesAprobadasIds }
    }

    /** La evaluación aparece cuando todas las lecciones de la unidad están completas. */
    fun puedeEvaluar(unidad: Unidad, leccionesCompletadasIds: List<String>): Boolean =
        unidad.lecciones.isNotEmpty() && unidad.lecciones.all {
            estaCompletada(unidad, it, leccionesCompletadasIds)
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
