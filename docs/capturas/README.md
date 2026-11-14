# Capturas de pantalla

Aquí van las evidencias de funcionamiento del APK (`docs/13-metodologia-desarrollo.md` §8).

| Archivo | Pantalla |
|---|---|
| `01-login.png` | Login con correo y contraseña |
| `02-pedidos.png` | Lista de pedidos disponibles |
| `03-detalle.png` | Tarjeta de pedido (cliente, dirección, importe, artículos) |
| `04-mapa.png` | Mapa de Google con los tres marcadores y el ETA |
| `05-ganancias.png` | Resumen de ganancias por periodo |
| `06-perfil.png` | Perfil y preferencias |
| `07-notificaciones.png` | Bandeja de notificaciones |
| `08-offline.png` | Modo sin conexión con acciones pendientes |

## Cómo generarlas

Con un teléfono Android **con Google Play Services** y depuración USB activada:

```bash
adb devices
adb install -r DriverApp-SUMAQ-debug.apk

# iniciar sesión con repartidor1@driverapp.com / repartidor123
# navegar a cada pantalla y capturar:
adb shell screencap -p /sdcard/01-login.png && adb pull /sdcard/01-login.png docs/capturas/
```

Repetir cambiando el nombre del archivo para cada pantalla. Para el modo sin conexión,
activar el modo avión antes de entrar a la pestaña de pedidos.
