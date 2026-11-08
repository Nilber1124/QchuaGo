package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.proyect.qchuago.data.entidad.EntradaDiccionario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class DireccionTraduccion { QUECHUA_A_ESPANOL, ESPANOL_A_QUECHUA }

data class EstadoDesafio(
    val entrada: EntradaDiccionario,
    val direccion: DireccionTraduccion,
    val opciones: List<String>,
    val seleccionada: String? = null,
    val verificada: Boolean = false,
    val esCorrecta: Boolean = false,
    val aciertos: Int = 0,
    val intentos: Int = 0,
) {
    val enunciado: String
        get() = if (direccion == DireccionTraduccion.QUECHUA_A_ESPANOL) entrada.quechua else entrada.espanol
    val respuestaCorrecta: String
        get() = if (direccion == DireccionTraduccion.QUECHUA_A_ESPANOL) entrada.espanol else entrada.quechua
}

/**
 * Práctica libre de traducción con el diccionario local (botón "Desafío" de
 * [com.proyect.qchuago.ui.screens.DiccionarioScreen]): sin persistencia, XP ni
 * racha, no está ligada a ninguna lección.
 */
class DesafioViewModel(private val entradas: List<EntradaDiccionario>) : ViewModel() {

    private val _estado = MutableStateFlow(generarPregunta(aciertos = 0, intentos = 0))
    val estado: StateFlow<EstadoDesafio> = _estado.asStateFlow()

    fun seleccionar(opcion: String) {
        val actual = _estado.value
        if (actual.verificada) return
        _estado.value = actual.copy(seleccionada = opcion)
    }

    fun comprobar() {
        val actual = _estado.value
        val opcion = actual.seleccionada ?: return
        if (actual.verificada) return
        val esCorrecta = opcion == actual.respuestaCorrecta
        _estado.value = actual.copy(
            verificada = true,
            esCorrecta = esCorrecta,
            intentos = actual.intentos + 1,
            aciertos = if (esCorrecta) actual.aciertos + 1 else actual.aciertos,
        )
    }

    fun siguiente() {
        val actual = _estado.value
        _estado.value = generarPregunta(aciertos = actual.aciertos, intentos = actual.intentos)
    }

    private fun generarPregunta(aciertos: Int, intentos: Int): EstadoDesafio {
        val entrada = entradas.random()
        val direccion = if (Random.nextBoolean()) {
            DireccionTraduccion.QUECHUA_A_ESPANOL
        } else {
            DireccionTraduccion.ESPANOL_A_QUECHUA
        }
        val correcta = if (direccion == DireccionTraduccion.QUECHUA_A_ESPANOL) entrada.espanol else entrada.quechua
        val distractores = entradas
            .asSequence()
            .filter { it !== entrada }
            .map { if (direccion == DireccionTraduccion.QUECHUA_A_ESPANOL) it.espanol else it.quechua }
            .filter { it != correcta }
            .distinct()
            .shuffled()
            .take(3)
            .toList()
        val opciones = (distractores + correcta).shuffled()
        return EstadoDesafio(entrada = entrada, direccion = direccion, opciones = opciones, aciertos = aciertos, intentos = intentos)
    }
}
