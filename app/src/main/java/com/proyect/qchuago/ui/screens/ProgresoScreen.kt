package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Unidad
import com.proyect.qchuago.ui.navigation.Rutas
import com.proyect.qchuago.ui.theme.colorDesdeHex
import com.proyect.qchuago.ui.viewmodel.AuthViewModel
import com.proyect.qchuago.ui.viewmodel.CursoViewModel
import com.proyect.qchuago.ui.viewmodel.EstadoCurso

/**
 * Comparte el [CursoViewModel] de Inicio (misma pantalla Home, mismo
 * ViewModelStoreOwner) para no recargar el curso ni duplicar la lógica de
 * bloqueo/completado de RF07.
 */
@Composable
fun ProgresoScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    cursoViewModel: CursoViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val usuario by authViewModel.usuario.collectAsState()
    val estadoCurso by cursoViewModel.estadoCurso.collectAsState()
    val leccionesCompletadasIds = usuario?.leccionesCompletadasIds ?: emptyList()

    Box(modifier = modifier.fillMaxSize()) {
        when (val estado = estadoCurso) {
            is EstadoCurso.Cargando -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is EstadoCurso.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text(
                            text = estado.mensaje,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { cursoViewModel.reintentar() }) { Text("Reintentar") }
                    }
                }
            }
            is EstadoCurso.Exito -> {
                // Obtenemos todas las métricas de avance en una sola línea clara
                val resumen = remember(estado.unidades, leccionesCompletadasIds) {
                    cursoViewModel.calcularResumenProgreso(estado.unidades, leccionesCompletadasIds)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    // 1. Título y subtítulo de la pantalla
                    item(key = "titulo") {
                        Text(
                            text = "Tu progreso",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Unidades y lecciones completadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // 2. Tarjeta con barra y estadísticas generales
                    item(key = "resumen") {
                        TarjetaResumenProgreso(
                            resumen = resumen,
                            totalUnidades = estado.unidades.size,
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    // 3. Tarjeta destacada para continuar con la siguiente lección
                    item(key = "continuar") {
                        TarjetaContinuar(
                            siguiente = resumen.siguienteLeccion,
                            onContinuar = { (unidad, leccion) ->
                                navController.navigate("${Rutas.LECCION}/${unidad.id}/${leccion.id}")
                            },
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    // 4. Lista de unidades del curso con su barra individual
                    item(key = "titulo_unidades") {
                        Text(
                            text = "Unidades",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    items(estado.unidades, key = { it.id }) { unidad ->
                        val completadasUnidad = unidad.lecciones.count {
                            cursoViewModel.estaCompletada(unidad, it, leccionesCompletadasIds)
                        }
                        FilaUnidadProgreso(
                            unidad = unidad,
                            completadas = completadasUnidad,
                            total = unidad.lecciones.size,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta superior que muestra el porcentaje general y contadores de lecciones y unidades.
 */
@Composable
private fun TarjetaResumenProgreso(
    resumen: com.proyect.qchuago.ui.viewmodel.ResumenProgresoCurso,
    totalUnidades: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Avance general",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${resumen.porcentaje}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { resumen.progresoFraccion },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EstadisticaProgreso(
                    valor = "${resumen.leccionesCompletadas}/${resumen.totalLecciones}",
                    etiqueta = "Lecciones",
                    modifier = Modifier.weight(1f),
                )
                EstadisticaProgreso(
                    valor = "${resumen.unidadesCompletadas}/$totalUnidades",
                    etiqueta = "Unidades",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EstadisticaProgreso(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TarjetaContinuar(
    siguiente: Pair<Unidad, Leccion>?,
    onContinuar: (Pair<Unidad, Leccion>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (siguiente != null) {
            val (unidad, leccion) = siguiente
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(leccion.icono, fontSize = 24.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Continuar aprendiendo",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "${unidad.nombre.substringAfter(": ").ifBlank { unidad.nombre }} · ${leccion.nombre}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { onContinuar(siguiente) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Continuar") }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🎉", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "¡Completaste todas las lecciones disponibles!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun FilaUnidadProgreso(
    unidad: Unidad,
    completadas: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val progreso = if (total > 0) completadas.toFloat() / total else 0f
    val colorUnidad = colorDesdeHex(unidad.colorHex, MaterialTheme.colorScheme.primary)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(colorUnidad.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(unidad.icono, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unidad.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    color = colorUnidad,
                    strokeCap = StrokeCap.Round,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "$completadas/$total",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
