package com.proyect.qchuago.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proyect.qchuago.data.entidad.EntradaDiccionario
import com.proyect.qchuago.data.entidad.TipoEntradaDiccionario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ARCHIVO_DICCIONARIO = "diccionario_quechua.csv"

/** Carga el diccionario local (bundleado en assets/, sin Firestore) una sola vez. */
class DiccionarioViewModel(private val context: Context) : ViewModel() {

    private val _estado = MutableStateFlow<Recurso<List<EntradaDiccionario>>>(Recurso.Cargando)
    val estado: StateFlow<Recurso<List<EntradaDiccionario>>> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = Recurso.Cargando
        viewModelScope.launch {
            _estado.value = cargarComoRecurso("No se pudo cargar el diccionario.") {
                withContext(Dispatchers.IO) {
                    context.assets.open(ARCHIVO_DICCIONARIO).bufferedReader(Charsets.UTF_8).useLines { lineas ->
                        lineas.drop(1).filter { it.isNotBlank() }.mapNotNull(::parsearLinea).toList()
                    }
                }
            }
        }
    }
}

private fun parsearLinea(linea: String): EntradaDiccionario? {
    val campos = dividirLineaCsv(linea)
    if (campos.size < 3) return null
    val tipo = if (campos[0].trim() == "expresion") {
        TipoEntradaDiccionario.EXPRESION
    } else {
        TipoEntradaDiccionario.PALABRA
    }
    val alias = campos.getOrNull(3)
        ?.split(";")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?: emptyList()
    return EntradaDiccionario(
        tipo = tipo,
        quechua = campos[1].trim(),
        espanol = campos[2].trim(),
        alias = alias,
        nota = campos.getOrNull(4)?.trim().orEmpty(),
    )
}

/** Separador de CSV minimalista: soporta campos entre comillas con comas internas (RFC4180 básico). */
private fun dividirLineaCsv(linea: String): List<String> {
    val campos = mutableListOf<String>()
    val actual = StringBuilder()
    var entreComillas = false
    var i = 0
    while (i < linea.length) {
        val c = linea[i]
        when {
            c == '"' -> {
                if (entreComillas && i + 1 < linea.length && linea[i + 1] == '"') {
                    actual.append('"')
                    i++
                } else {
                    entreComillas = !entreComillas
                }
            }
            c == ',' && !entreComillas -> {
                campos.add(actual.toString())
                actual.clear()
            }
            else -> actual.append(c)
        }
        i++
    }
    campos.add(actual.toString())
    return campos
}
