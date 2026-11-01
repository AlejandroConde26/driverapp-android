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

## Verificación

| Comprobación | Resultado |
|---|---|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | 52 tests, 0 fallos |
| `./gradlew :app:lintDebug` | 0 errores |
| Referencias residuales a `FirebaseAuthDataSource`, `Firestore`, `isStorageEnabled`, `id_token` | 0 en `app/src` |

## Pendiente (requiere acción fuera del repo)

1. **Backend**: `/api/v1/notifications/send` tiene que enviar de verdad por FCM
   HTTP v1 con una service account (`FIREBASE_CREDENTIALS_PATH` está vacío en el
   `.env` del servidor, así que hoy el push se registra y no sale). Mientras no
   haya credenciales, `services/fcm.py` sólo escribe un log.
2. **Prueba en dispositivo**: el AVD de este equipo no es estable (memoria), así
   que la recepción real del push se valida en un teléfono físico.
