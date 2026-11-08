package com.proyect.qchuago.ui.viewmodel

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Comparación acústica entre dos audios (MFCC + DTW) para calificar pronunciación
 * por similitud, sin reconocer el contenido (no es ASR). Funciones puras, sin
 * dependencias de Android, para poder probarse con JUnit normal.
 */

private const val TASA_OBJETIVO = 8000
private const val TAMANO_FRAME = 256
private const val SALTO_FRAME = 128
private const val NUM_FILTROS_MEL = 20
private const val NUM_COEF_MFCC = 13

/**
 * Escala que convierte la distancia DTW (en unidades de MFCC, sin un rango fijo
 * conocido de antemano) a un puntaje 0-100. Valor de partida sin calibrar contra
 * voz real: ajustar junto con el umbral de aprobación al probar en un dispositivo.
 */
private const val ESCALA_DISTANCIA = 4f

/** Puntaje de similitud 0-100 (100 = prácticamente idéntico) entre dos grabaciones. */
fun calcularSimilitud(
    pcmUsuario: ShortArray,
    tasaUsuario: Int,
    pcmReferencia: ShortArray,
    tasaReferencia: Int,
): Float {
    val usuario = remuestrear(pcmUsuario, tasaUsuario, TASA_OBJETIVO)
    val referencia = remuestrear(pcmReferencia, tasaReferencia, TASA_OBJETIVO)
    if (usuario.size < TAMANO_FRAME || referencia.size < TAMANO_FRAME) return 0f

    val mfccUsuario = extraerMfcc(usuario, TASA_OBJETIVO)
    val mfccReferencia = extraerMfcc(referencia, TASA_OBJETIVO)
    if (mfccUsuario.isEmpty() || mfccReferencia.isEmpty()) return 0f

    val distancia = distanciaDtw(mfccUsuario, mfccReferencia)
    return (100f - distancia * ESCALA_DISTANCIA).coerceIn(0f, 100f)
}

/** Remuestreo simple por interpolación lineal (suficiente para comparar, no para audio final). */
fun remuestrear(pcm: ShortArray, tasaOrigen: Int, tasaDestino: Int): ShortArray {
    if (pcm.isEmpty() || tasaOrigen == tasaDestino) return pcm
    val factor = tasaDestino.toDouble() / tasaOrigen
    val nuevoLargo = (pcm.size * factor).toInt().coerceAtLeast(1)
    return ShortArray(nuevoLargo) { i ->
        val posOrigen = i / factor
        val base = posOrigen.toInt().coerceIn(0, pcm.size - 1)
        val siguiente = (base + 1).coerceIn(0, pcm.size - 1)
        val frac = posOrigen - base
        (pcm[base] * (1 - frac) + pcm[siguiente] * frac).toInt().toShort()
    }
}

/** Secuencia de vectores MFCC (uno por frame de [TAMANO_FRAME] muestras, salto [SALTO_FRAME]). */
fun extraerMfcc(pcm: ShortArray, tasaMuestreo: Int): Array<FloatArray> {
    if (pcm.size < TAMANO_FRAME) return emptyArray()

    val ventana = FloatArray(TAMANO_FRAME) { i ->
        (0.54 - 0.46 * cos(2.0 * PI * i / (TAMANO_FRAME - 1))).toFloat()
    }
    val bancoMel = construirBancoMel(tasaMuestreo, TAMANO_FRAME, NUM_FILTROS_MEL)
    val numBins = TAMANO_FRAME / 2 + 1

    val frames = mutableListOf<FloatArray>()
    var inicio = 0
    while (inicio + TAMANO_FRAME <= pcm.size) {
        val real = FloatArray(TAMANO_FRAME)
        val imag = FloatArray(TAMANO_FRAME)
        for (i in 0 until TAMANO_FRAME) {
            real[i] = (pcm[inicio + i] / 32768f) * ventana[i]
        }
        fft(real, imag)

        val potencia = FloatArray(numBins) { k -> real[k] * real[k] + imag[k] * imag[k] }
        val energiaMel = FloatArray(NUM_FILTROS_MEL) { m ->
            var suma = 0f
            val pesos = bancoMel[m]
            for (k in pesos.indices) suma += pesos[k] * potencia[k]
            ln((suma + 1e-10f).toDouble()).toFloat()
        }

        val mfcc = FloatArray(NUM_COEF_MFCC) { k ->
            var suma = 0f
            for (m in 0 until NUM_FILTROS_MEL) {
                suma += energiaMel[m] * cos((PI / NUM_FILTROS_MEL) * (m + 0.5) * k).toFloat()
            }
            suma
        }
        frames.add(mfcc)
        inicio += SALTO_FRAME
    }
    return frames.toTypedArray()
}

/** Distancia DTW entre dos secuencias de vectores, normalizada por el largo del camino. */
fun distanciaDtw(a: Array<FloatArray>, b: Array<FloatArray>): Float {
    if (a.isEmpty() || b.isEmpty()) return Float.MAX_VALUE
    val n = a.size
    val m = b.size
    val costo = Array(n + 1) { FloatArray(m + 1) { Float.MAX_VALUE / 2 } }
    costo[0][0] = 0f
    for (i in 1..n) {
        for (j in 1..m) {
            val d = distanciaEuclidiana(a[i - 1], b[j - 1])
            costo[i][j] = d + minOf(costo[i - 1][j], costo[i][j - 1], costo[i - 1][j - 1])
        }
    }
    return costo[n][m] / (n + m)
}

private fun distanciaEuclidiana(x: FloatArray, y: FloatArray): Float {
    var suma = 0f
    for (k in x.indices) {
        val diferencia = x[k] - y[k]
        suma += diferencia * diferencia
    }
    return sqrt(suma)
}

private fun construirBancoMel(tasaMuestreo: Int, tamanoFrame: Int, numFiltros: Int): Array<FloatArray> {
    val numBins = tamanoFrame / 2 + 1
    val melMax = hzAMel(tasaMuestreo / 2f)
    val puntosMel = FloatArray(numFiltros + 2) { i -> i * (melMax / (numFiltros + 1)) }
    val bins = puntosMel.map { mel ->
        val hz = melAHz(mel)
        ((tamanoFrame + 1) * hz / tasaMuestreo).toInt().coerceIn(0, numBins - 1)
    }
    return Array(numFiltros) { m ->
        val filtro = FloatArray(numBins)
        val inicio = bins[m]
        val centro = bins[m + 1]
        val fin = bins[m + 2]
        if (centro > inicio) {
            for (k in inicio until centro) filtro[k] = (k - inicio).toFloat() / (centro - inicio)
        }
        if (fin > centro) {
            for (k in centro until fin) filtro[k] = (fin - k).toFloat() / (fin - centro)
        }
        filtro
    }
}

private fun hzAMel(hz: Float): Float = (2595.0 * log10(1.0 + hz / 700.0)).toFloat()

private fun melAHz(mel: Float): Float = (700.0 * (10.0.pow(mel / 2595.0) - 1.0)).toFloat()

/** FFT iterativa radix-2 (Cooley-Tukey), in-place. [real]/[imag] deben tener largo potencia de 2. */
private fun fft(real: FloatArray, imag: FloatArray) {
    val n = real.size
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j or bit
        if (i < j) {
            val tr = real[i]; real[i] = real[j]; real[j] = tr
            val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
        }
    }

    var largo = 2
    while (largo <= n) {
        val angulo = -2.0 * PI / largo
        val wr = cos(angulo).toFloat()
        val wi = sin(angulo).toFloat()
        var i = 0
        while (i < n) {
            var curWr = 1f
            var curWi = 0f
            for (k in 0 until largo / 2) {
                val uR = real[i + k]
                val uI = imag[i + k]
                val vIndice = i + k + largo / 2
                val vR = real[vIndice] * curWr - imag[vIndice] * curWi
                val vI = real[vIndice] * curWi + imag[vIndice] * curWr
                real[i + k] = uR + vR
                imag[i + k] = uI + vI
                real[vIndice] = uR - vR
                imag[vIndice] = uI - vI
                val nextWr = curWr * wr - curWi * wi
                val nextWi = curWr * wi + curWi * wr
                curWr = nextWr
                curWi = nextWi
            }
            i += largo
        }
        largo = largo shl 1
    }
}
