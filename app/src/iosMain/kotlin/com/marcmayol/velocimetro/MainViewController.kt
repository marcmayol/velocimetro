package com.marcmayol.velocimetro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIViewController

private val trayecto = Trayecto()
private val almacen by lazy { AlmacenUserDefaults() }
private val gps by lazy { GpsIos() }

/** En segundo plano no se escucha al GPS, igual que en Android. */
private var activo by mutableStateOf(true)

/** Lo que pinta la app de iOS (iosApp/iosApp/ContentView.swift). */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    // Mirando la velocidad, que no se apague la pantalla.
    UIApplication.sharedApplication.idleTimerDisabled = true
    val centro = NSNotificationCenter.defaultCenter
    centro.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, NSOperationQueue.mainQueue) { _ ->
        activo = false
    }
    centro.addObserverForName(UIApplicationWillEnterForegroundNotification, null, NSOperationQueue.mainQueue) { _ ->
        activo = true
    }
    return ComposeUIViewController {
        App(
            trayecto = trayecto,
            almacen = almacen,
            fuente = gps,
            acceso = gps.acceso,
            onAcceso = gps::resolverAcceso,
            activo = activo,
        )
    }
}
