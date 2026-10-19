package com.proyect.qchuago.data.entidad

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

enum class TipoActividad {
    VOCABULARIO,
    PRONUNCIACION,
    SELECCION,
    COMPLETAR,
    ORDENAR,
    ASOCIAR,
}


@Keep
data class Actividad(
    @DocumentId val id: String = "",
    val tipo: TipoActividad = TipoActividad.VOCABULARIO,
    val orden: Int = 0,
    val activo: Boolean = true,

    val quechua: String = "",
    val traduccion: String = "",
    val explicacion: String = "",
    val audioLocal: String = "",
    val audioUrl: String = "",

    val enunciado: String = "",
    val opciones: List<String> = emptyList(),
    val elementos: List<String> = emptyList(),
    val respuestaCorrecta: String = "",
    val respuestasCorrectas: List<String> = emptyList(),
)
