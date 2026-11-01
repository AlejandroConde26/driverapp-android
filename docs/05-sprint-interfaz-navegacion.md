# Sprint 05 — Interfaz y navegación en Jetpack Compose (ejecutado)

**Estado: cumplido.** La capa de presentación completa está en Compose. No queda
ningún layout XML ni Fragment: `res/layout/` está vacío y `viewBinding` se eliminó
del build.

---

## 1. Qué se migró

| Antes (XML + Fragments) | Ahora (Compose) |
|---|---|
| 10 layouts: **2.922 líneas** de XML | **0 layouts**, UI declarativa en Kotlin |
| `LoginActivity` + `MainActivity` con 2 Activities | **1 Activity** (`MainActivity`) + `NavHost` |
| `supportFragmentManager.replace()` manual | `NavHost` declarativo (`ui/AppNav.kt`) |
| Bottom nav vía `BottomNavigationView` + `menu/bottom_nav.xml` | `NavigationBar` de Material 3 |
| `OrdersAdapter` + `RecyclerView` + `DiffUtil` | `LazyColumn` + `OrderCard` composable |
| `PieView` (Canvas en una View custom) | `EarningsChart` composable con `Canvas` |
| Toast custom inflado (`view_toast.xml`) | `ToastHost` con `AnimatedVisibility` |

## 2. Dependencias (Compose 1.5, compatible con Kotlin 1.9.0)

```kotlin
implementation(platform("androidx.compose:compose-bom:2023.08.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

```kotlin
buildFeatures { compose = true; viewBinding = false }
composeOptions { kotlinCompilerExtensionVersion = "1.5.0" }
```

El compilador de Compose va emparejado **exactamente** con la versión de Kotlin
(1.9.0 → 1.5.0). Subir Kotlin sin subir el compilador rompe el build, así que se dejó
la pareja fija y documentada.

## 3. Navegación

`ui/AppNav.kt` → `NavHost(startDestination = if (isLoggedIn) "orders" else "login")`

| Ruta | Pantalla | Notas |
|---|---|---|
| `login` | `LoginScreen` | al autenticar: `navigate("orders") { popUpTo("login") { inclusive = true } }` |
| `orders` | `OrdersScreen` | destino inicial con sesión |
| `map` | `TrackingScreen` | |
| `earnings` | `EarningsScreen` | |
| `profile` | `ProfileScreen` | logout: `navigate("login") { popUpTo(0) }` |
| `notifications` | `NotificationsScreen` | acceso desde la campana del header |

- El **back** del sistema lo gestiona el `NavHost` (sin `onBackPressed` deprecado).
- Cada destino crea su ViewModel con `viewModel(factory = container.xFactory())`, así
  el grafo de DI sigue siendo manual (`AppContainer`).
- `MainScaffold` unifica header (saludo, pill online/offline, campana), banner de
  conectividad, contenido y `NavigationBar`.

## 4. Catálogo de componentes (`ui/components/`)

| Componente | Archivo | Rol |
|---|---|---|
| `ValidatedTextField` | `Fields.kt` | `OutlinedTextField` + `isError` + `supportingText` |
| `PasswordField` | `Fields.kt` | campo con ojo mostrar/ocultar |
| `OrderCard` | `OrderCard.kt` | pedido con cliente, dirección, total, comisión, items |
| `OrderDetailSheet` | `OrderCard.kt` | detalle completo + acciones |
| `StatusPill` | `Common.kt` | pill online/offline con dot |
| `ToggleRow` | `Common.kt` | fila switch para las 5 preferencias |
| `PeriodPills` | `Charts.kt` | selector día/semana/mes/año (y pestañas de Órdenes) |
| `EarningsChart` | `Charts.kt` | dona + barras en `Canvas` |
| `ToastHost` | `Common.kt` | toast animado que resuelve textos i18n |
| `LoadingStateBox` / `ErrorStateBox` / `EmptyStateBox` / `BusyOverlay` | `Common.kt` | estados de pantalla |

El tema (`ui/theme/Theme.kt`) replica la paleta B/N en claro y oscuro y usa la familia
`Inter` de `res/font/`.

## 5. Validaciones implementadas

Se movieron al ViewModel para poder testearlas sin UI (`LoginViewModel.validateEmail`,
`validatePassword`), y la UI sólo pinta el `@StringRes` devuelto.

| Campo | Regla | Texto |
|---|---|---|
| Correo | vacío → error; con `@` debe cumplir `^[^@\s]+@[^@\s]+\.[^@\s]+$`; sin `@` se acepta usuario (modo dev) | `error_email_required` / `error_email_invalid` |
| Contraseña | ≥ 6 caracteres | `error_password_short` |
| Nombre | ≥ 2 caracteres | `error_name_short` |
| Teléfono | 7–15 dígitos | `error_phone_invalid` |
| Vehículo | selección obligatoria entre `Bicicleta / Moto / Auto` | — |

El botón de login queda **inhabilitado** mientras el formulario no sea válido
(`enabled = !loading && formValid`).

## 6. Criterios de aceptación

| Criterio | Estado | Evidencia |
|---|---|---|
| NavHost + NavigationBar con back funcional | ✅ | `ui/AppNav.kt`, `ui/main/MainScaffold.kt` |
| Componentes reutilizables sin duplicar el XML | ✅ | 10 componentes en `ui/components/` |
| Validaciones bloquean el envío inválido | ✅ | `LoginViewModelTest` (6 tests) |
| Carga/error con reintento en todas las pantallas | ✅ | `LoadingStateBox`, `ErrorStateBox`, `EmptyStateBox` |
| Tema B/N y es/en preservados | ✅ | `ui/theme/Theme.kt` + 166 claves por idioma |
| Mapa osmdroid vía `AndroidView` | ✅ | `TrackingScreen.kt` |
| Tests en verde tras la migración | ✅ | **51 tests, 0 fallos** |

## 7. Deuda conocida

- Sin emulador en la máquina de desarrollo, la verificación es **compilación + 51 tests
  unitarios**; falta un smoke test visual en dispositivo.
- `ProfileScreen.kt` (394 líneas) y `TrackingScreen.kt` (356) son las pantallas más
  grandes; si crecen conviene extraer sub-composables por sección.
- El bottom bar se construyó con `NavigationBar` + ítems propios porque
  `NavigationBarItem` de material3 1.1.1 no resolvía en este classpath
  (verificable en `MainScaffold.kt`); funcionalmente es el mismo patrón.
