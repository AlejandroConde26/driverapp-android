# DriverApp / SUMAQ — Historias de usuario y criterios de aceptación

Parte de una plataforma de reparto de 3 componentes: **app Android** (repartidor),
**backend FastAPI** (`:8084`) y **dashboard web-admin** (`:3000`). Contexto SUMAQ:
la app ve pedidos del restaurante id=5.

Convenciones de estado del pedido: `PENDIENTE → ACEPTADO → EN_CAMINO → ENTREGADO`
(o `RECHAZADO`).

---

## HU-01 — Iniciar sesión como repartidor

**Como** repartidor, **quiero** iniciar sesión con mi correo y contraseña,
**para** acceder a mis pedidos y ganancias de forma segura.

**Criterios de aceptación**
- Se permite login con correo + contraseña. En modo dev la contraseña es libre y
  el backend usa la parte local del correo como UID.
- El token de sesión se almacena **cifrado** en `TokenDataStore` y se manda como
  `Authorization: Bearer <token>` en cada request.
- Si el login falla, se muestra error claro sin cerrar la app.
- Al iniciar sesión se registra el token FCM en el backend (`PUT users/me/fcm-token`).
- El botón de sesión ocultar/mostrar contraseña funciona (ojo del login).

## HU-02 — Gestionar el perfil

**Como** repartidor, **quiero** editar mis datos, teléfono y modo de vehículo,
**para** que el sistema y el admin me identifiquen correctamente.

**Criterios de aceptación**
- Se pueden editar nombre, teléfono y vehículo (`CARRO` / `BICICLETA` / `MOTO`).
- El cambio de vehículo se guarda en el backend (`updateVehicle`) y afecta la ETA calculada.
- Hay ver estadísticas anuales y cerrar sesión.

## HU-03 — Ver y aceptar pedidos disponibles

**Como** repartidor, **quiero** ver los pedidos disponibles de SUMAQ y aceptar o
rechazar cada uno, **para** decidir qué entregas tomo.

**Criterios de aceptación**
- La pestaña *Órdenes* muestra los pedidos `PENDIENTE` del restaurante (filtro `restaurant_id=5`).
- Cada pedido muestra cliente, dirección, total, comisión (`delivery_fee`) y
  platillos (`cantidad × nombre — descripción` + notas).
- Aceptar cambia el estado a `ACEPTADO`; rechazar lo devuelve a disponible para otros.
- Si no hay red, hay reintento y mensaje de sin-conexión.

## HU-04 — Seguir la entrega en el mapa

**Como** repartidor, **quiero** ver en el mapa la ubicación del restaurante y del
cliente y mi posición en vivo, **para** llegar al destino.

**Criterios de aceptación**
- El mapa (osmdroid) marca restaurante, cliente y posición GPS.
- Muestra distancia y ETA en vivo según velocidad del vehículo o del GPS.
- Marcar `EN_CAMINO` al salir; al llegar, `ENTREGADO` cierra el pedido y suma la comisión.

## HU-05 — Gestionar ganancias

**Como** repartidor, **quiero** ver mis ganancias por día/semana/mes/año y poder
ocultar o mostrar el saldo, **para** controlar lo que gano.

**Criterios de aceptación**
- Ganancias se agrupan por `day|week|month|year`.
- El botón ojo **muestra/oculta el saldo** (máscara `$••••••`); la preferencia
  persiste en `UserPreferences` (`show_balance`, default `true`).
- Las gráficas e historial se refrescan al completar una entrega.

## HU-06 — Preferencias de usuario

**Como** repartidor, **quiero** configurar notificaciones, ubicación, sonido,
idioma y modo oscuro, **para** que la app se adapte a mí.

| # | Preferencia | Rutas | Criterios de aceptación |
|---|---|---|---|
| 1 | Modo oscuro | `UserPreferences` (clave `dark`) → `AppCompatDelegate.setDefaultNightMode()` | Conmuta a `values-night/colors.xml`; persiste. |
| 2 | Notificaciones | `UserPreferences` (`notifications`) → `DriverFirebaseMessagingService` | Si está off, el push FCM no se muestra. |
| 3 | Ubicación en tiempo real | `UserPreferences` (`location`) → `MainActivity` (loop GPS) | Arranca/para el reporte `PUT users/me/location`. |
| 4 | Sonido de avisos | `UserPreferences` (`sound`) → `.setSilent(...)` | Si está off, la notificación llega silenciosa. |
| 5 | Idioma es/en | `UserPreferences` (`language`) → `values/` + `values-en/` | Se aplica con `setApplicationLocales()` al iniciar y con `recreate()`. |

## HU-07 — Recibir notificaciones push

**Como** repartidor, **quiero** recibir notificaciones FCM cuando hay novedades de
mis pedidos, **para** no estar mirando la app todo el tiempo.

**Criterios de aceptación**
- Canal de notificaciones `orders`; token registrado en `PUT users/me/fcm-token`.
- Respeta las preferencias de notificaciones y sonido.

## HU-08 — Admin: gestionar pedidos, menú y repartidores (dashboard web)

**Como** administrador, **quiero** crear pedidos con platillos del menú de SUMAQ,
ver indicadores y gestionar repartidores, **para** operar la plataforma.

**Criterios de aceptación**
- Crear pedido sélecciona platillos del catálogo; el servidor calcula el `total`
  a partir del menú (no se confía en precios del cliente).
- `GET /admin/stats/dashboard`: repartidores activos, pedidos en curso, pedidos hoy, ganancia hoy.
- `GET/POST /restaurants/{id}/menu` y `PUT/DELETE /restaurants/menu/{id}` para el catálogo.
- `GET /admin/drivers` y `PUT /admin/drivers/{id} { active }`.

## HU-09 — Notificaciones de estado (backend)

**Como** repartidor, **quiero** que el backend registre cada transición de pedido,
**para** que app y dashboard estén sincronizados.

**Criterios de aceptación**
- Endpoints `POST /orders/{id}/accept | reject | in-transit | complete`.
- `GET /orders/available` (PENDIENTE), `GET /orders/my` (ACEPTADO/EN_CAMINO),
  `GET /orders/history`.
- El repartidor solo opera sobre pedidos de su restaurante (filtro `restaurant_id`).