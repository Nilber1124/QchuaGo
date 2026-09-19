package com.proyect.qchuago.data.entidad

data class Usuario(
    val uid: String = "",
    val correo: String = "",
    val nombre: String = "",
    val temaOscuro: Boolean = false,
    val nivel: String = "Principiante",
    val xp: Int = 0,
    val racha: Int = 1,
    val vidas: Int = 5,
    val leccionesCompletadas: Int = 0,
    val totalLecciones: Int = 9,
)
