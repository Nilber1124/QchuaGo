package com.proyect.qchuago.data.servicio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Encapsula la reproducción (MediaPlayer) y grabación (MediaRecorder) de audio
 * para las actividades de tipo VOCABULARIO/PRONUNCIACION. Es práctica local y
 * no evaluable: no sube nada a Firebase ni valida la pronunciación.
 */
class AudioService(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private val archivoGrabacion = File(context.cacheDir, "practica_pronunciacion.3gp")

    /** Reproduce [audioLocal] (recurso en res/raw) o, si no existe, [audioUrl]. */
    fun reproducir(audioLocal: String, audioUrl: String, alTerminar: () -> Unit): Boolean {
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

    fun iniciarGrabacion() {
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

    fun detenerGrabacion() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Grabación demasiado corta o sin datos: se descarta silenciosamente.
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    fun hayGrabacionDisponible(): Boolean = archivoGrabacion.exists()

    fun reproducirGrabacion(alTerminar: () -> Unit): Boolean {
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

    fun liberar() {
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
}
