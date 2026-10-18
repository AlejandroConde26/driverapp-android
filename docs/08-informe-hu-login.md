# DriverApp / SUMAQ — Informe de avance: HU-01 Login con persistencia de datos del negocio

**Historia de usuario implementada:** HU-01 (Iniciar sesión) con persistencia de
los datos del negocio en el backend elegido (**FastAPI + PostgreSQL**).

> Evidencia verificada: `pytest tests/ -q` → **13 passed** (1.49 s), suite del
> backend (`backend/tests/`). Servicio activo en `:8084`.

---

## 1. Historia de usuario

**HU-01 — Iniciar sesión como repartidor**
*Como repartidor, quiero iniciar sesión con mi correo y contraseña, para acceder
a mis pedidos y ganancias de forma segura.*

**Criterios de aceptación (de `docs/01-requisitos.md`)**
1. Login con correo + contraseña (en modo dev la contraseña es libre y el backend
   usa la parte local del correo como UID).
2. El token de sesión se almacena **cifrado** y se envía como `Authorization: Bearer <token>`.
3. Si el login falla, se muestra error claro sin cerrar la app.
4. Al iniciar sesión se registra el token FCM en el backend (`PUT users/me/fcm-token`).
5. El botón de ocultar/mostrar contraseña del login funciona (ojo del login).

## 2. Persistencia de datos del negocio

El login **no es transaccional con la BD, sino que crea/actualiza la fila del
negocio del usuario**: la tabla `users` es el asiento de identidad que luego
alimenta `orders`, `earnings` y `notifications`.

```
Tabla users (app/models/__init__.py)
┌──────────────────┬────────────────────────────┬────────────────────────────┐
│ Campo            │ Tipo                       │ Rol en el login            │
├──────────────────┼────────────────────────────┼────────────────────────────┤
│ id               | INTEGER PK                 │ identidad interna          │
│ firebase_uid     | VARCHAR(128) UNIQUE, idx   │ UID recibido del token     │
│ email            | VARCHAR(255) UNIQUE        │ correo del usuario         │
│ name             | VARCHAR(120)               │ nombre mostrado            │
│ role             | VARCHAR(20) default        │ REPARTIDOR al crear        │
│ fcm_token        | VARCHAR(255)               │ se actualiza en el login   │
│ online / active  | BOOLEAN                    │ active=false → 403         │
│ vehicle          | VARCHAR(20)                │ Bicicleta (default)        │
│ latitude/longitude| FLOAT                     │ reporte de ubicación       │
│ created_at       | DateTime(tz) server_default| alta automática            │
└──────────────────┴────────────────────────────┴────────────────────────────┘

users 1─N orders · users 1─N earnings · users 1─N notifications   (ver docs/07)
```

**Operación de persistencia del login (upsert por `firebase_uid`)** —
`app/routers/auth.py`:
1. `verify_id_token(id_token)` → extrae `sub` (UID) y `email`.
2. `SELECT User WHERE firebase_uid = UID`.
3. Si **no existe** → `INSERT` (uid, email, name derivado, `role=REPARTIDOR`).
4. Si existe y viene `fcm_token` → `UPDATE users.fcm_token` + `commit`.
5. Si `active == False` → `403`.

El resto de la sesión vive **localmente** en el cliente (cifrada, ver §6).

## 3. Avance — implementación

### 3.1 Backend (servicio elegido: FastAPI)
- **Endpoint**: `POST /api/v1/auth/login` → entrada `LoginIn`, salida `LoginResponse`.
- **Guard post-login**: `GET /api/v1/auth/session` y cualquier ruta protegida
  pasan por `get_current_user` (`deps.py`), que re-verifica el token y rehidrata
  el usuario desde `users`.
- **Códigos**: `401` token inválido/ausente · `403` usuario deshabilitado o rol
  insuficiente · `409` conflicto de estado (transiciones de pedido).
- Semilla de usuarios de prueba: `repartidor1` ("Juan Delgado"), `repartidor2`, `admin`.

### 3.2 Cliente (app Android)
- `AuthRepositoryImpl.login(idToken, fcmToken)` → `AuthApiService.login()` (Retrofit)
  → `tokenStore.saveToken(resp.token)` + `saveUser(...)` (**`EncryptedSharedPreferences`**).
- `LoginViewModel` expone `LoginState { Idle, Loading, Error, Success }`; la UI
  muestra spinner, deshabilita el botón y pinta `errorText` en fallo.
- Ojo de la contraseña: alterna `inputType` visible/oculto en `LoginActivity`.

### 3.3 Evidencia (tests)
- `backend/tests/test_api.py::test_login_and_profile` → login con
  `repartidor1`, `GET /api/v1/users/me` → `200`, `name == "Juan Delgado"`,
  `role == "REPARTIDOR"`.
- Suite completa del backend: **13 passed, 8 warnings in 1.49 s**.
- App Android: `LoginUseCaseTest` (3) + `LoginViewModelTest` (2) en verde (16/16 totales).

## 4. Datos de entrada y retorno

**Entrada** `LoginIn` (`schemas/user.py` + `routers/auth.py`):
```json
{ "id_token": "<UID o JWT de Firebase>", "fcm_token": "opcional" }
```

**Retorno** `LoginResponse`:
```json
{
  "user": {
    "id": 1, "email": "repartidor1@driverapp.com", "name": "Juan Delgado",
    "phone": null, "role": "REPARTIDOR", "vehicle": "Bicicleta",
    "online": false, "active": true, "latitude": null, "longitude": null,
    "created_at": "2026-09-27T03:02:14Z"
  },
  "token": "<mismo id_token>"
}
```

Llamadas posteriores usan `Authorization: Bearer <token>` → `UserOut` en `GET /api/v1/users/me`.

## 5. Servicio, base de datos y stack

| Ítem | Valor |
|---|---|
| Servicio | `DriverApp API` (FastAPI) — `GET /health` → `driverapp-api`, puerto **8084** |
| Base de datos | **PostgreSQL 16** (`postgres:16-alpine`, `driverapp-db`, `:5432`) — ORM SQLAlchemy 2.0 async + `asyncpg` |
| Lenguaje | Python **3.12.3** |
| Framework | FastAPI **0.115.6** + Uvicorn **0.34.0** · Pydantic **2.10.4** |
| Tests | pytest **8.3.4** + pytest-asyncio (SQLite/aiosqlite) |

## 6. Consideraciones de seguridad aplicadas al login

- **Auth Bearer** en cada request; en modo real el token es un **JWT de Firebase**
  (RS256) verificado con `firebase_admin.auth.verify_id_token`; en `DEV_MODE=1`
  se acepta el UID como token (solo pruebas — para producción: `DEV_MODE=0` +
  `FIREBASE_CREDENTIALS_PATH`).
- **Cifrado en cliente**: el token y el usuario se guardan en
  `EncryptedSharedPreferences` (`TokenDataStore`, AES256_SIV/GCM) — nunca en claro.
- **Validación de entrada** con Pydantic (`id_token` no vacío, `fcm_token` opcional).
- **Acceso denegado explícito**: `401` (token) y `403` (usuario `active=false`).
- **No hay contraseñas en la BD de negocio**: `users` no almacena credenciales
  (la autenticación la resuelve Firebase/cliente; el backend solo custodia el perfil).

## 7. Criterios de aceptación — cumplidos

- [x] Login con correo + contraseña (modo dev: UID derivado del correo).
- [x] Token de sesión guardado **cifrado** y enviado como Bearer en cada request.
- [x] Error claro en fallo sin cerrar la app (`LoginState.Error` + toast + `errorText`).
- [x] Token FCM registrado al iniciar sesión (`PUT users/me/fcm-token` y `fcm_token` en login).
- [x] Ojo mostrar/ocultar contraseña funcional (`LoginActivity.eyeToggle`).
- [x] Datos del negocio persistidos en `users` (upsert por `firebase_uid`) y **13/13 tests** en verde.

## 8. Trazabilidad

| Recurso | Lugar |
|---|---|
| Historia y criterios | `docs/01-requisitos.md` (HU-01) |
| Planificación de sprints | `docs/03-sprints.md` (Sprint 1) |
| Arquitectura MVVM | `docs/04-arquitectura.md` |
| Backend / E-R / API | `docs/07-modelo-datos-backend.md` |
| Código | `backend/app/routers/auth.py`, `app/deps.py`, `app/models/__init__.py` · `data/repository/AuthRepositoryImpl.kt`, `ui/login/` |