# Sprint 06 — Persistencia local: DataStore, SQLite/Room y patrón Repository (ejecutado)

**Estado: cumplido.** Preferencias en `PreferencesDataStore`, caché offline en Room y
los repositorios de pedidos y ganancias leen primero de la base local.

---

## 1. Esquema real (`AppDatabase` v1)

Archivo: `data/local/db/AppDatabase.kt` · esquema exportado en
`app/schemas/com.driverapp.repartidor.data.local.db.AppDatabase/1.json`

```
orders_cache                     cacé offline de pedidos
├── orderId       INTEGER PK     id del backend
├── restaurantId  INTEGER
├── driverId      INTEGER NULL
├── clientName / clientAddress   TEXT
├── destLat / destLng            REAL
├── status        TEXT           PENDIENTE|ACEPTADO|EN_CAMINO|ENTREGADO|RECHAZADO
├── total / deliveryFee          REAL
├── paymentMethod                TEXT
├── distanceKm / estTimeMin      REAL / INTEGER
├── restaurantName/Address/Phone/Lat/Lng   (aplanado, evita otra tabla)
├── bucket        TEXT           AVAILABLE|ACTIVE|HISTORY  (índice)
└── updatedAt     INTEGER        epoch millis (índice + TTL)

order_items_cache                items de cada pedido (1-N)
├── id            INTEGER PK AUTOINCREMENT
├── orderId       FK → orders_cache(orderId) ON DELETE CASCADE  (índice)
├── menuItemId / productName / description / notes
└── quantity / unitPrice

earnings_cache                   resumen por periodo
├── period        TEXT PK        day|week|month|year
├── total / deliveriesTotal / tipsTotal / bonusesTotal / otherTotal
├── ordersCount   INTEGER
├── chartLabelsJson / chartValuesJson   TEXT (Gson)
└── updatedAt

earnings_history_cache           movimientos de ganancias
├── id            INTEGER PK
├── driverId / orderId / amount / type / clientName / createdAt
└── updatedAt

pending_actions                  cola offline de transiciones (S09)
├── id            INTEGER PK AUTOINCREMENT
├── orderId       INTEGER (índice)
├── type          TEXT   ACCEPT|REJECT|IN_TRANSIT|COMPLETE
├── createdAt / attempts / lastError
```

> **Nota sobre el "diseño anterior":** este documento describía 3 tablas que nunca se
> implementaron. El esquema real tiene 5: se añadieron `earnings_history_cache` (el
> historial no cabía en el resumen) y `pending_actions` (cola offline del sprint S09).

## 2. DAOs

`data/local/db/Daos.kt`

| DAO | Operaciones |
|---|---|
| `OrderDao` | `observeByBucket`, `getByBucket`, `getById`, `observeFirstOfBucket`, `upsert(All)`, `insertItems`, `deleteItems`, `deleteByBucket`, `updateStatus`, `deleteHistoryOlderThan` (TTL), `clear` |
| `EarningsDao` | `upsertSummary`, `getSummary`, `observeSummary`, `upsertHistory`, `getHistory`, `clearHistory`, `clearSummaries` |
| `PendingActionDao` | `insert`, `all`, `observeCount`, `markFailed`, `delete`, `clear` |

Sin `fallbackToDestructiveMigration()`: cualquier cambio de esquema futuro exige su
`Migration` explícita, para no borrar la caché del repartidor por accidente.

`LocalDataSource` (`data/local/db/LocalDataSource.kt`) es la única fachada sobre Room:
convierte entidades ↔ dominio y expone `Flow` para la UI.

## 3. DataStore — preferencias

`data/local/UserPreferences.kt:20`

```kotlin
val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "driverapp_prefs",
    produceMigrations = { ctx -> listOf(SharedPreferencesMigration(ctx, "driverapp_prefs")) }
)
```

- **Migración**: quien ya tenía el `SharedPreferences` `driverapp_prefs` conserva sus
  valores (mismas claves: `dark`, `notifications`, `location`, `sound`, `show_balance`,
  `language`).
- **Lectura**: `Flow<Boolean>` / `Flow<String>` → los ViewModels hacen `stateIn`.
- **Escritura**: funciones `suspend` (`setDarkMode`, `setLanguage`, `toggleShowBalance`…).
- **Excepción documentada**: `snapshot()` es una lectura puntual que se usa en
  `App.onCreate` (tema e idioma antes de inflar la primera pantalla) y en el servicio
  FCM, ambos contextos no suspendibles. Está encapsulada y marcada como tal.

La sesión sigue en `TokenDataStore` (`EncryptedSharedPreferences`, AES256-SIV/GCM):
**no** se migró porque el Keystore no protege un fichero DataStore en claro.

## 4. Repository cache-first

`PedidoRepositoryImpl` (`data/repository/PedidoRepositoryImpl.kt`):

```
lectura  → local.observeBucket(bucket)              (Room, siempre disponible)
refresco → api.availableOrders() → local.replaceBucket()  (red → caché)
fallo    → la caché queda servida; el error se propaga tipado a la UI
```

| Detalle | Implementación |
|---|---|
| Listas | `available`, `history`, `activeOrder` son `Flow` desde Room (`:32-34`) |
| Refresh | `refreshAvailable/History/Active` escriben en caché en una transacción |
| Bug corregido | `refreshActive` con lista vacía **limpia** `ACTIVE` (antes quedaba pegado un pedido ya cerrado) |
| TTL | `purgeHistoryOlderThan(15 días)` en cada refresh (`HISTORY_TTL_DAYS`) |
| Transiciones | `transition()` (`:80`): red primero; sin red → encola + aplica optimista |
| Buckets | `accept/inTransit` → `ACTIVE`; `complete/reject` → `HISTORY` |

`EarningsRepositoryImpl` aplica el mismo patrón sobre `earnings_cache` y
`earnings_history_cache`: si la red falla devuelve lo cacheado; si no hay nada
cacheado, propaga el error.

## 5. Consecuencia verificable

| Escenario | Comportamiento |
|---|---|
| Sin red al abrir Órdenes | se ven los pedidos cacheados + banner "Sin conexión" |
| Sin red al completar una entrega | se aplica en caché y se encola; el banner muestra "1 cambio por enviar" |
| Vuelve la red | `OrderSyncWorker` reenvía en orden FIFO y borra la acción |
| Ganancias sin red | se sirve el último resumen cacheado por periodo |

## 6. Tests (Sprint 6)

| Test | Qué ejercita |
|---|---|
| `UserPreferencesTest` (4) | DataStore real sobre fichero temporal: defaults, escritura de las 5 claves, `toggleShowBalance`, `snapshot` |
| `OrderDaoTest` (6) | Room in-memory con Robolectric: pedido + items, `replaceBucket`, `moveTo`, cola de acciones, caché de ganancias, `clearOrders` |
| `PedidoRepositoryImplTest` (8) | cache-first: escribe en el bucket correcto, `EN_CAMINO`, `HISTORY`, encolado sin red, 404 no se encola, `detail` cae a caché |

Comando: `./gradlew :app:testDebugUnitTest` → 51 tests, 0 fallos.

## 7. Evidencia de arquitectura MVVM

```
UI (Compose) → ViewModel (StateFlow) → UseCase → Repository (cache-first)
                                                      ├── LocalDataSource → Room
                                                      └── ApiService → Retrofit
```

`domain/` sigue sin importar Android ni Retrofit (verificable con
`grep -rn "import android" app/src/main/java/com/driverapp/repartidor/domain` → vacío).
