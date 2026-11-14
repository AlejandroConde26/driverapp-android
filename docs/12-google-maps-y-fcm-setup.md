# Google Maps y FCM — datos y paso a paso de configuración

Documento de trámites: qué poner en Google Cloud / Firebase y qué me tienes que
entregar. El código del mapa todavía **no** está migrado (hoy la app usa osmdroid).

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

## 4. Lo que falta en código (no incluido en esta entrega)

| Tarea | Detalle |
|---|---|
| Migrar el mapa | `TrackingScreen`: `MapView` (osmdroid) → `GoogleMap` de `maps-compose`; marcadores de restaurante, cliente y repartidor; cámara y ciclo de vida |
| Key en el build | `MAPS_API_KEY` en `local.properties` → `manifestPlaceholders` → `com.google.android.geo.API_KEY` |
| Dependencias | `com.google.maps.android:maps-compose` + `play-services-maps`; quitar `osmdroid` |
| Degradación | Si falta la key o Google Play Services, mostrar un aviso en vez de un mapa vacío |
