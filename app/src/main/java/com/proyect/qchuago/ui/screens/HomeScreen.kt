package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.proyect.qchuago.R
import com.proyect.qchuago.data.entidad.Usuario
import com.proyect.qchuago.ui.navigation.Rutas

enum class NavTab(
    val title: String,
    val iconRes: Int,
) {
    INICIO("Inicio", R.drawable.ic_nav_inicio),
    PROGRESO("Progreso", R.drawable.ic_nav_progreso),
    DICCIONARIO("Dicc.", R.drawable.ic_nav_dicc),
    REPASO("Repaso", R.drawable.ic_nav_repaso),
    PERFIL("Perfil", R.drawable.ic_nav_perfil),
}

@Composable
fun HomeScreen(
    navController: NavController,
    usuario: Usuario?,
    error: String?,
    temaOscuro: Boolean,
    onCambiarTema: (Boolean) -> Unit,
    onCargarPerfil: () -> Unit,
    onActualizarNombre: (String, () -> Unit) -> Unit,
    onCambiarContrasena: (String, String, () -> Unit) -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.INICIO) }

    LaunchedEffect(usuario) {
        if (usuario == null) {
            navController.navigate(Rutas.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            QchuaGoBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavTab.INICIO -> InicioScreen()
                NavTab.PROGRESO -> ProgresoScreen()
                NavTab.DICCIONARIO -> DiccionarioScreen()
                NavTab.REPASO -> RepasoScreen()
                NavTab.PERFIL -> PerfilScreen(
                    usuario = usuario,
                    error = error,
                    temaOscuro = temaOscuro,
                    onCambiarTema = onCambiarTema,
                    onCargarPerfil = onCargarPerfil,
                    onActualizarNombre = onActualizarNombre,
                    onCambiarContrasena = onCambiarContrasena,
                    onCerrarSesion = onCerrarSesion
                )
            }
        }
    }
}

@Composable
fun QchuaGoBottomBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        // Línea divisoria superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab.entries.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedTextColor = MaterialTheme.colorScheme.primary
    val selectedBgColor = MaterialTheme.colorScheme.surfaceVariant

    if (isSelected) {
        Surface(
            color = selectedBgColor,
            shape = RoundedCornerShape(20.dp),
            modifier = modifier
                .padding(horizontal = 2.dp)
                .clickable(onClick = onClick)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(id = tab.iconRes),
                    contentDescription = tab.title,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = selectedTextColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(onClick = onClick)
        ) {
            Image(
                painter = painterResource(id = tab.iconRes),
                contentDescription = tab.title,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = unselectedTextColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
