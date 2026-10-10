# DriverApp / SUMAQ — Planificación de sprints (4 sprints)

Sistema de 3 componentes: app Android (repartidor), backend FastAPI (`:8084`),
dashboard web-admin (`:3000`). Las historias de usuario referenciadas viven en
`docs/01-requisitos.md`; los no funcionales en `docs/02-requerimientos-no-funcionales.md`.

Estado del pedido: `PENDIENTE → ACEPTADO → EN_CAMINO → ENTREGADO` (o `RECHAZADO`).

---

## Sprint 1 — Base: login, sesión y perfil
**Objetivo:** repartidor autenticado y con perfil usable + backend/auth listos.

- [x] HU-01 Iniciar sesión (token cifrado local, Bearer, registro FCM).
- [x] HU-02 Gestionar perfil (datos, teléfono, vehículo `CARRO|BICICLETA|MOTO`).
- [x] HU-09 (parcial) Auth + lógica de usuario en backend (`auth`, `users`).
- Entregable: APK con login/perfil + backend `:8084` levantado.
- Referencia: commits `fcc8c9e`, `1897594` (refactor MVVM) del repo.

**Definición de terminado**
- Login y perfil funcionando en modo dev; sesión cifrada; tests de
  `LoginUseCase`, `LoginViewModel`, `ProfileViewModel` en verde.

---

## Sprint 2 — Órdenes y flujo de entrega
**Objetivo:** repartidor acepta/rechaza pedidos y el flujo de estados opera de punta a punta.

- [x] HU-03 Ver y aceptar/rechazar pedidos disponibles (PENDIENTE, filtro `restaurant_id=5`).
- [x] HU-09 Backend: `accept | reject | in-transit | complete`, `available`, `my`, `history`.
- [x] HU-08 (parcial) Admin crea pedidos con cálculo de total desde el menú.
- Entregable: Órdenes con lista Asignados/Historial + detalle de platillos y notas.
- Referencia: commits SUMAQ `34320c4`, demo de pedidos y menú en BD.

**Definición de terminado**
- Pedido nuevo creado desde el dashboard aparece en la app y completa el ciclo
  hasta `ENTREGADO`; `PedidoRepositoryImplTest` en verde.

---

## Sprint 3 — Mapa, GPS y notificaciones
**Objetivo:** seguimiento en vivo y push FCM respetando preferencias.

- [x] HU-04 Mapa osmdroid (restaurante, cliente, GPS) con distancia y ETA en vivo.
- [x] HU-07 Notificaciones FCM (canal `orders`, token vía `PUT users/me/fcm-token`).
- [x] Ubicación en tiempo real: loop GPS en `MainActivity` → `PUT users/me/location`.
- Entregable: Mapa operativo + push con sonido configurable.
- Referencia: `data/hardware/gps/`, `data/firebase/`, `ui/tracking/`.

**Definición de terminado**
- Mapa refresca datos con el vehículo configurado; notificación llega silenciosa si
  `sound=false` y no llega si `notifications=false`.

---

## Sprint 4 — Ganancias, preferencias y pulido
**Objetivo:** datos de negocio, personalización y cierre de producto.

- [x] HU-05 Ganancias por `day|week|month|year` + gráficas + **botón ojo** (máscara `$••••••`, `show_balance`).
- [x] HU-06 Cinco preferencias centralizadas en `UserPreferences`.
- [x] Logo B/N adaptativo + icono de notificación.
- [x] Documentación completa (`docs/`).
- Entregable: release funcional demo + README/documentación.
- Referencia: commits `3c96e78`, `f28e159`, `bd22bc3` y `docs/`.

**Definición de terminado**
- 16/16 tests de la app y 13/13 del backend en verde; docs publicados en el repo;
  demo con pedidos SUMAQ visible desde la app.

---

## Trazabilidad a documentación
| Artefacto | Documento |
|---|---|
| Historias + criterios de aceptación | `docs/01-requisitos.md` |
| Requerimientos no funcionales | `docs/02-requerimientos-no-funcionales.md` |
| Planificación de sprints | `docs/03-sprints.md` |
| Arquitectura MVVM + modelo de datos local | `docs/04-arquitectura.md` |