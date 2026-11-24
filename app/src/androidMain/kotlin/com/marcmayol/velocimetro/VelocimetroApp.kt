package com.marcmayol.velocimetro

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig

class VelocimetroApp : Application() {
    /** Se reparte por DracApps: mismo módulo de actualización que Yenes y Rodet. */
    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = "https://marcmayol.com/velocimetro/updates.json",
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    // Aquí y no en la actividad: al girar el móvil no se pierde el trayecto.
    val trayecto = Trayecto()
    val almacen: Almacen by lazy { AlmacenPrefs(this) }
    val gps: GpsAndroid by lazy { GpsAndroid(this) }
}
