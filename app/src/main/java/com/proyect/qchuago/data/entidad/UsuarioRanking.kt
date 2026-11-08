package com.proyect.qchuago.data.entidad

/**
 * Datos de un usuario en el ranking semanal.
 */
data class UsuarioRanking(
    val uid: String = "",
    val nombre: String = "",
    val xpSemanal: Int = 0,
    val posicion: Int = 0,
)
