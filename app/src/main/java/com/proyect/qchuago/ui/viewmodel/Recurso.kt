package com.proyect.qchuago.ui.viewmodel

/**
 * Estado genérico de carga/éxito/error para ViewModels que solo cargan un dato
 * (una lista, un objeto) y lo muestran. Patrón único a reutilizar en vez de
 * reinventar un sealed class de estado en cada ViewModel nuevo.
 */
sealed class Recurso<out T> {
    object Cargando : Recurso<Nothing>()
    data class Exito<T>(val datos: T) : Recurso<T>()
    data class Error(val mensaje: String) : Recurso<Nothing>()
}
