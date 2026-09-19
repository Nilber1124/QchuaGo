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

    /** `true` cuando ya se comprobó si existía una sesión guardada. */
    private val _sesionInicializada = MutableStateFlow(false)
    val sesionInicializada: StateFlow<Boolean> = _sesionInicializada.asStateFlow()

    /** Usuario con sesión activa o `null` sin sesión. */
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    /** Tema oscuro global. */
    private val _temaOscuro = MutableStateFlow(false)
    val temaOscuro: StateFlow<Boolean> = _temaOscuro.asStateFlow()

    /** `true` mientras se ejecuta un login o registro. */
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /** Mensaje de error comprensible o `null`. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
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

    fun registrar(nombre: String, correo: String, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            authService.registrar(correo.trim(), contrasena, nombre.trim())
                .onSuccess { user ->
                    firestoreService.obtenerOCrearUsuario(user)
                        .onSuccess {
                            _temaOscuro.value = it.temaOscuro
                            _usuario.value = it
                        }
                        .onFailure {
                            _error.value = "La cuenta fue creada, pero no se pudo preparar el perfil."
                            _usuario.value = user
                        }
                }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
        }
    }

    fun cambiarTema(activo: Boolean) {
        val uid = _usuario.value?.uid ?: return
        val previo = _temaOscuro.value
        val usuarioPrevio = _usuario.value
        _temaOscuro.value = activo
        _usuario.value = usuarioPrevio?.copy(temaOscuro = activo)

        viewModelScope.launch {
            firestoreService.actualizarTemaOscuro(uid, activo).onFailure {
                _temaOscuro.value = previo // revert on failure
                _usuario.value = usuarioPrevio
                _error.value = "Error al guardar el tema."
            }
        }
    }

    /** Recarga el perfil al volver a abrir la pestaña Perfil. */
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
        _cargando.value = true
        _error.value = null
        val usuarioActual = _usuario.value
        val uid = usuarioActual?.uid
        if (uid == null) {
            _error.value = "No hay sesión activa."
            _cargando.value = false
            return
        }

        viewModelScope.launch {
            authService.actualizarNombre(nuevoNombre)
                .onSuccess {
                    firestoreService.actualizarNombre(uid, nuevoNombre)
                        .onSuccess {
                            // Actualizar estado local
                            _usuario.value = usuarioActual.copy(nombre = nuevoNombre.trim())
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
}
