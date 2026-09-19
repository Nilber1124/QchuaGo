package com.proyect.qchuago.data.servicio

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.proyect.qchuago.data.entidad.Usuario
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Servicio que se comunica con Firebase Authentication.
 * La UI nunca llama a FirebaseAuth directamente.
 */
class AuthService {

    private val auth = FirebaseAuth.getInstance()

    /** Usuario con sesión activa o `null` si no la hay. */
    val usuarioActual: Usuario?
        get() = auth.currentUser?.let(::aUsuario)

    /** Inicia sesión con Email/Password. */
    suspend fun iniciarSesion(correo: String, contrasena: String): Result<Usuario> =
        try {
            val usuario = esperar(auth.signInWithEmailAndPassword(correo, contrasena)).user
                ?: return Result.failure(Exception("Ocurrió un error inesperado."))
            Result.success(aUsuario(usuario))
        } catch (e: Exception) {
            Result.failure(Exception(mensajeComprensible(e)))
        }

    /** Crea la cuenta, guarda el nombre en el perfil y devuelve el usuario. */
    suspend fun registrar(correo: String, contrasena: String, nombre: String): Result<Usuario> =
        try {
            val usuario = esperar(auth.createUserWithEmailAndPassword(correo, contrasena)).user
                ?: return Result.failure(Exception("Ocurrió un error inesperado."))
            esperar(
                usuario.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(nombre.trim()).build()
                )
            )
            Result.success(aUsuario(usuario))
        } catch (e: Exception) {
            Result.failure(Exception(mensajeComprensible(e)))
        }

    /** Cierra la sesión activa. */
    fun cerrarSesion() {
        auth.signOut()
    }

    /** Actualiza el nombre del usuario en Firebase Auth. */
    suspend fun actualizarNombre(nombre: String): Result<Unit> = try {
        val user = auth.currentUser ?: return Result.failure(Exception("No hay sesión activa."))
        esperar(
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(nombre.trim()).build()
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(mensajeComprensible(e)))
    }

    /** Cambia la contraseña del usuario en Firebase Auth. */
    suspend fun cambiarContrasena(nuevaContrasena: String): Result<Unit> = try {
        val user = auth.currentUser ?: return Result.failure(Exception("No hay sesión activa."))
        esperar(user.updatePassword(nuevaContrasena))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(mensajeComprensible(e)))
    }

    /** Convierte una Task de Firebase en una operación suspendida (sin bloquear el hilo). */
    private suspend fun <T> esperar(tarea: Task<T>): T =
        suspendCancellableCoroutine { cont ->
            tarea.addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    cont.resume(t.result)
                } else {
                    cont.resumeWithException(
                        t.exception ?: Exception("Ocurrió un error inesperado.")
                    )
                }
            }
        }

    /** Traduce el error técnico de Firebase a un mensaje comprensible. */
    private fun mensajeComprensible(error: Exception): String {
        if (error is FirebaseAuthException) {
            return when (error.errorCode) {
                "ERROR_INVALID_EMAIL" -> "El correo no es válido."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "El correo ya está registrado."
                "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres."
                "ERROR_WRONG_PASSWORD",
                "ERROR_INVALID_CREDENTIAL",
                "ERROR_USER_NOT_FOUND" -> "El correo o la contraseña son incorrectos."
                "ERROR_REQUIRES_RECENT_LOGIN" -> "Por seguridad, vuelve a iniciar sesión antes de cambiar tu contraseña."
                "ERROR_NETWORK_REQUEST_FAILED" -> "No se pudo conectar con Firebase."
                else -> "Ocurrió un error inesperado."
            }
        }
        return "Ocurrió un error inesperado."
    }

    private fun aUsuario(usuario: FirebaseUser): Usuario {
        val correo = usuario.email.orEmpty()
        return Usuario(
            uid = usuario.uid,
            correo = correo,
            nombre = usuario.displayName ?: correo.substringBefore("@").ifBlank { "Usuario" },
        )
    }
}
