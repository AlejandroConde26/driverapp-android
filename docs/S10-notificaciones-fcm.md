# S10 — Notificaciones push con Firebase Cloud Messaging

**Estado: cumplido.** Firebase entra en el proyecto **sólo** para notificaciones push.
La autenticación de usuarios es responsabilidad exclusiva del backend REST
(ver `docs/auth-backend.md`).

## Alcance decidido

| Pieza | ¿Se usa? | Motivo |
|---|---|---|
| Firebase Cloud Messaging (FCM) | **Sí** | Es el único servicio de Firebase que necesita la app: avisar de pedidos nuevos y cambios de estado. |
| Firebase Authentication | **No** | La identidad la emite el backend con correo + contraseña. Tener dos emisores de identidad rompía el login (el backend recibía un JWT de Firebase donde esperaba su propio usuario). |
| Cloud Firestore | **No** | La presencia del repartidor ya viaja por `PUT /users/me/location` y `/status`; un espejo en Firestore duplicaba la fuente de verdad. |
| Cloud Storage | **No** | El comprobante de entrega exigía el plan Blaze en proyectos nuevos y el backend no lo consume. Se retiró el botón en vez de dejar una función muerta. |

Un solo proveedor de identidad (el backend) y un solo uso de Firebase (push).

## Cómo quedó implementado en la app

| Archivo | Función |
|---|---|
| `data/firebase/FirebaseMessagingDataSource.kt` | Obtiene el token FCM del dispositivo. |
| `data/firebase/DriverFirebaseMessagingService.kt` | Recibe los mensajes, respeta las preferencias de notificaciones/sonido y muestra la notificación en el canal `orders`. |
| `data/firebase/FirebaseConfig.kt` | `isConfigured()` (api key + appId + sender id) y `senderId()`. |
| `App.kt` | Inicializa `FirebaseApp` con `FirebaseOptions` manuales (sin `google-services.json`) y **`setGcmSenderId`**. |
| `MainViewModel` → `PUT /users/me/fcm-token` | Registra el token en el backend para que éste pueda enviar el push. |

### El Sender ID es obligatorio

`FirebaseOptions` construido a mano no trae el número de proyecto, y **FCM no emite
token sin él** (`FirebaseMessaging.getInstance().token` queda colgado o falla).
Por eso `local.properties` incluye:

```properties
FIREBASE_SENDER_ID=1009763251368   # project_info.project_number de google-services.json
```

### Permisos y ciclo de vida

- `POST_NOTIFICATIONS` se pide en runtime (Android 13+) y se comprueba antes de
  llamar a `notify()`; sin la comprobación el servicio lanzaba `SecurityException`.
- El canal de notificación se crea sólo en API ≥ 26: `NotificationChannel` no
  existe en Android 7 y `minSdk` es 24.
- Sin credenciales Firebase la app arranca igual: `initFirebase()` sale con un log
  y no hay push. Nada más depende de Firebase.

## Lo que se retiró

| Archivo | Líneas | Qué hacía |
|---|---|---|
| `data/firebase/FirebaseAuthDataSource.kt` | 16 | `signInWithEmailAndPassword` contra Firebase Auth. |
| `data/firebase/FirestoreDriverDataSource.kt` | 79 | Espejo de presencia en `drivers/{email}`. |
| `data/firebase/StorageProofDataSource.kt` | 22 | Subida del comprobante a Storage. |
| `domain/repository/ProofRepository.kt` + `data/repository/ProofRepositoryImpl.kt` | 19 | Contrato e implementación del comprobante. |
| Dependencias `firebase-auth`, `firebase-firestore`, `firebase-storage` | — | Ya no se usan. |

También se quitó el botón «Adjuntar comprobante» de `TrackingScreen`, los estados
`proofUrl`/`uploadingProof` del `TrackingViewModel` y la llamada `syncPresence`
del `LocationForegroundService`.

## Aviso de pedido nuevo (extremo a extremo)

Cuando el panel crea un pedido (`POST /api/v1/admin/orders`), el backend ejecuta
`_notify_new_order()` y avisa a **todos los repartidores activos**:

| Pieza | Comportamiento |
|---|---|
| Payload | `data = {type: "new_order", order_id, client, screen: "orders"}` — **solo datos**, sin bloque `notification`, para que la app decida el texto y el destino del toque |
| Persistencia | Se crea una fila en `notifications` por repartidor: aparece también en la pestaña Notificaciones |
| Si hay token | `messaging.send()` por FCM (HTTP v1) con las credenciales del proyecto |
| Si no hay token | Se registra en el log y la notificación queda solo in-app |
| En la app | `DriverFirebaseMessagingService` arma el título con `notif_new_order_title` en el idioma del usuario; **una notificación por pedido** (id = `order_id`) |
| Al tocar | `PendingIntent` con `extra_order_id` → `MainActivity` (`singleTop`, recibe `onNewIntent`) → `AppNav` navega a Órdenes → `OrdersViewModel.openOrderById` abre el detalle (refresca de la API si el pedido no estaba en caché) |

Verificado: pedido #30 creado con `POST /admin/orders` → 200; el repartidor ve
«Nuevo pedido disponible — Pedido #30 · Prueba Push» en `/notifications/my`; el envío
FCM se intenta en el acto y solo falla (`SenderId mismatch`) mientras el teléfono
tenga instalado el APK del proyecto Firebase anterior.

## Rotación del token FCM (defecto corregido)

`onNewToken` solo guardaba el token en memoria: si FCM rotaba el token
(reinstalación, limpieza de datos, restauración), la base de datos se quedaba con
el anterior y el envío respondía **`NotRegistered`**. Ahora:

| Punto | Comportamiento |
|---|---|
| `DriverFirebaseMessagingService.onNewToken` | Además de guardarlo en memoria, lo registra en el backend con `PUT /users/me/fcm-token` (corrutina en IO, solo si hay sesión) |
| `MainActivity.onResume` | Autorreparación: pide el token actual a `FirebaseMessaging` y lo reenvía al backend si hay sesión |

Así el backend siempre tiene un token vivo, incluso si el usuario nunca vuelve a
iniciar sesión.

## Verificación

| Comprobación | Resultado |
|---|---|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | 56 tests, 0 fallos |
| `./gradlew :app:lintDebug` | 0 errores |
| Referencias residuales a `FirebaseAuthDataSource`, `Firestore`, `isStorageEnabled`, `id_token` | 0 en `app/src` |

## Pendiente (requiere acción fuera del repo)

1. **Entrega en el dispositivo**: el backend ya envía con credenciales del proyecto
   `proyectorepartidor-87835` (el mismo de la app) y `POST /notifications/send` devuelve
   200. Falta instalar el APK alineado para que el móvil registre un token de ese
   proyecto; el anterior pertenecía al proyecto viejo y respondía
   `SenderIdMismatchError` (se puso a `NULL` en la BD).
2. **Prueba en dispositivo**: el AVD de este equipo no es estable (memoria), así que la
   recepción real del push se valida en un teléfono físico.
