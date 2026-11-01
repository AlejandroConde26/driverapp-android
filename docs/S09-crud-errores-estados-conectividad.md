# S09 — Operaciones CRUD con API REST, manejo de errores, estados de carga y conectividad

**Estado: cumplido.** El repartidor opera CRUD real sobre dos recursos (pedidos y
avisos), cada error HTTP tiene su propio estado de UI, todas las pantallas tienen
carga/error/vacío, y las transiciones hechas sin red se encolan y se reenvían solas.

---

## 1. Matriz CRUD

| Recurso | Create | Read | Update | Delete |
|---|---|---|---|---|
| **Pedidos** | — (los crea el admin) | `GET /orders/available`, `/my`, `/history`, `/{id}` | `POST /orders/{id}/accept\|reject\|in-transit\|complete` → cambia `status` | `POST /orders/{id}/reject` lo saca de la lista del repartidor |
| **Perfil** | — | `GET /users/me` | `PUT /users/me`, `/vehicle`, `/status`, `/location`, `/fcm-token` | — |
| **Avisos** (`S09`) | — (el backend los genera al cambiar el estado) | `GET /notifications/my` | `PUT /notifications/my/read` | descarte local en memoria |
| **Caché local** | `upsert` de pedidos/ganancias al refrescar | lectura offline desde Room | `updateStatus` + `bucket` | `DELETE` por bucket y TTL de historial |

Que `C` no exista para el repartidor es una decisión de dominio, no un hueco: quien
crea pedidos es el dashboard del admin (`POST /admin/orders`). La app sí crea
registros propios: cada transición sin red se inserta en `pending_actions`
(`pending_actions` es el "C" del repartidor) y se borra al confirmarse.

## 2. UI de avisos (pantalla nueva)

`ui/notifications/NotificationsScreen.kt` + `NotificationsViewModel`:

- lista con badge **Nueva/Leída**,
- acción "Marcar todas como leídas" (`PUT /notifications/my/read`),
- descarte de un aviso (desaparece de la lista y no vuelve en el siguiente refresh),
- reintento ante error.

## 3. Manejo de errores

`ui/common/UiStates.kt` define el estado que consume la UI:

```kotlin
sealed interface ErrorState {
    data object None
    data class SinConexion(text = "Sin conexión. Se muestran los datos guardados.")
    data class NoAutorizado(text = "Tu sesión expiró. Vuelve a iniciar sesión.")
    data class NoEncontrado(text = "El pedido ya no está disponible.")
    data class Validacion(text = "El servidor rechazó los datos.")
    data class Servidor(text = "El servidor falló. Reintenta en unos minutos.")
    data class Message(text: String)
}
```

`Throwable.toErrorState()` mapea desde `ApiFailure`. Ningún `catch` de la app muestra
el mensaje crudo del `HttpException`: el texto sale de recursos traducibles
(`UiMessage.Res`) o del `detail` del backend cuando es un 422.

## 4. Estados de carga

Todas las pantallas usan el mismo par de componentes:

| Componente | Archivo | Uso |
|---|---|---|
| `LoadingStateBox` | `ui/components/Common.kt` | spinner + "Cargando…" |
| `ErrorStateBox` | `ui/components/Common.kt` | mensaje + botón "Reintentar" |
| `EmptyStateBox` | `ui/components/Common.kt` | vacío con acción opcional |
| `BusyOverlay` | `ui/components/Common.kt` | bloquea la pantalla mientras se envía una transición |

Cada ViewModel publica `loading: StateFlow<LoadingState>` y `error: StateFlow<ErrorState>`
(`OrdersViewModel`, `EarningsViewModel`, `NotificationsViewModel`, `TrackingViewModel`).

## 5. Conectividad y cola offline

### 5.1 Monitor

`data/network/NetworkMonitor.kt` observa `ConnectivityManager` con
`registerDefaultNetworkCallback` (no sondea: no consume batería) y expone
`Flow<Boolean>`. `MainViewModel.connectivity` lo publica y `OfflineBanner`
(`ui/components/Common.kt`) lo pinta en todas las pestañas.

### 5.2 Cola de transiciones

```
sin red al pulsar "Salí del restaurante"
  → PedidoRepositoryImpl.transition() captura ApiFailure.NoConnection   (PedidoRepositoryImpl.kt:80)
  → SyncQueue.enqueue() inserta en pending_actions y programa el Worker  (SyncQueue.kt:24)
  → la caché se actualiza de forma optimista (el pedido pasa a EN_CAMINO)
  → OrderSyncWorker reenvía en orden FIFO cuando vuelve la red             (OrderSyncWorker.kt:17)
  → 4xx descarta la acción (no bloquea la cola) · sin red → Result.retry()
```

El contador de pendientes (`repo.pendingActions`) se muestra en el banner
("N cambios por enviar"), así el repartidor sabe que su cambio no se ha perdido.

**Decisión explícita:** las posiciones GPS **no** se encolan. Encolar un histórico de
posiciones sin red solo aporta datos obsoletos; se descarta la lectura y se reenvía la
siguiente.

## 6. Verificación

| Test | Qué demuestra |
|---|---|
| `ApiErrorMapperTest` (6) | cada código HTTP cae en su `ApiFailure` |
| `OrdersViewModelTest` (6) | listas desde repositorio, `ErrorState.SinConexion` sin red, accept/reject delegan, detalle se abre y se cierra |
| `TrackingViewModelTest` (4) | `EN_CAMINO`, completar refresca historial, fallo de red no rompe la pantalla |
| `PedidoRepositoryImplTest` (8) | sin red **encola** y aplica optimista; un 404 **no** se encola |
| `OrderDaoTest` (6) | `pending_actions` persiste la acción y el contador la refleja |

Comando: `./gradlew :app:testDebugUnitTest` → 51 tests, 0 fallos.
