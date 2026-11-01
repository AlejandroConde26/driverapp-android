# S08 — Integración con API REST: endpoints, contrato JSON y consumo con Retrofit

**Estado: cumplido y verificado.** Cada endpoint que declara la app existe en el
backend vivo (`http://169.58.190.93:8084`) con el método correcto y los nombres de
campo coinciden. La verificación se hizo cruzando `AuthApiService.kt` y
`PedidoApiService.kt` contra el `openapi.json` del servicio en ejecución.

---

## 1. Arquitectura del consumo

```
UI (Compose) → ViewModel → UseCase → Repository → Retrofit (ApiService) → DTO → toDomain()
```

| Pieza | Archivo | Responsabilidad |
|---|---|---|
| Cliente HTTP | `data/remote/RetrofitClient.kt:19` | OkHttp + Retrofit, `Authorization: Bearer`, timeouts, log sólo en debug |
| API de sesión/perfil | `data/remote/AuthApiService.kt` | 7 endpoints |
| API de pedidos/ganancias/avisos | `data/remote/PedidoApiService.kt` | 13 endpoints |
| DTOs + mapeo a dominio | `data/remote/dto/AuthDto.kt`, `data/remote/dto/PedidoDto.kt` | `@SerializedName` snake_case → camelCase y `toDomain()` |
| Errores tipados | `data/remote/ApiError.kt:50` | HTTP/IO → `ApiFailure` |

Decisiones de configuración:

- **Base URL** en `BuildConfig.API_BASE_URL` (`app/build.gradle.kts:25`), no en el código.
- **Token** leído en cada request desde `TokenDataStore` (cifrado), nunca cacheado en memoria.
- **Timeouts**: 15 s connect / 20 s read / 20 s write (`RetrofitClient.kt:28-31`).
- **Log de red** (`HttpLoggingInterceptor`) sólo si `BuildConfig.DEBUG`: en release no
  se escriben URLs ni cabeceras en logcat (`RetrofitClient.kt:35`).
- **Tráfico en claro acotado**: `res/xml/network_security_config.xml` prohíbe cleartext
  por defecto y lo permite únicamente para la IP del backend de desarrollo.

## 2. Contrato: endpoints que consume la app

### 2.1 Autenticación y perfil (`AuthApiService.kt`)

| Método | Ruta | Auth | Body | Respuesta |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | — | `LoginRequestDto` | `LoginResponseDto` |
| GET | `/api/v1/users/me` | Bearer | — | `UserDto` |
| PUT | `/api/v1/users/me` | Bearer | `UserUpdateDto` | `UserDto` |
| PUT | `/api/v1/users/me/vehicle` | Bearer | `VehicleUpdateDto` | `UserDto` |
| PUT | `/api/v1/users/me/status` | Bearer | `StatusUpdateDto` | `UserDto` |
| PUT | `/api/v1/users/me/location` | Bearer | `LocationUpdateDto` | `UserDto` |
| PUT | `/api/v1/users/me/fcm-token` | Bearer | `FcmTokenUpdateDto` | `UserDto` |

### 2.2 Pedidos, ganancias y avisos (`PedidoApiService.kt`)

| Método | Ruta | Query | Respuesta |
|---|---|---|---|
| GET | `/api/v1/orders/available` | `restaurant_id` | `List<PedidoDto>` |
| GET | `/api/v1/orders/my` | `restaurant_id` | `List<PedidoDto>` |
| GET | `/api/v1/orders/history` | `restaurant_id` | `List<PedidoDto>` |
| GET | `/api/v1/orders/{id}` | — | `PedidoDto` |
| POST | `/api/v1/orders/{id}/accept` | — | `PedidoDto` |
| POST | `/api/v1/orders/{id}/reject` | — | `PedidoDto` |
| POST | `/api/v1/orders/{id}/in-transit` | — | `PedidoDto` |
| POST | `/api/v1/orders/{id}/complete` | — | `PedidoDto` |
| GET | `/api/v1/earnings/summary` | `period` | `ResumenGananciasDto` |
| GET | `/api/v1/earnings/history` | — | `List<GananciaDto>` |
| GET | `/api/v1/restaurants` | — | `List<RestauranteDto>` |
| GET | `/api/v1/notifications/my` | — | `List<NotificacionDto>` |
| PUT | `/api/v1/notifications/my/read` | — | `204/200` |

> `restaurant_id` no es opcional en la práctica: la app siempre envía
> `BuildConfig.RESTAURANT_ID` (= 5, SUMAQ) desde `PedidoRepositoryImpl`.

## 3. JSON real del contrato

### Login (request)

```json
{ "email": "repartidor1@driverapp.com", "password": "••••••", "fcm_token": "dQw4..." }
```

`email` y `password` son obligatorios; `fcm_token` es opcional. El backend verifica la
contraseña con bcrypt y devuelve su propio **JWT** (no un token de Firebase). Ver
`docs/auth-backend.md`.

### Login (response)

```json
{
  "user": {
    "id": 2, "email": "repartidor1@driverapp.com", "name": "Juan Delgado",
    "phone": "+52 555 123 4567", "role": "REPARTIDOR", "vehicle": "Moto",
    "online": true, "active": true, "latitude": -12.1219, "longitude": -77.0297,
    "created_at": "2026-09-30T12:00:00"
  },
  "token": "eyJhbGciOi..."
}
```

### Pedido (`OrderOut` → `PedidoDto`)

```json
{
  "id": 10, "restaurant_id": 5, "driver_id": 2,
  "client_name": "Ana López", "client_address": "Calle Falsa 123",
  "dest_lat": 19.4326, "dest_lng": -99.1332, "status": "PENDIENTE",
  "total": 150.0, "delivery_fee": 25.0, "payment_method": "Efectivo",
  "distance_km": 3.4, "est_time_min": 20,
  "restaurant": { "id": 5, "name": "SUMAQ", "address": "Av. Larco 123",
                  "lat": -12.1219, "lng": -77.0297, "phone": "+51 999 111 222" },
  "items": [ { "id": 1, "menu_item_id": 2, "product_name": "Ceviche Clásico",
               "description": "Pesca del día", "quantity": 2,
               "unit_price": 32.0, "notes": "Sin picante" } ]
}
```

Los DTO replican exactamente esa forma con `@SerializedName("delivery_fee")`, etc.
(`PedidoDto.kt:54-90`). El mapeo a dominio está cubierto por
`PedidoRepositoryImplTest.refreshAvailable escribe el resultado en la cache AVAILABLE`.

## 4. Errores de transporte

`ApiError.kt` traduce la respuesta a un vocabulario propio en lugar de propagar el
`message` de Retrofit:

| Condición | `ApiFailure` | Efecto en la app |
|---|---|---|
| `IOException` / `UnknownHost` / `SocketTimeout` / `ConnectException` | `NoConnection` | se usa la caché de Room; las transiciones se encolan |
| 401 / 403 | `Unauthorized` | "Sesión no válida", se sugiere volver a entrar |
| 404 | `NotFound` | "El pedido ya no está disponible" |
| 422 | `Validation` (con el `detail` del backend) | mensaje del servidor al usuario |
| 5xx | `Server` | "El servidor falló (código)" |
| otro | `Unknown` | mensaje genérico |

Cubierto por `ApiErrorMapperTest` (6 tests).

## 5. Verificación

| Comprobación | Resultado |
|---|---|
| Rutas declaradas en la app que existen en `openapi.json` | **20 / 20** |
| Métodos HTTP coincidentes | **20 / 20** |
| Campos snake_case con `@SerializedName` | 22 campos, 0 faltantes |
| Tests del mapeo DTO→dominio | 8 en `PedidoRepositoryImplTest` |
| Tests del mapeo de errores | 6 en `ApiErrorMapperTest` |

Comando reproducible:

```bash
curl -s http://169.58.190.93:8084/openapi.json | jq '.paths | keys'
```
