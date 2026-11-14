# Metodología de desarrollo utilizada

**Proyecto:** DriverApp — aplicación móvil de repartidores del restaurante SUMAQ
**Producto:** app Android (Kotlin + Jetpack Compose) + backend REST (FastAPI) + Firebase Cloud Messaging
**Periodo de ejecución registrado en el repositorio:** 13/09/2026 – 14/11/2026 (9 semanas)
**Documento:** análisis, selección justificada y aplicación de la metodología

---

## 1. Contexto y restricciones reales

Antes de elegir metodología hay que aceptar las condiciones en las que se ejecutó el
proyecto. Una metodología que ignore estas restricciones es teoría.

| Restricción | Valor real | Consecuencia metodológica |
|---|---|---|
| Equipo | 1 desarrollador (con asistencia de IA) | Sin ceremonias de equipo; todo el *overhead* documental recae en una persona |
| Plazo | 9 semanas, con hitos de entrega semanales | Sprints de **1 semana**; no caben iteraciones de 30 días |
| Requisitos | Enunciado académico cerrado (9 sprints + S08–S10) | El alcance no se negocia, pero el **detalle** sí evoluciona |
| Cambios de alcance reales | 2 cambios mayores a mitad del proyecto (ver §5) | La metodología debía tolerar re-planificación sin tirar trabajo |
| Backend | Servicio externo ya desplegado (FastAPI en `169.58.190.93:8084`) | El contrato se descubre, no se impone: exige análisis empírico (leer `openapi.json`) |
| QA | Sin tester dedicado | Las pruebas automáticas sustituyen al QA manual: **56 tests**, lint obligatorio |
| Cliente | Docente/revisor con revisiones periódicas | *Product Owner* parcial: prioriza y valida, pero no está disponible a diario |
| Hardware | Sin emulador estable (fallo de memoria del host) | La verificación se apoya en build + tests + pruebas de API reales, no en capturas de emulador |
| Documentación | Exigida como entregable evaluable | La documentación es un artefacto del sprint, no un anexo posterior |

## 2. Análisis de las metodologías contempladas

| Metodología | Cómo funciona | Encaje con las restricciones | Veredicto |
|---|---|---|---|
| **Cascada** (Waterfall) | Fases secuenciales; el alcance se congela al inicio | El enunciado es estable, pero los requisitos técnicos **no** se conocían al inicio: el contrato real del backend apareció en la semana 4 y obligó a cambiar auth y persistencia | ❌ Descartada |
| **Modelo incremental** | Entregas funcionales sucesivas sobre una arquitectura base | Buen encaje; es la base de lo aplicado | ✅ Parcial (adoptado como estructura) |
| **Espiral** | Iteraciones con análisis de riesgo formal en cada vuelta | El análisis de riesgo formal por iteración es desproporcionado para 9 semanas y 1 persona | ❌ Descartada |
| **Prototipado / RAD** | Prototipos desechables para acordar requisitos | Útil para la UI (hubo 2 rediseños: tema blanco/negro y logo), pero no gestiona el trabajo ni las pruebas | ⚠️ Complemento |
| **Scrum** | Sprints cortos, backlog priorizado, revisión al final de cada sprint | **Mejor encaje**: sprints semanales, backlog derivado del enunciado, revisión documentada por sprint | ✅ **Base elegida** |
| **Kanban** | Flujo continuo con límite de trabajo en curso | Útil para el día a día de una sola persona; sin tablero formal pierde trazabilidad | ⚠️ Complemento (flujo) |
| **XP (Extreme Programming)** | Pruebas primero, integración continua, código simple, revisiones | Se adoptaron **pruebas automatizadas por cada caso de uso y DAO** y build verde como puerta de paso | ✅ Prácticas adoptadas |
| **Scrumban / Lean** | Scrum con flujo Kanban y menos ceremonias | Es lo que en la práctica ocurrió; se documenta como adaptación explícita | ✅ Resultado |

### 2.1 Por qué **no** Cascada (la opción "cómoda" en un proyecto académico)

Cascada habría exigido congelar el diseño en la semana 1. En la semana 4, al leer
`/openapi.json` del backend real, se descubrió que:

- el login **no** validaba contraseña (`LoginIn` solo aceptaba `id_token`);
- los pedidos **no** tenían teléfono del cliente;
- el backend estaba en `dev_mode`, aceptando cualquier identificador.

Un modelo secuencial habría entregado un diseño inválido y una app que "funciona" solo
contra supuestos. El enfoque incremental permitió corregir en la semana siguiente sin
descartar el trabajo previo.

## 3. Metodología seleccionada y justificación

> **Scrum adaptado a un equipo unipersonal: sprints de 1 semana con entrega verificable,
> respaldo en prácticas XP (pruebas automatizadas y build verde como puerta) y un flujo
> Kanban implícito para el trabajo diario.**

### 3.1 Justificación

| Criterio | Por qué Scrum adaptado gana |
|---|---|
| Entrega de valor temprana | Cada semana termina con algo ejecutable (APK compilado, endpoint consumido, pantalla en Compose) |
| Tolerancia al cambio | Dos cambios de alcance a mitad de proyecto se absorbieron re-priorizando el sprint siguiente, no rehaciendo el plan completo |
| Carga documental | Se limita a 3 artefactos por sprint: objetivo, entregable y evidencia; el resto nace del código |
| Verificación objetiva | El criterio de "hecho" es **ejecutable**: `assembleDebug` + `testDebugUnitTest` + `lintDebug` en verde, más pruebas contra el backend real |
| Equipo mínimo | Sin `daily` de 15 min: la bitácora es el historial de commits (26 commits fechados) y el documento del sprint |
| Riesgo técnico | Los riesgos se descubren probando (contrato del backend, emulador, GMS del teléfono) y se resuelven en el sprint siguiente |

### 3.2 Adaptaciones concretas respecto a Scrum "de libro"

| Artefacto Scrum | Adaptación aplicada | Evidencia en el proyecto |
|---|---|---|
| Product Backlog | Tabla de 9 sprints del enunciado (`docs/03-sprints.md`) | 9 unidades con criterio de aceptación |
| Sprint Backlog | Checklist del sprint en el documento correspondiente | `docs/05`, `docs/06`, `docs/S08`, `docs/S09`, `docs/S10` |
| Sprint (2–4 semanas) | **1 semana**, comprimido por el plazo | Rango real de commits: 13/09 → 14/11 |
| Daily | Sustituido por commits atómicos con mensaje convencional (`feat`, `fix`, `docs`, `merge`) | 26 commits |
| Sprint Review | Documento de sprint con matriz de verificación (casos, esperado, obtenido) | `docs/auth-backend.md`, `docs/10-correcciones-ui.md` |
| Retrospectiva | Sección "errores encontrados y correcciones" de cada documento | Idem |
| Definition of Done | Build + tests + lint + documentación actualizada | 56 tests, 0 fallos, lint 0 errores |
| Scrum Master / PO | Rol combinado: el revisor prioriza, el desarrollador negocia el detalle | Cambios de alcance §5 |

## 4. EDT (Estructura de Desglose del Trabajo)

```
1. Gestión del proyecto
   1.1 Definición del alcance y sprints
   1.2 Repositorio y control de versiones
   1.3 Seguimiento semanal y re-planificación

2. Análisis
   2.1 Historias de usuario y criterios de aceptación
   2.2 Requerimientos no funcionales
   2.3 Modelo de datos (E-R) y contrato de API
   2.4 Análisis del backend real (openapi.json) y de riesgos

3. Diseño
   3.1 Arquitectura por capas (domain / data / ui) + Repository
   3.2 Diseño de interfaz (Compose) y navegación
   3.3 Diseño de persistencia local (Room, DataStore, cifrado)
   3.4 Diseño de integración (Retrofit, JWT, FCM)

4. Desarrollo (9 incrementos)
   4.1 Login y sesión            4.6 Persistencia local
   4.2 Pedidos disponibles       4.7 S08 API REST con Retrofit
   4.3 Mapa y GPS                4.8 S09 CRUD, errores y offline
   4.4 Ganancias                 4.9 S10 Firebase (FCM)
   4.5 Interfaz Compose

5. Pruebas
   5.1 Unitarias de dominio y casos de uso
   5.2 De DAO con SQLite en memoria (Robolectric)
   5.3 De ViewModel con Turbine
   5.4 De recursos (i18n es/en)
   5.5 De integración contra el backend real (curl/HTTP)
   5.6 Lint estático

6. Implementación y entrega
   6.1 APK de depuración y firma
   6.2 Publicación del código en GitHub
   6.3 Manual de despliegue y credenciales fuera del repositorio

7. Documentación y aceptación
   7.1 Documentos por sprint
   7.2 Informe de permisos y privacidad
   7.3 Metodología y evidencias
```

## 5. Cronograma de desarrollo (real, tomado del historial de git)

| Semana | Fechas | Fase | Entregable | Evidencia (commits) |
|---|---|---|---|---|
| 1 | 13/09 | Análisis | Repositorio creado, primer esqueleto | 1 commit |
| 2 | 20/09 | Análisis / Diseño | Historias de usuario y estructura del proyecto | 2 commits |
| 3 | 26–27/09 | Desarrollo (S1–S2) | Login, tema, logo, preferencia de saldo, correcciones de UI | 6 commits |
| 4 | 04/10 | Desarrollo (S5) | Rediseño blanco y negro + login SUMAQ | 1 commit |
| 5 | 10/10 | Análisis / Doc | Historias de usuario, RNF, sprints 5 y 6, criterios de aceptación | 4 commits |
| 6 | 11/10 | Análisis | Propuesta de backend (modelo E-R, API, seguridad) | 1 commit |
| 7 | 18/10 | Análisis | Informe HU-01 (login con persistencia del negocio) | 1 commit |
| 8 | 01/11 | Desarrollo (S3–S9, S08–S10) | Compose completo, Room, cola offline, REST, lint y Firebase | **8 commits** |
| 9 | 14/11 | Pruebas / Implementación | Correcciones de UI, Google Maps, i18n, credenciales FCM | 2 commits |

**Hitos verificables**

| Hito | Fecha | Estado |
|---|---|---|
| App compila y arranca (APK) | 01/11 | ✅ 17,0 MB |
| Autenticación contra el backend propio (JWT + bcrypt) | 01/11 | ✅ verificado con 16 casos HTTP |
| Migración completa a Jetpack Compose | 01/11 | ✅ 0 layouts XML |
| Persistencia local (Room + DataStore + cola offline) | 01/11 | ✅ 6 tests de DAO |
| Google Maps SDK integrado | 14/11 | ✅ key en manifest fusionado |
| Internacionalización es/en | 14/11 | ✅ 166/166 cadenas, 4 tests |
| Push FCM extremo a extremo | 14/11 | ✅ app y backend en el mismo proyecto (`proyectorepartidor-87835`); `senderId` alineado y `SenderIdMismatch` resuelto |

## 6. Aplicación de las 5 etapas de la solución técnica

| Etapa | Qué se hizo | Entregables | Criterio de salida |
|---|---|---|---|
| **Planificación** | EDT, cronograma de 9 sprints, definición de "hecho", repositorio y convención de commits | Este documento + `docs/03-sprints.md` | Backlog priorizado y entorno listo |
| **Análisis** | Historias de usuario, RNF, modelo E-R, contrato de API; **análisis del backend real** y de riesgos | `docs/01`, `docs/02`, `docs/07`, `docs/08`, `docs/auth-backend.md` | Requisitos trazables a un sprint |
| **Desarrollo** | 9 incrementos semanales; arquitectura por capas con inyección manual | 68 archivos Kotlin, 6.106 líneas | Cada sprint compila |
| **Pruebas** | Unitarias (dominio, casos de uso, ViewModel), DAO con SQLite real, i18n, lint, y pruebas HTTP contra el backend | 15 archivos de test, 1.234 líneas, **56 tests** | 0 fallos y 0 errores de lint para cerrar el sprint |
| **Implementación** | APK firmado en depuración, publicación en GitHub, credenciales fuera del repo, manual de configuración | APK + repo + `docs/12` | Artefacto instalable y reproducible |

## 7. Resultados presentados por etapa (incrementos)

| Sprint | Objetivo | Entregable verificable | Estado |
|---|---|---|---|
| 1 | Autenticación y perfil | Login con JWT del backend, perfil editable, preferencias | ✅ |
| 2 | Gestión de pedidos | Disponibles, aceptar/rechazar, mis pedidos, historial | ✅ |
| 3 | Mapa y GPS | Servicio en primer plano, ruta y ETA, Google Maps | ✅ |
| 4 | Ganancias y pulido | Resumen por día/semana/mes, gráficas, tema e idioma | ✅ |
| 5 | Interfaz y navegación | 6 pantallas en Jetpack Compose, `NavHost` | ✅ |
| 6 | Persistencia local | Room (5 tablas), DataStore, cifrado, esquema exportado | ✅ |
| S08 | API REST | Retrofit, DTOs, 39 operaciones del backend mapeadas | ✅ |
| S09 | CRUD, errores y conectividad | Cola offline con WorkManager, estados de carga y error | ✅ |
| S10 | Firebase | FCM: token, canal, permiso runtime, servicio de recepción | ✅ (envío pendiente) |

## 8. Evidencias del funcionamiento

> Las capturas se toman del APK instalado en un teléfono con Google Play Services. Los
> comandos para generarlas están al final de esta sección.

| # | Pantalla | Funcionalidad principal | Captura |
|---|---|---|---|
| 1 | Login | Autenticación contra el backend con correo y contraseña; validación en línea y mensajes de error | `capturas/01-login.png` |
| 2 | Pedidos disponibles | Lista de pedidos del restaurante SUMAQ con distancia y total; aceptar o rechazar | `capturas/02-pedidos.png` |
| 3 | Detalle / tarjeta de pedido | Cliente, dirección, importe, medio de pago y artículos del pedido | `capturas/03-detalle.png` |
| 4 | Mapa de entrega | Mapa de Google con marcadores de restaurante, cliente y repartidor; ETA y distancia en vivo | `capturas/04-mapa.png` |
| 5 | Ganancias | Resumen por día/semana/mes, historial y preferencia de ocultar saldo | `capturas/05-ganancias.png` |
| 6 | Perfil y preferencias | Datos del repartidor, vehículo, modo oscuro, idioma es/en, ubicación, notificaciones | `capturas/06-perfil.png` |
| 7 | Notificaciones | Avisos de pedido con canal propio y respeto de la preferencia de sonido | `capturas/07-notificaciones.png` |
| 8 | Modo sin conexión | Banda de estado sin red y contador de acciones pendientes de sincronizar | `capturas/08-offline.png` |

Para generarlas con el teléfono conectado por USB:

```bash
adb devices
adb install -r DriverApp-SUMAQ-debug.apk
adb shell screencap -p /sdcard/01-login.png && adb pull /sdcard/01-login.png docs/capturas/
```

## 9. Protección de datos en el producto y en la investigación

| Medida | Implementación | Evidencia |
|---|---|---|
| Credenciales nunca en claro | Hash **bcrypt (coste 12)** en `users.password_hash`; el hash no sale en ninguna respuesta | `docs/auth-backend.md` |
| Sesión firmada | JWT HS256 con `exp` de 7 días; cualquier token manipulado devuelve 401 | 16 casos HTTP verificados |
| Sesión cifrada en el dispositivo | `EncryptedSharedPreferences` (AES256-SIV/GCM); si el Keystore falla, cae a memoria y **no** escribe en claro | `TokenDataStore` |
| Copias de seguridad desactivadas | `android:allowBackup="false"` | `AndroidManifest.xml` |
| Secretos fuera del repositorio | `local.properties` ignorado; verificado con una búsqueda del prefijo de las claves de Google en el árbol remoto → 0 resultados (la búsqueda solo devuelve este mismo documento si se escribe el prefijo literal) | `docs/12` §2 |
| Permisos mínimos y justificados | 7 permisos, cada uno con propósito documentado; 8 descartados con motivo | `docs/11-permisos-y-privacidad.md` |
| Trazabilidad del dato personal | Tabla de datos tratados (origen, finalidad, retención, destinatario) | `docs/11` §2 |
| Términos y Política de Privacidad | Texto base redactado (Ley 29733) | `docs/11` §3 y §4 |
| Datos de prueba identificables | Teléfonos demo `+51 999 111 2NN` marcados como ficticios | `docs/10` |
| Riesgo abierto | El backend responde por **HTTP**, no HTTPS | Declarado en `docs/11` §6 |

En la investigación (documentación y este informe) no se incluyen credenciales,
tokens ni claves: los valores sensibles se referencian por nombre.

## 10. Repositorios y control de versiones

| Ítem | Valor |
|---|---|
| Software de control de versiones | **Git** |
| Repositorio de la app | <https://github.com/AlejandroConde26/driverapp-android> |
| Rama principal | `main` |
| Commits | 26 (13/09/2026 – 14/11/2026) |
| Convención de mensajes | `feat:`, `fix:`, `docs:`, `merge:` + cuerpo con el porqué |
| Backend | No versionado en este repositorio: vive en `/root/driverapp/backend` del servidor, con respaldo en `/root/backups/` y su contrato documentado en `docs/auth-backend.md` |
| Artefactos | `docs/` (15 documentos), `app/` (68 archivos Kotlin), `app/src/test/` (15 archivos de prueba) |

## 11. Riesgos identificados y cómo se gestionaron

| Riesgo | Impacto | Mitigación aplicada |
|---|---|---|
| El contrato del backend no coincidía con el supuesto | Alto | Se leyó `openapi.json` y se reescribió auth y modelo de pedidos en el sprint siguiente |
| Emulador inestable (memoria del host) | Medio | Verificación por build + tests + HTTP real, y registro de crash en el teléfono |
| Sin Play Console ni QA | Medio | Pruebas automatizadas como puerta de calidad (56) y `lintDebug` obligatorio |
| Proyecto Firebase distinto al de la app | Medio | Se detectó consultando el service account (`SenderIdMismatchError`) y se resolvió registrando la app en `proyectorepartidor-87835` y alineando `senderId` |
| Cambio de alcance a mitad de proyecto | Alto | Sprint de corrección dedicado y reescritura de la documentación afectada (no se dejó convivir la vieja) |
