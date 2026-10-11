package com.proyect.qchuago.data.servicio

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proyect.qchuago.data.entidad.Usuario

class FirestoreService {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")

    /**
     * Obtiene los datos del usuario desde Firestore, o crea un nuevo documento si no existe.
     * Utiliza la serialización nativa de Firestore para deserializar el objeto Usuario.
     * Si en Firestore faltan campos nuevos de la app, Firestore usa automáticamente
     * sus valores por defecto en Kotlin y los sincroniza mediante merge.
     */
    suspend fun obtenerOCrearUsuario(usuarioAuth: Usuario): Result<Usuario> = try {
        val documento = usuariosCollection.document(usuarioAuth.uid)
        val snapshot = esperar(documento.get())

        if (!snapshot.exists()) {
            esperar(documento.set(usuarioAuth))
            Result.success(usuarioAuth)
        } else {
            val usuarioRemoto = snapshot.toObject(Usuario::class.java)
            val usuarioFinal = if (usuarioRemoto != null) {
                usuarioRemoto.copy(
                    uid = usuarioAuth.uid,
                    correo = usuarioRemoto.correo.ifBlank { usuarioAuth.correo },
                    nombre = usuarioRemoto.nombre.ifBlank { usuarioAuth.nombre }
                )
            } else {
                usuarioAuth
            }
            // Sincroniza campos que puedan faltar en Firestore manteniendo los existentes
            esperar(documento.set(usuarioFinal, SetOptions.merge()))
            Result.success(usuarioFinal)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Registra un nuevo documento en la colección usuarios/{uid} al completar el registro.
     *
     * @param usuario Entidad Usuario con su UID asignado por Firebase Auth.
     */
    suspend fun guardarNuevoUsuario(usuario: Usuario): Result<Unit> = try {
        esperar(usuariosCollection.document(usuario.uid).set(usuario))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Guarda o actualiza todas las propiedades de un usuario mediante merge nativo.
     *
     * @param usuario Entidad con los datos actualizados.
     */
    suspend fun actualizarUsuario(usuario: Usuario): Result<Unit> = try {
        esperar(usuariosCollection.document(usuario.uid).set(usuario, SetOptions.merge()))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Actualiza un conjunto específico de campos para un usuario en Firestore.
     *
     * @param uid Identificador único del usuario.
     * @param campos Mapa de claves y valores a actualizar.
     */
    suspend fun actualizarCampos(uid: String, campos: Map<String, Any>): Result<Unit> = try {
        esperar(usuariosCollection.document(uid).update(campos))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

}
