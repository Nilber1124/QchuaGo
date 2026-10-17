package com.proyect.qchuago.data.entidad

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId

/**
 * Modelo de datos de usuario con serialización nativa de Firebase Firestore.
 * Cada nuevo campo con valor por defecto se sincroniza automáticamente con Firestore
 * sin necesidad de mapeos manuales ni boilerplate.
 */
@Keep
data class Usuario(
    @DocumentId
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
    val leccionesCompletadasIds: List<String> = emptyList(),
    val onboardingCompletado: Boolean = false,
)
