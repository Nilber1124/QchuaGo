package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.data.servicio.AuthService
import com.proyect.qchuago.data.servicio.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthService = AuthService(),
    private val firestoreService: FirestoreService = FirestoreService(),
) : ViewModel() {

    private val cancelarEscuchaAuth: () -> Unit

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
        cancelarEscuchaAuth = authService.escucharEstadoSesion { autenticado ->
            if (!autenticado && _sesionInicializada.value) {
                _usuario.value = null
                _temaOscuro.value = false
            }
        }

        val usuarioAuth = authService.usuarioActual
        if (usuarioAuth != null) {
            viewModelScope.launch {
                firestoreService.obtenerOCrearUsuario(usuarioAuth)
                    .onSuccess { u ->
                        _temaOscuro.value = u.temaOscuro
                        _usuario.value = u
                    }
                    .onFailure {
                        _error.value = "No se pudo cargar tu perfil. Inténtalo nuevamente."
                        _usuario.value = usuarioAuth
                    }
                _sesionInicializada.value = true
            }
        } else {
            _sesionInicializada.value = true
        }
    }

    /**
     * Actualiza el usuario tanto localmente (optimistic UI) como en Firestore.
     * Si ocurre un error, revierte los cambios automáticamente.
     * Permite modificar cualquier campo actual o futuro (vidas, xp, racha, tema, etc.) con una sola llamada.
     */
    fun actualizarUsuario(transformar: (Usuario) -> Usuario) {
        actualizarUsuario(transformar, null)
    }

    fun actualizarUsuario(
        transformar: (Usuario) -> Usuario,
        onCompletado: ((Boolean) -> Unit)?
    ) {
        val previo = _usuario.value ?: return
        val nuevo = transformar(previo)
        _usuario.value = nuevo
        _temaOscuro.value = nuevo.temaOscuro

        viewModelScope.launch {
            firestoreService.actualizarUsuario(nuevo)
                .onSuccess {
                    onCompletado?.invoke(true)
                }
                .onFailure { e ->
                    // Revertir cambios en caso de falla
                    _usuario.value = previo
                    _temaOscuro.value = previo.temaOscuro
                    _error.value = "Error al guardar los cambios: ${e.message ?: "Inténtalo más tarde."}"
                    onCompletado?.invoke(false)
                }
        }
    }

    fun cambiarTema(activo: Boolean) {
        actualizarUsuario { it.copy(temaOscuro = activo) }
    }

    fun iniciarSesion(correo: String, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            authService.iniciarSesion(correo.trim(), contrasena)
                .onSuccess { userAuth ->
                    firestoreService.obtenerOCrearUsuario(userAuth)
                        .onSuccess { u ->
                            _temaOscuro.value = u.temaOscuro
                            _usuario.value = u
                        }
                        .onFailure {
                            _error.value = "No se pudo cargar tu perfil. Inténtalo nuevamente."
                            _usuario.value = userAuth
                        }
                }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
        }
    }

    fun registrar(nuevoUsuario: Usuario, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            authService.registrar(nuevoUsuario, contrasena)
                .onSuccess { usuarioConUid ->
                    // Guardado directo y explícito del usuario en la colección usuarios/{uid}
                    firestoreService.guardarNuevoUsuario(usuarioConUid)
                        .onSuccess {
                            _temaOscuro.value = usuarioConUid.temaOscuro
                            _usuario.value = usuarioConUid
                        }
                        .onFailure { e ->
                            _error.value = "La cuenta fue creada en Auth, pero ocurrió un error al guardar en Firestore: ${e.message}"
                        }
                }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
        }
    }

    fun registrar(nombre: String, correo: String, contrasena: String) {
        registrar(Usuario(nombre = nombre.trim(), correo = correo.trim()), contrasena)
    }

    fun recargarPerfil() {
        val usuarioActual = _usuario.value ?: return
        viewModelScope.launch {
            firestoreService.obtenerOCrearUsuario(usuarioActual)
                .onSuccess {
                    _usuario.value = it
                    _temaOscuro.value = it.temaOscuro
                }
                .onFailure {
                    _error.value = "No se pudo actualizar tu perfil."
                }
        }
    }

    fun actualizarNombre(nuevoNombre: String, onSuccess: () -> Unit) {
        if (nuevoNombre.isBlank()) {
            _error.value = "El nombre no puede estar vacío."
            return
        }
        val usuarioActual = _usuario.value ?: run {
            _error.value = "No hay sesión activa."
            return
        }
        val nombreLimpio = nuevoNombre.trim()
        _cargando.value = true
        _error.value = null

        viewModelScope.launch {
            authService.actualizarNombre(nombreLimpio)
                .onSuccess {
                    firestoreService.actualizarUsuario(usuarioActual.copy(nombre = nombreLimpio))
                        .onSuccess {
                            _usuario.value = usuarioActual.copy(nombre = nombreLimpio)
                            onSuccess()
                        }
                        .onFailure { _error.value = "Error al guardar en base de datos." }
                }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
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
            authService.cambiarContrasena(nueva)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
        }
    }

    fun cerrarSesion() {
        authService.cerrarSesion()
        _usuario.value = null
        _temaOscuro.value = false
    }

    fun limpiarError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelarEscuchaAuth()
    }
}
