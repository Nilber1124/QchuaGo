package com.proyect.qchuago.data.servicio

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.proyect.qchuago.data.entidad.Usuario
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")

    /** Obtiene el perfil persistido. No crea documentos ni oculta errores de red/permisos. */
    suspend fun obtenerUsuario(uid: String): Result<Usuario> = try {
        val snapshot = esperar(usuariosCollection.document(uid).get())
        if (!snapshot.exists()) {
            Result.failure(Exception("El perfil aún no existe."))
        } else {
            Result.success(aUsuario(snapshot, uid))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Garantiza el documento usuarios/{uid}. Los valores iniciales se escriben solo cuando
     * el documento o un campo concreto no existe; nunca se reemplazan estadísticas existentes.
     */
    suspend fun obtenerOCrearUsuario(usuarioAuth: Usuario): Result<Usuario> = try {
        val documento = usuariosCollection.document(usuarioAuth.uid)
        val snapshot = esperar(documento.get())

        if (!snapshot.exists()) {
            esperar(documento.set(datosIniciales(usuarioAuth)))
            Result.success(usuarioAuth)
        } else {
            val camposFaltantes = camposFaltantes(snapshot, usuarioAuth)
            if (camposFaltantes.isNotEmpty()) {
                esperar(documento.set(camposFaltantes, SetOptions.merge()))
            }
            Result.success(aUsuario(snapshot, usuarioAuth.uid, usuarioAuth))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarTemaOscuro(uid: String, temaOscuro: Boolean): Result<Unit> = try {
        esperar(usuariosCollection.document(uid).update("temaOscuro", temaOscuro))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit> = try {
        esperar(usuariosCollection.document(uid).update("nombre", nombre.trim()))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun datosIniciales(usuario: Usuario) = mapOf(
        "nombre" to usuario.nombre,
        "correo" to usuario.correo,
        "temaOscuro" to false,
        "nivel" to "Principiante",
        "xp" to 0,
        "racha" to 1,
        "vidas" to 5,
        "leccionesCompletadas" to 0,
        "totalLecciones" to 9,
    )

    private fun camposFaltantes(snapshot: DocumentSnapshot, usuario: Usuario): Map<String, Any> =
        buildMap {
            if (!snapshot.contains("nombre")) put("nombre", usuario.nombre)
            if (!snapshot.contains("correo")) put("correo", usuario.correo)
            if (!snapshot.contains("temaOscuro")) put("temaOscuro", false)
            if (!snapshot.contains("nivel")) put("nivel", "Principiante")
            if (!snapshot.contains("xp")) put("xp", 0)
            if (!snapshot.contains("racha")) put("racha", 1)
            if (!snapshot.contains("vidas")) put("vidas", 5)
            if (!snapshot.contains("leccionesCompletadas")) put("leccionesCompletadas", 0)
            if (!snapshot.contains("totalLecciones")) put("totalLecciones", 9)
        }

    private fun aUsuario(
        snapshot: DocumentSnapshot,
        uid: String,
        usuarioAuth: Usuario = Usuario(uid = uid),
    ) = Usuario(
        uid = uid,
        correo = snapshot.getString("correo").orEmpty().ifBlank { usuarioAuth.correo },
        nombre = snapshot.getString("nombre").orEmpty().ifBlank { usuarioAuth.nombre },
        temaOscuro = snapshot.getBoolean("temaOscuro") ?: false,
        nivel = snapshot.getString("nivel") ?: "Principiante",
        xp = snapshot.getLong("xp")?.toInt() ?: 0,
        racha = snapshot.getLong("racha")?.toInt() ?: 1,
        vidas = snapshot.getLong("vidas")?.toInt() ?: 5,
        leccionesCompletadas = snapshot.getLong("leccionesCompletadas")?.toInt() ?: 0,
        totalLecciones = snapshot.getLong("totalLecciones")?.toInt() ?: 9,
    )

    private suspend fun <T> esperar(tarea: Task<T>): T = suspendCancellableCoroutine { cont ->
        tarea.addOnCompleteListener { t ->
            if (t.isSuccessful) {
                cont.resume(t.result)
            } else {
                cont.resumeWithException(t.exception ?: Exception("Error desconocido"))
            }
        }
    }
}
