package com.marcmayol.velocimetro

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** Preferencias pequeñas: SharedPreferences en Android, NSUserDefaults en iOS. */
interface Almacen {
    fun leer(clave: String): String?
    fun guardar(clave: String, valor: String)
}

/** El GPS del móvil, sin red: LocationManager en Android, CLLocationManager en iOS. */
interface FuenteGps {
    fun empezar(alRecibir: (Fix) -> Unit)
    fun parar()
}

/** Lo que falta para poder usar el GPS. */
enum class Acceso { LISTO, PEDIR, DENEGADO, SOLO_APROXIMADA, GPS_APAGADO }

/** Número con los separadores del idioma del móvil y exactamente estos decimales. */
expect fun formatoNumero(valor: Double, decimales: Int): String

@Composable
expect fun esquemaColores(oscuro: Boolean): ColorScheme
