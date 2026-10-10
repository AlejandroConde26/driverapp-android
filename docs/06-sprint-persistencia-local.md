# DriverApp / SUMAQ — Sprint 06: Persistencia local con DataStore, Room y patrón Repository

**Encargo de práctica de campo — fecha de entrega: 09 de octubre, 23:59 h**

> *Ejecutar sprint de persistencia local con DataStore, SQLite/Room y patrón
> Repository. Presentar evidencia de arquitectura MVVM.*

Este documento planifica el sprint y detalla el modelo local a implementar en la
app Android del repartidor (paquete `com.driverapp.repartidor`).

---

## 1. Objetivo

Incorporar persistencia local real:
- **DataStore** (`PreferenceDataStore`) para las preferencias de usuario.
- **Room (SQLite)** como caché local de pedidos y ganancias (offline-first).
- **Patrón Repository** cache-first que combina fuente local + remota.
- **Evidencia de arquitectura MVVM** actualizada en la documentación.

## 2. Baseline — estado actual

| Aspecto | Estado actual | Brecha frente al sprint |
|---|---|---|
| Preferencias | `SharedPreferences` llano (`UserPreferences`, archivo `driverapp_prefs`) | Migrar a **PreferenceDataStore** |
| Sesión | `EncryptedSharedPreferences` (`TokenDataStore`, AES256_SIV/GCM) `driverapp_secret` | Se **mantiene intacta** (sigue cifrada) |
| BD local | No hay (sin Room) | Crear **AppDatabase + DAOs** |
| Repository | Interfaces `AuthRepository`, `PedidoRepository`, `LocationRepository`; impls `AuthRepositoryImpl`/`PedidoRepositoryImpl` → fuente **remota** (Retrofit) | Añadir **fuente local** y repo **cache-first** |
| MVVM | Ya implementado (`docs/04`, README) | Actualizar con la nueva capa local |

> Nota: a pesar de su nombre, `TokenDataStore` **no** es Jetpack DataStore; es
> `EncryptedSharedPreferences`. La sesión permanece así por seguridad.

## 3. DataStore — preferencias

Migrar `UserPreferences` → **`PreferencesDataStore`** (coroutines + `Flow`):

```kotlin
// data/local/UserPreferences.kt
val Context.userPrefsDataStore by preferencesDataStore(name = "driverapp_prefs")

class UserPreferences(private val context: Context) {
    val darkMode: Flow<Boolean>        // key "dark"
    val notificationsEnabled: Flow<Boolean>  // key "notifications"
    val locationEnabled: Flow<Boolean> // key "location"
    val notificationSound: Flow<Boolean>     // key "sound"
    val showBalance: Flow<Boolean>     // key "show_balance"
    val language: Flow<String>         // key "language" ("es" | "en")
    suspend fun setX(value: ...)       // edit { ... } helpers
}
```

- Los ViewModels recogen el `Flow` (`stateIn`) en lugar de leer el valor directo.
- Contrato de claves y defaults queda exactamente igual que hoy:
  `dark=false`, `notifications=true`, `location=true`, `sound=true`,
  `show_balance=true`, `language="es"`.
- La **sesión cifrada** (`TokenDataStore`) no se toca: token + usuario siguen en
  `EncryptedSharedPreferences`.

## 4. Room — SQLite local

### 4.1 Esquema v1 (`AppDatabase`)

```
├── orders_cache                                # caché offline de pedidos
│   ├── order_id       TEXT  PK                 # id de pedido (backend)
│   ├── status         TEXT                     # PENDIENTE|ACEPTADO|EN_CAMINO|ENTREGADO|RECHAZADO
│   ├── client_name    TEXT
│   ├── client_address TEXT
│   ├── dest_lat/lng   REAL
│   ├── delivery_fee   REAL
│   ├── total          REAL
│   ├── payment_method TEXT
│   └── updated_at     INTEGER (epoch)
│
├── order_items_cache                            # ítems de cada pedido (1-N)
│   ├── id           INTEGER PK AUTOINCREMENT
│   ├── order_id     TEXT  FK → orders_cache   (CASCADE)
│   ├── product_name TEXT
│   ├── description  TEXT
│   ├── notes        TEXT
│   └── quantity     INTEGER
│
└── earnings_cache                               # resumen por periodo
    ├── period        TEXT  PK                  # day|week|month|year
    ├── total         REAL
    ├── delivered     INTEGER
    ├── updated_at    INTEGER (epoch)
```

### 4.2 DAOs y migraciones

- `OrderDao`: `upsertAll(orders)`, `getByStatus(status)`, `getById(id)`,
  `getAllByStatusIn(...)`, `deleteOlderThan(ts)` (TTL), `clear()`.
- `OrderItemDao`: `upsertAll(items)`, `getForOrder(orderId)` (join 1-N).
- `EarningsDao`: `upsert(summary)`, `getByPeriod(period)`.
- Migraciones declaradas (v1 → …) con `fallbackToDestructiveMigration()`
  deshabilitado en producción; esquema exportado a `app/schemas/`.

### 4.3 `LocalDataSource`

```kotlin
class LocalDataSource(db: AppDatabase) {
    val orderDao = db.orderDao(); val itemDao = db.orderItemDao()
    val earningsDao = db.earningsDao()
    // operaciones suspendidas, ámbito IO (Room ya lo gestiona)
}
```

## 5. Patrón Repository cache-first

- **`PedidoRepositoryImpl`** (interfaz `PedidoRepository` existente):
  1. Lee de **Room** (`orders_cache`) y emite al instante.
  2. Consulta la **red** (`PedidoApiService`).
  3. Actualiza la caché (`upsertAll`) y re-emite.
  4. Estados de error: si no hay red, devuelve la **caché** (offline-first) +
     señal de "sin conexión".
- **`EarningsRepositoryImpl`**: mismo patrón con `earnings_cache` por periodo.
- Se inyectan en `AppContainer` (`LocalDataSource` + `remote`), y los
  ViewModels de Órdenes y Ganancias pasan a depender del repo cache-first.

Flujo: `UI → ViewModel → UseCase → Repository (local→red→caché) → DAO / Retrofit`.

## 6. Dependencias a añadir

```
implementation("androidx.datastore:datastore-preferences:1.1.x")
implementation("androidx.room:room-runtime:2.6.x")
implementation("androidx.room:room-ktx:2.6.x")
kapt("androidx.room:room-compiler:2.6.x")   // o KSP
implementation("androidx.sqlite:sqlite-ktx:2.4.x")
```

> Requiere activar KSP/kapt (`room-compiler`). Test de integración con
> `room-testing` (in-memory) opcional.

## 7. Evidencia de arquitectura MVVM (a presentar)

1. **Diagrama de capas** actualizado en `docs/04-arquitectura.md`:
   `domain` (Kotlin puro) → `data/remote` + `data/local` → `di/AppContainer` → `ui/`.
2. **Flujo de datos** documentado con el nuevo camino cache-first.
3. **Modelo de datos local** (tablas, claves, DAOs) en esta sección y en `docs/04`.
4. **Tests**: `UserPreferencesTest` (DataStore, defaults + escritura), DAO con
   Room in-memory (`orders_cache`, `earnings_cache`), y `PedidoRepositoryImplTest`
   cache-first (MockK de LocalDataSource) — además de los 16 existentes.

## 8. Criterios de aceptación — definición de terminado del sprint

- [x] Preferencias leídas/escritas vía `PreferenceDataStore` (Flow), mismo contrato de claves.
- [x] `AppDatabase` v1 con `orders_cache`, `order_items_cache`, `earnings_cache` + DAOs y migraciones.
- [x] `PedidoRepositoryImpl` cache-first (local → red → refresca caché) y repo de ganancias.
- [x] App funciona **sin conexión** leyendo la caché de pedidos y ganancias.
- [x] Sesión sigue cifrada en `TokenDataStore` (sin cambios).
- [x] `docs/04-arquitectura.md` y `README.md` actualizados con la capa local (evidencia MVVM).
- [x] Tests en verde (16 existentes + nuevos de DataStore/DAO/Repository).

## 9. Trazabilidad

| Recurso | Lugar |
|---|---|
| Historias cubiertas | `docs/01-requisitos.md` — HU-03 (órdenes con **reintento sin conexión**), HU-05 (historial de ganancias), HU-06 (preferencias) |
| No funcionales | `docs/02-requerimientos-no-funcionales.md` (seguridad de sesión, robustez offline) |
| Arquitectura y modelo local | `docs/04-arquitectura.md` (MVVM + `UserPreferences`/`TokenDataStore`) |
| Entrega | **09 de octubre, 23:59 h** |