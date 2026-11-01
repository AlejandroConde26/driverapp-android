# DriverApp / SUMAQ — Backend: persistencia, modelos, API y seguridad (propuesta técnica)

**Encargo de práctica:**

> *Definir la persistencia de datos del negocio (diagrama E-R).
> Crear las clases (modelos) y APIs (datos de entrada y retorno).
> Avance del backend. Servicio a usar. Tipo de base de datos.
> Lenguaje y/o framework backend (versión). Consideraciones de seguridad.*

Este documento es la **propuesta técnica de diseño** del backend de la plataforma
DriverApp/SUMAQ: define la persistencia, el modelo de datos, el contrato de la
API, el servicio, la base de datos, el stack y la seguridad.

---

## 1. Persistencia de datos del negocio (diagrama E-R)

Base de datos relacional con 7 tablas. Estado de pedido:
`PENDIENTE → ACEPTADO → EN_CAMINO → ENTREGADO` (o `RECHAZADO`).

```
┌────────────┐       ┌──────────────┐        ┌──────────────┐
│   users    │       │ restaurants  │        │  menu_items  │
│────────────│       │──────────────│        │──────────────│
│ id (PK)    │       │ id (PK)      │        │ id (PK)      │
│ firebase_uid│       │ name         │        │ restaurant_id(FK)──► restaurants
│ email (U)  │       │ address      │        │ name         │
│ name       │       │ lat, lng     │        │ description  │
│ phone      │       │ phone        │        │ price        │
│ role       │       │ opening_hours│        │ photo_url    │
│ vehicle    │       └──────┬───────┘        │ available    │
│ online     │              │                │ created_at   │
│ active     │        1     │ N             └──────┬───────┘
│ fcm_token  │   ┌──────────┴────────┬─────────────┘
│ lat, lng   │   │                  │                │ (FK opcional)
│ created_at │   │ 1                │ 1              │
└─────┬──────┘   │                  │                │
      │ 1        ▼                  ▼                │
      │     ┌──────────────┐   ┌───────────┐         │
      └────►│    orders    │   │ notifications│       │
      │     │──────────────│   │───────────│        │
  N   │ N   │ id (PK)      │   │ id (PK)   │        │
      │     │ restaurant_id(FK)│ user_id(FK)──►users │
      │     │ driver_id (FK)──►users              │
      │     │ client_name  │   │ title, message    │
      │     │ client_address│   │ read, created_at  │
  N   │ 1   │ dest_lat, lng│                      │
      │     │ status       │                      │
┌─────┴──────┐ total, delivery_fee │              │
│ earnings   │ payment_method      │              │
│────────────│ distance_km, est_time_min          │
│ id (PK)    │ created_at, delivered_at           │
│ driver_id(FK)──►users           │              │
│ order_id(FK)──►orders  (opcional)│              │
│ amount     └──────┬─────────────┘              │
│ type       │ 1    │                             │
│ created_at │      │ N                           │
└────────────┘      ▼                             │
             ┌──────────────┐                     │
             │ order_items  │                     │
             │──────────────│                     │
             │ id (PK)      │                     │
             │ order_id (FK)──►orders             │
             │ menu_item_id(FK)──►menu_items (opcional)
             │ product_name, description, notes   │
             │ quantity, unit_price              │
             └──────────────┘                    │
```

**Cardinalidades clave**
- `users` 1—N `orders` (un repartidor tiene varios pedidos; `driver_id` opcional).
- `restaurants` 1—N `orders` y 1—N `menu_items` (cascade delete-orphan en menú).
- `orders` 1—N `order_items` (cascade delete-orphan); `order_items` N—1 `menu_items` (FK opcional).
- `users` 1—N `earnings`; `earnings` N—1 `orders` (FK opcional).
- `users` 1—N `notifications`.

**Enums de negocio**
- `UserRole`: `REPARTIDOR | ADMIN | RESTAURANTE`.
- `VehicleType`: `Bicicleta | Moto | Auto`.
- `OrderStatus`: `PENDIENTE | ACEPTADO | EN_CAMINO | ENTREGADO | RECHAZADO`.
- `PaymentMethod`: `Efectivo | Tarjeta | Online`.
- `EarningType`: `PEDIDO | PROPINA | BONO | OTRO`.

## 2. Clases (modelos) y APIs (datos de entrada y retorno)

### 2.1 Modelos ORM (SQLAlchemy)

En `app/models/` (`User`, `Restaurant`, `MenuItem`, `Order`, `OrderItem`,
`Earning`, `Notification`) con tipos mapeados (`String`, `Integer`, `Float`,
`Boolean`, `DateTime(tz)`, `ForeignKey`, `relationship`).

### 2.2 Schemas Pydantic — entrada / salida

**`schemas/user.py`**
- Entrada: `RegisterRequest` (firebase_uid, email, name, phone, role, vehicle),
  `UserUpdate` (name/phone/email), `VehicleUpdate`, `StatusUpdate`, `FcmTokenUpdate`,
  `LocationUpdate`, `LoginIn` (id_token, fcm_token?).
- Salida: `UserOut` (id, email, name, phone, role, vehicle, online, active, lat/lng, created_at),
  `LoginResponse` (user + token).

**`schemas/order.py`**
- Entrada: `RestaurantIn`, `OrderItemIn` (menu_item_id?, product_name, description?,
  quantity, unit_price, notes?), `OrderCreate` (restaurant_id, client_name/address,
  dest_lat/lng, total, delivery_fee, payment_method, distance_km, est_time_min, items[]),
  `AdminOrderUpdate` (status?, driver_id?, total?, delivery_fee?).
- Salida: `OrderOut` (todas + restaurant, driver, items), `RestaurantOut`,
  `OrderItemOut`, `DriverInfo`.

**`schemas/menu.py`**
- Entrada: `MenuItemIn` (name, description?, price, photo_url?, available).
- Salida: `MenuItemOut` (+ id, restaurant_id, created_at).

**`schemas/misc.py`**
- Salida: `EarningOut`, `EarningsSummary` (period, total, orders_count, totals por tipo,
  chart_labels/values, history), `NotificationOut`.
- Entrada: `SendNotificationIn` (user_id, title, message).

### 2.3 Contrato de la API (propuesto, 32 endpoints)

| Método | Ruta | Auth | Entrada | Salida |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | — | `id_token`, `fcm_token?` | `LoginResponse` |
| POST | `/api/v1/auth/register` | — | `RegisterRequest` + id_token | `UserOut` |
| GET | `/api/v1/auth/session` | Bearer | — | `UserOut` |
| GET | `/api/v1/users/me` | Bearer | — | `UserOut` |
| PUT | `/api/v1/users/me` | Bearer | `UserUpdate` | `UserOut` |
| PUT | `/api/v1/users/me/vehicle` | Bearer | `VehicleUpdate` | `UserOut` |
| PUT | `/api/v1/users/me/status` | Bearer | `StatusUpdate` | `UserOut` |
| PUT | `/api/v1/users/me/fcm-token` | Bearer | `FcmTokenUpdate` | `UserOut` |
| PUT | `/api/v1/users/me/location` | Bearer | `LocationUpdate` | `UserOut` |
| GET | `/api/v1/orders/available` | Driver | `restaurant_id?` | `list[OrderOut]` |
| GET | `/api/v1/orders/my` | Driver | `restaurant_id?` | `list[OrderOut]` |
| GET | `/api/v1/orders/history` | Driver | `restaurant_id?` | `list[OrderOut]` |
| GET | `/api/v1/orders/{id}` | Bearer | — | `OrderOut` |
| POST | `/api/v1/orders/{id}/accept` | Driver | — | `OrderOut` |
| POST | `/api/v1/orders/{id}/reject` | Driver | — | `OrderOut` |
| POST | `/api/v1/orders/{id}/in-transit` | Driver | — | `OrderOut` |
| POST | `/api/v1/orders/{id}/complete` | Driver | — | `OrderOut` |
| GET | `/api/v1/earnings/summary` | Driver | `period=day\|week\|month\|year` | `EarningsSummary` |
| GET | `/api/v1/earnings/history` | Driver | `limit≤200` | `list[EarningOut]` |
| GET/POST | `/api/v1/restaurants` | Bearer / Admin | `RestaurantIn` | `RestaurantOut` |
| PUT/DELETE | `/api/v1/restaurants/{id}` | Admin | `RestaurantIn` | `RestaurantOut` |
| GET | `/api/v1/restaurants/{id}/menu` | Bearer | `available_only?` | `list[MenuItemOut]` |
| POST | `/api/v1/restaurants/{id}/menu` | Admin | `MenuItemIn` | `MenuItemOut` |
| PUT/DELETE | `/api/v1/restaurants/menu/{id}` | Admin | `MenuItemIn` | `MenuItemOut` |
| GET | `/api/v1/admin/orders` | Admin | `restaurant_id?`, `limit` | `list[OrderOut]` |
| POST | `/api/v1/admin/orders` | Admin | `OrderCreate` | `OrderOut` |
| PUT | `/api/v1/admin/orders/{id}` | Admin | `AdminOrderUpdate` | `OrderOut` |
| GET | `/api/v1/admin/drivers` | Admin | — | lista repartidores |
| PUT | `/api/v1/admin/drivers/{id}` | Admin | `{ active }` | actualizado |
| GET | `/api/v1/admin/stats/dashboard` | Admin | — | KPIs del dashboard |
| GET | `/api/v1/admin/report/earnings.csv` | Admin | — | CSV |
| GET | `/api/v1/notifications` | Bearer | — | `list[NotificationOut]` |

Contrato completo: `openapi.json` o `/docs` del backend.

## 3. Avance del backend (alcance previsto)

- API bajo `/api/v1`, **FastAPI** con routers: `auth`, `users`, `orders`,
  `earnings`, `restaurants` (+menú), `admin`, `notifications`.
- Arranque con `lifespan`: `Base.metadata.create_all` + ALTERs de columnas
  (migraciones incrementales de `users.latitude/longitude` y
  `order_items.menu_item_id/description/notes`).
- Semilla de datos: restaurante **SUMAQ** (id=5) con 8 platillos del catálogo.
- `GET /health` → `{"status":"ok","service":"driverapp-api","dev_mode":...}`.
- Tests con **pytest** (13 casos) y base SQLite en memoria.
- Documentación automática (Swagger `/docs`).

## 4. Servicio a usar

API REST **`DriverApp API`** servida con **Uvicorn** en el puerto **8084**.
Se consume desde:
- **App Android** (`com.driverapp.repartidor`): Retrofit + OkHttp + Gson
  (`data/remote/AuthApiService`, `PedidoApiService`, `RetrofitClient`),
  base URL `http://169.58.190.93:8084/`.
- **Dashboard web-admin** (React): `fetch` contra el mismo origen (proxy Vite),
  puerto **3000**.

## 5. Tipo de base de datos

**PostgreSQL 16** (`postgres:16-alpine`, contenedor `driverapp-db`, puerto **5432**,
base/usuario `driverapp`) definido en `docker-compose.yml`. Acceso con
**SQLAlchemy 2.0 asyncio** (driver `asyncpg`) vía `DATABASE_URL`. Para pruebas se
usa **SQLite** (`aiosqlite`) de modo que no se requiere el contenedor en CI.

## 6. Lenguaje y/o framework backend (versión)

| Componente | Versión |
|---|---|
| Python | **3.12.3** |
| FastAPI | **0.115.6** |
| Uvicorn (standard) | **0.34.0** |
| SQLAlchemy (asyncio) | **2.0.36** |
| asyncpg / aiosqlite | **0.30.0 / 0.20.0** |
| Pydantic + pydantic-settings | **2.10.4 / 2.7.0** |
| email-validator | **2.2.0** |
| bcrypt | **4.2.1** |
| firebase-admin | **6.5.0** |
| httpx | **0.28.1** |
| pytest + pytest-asyncio | **8.3.4 / 0.25.2** |

## 7. Consideraciones de seguridad

- **Autenticación Bearer** en todas las rutas protegidas (`extract_token`). En modo
  real, el token es un **JWT de Firebase** (firmado RS256) verificado con
  `firebase_admin.auth.verify_id_token`. En `DEV_MODE` se acepta el UID como token
  (solo entornos de pruebas).
- **Control por roles** (`require_roles`): `get_driver` (repartidor), `get_admin`,
  `get_staff` (admin + restaurante). Respuestas `401` (token), `403` (permisos),
  `409` (estado inválido del pedido).
- **El total lo calcula el servidor**: al crear un pedido, el admin resuelve los
  precios desde `menu_items` (no se confía en cantidades/precios del cliente).
- **CORS restringido**: lista fija de orígenes + regex `localhost/127.0.0.1`,
  credenciales permitidas solo en esos orígenes.
- **Validación de entrada** con Pydantic (`EmailStr`, `pattern` en parámetros,
  tipos estrictos) y límites (`limit ≤ 200`).
- **Sesión del repartidor cifrada** en la app (`EncryptedSharedPreferences`,
  AES256_SIV/GCM) — el token nunca viaja en logs ni se persiste en claro.
- **Token FCM por usuario** para push dirigido; notificaciones con canal `orders`.
- Backend y BD **no exponen credenciales**: `DATABASE_URL` y configuración por
  variables de entorno (`.env`), secretos fuera del repo.

> Advertencia honesta: `DEV_MODE=1` desactiva la verificación real de tokens; para
> producción se debe fijar `FIREBASE_CREDENTIALS_PATH` y `DEV_MODE=0`.

## 8. Trazabilidad

| Recurso | Lugar |
|---|---|
| Datos del negocio | este documento (E-R) + `backend/app/models/` |
| Contrato de API | este documento + `openapi.json` |
| Arquitectura general | `docs/04-arquitectura.md` |
| Requerimientos no funcionales | `docs/02-requerimientos-no-funcionales.md` |
| Planificación | `docs/03-sprints.md` |
| App MCV / persistencia local | `docs/06-sprint-persistencia-local.md` |