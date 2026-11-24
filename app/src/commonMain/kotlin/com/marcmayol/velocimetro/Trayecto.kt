package com.marcmayol.velocimetro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Una posición del GPS. `tiempoMs` sirve para restar entre posiciones, no es una hora. */
data class Fix(
    val lat: Double,
    val lon: Double,
    val tiempoMs: Long,
    /** Velocidad que da el propio GPS (efecto Doppler), en m/s. Null si no la da. */
    val velocidadMs: Double? = null,
    /** Radio de error en metros. Null si no lo da. */
    val precisionM: Double? = null,
)

data class Resumen(
    /** Velocidad actual en m/s; null mientras no hay ninguna posición. */
    val velocidadMs: Double? = null,
    val maximaMs: Double = 0.0,
    val distanciaM: Double = 0.0,
    val enMarchaMs: Long = 0,
    val precisionM: Double? = null,
) {
    /** Media de lo recorrido en marcha, sin contar las paradas. */
    val mediaMs: Double? get() = if (enMarchaMs >= 1_000) distanciaM / (enMarchaMs / 1000.0) else null
}

/**
 * Suma las posiciones del GPS: velocidad actual, máxima, distancia y tiempo en marcha.
 *
 * Parado, el GPS baila unos metros: por eso solo se suma distancia cuando se va a más
 * de [EN_MARCHA] y la posición es buena ([PRECISION_MAX]).
 */
class Trayecto {
    var resumen by mutableStateOf(Resumen())
        private set

    /** Se ha pulsado Empezar. Vive aquí para que no se pierda al girar la pantalla. */
    var midiendo by mutableStateOf(false)

    private var anterior: Fix? = null

    fun anadir(fix: Fix) {
        val prev = anterior
        val dt = prev?.let { (fix.tiempoMs - it.tiempoMs) / 1000.0 }
        if (dt != null && dt <= 0.0) return
        val tramo = prev?.let { distanciaM(it, fix) }

        val medida = fix.velocidadMs?.takeIf { it >= 0.0 && !it.isNaN() }
            ?: if (dt != null && tramo != null) tramo / dt else null
        val velocidad = medida?.let { if (it < PARADO) 0.0 else it }
        val fiable = (fix.precisionM ?: 0.0) <= PRECISION_MAX

        var r = resumen.copy(velocidadMs = velocidad, precisionM = fix.precisionM)
        if (fiable && velocidad != null) {
            if (velocidad > r.maximaMs && velocidad < CREIBLE_MAX) r = r.copy(maximaMs = velocidad)
            // Si la posición no cambia (algunos GPS repiten la última), no se suma tiempo
            // sin distancia: hundiría la media.
            if (tramo != null && tramo > 0.0 && dt != null && dt <= HUECO_MAX_S && velocidad >= EN_MARCHA) {
                r = r.copy(distanciaM = r.distanciaM + tramo, enMarchaMs = r.enMarchaMs + (dt * 1000).toLong())
            }
        }
        if (fiable) anterior = fix
        resumen = r
    }

    /** Empieza de cero, pero sigue enseñando la velocidad de ahora. */
    fun reiniciar() {
        resumen = Resumen(velocidadMs = resumen.velocidadMs, precisionM = resumen.precisionM)
        anterior = null
    }

    companion object {
        /** Por debajo, 0: es el baile del GPS estando quieto (≈1 km/h). */
        const val PARADO = 0.3
        /** A partir de aquí se cuenta como en marcha (≈3 km/h). */
        const val EN_MARCHA = 0.8
        /** Posiciones con más error que esto no cuentan para máxima ni distancia. */
        const val PRECISION_MAX = 30.0
        /** Tras un túnel o con la app en segundo plano, se suma el tramo si no pasó más de esto. */
        const val HUECO_MAX_S = 120.0
        /** 500 km/h: un salto mayor es un error del GPS, no se guarda como máxima. */
        const val CREIBLE_MAX = 139.0
    }
}

/** Distancia en metros por la superficie de la Tierra (haversine). */
fun distanciaM(a: Fix, b: Fix): Double {
    val r = 6_371_000.0
    val dLat = rad(b.lat - a.lat)
    val dLon = rad(b.lon - a.lon)
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(rad(a.lat)) * cos(rad(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

private fun rad(g: Double) = g * kotlin.math.PI / 180.0
