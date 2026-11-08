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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.proyect.qchuago.data.entidad.Leccion
import com.proyect.qchuago.data.entidad.Unidad
import com.proyect.qchuago.data.entidad.UsuarioRanking
import com.proyect.qchuago.ui.navigation.Rutas
import com.proyect.qchuago.ui.theme.colorDesdeHex
import com.proyect.qchuago.ui.viewmodel.AuthViewModel
import com.proyect.qchuago.ui.viewmodel.CursoViewModel
import com.proyect.qchuago.ui.viewmodel.RankingViewModel
import com.proyect.qchuago.ui.viewmodel.Recurso

/**
 * Recibe el mismo [CursoViewModel] creado una sola vez en `HomeScreen` y compartido
 * con Inicio y Perfil, para no recargar el curso ni duplicar la lógica de
 * bloqueo/completado de RF07.
 *
 * Muestra el progreso del curso y al final el ranking semanal (RF20).
 */
@Composable
fun ProgresoScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    cursoViewModel: CursoViewModel = viewModel(),
    rankingViewModel: RankingViewModel = viewModel(),
    modifier: Modifier = Modifier,
) {
    val usuario by authViewModel.usuario.collectAsState()
    val estadoCurso by cursoViewModel.estadoCurso.collectAsState()
    val rankingState by rankingViewModel.ranking.collectAsState()
    val miRankingFueraDeTop by rankingViewModel.miRankingFueraDeTop.collectAsState()

    val leccionesCompletadasIds = usuario?.leccionesCompletadasIds ?: emptyList()
    val evaluacionesAprobadasIds = usuario?.evaluacionesAprobadasIds ?: emptyList()

    val resumen = cursoViewModel.resumen(leccionesCompletadasIds, evaluacionesAprobadasIds)

    LaunchedEffect(usuario?.uid) {
        rankingViewModel.cargarRanking(usuario?.uid)
    }

    ContenidoRecurso(
        recurso = estadoCurso,
        onReintentar = cursoViewModel::reintentar,
        modifier = modifier,
    ) { unidades ->
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
                    totalUnidades = unidades.size,
                )
                Spacer(Modifier.height(16.dp))
            }

            // 3. Tarjeta destacada para continuar con la siguiente lección
            item(key = "continuar") {
                TarjetaContinuar(
                    siguiente = resumen.siguienteLeccion,
                    siguienteEvaluacion = resumen.siguienteEvaluacion,
                    onContinuar = { (unidad, leccion) ->
                        navController.navigate(Rutas.leccion(unidad.id, leccion.id))
                    },
                    onAbrirEvaluacion = { unidad ->
                        navController.navigate(Rutas.evaluacion(unidad.id))
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
            items(unidades, key = { it.id }) { unidad ->
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

            // 5. Sección de Ranking Semanal (RF20)
            item(key = "titulo_ranking") {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "🏆 Ranking semanal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Estudiantes con más XP ganados esta semana",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
            }

            item(key = "contenido_ranking") {
                SeccionRankingSemanal(
                    recursoRanking = rankingState,
                    miRankingFueraDeTop = miRankingFueraDeTop,
                    uidUsuarioActual = usuario?.uid,
                    onReintentar = { rankingViewModel.reintentar(usuario?.uid) },
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Seccion completa del ranking semanal: podio de 3 primeros + clasificacion.
 */
@Composable
private fun SeccionRankingSemanal(
    recursoRanking: Recurso<List<UsuarioRanking>>,
    miRankingFueraDeTop: UsuarioRanking?,
    uidUsuarioActual: String?,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (recursoRanking) {
        is Recurso.Cargando -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        is Recurso.Error -> {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = recursoRanking.mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onReintentar) {
                        Text("Reintentar")
                    }
                }
            }
        }
        is Recurso.Exito -> {
            val lista = recursoRanking.datos
            if (lista.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🌱", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "¡Aún no hay participantes esta semana!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sé el primero en completar una lección para liderar el ranking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(modifier = modifier.fillMaxWidth()) {
                    // Podio visual para los 3 primeros
                    PodioRanking(
                        usuarios = lista.take(3),
                        uidUsuarioActual = uidUsuarioActual,
                    )

                    // Clasificacion para los siguientes puestos
                    val restoUsuarios = lista.drop(3)
                    if (restoUsuarios.isNotEmpty() || miRankingFueraDeTop != null) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "Clasificación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(10.dp))

                        restoUsuarios.forEach { user ->
                            FilaUsuarioRanking(
                                usuario = user,
                                esUsuarioActual = user.uid == uidUsuarioActual,
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        if (miRankingFueraDeTop != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Tu posición actual",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            FilaUsuarioRanking(
                                usuario = miRankingFueraDeTop,
                                esUsuarioActual = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Podio visual de 3 barras verticales: Segundo (izq), Primero (centro), Tercero (der).
 */
@Composable
private fun PodioRanking(
    usuarios: List<UsuarioRanking>,
    uidUsuarioActual: String?,
    modifier: Modifier = Modifier,
) {
    val primero = usuarios.getOrNull(0)
    val segundo = usuarios.getOrNull(1)
    val tercero = usuarios.getOrNull(2)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            // SEGUNDO PUESTO (Izquierda)
            ColumnaPodio(
                usuario = segundo,
                posicion = 2,
                medalla = "🥈",
                alturaBarra = 85.dp,
                colorBarra = Color(0xFFC0C0C0), // Plata
                esUsuarioActual = segundo?.uid == uidUsuarioActual,
                modifier = Modifier.weight(1f),
            )

            // PRIMER PUESTO (Centro - Mas alto)
            ColumnaPodio(
                usuario = primero,
                posicion = 1,
                medalla = "🥇",
                alturaBarra = 115.dp,
                colorBarra = Color(0xFFFFD700), // Oro
                esUsuarioActual = primero?.uid == uidUsuarioActual,
                modifier = Modifier.weight(1f),
            )

            // TERCER PUESTO (Derecha)
            ColumnaPodio(
                usuario = tercero,
                posicion = 3,
                medalla = "🥉",
                alturaBarra = 65.dp,
                colorBarra = Color(0xFFCD7F32), // Bronce
                esUsuarioActual = tercero?.uid == uidUsuarioActual,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColumnaPodio(
    usuario: UsuarioRanking?,
    posicion: Int,
    medalla: String,
    alturaBarra: androidx.compose.ui.unit.Dp,
    colorBarra: Color,
    esUsuarioActual: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        if (usuario != null) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (esUsuarioActual) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (esUsuarioActual) 2.dp else 1.dp,
                        color = if (esUsuarioActual) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usuario.nombre.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (esUsuarioActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))

            Text(
                text = if (esUsuarioActual) "${usuario.nombre} (Tú)" else usuario.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "${usuario.xpSemanal} XP",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alturaBarra)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(colorBarra)
                    .border(
                        width = if (esUsuarioActual) 2.dp else 0.dp,
                        color = if (esUsuarioActual) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(medalla, fontSize = 22.sp)
                    Text(
                        text = "#$posicion",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alturaBarra)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "#$posicion",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

/**
 * Fila individual para la lista de clasificacion (#4 en adelante).
 */
@Composable
private fun FilaUsuarioRanking(
    usuario: UsuarioRanking,
    esUsuarioActual: Boolean,
    modifier: Modifier = Modifier,
) {
    val fondo = if (esUsuarioActual) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val colorBorde = if (esUsuarioActual) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = fondo,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colorBorde, RoundedCornerShape(12.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${usuario.posicion}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (esUsuarioActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp),
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (esUsuarioActual) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = usuario.nombre.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (esUsuarioActual) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = if (esUsuarioActual) "${usuario.nombre} (Tú)" else usuario.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (esUsuarioActual) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = "${usuario.xpSemanal} XP",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
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
    siguienteEvaluacion: Unidad?,
    onContinuar: (Pair<Unidad, Leccion>) -> Unit,
    onAbrirEvaluacion: (Unidad) -> Unit,
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
        } else if (siguienteEvaluacion != null) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📝", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Evaluación disponible",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = siguienteEvaluacion.nombre,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Button(
                    onClick = { onAbrirEvaluacion(siguienteEvaluacion) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Empezar") }
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
