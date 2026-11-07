package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.TipoActividad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class EstadoEvaluacion {
    object Cargando : EstadoEvaluacion()
    data class Error(val mensaje: String) : EstadoEvaluacion()
    data class EnCurso(
        val nombreUnidad: String,
        val preguntas: List<Actividad>,
        val indice: Int = 0,
        val seleccionada: String? = null,
        val correctas: Int = 0,
    ) : EstadoEvaluacion() {
        val preguntaActual: Actividad get() = preguntas[indice]
        val esUltima: Boolean get() = indice == preguntas.lastIndex
    }
    data class Resultado(
        val nombreUnidad: String,
        val correctas: Int,
        val total: Int,
        val porcentaje: Int,
        val aprobada: Boolean,
        val guardando: Boolean = false,
        val error: String? = null,
    ) : EstadoEvaluacion()
}

class EvaluacionViewModel(
    private val unidadId: String,
    private val uid: String?,
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val unidadRef = firestore.collection("unidades").document(unidadId)
    private val usuariosCollection = firestore.collection("usuarios")

    private var nombreUnidad = "Unidad"
    private var preguntas: List<Actividad> = emptyList()

    private val _estado = MutableStateFlow<EstadoEvaluacion>(EstadoEvaluacion.Cargando)
    val estado: StateFlow<EstadoEvaluacion> = _estado.asStateFlow()

    init {
        cargarPreguntas()
    }

    fun cargarPreguntas() {
        _estado.value = EstadoEvaluacion.Cargando
        viewModelScope.launch {
            try {
                val unidad = unidadRef.get().await()
                nombreUnidad = unidad.getString("nombre").orEmpty().ifBlank { "Unidad" }
                val lecciones = unidadRef.collection("lecciones")
                    .orderBy("orden", Query.Direction.ASCENDING)
                    .get().await().documents

                val uidActual = uid
                if (uidActual == null || lecciones.isEmpty()) {
                    _estado.value = EstadoEvaluacion.Error(
                        "No se puede iniciar esta evaluación."
                    )
                    return@launch
                }
                val usuario = usuariosCollection.document(uidActual).get().await()
                val completadas = (usuario.get("leccionesCompletadasIds") as? List<*>)
                    ?.filterIsInstance<String>().orEmpty()
                val unidadCompletada = lecciones.all { "$unidadId/${it.id}" in completadas }
                if (!unidadCompletada) {
                    _estado.value = EstadoEvaluacion.Error(
                        "Completa todas las lecciones de la unidad antes de evaluarte."
                    )
                    return@launch
                }

                val encontradas = mutableListOf<Actividad>()
                for (leccion in lecciones) {
                    if (encontradas.size == MAXIMO_PREGUNTAS) break
                    val actividades = leccion.reference.collection("actividades")
                        .orderBy("orden", Query.Direction.ASCENDING)
                        .get().await()
                        .toObjects(Actividad::class.java)
                        .filter(::esPreguntaValida)
                    encontradas += actividades.take(MAXIMO_PREGUNTAS - encontradas.size)
                }

                preguntas = encontradas
                if (preguntas.isEmpty()) {
                    _estado.value = EstadoEvaluacion.Error(
                        "Esta unidad no tiene preguntas de selección disponibles."
                    )
                } else {
                    iniciarEvaluacion()
                }
            } catch (e: Exception) {
                _estado.value = EstadoEvaluacion.Error(
                    "No se pudo cargar la evaluación. Inténtalo nuevamente."
                )
            }
        }
    }

    fun seleccionar(opcion: String) {
        val actual = _estado.value as? EstadoEvaluacion.EnCurso ?: return
        _estado.value = actual.copy(seleccionada = opcion)
    }

    fun continuar() {
        val actual = _estado.value as? EstadoEvaluacion.EnCurso ?: return
        val seleccionada = actual.seleccionada ?: return
        val nuevasCorrectas = actual.correctas +
            if (seleccionada == actual.preguntaActual.respuestaCorrecta) 1 else 0

        if (actual.esUltima) {
            terminarEvaluacion(nuevasCorrectas, actual.preguntas.size)
        } else {
            _estado.value = actual.copy(
                indice = actual.indice + 1,
                seleccionada = null,
                correctas = nuevasCorrectas,
            )
        }
    }

    fun repetir() = iniciarEvaluacion()

    fun reintentarGuardado() {
        val resultado = _estado.value as? EstadoEvaluacion.Resultado ?: return
        guardarResultado(resultado.copy(guardando = true, error = null))
    }

    private fun iniciarEvaluacion() {
        if (preguntas.isNotEmpty()) {
            _estado.value = EstadoEvaluacion.EnCurso(nombreUnidad, preguntas)
        }
    }

    private fun terminarEvaluacion(correctas: Int, total: Int) {
        val porcentaje = (correctas * 100) / total
        guardarResultado(
            EstadoEvaluacion.Resultado(
                nombreUnidad = nombreUnidad,
                correctas = correctas,
                total = total,
                porcentaje = porcentaje,
                aprobada = porcentaje >= NOTA_MINIMA,
                guardando = true,
            )
        )
    }

    private fun guardarResultado(resultado: EstadoEvaluacion.Resultado) {
        val uidActual = uid
        if (uidActual == null) {
            _estado.value = resultado.copy(
                guardando = false,
                error = "No se pudo identificar al usuario.",
            )
            return
        }

        _estado.value = resultado
        viewModelScope.launch {
            try {
                val aprobada = guardarEnFirebase(uidActual, resultado.porcentaje)
                _estado.value = resultado.copy(aprobada = aprobada, guardando = false)
            } catch (e: Exception) {
                _estado.value = resultado.copy(
                    guardando = false,
                    error = "No se pudo guardar el resultado. Revisa tu conexión.",
                )
            }
        }
    }

    private suspend fun guardarEnFirebase(uidActual: String, porcentaje: Int): Boolean {
        val usuarioRef = usuariosCollection.document(uidActual)
        val evaluacionRef = usuarioRef.collection("evaluaciones").document(unidadId)

        return firestore.runTransaction { transaccion ->
            val evaluacionAnterior = transaccion.get(evaluacionRef)
            val usuario = transaccion.get(usuarioRef)
            val aprobada = evaluacionAnterior.getBoolean("aprobada") == true ||
                porcentaje >= NOTA_MINIMA
            val mejorCalificacion = maxOf(
                evaluacionAnterior.getLong("calificacion")?.toInt() ?: 0,
                porcentaje,
            )

            transaccion.set(
                evaluacionRef,
                mapOf(
                    "unidadId" to unidadId,
                    "calificacion" to mejorCalificacion,
                    "aprobada" to aprobada,
                    "fecha" to Timestamp.now(),
                ),
                SetOptions.merge(),
            )

            if (aprobada) {
                val aprobadas = (usuario.get("evaluacionesAprobadasIds") as? List<*>)
                    ?.filterIsInstance<String>().orEmpty()
                if (unidadId !in aprobadas) {
                    transaccion.update(
                        usuarioRef,
                        "evaluacionesAprobadasIds",
                        aprobadas + unidadId,
                    )
                }
            }
            aprobada
        }.await()
    }

    private fun esPreguntaValida(actividad: Actividad): Boolean =
        actividad.activo &&
            actividad.tipo == TipoActividad.SELECCION &&
            actividad.enunciado.isNotBlank() &&
            actividad.opciones.size >= 2 &&
            actividad.respuestaCorrecta in actividad.opciones
}

private const val MAXIMO_PREGUNTAS = 10
private const val NOTA_MINIMA = 70
