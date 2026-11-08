package com.proyect.qchuago.ui.viewmodel

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.proyect.qchuago.data.entidad.Actividad
import com.proyect.qchuago.data.entidad.TipoActividad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

private val TIPOS_EVALUABLES = setOf(
    TipoActividad.SELECCION,
    TipoActividad.COMPLETAR,
    TipoActividad.ORDENAR,
    TipoActividad.ASOCIAR,
    TipoActividad.ESCUCHA,
    TipoActividad.PRONUNCIACION,
)

sealed class EstadoLeccion {
    object Cargando : EstadoLeccion()
    data class Error(val mensaje: String) : EstadoLeccion()
    data class EnCurso(
        val actividades: List<Actividad>,
        val indice: Int,
        val correctas: Int,
        val totalEvaluables: Int,
        val respuestaSeleccionada: String? = null,
        val textoIngresado: String = "",
        val elementosDisponibles: List<String> = emptyList(),
        val ordenActual: List<String> = emptyList(),
        val indiceElementoSeleccionado: Int? = null,
        val pares: Map<Int, Int> = emptyMap(),
        val verificada: Boolean = false,
        val esCorrecta: Boolean? = null,
        val reproduciendoAudio: Boolean = false,
        val grabando: Boolean = false,
        val grabacionDisponible: Boolean = false,
        val evaluandoPronunciacion: Boolean = false,
        val similitudPronunciacion: Int? = null,
        val errorEvaluacion: String? = null,
    ) : EstadoLeccion() {
        val actividadActual: Actividad get() = actividades[indice]
        val esUltima: Boolean get() = indice == actividades.lastIndex
    }
    data class Finalizada(
        val correctas: Int,
        val totalEvaluables: Int,
        val xpGanados: Int = 0,
        val guardando: Boolean = false,
        val error: String? = null,
    ) : EstadoLeccion()
}

/**
 * Coordina una lección: carga sus actividades desde Firestore (o usa actividades
 * estáticas para la prueba de nivel), lleva el estado de cada ejercicio, y maneja
 * la reproducción/grabación de audio de las actividades de tipo
 * VOCABULARIO/PRONUNCIACION. VOCABULARIO es solo práctica (no evaluable); en
 * PRONUNCIACION, además, se califica la grabación por similitud acústica contra
 * el audio de referencia (ver [evaluarPronunciacion] y `SimilitudAudio.kt`).
 */
class LeccionViewModel(
    private val unidadId: String,
    private val leccionId: String,
    private val context: Context,
    private val actividadesEstaticas: List<Actividad>? = null,
    private val uid: String? = null,
) : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val unidadesCollection = firestore.collection("unidades")
    private val usuariosCollection = firestore.collection("usuarios")

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private val archivoGrabacion = File(context.cacheDir, "practica_pronunciacion.3gp")
    private val respuestasComprobadas = mutableMapOf<String, Boolean>()

    private val _estadoLeccion = MutableStateFlow<EstadoLeccion>(EstadoLeccion.Cargando)
    val estadoLeccion: StateFlow<EstadoLeccion> = _estadoLeccion.asStateFlow()

    init {
        cargarActividades()
    }

    fun cargarActividades() {
        _estadoLeccion.value = EstadoLeccion.Cargando
        val estaticas = actividadesEstaticas
        if (estaticas != null) {
            iniciarConActividades(estaticas)
            return
        }
        viewModelScope.launch {
            try {
                val actividades = unidadesCollection.document(unidadId)
                    .collection("lecciones").document(leccionId)
                    .collection("actividades")
                    .orderBy("orden", Query.Direction.ASCENDING)
                    .get().await()
                    .toObjects(Actividad::class.java)
                    .filter { it.activo }
                iniciarConActividades(actividades)
            } catch (e: Exception) {
                _estadoLeccion.value = EstadoLeccion.Error(
                    "No se pudo cargar la lección. Inténtalo nuevamente."
                )
            }
        }
    }

    private fun iniciarConActividades(actividades: List<Actividad>) {
        if (actividades.isEmpty()) {
            _estadoLeccion.value = EstadoLeccion.Error("Esta lección todavía no tiene contenido.")
            return
        }
        _estadoLeccion.value = EstadoLeccion.EnCurso(
            actividades = actividades,
            indice = 0,
            correctas = 0,
            totalEvaluables = actividades.count { it.tipo in TIPOS_EVALUABLES },
            elementosDisponibles = actividades.first().elementos,
        )
    }

    fun reintentar() = cargarActividades()

    fun seleccionarOpcion(opcion: String) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        _estadoLeccion.value = estado.copy(respuestaSeleccionada = opcion)
    }

    fun actualizarTexto(texto: String) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        _estadoLeccion.value = estado.copy(textoIngresado = texto)
    }

    fun agregarElemento(elemento: String) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        _estadoLeccion.value = estado.copy(
            ordenActual = estado.ordenActual + elemento,
            elementosDisponibles = estado.elementosDisponibles - elemento,
        )
    }

    fun quitarUltimoElemento() {
        val estado = enCursoActual() ?: return
        if (estado.verificada || estado.ordenActual.isEmpty()) return
        val elemento = estado.ordenActual.last()
        _estadoLeccion.value = estado.copy(
            ordenActual = estado.ordenActual.dropLast(1),
            elementosDisponibles = estado.elementosDisponibles + elemento,
        )
    }

    fun seleccionarElementoAsociar(indiceElemento: Int) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return
        if (estado.pares.containsKey(indiceElemento)) {
            _estadoLeccion.value = estado.copy(
                pares = estado.pares - indiceElemento,
                indiceElementoSeleccionado = null,
            )
            return
        }
        _estadoLeccion.value = estado.copy(indiceElementoSeleccionado = indiceElemento)
    }

    fun seleccionarOpcionAsociar(indiceOpcion: Int) {
        val estado = enCursoActual() ?: return
        if (estado.verificada) return

        val parExistente = estado.pares.entries.find { it.value == indiceOpcion }
        if (parExistente != null) {
            _estadoLeccion.value = estado.copy(
                pares = estado.pares - parExistente.key,
                indiceElementoSeleccionado = null,
            )
            return
        }

        val indiceElemento = estado.indiceElementoSeleccionado ?: return
        _estadoLeccion.value = estado.copy(
            pares = estado.pares + (indiceElemento to indiceOpcion),
            indiceElementoSeleccionado = null,
        )
    }

    fun comprobar() {
        val estado = enCursoActual() ?: return
        if (estado.verificada || estado.evaluandoPronunciacion) return
        val actividad = estado.actividadActual

        if (actividad.tipo == TipoActividad.PRONUNCIACION) {
            evaluarPronunciacion(actividad)
            return
        }

        val esCorrecta = when (actividad.tipo) {
            TipoActividad.SELECCION -> estado.respuestaSeleccionada == actividad.respuestaCorrecta
            TipoActividad.COMPLETAR -> estado.textoIngresado.trim()
                .equals(actividad.respuestaCorrecta.trim(), ignoreCase = true)
            TipoActividad.ORDENAR -> estado.ordenActual == actividad.respuestasCorrectas
            TipoActividad.ASOCIAR -> actividad.elementos.indices.map { i ->
                estado.pares[i]?.let { actividad.opciones.getOrNull(it) }
            } == actividad.respuestasCorrectas
            TipoActividad.ESCUCHA -> estado.respuestaSeleccionada == actividad.respuestaCorrecta
            TipoActividad.VOCABULARIO, TipoActividad.PRONUNCIACION -> return
        }

        _estadoLeccion.value = estado.copy(
            verificada = true,
            esCorrecta = esCorrecta,
            correctas = if (esCorrecta) estado.correctas + 1 else estado.correctas,
        )

        respuestasComprobadas[actividad.id] = esCorrecta

        registrarRespuestaSiCorresponde(actividad, esCorrecta)
    }

    /**
     * Califica PRONUNCIACION por similitud acústica (MFCC + DTW, ver
     * [com.proyect.qchuago.ui.viewmodel] `SimilitudAudio.kt`) contra el audio de
     * referencia de la actividad — no es reconocimiento de voz, solo "qué tan
     * parecido suena" la grabación del usuario. Corre fuera del hilo principal
     * porque decodificar y comparar audio no es instantáneo.
     */
    private fun evaluarPronunciacion(actividad: Actividad) {
        val estadoInicial = enCursoActual() ?: return
        _estadoLeccion.value = estadoInicial.copy(evaluandoPronunciacion = true, errorEvaluacion = null)

        viewModelScope.launch(Dispatchers.Default) {
            val pcmUsuario = decodificarArchivoAPcm(archivoGrabacion.absolutePath)
            val pcmReferencia = decodificarReferenciaAPcm(actividad)

            if (pcmUsuario == null || pcmReferencia == null) {
                val estadoActual = enCursoActual() ?: return@launch
                _estadoLeccion.value = estadoActual.copy(
                    evaluandoPronunciacion = false,
                    errorEvaluacion = "No se pudo evaluar la grabación. Inténtalo de nuevo.",
                )
                return@launch
            }

            val similitud = calcularSimilitud(
                pcmUsuario = pcmUsuario.muestras,
                tasaUsuario = pcmUsuario.tasaMuestreo,
                pcmReferencia = pcmReferencia.muestras,
                tasaReferencia = pcmReferencia.tasaMuestreo,
            )
            val esCorrecta = similitud >= UMBRAL_SIMILITUD_PRONUNCIACION

            val estadoFinal = enCursoActual() ?: return@launch
            _estadoLeccion.value = estadoFinal.copy(
                verificada = true,
                esCorrecta = esCorrecta,
                evaluandoPronunciacion = false,
                similitudPronunciacion = similitud.roundToInt(),
                correctas = if (esCorrecta) estadoFinal.correctas + 1 else estadoFinal.correctas,
            )
            respuestasComprobadas[actividad.id] = esCorrecta
            registrarRespuestaSiCorresponde(actividad, esCorrecta)
        }
    }

    /**
     * Permite regrabar y volver a calificar la misma actividad de PRONUNCIACION
     * sin pasar a la siguiente — limpia el resultado y la grabación anterior,
     * dejando el botón "Grabar mi voz" listo para un nuevo intento.
     */
    fun reintentarPronunciacion() {
        val estado = enCursoActual() ?: return
        if (estado.actividadActual.tipo != TipoActividad.PRONUNCIACION) return
        _estadoLeccion.value = estado.copy(
            verificada = false,
            esCorrecta = null,
            grabacionDisponible = false,
            evaluandoPronunciacion = false,
            similitudPronunciacion = null,
            errorEvaluacion = null,
        )
    }

    /**
     * Registra si una respuesta de actividad evaluable fue correcta o incorrecta,
     * bajo usuarios/{uid}/respuestas. Un documento por actividad (se sobrescribe
     * si el usuario repite el ejercicio).
     *
     * No usamos `await()`: no nos importa esperar el resultado. Firestore guarda la
     * escritura en cola y la envía sola (incluso si no hay internet en ese momento).
     */
    private fun registrarRespuestaSiCorresponde(actividad: Actividad, esCorrecta: Boolean) {
        if (actividadesEstaticas != null) return
        val uidActual = uid ?: return
        val datos = mapOf(
            "unidadId" to unidadId,
            "leccionId" to leccionId,
            "actividadId" to actividad.id,
            "esCorrecta" to esCorrecta,
            "fecha" to Timestamp.now(),
        )
        usuariosCollection.document(uidActual)
            .collection("respuestas").document("${unidadId}_${leccionId}_${actividad.id}")
            .set(datos, SetOptions.merge())
    }

    fun continuar() {
        val estado = enCursoActual() ?: return
        if (estado.esUltima) {
            finalizarLeccion(estado.correctas, estado.totalEvaluables)
            return
        }
        val siguienteIndice = estado.indice + 1
        _estadoLeccion.value = estado.copy(
            indice = siguienteIndice,
            respuestaSeleccionada = null,
            textoIngresado = "",
            elementosDisponibles = estado.actividades[siguienteIndice].elementos,
            ordenActual = emptyList(),
            indiceElementoSeleccionado = null,
            pares = emptyMap(),
            verificada = false,
            esCorrecta = null,
            grabacionDisponible = false,
            evaluandoPronunciacion = false,
            similitudPronunciacion = null,
            errorEvaluacion = null,
        )
    }

    fun reintentarGuardado() {
        val estado = _estadoLeccion.value as? EstadoLeccion.Finalizada ?: return
        finalizarLeccion(estado.correctas, estado.totalEvaluables)
    }

    private fun finalizarLeccion(correctas: Int, totalEvaluables: Int) {
        if (actividadesEstaticas != null) {
            _estadoLeccion.value = EstadoLeccion.Finalizada(correctas, totalEvaluables)
            return
        }

        val uidActual = uid
        if (uidActual == null) {
            mostrarErrorGuardado(correctas, totalEvaluables)
            return
        }

        _estadoLeccion.value = EstadoLeccion.Finalizada(
            correctas = correctas,
            totalEvaluables = totalEvaluables,
            guardando = true,
        )

        viewModelScope.launch {
            try {
                val xpGanados = guardarResultado(uidActual)
                _estadoLeccion.value = EstadoLeccion.Finalizada(
                    correctas = correctas,
                    totalEvaluables = totalEvaluables,
                    xpGanados = xpGanados,
                )
            } catch (e: Exception) {
                android.util.Log.e("LeccionViewModel", "Error al guardar resultado de leccion: ${e.message}", e)
                mostrarErrorGuardado(correctas, totalEvaluables)
            }
        }
    }

    /** Guarda XP, progreso y racha juntos al completar la lección. */
    private suspend fun guardarResultado(uidActual: String): Int {
        val usuarioRef = usuariosCollection.document(uidActual)
        val respuestas = respuestasComprobadas.toMap()
        val claveLeccion = "$unidadId/$leccionId"
        val hoy = LocalDate.now(ZONA_HORARIA_LIMA)

        return firestore.runTransaction { transaccion ->
            val semanaId = RankingViewModel.obtenerSemanaActual()
            val rankingRef = firestore.collection("rankingsSemanales")
                .document(semanaId)
                .collection("usuarios")
                .document(uidActual)

            // --- 1. LECTURAS PRIMERO (Regla obligatoria de las transacciones de Firestore) ---
            val usuario = transaccion.get(usuarioRef)
            val docRankingSnap = transaccion.get(rankingRef)
            val respuestasGuardadas = respuestas.map { (actividadId, esCorrecta) ->
                val referencia = usuarioRef.collection("respuestas")
                    .document("${unidadId}_${leccionId}_$actividadId")
                Triple(actividadId, esCorrecta, referencia to transaccion.get(referencia))
            }

            // --- 2. CÁLCULO DE RECOMPENSAS Y PROGRESO EN MEMORIA ---
            val idsCompletados = (usuario.get("leccionesCompletadasIds") as? List<*>)
                ?.filterIsInstance<String>().orEmpty()

            var xpGanados = respuestasGuardadas.count { (_, esCorrecta, datos) ->
                esCorrecta && datos.second.getBoolean("xpOtorgado") != true
            } * XP_RESPUESTA_CORRECTA

            val esPrimeraVez = claveLeccion !in idsCompletados
            val nuevosIds = if (esPrimeraVez) idsCompletados + claveLeccion else idsCompletados
            if (esPrimeraVez) xpGanados += XP_LECCION_COMPLETADA

            val nuevaRacha = calcularRacha(
                rachaActual = (usuario.getLong("racha") ?: 0L).toInt(),
                ultimaFecha = usuario.getString("ultimaFechaPractica").orEmpty(),
                hoy = hoy,
            )

            // --- 3. ESCRITURAS AL FINAL ---
            transaccion.update(
                usuarioRef,
                mapOf(
                    "xp" to ((usuario.getLong("xp") ?: 0L) + xpGanados),
                    "racha" to nuevaRacha,
                    "ultimaFechaPractica" to hoy.toString(),
                    "leccionesCompletadasIds" to nuevosIds,
                    "leccionesCompletadas" to nuevosIds.size,
                ),
            )

            respuestasGuardadas.forEach { (actividadId, esCorrecta, datos) ->
                val respuesta = mutableMapOf<String, Any>(
                    "unidadId" to unidadId,
                    "leccionId" to leccionId,
                    "actividadId" to actividadId,
                    "esCorrecta" to esCorrecta,
                    "fecha" to Timestamp.now(),
                )
                if (esCorrecta && datos.second.getBoolean("xpOtorgado") != true) {
                    respuesta["xpOtorgado"] = true
                }
                transaccion.set(datos.first, respuesta, SetOptions.merge())
            }

            if (xpGanados > 0) {
                val xpSemanalPrevio = (docRankingSnap.getLong("xpSemanal") ?: 0L).toInt()
                val nombreUsuario = usuario.getString("nombre")?.ifBlank { "Estudiante" } ?: "Estudiante"

                transaccion.set(
                    rankingRef,
                    mapOf(
                        "uid" to uidActual,
                        "nombre" to nombreUsuario,
                        "xpSemanal" to (xpSemanalPrevio + xpGanados),
                    ),
                    SetOptions.merge(),
                )
            }

            xpGanados
        }.await()
    }

    private fun mostrarErrorGuardado(correctas: Int, totalEvaluables: Int) {
        _estadoLeccion.value = EstadoLeccion.Finalizada(
            correctas = correctas,
            totalEvaluables = totalEvaluables,
            error = "No se pudo guardar el resultado. Revisa tu conexión e inténtalo nuevamente.",
        )
    }

    fun reproducirAudio() {
        val estado = enCursoActual() ?: return
        val actividad = estado.actividadActual
        val reproduciendo = reproducir(actividad.audioLocal, actividad.audioUrl) {
            marcarAudioDetenido()
        }
        if (reproduciendo) {
            _estadoLeccion.value = estado.copy(reproduciendoAudio = true)
        }
    }

    fun alternarGrabacion() {
        val estado = enCursoActual() ?: return
        if (estado.grabando) {
            detenerGrabacion()
            _estadoLeccion.value = estado.copy(grabando = false, grabacionDisponible = true)
        } else {
            iniciarGrabacion()
            _estadoLeccion.value = estado.copy(grabando = true)
        }
    }

    fun reproducirMiGrabacion() {
        val estado = enCursoActual() ?: return
        val reproduciendo = reproducirGrabacion { marcarAudioDetenido() }
        if (reproduciendo) {
            _estadoLeccion.value = estado.copy(reproduciendoAudio = true)
        }
    }

    private fun marcarAudioDetenido() {
        val estado = enCursoActual() ?: return
        _estadoLeccion.value = estado.copy(reproduciendoAudio = false)
    }

    private fun enCursoActual(): EstadoLeccion.EnCurso? = _estadoLeccion.value as? EstadoLeccion.EnCurso

    /** Reproduce [audioLocal] (recurso en res/raw) o, si no existe, [audioUrl]. */
    private fun reproducir(audioLocal: String, audioUrl: String, alTerminar: () -> Unit): Boolean {
        val idRecurso = if (audioLocal.isNotBlank()) {
            context.resources.getIdentifier(audioLocal, "raw", context.packageName)
        } else 0

        liberarReproductor()

        return try {
            val reproductor = when {
                idRecurso != 0 -> MediaPlayer.create(context, idRecurso)
                audioUrl.isNotBlank() -> MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepare()
                }
                else -> null
            } ?: return false

            mediaPlayer = reproductor
            reproductor.setOnCompletionListener {
                liberarReproductor()
                alTerminar()
            }
            reproductor.start()
            true
        } catch (e: Exception) {
            liberarReproductor()
            false
        }
    }

    private fun iniciarGrabacion() {
        liberarReproductor()
        val grabador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mediaRecorder = grabador.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(archivoGrabacion.absolutePath)
            prepare()
            start()
        }
    }

    private fun detenerGrabacion() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Grabación demasiado corta o sin datos: se descarta silenciosamente.
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    private fun hayGrabacionDisponible(): Boolean = archivoGrabacion.exists()

    private data class AudioPcm(val muestras: ShortArray, val tasaMuestreo: Int)

    private fun decodificarArchivoAPcm(rutaArchivo: String): AudioPcm? =
        decodificarAPcm { MediaExtractor().apply { setDataSource(rutaArchivo) } }

    /** Resuelve [Actividad.audioLocal] (recurso `raw`) o [Actividad.audioUrl], igual que [reproducir]. */
    private fun decodificarReferenciaAPcm(actividad: Actividad): AudioPcm? {
        val idRecurso = if (actividad.audioLocal.isNotBlank()) {
            context.resources.getIdentifier(actividad.audioLocal, "raw", context.packageName)
        } else 0
        return when {
            idRecurso != 0 -> decodificarAPcm {
                MediaExtractor().apply { setDataSource(context.resources.openRawResourceFd(idRecurso)) }
            }
            actividad.audioUrl.isNotBlank() -> decodificarAPcm {
                MediaExtractor().apply { setDataSource(actividad.audioUrl, emptyMap()) }
            }
            else -> null
        }
    }

    /**
     * Decodifica la primera pista de audio que entregue [crearExtractor] a PCM de
     * 16 bits (mono, mezclando a mono si viene estéreo), usando [MediaExtractor] +
     * [MediaCodec] (SDK de Android, sin dependencias nuevas). `null` si falla.
     */
    private fun decodificarAPcm(crearExtractor: () -> MediaExtractor): AudioPcm? {
        val extractor = try {
            crearExtractor()
        } catch (e: Exception) {
            return null
        }
        return try {
            var formato: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val candidato = extractor.getTrackFormat(i)
                if (candidato.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    extractor.selectTrack(i)
                    formato = candidato
                    break
                }
            }
            val mime = formato?.getString(MediaFormat.KEY_MIME) ?: return null
            val tasaMuestreo = formato.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val canales = formato.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val decodificador = MediaCodec.createDecoderByType(mime)
            decodificador.configure(formato, null, null, 0)
            decodificador.start()

            val salida = ByteArrayOutputStream()
            val buferInfo = MediaCodec.BufferInfo()
            var entradaTerminada = false
            var salidaTerminada = false

            try {
                while (!salidaTerminada) {
                    if (!entradaTerminada) {
                        val indiceEntrada = decodificador.dequeueInputBuffer(10_000)
                        if (indiceEntrada >= 0) {
                            val buferEntrada = decodificador.getInputBuffer(indiceEntrada)!!
                            val tamanoLeido = extractor.readSampleData(buferEntrada, 0)
                            if (tamanoLeido < 0) {
                                decodificador.queueInputBuffer(
                                    indiceEntrada, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                entradaTerminada = true
                            } else {
                                decodificador.queueInputBuffer(indiceEntrada, 0, tamanoLeido, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val indiceSalida = decodificador.dequeueOutputBuffer(buferInfo, 10_000)
                    if (indiceSalida >= 0) {
                        if (buferInfo.size > 0) {
                            val buferSalida = decodificador.getOutputBuffer(indiceSalida)!!
                            val datos = ByteArray(buferInfo.size)
                            buferSalida.position(buferInfo.offset)
                            buferSalida.get(datos)
                            salida.write(datos)
                        }
                        decodificador.releaseOutputBuffer(indiceSalida, false)
                        if (buferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            salidaTerminada = true
                        }
                    }
                }
            } finally {
                decodificador.stop()
                decodificador.release()
            }

            val bytes = salida.toByteArray()
            val bufferMuestras = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val entrelazadas = ShortArray(bufferMuestras.remaining())
            bufferMuestras.get(entrelazadas)
            val muestras = if (canales == 2) mezclarAMono(entrelazadas) else entrelazadas
            AudioPcm(muestras, tasaMuestreo)
        } catch (e: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun mezclarAMono(entrelazadas: ShortArray): ShortArray =
        ShortArray(entrelazadas.size / 2) { i ->
            ((entrelazadas[2 * i].toInt() + entrelazadas[2 * i + 1].toInt()) / 2).toShort()
        }

    private fun reproducirGrabacion(alTerminar: () -> Unit): Boolean {
        if (!hayGrabacionDisponible()) return false
        liberarReproductor()
        return try {
            val reproductor = MediaPlayer().apply {
                setDataSource(archivoGrabacion.absolutePath)
                prepare()
            }
            mediaPlayer = reproductor
            reproductor.setOnCompletionListener {
                liberarReproductor()
                alTerminar()
            }
            reproductor.start()
            true
        } catch (e: Exception) {
            liberarReproductor()
            false
        }
    }

    private fun liberar() {
        liberarReproductor()
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Sin grabación en curso.
        }
        mediaRecorder?.release()
        mediaRecorder = null
    }

    private fun liberarReproductor() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        liberar()
    }

    fun anterior() {
        val estado = enCursoActual() ?: return
        if (estado.indice > 0) {
            val anteriorIndice = estado.indice - 1
            _estadoLeccion.value = estado.copy(
                indice = anteriorIndice,
                respuestaSeleccionada = null,
                textoIngresado = "",
                elementosDisponibles = estado.actividades[anteriorIndice].elementos,
                ordenActual = emptyList(),
                indiceElementoSeleccionado = null,
                pares = emptyMap(),
                verificada = false,
                esCorrecta = null,
                grabacionDisponible = false,
                evaluandoPronunciacion = false,
                similitudPronunciacion = null,
                errorEvaluacion = null,
            )
        }
    }
}

private const val XP_RESPUESTA_CORRECTA = 5
private const val XP_LECCION_COMPLETADA = 20

/**
 * Similitud mínima (0-100, ver [calcularSimilitud]) para aprobar PRONUNCIACION.
 * Valor de partida sin calibrar con voz real — ajustar al probar en un dispositivo.
 */
private const val UMBRAL_SIMILITUD_PRONUNCIACION = 55f
private val ZONA_HORARIA_LIMA = ZoneId.of("America/Lima")

private fun calcularRacha(rachaActual: Int, ultimaFecha: String, hoy: LocalDate): Int {
    val fechaAnterior = runCatching { LocalDate.parse(ultimaFecha) }.getOrNull()
    return when (fechaAnterior) {
        hoy -> rachaActual
        hoy.minusDays(1) -> rachaActual + 1
        else -> 1
    }
}
