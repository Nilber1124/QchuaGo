package com.proyect.qchuago

import com.proyect.qchuago.ui.viewmodel.calcularSimilitud
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Valida que la matemática de comparación acústica (FFT/MFCC/DTW, en
 * [com.proyect.qchuago.ui.viewmodel.SimilitudAudio]) no esté rota, usando señales
 * sintéticas. No reemplaza probar con voz real en un dispositivo.
 */
class SimilitudAudioTest {

    @Test
    fun `tono comparado contra si mismo da similitud alta`() {
        val tono = generarTono(frecuencia = 440.0, tasa = 8000, duracionSegundos = 0.5)

        val similitud = calcularSimilitud(tono, 8000, tono, 8000)

        assertTrue("similitud esperada alta, fue $similitud", similitud > 90f)
    }

    @Test
    fun `tono comparado contra ruido da similitud baja`() {
        val tono = generarTono(frecuencia = 440.0, tasa = 8000, duracionSegundos = 0.5)
        val ruido = generarRuido(tasa = 8000, duracionSegundos = 0.5)

        val similitud = calcularSimilitud(tono, 8000, ruido, 8000)

        assertTrue("similitud esperada baja, fue $similitud", similitud < 50f)
    }

    @Test
    fun `tono comparado contra silencio da similitud baja`() {
        val tono = generarTono(frecuencia = 440.0, tasa = 8000, duracionSegundos = 0.5)
        val silencio = ShortArray((8000 * 0.5).toInt())

        val similitud = calcularSimilitud(tono, 8000, silencio, 8000)

        assertTrue("similitud esperada baja, fue $similitud", similitud < 50f)
    }

    @Test
    fun `mismo tono a otra tasa de muestreo sigue dando similitud razonable`() {
        val tonoBajo = generarTono(frecuencia = 440.0, tasa = 8000, duracionSegundos = 0.5)
        val tonoAlto = generarTono(frecuencia = 440.0, tasa = 16000, duracionSegundos = 0.5)
        val ruido = generarRuido(tasa = 8000, duracionSegundos = 0.5)

        val similitudTono = calcularSimilitud(tonoBajo, 8000, tonoAlto, 16000)
        val similitudRuido = calcularSimilitud(tonoBajo, 8000, ruido, 8000)

        assertTrue(
            "el remuestreo debería dejar el mismo tono más parecido que el ruido " +
                "(tono=$similitudTono, ruido=$similitudRuido)",
            similitudTono > similitudRuido,
        )
    }

    private fun generarTono(frecuencia: Double, tasa: Int, duracionSegundos: Double): ShortArray {
        val totalMuestras = (tasa * duracionSegundos).toInt()
        return ShortArray(totalMuestras) { i ->
            (sin(2.0 * PI * frecuencia * i / tasa) * Short.MAX_VALUE * 0.8).toInt().toShort()
        }
    }

    private fun generarRuido(tasa: Int, duracionSegundos: Double): ShortArray {
        val totalMuestras = (tasa * duracionSegundos).toInt()
        val random = Random(42)
        return ShortArray(totalMuestras) {
            random.nextInt(-Short.MAX_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
