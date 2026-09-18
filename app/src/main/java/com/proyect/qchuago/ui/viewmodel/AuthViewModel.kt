package com.proyect.qchuago.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.data.servicio.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthService = AuthService(),
) : ViewModel() {

    /** `true` cuando ya se comprobó si existía una sesión guardada. */
    private val _sesionInicializada = MutableStateFlow(false)
    val sesionInicializada: StateFlow<Boolean> = _sesionInicializada.asStateFlow()

    /** Usuario con sesión activa o `null` sin sesión. */
    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    /** `true` mientras se ejecuta un login o registro. */
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /** Mensaje de error comprensible o `null`. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        _usuario.value = authService.usuarioActual
        _sesionInicializada.value = true
    }

    fun iniciarSesion(correo: String, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            authService.iniciarSesion(correo.trim(), contrasena)
                .onSuccess { _usuario.value = it }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
        }
    }

    fun registrar(nombre: String, correo: String, contrasena: String) {
        _cargando.value = true
        _error.value = null
        viewModelScope.launch {
            authService.registrar(correo.trim(), contrasena, nombre.trim())
                .onSuccess { _usuario.value = it }
                .onFailure { _error.value = it.message ?: "Ocurrió un error inesperado." }
            _cargando.value = false
        }
    }

    fun cerrarSesion() {
        authService.cerrarSesion()
        _usuario.value = null
    }
}