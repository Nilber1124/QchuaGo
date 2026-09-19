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

    /**
     * Obtiene los datos del usuario desde Firestore, o crea un nuevo documento si no existe.
     * Garantiza el documento usuarios/{uid}. Los valores iniciales se escriben solo cuando
     * el documento o un campo concreto no existe; nunca se reemplazan estadísticas existentes.
     *
     * @param usuarioAuth El objeto Usuario autenticado con los datos básicos (uid, correo, nombre).
     * @return Result<Usuario> con los datos completos del usuario desde la base de datos o un error en caso de fallo.
     */
    suspend fun obtenerOCrearUsuario(usuarioAuth: Usuario): Result<Usuario> = try {
        val documento = usuariosCollection.document(usuarioAuth.uid)
        val snapshot = esperar(documento.get())

        if (!snapshot.exists()) {
            esperar(documento.set(datosIniciales(usuarioAuth)))
            Result.success(usuarioAuth)
        } else {
            val camposFaltantes = obtenerCamposFaltantes(snapshot, usuarioAuth)
            if (camposFaltantes.isNotEmpty()) {
                esperar(documento.set(camposFaltantes, SetOptions.merge()))
            }
            Result.success(crearUsuario(snapshot, usuarioAuth.uid, usuarioAuth))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Actualiza la preferencia del tema oscuro para un usuario específico en Firestore.
     *
     * @param uid Identificador único del usuario.
     * @param temaOscuro True si el usuario prefiere el modo oscuro, False en caso contrario.
     * @return Result<Unit> indicando éxito o fracaso de la operación.
     */
    suspend fun actualizarTemaOscuro(uid: String, temaOscuro: Boolean): Result<Unit> = try {
        esperar(usuariosCollection.document(uid).update("temaOscuro", temaOscuro))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Actualiza el nombre de visualización del usuario.
     * Los espacios en blanco al principio y al final del nombre se eliminan automáticamente (trim).
     *
     * @param uid Identificador único del usuario.
     * @param nombre El nuevo nombre que se desea guardar.
     * @return Result<Unit> indicando éxito o fracaso de la operación.
     */
    suspend fun actualizarNombre(uid: String, nombre: String): Result<Unit> = try {
        esperar(usuariosCollection.document(uid).update("nombre", nombre.trim()))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Genera un mapa con los valores por defecto iniciales para un nuevo usuario.
     * Se usa al crear un documento por primera vez en Firestore.
     *
     * @param usuario El objeto Usuario del cual extraer el nombre y correo base.
     * @return Map con los campos iniciales estructurados para Firestore.
     */
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

    /**
     * Compara un documento existente con los campos requeridos por el sistema.
     * Esto asegura que si se añaden nuevos campos en actualizaciones de la app,
     * los usuarios antiguos los reciban con sus valores por defecto (migración automática local).
     *
     * @param snapshot El documento actual del usuario recuperado de Firestore.
     * @param usuario El objeto Usuario base (usado para campos de fallback).
     * @return Un Map conteniendo solo los campos que no estaban presentes en el snapshot.
     */
    private fun obtenerCamposFaltantes(
        snapshot: DocumentSnapshot,
        usuario: Usuario,
    ): Map<String, Any> {
        val campos = mutableMapOf<String, Any>()
        if (!snapshot.contains("nombre")) campos["nombre"] = usuario.nombre
        if (!snapshot.contains("correo")) campos["correo"] = usuario.correo
        if (!snapshot.contains("temaOscuro")) campos["temaOscuro"] = false
        if (!snapshot.contains("nivel")) campos["nivel"] = "Principiante"
        if (!snapshot.contains("xp")) campos["xp"] = 0
        if (!snapshot.contains("racha")) campos["racha"] = 1
        if (!snapshot.contains("vidas")) campos["vidas"] = 5
        if (!snapshot.contains("leccionesCompletadas")) campos["leccionesCompletadas"] = 0
        if (!snapshot.contains("totalLecciones")) campos["totalLecciones"] = 9
        return campos
    }

    /**
     * Mapea un [DocumentSnapshot] de Firestore a una entidad [Usuario] de la aplicación.
     * Realiza un manejo seguro de nulos y tipos, asignando valores por defecto
     * cuando los datos no están disponibles o tienen un formato incorrecto en la base de datos.
     *
     * @param snapshot El documento Firestore con la información del usuario.
     * @param uid Identificador único del usuario.
     * @param usuarioAuth Información base del usuario obtenida de Firebase Auth (usado como fallback).
     * @return Una instancia completa de la entidad [Usuario].
     */
    private fun crearUsuario(
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

    /**
     * Función de utilidad para convertir Tareas asíncronas de Firebase (API basada en callbacks)
     * en corrutinas suspendidas de Kotlin de forma segura y cancelable. 
     * Facilita la legibilidad y evita el Callback Hell.
     *
     * @param tarea Tarea de Google Play Services / Firebase a ejecutar.
     * @return El resultado exitoso [T] de la tarea.
     * @throws Exception Si la tarea falla o lanza una excepción.
     */
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
