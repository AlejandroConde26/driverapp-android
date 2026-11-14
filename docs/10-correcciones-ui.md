# Correcciones de UI, mapa y sesión

Estado: **corregido en el APK de depuración**, con build y suite en verde
(56 tests, 0 fallos, `lintDebug` 0 errores).

| # | Síntoma reportado | Causa raíz | Corrección |
|---|---|---|---|
| 1 | Desactivar el modo oscuro no pasa a tonos claros | `DriverAppTheme {}` usaba su valor por defecto `isSystemInDarkTheme()`: la preferencia nunca llegaba al tema. Además `AppCompatDelegate.setDefaultNightMode` **no se aplica** en `ComponentActivity` (sólo en `AppCompatActivity`), así que era código muerto. | `AppNav` recoge `userPreferences.darkMode` y lo pasa como `darkTheme = darkPref`. |
| 2 | El mapa no carga / se queda bugueado | Nunca se llamaba a `Configuration.getInstance().load(...)` de osmdroid: `osmdroidBasePath` y la caché de teselas quedaban sin definir, y el `User-Agent` se asignaba tarde (dentro del `factory`). Además la cámara arrancaba en `(0,0)` con zoom 15 → océano, sin `onPause`/`onDetach`. | `initOsmdroid()` en `App.onCreate` (load + UA `paquete/version` + `osmdroidBasePath`/`osmdroidTileCache`); el `MapView` se centra en el pedido o en la posición del repartidor, y `DisposableEffect` reenvía `onResume`/`onPause`/`onDetach`. |
| 3 | El botón decía «Llamar al restaurante» | La app es de **delivery de SUMAQ**: a quien llama el repartidor es al **cliente**. El botón usaba `restaurant.phone`, y el modelo `orders` **no tenía teléfono del cliente**. | Backend: `orders.client_phone` (columna, modelo, `OrderCreate`, `OrderOut`). App: `Pedido.clientPhone` + DTO; botón «Llamar al cliente»; si el pedido no trae teléfono se avisa en vez de marcar a otro. |
| 4 | Desactivar la ubicación en tiempo real cierra la app | `LocationForegroundService.stop()` enviaba un intent `ACTION_STOP` con `startService()`, es decir, **arrancaba el servicio para pedirle que se parara**: pasaba por `onStartCommand` y podía romper el contrato de `startForegroundService`. | `stop()` usa `context.stopService()` (no ejecuta `onStartCommand`); `stopTracking()` llama a `stopForeground(STOP_FOREGROUND_REMOVE)` para no dejar la notificación colgada. |
| 5 | Cerrar sesión cierra la app | `nav.navigate(LOGIN) { popUpTo(0) { inclusive = true } }` desmontaba el grafo entero mientras el `NavHost` seguía componiendo. | `popUpTo(nav.graph.id) { inclusive = true }` + `launchSingleTop = true`. |
| 6 | El icono del login no aparece (o sale un bloque) | `Icon(painter = app_logo, tint = onBackground)`: el `tint` pinta **toda** la imagen de un color. El asset es un PNG 192×192 con fondo negro y pin blanco, así que se veía una mancha lisa. | `Image(...)` sin `tint`, recortado con `RoundedCornerShape(22.dp)` y borde `outlineVariant` para que el cuadrado negro no se pierda sobre fondo oscuro. |
| 7 | El idioma inglés no se aplicaba en toda la app | `MainActivity` era `ComponentActivity` y `AppCompatDelegate.setApplicationLocales` **sólo actúa con AppCompat**; además al cambiar de idioma nadie llamaba a `applyLanguage`, solo se recreaba la Activity. Las cadenas ya estaban 166/166 traducidas. | `MainActivity : AppCompatActivity()` y `onLanguageChanged(code)` aplica el idioma antes de recrear. Añadido `LocalizationTest` (4 casos) que falla si falta una clave, si cambian los argumentos de formato o si queda español en `values-en`. |

## Mejoras de usabilidad y notificaciones

| # | Pedido | Implementación |
|---|---|---|
| 8 | Ver mi ubicación en el mapa en tiempo real | Se activa la capa nativa `map.isMyLocationEnabled` (punto azul, se mueve solo) cuando hay permiso; si no lo hay, se dibuja el marcador con la última posición conocida. Sin pedido activo la cámara se centra en el repartidor. |
| 9 | Botón para la ruta en Google Maps | Botón «Cómo llegar en Google Maps» con cadena de intents: `google.navigation:q=lat,lng&mode=d` (app de Maps) → `geo:` → Google Maps web. Si el teléfono no tiene Maps, cae al siguiente y avisa con `map_route_error`. |
| 10 | Volver a centrar el mapa | Botón flotante con `ic_fa_location_crosshairs` sobre el mapa; resetea el centrado y vuelve a la posición del repartidor. |
| 11 | Pantalla de carga al cambiar preferencias | `BusyOverlay(applying, "Aplicando cambios…")` en Perfil: cada interruptor y el cambio de idioma muestran el aviso 400–450 ms antes de aplicarse (antes el tema o el idioma cambiaban de golpe). |
| 12 | Notificación de pedido nuevo que abra el pedido | Backend: al crear un pedido, `_notify_new_order()` avisa a todos los repartidores activos con payload `{type: new_order, order_id, client}`. App: el servicio arma el texto en el idioma del usuario, usa una notificación por pedido y su `PendingIntent` lleva `extra_order_id`; `MainActivity` (ahora `singleTop`) lo recibe en `onNewIntent` y `AppNav` navega a Órdenes y abre el detalle (`OrdersViewModel.openOrderById`). |
| 13 | El mapa marcaba siempre la misma dirección | **Dos causas.** (a) El mapa dibuja el pedido **en curso**, no los disponibles: con un solo pedido aceptado no hay nada que cambie. (b) Bug real: `replaceBucket()` reescribía `updatedAt = ahora` en **cada refresco**, así que todos los pedidos del bucket quedaban empatados y `ORDER BY updatedAt DESC LIMIT 1` devolvía siempre el mismo. | `replaceBucket()` conserva el sello de los pedidos que no cambiaron de estado → gana el último **cambio** real (aceptar, salir a camino). Añadido «Ver en el mapa» en el detalle de pedidos en curso: `mapFocus` → `TrackingViewModel.displayedOrder`. Tocar la pestaña Mapa limpia el foco y vuelve al pedido activo. |

## Verificación

| Comprobación | Resultado |
|---|---|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL · APK 17,0 MB |
| `./gradlew :app:testDebugUnitTest` | **56 tests, 0 fallos** (incluye `LocalizationTest`) |
| `./gradlew :app:lintDebug` | 0 errores |
| `GET /orders/available` con JWT | devuelve `client_phone` en cada pedido (`+51 999 111 2NN`) |
| APK: referencias a osmdroid | **0** (migrado a `play-services-maps`, 632 referencias) |
| Manifest fusionado | `com.google.android.geo.API_KEY` presente |
| Cadenas es/en | 166/166 claves iguales, mismos argumentos de formato, 0 español en `values-en` |
| Pedido creado por el admin | `POST /admin/orders` → 200 y notificación in-app «Nuevo pedido disponible — Pedido #30» visible en `/notifications/my` del repartidor |
| Push de pedido nuevo | intentado en el acto; responde `SenderIdMismatch` mientras el teléfono siga con el APK del proyecto Firebase anterior |

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
