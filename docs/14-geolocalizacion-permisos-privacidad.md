# Geolocalización, permisos, privacidad, persistencia, REST y Firebase

**Proyecto:** DriverApp — repartidores del restaurante SUMAQ
**Alcance:** estado real de los seis ejes solicitados, con la evidencia de cada uno.
Todo lo que dice "verificado" se comprobó sobre el código compilado, el APK o el
backend en producción, no sobre la intención del diseño.

---

## 1. Matriz de cumplimiento

| Requisito | Estado | Evidencia | Pendiente |
|---|---|---|---|
| Geolocalización con **Google Maps API** | ✅ Implementado | Manifest fusionado con `com.google.android.geo.API_KEY`; 632 referencias a `gms/maps`; 0 a osmdroid | Restringir la key en Cloud (package + SHA-1) y probar en teléfono con Play Services |
| **Permisos (todos)** | ✅ Declarados y justificados (documentación) | 7 permisos en el manifest, 8 descartados con motivo | Flujo de "denegado permanentemente → Ajustes" |
| **Privacidad / Términos y Condiciones** | 📄 Documentado (no implementado en la app, por decisión del cliente) | `docs/11` §3 y §4 (texto base Ley 29733) | Pantalla y registro de aceptación si se publica |
| **Persistencia local** | ✅ Implementado | Room (5 tablas + `room_master_table`), DataStore, cifrado, cola WorkManager | — |
| **REST** | ✅ Implementado | 33 rutas / 39 operaciones del backend, Retrofit, JWT, manejo de errores | — |
| **Firebase** | ✅ Configurado | FCM en la app (token, canal, servicio, permiso runtime); backend con service account del **mismo proyecto** (`proyectorepartidor-87835`) | Entrega al teléfono: falta instalar el APK nuevo (el token debe emitirse en el proyecto nuevo) |
| **Geolocalización del dispositivo** | ✅ Implementado | Servicio en primer plano tipo `location`, GPS + red, reporte cada 30 s | — |

---

## 2. Geolocalización con Google Maps API

### 2.1 Implementación

| Elemento | Detalle |
|---|---|
| SDK | `com.google.android.gms:play-services-maps:18.2.0` (sustituye a osmdroid) |
| Clave | `MAPS_API_KEY` en `local.properties` (ignorado por git) → `manifestPlaceholders` → `com.google.android.geo.API_KEY` |
| Vista | `MapView` dentro de `AndroidView`, con `getMapAsync` para obtener el `GoogleMap` |
| Marcadores | Restaurante (origen), cliente (destino) y repartidor (posición en vivo) |
| Mi ubicación | Capa nativa `isMyLocationEnabled` (punto azul en tiempo real) con permiso; marcador de respaldo si no lo hay. Sin pedido activo, la cámara se centra en el repartidor |
| Ruta | Botón «Cómo llegar en Google Maps»: `google.navigation:` → `geo:` → Maps web |
| Cámara | Centrada una sola vez (restaurante → repartidor → destino) y botón para volver a centrar en la posición propia |
| Ciclo de vida | `onCreate`, `onResume`, `onPause`, `onDestroy` conectados al `LifecycleOwner` de Compose |
| Degradación | Sin Google Play Services se muestra un aviso (`map_play_services_missing`) en lugar de un mapa vacío |

### 2.2 Requisito de configuración (responsabilidad del cliente)

La clave debe estar **restringida** en Google Cloud; si no, cualquier app puede usarla
y facturar contra la cuenta:

| Campo | Valor |
|---|---|
| Paquete | `com.driverapp.repartidor` |
| SHA-1 (debug) | `47:92:21:36:78:BD:EC:8D:97:5A:53:68:D6:02:B6:04:FF:A6:87:78` |
| SHA-256 (debug) | `5A:67:8C:16:1F:A1:24:6D:76:75:A6:B2:7F:CF:99:0D:84:41:88:AF:B2:CD:BF:AC:99:03:75:0C:D1:3F:D5:B9` |
| API a habilitar | *Maps SDK for Android* |
| Facturación | Cuenta activa (el SKU *Maps SDK* es de uso ilimitado y sin cargo) |

Al publicar con un keystore de release hay que añadir también **ese** SHA-1, o el mapa
dejará de cargar en el APK firmado.

### 2.3 Verificación ejecutada

```text
APK: referencias a org/osmdroid ......... 0
APK: referencias a com/google/android/gms/maps ... 632
Manifest fusionado .................... <meta-data name="com.google.android.geo.API_KEY" value="…btJI" />
build ................................. BUILD SUCCESSFUL · APK 17,0 MB
```

---

## 3. Permisos

### 3.1 Declarados (7)

| Permiso | Tipo | Propósito | Se pide |
|---|---|---|---|
| `INTERNET` | Normal | API REST y FCM | Instalación |
| `ACCESS_NETWORK_STATE` | Normal | Detectar conectividad para la cola offline | Instalación |
| `ACCESS_FINE_LOCATION` | Peligroso | Ubicar al repartidor y trazar la entrega | Al activar "Ubicación en tiempo real" |
| `ACCESS_COARSE_LOCATION` | Peligroso | Fallback de ubicación aproximada (Android 12+) | Junto con la fina |
| `POST_NOTIFICATIONS` | Peligroso (33+) | Avisos de pedido | Primer arranque / login |
| `FOREGROUND_SERVICE` | Normal | Mantener el reporte de ubicación | Instalación |
| `FOREGROUND_SERVICE_LOCATION` | Normal (34+) | Tipar el servicio como `location` | Instalación |

### 3.2 Evaluados y descartados (8, con motivo)

`ACCESS_BACKGROUND_LOCATION` (Play exige video de revisión y el servicio en primer
plano ya cubre el caso), `CAMERA` (se retiró con el comprobante), `USE_FULL_SCREEN_INTENT`
(restringido por Play), `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `VIBRATE`,
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `READ/WRITE_EXTERNAL_STORAGE`.

El detalle completo —con el comportamiento esperado en cada estado de concesión y el
mapeo al formulario *Data safety* de Play— está en **`docs/11-permisos-y-privacidad.md`**.

### 3.3 Estado del flujo

| Estado | Comportamiento actual |
|---|---|
| Concedido | Arranca el servicio en primer plano con notificación persistente |
| Denegado | Aviso al usuario y la preferencia vuelve a `false` |
| Revocado | El servicio se detiene y el aviso de ubicación desaparece del mapa |
| Denegado permanentemente | ⚠️ **Pendiente**: hoy solo se muestra el aviso, sin acceso directo a Ajustes |

---

## 4. Privacidad y Términos y Condiciones

**Decisión del cliente: solo documentación; la app no se modifica.**

| Entregable | Ubicación | Contenido |
|---|---|---|
| Inventario de datos tratados | `docs/11` §2 | Origen, finalidad, almacenamiento, retención y destinatario de cada dato |
| Base legal y derechos ARCO | `docs/11` §2.1 | Ley 29733 (Perú) |
| Términos y Condiciones (texto base, v1.0) | `docs/11` §3 | 8 cláusulas, incluida la de geolocalización y uso de datos del cliente |
| Política de Privacidad (texto base, v1.0) | `docs/11` §4 | Responsable, datos, finalidad, terceros, retención, seguridad, derechos |
| Mapeo Play *Data safety* | `docs/11` §5 | Ubicación recogida y compartida con el backend propio; sin publicidad |

### 4.1 Qué hace la app hoy con esos datos

| Medida | Implementación |
|---|---|
| Contraseña | Hash **bcrypt coste 12** en el servidor; nunca se devuelve ni se guarda en el móvil |
| Sesión | JWT HS256 firmado por el backend, 7 días, guardado **cifrado** (AES256-SIV/GCM) |
| Copias de seguridad | `android:allowBackup="false"` |
| Secretos | Fuera del repositorio: `local.properties` ignorado y verificado (0 claves en el remoto) |
| Ubicación | Se envía solo mientras el reparto está activo; el backend guarda la última posición, sin histórico de recorridos |

### 4.2 Riesgo abierto declarado

El backend responde por **HTTP** (`169.58.190.93:8084`). Para publicar en Play hace falta
TLS (dominio + certificado) porque el formulario de seguridad exige cifrado en tránsito.

---

## 5. Persistencia local

| Mecanismo | Uso | Detalle |
|---|---|---|
| **Room 2.6.1** (SQLite) | Caché de pedidos, ganancias, historial y cola de acciones | BD `driverapp.db`; tablas `orders_cache`, `order_items_cache`, `earnings_cache`, `earnings_history_cache`, `pending_actions`, `room_master_table`; esquema exportado en `app/schemas/…/1.json` |
| **DataStore** | Preferencias (modo oscuro, idioma, ubicación, notificaciones, saldo) | `datastore/driverapp_prefs.preferences_pb` |
| **EncryptedSharedPreferences** | Token de sesión y usuario | `shared_prefs/driverapp_secret.xml`; si el Keystore falla cae a memoria, nunca a disco en claro |
| **WorkManager** | Reintento de acciones pendientes al recuperar red | `OrderSyncWorker` |

**Verificación:** `OrderDaoTest` ejecuta consultas reales contra SQLite en memoria con
Robolectric (6 casos); `UserPreferencesTest` cubre el almacén de preferencias. El total
del proyecto es de **56 tests, 0 fallos**.

---

## 6. REST

| Elemento | Detalle |
|---|---|
| Backend | FastAPI, `http://169.58.190.93:8084`, **33 rutas / 39 operaciones / 28 esquemas** en `openapi.json` |
| Cliente | Retrofit + Gson (`AuthApiService`, `PedidoApiService`), DTOs con `@SerializedName` |
| Autenticación | `Authorization: Bearer <JWT>` que emite el propio backend |
| Errores | `ApiError` + `toErrorState()`: 401 sesión, 4xx validación, 5xx servidor, sin red, timeout |
| Offline | Cola de acciones en Room + reintento con WorkManager; contador visible en la UI |
| Verificación | 16 casos HTTP contra el servidor real: login 200/401, sesión 200/401, token manipulado 401, pedidos/ganancias/perfil 200, registro 201/409/422, cambio de contraseña 204/400 |

---

## 7. Firebase (notificaciones)

| Pieza | Estado |
|---|---|
| SDK | `firebase-messaging` (único componente de Firebase; Auth, Firestore y Storage se retiraron) |
| Inicialización | `FirebaseOptions` manual con **`setGcmSenderId`** (sin Sender ID no se emite token) |
| Proyecto | **`proyectorepartidor-87835`** — el mismo del service account del backend |
| Token | Se registra en el backend con `PUT /users/me/fcm-token` |
| Recepción | `DriverFirebaseMessagingService` con canal `orders`, respeta preferencias de aviso y sonido y comprueba `POST_NOTIFICATIONS` |
| Backend | Service account en `/root/driverapp/backend/firebase-service-account.json` (permisos 600) y `FIREBASE_CREDENTIALS_PATH` en el `.env` |
| Envío | `POST /api/v1/notifications/send` → **HTTP 200**, notificación persistida y `messaging.send()` con credenciales válidas |
| Pedido nuevo | Al crear un pedido (`POST /admin/orders`) el backend avisa a los repartidores activos con `{type: new_order, order_id, client}`; la app arma el texto en su idioma y la notificación abre el pedido (`extra_order_id` → Órdenes → detalle) |
| Pendiente | La entrega al teléfono se confirma al instalar el APK alineado: el token anterior era de otro proyecto y respondía `SenderIdMismatchError` (se puso a `NULL` en la BD) |

---

## 8. Geolocalización del dispositivo

| Elemento | Detalle |
|---|---|
| Servicio | `LocationForegroundService` con `foregroundServiceType="location"` |
| Fuente | `LocationManager` con `GPS_PROVIDER` y `NETWORK_PROVIDER` (10 s / 10 m), más última posición conocida |
| Reporte | Cada 30 s al backend (`PUT /users/me/location`) mientras hay seguimiento activo |
| Arranque | Solo desde primer plano (`MainActivity.onResume`) y con permiso concedido: en Android 12+ arrancarlo desde `Application.onCreate` lanza `ForegroundServiceStartNotAllowedException` |
| Parada | `stopService()` + `stopForeground(REMOVE)`; no se arranca el servicio "para pedirle que se pare" |
| Notificación | Canal `location`, prioridad baja, abre la app al pulsarla |
| Pantalla apagada | Sigue reportando sin `ACCESS_BACKGROUND_LOCATION`, por ser servicio en primer plano |

---

## 9. Pendientes que dependen del cliente

| # | Pendiente | Impacto | Quién |
|---|---|---|---|
| 1 | Restringir la API key de Google Maps (package + SHA-1) y habilitar *Maps SDK for Android* | Sin esto el mapa sale en blanco | Cliente (consola) |
| 2 | ~~Alinear el proyecto de Firebase con la app~~ | **Resuelto**: app y service account en `proyectorepartidor-87835`; falta instalar el APK alineado para confirmar la entrega | Cliente (instalar APK) |
| 3 | TLS para el backend | Requisito para publicar en Play | Cliente / infraestructura |
| 4 | Pantalla de Términos y registro de aceptación | Hoy es solo documentación | Cliente (decisión) |
| 5 | Acceso a Ajustes cuando el permiso se deniega para siempre | UX de permisos | Desarrollo |
| 6 | Capturas de pantalla del APK en un teléfono real | Evidencia de funcionamiento | Cliente (dispositivo) |
