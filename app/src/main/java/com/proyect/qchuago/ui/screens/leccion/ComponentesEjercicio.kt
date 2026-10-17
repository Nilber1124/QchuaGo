package com.proyect.qchuago.ui.screens.leccion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Barra de progreso superior de la lección (un segmento por actividad). */
@Composable
internal fun BarraProgresoLeccion(
    total: Int,
    indiceActual: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (i <= indiceActual) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}

/** Tarjeta de opción seleccionable, usada por SELECCION y ASOCIAR. */
@Composable
internal fun OpcionTarjeta(
    texto: String,
    seleccionada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (seleccionada) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = habilitada, onClick = onClick),
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(16.dp),
            color = if (seleccionada) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** Chip simple, usado por ORDENAR y el banco de palabras de COMPLETAR. */
@Composable
internal fun Chip(
    texto: String,
    habilitada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(enabled = habilitada, onClick = onClick),
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
