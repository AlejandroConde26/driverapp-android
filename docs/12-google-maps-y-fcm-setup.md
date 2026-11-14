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

## 5. Firebase: el proyecto no coincide (bloqueante)

| Dato | Valor |
|---|---|
| Proyecto del **service account** (`proyectorepartidor-87835`) | credenciales válidas: `firebase-admin` inicializa y la API de FCM responde |
| Apps Android registradas en ese proyecto | **ninguna** |
| Proyecto de la **app** (`FIREBASE_SENDER_ID`) | `1009763251368` (`camarasproyectocdlatam`) |

Un token FCM solo es válido para el proyecto que lo emitió: si el backend envía con
credenciales de otro proyecto, FCM lo rechaza (`SENDER_ID_MISMATCH`). Hay dos salidas:

1. **Recomendada:** registrar `com.driverapp.repartidor` en `proyectorepartidor-87835`
   (Añadir app → Android), descargar su `google-services.json` y pasarme la `apiKey`,
   el `mobilesdk_app_id` y el `project_number`. Cambio de 3 valores en `local.properties`.
2. Generar el service account en `camarasproyectocdlatam` y reemplazar el JSON del
   servidor (no requiere tocar la app).
