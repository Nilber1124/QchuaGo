package com.proyect.qchuago.data.entidad

enum class TipoEntradaDiccionario { PALABRA, EXPRESION }

data class EntradaDiccionario(
    val tipo: TipoEntradaDiccionario,
    val quechua: String,
    val espanol: String,
    val alias: List<String> = emptyList(),
    val nota: String = "",
)
