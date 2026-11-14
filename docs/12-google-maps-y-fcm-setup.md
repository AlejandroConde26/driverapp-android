# Google Maps y FCM — datos y paso a paso de configuración

**Estado: mapa migrado a Google Maps (SDK Android).** La app ya no usa osmdroid.
Firebase: credenciales instaladas en el servidor, pero **el push no llega todavía**
porque el proyecto del service account no es el de la app (ver §3.1).

## 1. Datos de la app para restringir la API key

| Campo | Valor |
|---|---|
| **Package name / applicationId** | `com.driverapp.repartidor` |
| **SHA-1 (debug)** | `47:92:21:36:78:BD:EC:8D:97:5A:53:68:D6:02:B6:04:FF:A6:87:78` |
| **SHA-256 (debug)** | `5A:67:8C:16:1F:A1:24:6D:76:75:A6:B2:7F:CF:99:0D:84:41:88:AF:B2:CD:BF:AC:99:03:75:0C:D1:3F:D5:B9` |
| Keystore usado | `/home/alexei/.config/.android/debug.keystore` alias `androiddebugkey` |
| **SHA-1 (release)** | **pendiente**: no existe keystore de release todavía |

Si más adelante se publica, hay que generar el keystore de release y añadir su SHA-1
a la **misma** restricción; si no, el mapa dejará de cargar en el APK firmado.

## 2. Restringir la API key en Google Cloud

1. Console → **APIs y servicios → Biblioteca** → habilitar **«Maps SDK for Android»**.
2. **APIs y servicios → Credenciales** → tu clave → **Editar**.
3. **Restricciones de aplicación** → *Aplicaciones para Android* → **Añadir**:
   - Nombre del paquete: `com.driverapp.repartidor`
   - Huella digital SHA-1: `47:92:21:36:78:BD:EC:8D:97:5A:53:68:D6:02:B6:04:FF:A6:87:78`
4. **Restricciones de API** → *Restringir clave* → marcar **Maps SDK for Android**
   (y nada más, salvo que se añada Routes).
5. Guardar y esperar ~5 minutos de propagación.
6. Proyecto de facturación: **debe tener una cuenta de facturación activa**, aunque el
   SKU *Maps SDK* es de uso ilimitado y sin cargo.

La key se guarda **solo** en `local.properties` (ignorado por git) y se inyecta al
manifest por `manifestPlaceholders`; aparece dentro del APK, y eso es normal: la
restricción por package + SHA-1 es lo que impide que otro la use.

## 3. Firebase: enviar push de verdad (service account)

Hoy el backend registra el mensaje y no lo envía porque `FIREBASE_CREDENTIALS_PATH`
está vacío. Pasos en la consola:

1. Entrar a <https://console.firebase.google.com> y abrir el proyecto
   **camarasproyectocdlatam** (el del `appId 1:1009763251368:android:91893284a4d2a137453e9d`).
2. Engranaje ⚙ → **Configuración del proyecto** → pestaña **Cuentas de servicio**.
3. Botón **«Generar nueva clave privada»** → confirmar → se descarga un `.json`.
4. Renombrarlo a `firebase-service-account.json` y dejarlo en **`~/Downloads/`** de
   este equipo.
5. Avisarme: lo copio al servidor como
   `/root/driverapp/backend/firebase-service-account.json` con permisos `600`, añado
   la ruta al `.env` y reinicio `driverapp-api`.
6. Comprobación: `POST /api/v1/notifications/send` desde el panel y el log debe decir
   **«Firebase Admin inicializado»**; el móvil con sesión abierta recibe el aviso.

**El JSON contiene una clave privada**: no debe pegarse en el chat ni subirse al repo.
Si se filtra, se revoca en la misma pantalla («Administrar claves») y se genera otra.

Además, en Google Cloud debe estar habilitada **«Firebase Cloud Messaging API»**
(Cloud Messaging V1); normalmente ya lo está al crear el proyecto.

## 4. Estado del código (hecho)

| Tarea | Estado |
|---|---|
| Mapa | `TrackingScreen` usa `MapView` de `play-services-maps` con `getMapAsync`; marcadores de restaurante, cliente y repartidor; cámara centrada una sola vez |
| Key en el build | `MAPS_API_KEY` en `local.properties` → `manifestPlaceholders` → `com.google.android.geo.API_KEY` (verificado en el manifest fusionado) |
| Dependencias | `com.google.android.gms:play-services-maps:18.2.0`; osmdroid eliminado (0 referencias en el APK) |
| Degradación | Sin Google Play Services se muestra `map_play_services_missing` en vez de un mapa vacío |
| Ciclo de vida | `onCreate`/`onResume`/`onPause`/`onDestroy` conectados al lifecycle de Compose |

## 5. Firebase: proyecto alineado (resuelto)

La app y el service account del backend pertenecen ahora **al mismo proyecto**:

| Dato | Valor |
|---|---|
| Proyecto | `proyectorepartidor-87835` |
| Cuenta de la consola | `alejandro.conde.contacto@gmail.com` |
| Paquete registrado | `com.driverapp.repartidor` |
| `mobilesdk_app_id` | `1:1051770700715:android:81378759d7e77d408c08ee` |
| Sender ID (project_number) | `1051770700715` |
| Service account del backend | `firebase-adminsdk-fbsvc@proyectorepartidor-87835.iam.gserviceaccount.com` |

Los tres valores viajan en `local.properties` (nunca en el repositorio) y llegan al
build como `BuildConfig.FIREBASE_API_KEY`, `FIREBASE_APP_ID` y `FIREBASE_SENDER_ID`;
el `project_id` del service account coincide con el de la app.

El archivo `google-services.json` **no se versiona** (contiene la API key y lo exige el
plugin de Google): del archivo se copian los tres valores a `local.properties` y la app
inicializa Firebase con `FirebaseOptions` manual, sin depender del plugin.

### 5.1 Evidencia del problema que había

Antes de alinear, con el token que la app registró siendo del proyecto anterior:

```text
users.fcm_token (usuario 2) = dHxGS3TaRpuocoHmA6… (142 chars)
envío con credenciales de proyectorepartidor-87835 →
  SenderIdMismatchError: SenderId mismatch
```

Ese token se puso a `NULL` en la base de datos: era inservible y el nuevo se registra
solo al abrir la app con el APK actualizado (`PUT /users/me/fcm-token`).

### 5.2 Verificación del circuito

| Paso | Resultado |
|---|---|
| `firebase-admin` inicializa con el JSON | OK |
| API de FCM acepta la petición | error de token, no de credenciales |
| `POST /api/v1/notifications/send` (admin) | **HTTP 200**, notificación persistida (id 23) |
| Entrega al teléfono | pendiente: requiere el APK nuevo instalado (el token debe emitirse en el proyecto nuevo) |

## 6. Comportamiento del mapa en la versión actual

Resumen de lo que hace el mapa (`ui/tracking/TrackingScreen.kt`) y qué se necesita para que
funcione en el teléfono:

| Aspecto | Implementación |
|---|---|
| Vista | `MapView` dentro de `AndroidView`, ciclo de vida conectado al `LifecycleOwner` (`onCreate`/`onResume`/`onPause`/`onDestroy`) |
| Marcadores | Restaurante **azul**, cliente **rojo**, pedidos disponibles **naranja** (con `id` en el `tag`), repartidor **verde** solo si no hay punto azul nativo |
| Encuadre | `newLatLngBounds` con todos los puntos y 140 px de relleno; se reajusta al cambiar de pedido y al pulsar «Centrar en mi ubicación» (no en cada lectura del GPS) |
| Tocar un pedido disponible | Abre `AvailableOrderCard` bajo el mapa: cliente, dirección, distancia, importe, pago y botones **Aceptar / Rechazar** |
| Mi ubicación | `isMyLocationEnabled` con permiso concedido; sin permiso se dibuja un marcador verde y una banda de aviso sobre el mapa |
| Ruta | `google.navigation:` → `geo:` → Maps web. Con el pedido `ACEPTADO` apunta al **restaurante** («Cómo llegar al restaurante en Google Maps»); desde `EN_CAMINO`, al **cliente** |
| Permiso de ubicación | Se solicita al primer arranque con sesión, una sola vez (`location_asked` en DataStore); si la denegación es definitiva, se abre Ajustes → Permisos |

**Requisito de la clave:** en Google Cloud, la key de `MAPS_API_KEY` debe tener habilitada
*Maps SDK for Android* y estar restringida por nombre de paquete (`com.driverapp.repartidor`)
y huella SHA-1 (`47:92:21:36:78:BD:EC:8D:97:5A:53:68:D6:02:B6:04:FF:A6:87:78` para debug).
Sin eso el mapa sale gris aunque las coordenadas sean correctas.
