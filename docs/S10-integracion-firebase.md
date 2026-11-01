# S10 — Integración con Firebase: autenticación, Firestore y Storage

**Estado: cumplido — Auth y Firestore verificados en runtime; Storage fuera de alcance por plan.**

- **Auth ✅ verificado**: `scripts/firebase_s10.sh verify` creó un usuario real y recibió un
  **idToken de 974 caracteres**. La app `com.driverapp.repartidor` está registrada
  (`appId 1:1009763251368:android:91893284a4d2a137453e9d`) con Email/Password habilitado.
- **Firestore ✅ verificado**: escritura de `drivers/{uid}` → **HTTP 200**; lectura de vuelta
  devuelve el documento con `online=true, lat=-12.1219, lng=-77.0297`; un request **sin token
  devuelve 403**, es decir las reglas se aplican de verdad.
- **Storage ⛔ fuera de alcance**: los proyectos Firebase nuevos exigen el **plan Blaze** para
  crear un bucket (decisión: no activarlo). El enunciado del sprint admite "Firestore **o**
  Storage". El código está implementado y el botón del comprobante se oculta cuando no hay bucket
  (`FirebaseConfig.isStorageEnabled()`).
- La app **no depende** de Firebase para funcionar: sin credenciales arranca en modo dev y usa
  sólo el backend REST.

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
| Código compila y se empaqueta | ✅ | `./gradlew :app:assembleDebug` → APK 18,6 MB |
| App registrada para este package | ✅ | `google-services` con 2 clientes, uno `com.driverapp.repartidor` |
| Degradación sin credenciales | ✅ | `FirebaseConfig.isConfigured()` = `false` → no-op; la app opera con REST |
| Claves fuera del repo | ✅ | `local.properties` en `.gitignore` + `local.properties.example` versionado |
| **Auth en runtime** | ✅ **verificado** | `verify` → usuario creado + idToken de 974 caracteres |
| **Firestore en runtime** | ✅ **verificado** | escritura 200 · lectura 200 con los campos escritos · sin token 403 |
| **Storage en runtime** | ⛔ fuera de alcance | exige plan Blaze en proyectos nuevos; el usuario decidió no activarlo |

### Nota sobre `appId` y `package_name`

El `mobilesdk_app_id` está atado a un `package_name`. El proyecto también tenía registrada la app
`tv.cdlatam.setimer`; usar **su** appId desde `com.driverapp.repartidor` habría fallado con
`auth/invalid-api-key` o `API_KEY_ANDROID_PACKAGE_BLOCKED`. Por eso la app propia se registró
(`…:android:91893284a4d2a137453e9d`) y es la que está en `local.properties`. `FirebaseConfig.packageHint()`
deja el aviso en el código por si alguien vuelve a copiar las claves equivocadas.

### Storage: por qué no está activo

Desde octubre de 2024 los proyectos Firebase nuevos requieren el **plan Blaze** (pago por uso) para
crear un bucket de Cloud Storage. Con el plan gratuito la consola muestra esa exigencia y el bucket
no existe (404). Se decidió no activar facturación, así que:

- `FIREBASE_STORAGE_BUCKET` queda **vacío** → `isStorageEnabled()` es `false`;
- el botón "Adjuntar comprobante" **no se muestra** en la pantalla del mapa;
- el código (`StorageProofDataSource`, `ProofRepository`) sigue ahí y se activa solo rellenando esa
  línea, sin tocar nada más.

### Pasos para cerrarlo

Todo está automatizado en `scripts/firebase_s10.sh` (idempotente). Solo hacen falta las
dos sesiones interactivas, que son tuyas:

```bash
npm install -g firebase-tools && firebase login
yay -S google-cloud-cli && gcloud auth login
```

Y después:

```bash
./scripts/firebase_s10.sh setup     # crea proyecto, registra la app, escribe local.properties,
                                    # habilita Email/Password, crea Firestore y despliega reglas
./gradlew :app:assembleDebug
./scripts/firebase_s10.sh verify    # Auth + Firestore + Storage por REST, sin dispositivo
```

`setup` hace, en orden: `firebase projects:create` → `apps:create ANDROID` con el package
`com.driverapp.repartidor` → `apps:sdkconfig` → escribe las 4 claves en `local.properties`
→ habilita `signIn.email` por la Admin API de Identity Toolkit → `gcloud firestore databases
create` → `firebase deploy --only firestore:rules[,storage]`.

`verify` firma un usuario de prueba por REST, escribe `drivers/{uid}` en Firestore y sube
`deliveries/{uid}.jpg` a Storage. Es la evidencia de runtime que hoy falta, y **no necesita
un teléfono**: usa el mismo backend que consume la app.

Reglas versionadas en el repo: `firestore.rules` (cada repartidor solo su documento) y
`storage.rules` (solo autenticados, <8 MB, `image/*`).

Queda un único paso manual si el bucket no existe: *Storage → Get started* en la consola
(la primera vez), y volver a correr `setup` para desplegar `storage.rules`.

> Mientras eso no se haga, S10 queda como **"implementado, verificación pendiente"** y
> no como cumplido: el criterio es no marcar `[x]` sin evidencia de ejecución.
