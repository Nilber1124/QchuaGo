package com.proyect.qchuago.ui.viewmodel

import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.TipoActividad

/**
 * Contenido estático de demostración para el "Test de nivel" del onboarding.
 * No proviene de Firestore: es una lección fija usada únicamente para mostrar
 * el flujo mientras no exista una evaluación real que determine el nivel del usuario.
 */
object ActividadesPruebaNivel {
    val lista: List<Actividad> = listOf(
        Actividad(
            id = "1",
            tipo = TipoActividad.VOCABULARIO,
            orden = 1,
            quechua = "Allin p'unchay",
            traduccion = "Buenos días",
            explicacion = "Saludo usado durante la mañana en quechua.",
        ),
        Actividad(
            id = "2",
            tipo = TipoActividad.SELECCION,
            orden = 2,
            enunciado = "¿Cómo se dice 'gracias' en quechua?",
            opciones = listOf("Yupaychani", "Allillanchu", "Ñuqa", "Kunan"),
            respuestaCorrecta = "Yupaychani",
        ),
        Actividad(
            id = "3",
            tipo = TipoActividad.COMPLETAR,
            orden = 3,
            enunciado = "Completa: '_____, imaynallan kashanki?' (Hola, ¿cómo estás?)",
            opciones = listOf("Napaykullayki", "Rimaykullayki", "Añay"),
            respuestaCorrecta = "Napaykullayki",
        ),
        Actividad(
            id = "4",
            tipo = TipoActividad.ORDENAR,
            orden = 4,
            enunciado = "Ordena las palabras para formar: 'Yo aprendo quechua'",
            elementos = listOf("Ñuqa", "qichwata", "yachakuni"),
            respuestasCorrectas = listOf("Ñuqa", "qichwata", "yachakuni"),
        ),
        Actividad(
            id = "5",
            tipo = TipoActividad.ASOCIAR,
            orden = 5,
            enunciado = "Empareja cada palabra en quechua con su traducción",
            elementos = listOf("Yaku", "Inti", "Killa"),
            opciones = listOf("Luna", "Agua", "Sol"),
            respuestasCorrectas = listOf("Agua", "Sol", "Luna"),
        ),
    )
}
