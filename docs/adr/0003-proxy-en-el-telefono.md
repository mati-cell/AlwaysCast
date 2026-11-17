# ADR 0003 — Servidor proxy HTTP local en el teléfono

Estado: aceptado
Fecha: 2026-11-16

## Contexto

Muchos proveedores de video en la web protegen el acceso a sus archivos y segmentos HLS validando cabeceras HTTP (`Referer`, `Origin`, `User-Agent`, `Cookie`), o rechazando peticiones que provengan de navegadores no reconocidos. Al enviar un video al Chromecast mediante el Default Media Receiver (DMR), el dispositivo realiza las peticiones HTTP directamente utilizando su propio `User-Agent` (Chromium 70 sobre Linux armv7l) y sin enviar las cabeceras del reproductor web original, lo que resulta en respuestas `403 Forbidden` o fallos de reproducción.

Se evaluaron tres alternativas para la inyección de cabeceras:
1. **Proxy en la nube (VPS / Serverless)**: Centraliza la inyección, pero introduce costos operativos recurrentes, latencia, riesgos de saturación de ancho de banda y problemas de privacidad.
2. **Proxy en una computadora de la red local (Daemon de escritorio)**: Obliga al usuario a tener una PC encendida para castear desde el teléfono.
3. **Proxy HTTP embebido en la aplicación Android**: El teléfono actúa como servidor HTTP dentro de la red Wi-Fi local.

## Decisión

Implementar un servidor HTTP embebido en la aplicación Android (`:core:proxy`), ejecutado sobre **Ktor Server (motor CIO)** y coordinado mediante un **`CastForegroundService`**:

1. **Flujo de solicitud**:
   - La aplicación inspecciona el video y registra una sesión temporal con las cabeceras requeridas (`SessionRegistry`).
   - El Chromecast recibe una URL local: `http://<IP_LAN_TELEFONO>:<PUERTO>/s/<TOKEN>/media.ext`.
   - Cuando el Chromecast solicita el video, el proxy local reenvía la petición al CDN de origen utilizando `OkHttpClient`, inyectando las cabeceras originales (`Referer`, `Origin`, `User-Agent`) y canalizando la respuesta hacia el televisor.
2. **Soporte de `Range` (RFC 7233)**:
   - El proxy reenvía cabeceras `Range: bytes=start-end` y `Range: bytes=start-` de forma transparente hacia el origen, respondiendo con `206 Partial Content`, `Content-Range` y `Accept-Ranges: bytes`. Esto permite que el Chromecast realice búsquedas temporales (*seek*) y descargue el índice al final del archivo en videos MP4 sin *faststart*.
3. **Persistencia en segundo plano**:
   - El proxy corre en un servicio en primer plano de tipo `mediaPlayback`, manteniendo un `WakeLock` y una notificación persistente para asegurar que el sistema operativo (y capas agresivas como MIUI) no suspendan el hilo de red al bloquear la pantalla.

## Consecuencias

- **Autonomía y privacidad**: La solución no requiere servidores externos ni cuentas de usuario. Las URLs y cookies permanecen estrictamente entre el teléfono y el televisor.
- **Topología de red**: Requiere que el teléfono y el Chromecast estén en la misma red local sin aislamiento de clientes (AP Isolation desactivado en el router Wi-Fi).
- **Tráfico en el enlace inalámbrico**: El tráfico de video viaja del router al teléfono y del teléfono al Chromecast. En una red Wi-Fi doméstica moderna, un flujo de 3 a 8 Mbps (e incluso el caso extremo probado de 12 Mbps) representa menos del 10% del ancho de banda disponible, sin impacto perceptible en la reproducción.
- **Consumo energético contenido**: Al actuar únicamente como puente de red (I/O de sockets) sin transcodificación de video, el uso de CPU se mantiene mínimo.
