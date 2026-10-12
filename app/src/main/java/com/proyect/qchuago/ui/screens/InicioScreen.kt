package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Unidad
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.ui.viewmodel.AuthViewModel
import com.proyect.qchuago.ui.viewmodel.CursoViewModel
import com.proyect.qchuago.ui.viewmodel.EstadoCurso

@Composable
fun InicioScreen(
    authViewModel: AuthViewModel,
    cursoViewModel: CursoViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val usuario by authViewModel.usuario.collectAsState()
    val estadoCurso by cursoViewModel.estadoCurso.collectAsState()
    val leccionesCompletadasIds = usuario?.leccionesCompletadasIds ?: emptyList()

    Column(modifier = modifier.fillMaxSize()) {
        BarraSuperiorAprendizaje(usuario = usuario)

        when (val estado = estadoCurso) {
            is EstadoCurso.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is EstadoCurso.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { cursoViewModel.cargarUnidades() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is EstadoCurso.Exito -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    estado.unidades.forEach { unidad ->
                        val unidadDesbloqueada =
                            cursoViewModel.estaDesbloqueadaUnidad(unidad, leccionesCompletadasIds)
                        item {
                            BannerUnidad(unidad = unidad, desbloqueada = unidadDesbloqueada)
                        }
                        itemsIndexed(unidad.lecciones) { index, leccion ->
                            val desbloqueada =
                                cursoViewModel.estaDesbloqueada(leccion, leccionesCompletadasIds)
                            BurbujaLeccion(
                                leccion = leccion,
                                index = index,
                                desbloqueada = desbloqueada,
                                colorUnidad = parsearColor(unidad.colorHex),
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BarraSuperiorAprendizaje(
    usuario: Usuario?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 20.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = "${usuario?.racha ?: 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "${usuario?.xp ?: 0} XP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "NV. ${nivelANumero(usuario?.nivel)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val vidas = usuario?.vidas ?: 5
            repeat(5) { i ->
                Text(
                    text = if (i < vidas) "❤️" else "🤍",
                    fontSize = 16.sp,
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun BannerUnidad(
    unidad: Unidad,
    desbloqueada: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorFondo = if (desbloqueada) parsearColor(unidad.colorHex) else Color(0xFFE0E0E0)
    val colorTexto = if (desbloqueada) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorFondo)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(unidad.icono, fontSize = 32.sp)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = unidad.nombre,
                color = colorTexto,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = unidad.descripcion,
                color = colorTexto.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BurbujaLeccion(
    leccion: Leccion,
    index: Int,
    desbloqueada: Boolean,
    colorUnidad: Color,
    modifier: Modifier = Modifier,
) {
    val offsetX = when (index % 4) {
        1 -> (-64).dp
        3 -> 64.dp
        else -> 0.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = offsetX),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (desbloqueada) colorUnidad else Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center,
            ) {
                if (desbloqueada) {
                    Text(leccion.icono, fontSize = 32.sp)
                } else {
                    Text("🔒", fontSize = 28.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = leccion.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (desbloqueada) FontWeight.Bold else FontWeight.Normal,
                color = if (desbloqueada) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun parsearColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF4CAF50)
}

private fun nivelANumero(nivel: String?): Int = when (nivel?.lowercase()) {
    "principiante" -> 1
    "básico", "basico" -> 2
    "intermedio" -> 3
    "avanzado" -> 4
    else -> 1
}
