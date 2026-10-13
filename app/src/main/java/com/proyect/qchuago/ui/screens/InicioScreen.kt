package com.proyect.qchuago.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.delay

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

        AnimatedContent(
            targetState = estadoCurso,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            label = "estado_curso",
            modifier = Modifier.fillMaxSize(),
        ) { estado ->
            when (estado) {
                is EstadoCurso.Cargando -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Cargando lecciones...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                is EstadoCurso.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp),
                        ) {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = estado.mensaje,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { cursoViewModel.cargarUnidades() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                is EstadoCurso.Exito -> {
                    val siguienteLeccionId = remember(leccionesCompletadasIds, estado.unidades) {
                        estado.unidades.flatMap { it.lecciones }
                            .firstOrNull { it.id !in leccionesCompletadasIds }?.id
                    }
                    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                        estado.unidades.forEach { unidad ->
                            val unidadDesbloqueada = cursoViewModel.estaDesbloqueadaUnidad(
                                unidad, leccionesCompletadasIds
                            )
                            item(key = "banner_${unidad.id}") {
                                BannerUnidad(unidad = unidad, desbloqueada = unidadDesbloqueada)
                            }
                            itemsIndexed(
                                items = unidad.lecciones,
                                key = { _, lec -> "lec_${unidad.id}_${lec.id}" },
                            ) { index, leccion ->
                                val desbloqueada = cursoViewModel.estaDesbloqueada(
                                    leccion, leccionesCompletadasIds
                                )
                                BurbujaLeccion(
                                    leccion = leccion,
                                    index = index,
                                    desbloqueada = desbloqueada,
                                    esSiguiente = leccion.id == siguienteLeccionId,
                                    colorUnidad = parsearColor(unidad.colorHex),
                                )
                            }
                            item(key = "spacer_${unidad.id}") {
                                Spacer(Modifier.height(20.dp))
                            }
                        }
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
    val progreso = if ((usuario?.totalLecciones ?: 0) > 0)
        (usuario?.leccionesCompletadas ?: 0).toFloat() / (usuario?.totalLecciones ?: 1).toFloat()
    else 0f

    val progresoAnimado by animateFloatAsState(
        targetValue = progreso,
        animationSpec = tween(800),
        label = "progreso_xp",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Racha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 20.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${usuario?.racha ?: 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            // XP + barra + nivel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            ) {
                Text(
                    text = "${usuario?.xp ?: 0} XP",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { progresoAnimado },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(50)),
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "NV. ${nivelANumero(usuario?.nivel)}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            // Vidas
            Row(verticalAlignment = Alignment.CenterVertically) {
                val vidas = usuario?.vidas ?: 5
                repeat(5) { i ->
                    Text(text = if (i < vidas) "❤️" else "🤍", fontSize = 14.sp)
                }
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun BannerUnidad(
    unidad: Unidad,
    desbloqueada: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorFondo = if (desbloqueada) parsearColor(unidad.colorHex) else Color(0xFF9E9E9E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(elevation = if (desbloqueada) 8.dp else 2.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(colorFondo),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(unidad.icono, fontSize = 28.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = unidad.nombre,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = unidad.descripcion,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${unidad.lecciones.size}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    text = "lecciones",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (!desbloqueada) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("🔒", fontSize = 26.sp, modifier = Modifier.padding(end = 22.dp))
            }
        }
    }
}

@Composable
private fun BurbujaLeccion(
    leccion: Leccion,
    index: Int,
    desbloqueada: Boolean,
    esSiguiente: Boolean,
    colorUnidad: Color,
    modifier: Modifier = Modifier,
) {
    var aparecio by remember { mutableStateOf(false) }
    LaunchedEffect(leccion.id) {
        delay(index * 70L)
        aparecio = true
    }

    val opacidad by animateFloatAsState(
        targetValue = if (aparecio) 1f else 0f,
        animationSpec = tween(350),
        label = "opacidad_${leccion.id}",
    )
    val escalaEntrada by animateFloatAsState(
        targetValue = if (aparecio) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "escala_${leccion.id}",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_${leccion.id}")
    val escalaPulso by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (esSiguiente) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_val_${leccion.id}",
    )

    val offsetX = when (index % 4) {
        1 -> (-64).dp
        3 -> 64.dp
        else -> 0.dp
    }

    val esCheckpoint = leccion.tipo == "checkpoint"
    val colorBurbuja = when {
        !desbloqueada -> Color(0xFFE0E0E0)
        esCheckpoint -> Color(0xFFFDD835)
        else -> colorUnidad
    }
    val tamano = if (esCheckpoint) 90.dp else 80.dp
    val iconoSize = if (esCheckpoint) 38.sp else 30.sp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(opacidad)
            .scale(escalaEntrada * escalaPulso),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = offsetX),
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(tamano)
                        .shadow(
                            elevation = if (desbloqueada) 10.dp else 0.dp,
                            shape = CircleShape,
                            ambientColor = colorBurbuja,
                            spotColor = colorBurbuja,
                        )
                        .clip(CircleShape)
                        .background(colorBurbuja),
                    contentAlignment = Alignment.Center,
                ) {
                    if (desbloqueada) {
                        Text(leccion.icono, fontSize = iconoSize)
                    } else {
                        Text("🔒", fontSize = 26.sp)
                    }
                }
                if (esSiguiente) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6D00)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "!",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
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
    Color(0xFF388E3C)
}

private fun nivelANumero(nivel: String?): Int = when (nivel?.lowercase()) {
    "principiante" -> 1
    "básico", "basico" -> 2
    "intermedio" -> 3
    "avanzado" -> 4
    else -> 1
}
