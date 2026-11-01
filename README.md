# SUMAQ Repartidor — App Android (Kotlin + Jetpack Compose)

App móvil del **repartidor del restaurante SUMAQ**. Sistema de 3 componentes:
backend FastAPI (`:8084`), dashboard web-admin (`:3000`) y esta app.

## Resumen

- **Iniciar sesión** con correo y contraseña (Firebase Auth, o modo dev sin Firebase).
- **Ver pedidos disponibles**, aceptarlos o rechazarlos (pestaña *Órdenes*).
- **Seguir la entrega en el mapa** (osmdroid) con ETA y distancia en vivo, y marcar
  *EN_CAMINO* al salir del restaurante.
- **Ver ganancias** por día/semana/mes/año con gráficas.
- **Gestionar perfil y 5 preferencias** (modo oscuro, notificaciones, ubicación, sonido,
  idioma es/en).
- **Funcionar sin conexión** con la caché local y reenviar los cambios al reconectar.

## Arquitectura

```
app/src/main/java/com/driverapp/repartidor/
├── domain/          Kotlin puro (0 imports de android/retrofit)
│   ├── model/       Pedido, EstadoPedido, Ubicacion, User, Ganancia, Notificacion, Geo
│   ├── repository/  interfaces: Auth, Pedido, Earnings, Location, Notification, Proof
│   └── usecase/     18 casos de uso
├── data/
│   ├── remote/      Retrofit + DTOs con toDomain() + ApiError (errores tipados)
│   ├── local/       DataStore (UserPreferences), TokenDataStore, Room (db/, queue/)
│   ├── network/     NetworkMonitor (conectividad pasiva)
│   ├── firebase/    Auth, Messaging, Firestore (presencia), Storage (comprobante)
│   └── repository/  implementaciones cache-first
├── di/              AppContainer (DI manual) + ViewModelFactory
└── ui/              Compose: theme/, components/, y una pantalla por pestaña + AppNav
```

Flujo: `UI (Compose) → ViewModel → UseCase → Repository (cache-first) → Room/Retrofit`.

| Métrica | Valor |
|---|---|
| Kotlin producción | 73 archivos · 6.139 líneas |
| Kotlin test | 14 archivos · 1.156 líneas · **51 tests** |
| Layouts XML | **0** (2.922 líneas migradas a Compose) |
| Capas | domain 18 · data 29 · di 2 · ui 23 archivos |

## Puesta en marcha

Requisitos: JDK 17, Android SDK 34, Gradle 8.6 (wrapper incluido).

```bash
git clone <repo> && cd driverapp-android-main
# local.properties: sdk.dir, y opcionalmente las claves FIREBASE_* (ver docs/S10)
./gradlew :app:assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # 51 tests unitarios (MockK + Turbine + Robolectric)
```

## Configuración

| Clave | Dónde | Valor |
|---|---|---|
| `API_BASE_URL` | `app/build.gradle.kts` | `http://169.58.190.93:8084/` |
| `RESTAURANT_ID` / `RESTAURANT_NAME` | `app/build.gradle.kts` | `5` / `SUMAQ` |
| `FIREBASE_*` | **`local.properties`** (no versionado) | vacías = modo dev |

- Emulador apuntando a tu PC: `http://10.0.2.2:8084/`.
- El tráfico en claro está permitido **sólo** para la IP del backend
  (`res/xml/network_security_config.xml`).
- Credenciales dev (contraseña libre): `admin@driverapp.com`,
  `repartidor1@driverapp.com`, `repartidor2@driverapp.com`.

## Documentación

| Documento | Contenido |
|---|---|
| `docs/01-requisitos.md` | Historias de usuario y criterios de aceptación |
| `docs/02-requerimientos-no-funcionales.md` | Rendimiento, seguridad, compatibilidad |
| `docs/03-sprints.md` | **9 sprints con estado real y evidencia** |
| `docs/04-arquitectura.md` | Arquitectura MVVM y modelo de datos local |
| `docs/05-sprint-interfaz-navegacion.md` | Migración a Jetpack Compose |
| `docs/06-sprint-persistencia-local.md` | DataStore, Room y Repository cache-first |
| `docs/07-modelo-datos-backend.md` | API y modelo del backend |
| `docs/08-informe-hu-login.md` | Informe de la HU-01 |
| `docs/S08-integracion-api-rest-retrofit.md` | Endpoints, contrato JSON y Retrofit |
| `docs/S09-crud-errores-estados-conectividad.md` | CRUD, errores, estados y cola offline |
| `docs/S10-integracion-firebase.md` | Auth, Firestore y Storage |

## Estado

- Suite: **51 tests, 0 fallos** · `lintDebug`: **0 errores**.
- Sprints 1–6 y S08–S10 **cumplidos y verificados**; detalle con evidencia en
  `docs/03-sprints.md`.
- S10: **Auth y Firestore verificados en runtime** (idToken real, escritura 200, lectura 200,
  sin token 403). **Storage fuera de alcance**: los proyectos Firebase nuevos exigen plan Blaze;
  el código queda listo y desactivado por configuración.
