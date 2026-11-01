# Correcciones de UI, mapa y sesión

Estado: **corregido en el APK de depuración**, con build y suite en verde
(52 tests, 0 fallos, `lintDebug` 0 errores).

| # | Síntoma reportado | Causa raíz | Corrección |
|---|---|---|---|
| 1 | Desactivar el modo oscuro no pasa a tonos claros | `DriverAppTheme {}` usaba su valor por defecto `isSystemInDarkTheme()`: la preferencia nunca llegaba al tema. Además `AppCompatDelegate.setDefaultNightMode` **no se aplica** en `ComponentActivity` (sólo en `AppCompatActivity`), así que era código muerto. | `AppNav` recoge `userPreferences.darkMode` y lo pasa como `darkTheme = darkPref`. |
| 2 | El mapa no carga / se queda bugueado | Nunca se llamaba a `Configuration.getInstance().load(...)` de osmdroid: `osmdroidBasePath` y la caché de teselas quedaban sin definir, y el `User-Agent` se asignaba tarde (dentro del `factory`). Además la cámara arrancaba en `(0,0)` con zoom 15 → océano, sin `onPause`/`onDetach`. | `initOsmdroid()` en `App.onCreate` (load + UA `paquete/version` + `osmdroidBasePath`/`osmdroidTileCache`); el `MapView` se centra en el pedido o en la posición del repartidor, y `DisposableEffect` reenvía `onResume`/`onPause`/`onDetach`. |
| 3 | El botón decía «Llamar al restaurante» | La app es de **delivery de SUMAQ**: a quien llama el repartidor es al **cliente**. El botón usaba `restaurant.phone`, y el modelo `orders` **no tenía teléfono del cliente**. | Backend: `orders.client_phone` (columna, modelo, `OrderCreate`, `OrderOut`). App: `Pedido.clientPhone` + DTO; botón «Llamar al cliente»; si el pedido no trae teléfono se avisa en vez de marcar a otro. |
| 4 | Desactivar la ubicación en tiempo real cierra la app | `LocationForegroundService.stop()` enviaba un intent `ACTION_STOP` con `startService()`, es decir, **arrancaba el servicio para pedirle que se parara**: pasaba por `onStartCommand` y podía romper el contrato de `startForegroundService`. | `stop()` usa `context.stopService()` (no ejecuta `onStartCommand`); `stopTracking()` llama a `stopForeground(STOP_FOREGROUND_REMOVE)` para no dejar la notificación colgada. |
| 5 | Cerrar sesión cierra la app | `nav.navigate(LOGIN) { popUpTo(0) { inclusive = true } }` desmontaba el grafo entero mientras el `NavHost` seguía componiendo. | `popUpTo(nav.graph.id) { inclusive = true }` + `launchSingleTop = true`. |
| 6 | El icono del login no aparece (o sale un bloque) | `Icon(painter = app_logo, tint = onBackground)`: el `tint` pinta **toda** la imagen de un color. El asset es un PNG 192×192 con fondo negro y pin blanco, así que se veía una mancha lisa. | `Image(...)` sin `tint`, recortado con `RoundedCornerShape(22.dp)` y borde `outlineVariant` para que el cuadrado negro no se pierda sobre fondo oscuro. |

## Verificación

| Comprobación | Resultado |
|---|---|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL · APK 17,0 MB |
| `./gradlew :app:testDebugUnitTest` | 52 tests, 0 fallos |
| `./gradlew :app:lintDebug` | 0 errores |
| `GET /orders/available` con JWT | devuelve `client_phone` en cada pedido (`+51 999 111 2NN`) |

## Registro de crashes en el teléfono

Como no hay Play Console ni emulador estable en este equipo, la app ahora **guarda el
último fallo** en:

```
Android/data/com.driverapp.repartidor/files/crash_last.txt
```

El fichero trae fecha, versión, modelo de teléfono, hilo y el stacktrace completo.
Si los puntos 4 y 5 volvieran a fallar, ese archivo dice exactamente en qué línea.

## Datos de prueba

Los pedidos que no tenían teléfono recibieron un número ficticio
`+51 999 111 2NN` (NN = id del pedido) para poder probar el botón. Los pedidos
nuevos lo traen del formulario del panel de administración.
