# Informe técnico integrado — Geolocalización, permisos, privacidad, persistencia, REST y Firebase

**Proyecto:** DriverApp — aplicación del repartidor del restaurante SUMAQ
**Paquete Android:** `com.driverapp.repartidor` · `minSdk 24` · `targetSdk 34`
**Backend:** `http://169.58.190.93:8084` (API en `/api/v1`)
**Repositorio:** <https://github.com/AlejandroConde26/driverapp-android> (rama `main`)
**Documento:** versión 1.0 — consolidado a partir de `docs/01`, `02`, `03`, `11`, `12`, `14`, `auth-backend.md` y `S10-notificaciones-fcm.md`
**Fecha de verificación:** 14/11/2026

> Este documento reúne en un solo lugar los seis ejes del producto (Google Maps API,
> permisos, privacidad con Términos y Condiciones, persistencia local, REST y Firebase)
> **y la evidencia de que funcionan**. Cada afirmación se contrastó contra el código, el
> build o el servidor, no contra la documentación previa.

---

## 1. Matriz de cumplimiento

| Eje | Alcance | Estado | Evidencia |
|---|---|---|---|
| **Geolocalización con Google Maps API** | Mapa, marcadores, ruta, punto azul | ✅ | `play-services-maps:18.2.0`, `TrackingScreen.kt`, `MAPS_API_KEY` en manifest |
| **Permisos (todos)** | 7 declarados, 8 descartados con motivo, flujo de denegación | ✅ | `AndroidManifest.xml`, `AppNav.kt`, `docs/11` §1 |
| **Privacidad + T&C** | Datos tratados, base legal, TyC, Política, Data safety | ✅ documentación | Apartados 5 de este documento |
| **Persistencia local** | Room, DataStore, EncryptedSharedPreferences, cola offline | ✅ | 5 entidades, esquema exportado, 56 tests |
| **REST** | 20 endpoints Retrofit, JWT, manejo de errores | ✅ | `ApiService.kt`, verificación en vivo |
| **Firebase** | Solo FCM (push de pedidos) | ✅ | Proyecto unificado + `message_id` real |
| **Geolocalización en vivo** | Reporte cada 30 s al backend | ✅ verificado en producción | `PUT /users/me/location` cada ~50 s desde el teléfono |

**Verificación del código:** `56 tests, 0 fallos` · `lintDebug`: **0 errores** · `assembleDebug`: OK.

---

## 2. Geolocalización con Google Maps API

### 2.1 Integración

| Elemento | Implementación |
|---|---|
| Dependencia | `com.google.android.gms:play-services-maps:18.2.0` (sustituyó a osmdroid) |
| Clave | `MAPS_API_KEY` en `local.properties` (ignorado por git) → `manifestPlaceholders` → `<meta-data android:name="com.google.android.geo.API_KEY">` |
| Vista | `MapView` dentro de `AndroidView`, con `getMapAsync` para obtener `GoogleMap` |
| Ciclo de vida | `onCreate`/`onResume`/`onPause`/`onDestroy` conectados al `LifecycleOwner` de Compose (`LifecycleEventObserver` + `DisposableEffect`); sin esto la vista queda en blanco al volver a la pestaña |
| Play Services | Se comprueba con `GoogleApiAvailability`; si falta, se muestra un aviso en lugar de un mapa gris |

### 2.2 Marcadores y encuadre

| Marcador | Color | Contenido |
|---|---|---|
| Restaurante (origen) | Azul (`HUE_AZURE`) | «Restaurante: SUMAQ» |
| Cliente (destino) | Rojo (`HUE_RED`) | «Cliente: ‹nombre›» + dirección en el subtítulo |
| Pedidos disponibles | Naranja (`HUE_ORANGE`) | «Disponible · pedido #N» + dirección; **tocables** |
| Repartidor | Verde (`HUE_GREEN`) | Solo si no hay punto azul nativo (permiso pendiente) |

El encuadre usa `CameraUpdateFactory.newLatLngBounds` con los puntos de la ruta y 140 px de
relleno, y se recalcula solo cuando cambia el pedido o al pulsar «Centrar en mi ubicación» —
no en cada lectura del GPS, para que la cámara no baile.

**Corrección documentada:** antes la cámara se centraba únicamente en el restaurante. Como
SUMAQ tiene coordenadas en `-12.0464, -77.0428` (Plaza 2 de Mayo) y el destino de un pedido
de Barranco está 11,4 km al sur, el cliente quedaba fuera de pantalla y el único punto visible
era el del restaurante.

### 2.3 Rutas

`openRoute()` encadena tres alternativas: `google.navigation:q=lat,lng` → `geo:lat,lng` →
`https://www.google.com/maps/dir/?api=1&destination=lat,lng`.

| Estado del pedido | Destino del botón | Texto |
|---|---|---|
| `ACEPTADO` (aún no recogido) | Coordenadas del restaurante | «Cómo llegar al restaurante en Google Maps» |
| `EN_CAMINO` (ya salió) | Coordenadas del cliente | «Cómo llegar al cliente en Google Maps» |

### 2.4 Reporte de posición en tiempo real

| Elemento | Valor |
|---|---|
| Servicio | `LocationForegroundService` (primer plano, `foregroundServiceType="location"`) |
| Proveedor | `LocationManager` (GPS + red) con `requestLocationUpdates` |
| Ciclo interno | Tick de **10 s**; reporte al backend cada **30 s** (`REPORT_INTERVAL_MS`) si hay lectura nueva |
| Endpoint | `PUT /api/v1/users/me/location` con `{latitude, longitude, online}` |
| Arranque | `MainActivity.onResume()` solo si la preferencia está activa y hay permiso (en Android 12+ arrancarlo desde `Application.onCreate` lanza `ForegroundServiceStartNotAllowedException`) |

**Evidencia en producción:** el teléfono del repartidor ejecutó `PUT /api/v1/users/me/location`
cada ~50 s (registro del servidor) y la base quedó con `latitude=-12.1219`,
`longitude=-77.0297`, `online=true`.

---

## 3. Permisos (todos)

### 3.1 Declarados en `AndroidManifest.xml` (7)

| Permiso | Tipo | Para qué | Cuándo se pide | Si se niega |
|---|---|---|---|---|
| `INTERNET` | Normal | API REST y FCM | Instalación | La app no sirve |
| `ACCESS_NETWORK_STATE` | Normal | Detectar conectividad para la cola offline | Instalación | Los reintentos fallan |
| `ACCESS_FINE_LOCATION` | Peligroso | Ubicar al repartidor y trazar rutas | **Primer arranque con sesión** (una vez) y desde Perfil | Se desactiva el rastreo; el resto funciona |
| `ACCESS_COARSE_LOCATION` | Peligroso | Ubicación aproximada (Android 12+) | Junto con la anterior | Sin ubicación aproximada tampoco |
| `POST_NOTIFICATIONS` | Peligroso (API 33+) | Avisar de pedidos nuevos | Primer arranque / tras login | No llegan avisos visibles |
| `FOREGROUND_SERVICE` | Normal | Mantener el reporte con la app en segundo plano | Instalación | El rastreo se corta |
| `FOREGROUND_SERVICE_LOCATION` | Normal (API 34+) | Tipar el servicio como `location` | Instalación | `SecurityException` al arrancar |

### 3.2 Permisos evaluados y descartados (8)

| Permiso | Motivo del descarte |
|---|---|
| `ACCESS_BACKGROUND_LOCATION` | El servicio en primer plano ya reporta con la pantalla apagada; este permiso exige declaración especial y **video de revisión** en Play |
| `CAMERA` | Se retiró junto con el comprobante en Firebase Storage (fuera de alcance) |
| `USE_FULL_SCREEN_INTENT` | Play lo restringe a apps de llamadas/alarma; el canal `orders` usa `IMPORTANCE_HIGH` |
| `RECEIVE_BOOT_COMPLETED` | El reporte se reanuda al abrir la app (`MainActivity.onResume`) |
| `WAKE_LOCK` | `requestLocationUpdates` mantiene el GPS mientras el servicio vive |
| `VIBRATE` | La vibración la aplica el canal de notificación del sistema |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Play lo penaliza; se documenta la exclusión manual |
| `READ/WRITE_EXTERNAL_STORAGE` | No se escribe en almacenamiento compartido; caché de Maps y Room van en el directorio privado |

### 3.3 Flujo de solicitud

| Estado | Comportamiento implementado |
|---|---|
| Primera vez | Al entrar con sesión iniciada se lanza el diálogo del sistema; la marca `location_asked` (DataStore) evita repetirlo |
| Concedido | Arranca `LocationForegroundService` y aparece la notificación persistente |
| Denegado una vez | Aviso `msg_location_denied`; la preferencia pasa a `false`; se puede reintentar desde Perfil |
| **Denegado permanentemente** | La app **abre Ajustes → Permisos** de la aplicación (`shouldShowRequestPermissionRationale` = `false`) |
| Concedido solo aproximada | Se acepta: el mapa funciona con menor precisión |
| Revocado con la app abierta | El servicio se detiene y el mapa oculta el punto azul |

---

## 4. Privacidad — datos tratados

| Dato | Origen | Finalidad | Almacenamiento | Retención | Destinatario |
|---|---|---|---|---|---|
| Correo y contraseña | Usuario | Autenticación | Backend: `users.email` + hash **bcrypt** | Mientras exista la cuenta | Backend propio |
| Token de sesión (JWT) | Backend | Autorizar cada request | **Cifrado** en el móvil (`EncryptedSharedPreferences`) | Hasta cerrar sesión / 7 días | Backend propio |
| Nombre, teléfono, vehículo | Usuario | Perfil y asignación | Backend + caché local | Mientras exista la cuenta | Backend propio |
| **Ubicación (lat/lng)** | GPS | Asignar pedidos y seguir la entrega | Backend (`users.latitude/longitude`), última posición | Se sobrescribe; **sin histórico** | Backend propio |
| Token FCM | Firebase | Enviar notificaciones | Backend (`users.fcm_token`) | Hasta cambio de dispositivo | Google (Firebase) |
| Pedidos y ganancias | Backend | Operación diaria | Room local + backend | Local hasta sincronizar | Backend propio |
| Dirección y teléfono del cliente | Panel del restaurante | Entregar el pedido | Backend (`orders.client_*`) | Lo que defina el restaurante | Repartidor asignado |

**Terceros:** Google (Firebase Cloud Messaging) para el push y el proveedor de teselas del mapa.
La contraseña **nunca** se guarda en claro: solo su hash bcrypt (coste 12) en el servidor.

**Base legal (Perú, Ley 29733):** consentimiento al aceptar los Términos y ejecución de la
relación de servicio. Derechos ARCO por correo al responsable. Servicio solo para mayores de
edad. Sin venta de datos a terceros.

### 4.1 Términos y Condiciones (texto base, v1.0)

**1. Objeto.** DriverApp es la aplicación del repartidor del restaurante SUMAQ; permite
recibir pedidos, gestionar la entrega y consultar ganancias.

**2. Cuenta.** El acceso requiere credenciales entregadas por el restaurante. El repartidor es
responsable de su confidencialidad y de la veracidad de sus datos (nombre, teléfono, vehículo).

**3. Geolocalización.** Al activar «Ubicación en tiempo real» el repartidor autoriza la captura
de su posición mientras la aplicación está en primer o segundo plano y el reporte al sistema,
con la única finalidad de asignar pedidos y permitir el seguimiento de la entrega. Puede
desactivarla en cualquier momento en Perfil.

**4. Uso aceptable.** Prohibido manipular estados de pedido, compartir la cuenta, suplantar
identidad o usar la aplicación fuera de la operación del restaurante.

**5. Datos del cliente.** La dirección y el teléfono del cliente son datos operativos: se usan
solo para completar la entrega y no deben almacenarse ni difundirse fuera de la aplicación.

**6. Disponibilidad.** El servicio puede interrumpirse por mantenimiento o fallos de red; la
aplicación opera con caché y cola offline y sincroniza al reconectar.

**7. Modificaciones.** El restaurante puede actualizar estos términos; la versión vigente se
muestra en la aplicación y su aceptación queda registrada.

**8. Legislación aplicable.** Legislación de la República del Perú.

### 4.2 Política de Privacidad (texto base, v1.0)

**Responsable:** SUMAQ (restaurante) — completar razón social, RUC y correo de contacto antes
de publicar.

**Datos que recogemos:** identificación y contacto del repartidor (nombre, correo, teléfono),
datos del vehículo, ubicación mientras el reporte está activo, token de notificaciones y
actividad de pedidos.

**Para qué:** autenticar el acceso, asignar y seguir pedidos, calcular distancias y ganancias,
y enviar notificaciones de pedido.

**Con quién:** únicamente el personal autorizado del restaurante y el servicio de notificaciones
(Google Firebase). No se venden ni se ceden datos con fines publicitarios.

**Cuánto tiempo:** mientras la cuenta esté activa. La última posición se sobrescribe; no se
guarda un histórico de recorridos.

**Seguridad:** contraseñas con hash bcrypt, sesión cifrada en el dispositivo, token firmado y
permisos de ubicación revocables.

**Derechos:** acceso, rectificación, cancelación y oposición escribiendo a **[correo de
contacto]**. También puede desinstalar la aplicación y solicitar la baja.

**Cambios:** se publicará la versión y la fecha; los cambios sustanciales se avisan dentro de
la aplicación.

> **Nota de alcance:** por indicación del cliente, los Términos y la Política son
> **documentación**; no hay pantallas de T&C dentro de la app.

### 4.3 Formulario «Data safety» de Play Store

| Sección | Respuesta |
|---|---|
| Ubicación aproximada / precisa | Recogida y compartida con el backend propio |
| Finalidad de la ubicación | Funcionalidad de la app (asignación y seguimiento) |
| Datos personales | Recogidos; finalidad: gestión de cuenta |
| Datos financieros | No se recogen |
| Datos cifrados en tránsito | **Pendiente**: hoy el backend es HTTP |
| Borrado de datos | A implementar (endpoint de baja o correo) |
| Publicidad / seguimiento | No |

---

## 5. Persistencia local

| Mecanismo | Archivo / BD | Contenido |
|---|---|---|
| **Room 2.6.1 (SQLite)** | `driverapp.db` | `orders_cache`, `order_items_cache`, `earnings_cache`, `earnings_history_cache`, `pending_actions` (+ `room_master_table`) |
| **DataStore Preferences** | `datastore/driverapp_prefs.preferences_pb` | Tema, idioma, sonido, ubicación en tiempo real, `location_asked` |
| **EncryptedSharedPreferences** | `shared_prefs/driverapp_secret.xml` | JWT y usuario, cifrados con Android Keystore |

**Detalles que importan:**

- **Esquema exportado** en `app/schemas/…AppDatabase/1.json` (13.674 bytes) → migraciones
  futuras verificables.
- `orders_cache` usa `bucket` (`AVAILABLE`/`ACTIVE`/`HISTORY`) y `updatedAt`. El sello
  representa la **última transición real de estado**, no el momento del refresco: así el mapa
  elige el pedido correcto (antes todos los pedidos quedaban empatados en la misma hora).
- **Cola offline** (`pending_actions` + `WorkManager`/`NetworkMonitor`): aceptar, salir a
  camino o completar sin conexión se guarda y se reintenta al recuperar red.
- `TokenDataStore` envuelve Keystore en `runCatching` con respaldo en memoria: un Keystore
  corrupto tras restaurar un backup no tumba la app, y **nunca** se escribe el token en claro.
- **Tests:** `OrderDaoTest` (162 líneas) corre contra SQLite **en memoria** con Robolectric:
  cubre inserción, reemplazo por bucket y orden por `updatedAt`.

---

## 6. REST

### 6.1 Endpoints que consume la app (20)

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `api/v1/auth/login` | Autenticación (correo + contraseña + token FCM) |
| `GET` | `api/v1/users/me` | Perfil |
| `PUT` | `api/v1/users/me` | Editar perfil |
| `PUT` | `api/v1/users/me/vehicle` | Vehículo |
| `PUT` | `api/v1/users/me/status` | Disponible / no disponible |
| `PUT` | `api/v1/users/me/location` | Reporte GPS |
| `PUT` | `api/v1/users/me/fcm-token` | Alta y rotación del token de push |
| `GET` | `api/v1/orders/available` | Pedidos sin asignar |
| `GET` | `api/v1/orders/my` | Pedidos del repartidor |
| `GET` | `api/v1/orders/history` | Historial |
| `GET` | `api/v1/orders/{id}` | Detalle |
| `POST` | `api/v1/orders/{id}/accept` | Aceptar |
| `POST` | `api/v1/orders/{id}/reject` | Rechazar |
| `POST` | `api/v1/orders/{id}/in-transit` | «Salí del restaurante» |
| `POST` | `api/v1/orders/{id}/complete` | Entregado |
| `GET` | `api/v1/earnings/summary` | Ganancias del periodo |
| `GET` | `api/v1/earnings/history` | Historial de ganancias |
| `GET` | `api/v1/notifications/my` | Notificaciones |
| `PUT` | `api/v1/notifications/my/read` | Marcar leídas |
| `GET` | `api/v1/restaurants` | Datos del restaurante |

### 6.2 Autenticación y errores

- **Autenticación 100 % backend:** `POST /auth/login` valida bcrypt (coste 12) y devuelve un
  **JWT HS256** de 7 días (`sub = users.id`). Firebase Auth **no** se usa para el login.
- Correo inexistente y contraseña incorrecta devuelven el **mismo `401`** (no se filtra qué
  falló).
- `ApiError` mapea `401/403/404/409/422/5xx`, errores de red y de timeout a mensajes localizados.
- `RetrofitClient` añade el token en un interceptor y lo renueva desde `TokenDataStore`.
- La cola offline se apoya en `ACCESS_NETWORK_STATE` para no encolar cuando ya no hay red.

**Verificación en vivo:** login correcto `200`, contraseña incorrecta `401`, correo inexistente
`401`, JWT manipulado `401`, token revocado `401`, contraseña corta `422`, pedido inexistente
`404`.

---

## 7. Firebase (solo notificaciones)

Firebase se usa **exclusivamente** para FCM: ni Auth, ni Firestore, ni Storage.

| Elemento | Valor |
|---|---|
| Proyecto | `proyectorepartidor-87835` (sender `1051770700715`) |
| Configuración en la app | Sin `google-services.json`: `FirebaseApp.initializeApp` con `FirebaseOptions` desde `BuildConfig` (`FIREBASE_API_KEY`, `APP_ID`, `PROJECT_ID`, `SENDER_ID`), leídos de `local.properties` |
| Servicio | `DriverFirebaseMessagingService`: arma título/cuerpo localizados (es/en), extrae `order_id`, crea un `PendingIntent` por pedido y usa `notify(orderId)` para no pisar avisos |
| Token | Se envía en el login; se registra al rotar (`onNewToken`) y al volver a primer plano |
| Canal | `orders` con `IMPORTANCE_HIGH`; permiso `POST_NOTIFICATIONS` comprobado antes de `notify()` |
| Backend | `firebase-service-account.json` (permisos 600) del **mismo** proyecto; `FIREBASE_CREDENTIALS_PATH` en `.env` |

### 7.1 Evidencia end-to-end

1. **Proyecto coherente:** el service account del backend y la configuración de la app
   declaran el mismo `project_id` (`proyectorepartidor-87835`).
2. **Token real en base de datos** para el usuario `repartidor1`.
3. **Envío directo aceptado** por FCM desde el servidor:

   ```
   OK entrega aceptada — message_id = projects/proyectorepartidor-87835/messages/0:1794696669713585%a7c0cc46a7c0cc46
   ```

4. **Circuito automático:** al crear un pedido de prueba (#40), el backend generó las
   notificaciones **51** (repartidor 1, con token) y **52** (repartidor 2, sin token) sin
   errores en el log.

---

## 8. Evidencias de verificación

| Comprobación | Comando / fuente | Resultado |
|---|---|---|
| Compilación | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| Tests unitarios | `./gradlew :app:testDebugUnitTest` | **56 tests, 0 fallos** |
| Lint | `./gradlew :app:lintDebug` | **0 errores** |
| Login backend | `POST /api/v1/auth/login` | 200 con JWT |
| Reporte GPS | `PUT /api/v1/users/me/location` | 200 y BD actualizada |
| Push FCM | `messaging.send()` con el service account | `message_id` devuelto |
| Notificación por pedido nuevo | `POST /api/v1/admin/orders` | notificaciones 51 y 52 creadas |
| Idiomas | Comparación `values/` vs `values-en/` | 180 cadenas, 0 sin traducir, 0 emojis |

---

## 9. Pendientes (no bloquean el funcionamiento)

| Pendiente | Motivo |
|---|---|
| **Capturas de pantalla** | `docs/capturas/` solo tiene el README; el entregable académico pide evidencias visuales |
| **TLS** | El backend es HTTP; Play exige cifrado en tránsito → dominio + certificado (Caddy/Let's Encrypt) delante de `:8084` |
| **Restricción de la key de Maps** | Debe limitarse a `com.driverapp.repartidor` + SHA-1 `47:92:21:36:78:BD:EC:8D:97:5A:53:68:D6:02:B6:04:FF:A6:87:78` (debug) |
| **Borrado de cuenta** | Endpoint de baja o formulario público para el formulario de Play |
| **Registro de aceptación de T&C** | Hoy la app muestra el texto; no guarda versión + fecha |

---

## 10. Repositorio y documentos relacionados

**Repositorio:** <https://github.com/AlejandroConde26/driverapp-android> (rama `main`)

| Documento | Contenido |
|---|---|
| `docs/01-requisitos.md` | Requisitos funcionales y no funcionales |
| `docs/03-sprints.md` | Sprints 1–6 y S08–S10 con criterios de aceptación |
| `docs/04-arquitectura.md` | Capas, MVVM, inyección de dependencias |
| `docs/07-modelo-datos-backend.md` | Esquema de la base de datos |
| `docs/08-informe-hu-login.md` | Historia de usuario de autenticación |
| `docs/10-correcciones-ui.md` | 18 correcciones con causa y solución |
| `docs/11-permisos-y-privacidad.md` | Versión extendida de permisos, TyC y Política |
| `docs/12-google-maps-y-fcm-setup.md` | Configuración de la key de Maps y de FCM |
| `docs/13-metodologia-desarrollo.md` | Metodología, EDT, cronograma, riesgos |
| `docs/14-geolocalizacion-permisos-privacidad.md` | Estado de los seis ejes (versión previa) |
| `docs/auth-backend.md` | Contrato de autenticación con el backend |
| `docs/S08`, `S09`, `S10` | API REST, CRUD/errores/offline y notificaciones |
| **`docs/15-informe-integrado.md`** | **Este documento** |
