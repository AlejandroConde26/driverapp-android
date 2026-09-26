# Yallego Repartidor — App Android (Kotlin)

App móvil del **repartidor** Yallego (entregas a domicilio de restaurantes).
El dashboard recibe pedidos con platillos del catálogo y el repartidor los
entrega con esta app. Parte de un sistema de 3 componentes: backend FastAPI
(`:8084`), dashboard web-admin (`:3000`) y esta app.

## Resumen

La app permite al repartidor:

- **Iniciar sesión** con correo y contraseña (Firebase Auth, o modo dev sin Firebase).
- **Ver pedidos disponibles**, aceptarlos o rechazarlos (pestaña *Órdenes*).
- **Seguir la entrega en el mapa** (osmdroid) con ETA y distancia en vivo (pestaña *Mapa*).
- **Ver ganancias** por día/semana/mes/año con gráficas (pestaña *Ganancias*).
- **Gestionar su perfil**: datos, vehículo, contraseña y 5 preferencias (pestaña *Perfil*).

## Cómo funciona

```
Login ──► Órdenes ──► Mapa ──► Ganancias / Perfil
            │            │
      aceptar/rechazar   │
            │            ▼
            └──► PENDIENTE → ACEPTADO → EN_CAMINO → ENTREGADO
```

1. **Login** (`ui/login`): si Firebase está configurado usa
   `signInWithEmailAndPassword`; si no, modo dev (el backend usa la parte
   local del correo como UID). El token se guarda cifrado.
2. **Órdenes** (`ui/orders`): lista *Asignados* (disponibles) e
   *Historial*, con detalle inline que muestra cada platillo
   (`cantidad × nombre — descripción` + notas), diálogos de confirmación y
   reintento ante sin-conexión.
3. **Mapa** (`ui/tracking`): marcadores de restaurante, cliente y posición GPS;
   ETA/distancia calculados con `Geo` + velocidad del vehículo o del GPS.
   Al completar, se refrescan historial y ganancias.
4. **GPS en segundo plano**: `MainActivity` reporta la ubicación al backend
   cada 30 s (`PUT users/me/location`) mientras el tracking está activo.
5. **Notificaciones FCM**: canal `orders`; el token se registra en el backend
   (`PUT users/me/fcm-token`). Respetan las preferencias del usuario.
6. **Perfil** (`ui/profile`): editar datos, vehículo, ver estadísticas anuales
   y cerrar sesión.

## Arquitectura — MVVM limpio

```
app/src/main/java/com/driverapp/repartidor/
├── domain/        # Kotlin puro (sin Android ni Retrofit)
│   ├── model/     # Pedido, EstadoPedido (enum), Ubicacion, User,
│   │              #   Ganancia, ResumenGanancias, Restaurante, Geo
│   ├── repository/# Interfaces: AuthRepository, PedidoRepository,
│   │              #   LocationRepository
│   └── usecase/   # 15 casos: Login, ObtenerPedidosDisponibles,
│                  #   Aceptar/Rechazar/EnCamino/Entregado, Ganancias,
│                  #   Ubicación, Perfil, Vehículo, Estado en línea…
├── data/          # Implementaciones
│   ├── remote/    # DTOs (con toDomain()) + AuthApiService,
│   │              #   PedidoApiService, RetrofitClient (Retrofit + OkHttp)
│   ├── repository/# AuthRepositoryImpl, PedidoRepositoryImpl
│   ├── firebase/  # Auth, Messaging, service FCM, FirebaseConfig
│   ├── hardware/gps/ # LocationRepositoryImpl + LocationTracker
│   └── local/     # TokenDataStore (cifrado), UserPreferences
├── di/            # AppContainer (DI manual) + ViewModelFactory
└── ui/            # Un ViewModel por pantalla (login, main, orders,
                   #   tracking, earnings, profile) + common
                   #   (UiMessenger, LoadingState/ErrorState)
```

Flujo de datos: `UI → ViewModel → UseCase → Repository → DTO → dominio`.
La UI usa XML + Fragments con ViewBinding. Sin Hilt (DI manual vía
`AppContainer`, expuesto en `App`).

## Las 5 preferencias de usuario

Centralizadas en `data/local/UserPreferences` (fichero único
`driverapp_prefs`), inyectadas en `ProfileViewModel`. Rutas verificadas:

| # | Preferencia | Ruta | Funcionamiento y relación |
|---|---|---|---|
| 1 | Modo oscuro | `data/local/UserPreferences.kt` (clave `dark`) · `ui/profile/ProfileFragment.kt` (switch) | Persiste el Boolean; el Fragment aplica `AppCompatDelegate.setDefaultNightMode()`. Conmuta a `values-night/colors.xml`. |
| 2 | Notificaciones | `UserPreferences.kt` (clave `notifications`) · Efecto: `data/firebase/DriverFirebaseMessagingService.kt` | El service FCM no muestra nada si está off. Relaciona Perfil → notificaciones push. |
| 3 | Ubicación en tiempo real | `UserPreferences.kt` (clave `location`) · `ui/main/MainActivity.kt` | Arranca/para el GPS (`LocationRepository`); el loop cada 10 s reporta al backend (`PUT users/me/location`). Relaciona Perfil → GPS → backend. |
| 4 | Sonido de avisos | `UserPreferences.kt` (clave `sound`) · Efecto: `DriverFirebaseMessagingService.kt` (`.setSilent(...)`) | La notificación FCM llega en silencio si está off. |
| 5 | Idioma (es/en) | `UserPreferences.kt` (clave `language`) · Textos: `res/values/` y `res/values-en/` · Aplicación: `App.kt` | Se aplica con `AppCompatDelegate.setApplicationLocales()` al arrancar y con `recreate()` al cambiar. Bottom-nav, cabeceras y Preferencias cambian de idioma. |

La sesión (token + usuario) se guarda aparte y **cifrada** en
`data/local/TokenDataStore.kt` (`EncryptedSharedPreferences`), acorde al
tipo de app (delivery con login).

## Abrir en Android Studio

1. Clona el repo: `git clone https://github.com/AlejandroConde26/driverapp-android.git`
2. Android Studio → **File > Open** → selecciona la carpeta clonada.
3. Espera el sync de Gradle (descarga dependencias con internet).
4. `local.properties` (ruta del SDK) se genera solo; no está en el repo.

Requisitos: JDK 17, Android SDK Platform 34, Gradle 8.6 + AGP 8.4.0
(el wrapper `gradlew` ya viene incluido).

```bash
./gradlew :app:assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 15 tests unitarios (MockK + Turbine)
```

## Configuración

| Clave | Dónde | Valor actual |
|---|---|---|
| `API_BASE_URL` | `app/build.gradle.kts` | `http://169.58.190.93:8084/` (backend) |
| `FIREBASE_*` | `app/build.gradle.kts` | vacías = modo dev (sin `google-services.json`) |

- **Emulador apuntando a tu PC**: `http://10.0.2.2:8084/`.
- **Firebase real**: rellena `FIREBASE_API_KEY`, `FIREBASE_APP_ID`,
  `FIREBASE_PROJECT_ID` (la app inicializa `FirebaseOptions` sin
  `google-services.json`).
- **Backend y dashboard**: ver estado en el proyecto principal
  (backend `:8084` + `/docs`, web-admin `:3000`).
- **Credenciales dev** (contraseña libre): `admin@driverapp.com`,
  `repartidor1@driverapp.com`, `repartidor2@driverapp.com`.

## Tests

`app/src/test/` — JUnit + MockK + Turbine + coroutines-test:

- `domain/usecase/`: `LoginUseCaseTest`, `AceptarPedidoUseCaseTest`,
  `MarcarPedidoEntregadoUseCaseTest`
- `ui/login/LoginViewModelTest`, `ui/profile/ProfileViewModelTest`
- `data/`: `PedidoRepositoryImplTest`, `UserPreferencesTest`
