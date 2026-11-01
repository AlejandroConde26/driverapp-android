# S10 — Integración con Firebase: autenticación, Firestore y Storage

**Estado: implementado, con una verificación pendiente de consola.**

- **Auth, Firestore y Storage tienen código funcional y compilan.**
- **La prueba en runtime no se pudo completar** por una condición del proyecto
  Firebase facilitado (ver §5): el `appId` corresponde a otro `package_name`.
- La app **no depende** de Firebase para funcionar: sin credenciales arranca en modo
  dev y usa sólo el backend REST.

---

## 1. Configuración sin secretos en el repo

`docs/02` exige que no se versioneen credenciales. Por eso:

| Clave | Origen | BuildConfig |
|---|---|---|
| `FIREBASE_API_KEY` | `local.properties` (gitignored) | `app/build.gradle.kts:38` |
| `FIREBASE_APP_ID` | idem | `:39` |
| `FIREBASE_PROJECT_ID` | idem | `:40` |
| `FIREBASE_STORAGE_BUCKET` | idem | `:41` |

Si faltan, los valores llegan vacíos y `FirebaseConfig.isConfigured()` devuelve
`false` → toda la capa Firebase queda en no-op (`data/firebase/FirebaseConfig.kt`).

Inicialización manual con `FirebaseOptions` en `App.initFirebase()`: no se usa
`google-services.json`, así el build no falla en máquinas sin el fichero.

## 2. Autenticación

| Aspecto | Implementación |
|---|---|
| Login con Firebase | `FirebaseAuthDataSource.signIn()` → `signInWithEmailAndPassword` → idToken |
| Login sin Firebase | modo dev: el backend usa la parte local del correo como UID (`LoginUseCase`) |
| Sesión local | `TokenDataStore` con `EncryptedSharedPreferences` (AES256-SIV/GCM) |
| Registro del token de push | `PUT /users/me/fcm-token` al entrar (`AppNav.ensureFcmToken`) |
| Push | `DriverFirebaseMessagingService` respeta `notifications` y `sound` (DataStore) |

El login **no** crea usuarios: el backend hace upsert por `firebase_uid` al recibir
el `id_token`, así que un usuario nuevo en Firebase queda registrado en Postgres en
su primer login.

## 3. Firestore — presencia del repartidor

`data/firebase/FirestoreDriverDataSource.kt:33`

```
drivers/{email}
   ├── online:    Boolean
   ├── lat, lng:  Double
   └── updatedAt: serverTimestamp
   └── deliveries/{orderId}
          ├── proofUrl: String
          └── updatedAt: serverTimestamp
```

- Se escribe desde el servicio de ubicación **después** de cada reporte REST
  (`LocationForegroundService`), como espejo para que el dashboard pueda escuchar
  cambios en tiempo real sin hacer polling.
- El `Task` de Play Services se convierte a corrutina con `awaitResult()`
  (`FirestoreDriverDataSource.kt:13`), sin añadir `kotlinx-coroutines-play-services`.
- Todo va dentro de `runCatching`: si Firestore falla, el reporte REST ya se envió y
  la entrega no se interrumpe.

## 4. Storage — comprobante de entrega

`data/firebase/StorageProofDataSource.kt:16`

- Flujo: botón **"Adjuntar comprobante"** en la pantalla Mapa → cámara
  (`ActivityResultContracts.TakePicture`) → `FileProvider` → `deliveries/{orderId}.jpg`
  → `downloadUrl` → se registra en Firestore (`ProofRepositoryImpl`).
- `ProofRepository` (dominio) mantiene la capa limpia: el ViewModel no conoce Firebase.
- Sin Storage configurado lanza un error explícito y la UI muestra
  "No se pudo subir el comprobante" en vez de fallar en silencio.

Permisos: `<provider androidx.core.content.FileProvider>` con
`res/xml/file_paths.xml` (`cache-path proofs/`).

## 5. Verificación

| Punto | Estado | Evidencia |
|---|---|---|
| Código compila y se empaqueta | ✅ | `./gradlew :app:assembleDebug` → APK 19,5 MB |
| Degradación sin credenciales | ✅ | `FirebaseConfig.isConfigured()` = `false` → no-op; la app opera con REST |
| Claves fuera del repo | ✅ | `local.properties` está en `.gitignore` (`git check-ignore -v local.properties`) |
| Auth en runtime | ⚠️ **pendiente** | el `appId` del proyecto es de `tv.cdlatam.setimer`, no de `com.driverapp.repartidor` |
| Firestore en runtime | ⚠️ **pendiente** | depende del punto anterior + que Firestore esté habilitado en el proyecto |
| Storage en runtime | ⚠️ **pendiente** | idem + reglas de seguridad que permitan escritura al usuario autenticado |

### Por qué no se pudo cerrar el runtime

El `mobilesdk_app_id` de Firebase está atado a un `package_name`. El facilitado
(`1:1009763251368:android:22b0609fded44bd8453e9d`) pertenece a **`tv.cdlatam.setimer`**.
Desde `com.driverapp.repartidor` Google puede responder
`API_KEY_ANDROID_PACKAGE_BLOCKED` / `auth/invalid-api-key`. `FirebaseConfig.packageHint()`
deja ese aviso en el código.

### Pasos para cerrarlo (5 minutos, en la consola)

1. Firebase Console → proyecto `camarasproyectocdlatam` → *Add app* → Android →
   package `com.driverapp.repartidor` → registrar.
2. Copiar el nuevo `mobilesdk_app_id` a `FIREBASE_APP_ID` en `local.properties`.
3. Firestore → *Create database* (modo producción) y reglas:
   `match /drivers/{doc} { allow read, write: if request.auth != null; }`.
4. Storage → *Get started* y reglas equivalentes para `deliveries/`.
5. `./gradlew :app:assembleDebug` y probar login + comprobante en un dispositivo.

> Mientras eso no se haga, S10 queda como **"implementado, verificación pendiente"** y
> no como cumplido: el criterio es no marcar `[x]` sin evidencia de ejecución.
