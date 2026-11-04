package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proyect.qchuago.data.entidad.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Maneja la sesión y el perfil del usuario.
 *
 * Todas las funciones que hablan con Firebase siguen el mismo molde:
 * `cargando = true → try { ... .await() } catch { mensaje en español } → cargando = false`.
 */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val usuariosCollection = FirebaseFirestore.getInstance().collection("usuarios")

    /** `true` cuando ya se comprobó si existía una sesión guardada. */
    private val _sesionInicializada = MutableStateFlow(false)
    val sesionInicializada: StateFlow<Boolean> = _sesionInicializada.asStateFlow()

    /** Usuario con sesión activa o `null` sin sesión. */
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    /** Tema oscuro global. */
    private val _temaOscuro = MutableStateFlow(false)
    val temaOscuro: StateFlow<Boolean> = _temaOscuro.asStateFlow()

    /** `true` mientras se ejecuta un login, registro o acción bloqueante. */
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /** Mensaje de error comprensible o `null`. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Si ya había una sesión guardada, cargamos su perfil antes de mostrar la app.
        val usuarioAuth = auth.currentUser
        viewModelScope.launch {
            if (usuarioAuth != null) cargarPerfil(aUsuario(usuarioAuth))
            _sesionInicializada.value = true
        }
    }

    /**
     * Cambia cualquier campo del usuario (xp, vidas, tema, etc.) con una sola llamada.
     * Primero actualiza la pantalla y luego guarda en Firestore; si falla, deshace el cambio.
     *
     * Ejemplo: `actualizarUsuario { it.copy(xp = it.xp + 10) }`
     */
    fun actualizarUsuario(
        onCompletado: (exito: Boolean) -> Unit = {},
        transformar: (Usuario) -> Usuario,
    ) {
        val previo = _usuario.value ?: return
        val nuevo = transformar(previo)
        _usuario.value = nuevo
        _temaOscuro.value = nuevo.temaOscuro

        viewModelScope.launch {
            try {
                usuariosCollection.document(nuevo.uid).set(nuevo, SetOptions.merge()).await()
                onCompletado(true)
            } catch (e: Exception) {
                _usuario.value = previo
                _temaOscuro.value = previo.temaOscuro
                _error.value = "No se pudieron guardar los cambios. Inténtalo más tarde."
                onCompletado(false)
            }
        }
    }

    fun cambiarTema(activo: Boolean) {
        actualizarUsuario { it.copy(temaOscuro = activo) }
    }

    /** Marca que el usuario ya vio la bienvenida para no mostrársela otra vez. */
    fun completarOnboarding() {
        actualizarUsuario { it.copy(onboardingCompletado = true) }
    }

    /**
     * Guarda que el usuario terminó una lección (RF14). Esto desbloquea la siguiente.
     * La clave tiene el mismo formato que `Leccion.claveEn(unidadId)`: "unidadId/leccionId".
     */
    fun marcarLeccionCompletada(unidadId: String, leccionId: String) {
        val clave = "$unidadId/$leccionId"
        if (_usuario.value?.leccionesCompletadasIds?.contains(clave) == true) return
        actualizarUsuario { u ->
            val ids = u.leccionesCompletadasIds + clave
            u.copy(leccionesCompletadasIds = ids, leccionesCompletadas = ids.size)
        }
    }

    fun iniciarSesion(correo: String, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val resultado = auth.signInWithEmailAndPassword(correo.trim(), contrasena).await()
                val usuarioAuth = resultado.user ?: throw Exception("Sin usuario")
                cargarPerfil(aUsuario(usuarioAuth))
            } catch (e: Exception) {
                _error.value = mensajeComprensible(e)
            }
            _cargando.value = false
        }
    }

    fun registrar(nombre: String, correo: String, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val resultado = auth.createUserWithEmailAndPassword(correo.trim(), contrasena).await()
                val usuarioAuth = resultado.user ?: throw Exception("Sin usuario")
                val nuevo = Usuario(uid = usuarioAuth.uid, nombre = nombre.trim(), correo = correo.trim())
                usuariosCollection.document(nuevo.uid).set(nuevo).await()
                _usuario.value = nuevo
                _temaOscuro.value = nuevo.temaOscuro
            } catch (e: Exception) {
                _error.value = mensajeComprensible(e)
            }
            _cargando.value = false
        }
    }

    fun actualizarNombre(nuevoNombre: String, onSuccess: () -> Unit) {
        if (nuevoNombre.isBlank()) {
            _error.value = "El nombre no puede estar vacío."
            return
        }
        _error.value = null
        actualizarUsuario(onCompletado = { exito -> if (exito) onSuccess() }) {
            it.copy(nombre = nuevoNombre.trim())
        }
    }

    fun cambiarContrasena(nueva: String, confirmar: String, onSuccess: () -> Unit) {
        if (nueva.isBlank() || confirmar.isBlank()) {
            _error.value = "Las contraseñas no pueden estar vacías."
            return
        }
        if (nueva.length < 6) {
            _error.value = "La contraseña debe tener al menos 6 caracteres."
            return
        }
        if (nueva != confirmar) {
            _error.value = "Las contraseñas no coinciden."
            return
        }

        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: throw Exception("Sin sesión")
                user.updatePassword(nueva).await()
                onSuccess()
            } catch (e: Exception) {
                _error.value = mensajeComprensible(e)
            }
            _cargando.value = false
        }
    }

    fun cerrarSesion() {
        auth.signOut()
        _usuario.value = null
        _temaOscuro.value = false
    }

    fun limpiarError() {
        _error.value = null
    }

    /**
     * Lee el perfil de `usuarios/{uid}`. Si es la primera vez (no existe), lo crea.
     * Si falla la lectura, deja entrar al usuario con los datos básicos de Auth.
     */
    private suspend fun cargarPerfil(usuarioAuth: Usuario) {
        try {
            val documento = usuariosCollection.document(usuarioAuth.uid)
            val guardado = documento.get().await().toObject(Usuario::class.java)
            val usuario = if (guardado != null) {
                guardado
            } else {
                documento.set(usuarioAuth).await()
                usuarioAuth
            }
            _usuario.value = usuario
            _temaOscuro.value = usuario.temaOscuro
        } catch (e: Exception) {
            _usuario.value = usuarioAuth
            _error.value = "No se pudo cargar tu perfil. Inténtalo nuevamente."
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

    /** Convierte el usuario de Firebase Auth en nuestro [Usuario] con datos básicos. */
    private fun aUsuario(usuario: FirebaseUser): Usuario {
        val correo = usuario.email.orEmpty()
        return Usuario(
            uid = usuario.uid,
            correo = correo,
            nombre = usuario.displayName ?: correo.substringBefore("@").ifBlank { "Usuario" },
        )
    }
}
