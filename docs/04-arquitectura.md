# DriverApp / SUMAQ — Arquitectura MVVM y modelo de datos local

Componentes: **app Android** (Kotlin MVVM), **backend FastAPI** (`:8084`),
**dashboard web-admin** (React, `:3000`). Este documento se enfoca en la app Android.

---

## 1. Arquitectura de la app — MVVM limpio

Paquete base: `com.driverapp.repartidor`.

```
app/src/main/java/com/driverapp/repartidor/
├── domain/                      # Kotlin puro (sin Android ni Retrofit)
│   ├── model/                   # Pedido, EstadoPedido (enum), Ubicacion, User,
│   │                            #   Ganancia, ResumenGanancias, Restaurante, Geo
│   ├── repository/              # Interfaces: AuthRepository, PedidoRepository,
│   │                            #   LocationRepository
│   └── usecase/                 # 15 casos: Login, ObtenerPedidosDisponibles,
│                                #   Aceptar/Rechazar/EnCamino/Entregado, Ganancias,
│                                #   Ubicación, Perfil, Vehículo, Estado en línea…
├── data/                        # Implementaciones
│   ├── remote/                  # DTOs (con toDomain()) + AuthApiService,
│   │                            #   PedidoApiService, RetrofitClient (Retrofit + OkHttp)
│   ├── repository/              # AuthRepositoryImpl, PedidoRepositoryImpl
│   ├── firebase/                # Auth, Messaging, service FCM, FirebaseConfig
│   ├── hardware/gps/            # LocationRepositoryImpl + LocationTracker
│   └── local/                   # TokenDataStore (cifrado), UserPreferences
├── di/                          # AppContainer (DI manual) + ViewModelFactory
└── ui/                          # Un ViewModel por pantalla (login, main, orders,
                                 #   tracking, earnings, profile) + common
                                 #   (UiMessenger, LoadingState/ErrorState)
```

**Flujo de datos:** `UI → ViewModel → UseCase → Repository → DTO → dominio`

- La UI usa **XML + Fragments con ViewBinding**; un `ViewModel` por pantalla.
- **DI manual** (sin Hilt): `AppContainer` construido en `App` expone las
  fábricas `ViewModelFactory` a los Fragments.
- El dominio no conoce Android ni Retrofit (Kotlin puro) → fácil de testear.

**Ciclo de un pedido**

```
PENDIENTE ──aceptar──► ACEPTADO ──en_camino──► EN_CAMINO ──entregado──► ENTREGADO
     └────────rechazar────────► RECHAZADO
```

El backend filtra por `restaurant_id` (la app solo ve SUMAQ, id=5).

---

## 2. Modelo de datos local

Dos piezas en `data/local/`, un fichero de preferencias y uno de sesión.

### 2.1 UserPreferences — fichero `driverapp_prefs`

Se accede vía `SharedPreferences` (una única instancia inyectada en
`ProfileViewModel` y `EarningsViewModel`).

| Clave | Propiedad | Tipo | Default | Uso |
|---|---|---|---|---|
| `dark` | `darkMode` | Boolean | false | Modo oscuro: `AppCompatDelegate.setDefaultNightMode()` |
| `notifications` | `notificationsEnabled` | Boolean | true | Si off, el service FCM no muestra nada |
| `location` | `locationEnabled` | Boolean | true | Arranca/para el loop GPS en `MainActivity` |
| `sound` | `notificationSound` | Boolean | true | Si off, notificación FCM silenciosa (`.setSilent`) |
| `show_balance` | `showBalance` | Boolean | true | Ojo en Ganancias: monto real vs `$••••••` |
| `language` | `language` | String (`es`/`en`) | `es` | Locales con `values/` y `values-en/` |

Contrato (esquema de la clase):

```kotlin
class UserPreferences(private val prefs: SharedPreferences) {
    var darkMode: Boolean          // clave "dark"
    var notificationsEnabled: Boolean  // clave "notifications"
    var locationEnabled: Boolean   // clave "location"
    var notificationSound: Boolean // clave "sound"
    var showBalance: Boolean       // clave "show_balance"
    var language: String           // clave "language" (es | en)
    companion object { const val LANG_ES = "es"; const val LANG_EN = "en" }
}
```

> El vehículo (`CARRO`/`BICICLETA`/`MOTO`) no se guarda en preferencias: persiste
> en el **backend** vía `updateVehicle` y la ETA lo usa al calcularse.

### 2.2 TokenDataStore — sesión cifrada

La sesión (token + usuario) se guarda **aparte** de las preferencias y **cifrada**
con `EncryptedSharedPreferences` (`MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)`).

```kotlin
class TokenDataStore(context: Context) {
    // token + sesión de usuario, cifrados en disco
    // se consume en ApiClient (Authorization: Bearer <token>)
}
```

Backend de los datos del repartidor: `users` (Postgres), custodiados por el backend
(en `backend/app/models/`). Local solo se persiste sesión y preferencias; el resto
de la información es remota.

---

## 3. Navegación y configuración

- Clave de aplicación: `RESTAURANT_ID=5` y `RESTAURANT_NAME="SUMAQ"` en
  `app/build.gradle.kts`; `API_BASE_URL=http://169.58.190.93:8084/`.
- Pestañas: `Login → Órdenes → Mapa → Ganancias / Perfil`.
- Firebase se inicializa vía `FirebaseOptions` (sin `google-services.json`); con
  `FIREBASE_*` vacías corre en **modo dev**.

## 4. Backend (referencia rápida)

- FastAPI + SQLAlchemy async + Postgres; schemas Pydantic en `schemas/`,
  routers en `routers/`, servicios en `services/` (`firebase.py`, `fcm.py`).
- Auth Bearer (JWT Firebase en modo real; token=UID en `DEV_MODE`).
- Contrato completo: `openapi.json` o `/docs` del backend.