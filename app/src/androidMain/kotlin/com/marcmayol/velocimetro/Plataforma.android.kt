package com.marcmayol.velocimetro

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat

class AlmacenPrefs(context: Context) : Almacen {
    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
    override fun leer(clave: String): String? = prefs.getString(clave, null)
    override fun guardar(clave: String, valor: String) {
        prefs.edit().putString(clave, valor).apply()
    }
}

/**
 * GPS_PROVIDER directo: solo satélites, sin Google Play Services ni red. Una posición
 * por segundo. [alCambiarProveedor] avisa si apagan o encienden la ubicación con la app abierta.
 */
class GpsAndroid(context: Context) : FuenteGps {
    private val gestor = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var alCambiarProveedor: (() -> Unit)? = null
    private var oyente: LocationListener? = null

    fun encendido(): Boolean = gestor.isProviderEnabled(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission") // Solo se llama con Acceso.LISTO.
    override fun empezar(alRecibir: (Fix) -> Unit) {
        parar()
        // Todos los métodos a mano: en Android 10 y anteriores no tienen implementación por
        // defecto y el sistema los llama igual.
        val nuevo = object : LocationListener {
            override fun onLocationChanged(location: Location) = alRecibir(location.aFix())
            override fun onProviderEnabled(provider: String) { alCambiarProveedor?.invoke() }
            override fun onProviderDisabled(provider: String) { alCambiarProveedor?.invoke() }
            @Deprecated("Solo para Android 10 y anteriores")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        oyente = nuevo
        gestor.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, nuevo, Looper.getMainLooper())
    }

    override fun parar() {
        oyente?.let { gestor.removeUpdates(it) }
        oyente = null
    }
}

private fun Location.aFix() = Fix(
    lat = latitude,
    lon = longitude,
    tiempoMs = elapsedRealtimeNanos / 1_000_000,
    velocidadMs = if (hasSpeed()) speed.toDouble() else null,
    precisionM = if (hasAccuracy()) accuracy.toDouble() else null,
)

actual fun formatoNumero(valor: Double, decimales: Int): String =
    NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = decimales
        maximumFractionDigits = decimales
    }.format(valor)

@Composable
actual fun esquemaColores(oscuro: Boolean): ColorScheme {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val contexto = LocalContext.current
        return if (oscuro) dynamicDarkColorScheme(contexto) else dynamicLightColorScheme(contexto)
    }
    return if (oscuro) esquemaOscuro else esquemaClaro
}
