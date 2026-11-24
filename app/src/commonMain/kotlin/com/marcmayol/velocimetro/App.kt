package com.marcmayol.velocimetro

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcmayol.velocimetro.res.Res
import com.marcmayol.velocimetro.res.accuracy
import com.marcmayol.velocimetro.res.analog
import com.marcmayol.velocimetro.res.approximate
import com.marcmayol.velocimetro.res.average
import com.marcmayol.velocimetro.res.denied
import com.marcmayol.velocimetro.res.digital
import com.marcmayol.velocimetro.res.distance
import com.marcmayol.velocimetro.res.gps_off
import com.marcmayol.velocimetro.res.grant
import com.marcmayol.velocimetro.res.local_note
import com.marcmayol.velocimetro.res.max
import com.marcmayol.velocimetro.res.moving
import com.marcmayol.velocimetro.res.need_permission
import com.marcmayol.velocimetro.res.open_settings
import com.marcmayol.velocimetro.res.paused
import com.marcmayol.velocimetro.res.reset
import com.marcmayol.velocimetro.res.reset_confirm
import com.marcmayol.velocimetro.res.searching
import com.marcmayol.velocimetro.res.start
import com.marcmayol.velocimetro.res.stop
import com.marcmayol.velocimetro.res.turn_on
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Sin posiciones nuevas durante este tiempo, se da la señal por perdida. */
private const val SIN_SENAL_MS = 5_000L

/**
 * Toda la app. La plataforma pone el GPS, el permiso y si la app está a la vista
 * ([activo]): con la app en segundo plano no se escucha al GPS.
 */
@Composable
fun App(
    trayecto: Trayecto,
    almacen: Almacen,
    fuente: FuenteGps,
    acceso: Acceso,
    onAcceso: () -> Unit,
    activo: Boolean = true,
    cabecera: @Composable () -> Unit = {},
) {
    MaterialTheme(colorScheme = esquemaColores(isSystemInDarkTheme())) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Pantalla(trayecto, almacen, fuente, acceso, onAcceso, activo, cabecera)
        }
    }
}

@Composable
private fun Pantalla(
    trayecto: Trayecto,
    almacen: Almacen,
    fuente: FuenteGps,
    acceso: Acceso,
    onAcceso: () -> Unit,
    activo: Boolean,
    cabecera: @Composable () -> Unit,
) {
    var unidad by remember { mutableStateOf(Unidad.desde(almacen.leer("unidad"))) }
    var analogica by remember { mutableStateOf(almacen.leer("vista") != "digital") }
    var ultimaPosicion by remember { mutableStateOf<TimeMark?>(null) }
    var reloj by remember { mutableIntStateOf(0) }

    val escuchando = trayecto.midiendo && acceso == Acceso.LISTO && activo
    if (escuchando) {
        DisposableEffect(fuente) {
            fuente.empezar { fix ->
                trayecto.anadir(fix)
                ultimaPosicion = TimeSource.Monotonic.markNow()
            }
            onDispose {
                fuente.parar()
                ultimaPosicion = null
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000)
                reloj++
            }
        }
    }
    // Se lee [reloj] para que cada segundo se vuelva a mirar si la señal sigue viva.
    val conSenal = reloj >= 0 && escuchando &&
        ultimaPosicion?.let { it.elapsedNow().inWholeMilliseconds < SIN_SENAL_MS } == true

    val r = trayecto.resumen
    val velocidad = if (conSenal) r.velocidadMs?.let(unidad::velocidad) else null

    val indicador: @Composable (Modifier) -> Unit = { mod ->
        Column(mod, horizontalAlignment = Alignment.CenterHorizontally) {
            if (analogica) Esfera(velocidad, unidad, Modifier.fillMaxWidth().widthIn(max = 420.dp))
            else Digital(velocidad, unidad, Modifier.fillMaxWidth().widthIn(max = 520.dp))
            Text(
                stringResource(Res.string.max, formatoNumero(unidad.velocidad(r.maximaMs), 1), unidad.etiqueta),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }

    val controles: @Composable (Modifier) -> Unit = { mod ->
        Column(mod, verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Unidad.entries.forEach { u ->
                    FilterChip(
                        selected = u == unidad,
                        onClick = { unidad = u; almacen.guardar("unidad", u.clave) },
                        label = { Text(u.etiqueta, maxLines = 1, softWrap = false) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = analogica,
                    onClick = { analogica = true; almacen.guardar("vista", "analogica") },
                    label = { Text(stringResource(Res.string.analog)) },
                )
                FilterChip(
                    selected = !analogica,
                    onClick = { analogica = false; almacen.guardar("vista", "digital") },
                    label = { Text(stringResource(Res.string.digital)) },
                )
            }
            Button(
                onClick = {
                    if (trayecto.midiendo) {
                        trayecto.midiendo = false
                    } else {
                        trayecto.midiendo = true
                        if (acceso != Acceso.LISTO) onAcceso()
                    }
                },
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).height(56.dp),
                colors = if (trayecto.midiendo) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                },
            ) {
                Text(
                    stringResource(if (trayecto.midiendo) Res.string.stop else Res.string.start),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Estado(trayecto.midiendo, acceso, conSenal, r.precisionM, onAcceso)
            Datos(r, unidad)
            Reiniciar { trayecto.reiniciar() }
            Text(
                stringResource(Res.string.local_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val apaisada = maxWidth > maxHeight && maxWidth > 560.dp
        Column(Modifier.fillMaxSize()) {
            cabecera()
            if (apaisada) {
                Row(
                    Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    indicador(Modifier.weight(1f))
                    controles(Modifier.weight(1f).verticalScroll(rememberScrollState()))
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    indicador(Modifier.fillMaxWidth())
                    controles(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Esfera de 0 a [Unidad.escala] con aguja, como la de la web. */
@Composable
private fun Esfera(velocidad: Double?, unidad: Unidad, modifier: Modifier) {
    val medidor = rememberTextMeasurer()
    val pista = MaterialTheme.colorScheme.surfaceVariant
    val marcas = MaterialTheme.colorScheme.onSurfaceVariant
    val acento = MaterialTheme.colorScheme.primary
    val aguja = MaterialTheme.colorScheme.error
    val texto = MaterialTheme.colorScheme.onSurface
    val objetivo = ((velocidad ?: 0.0) / unidad.escala).coerceIn(0.0, 1.0).toFloat()
    val fraccion by animateFloatAsState(objetivo, tween(400), label = "aguja")
    val tamanoNumero = with(LocalDensity.current) { 13.dp.toSp() }

    BoxWithConstraints(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val lado = minOf(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) {
            val inicio = 135f
            val barrido = 270f
            val radio = size.minDimension / 2f * 0.94f
            val centro = Offset(size.width / 2f, size.height / 2f)
            val grosor = radio * 0.06f
            val esquina = Offset(centro.x - radio + grosor / 2, centro.y - radio + grosor / 2)
            val caja = Size(2 * radio - grosor, 2 * radio - grosor)
            drawArc(pista, inicio, barrido, false, esquina, caja, style = Stroke(grosor, cap = StrokeCap.Round))
            if (fraccion > 0f) {
                drawArc(acento, inicio, barrido * fraccion, false, esquina, caja, style = Stroke(grosor, cap = StrokeCap.Round))
            }

            val menor = unidad.paso / 2
            for (v in 0..unidad.escala step menor) {
                val grande = v % unidad.paso == 0
                val a = (inicio + barrido * v / unidad.escala) * PI.toFloat() / 180f
                val fuera = radio - grosor * 1.6f
                val dentro = fuera - if (grande) radio * 0.10f else radio * 0.05f
                drawLine(
                    marcas,
                    Offset(centro.x + fuera * cos(a), centro.y + fuera * sin(a)),
                    Offset(centro.x + dentro * cos(a), centro.y + dentro * sin(a)),
                    strokeWidth = if (grande) radio * 0.018f else radio * 0.01f,
                    cap = StrokeCap.Round,
                )
                if (grande) {
                    val medida = medidor.measure(v.toString(), TextStyle(fontSize = tamanoNumero, color = marcas))
                    val rt = dentro - radio * 0.11f
                    drawText(
                        medida,
                        topLeft = Offset(
                            centro.x + rt * cos(a) - medida.size.width / 2f,
                            centro.y + rt * sin(a) - medida.size.height / 2f,
                        ),
                    )
                }
            }

            val a = (inicio + barrido * fraccion) * PI.toFloat() / 180f
            val largo = radio * 0.72f
            drawLine(
                aguja,
                centro,
                Offset(centro.x + largo * cos(a), centro.y + largo * sin(a)),
                strokeWidth = radio * 0.03f,
                cap = StrokeCap.Round,
            )
            drawCircle(texto, radius = radio * 0.06f, center = centro)
        }
        // La lectura en números, debajo del eje de la aguja.
        Column(
            Modifier.align(Alignment.Center).offset(y = lado * 0.27f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                velocidad?.let { formatoNumero(it, 1) } ?: "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(unidad.etiqueta, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Número grande, como la vista digital de la web. No crece con la letra del sistema: ya es enorme. */
@Composable
private fun Digital(velocidad: Double?, unidad: Unidad, modifier: Modifier) {
    BoxWithConstraints(modifier.padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        val tamano = with(LocalDensity.current) { (maxWidth * 0.30f).toSp() }
        val unidadTamano = with(LocalDensity.current) { (maxWidth * 0.08f).toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                velocidad?.let { formatoNumero(it, 1) } ?: "—",
                style = TextStyle(fontSize = tamano, fontWeight = FontWeight.Bold, lineHeight = tamano),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                unidad.etiqueta,
                style = TextStyle(fontSize = unidadTamano),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Estado(midiendo: Boolean, acceso: Acceso, conSenal: Boolean, precision: Double?, onAcceso: () -> Unit) {
    if (midiendo && acceso != Acceso.LISTO) {
        val (mensaje, boton) = when (acceso) {
            Acceso.PEDIR -> Res.string.need_permission to Res.string.grant
            Acceso.DENEGADO -> Res.string.denied to Res.string.open_settings
            Acceso.SOLO_APROXIMADA -> Res.string.approximate to Res.string.grant
            Acceso.GPS_APAGADO -> Res.string.gps_off to Res.string.turn_on
            Acceso.LISTO -> return
        }
        Card(
            Modifier.fillMaxWidth().widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(mensaje), color = MaterialTheme.colorScheme.onErrorContainer)
                FilledTonalButton(onClick = onAcceso) { Text(stringResource(boton)) }
            }
        }
        return
    }
    val (texto, color) = when {
        !midiendo -> stringResource(Res.string.paused) to MaterialTheme.colorScheme.onSurfaceVariant
        !conSenal -> stringResource(Res.string.searching) to MaterialTheme.colorScheme.error
        precision != null -> stringResource(Res.string.accuracy, formatoNumero(precision, 0)) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> "GPS" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(texto, color = color, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
}

@Composable
private fun Datos(r: Resumen, unidad: Unidad) {
    val distancia = unidad.distancia(r.distanciaM)
    Row(
        Modifier.fillMaxWidth().widthIn(max = 520.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Dato(
            stringResource(Res.string.distance),
            "${formatoNumero(distancia, if (distancia < 10) 2 else 1)} ${unidad.etiquetaDistancia}",
            Modifier.weight(1f),
        )
        Dato(
            stringResource(Res.string.average),
            r.mediaMs?.let { "${formatoNumero(unidad.velocidad(it), 1)} ${unidad.etiqueta}" } ?: "—",
            Modifier.weight(1f),
        )
        Dato(stringResource(Res.string.moving), duracion(r.enMarchaMs), Modifier.weight(1f))
    }
}

@Composable
private fun Dato(titulo: String, valor: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(2.dp))
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
    }
}

/** Dos toques para reiniciar: uno sin querer no borra el trayecto. */
@Composable
private fun Reiniciar(onReiniciar: () -> Unit) {
    var armado by remember { mutableStateOf(false) }
    if (armado) {
        LaunchedEffect(Unit) {
            delay(3_000)
            armado = false
        }
    }
    TextButton(onClick = {
        if (armado) {
            onReiniciar()
            armado = false
        } else {
            armado = true
        }
    }) {
        Text(stringResource(if (armado) Res.string.reset_confirm else Res.string.reset))
    }
}

private val naranja = Color(0xFFE65100)

val esquemaClaro: ColorScheme = lightColorScheme(primary = naranja, secondary = Color(0xFF5B5F66))
val esquemaOscuro: ColorScheme = darkColorScheme(primary = Color(0xFFFFB74D), secondary = Color(0xFFC3C7CF))
