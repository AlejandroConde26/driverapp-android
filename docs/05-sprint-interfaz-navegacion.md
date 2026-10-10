# DriverApp / SUMAQ — Sprint 05: Interfaz y navegación en Jetpack Compose (ejecutado)

**Encargo de práctica de campo:**

> *Ejecutar sprint de interfaz y navegación en Kotlin/Jetpack Compose.
> Implementar validaciones y componentes principales.*

Se ejecutó sobre la app Android del repartidor (paquete `com.driverapp.repartidor`).
La capa de negocio (`domain`) y de datos (`data`) se mantuvo intacta; el alcance
del sprint fue la capa de presentación.

---

## 1. Objetivo cumplido

Se llevó la interfaz de la app a **Jetpack Compose** con navegación declarativa,
se desarrollaron los **componentes principales** reutilizables y se **implementaron
las validaciones** de formularios (login y perfil).

## 2. Estado inicial del sprint

La app partía de **XML + Fragments con ViewBinding** y navegación manual. Sin Compose.

| Aspecto | Estado inicial |
|---|---|
| UI | XML (`res/layout/`): `activity_login`, `activity_main`, `fragment_orders`, `fragment_tracking`, `fragment_earnings`, `fragment_profile` + vistas reutilizables |
| Navegación | Manual: `MainActivity.kt` `supportFragmentManager.beginTransaction().replace()` + `bottomNav.setOnItemSelectedListener` (4 tabs). Sin Navigation Component |
| ViewModels | Uno por pantalla (`login`, `main`, `orders`, `tracking`, `earnings`, `profile`) — **reutilizados en Compose** |
| Componentes | `ConfirmDialog`, `PieView` (Canvas), status pill online/offline, custom toast (`view_toast`), `LoadingState`/`ErrorState`, `UiMessenger` |
| Validaciones | Inexistentes en UI: el login enviaba el formulario directo al backend (`LoginActivity.kt`) y mostraba el error por toast/`errorText`; el perfil no validaba campos |
| Infraestructura | DI manual (`AppContainer` + `ViewModelFactory`) — **reutilizado** |

## 3. Dependencias incorporadas

```
implementation(platform("androidx.compose:compose-bom:<versión estable>"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("androidx.navigation:navigation-compose:2.7.x")
```

- El mapa **osmdroid es ajeno a Compose** → se embebió con `AndroidView`.
- La sesión cifrada (`TokenDataStore`) se consumió igual desde los ViewModels.

## 4. Navegación Compose (implementada)

- **`NavHost`** con grafo: `login` → `main` (destino inicial según sesión).
- **`main`** es un `Scaffold` con **`NavigationBar`** de 4 destinos:
  `orders` (Órdenes), `map` (Mapa), `earnings` (Ganancias), `profile` (Perfil).
- Detalle de pedido como **destino anidado** `orders/{orderId}`.
- **Back**: `PopBackStack` al destino raíz; el botón volver del detalle replica el
  comportamiento previo.
- El header (`hello_user`/subtítulos), status pill y toast viven en el `Scaffold`
  de `main`, igual que en el `MainActivity` original.

## 5. Componentes principales (catálogo Compose entregado)

| Componente | Rol |
|---|---|
| `ValidatedTextField` | `OutlinedTextField` + validación, `isError` y `supportingText` por campo |
| `PasswordField` | Campo con ojo mostrar/ocultar (replica `LoginActivity.eyeToggle`) |
| `OrderCard` | Card de pedido: cliente, dirección, total, comisión, `cantidad × nombre — descripción` + notas |
| `OrderDetailSheet` | Detalle expandible heredado de `view_order_detail` |
| `StatusPill` | Pill online/offline con dot |
| `ToggleRow` | Fila switch para las 5 preferencias (modo oscuro, notif, ubicación, sonido, idioma es/en) |
| `PeriodPills` | Selector día/semana/mes/año (pestaña Ganancias) |
| `EarningsChart` | Gráfica circular reutilizando la lógica de `PieView` → `Canvas` |
| `ToastHost` | Toast custom animado (replica `showToast`) |
| `LoadingState` / `ErrorState` | Pantallas de carga y error con reintento (desde `ui/common`) |
| `ThemeB/N` + `strings es/en` | Tema blanco/negro y soporte de idioma preservados |

## 6. Validaciones implementadas

- **Login**
  - Correo: no vacío y formato con regex (`^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$`).
  - Contraseña: no vacía y mínimo **6** caracteres.
  - Error por campo (`isError` + `supportingText`); botón inhabilitado si el
    formulario es inválido.
  - Estados `Idle/Loading/Error/Success` reutilizando `LoginState` del
    `LoginViewModel`.
- **Perfil**
  - Nombre: no vacío (mín. 2 caracteres).
  - Teléfono: solo dígitos, **7–15**.
  - Vehículo (`CARRO|BICICLETA|MOTO`): selección requerida al guardar.
- **Preferencias**: switches booleanos (sin validación), idioma es/en.

## 7. Criterios de aceptación — definición de terminado del sprint

- [x] Navegación `NavHost` + `NavigationBar` con back funcional entre las 4 pestañas.
- [x] Componentes del catálogo reutilizables y sin duplicación con el XML migrado.
- [x] Validaciones de login y perfil bloquean el envío si el formulario es inválido.
- [x] Estados de carga/error con reintento en todas las pantallas.
- [x] Tema B/N y soporte es/en preservados.
- [x] Mapa osmdroid operativo vía `AndroidView`.
- [x] Los **16 tests** unitarios siguen en verde (sin cambios de contrato).

## 8. Trazabilidad

| Recurso | Lugar |
|---|---|
| Historias cubiertas | `docs/01-requisitos.md` — HU-01 (login), HU-02 (perfil), HU-03 (órdenes), HU-04 (mapa), HU-05 (ganancias), HU-06 (preferencias) |
| No funcionales | `docs/02-requerimientos-no-funcionales.md` (compatibilidad, idioma, mantenibilidad) |
| Arquitectura | `docs/04-arquitectura.md` (MVVM; la UI migró, `domain/data` se mantuvieron) |
| Sprints | `docs/03-sprints.md` (Sprint 5 integrado en la planificación) |