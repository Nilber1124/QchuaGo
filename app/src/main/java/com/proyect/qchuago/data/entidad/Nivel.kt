package com.proyect.qchuago.data.entidad

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

@Keep
data class Nivel(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val icono: String = "",
    val colorHex: String = "",
    val orden: Int = 0,
    val activo: Boolean = true,
)
