package com.proyect.qchuago.ui.viewmodel

import com.proyect.qchuago.data.entidad.Actividad

/**
 * Contenido estático de demostración para el "Test de nivel" del onboarding.
 * No proviene de Firestore: es una lección fija usada únicamente para mostrar
 * el flujo mientras no exista una evaluación real que determine el nivel del usuario.
 */
object ActividadesPruebaNivel {
    val lista: List<Actividad> = listOf(
        Actividad(
            id = "1",
            tipo = "VOCABULARIO",
            orden = 1,
            quechua = "Allin p'unchay",
            traduccion = "Buenos días",
            explicacion = "Saludo usado durante la mañana en quechua.",
        ),
        Actividad(
            id = "2",
            tipo = "SELECCION",
            orden = 2,
            enunciado = "¿Cómo se dice 'gracias' en quechua?",
            opciones = listOf("Yupaychani", "Allillanchu", "Ñuqa", "Kunan"),
            respuestaCorrecta = "Yupaychani",
        ),
        Actividad(
            id = "3",
            tipo = "COMPLETAR",
            orden = 3,
            enunciado = "Completa: '_____, imaynallan kashanki?' (Hola, ¿cómo estás?)",
            respuestaCorrecta = "Napaykullayki",
        ),
        Actividad(
            id = "4",
            tipo = "ORDENAR",
            orden = 4,
            enunciado = "Ordena las palabras para formar: 'Yo aprendo quechua'",
            elementos = listOf("Ñuqa", "qichwata", "yachakuni"),
            respuestasCorrectas = listOf("Ñuqa", "qichwata", "yachakuni"),
        ),
    )
}
