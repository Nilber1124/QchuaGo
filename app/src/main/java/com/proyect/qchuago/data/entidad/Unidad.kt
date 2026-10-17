package com.proyect.qchuago.data.entidad

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class Unidad(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String = "",
    val colorHex: String = "",
    val orden: Int = 0,
    val lecciones: List<Leccion> = emptyList(),
)

@Keep
data class Leccion(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val icono: String = "",
    val orden: Int = 0,
    val tipo: String = "leccion",
) {
    /** Identificador único de la lección dentro del curso. */
    fun claveEn(unidadId: String): String = "$unidadId/$id"
}
