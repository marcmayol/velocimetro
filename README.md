# Velocímetro

Velocímetro GPS para Android e iOS, a imagen de la web [gpsspeedometer.app](https://gpsspeedometer.app/):
pulsas Empezar y te dice a qué velocidad vas.

- km/h, mph, m/s o nudos.
- Esfera analógica (0-240 km/h) o número grande en digital.
- Velocidad máxima, y además distancia, media y tiempo en marcha.
- La pantalla no se apaga mientras está abierta.
- Sin internet: lee el GPS directamente (`GPS_PROVIDER` en Android, CoreLocation en iOS), sin
  Google Play Services ni mapas. La ubicación no sale del móvil. Android solo usa la red para
  buscar actualizaciones en DracApps.

## Cómo está hecho

Kotlin Multiplatform con Compose Multiplatform, la misma base que Yenes. En `app/src/commonMain` van:

- la pantalla (`App.kt`);
- el cálculo (`Trayecto.kt`): usa la velocidad Doppler del GPS si la da y, si no, la calcula con la
  distancia entre posiciones. Parado, el GPS baila unos metros, por eso solo suma distancia por
  encima de 3 km/h y con posiciones de ±30 m o mejores;
- las unidades (`Unidad.kt`).

Cada plataforma pone solo el GPS, el permiso y las preferencias (`Plataforma.*.kt`).
Con la app en segundo plano no se escucha al GPS.

Tests: `gradlew :app:testDebugUnitTest`. La parte de iOS se compila en un Mac de GitHub
(`.github/workflows/ios.yml`), que pasa los tests en el simulador, abre la app y guarda una captura.

Para probar en el emulador de Android con movimiento: `adb emu geo fix <lon> <lat> <alt> <satélites> <nudos>`.
Sin el último número, el emulador manda velocidad 0.

## Android

Se reparte por [DracApps](https://marcmayol.com/DracApps/) y se actualiza sola.

```
python scripts/publicar_release.py --notas "…"
```

La versión está en `gradle.properties`.

## iPhone

Hace falta un Mac con Xcode. Con un Apple ID gratuito funciona, pero la app caduca a los 7 días.

```
brew install xcodegen
cd iosApp && xcodegen && open Velocimetro.xcodeproj
```

En Xcode: Velocimetro → Signing & Capabilities → elige tu equipo, conecta el iPhone y dale a Run.
