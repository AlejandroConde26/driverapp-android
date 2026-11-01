# Autenticación contra el backend (correo + contraseña)

**Estado: implementado y verificado en producción el 2026-11-01.**

La identidad de los repartidores la emite **el backend** (`169.58.190.93:8084`).
Firebase ya no participa en el login; sólo en las notificaciones push.

## Por qué se cambió

Antes, el backend no verificaba contraseñas: `LoginIn` sólo aceptaba `id_token` y
`verify_id_token()` lo interpretaba como UID en `DEV_MODE`. La consecuencia, medida
contra el servidor real:

```bash
$ curl -X POST .../api/v1/auth/login -d '{"id_token":"repartidor1"}'
{"user":{"id":2,"email":"repartidor1@driverapp.com",...,"role":"REPARTIDOR"},"token":"repartidor1"}
```

Cualquier valor devolvía una sesión válida, sin contraseña. Eso no es autenticación.

## Contrato actual

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{ "email": "repartidor1@driverapp.com", "password": "…", "fcm_token": "opcional" }
```

| Respuesta | Cuándo |
|---|---|
| `200` `{ "user": UserOut, "token": "<JWT HS256>" }` | credenciales correctas |
| `401 { "detail": "Credenciales inválidas" }` | correo inexistente **o** contraseña incorrecta (mismo mensaje: no se filtra qué correos existen) |
| `403 { "detail": "Usuario deshabilitado" }` | `active = false` |
| `422` | correo con formato inválido |

El `token` es un JWT firmado por el backend: `sub` = `users.id`, `exp` = 7 días
(`ACCESS_TOKEN_EXPIRE_MINUTES=10080`), algoritmo HS256 con `JWT_SECRET`.

### Registro

```http
POST /api/v1/auth/register
{ "email": "...", "password": "mín. 6", "name": "...", "phone": "opcional", "role": "REPARTIDOR", "vehicle": "MOTO" }
```

`201` con `{user, token}` (la app entra directa), `409` si el correo ya existe.

### Sesión y contraseña

| Endpoint | Uso |
|---|---|
| `GET /api/v1/auth/session` | valida el token y devuelve el usuario |
| `PUT /api/v1/users/me/password` | `{current_password, new_password}` → `204`; `400` si la actual no coincide |

### Cómo se protege el resto de la API

`app/deps.py → get_current_user()` resuelve el usuario en este orden:

1. **JWT del backend** → `sub` se interpreta como `users.id`. Camino normal.
2. **Legacy** (sólo si `LEGACY_AUTH_ENABLED=1`): ID token de Firebase o UID crudo.
   Queda en `0` para que no exista una segunda puerta de entrada.

Cualquier otro token devuelve `401 Sesión inválida o expirada`. Un JWT manipulado
también: la firma se verifica.

## Cambios en el servidor

| Archivo | Cambio |
|---|---|
| `app/services/security.py` | **nuevo**: `hash_password` / `verify_password` (bcrypt, coste 12) y `create_access_token` / `decode_access_token` (PyJWT). |
| `app/config.py` | `JWT_SECRET`, `ACCESS_TOKEN_EXPIRE_MINUTES`, `LEGACY_AUTH_ENABLED`. |
| `app/models/__init__.py` | `User.password_hash` (`VARCHAR(255)`, nullable). |
| `app/schemas/user.py` | `LoginRequest`, `RegisterRequest` con contraseña, `PasswordChange`. |
| `app/routers/auth.py` | Login/registro por contraseña; el token lo emite el backend. |
| `app/deps.py` | JWT primero; legacy opcional. |
| `app/routers/users.py` | `PUT /users/me/password`. |
| `.env` | `JWT_SECRET` (64 hex, permisos 600), `LEGACY_AUTH_ENABLED=0`. |

Migración de base de datos (aditiva, sin pérdida de datos):

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
```

## Usuarios existentes

Los 3 usuarios seed recibieron contraseña inicial. **Hay que cambiarlas** con
`PUT /users/me/password` o desde la BD:

| Correo | Rol | Contraseña inicial |
|---|---|---|
| `admin@driverapp.com` | ADMIN | `admin123` |
| `repartidor1@driverapp.com` | REPARTIDOR | `repartidor123` |
| `repartidor2@driverapp.com` | REPARTIDOR | `repartidor123` |

`bcrypt` guarda hashes con sal; no se puede recuperar la contraseña desde la BD,
sólo reemplazarla.

## Verificación ejecutada (contra el servidor real)

| Caso | Esperado | Obtenido |
|---|---|---|
| Login correcto | 200 + JWT | 200 (token de 161 caracteres) |
| Contraseña incorrecta | 401 | 401 |
| Correo inexistente | 401 | 401 |
| `GET /auth/session` con JWT | 200 | 200 |
| `GET /auth/session` sin token | 401 | 401 |
| Token viejo (`Bearer repartidor1`) | 401 | **401** (agujero cerrado) |
| JWT manipulado | 401 | 401 |
| `GET /orders/available` con JWT | 200 | 200 |
| `/earnings/summary`, `/users/me` | 200 | 200 |
| Registro nuevo | 201 | 201 |
| Registro duplicado | 409 | 409 |
| Contraseña < 6 caracteres | 422 | 422 |
| Cambio de contraseña correcto | 204 | 204 |
| Login con la contraseña nueva | 200 | 200 |
| Login con la contraseña vieja | 401 | 401 |
| Cambio con contraseña actual errónea | 400 | 400 |

## Respaldo y reversión

Antes de tocar nada se guardó en el servidor:

```
/root/backups/driverapp-backend-20261101-2309.tar.gz   (16 KB, 38 entradas)
/root/backups/driverapp-db-20261101-2309.sql           (26,7 KB, 7 tablas)
```

Revertir: restaurar el tar sobre `/root/driverapp/backend`, restaurar el dump y
`systemctl restart driverapp-api`. La columna `password_hash` puede quedarse: es
nullable y no rompe el código anterior.

## Hallazgo de seguridad pendiente

`driverapp-db` publica el puerto **`0.0.0.0:5432`**: Postgres escucha en todas las
interfaces, incluida la pública. Se recomienda `127.0.0.1:5432:5432` en el
`docker run`/compose del contenedor. No se cambió porque implica recrear el
contenedor de base de datos y eso no estaba autorizado en esta pasada.
