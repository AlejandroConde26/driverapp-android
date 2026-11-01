# DriverApp / SUMAQ — Planificación de sprints (9 sprints)

App Android del repartidor (Kotlin + Compose), backend FastAPI (`:8084`) y dashboard
web-admin (`:3000`). Las historias viven en `docs/01-requisitos.md`.

> **Regla de este documento:** ningún sprint se marca `[x]` sin evidencia ejecutable
> (`archivo:línea`, comando y test). Los que no la tienen se marcan `[ ]` y se explica
> exactamente qué falta.

Estado del pedido: `PENDIENTE → ACEPTADO → EN_CAMINO → ENTREGADO` (o `RECHAZADO`).

## Resumen

| # | Sprint | Estado | Evidencia principal |
|---|---|---|---|
| 1 | Base: login, sesión y perfil | ✅ | `LoginViewModelTest` (6), `ProfileViewModelTest` (5) |
| 2 | Órdenes y flujo de entrega | ✅ | `TrackingViewModelTest` cubre `ACEPTADO→EN_CAMINO→ENTREGADO` |
| 3 | Mapa, GPS y notificaciones | ✅ ¹ | `LocationForegroundService` + `TrackingScreen` |
| 4 | Ganancias, preferencias y pulido | ✅ | `EarningsViewModelTest`, `UserPreferencesTest` |
| 5 | Interfaz y navegación en Jetpack Compose | ✅ | 0 layouts XML, `NavHost` en `ui/AppNav.kt` |
| 6 | Persistencia local: DataStore, Room, Repository | ✅ | `OrderDaoTest` (6), `app/schemas/…/1.json` |
| 7 | **S08** Integración con API REST y Retrofit | ✅ | 20/20 endpoints del `openapi.json`, `ApiErrorMapperTest` |
| 8 | **S09** CRUD, errores, estados y conectividad | ✅ | cola offline + `OrdersViewModelTest` (6) |
| 9 | **S10** Firebase: Auth, Firestore, Storage | ⚠️ ² | código + degradación sin credenciales |

¹ El push FCM sólo funciona con un proyecto Firebase válido (ver sprint 9).
² Implementado y compilado; la prueba en runtime queda pendiente de registrar el
package en la consola de Firebase (`docs/S10-integracion-firebase.md` §5).

Resultado de la suite: **51 tests, 0 fallos** (`./gradlew :app:testDebugUnitTest`).

---

## Sprint 1 — Base: login, sesión y perfil ✅

**Objetivo:** repartidor autenticado y con perfil usable + backend/auth listos.

- [x] **HU-01** Login con correo y contraseña. Modo dev: el backend usa la parte local
  del correo como UID (`LoginUseCase`); con Firebase viaja el JWT
  (`FirebaseAuthDataSource.signIn`).
- [x] Token de sesión **cifrado** con `EncryptedSharedPreferences` (AES256-SIV/GCM) y
  `Authorization: Bearer` en cada request (`RetrofitClient.kt:19`).
- [x] Error de login claro sin cerrar la app: `LoginState.Error` + `ApiFailure` tipado.
- [x] Ojo mostrar/ocultar contraseña: `PasswordField` (`ui/components/Fields.kt`).
- [x] **HU-02** Perfil: nombre, teléfono, vehículo. El desplegable ofrece exactamente
  los valores que acepta el backend —`Bicicleta | Moto | Auto`— (`ProfileViewModel.VEHICLES`),
  corrigiendo el `"Carro"` que devolvía 422.
- [x] El vehículo afecta la ETA (`Geo.vehicleSpeedKmh`).
- [x] Estadísticas del perfil con datos reales (`/earnings/summary?period=year`);
  se eliminó el rating fijo `4.8` que no tenía fuente.
- [x] Cerrar sesión limpia token, usuario y detiene el servicio de ubicación.

**Definición de terminado:** tests de `LoginUseCase`, `LoginViewModel` y
`ProfileViewModel` en verde → 6 + 5 + 3 tests.

**Cambios de saneamiento incluidos:** se quitó el diálogo "Cambiar contraseña" que sólo
mostraba un toast sin llamar a ningún endpoint (el backend no expone esa operación).

---

## Sprint 2 — Órdenes y flujo de entrega ✅

**Objetivo:** el repartidor acepta/rechaza pedidos y el flujo de estados opera de punta
a punta.

- [x] **HU-03** Lista *Asignados* / *Historial* con filtro `restaurant_id=5`.
- [x] Detalle con cliente, dirección, total, comisión y `cantidad × nombre — descripción`
  + notas (`ui/components/OrderCard.kt`).
- [x] Aceptar → `ACEPTADO`; rechazar → `RECHAZADO` (sale de disponibles).
- [x] Sin red: mensaje de sin conexión + reintento (`ErrorStateBox`).
- [x] **Cierre del hueco crítico:** `EN_CAMINO` se marca con el botón
  *"Salí del restaurante"* (`TrackingViewModel.marcarEnCamino` → `POST /orders/{id}/in-transit`).
  Antes el caso de uso existía pero **nadie lo llamaba**, y el pedido pasaba de
  `ACEPTADO` a `ENTREGADO` sin pasar por el estado intermedio.

**Definición de terminado:** `PedidoRepositoryImplTest` (8) verifica que `accept`,
`markInTransit` y `complete` llaman al endpoint correcto y escriben en el bucket
correcto; `TrackingViewModelTest` verifica el callback con el estado `EN_CAMINO`.

---

## Sprint 3 — Mapa, GPS y notificaciones ✅ (push pendiente de credenciales)

- [x] **HU-04** osmdroid con marcador de restaurante, cliente y posición propia
  (`ui/tracking/TrackingScreen.kt`, `AndroidView`).
- [x] Distancia y ETA en vivo; el destino cambia según el estado: restaurante si
  `ACEPTADO`, cliente si `EN_CAMINO`.
- [x] **GPS en segundo plano real:** `LocationForegroundService`
  (`foregroundServiceType="location"`, notificación persistente, reporte cada 30 s).
  Antes el loop vivía en `MainActivity.lifecycleScope` y moría con la Activity.
- [x] El servicio arranca solo al abrir la app si la preferencia está activa
  (`App.onCreate`), en lugar de exigir tocar el switch cada vez.
- [x] **HU-07** canal `orders`, servicio FCM que respeta `notifications` y `sound`,
  registro del token con `PUT /users/me/fcm-token`.

**Salvedad:** el push real necesita Firebase configurado (sprint 9). El código y las
preferencias están implementados y son testeables, pero sin `google-services`/credenciales
válidas no llega ninguna notificación.

---

## Sprint 4 — Ganancias, preferencias y pulido ✅

- [x] **HU-05** Ganancias `day|week|month|year`, dona + barras (`EarningsChart`),
  historial y **botón ojo** con máscara `$••••••` persistida en `show_balance`.
- [x] **HU-06** Las 5 preferencias sobre **DataStore** (sprint 6):
  modo oscuro, notificaciones, ubicación, sonido e idioma.
- [x] El modo oscuro **se aplica al arrancar** (`App.applyNightMode`): antes sólo se
  aplicaba al mover el switch y se perdía al reabrir la app.
- [x] Logo adaptativo con capa `monochrome` + icono de notificación.
- [x] i18n completa: **166 claves** en `values/` y `values-en/` (mismo número en ambos),
  y los mensajes de los ViewModels salen de recursos (`UiMessage.Res`), no de literales.
- [x] Documentación del proyecto actualizada (`docs/`).

**Definición de terminado:** 51/51 tests en verde; `EarningsViewModelTest` verifica que
`toggleBalance` persiste en DataStore.

---

## Sprint 5 — Interfaz y navegación en Jetpack Compose ✅

Detalle completo en `docs/05-sprint-interfaz-navegacion.md`. Resumen:

- [x] `NavHost` login → main y `NavigationBar` de 4 destinos con back correcto.
- [x] Catálogo de componentes reutilizables (`ValidatedTextField`, `PasswordField`,
  `OrderCard`, `OrderDetailSheet`, `StatusPill`, `ToggleRow`, `PeriodPills`,
  `EarningsChart`, `ToastHost`, `LoadingState`/`ErrorState`).
- [x] Validaciones de login (regex de correo, contraseña ≥ 6, `isError` +
  `supportingText`, botón inhabilitado) y de perfil (nombre ≥ 2, teléfono 7–15 dígitos,
  vehículo obligatorio).
- [x] Mapa osmdroid embebido con `AndroidView`.
- [x] Se eliminaron **2.922 líneas de XML** y los 6 Fragments: 0 archivos en `res/layout`.

---

## Sprint 6 — Persistencia local: DataStore, Room y patrón Repository ✅

Detalle completo en `docs/06-sprint-persistencia-local.md`. Resumen:

- [x] `UserPreferences` migrado a `PreferencesDataStore` con `Flow` y
  `SharedPreferencesMigration` (no se pierden preferencias del usuario actual).
- [x] `AppDatabase` v1 con 5 tablas (`orders_cache`, `order_items_cache`,
  `earnings_cache`, `earnings_history_cache`, `pending_actions`), DAOs y esquema
  exportado en `app/schemas/`.
- [x] `PedidoRepositoryImpl` y `EarningsRepositoryImpl` **cache-first**:
  lectura desde Room, red para refrescar, caché como respaldo.
- [x] La app funciona sin conexión con los pedidos y ganancias cacheados.
- [x] `OrderDaoTest` (6) con Room in-memory sobre Robolectric, `UserPreferencesTest` (4).

---

## Sprint 7 (S08) — Integración con API REST: endpoints, contrato JSON y Retrofit ✅

Detalle en `docs/S08-integracion-api-rest-retrofit.md`.

- [x] 20 endpoints declarados con Retrofit, todos presentes en el `openapi.json` del
  backend vivo, con los campos snake_case mapeados por `@SerializedName`.
- [x] Cliente único con interceptores, timeouts, log sólo en debug y
  `network_security_config` que limita el tráfico en claro a la IP del backend.
- [x] Errores HTTP tipados (`ApiFailure`) con tests del mapeo.

---

## Sprint 8 (S09) — CRUD, manejo de errores, estados de carga y conectividad ✅

Detalle en `docs/S09-crud-errores-estados-conectividad.md`.

- [x] Matriz CRUD explícita por recurso (pedidos, perfil, avisos, caché local).
- [x] Pantalla de avisos con lectura, marcado de leídas y descarte.
- [x] `ErrorState` con un caso por código HTTP y reintento en todas las pantallas.
- [x] `NetworkMonitor` + banner de conectividad + contador de cambios pendientes.
- [x] **Cola offline con WorkManager**: las transacciones hechas sin red se guardan en
  `pending_actions` y se reenvían en orden al recuperar conexión.

---

## Sprint 9 (S10) — Firebase: Auth, Firestore y Storage ⚠️

Detalle en `docs/S10-integracion-firebase.md`.

- [x] Credenciales leídas de `local.properties` (fuera del repo).
- [x] Auth con `FirebaseOptions` (sin `google-services.json`) y degradación a modo dev.
- [x] Firestore: documento `drivers/{email}` con presencia y última posición.
- [x] Storage: comprobante de entrega (`deliveries/{orderId}.jpg`) con cámara + FileProvider.
- [ ] **Prueba en runtime:** bloqueada porque el `appId` facilitado pertenece al package
  `tv.cdlatam.setimer`. Pasos para cerrarlo al final del documento S10.

---

## Trazabilidad

| Artefacto | Documento |
|---|---|
| Historias y criterios de aceptación | `docs/01-requisitos.md` |
| Requerimientos no funcionales | `docs/02-requerimientos-no-funcionales.md` |
| Planificación de sprints (este documento) | `docs/03-sprints.md` |
| Arquitectura MVVM + modelo de datos local | `docs/04-arquitectura.md` |
| Sprint 5 — Compose | `docs/05-sprint-interfaz-navegacion.md` |
| Sprint 6 — DataStore/Room/Repository | `docs/06-sprint-persistencia-local.md` |
| Backend — modelo de datos y API | `docs/07-modelo-datos-backend.md` |
| Informe HU-01 | `docs/08-informe-hu-login.md` |
| S08 — API REST y Retrofit | `docs/S08-integracion-api-rest-retrofit.md` |
| S09 — CRUD y conectividad | `docs/S09-crud-errores-estados-conectividad.md` |
| S10 — Firebase | `docs/S10-integracion-firebase.md` |
