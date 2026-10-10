# DriverApp / SUMAQ — Requerimientos no funcionales

## 1. Rendimiento
- La app arranca y navega sin esperas perceptibles; el mapa (osmdroid) no bloquea el hilo principal.
- Consultas de pedidos y ganancias se hacen asíncronas (Retrofit/OkHttp + corrutinas).
- El backend responde los endpoints de la API en tiempos razonables para una flota pequeña.
- El historial de ganancias se cachea/sirve por `period` sin recalcular en cada request.

## 2. Seguridad
- Token de sesión del repartidor **cifrado** en disco (`EncryptedSharedPreferences`,
  `MasterKeys.AES256_GCM_SPEC`) en `data/local/TokenDataStore.kt`.
- Autenticación tipo Bearer en cada request. En modo real el token es un JWT de
  Firebase (firmado RS256); en `DEV_MODE` se acepta la parte local del correo
  como UID (solo entornos de pruebas).
- No se persisten secretos ni credenciales en el repo (los `FIREBASE_*` están vacíos
  en `build.gradle.kts`; token de push nunca en `git`).
- El costo del pedido lo **calcula el servidor** desde el menú (nunca se confía en el precio del cliente).

## 3. Disponibilidad y robustez
- Backend servido con uvicorn en `:8084` con health endpoint (`driverapp-api`).
- Postgres 16 vía docker compose (`driverapp-db`, puerto `5432`).
- Dashboard Vite en `:3000` con proxy al backend.
- Modo sin conexión: reintentos y avisos claros en Órdenes y Mapa.

## 4. Notificaciones (FCM)
- Canal `orders`; el servicio `DriverFirebaseMessagingService` **respeta** las
  preferencias de notificaciones (`notifications`) y de sonido (`sound` → `.setSilent`).

## 5. Localización e idioma
- Soporte **español e inglés**: recursos `res/values/` y `res/values-en/`.
- Aplicación del idioma con `AppCompatDelegate.setApplicationLocales()` al arrancar
  y `recreate()` al cambiar; afecta bottom-nav, cabeceras y Preferencias.
- Formato de moneda `$` con separadores (es-MX en dashboard).

## 6. Compatibilidad y build
- Android: Kotlin 1.9, minSdk 26, target/compile SDK 34, AGP 8.4.0, Gradle 8.6,
  JDK 17–21 (build con 21 verificado).
- Build reproducible con wrapper: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
- **16 tests unitarios** en verde (MockK + Turbine + coroutines-test) y **13 tests** de backend (pytest).

## 7. Mantenibilidad
- Arquitectura MVVM limpia con separación `domain` (Kotlin puro) / `data` / `di` / `ui`.
- DI **manual** (sin Hilt) vía `AppContainer` + `ViewModelFactory`.
- Backend modular: `models/`, `routers/`, `schemas/`, `services/`.
- Documentación en el README del repo y en `docs/`.

## 8. Operativa / puertos
- **No interferir** con puertos de otros proyectos: 8080–8083, 8090, 8200.
- Solo usamos: backend `8084`, dashboard `3000`, Postgres `5432`.