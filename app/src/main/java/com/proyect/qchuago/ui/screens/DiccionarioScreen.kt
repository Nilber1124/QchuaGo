package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proyect.qchuago.data.entidad.EntradaDiccionario
import com.proyect.qchuago.data.entidad.TipoEntradaDiccionario
import com.proyect.qchuago.ui.viewmodel.DiccionarioViewModel

@Composable
fun DiccionarioScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: DiccionarioViewModel = viewModel(
        factory = viewModelFactory { initializer { DiccionarioViewModel(context.applicationContext) } },
    )
    val estado by viewModel.estado.collectAsState()
    var busqueda by remember { mutableStateOf("") }
    var mostrarDesafio by remember { mutableStateOf(false) }

    ContenidoRecurso(
        recurso = estado,
        onReintentar = viewModel::cargar,
        modifier = modifier.fillMaxSize(),
        textoCargando = "Cargando diccionario...",
    ) { entradas ->
        val filtradas = remember(busqueda, entradas) {
            if (busqueda.isBlank()) {
                entradas
            } else {
                entradas.filter {
                    it.quechua.contains(busqueda, ignoreCase = true) ||
                        it.espanol.contains(busqueda, ignoreCase = true) ||
                        it.alias.any { alias -> alias.contains(busqueda, ignoreCase = true) }
                }
            }
        }
        val sinTraduccionLiteral = busqueda.isNotBlank() && filtradas.isNotEmpty() &&
            filtradas.none {
                it.quechua.contains(busqueda, ignoreCase = true) || it.espanol.contains(busqueda, ignoreCase = true)
            }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = "Diccionario",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar en quechua o español") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                if (sinTraduccionLiteral) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "💡 \"$busqueda\" no existe como palabra o frase literal en " +
                                "quechua. Lo que ves abajo son las expresiones que se usan como equivalente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (filtradas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Sin resultados para \"$busqueda\"",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(filtradas, key = { "${it.tipo}-${it.quechua}" }) { entrada ->
                            FilaDiccionario(entrada)
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }

            ExtendedFloatingActionButton(
                onClick = { mostrarDesafio = true },
                icon = { Text("🎯") },
                text = { Text("Desafío") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )

            if (mostrarDesafio) {
                DesafioDiccionario(entradas = entradas, onCerrar = { mostrarDesafio = false })
            }
        }
    }
}

@Composable
private fun FilaDiccionario(
    entrada: EntradaDiccionario,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entrada.quechua,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                EtiquetaTipo(entrada.tipo)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = entrada.espanol,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entrada.nota.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "💡 ${entrada.nota}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EtiquetaTipo(
    tipo: TipoEntradaDiccionario,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = if (tipo == TipoEntradaDiccionario.EXPRESION) "Expresión" else "Palabra",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
