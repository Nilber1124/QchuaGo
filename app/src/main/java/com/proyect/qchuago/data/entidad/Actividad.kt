package com.proyect.qchuago.data.entidad

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

/**
 * Paso dentro de una lección. El campo [tipo] decide qué subconjunto de campos
 * se usa: "VOCABULARIO"/"PRONUNCIACION" usan quechua/traduccion/explicacion/audio;
 * "SELECCION"/"COMPLETAR"/"ORDENAR" usan enunciado y las respuestas.
 */
@Keep
data class Actividad(
    @DocumentId val id: String = "",
    val tipo: String = "",
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
