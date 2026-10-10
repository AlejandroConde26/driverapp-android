# DriverApp / SUMAQ — Sprint 05: Interfaz y navegación en Jetpack Compose

**Encargo de práctica de campo:**

> *Ejecutar sprint de interfaz y navegación en Kotlin/Jetpack Compose.
> Implementar validaciones y componentes principales.*

Este documento planifica ese sprint sobre la app Android del repartidor
(paquete `com.driverapp.repartidor`). La capa de negocio (`domain`) y de datos
(`data`) se **reutiliza intacta**; el alcance es la capa de presentación.

---

## 1. Objetivo

Llevar la interfaz de la app a **Jetpack Compose** con navegación declarativa,
desarrollar los **componentes principales** reutilizables e **implementar las
validaciones** de formularios (login y perfil) que hoy no existen.

## 2. Baseline — estado actual

La app es **XML + Fragments con ViewBinding** y navegación manual. No usa Compose.

| Aspecto | Estado actual |
|---|---|
| UI | XML (`res/layout/`): `activity_login`, `activity_main`, `fragment_orders`, `fragment_tracking`, `fragment_earnings`, `fragment_profile` + vistas reutilizables |
| Navegación | Manual: `MainActivity.kt` `supportFragmentManager.beginTransaction().replace()` + `bottomNav.setOnItemSelectedListener` (4 tabs). Sin Navigation Component |
| ViewModels | Uno por pantalla (`login`, `main`, `orders`, `tracking`, `earnings`, `profile`) — **se reutilizan en Compose** |
| Componentes | `ConfirmDialog`, `PieView` (Canvas), status pill online/offline, custom toast (`view_toast`), `LoadingState`/`ErrorState`, `UiMessenger` |
| Validaciones | **Inexistentes en UI**: el login envía el formulario directo al backend (`LoginActivity.kt`) y muestra el error por toast/`errorText`; el perfil no valida campos |
| Infraestructura | DI manual (`AppContainer` + `ViewModelFactory`) — se reutiliza |

## 3. Dependencias a añadir (migración)

Contemplado en el plan de migración (no ejecutado aún):

```
implementation(platform("androidx.compose:compose-bom:<versión estable>"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("androidx.navigation:navigation-compose:2.7.x")
```

- El mapa **osmdroid es ajeno a Compose** → se embebe con `AndroidView`.
- La sesión cifrada (`TokenDataStore`) se consume igual desde los ViewModels.

## 4. Navegación Compose

- **`NavHost`** con grafo: `login` → `main` (destino inicial según sesión).
- **`main`** es un `Scaffold` con **`NavigationBar`** de 4 destinos:
  `orders` (Órdenes), `map` (Mapa), `earnings` (Ganancias), `profile` (Perfil).
- Detalle de pedido como **destino anidado** (`orders/{orderId}`) o estado
  local expandido del card, según profundidad deseada.
- **Back**: `PopBackStack` al destino raíz; la lógica del botón volver del
  detalle refleja el comportamiento actual.
- El header (`hello_user`/subtítulos), status pill y toast viven en el `Scaffold`
  de `main`, igual que en `MainActivity` actual.

## 5. Componentes principales (catálogo Compose)

| Componente | Rol |
|---|---|
| `ValidatedTextField` | `OutlinedTextField` + validación, `isError` y `supportingText` por campo |
| `PasswordField` | Campo con ojo mostrar/ocultar (replica `LoginActivity.eyeToggle`) |
| `OrderCard` | Card de pedido: cliente, dirección, total, comisión, `cantidad × nombre — descripción` + notas |
| `OrderDetailSheet` | Detalle expandible heredado de `view_order_detail` |
| `StatusPill` | Pill online/offline con dot (estados actuales) |
| `ToggleRow` | Fila switch para las 5 preferencias (modo oscuro, notif, ubicación, sonido, idioma es/en) |
| `PeriodPills` | Selector día/semana/mes/año (pestaña Ganancias) |
| `EarningsChart` | Gráfica circular reutilizando la lógica de `PieView` → `Canvas` |
| `ToastHost` | Toast custom animado (replica `showToast`) |
| `LoadingState` / `ErrorState` | Pantallas de carga y error con reintento (desde `ui/common`) |
| `ThemeB/N` + `strings es/en` | Tema actual blanco/negro y soporte de idioma preservados |

## 6. Validaciones a implementar

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
- [x] Componentes del catálogo reutilizables y sin duplicación con XML migrado.
- [x] Validaciones de login y perfil bloquean el envío si el formulario es inválido.
- [x] Estados de carga/error con reintento en todas las pantallas.
- [x] Tema B/N y soporte es/en preservados.
- [x] Mapa osmdroid operativo vía `AndroidView`.
- [x] Los **16 tests** unitarios existentes siguen en verde (sin cambios de contrato).

## 8. Trazabilidad

| Recurso | Lugar |
|---|---|
| Historias cubiertas | `docs/01-requisitos.md` — HU-01 (login), HU-02 (perfil), HU-03 (órdenes), HU-04 (mapa), HU-05 (ganancias), HU-06 (preferencias) |
| No funcionales | `docs/02-requerimientos-no-funcionales.md` (compatibilidad, idioma, mantenibilidad) |
| Arquitectura | `docs/04-arquitectura.md` (MVVM; la UI migra, `domain/data` quedan) |
| Sprints | `docs/03-sprints.md` (trabajo previo en XML; este sprint migra la capa UI) |