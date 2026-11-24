@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.marcmayol.velocimetro

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

class AlmacenUserDefaults : Almacen {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun leer(clave: String): String? = defaults.stringForKey(clave)
    override fun guardar(clave: String, valor: String) {
        defaults.setObject(valor, forKey = clave)
    }
}

/** CoreLocation con la máxima precisión. El permiso se pide al pulsar Empezar. */
class GpsIos : FuenteGps {
    private val gestor = CLLocationManager()
    private var alRecibir: ((Fix) -> Unit)? = null

    var acceso by mutableStateOf(Acceso.PEDIR)
        private set

    // El gestor solo guarda una referencia débil al delegado: hay que retenerlo aquí.
    private val delegado = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            didUpdateLocations.forEach { (it as? CLLocation)?.let { l -> alRecibir?.invoke(l.aFix()) } }
        }

        override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
            actualizar()
        }
    }

    init {
        gestor.delegate = delegado
        gestor.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        gestor.distanceFilter = kCLDistanceFilterNone
        gestor.pausesLocationUpdatesAutomatically = false
        actualizar()
    }

    private fun actualizar() {
        acceso = when (gestor.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> Acceso.PEDIR
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways ->
                if (gestor.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy) Acceso.SOLO_APROXIMADA
                else Acceso.LISTO
            else -> Acceso.DENEGADO
        }
    }

    fun resolverAcceso() {
        if (acceso == Acceso.PEDIR) {
            gestor.requestWhenInUseAuthorization()
        } else {
            val ajustes = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
            UIApplication.sharedApplication.openURL(ajustes, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
    }

    override fun empezar(alRecibir: (Fix) -> Unit) {
        this.alRecibir = alRecibir
        gestor.startUpdatingLocation()
    }

    override fun parar() {
        gestor.stopUpdatingLocation()
        alRecibir = null
    }
}

private fun CLLocation.aFix(): Fix {
    val (lat, lon) = coordinate.useContents { latitude to longitude }
    return Fix(
        lat = lat,
        lon = lon,
        tiempoMs = (timestamp.timeIntervalSince1970 * 1000).toLong(),
        velocidadMs = speed.takeIf { it >= 0 },
        precisionM = horizontalAccuracy.takeIf { it >= 0 },
    )
}

actual fun formatoNumero(valor: Double, decimales: Int): String {
    val formato = NSNumberFormatter()
    formato.numberStyle = NSNumberFormatterDecimalStyle
    formato.locale = NSLocale.currentLocale
    formato.minimumFractionDigits = decimales.toULong()
    formato.maximumFractionDigits = decimales.toULong()
    return formato.stringFromNumber(NSNumber(double = valor)) ?: valor.toString()
}

@Composable
actual fun esquemaColores(oscuro: Boolean): ColorScheme = if (oscuro) esquemaOscuro else esquemaClaro
