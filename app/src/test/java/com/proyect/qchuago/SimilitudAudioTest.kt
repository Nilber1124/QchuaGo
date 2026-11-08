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
    fun `mismo tono grabado bajito sigue dando similitud alta`() {
        // Reproduce el bug reportado: la referencia suena normal pero la
        // grabación del usuario quedó mucho más baja (mic/ganancia distinta).
        // Sin normalizar volumen antes de comparar, esto daba ~0%.
        val referencia = generarTono(frecuencia = 440.0, tasa = 8000, duracionSegundos = 0.5)
        val grabacionBajita = ShortArray(referencia.size) { i -> (referencia[i] / 15).toShort() }

        val similitud = calcularSimilitud(grabacionBajita, 8000, referencia, 8000)

        assertTrue("similitud esperada alta pese al volumen bajo, fue $similitud", similitud > 85f)
    }

    @Test
    fun `dos grabaciones distintas del mismo sonido complejo dan similitud alta, no 0`() {
        // Un tono puro comparado contra sí mismo es un caso casi perfecto (la
        // distancia DTW es ~0 sin importar la escala usada). Una señal con
        // varios armónicos + algo de ruido, comparada contra una segunda
        // "grabación" con pequeñas variaciones, se parece más a dos tomas
        // reales del mismo sonido y es lo que de verdad hacía falta probar:
        // que la escala de distancia no colapse todo a ~0% con algo más
        // realista que un tono idéntico.
        val original = generarSonidoComplejo(semilla = 1)
        val segundaToma = generarSonidoComplejo(semilla = 2)

        val similitud = calcularSimilitud(segundaToma, 8000, original, 8000)

        assertTrue("similitud esperada razonablemente alta, fue $similitud", similitud > 60f)
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

    /** Varios armónicos + algo de ruido: más parecido a una grabación de voz real que un tono puro. */
    private fun generarSonidoComplejo(semilla: Int, tasa: Int = 8000, duracionSegundos: Double = 0.6): ShortArray {
        val totalMuestras = (tasa * duracionSegundos).toInt()
        val random = Random(semilla)
        return ShortArray(totalMuestras) { i ->
            val t = i.toDouble() / tasa
            val senal = sin(2.0 * PI * 180.0 * t) * 0.5 +
                sin(2.0 * PI * 540.0 * t) * 0.3 +
                sin(2.0 * PI * 1200.0 * t) * 0.15
            val ruido = (random.nextDouble() - 0.5) * 0.1
            ((senal + ruido) * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
