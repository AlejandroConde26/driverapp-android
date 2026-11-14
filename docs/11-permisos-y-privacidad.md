# Permisos y Privacidad — documentación

> **Alcance:** este documento es **solo documentación**. No se modificó la app para
> nada de lo que aquí se describe: son los permisos declarados/justificados, los
> descartados con su motivo, y el texto base de Términos y Condiciones y Política de
> Privacidad para entregar como anexo.

## 1. Permisos declarados en la app

Estado real de `app/src/main/AndroidManifest.xml` al cierre de esta entrega.

| Permiso | Tipo | Por qué lo pide la app | Cuándo se solicita | Si se niega | Nota Play |
|---|---|---|---|---|---|
| `INTERNET` | Normal | Consumir la API REST del backend y recibir FCM | Instalación | La app no sirve | Sin justificación adicional |
| `ACCESS_NETWORK_STATE` | Normal | Detectar conectividad para la cola offline (`NetworkMonitor`) | Instalación | No se encola bien; reintentos fallan | — |
| `ACCESS_FINE_LOCATION` | Peligroso | Ubicar al repartidor y trazar la ruta al cliente | Al activar «Ubicación en tiempo real» en Perfil | Queda desactivado el rastreo; el resto funciona | Requiere `rationale` y propósito claro |
| `ACCESS_COARSE_LOCATION` | Peligroso | Fallback cuando el usuario concede solo ubicación aproximada (Android 12+) | Junto con la anterior | Sin ubicación aproximada tampoco | Se declara con la fina |
| `POST_NOTIFICATIONS` | Peligroso (API 33+) | Avisar de pedidos nuevos y cambios de estado | Primer arranque / tras login | No llegan avisos visibles | Se comprueba antes de `notify()` |
| `FOREGROUND_SERVICE` | Normal | Mantener el reporte de ubicación como servicio en primer plano | Instalación | El rastreo se corta con la app en segundo plano | — |
| `FOREGROUND_SERVICE_LOCATION` | Normal (API 34+) | Tipar el servicio como `location` (obligatorio desde Android 14) | Instalación | `SecurityException` al arrancar el servicio | Debe coincidir con `foregroundServiceType` |

### 1.1 Permisos evaluados y **descartados** (con motivo)

| Permiso | ¿Se usa? | Motivo |
|---|---|---|
| `ACCESS_BACKGROUND_LOCATION` | **No** | El servicio en primer plano con `foregroundServiceType="location"` ya reporta con la pantalla apagada. Este permiso exige declaración especial y **video de revisión** en Play; añadirlo sin necesidad degrada la ficha. |
| `CAMERA` | **No** | Se retiró junto con el comprobante en Firebase Storage (fuera de alcance de S10). |
| `USE_FULL_SCREEN_INTENT` | **No** | Play lo restringe a apps de llamadas/alarma; el canal `orders` ya usa `IMPORTANCE_HIGH`. |
| `RECEIVE_BOOT_COMPLETED` | **No** | El reporte se reanuda al abrir la app (`MainActivity.onResume`). Si se quisiera automático tras reiniciar, este permiso + un `BOOT_COMPLETED` receiver. |
| `WAKE_LOCK` | **No** | `LocationManager` con `requestLocationUpdates` mantiene el GPS mientras el servicio vive; no se observó corte por doze. |
| `VIBRATE` | **No** | La vibración la aplica el canal de notificación si el usuario la habilita en el sistema. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | **No** | Play lo penaliza; en su lugar se documenta al usuario que excluya la app de la optimización de batería del fabricante. |
| `READ/WRITE_EXTERNAL_STORAGE` | **No** | No se escribe en almacenamiento compartido; la caché de Google Maps y Room van en el directorio privado. |

### 1.2 Flujo de solicitud (estados)

| Estado | Comportamiento esperado |
|---|---|
| No solicitado | El interruptor «Ubicación en tiempo real» está apagado; al activarlo se pide el permiso del sistema |
| Concedido | Se arranca `LocationForegroundService`; notificación persistente visible |
| Denegado (una vez) | Aviso `msg_location_denied`; la preferencia vuelve a `false`; se puede reintentar |
| Denegado permanentemente | Debe mostrarse un aviso con acceso a **Ajustes → Permisos → Ubicación** (hoy solo se muestra el aviso, no el acceso directo) |
| Concedido solo aproximada | Se acepta: el mapa funciona con menor precisión |
| Revocado con la app abierta | El servicio debe detenerse y avisar |

## 2. Privacidad — qué datos trata la app

| Dato | Origen | Finalidad | Almacenamiento | Retención | Destinatario |
|---|---|---|---|---|---|
| Correo y contraseña | Usuario | Autenticación | Backend: `users.email` + hash bcrypt | Mientras exista la cuenta | Backend propio |
| Token de sesión (JWT) | Backend | Autorizar cada request | **Cifrado** en el móvil (`EncryptedSharedPreferences`) | Hasta cerrar sesión / 7 días | Backend propio |
| Nombre, teléfono, vehículo | Usuario | Perfil y asignación de pedidos | Backend + caché local | Mientras exista la cuenta | Backend propio |
| **Ubicación (lat/lng)** | GPS del móvil | Asignar pedidos cercanos y trazar la entrega | Backend (`users.latitude/longitude`), última posición | Última posición; sin histórico | Backend propio |
| Token FCM | Firebase | Enviar notificaciones de pedido | Backend (`users.fcm_token`) | Hasta cambio de dispositivo | Google (Firebase) |
| Pedidos y ganancias | Backend | Operación diaria | Room local + backend | Local hasta sincronizar | Backend propio |
| Dirección y teléfono del cliente | Panel del restaurante | Entregar el pedido | Backend (`orders.client_*`) | Lo que defina el restaurante | Repartidor asignado |

**Terceros:** Google (Firebase Cloud Messaging) para el envío del push; el proveedor
de teselas del mapa para dibujar el mapa. La contraseña **nunca** se guarda en claro:
solo su hash bcrypt en el servidor.

### 2.1 Base legal y derechos (Perú, Ley 29733)

- **Base legal:** consentimiento del repartidor al aceptar los Términos y Condiciones
  y ejecución de la relación de servicio con el restaurante.
- **Derechos ARCO** (acceso, rectificación, cancelación, oposición): se ejercen por
  correo al responsable del tratamiento; el plazo de respuesta debe quedar definido
  en la política.
- **Menores:** el servicio es solo para mayores de edad.
- **Transferencias:** el backend está alojado en un servidor propio; no se venden
  datos a terceros.

## 3. Términos y Condiciones — texto base

> Borrador para revisión legal. Versión 1.0.

**1. Objeto.** DriverApp es la aplicación del repartidor del restaurante SUMAQ; permite
recibir pedidos, gestionar la entrega y consultar ganancias.

**2. Cuenta.** El acceso requiere credenciales entregadas por el restaurante. El
repartidor es responsable de su confidencialidad y de la veracidad de sus datos
(nombre, teléfono, vehículo).

**3. Geolocalización.** Al activar «Ubicación en tiempo real» el repartidor autoriza la
captura de su posición mientras la aplicación está en primer o segundo plano y el
reporte al sistema, con la única finalidad de asignar pedidos y permitir el
seguimiento de la entrega. Puede desactivarla en cualquier momento en Perfil.

**4. Uso aceptable.** Prohibido manipular estados de pedido, compartir la cuenta,
suplantar identidad, o usar la aplicación fuera de la operación del restaurante.

**5. Datos del cliente.** La dirección y el teléfono del cliente son datos operativos:
se usan solo para completar la entrega y no deben almacenarse ni difundirse fuera de
la aplicación.

**6. Disponibilidad.** El servicio puede interrumpirse por mantenimiento o fallos de
red; la aplicación opera con caché y cola offline y sincroniza al reconectar.

**7. Modificaciones.** El restaurante puede actualizar estos términos; la versión
vigente se muestra en la aplicación y su aceptación queda registrada.

**8. Legislación aplicable.** Legislación de la República del Perú.

## 4. Política de Privacidad — texto base

> Borrador para revisión legal. Versión 1.0.

**Responsable:** SUMAQ (restaurante) — completar razón social, RUC y correo de
contacto antes de publicar.

**Datos que recogemos:** identificación y contacto del repartidor (nombre, correo,
teléfono), datos del vehículo, ubicación mientras el reporte está activo, token de
notificaciones y actividad de pedidos.

**Para qué:** autenticar el acceso, asignar y seguir pedidos, calcular distancias y
ganancias, y enviar notificaciones de pedido.

**Con quién:** únicamente el personal autorizado del restaurante y el servicio de
notificaciones (Google Firebase). No se venden ni se ceden datos a terceros con fines
publicitarios.

**Cuánto tiempo:** mientras la cuenta esté activa. La última posición se sobrescribe;
no se guarda un histórico de recorridos.

**Seguridad:** contraseñas con hash bcrypt en el servidor, sesión cifrada en el
dispositivo, comunicación con token firmado y permisos de ubicación revocables.

**Derechos:** acceso, rectificación, cancelación y oposición escribiendo a
**[correo de contacto]**. También puede desinstalar la aplicación y solicitar la baja.

**Cambios:** se publicará la versión y la fecha de actualización; los cambios
sustanciales se avisan dentro de la aplicación.

## 5. Formulario «Data safety» de Play Store (mapeo)

| Sección del formulario | Respuesta |
|---|---|
| Ubicación aproximada / precisa | **Recogida** y **compartida** con el backend propio (no con terceros ajenos) |
| Finalidad de la ubicación | Funcionalidad de la app (asignación y seguimiento de pedidos) |
| Datos personales (nombre, correo, teléfono) | Recogidos; finalidad: gestión de cuenta |
| Datos financieros | No se recogen (las ganancias son de solo lectura) |
| Datos cifrados en tránsito | Sí (a definir: hoy el backend es HTTP en la IP `169.58.190.93:8084`, **pendiente TLS**) |
| Borrado de datos | A implementar: endpoint de baja o correo de contacto |
| Publicidad / seguimiento | No |

## 6. Pendiente si se lleva a la tienda

1. **TLS**: el backend responde por HTTP; Play exige declarar cifrado en tránsito y el
   incumplimiento es motivo de rechazo. Necesita dominio + certificado (Caddy/Let's
   Encrypt) delante de `:8084`.
2. **URL pública de la política de privacidad** (HTTPS) para la ficha de la tienda.
3. **Borrado de cuenta** dentro de la app o por formulario público.
4. **Justificación de permisos** en la ficha (textos de la §1 en es/en).
5. **Registro de aceptación** de Términos (versión + fecha) si se quiere evidencia:
   hoy la app solo muestra el texto, no lo guarda.
