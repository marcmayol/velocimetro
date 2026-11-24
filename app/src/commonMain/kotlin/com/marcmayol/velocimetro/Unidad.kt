package com.marcmayol.velocimetro

/** Las cuatro unidades de la web: km/h, mph, m/s y nudos. */
enum class Unidad(
    val clave: String,
    val etiqueta: String,
    /** Multiplica m/s para pasar a esta unidad. */
    val factor: Double,
    /** Final de la esfera analógica y separación de las marcas con número. */
    val escala: Int,
    val paso: Int,
    val etiquetaDistancia: String,
    val metrosPorDistancia: Double,
) {
    KMH("kmh", "km/h", 3.6, 240, 20, "km", 1000.0),
    MPH("mph", "mph", 2.2369362920544, 160, 20, "mi", 1609.344),
    MS("ms", "m/s", 1.0, 70, 10, "km", 1000.0),
    NUDOS("kn", "kn", 1.9438444924406, 140, 20, "nmi", 1852.0);

    fun velocidad(ms: Double): Double = ms * factor
    fun distancia(m: Double): Double = m / metrosPorDistancia

    companion object {
        fun desde(clave: String?): Unidad = entries.firstOrNull { it.clave == clave } ?: KMH
    }
}

/** 3725 s → "1:02:05"; menos de una hora → "2:05". */
fun duracion(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val seg = (s % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$seg" else "$m:$seg"
}
