package com.proyect.qchuago.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.proyect.qchuago.data.entidad.Usuario

@Composable
fun BienvenidaScreen(
    usuario: Usuario?,
    onEmpezar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nombre = usuario?.nombre?.takeIf { it.isNotBlank() } ?: "amigo"

    Column(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🦙", fontSize = 90.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = buildAnnotatedString {
                append("¡Allin p'unchay, ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append(nombre)
                }
                append("!")
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "El quechua es el idioma de los Incas, hablado por más de 8 millones de personas en los Andes.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Vamos a personalizar tu camino de aprendizaje.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onEmpezar,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("¡Empezar! →", fontWeight = FontWeight.Bold)
        }
    }
}
