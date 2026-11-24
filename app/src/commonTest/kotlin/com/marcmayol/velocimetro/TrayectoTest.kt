package com.marcmayol.velocimetro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrayectoTest {
    // 0,0001° de latitud son 11,12 m.
    private fun fix(seg: Int, metrosNorte: Double, velocidad: Double? = null, precision: Double? = 5.0) =
        Fix(lat = 40.0 + metrosNorte / 111_195.0, lon = 3.0, tiempoMs = seg * 1000L, velocidadMs = velocidad, precisionM = precision)

    @Test
    fun usaLaVelocidadDelGps() {
        val t = Trayecto()
        t.anadir(fix(0, 0.0, velocidad = 25.0))
        assertEquals(25.0, t.resumen.velocidadMs)
        assertEquals(25.0, t.resumen.maximaMs)
    }

    @Test
    fun sinVelocidadDelGpsLaCalculaConLaDistancia() {
        val t = Trayecto()
        t.anadir(fix(0, 0.0))
        assertNull(t.resumen.velocidadMs)
        t.anadir(fix(2, 20.0))
        assertEquals(10.0, t.resumen.velocidadMs!!, 0.05)
        assertEquals(20.0, t.resumen.distanciaM, 0.1)
        assertEquals(2_000, t.resumen.enMarchaMs)
        assertEquals(10.0, t.resumen.mediaMs!!, 0.05)
    }

    @Test
    fun quietoNoSumaDistancia() {
        val t = Trayecto()
        // El GPS baila 3 m arriba y abajo con el coche parado.
        for (s in 0..20) t.anadir(fix(s, if (s % 2 == 0) 0.0 else 3.0, velocidad = 0.1))
        assertEquals(0.0, t.resumen.distanciaM)
        assertEquals(0.0, t.resumen.velocidadMs)
        assertNull(t.resumen.mediaMs)
    }

    @Test
    fun posicionesMalasNoCuentanParaLaMaxima() {
        val t = Trayecto()
        t.anadir(fix(0, 0.0, velocidad = 10.0))
        t.anadir(fix(1, 10.0, velocidad = 60.0, precision = 80.0))
        assertEquals(60.0, t.resumen.velocidadMs)
        assertEquals(10.0, t.resumen.maximaMs)
    }

    @Test
    fun posicionRepetidaNoSumaTiempo() {
        val t = Trayecto()
        t.anadir(fix(0, 0.0, velocidad = 20.0))
        t.anadir(fix(1, 20.0, velocidad = 20.0))
        for (s in 2..10) t.anadir(fix(s, 20.0, velocidad = 20.0))
        assertEquals(1_000, t.resumen.enMarchaMs)
        assertEquals(20.0, t.resumen.mediaMs!!, 0.05)
    }

    @Test
    fun saltosImposiblesNoSonMaxima() {
        val t = Trayecto()
        t.anadir(fix(0, 0.0, velocidad = 300.0))
        assertEquals(0.0, t.resumen.maximaMs)
    }

    @Test
    fun posicionesRepetidasSeIgnoran() {
        val t = Trayecto()
        t.anadir(fix(5, 0.0, velocidad = 10.0))
        t.anadir(fix(5, 50.0, velocidad = 99.0))
        t.anadir(fix(4, 50.0, velocidad = 99.0))
        assertEquals(10.0, t.resumen.velocidadMs)
    }

    @Test
    fun reiniciarBorraElTrayectoPeroNoLaVelocidad() {
        val t = Trayecto()
        t.anadir(fix(0, 0.0, velocidad = 10.0))
        t.anadir(fix(1, 10.0, velocidad = 10.0))
        assertTrue(t.resumen.distanciaM > 0)
        t.reiniciar()
        assertEquals(0.0, t.resumen.distanciaM)
        assertEquals(0.0, t.resumen.maximaMs)
        assertEquals(10.0, t.resumen.velocidadMs)
    }

    @Test
    fun unidades() {
        assertEquals(36.0, Unidad.KMH.velocidad(10.0), 1e-9)
        assertEquals(22.369, Unidad.MPH.velocidad(10.0), 1e-3)
        assertEquals(19.438, Unidad.NUDOS.velocidad(10.0), 1e-3)
        assertEquals(1.0, Unidad.MPH.distancia(1609.344), 1e-9)
        assertEquals(Unidad.KMH, Unidad.desde(null))
        assertEquals(Unidad.NUDOS, Unidad.desde("kn"))
    }

    @Test
    fun duraciones() {
        assertEquals("0:00", duracion(0))
        assertEquals("2:05", duracion(125_000))
        assertEquals("1:02:05", duracion(3_725_000))
    }
}
