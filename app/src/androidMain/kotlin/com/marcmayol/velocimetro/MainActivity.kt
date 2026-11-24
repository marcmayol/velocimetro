package com.marcmayol.velocimetro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marcm.actualizador.Modo

class MainActivity : ComponentActivity() {
    private val app get() = application as VelocimetroApp

    private var acceso by mutableStateOf(Acceso.PEDIR)
    private var activo by mutableStateOf(false)

    private val pedirPermiso = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        app.almacen.guardar("permiso_pedido", "1")
        actualizarAcceso()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Mirando la velocidad, que no se apague la pantalla.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        app.gps.alCambiarProveedor = { actualizarAcceso() }
        val updates = if (BuildConfig.DEBUG) null else app.actualizador
        setContent {
            App(
                trayecto = app.trayecto,
                almacen = app.almacen,
                fuente = app.gps,
                acceso = acceso,
                onAcceso = ::resolverAcceso,
                activo = activo,
                cabecera = {
                    if (updates != null) {
                        LaunchedEffect(Unit) { updates.comprobar(Modo.AUTOMATICO) }
                        val estado by updates.estado.collectAsState()
                        UpdateBanner(estado, onUpdate = { updates.actualizarAhora() })
                    }
                },
            )
        }
        if (!BuildConfig.DEBUG) app.actualizador.programarPeriodica()
    }

    override fun onStart() {
        super.onStart()
        actualizarAcceso()
        activo = true
    }

    override fun onStop() {
        activo = false
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Vuelve de Ajustes con el permiso o el GPS cambiados.
        actualizarAcceso()
        if (!BuildConfig.DEBUG) app.actualizador.onPermisoQuizaConcedido()
    }

    private fun concedido(permiso: String) = checkSelfPermission(permiso) == PackageManager.PERMISSION_GRANTED

    private fun actualizarAcceso() {
        acceso = when {
            concedido(Manifest.permission.ACCESS_FINE_LOCATION) ->
                if (app.gps.encendido()) Acceso.LISTO else Acceso.GPS_APAGADO
            concedido(Manifest.permission.ACCESS_COARSE_LOCATION) -> Acceso.SOLO_APROXIMADA
            // Ya se pidió y Android no deja volver a preguntar: solo queda ir a Ajustes.
            app.almacen.leer("permiso_pedido") != null &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> Acceso.DENEGADO
            else -> Acceso.PEDIR
        }
    }

    private fun resolverAcceso() {
        when (acceso) {
            Acceso.PEDIR, Acceso.SOLO_APROXIMADA -> pedirPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
            Acceso.DENEGADO -> startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
            )
            Acceso.GPS_APAGADO -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            Acceso.LISTO -> Unit
        }
    }
}
