package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.qchuago.ui.viewmodel.Recurso

/*
 * Componentes reutilizables por varias pantallas.
 * Antes de copiar un bloque de "Cargando..." o "Error + Reintentar", usa uno de estos.
 */

/** Ruedita de carga centrada, con un texto opcional debajo. */
@Composable
fun PantallaCargando(
    modifier: Modifier = Modifier,
    texto: String? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            if (texto != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Mensaje de error centrado con botón "Reintentar".
 * Si se pasa [onVolver], también muestra un botón "Volver".
 */
@Composable
fun PantallaError(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onReintentar) { Text("Reintentar") }
            if (onVolver != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onVolver) { Text("Volver") }
            }
        }
    }
}

/**
 * Muestra automáticamente cargando / error según el [recurso], y solo te pide
 * qué dibujar cuando los datos llegaron bien.
 *
 * Ejemplo:
 * ```
 * ContenidoRecurso(estado, onReintentar = vm::cargar) { palabras ->
 *     LazyColumn { items(palabras) { Text(it.quechua) } }
 * }
 * ```
 */
@Composable
fun <T> ContenidoRecurso(
    recurso: Recurso<T>,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    textoCargando: String? = null,
    contenido: @Composable (datos: T) -> Unit,
) {
    when (recurso) {
        is Recurso.Cargando -> PantallaCargando(modifier, textoCargando)
        is Recurso.Error -> PantallaError(recurso.mensaje, onReintentar, modifier)
        is Recurso.Exito -> contenido(recurso.datos)
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaErrorPreview() {
    PantallaError(mensaje = "No se pudieron cargar las unidades.", onReintentar = {}, onVolver = {})
}
