# AlwaysCast — Diseño

Fecha: 2026-11-16
Estado: borrador para aprobación

## 1. Intención y porqué

Un Chromecast de primera generación (2013) ya no recibe apps de la mayoría de los
servicios y, con el tiempo, va a perder también las que le quedan. El dispositivo
sigue siendo un televisor perfectamente usable: el problema no es la pantalla, es
que ya no existe la capa de software que conecte "el video que quiero ver" con "el
aparato que lo muestra".

AlwaysCast es esa capa. Toma un video que el usuario ya está viendo —en una página
web, en otra app, o en el propio teléfono— y lo manda al Chromecast usando el
receptor por defecto de Google Cast, que el gen1 todavía soporta.

Uso objetivo: sentado en el sillón, con el celular en la mano, en la misma red Wi-Fi
que la tele. Unas pocas pulsaciones entre "lo encontré" y "se está viendo".

Es también una pieza de portafolio: arquitectura modular, tests, CI, documentación
y decisiones registradas. La calidad del código es un requisito, no un extra.

## 2. Objetivos

- **O1.** Detectar el media de una página que el usuario navega dentro de la app y
  castearlo, aunque el sitio no tenga botón de Cast.
- **O2.** Recibir un video compartido desde otra app (`ACTION_SEND` con `video/*`) y
  castearlo.
- **O3.** Aceptar una URL pegada a mano y un archivo local del teléfono.
- **O4.** Resolver el caso en que el origen exige `Referer`, `Cookie` o
  `User-Agent`: un proxy HTTP local los inyecta y sirve el stream al Chromecast.
- **O5.** Seguir casteando con la pantalla apagada y la app en segundo plano.
- **O6.** Funcionar sin servidor, sin cuenta y sin telemetría.
- **O7.** Código y documentación dignos de portafolio: módulos separados, tests en
  las piezas con lógica, CI en verde, ADRs de las decisiones no obvias.

## 3. No objetivos

Explícitos, para que nadie los confunda con bugs:

- **N1.** Contenido con DRM (Widevine, PlayReady). No hay software que suba el nivel
  de seguridad del dispositivo.
- **N2.** HEVC/H.265, AV1, 4K. El gen1 no los decodifica.
- **N3.** Players basados en MSE (`blob:`) sin URL de origen. Sin URL no hay nada que
  mandarle a la tele.
- **N4.** Inyectar el botón de Cast dentro de apps de terceros.
- **N5.** Listas de proveedores o scrapers por sitio. La app no conoce ningún sitio.
- **N6.** Cuentas, sincronización en la nube, analítica, crash reporting.
- **N7.** iOS, TV, escritorio (ver "Diferido").
- **N8.** Streams en vivo (HLS live, eventos deportivos). Solo VOD: hay duración, hay fin,
  hay barra de progreso.
- **N9.** Subtítulos externos (WebVTT) y selección de pistas de audio. Los subtítulos
  quemados en la imagen vienen con el video y no son asunto nuestro.

## 4. Anti-objetivos

Lo que convertiría esto en un fracaso aunque técnicamente funcione:

- **A1.** Necesitar la computadora prendida para ver una película.
- **A2.** Volverse una matriz de parches por sitio, imposible de mantener y con
  exposición legal creciente.
- **A3.** Requerir root, VPN de captura o instalación de certificados.
- **A4.** Una arquitectura que dé vergüenza mostrar: un `Activity` de 2000 líneas.
- **A5.** Prometer "anda con todo" y fallar en silencio. Es preferible decir "esto no
  lo puedo castear y este es el motivo".

## 5. Restricciones

### 5.1 Del dispositivo (Chromecast gen1)

Envelope real, **medido** el 16-nov-2026 contra el equipo del usuario (firmware
`CrKey/1.36.157768`, receptor Default Media Receiver):

| Caso | Resultado |
|---|---|
| MP4 / H.264 High + AAC | Reproduce |
| MP4 **sin** faststart (`moov` al final) | Reproduce; el receptor pide el final del archivo por `Range` |
| MP4 / H.264 + AC3 | Reproduce |
| WebM / VP8 + Vorbis | Reproduce |
| HLS (`.m3u8`) sin cifrar | Reproduce |
| HLS con AES-128 | Reproduce; pide `enc.key` como request aparte |
| **MKV / H.264 + AAC** | **Reproduce** — no hace falta remux |
| MP4 1080p a 12 Mbps | Reproduce sin cortes sobre Wi-Fi 2.4 GHz |
| **MP4 HEVC/H.265** | **No reproduce** — el receptor acepta el `loadMedia` y cierra el media sin llegar a `PLAYING` |
| AV1, 4K | Sin probar; se asume que no |

Consecuencias medidas, que reemplazan las suposiciones de la primera versión:

- El techo de 1080p se confirma, y el ancho de banda de 2.4 GHz alcanza para 12 Mbps.
- El remux de MKV **sale del alcance**: el receptor lo demuxea solo.
- El proxy está obligado a soportar `Range`, incluido `bytes=N-` abierto: el propio
  televisor lo usa para leer el índice de un MP4 mal empaquetado.
- El receptor usa un navegador Chromium 70 sobre Linux armv7l; las URLs de página
  (por ejemplo `ok.ru/video/...`) no reproducen nada, que es exactamente el motivo por
  el que hace falta detección dentro del navegador y no un simple campo de URL.

### 5.2 De la plataforma

- Cast Sender SDK v3 requiere Google Play Services en el teléfono (el Xiaomi objetivo
  lo tiene). El receptor usado es el Default Media Receiver (`CC1AD845`), que no
  requiere registrar nada ni hostear nada.
- El proxy debe correr en un foreground service: Android y MIUI matan procesos en
  segundo plano de forma agresiva. En MIUI hay que guiar al usuario a desactivar la
  optimización de batería para la app.
- El Chromecast tiene que poder alcanzar la IP del teléfono: misma red, sin
  aislamiento de clientes (AP isolation) en el router.
- **Bandas.** El gen1 tiene radio solo de 2.4 GHz: vive en el SSID de 2.4 y no hay forma
  de moverlo a 5 GHz. Confirmado en el equipo real: la red de 5 GHz nunca aparece en el
  asistente, ni con la versión antigua de Google Home. Si el teléfono está en 5 GHz, la
  mayoría de los routers puentean ambas bandas en la misma subred y el descubrimiento
  mDNS funciona; cuando no lo hacen, falla en silencio. Consecuencia: el diagnóstico de
  red es requisito de primera clase, no un extra.
- **Ancho de banda.** Si el teléfono también está en 2.4, cada byte cruza el canal dos
  veces (AP→teléfono y teléfono→tele) y el techo efectivo se reduce a la mitad. 1080p
  H.264 de 5–8 Mbps va bien; un rip de 15 Mbps va a tartamudear. Configuración
  recomendada y a verificar en la fase 0: teléfono en 5 GHz, tele en 2.4.

### 5.3 De FFmpeg en Android

`ffmpeg-kit` fue retirado en 2025 y ya no tiene builds oficiales. Cualquier
transcodificación en el teléfono queda condicionada a alternativas mantenidas.
Consecuencia de diseño: **el MVP no transcodifica**. La fase 0 mostró que MKV no lo
necesita; lo único que quedaría afuera por codec es HEVC/AV1, y para eso la respuesta
correcta no es recodificar en un teléfono sino declararlo no soportado.

### 5.4 Legales y de publicación

- La app es agnóstica al contenido: no incluye sitios, no scrapea, no rompe DRM, no
  evita paywalls. Es un navegador con un botón de Cast, igual que un reproductor
  local. El uso que el usuario haga del contenido es responsabilidad suya, y así se
  declara en el README.
- Distribución primaria: GitHub Releases / F-Droid. Google Play es opcional y puede
  rechazar la app por su categoría; no se diseña alrededor de Play.
- Licencia: Apache-2.0 (grant de patentes, buena señal para portafolio).

## 6. Enfoques considerados

### A. App Android sola, sin recodificar — **elegido**

Navegador propio con `WebView` que intercepta requests, más receptor de "compartir",
más entrada manual. Proxy HTTP en el teléfono que inyecta headers y reescribe
playlists. Cast al Default Media Receiver.

- A favor: cero infraestructura, publicable, todo gratis, un solo dispositivo, y es
  el flujo que el usuario ya tiene (celular en el sillón).
- En contra: se limita al envelope de 5.1; HEVC y MKV quedan afuera.

### B. A + servicio opcional en la LAN (ffmpeg)

Cuando el media no es compatible, la app delega a un daemon en la computadora que
remuxea/transcodea y devuelve una URL.

- A favor: cubre todo, incluido MKV y HEVC.
- En contra: exige PC prendida, apareamiento, descubrimiento, y multiplica la
  superficie de instalación. Es el A1 de los anti-objetivos.
- Se conserva como evolución posible detrás del puerto de 7.2.

### C. Servicio de escritorio con UI web en el teléfono

- A favor: lo más simple de programar.
- En contra: obliga a prender la computadora para ver una película. Descartado.

## 7. El cómo, ya decidido

### 7.1 Stack

- Kotlin, Jetpack Compose, single-activity, unidirectional data flow.
- MVVM + capas: `presentation` (Compose + ViewModel), `domain` (casos de uso,
  modelos), `data` (repositorios, fuentes).
- Hilt para inyección de dependencias; Coroutines + Flow para asincronía.
- Room para historial; DataStore para preferencias.
- Ktor Server (CIO) embebido para el proxy; OkHttp para sondeo de medios.
- `play-services-cast-framework` para el envío; `MediaMetadataRetriever` /
  `MediaExtractor` para leer codecs sin descargar el archivo entero.
- minSdk 26, target el último estable. Solo teléfono (portrait y landscape).

### 7.2 Módulos

```
:app
:core:model            modelos de dominio, sin dependencias de Android
:core:common           Result, dispatchers, utilidades
:core:designsystem     tema, componentes
:core:network          OkHttp, sondeo de medios, sniffing de tipos
:core:proxy            Ktor embebido, inyección de headers, reescritura HLS
:core:cast             envoltorio del Cast SDK, sesión y estados
:core:data             Room, DataStore, repositorios
:feature:browser       WebView + detección + UI de "encontramos esto"
:feature:cast           pantalla de conexión y control remoto
:feature:local         archivos del dispositivo + entrada manual de URL
:feature:history       historial
```

Regla de dependencias: `feature` → `core`; nunca `feature` → `feature`; `core:model`
no depende de nadie.

### 7.3 Flujo

```
Link pegado ──┐
Compartir video ──┼──► MediaCandidate (url, headers, tipo)
WebView sniffing ──┘            │
                                ▼
                     MediaProbe (¿es casteable?)
                       │            │
              compatible         no compatible
                       │            │
                       ▼            ▼
              CastableSource    explicación + alternativas
                       │
                       ▼
   Proxy local (headers, token, reescritura .m3u8)
                       │
                       ▼
        Cast SDK → Default Media Receiver → TV
```

### 7.4 El puerto que deja la puerta abierta a B

Un único contrato en `:core:model`:

```kotlin
interface MediaSourceResolver {
    suspend fun resolve(candidate: MediaCandidate): ResolvedSource
}
```

`ResolvedSource` es una URL HTTP que el Chromecast puede abrir, más su content type
y metadatos. El MVP tiene una implementación (`LocalProxyResolver`). Un eventual
`RemoteTranscoderResolver` se agrega al grafo de Hilt sin tocar las features.

### 7.5 Detección en el navegador

1. `WebViewClient.shouldInterceptRequest`: captura cada request; se queda con
   `m3u8`, `mp4`, `webm`, `mp3` y los `Content-Type` de video.
2. JS inyectado en `onPageStarted`: observa `fetch`, `XMLHttpRequest`, asignaciones a
   `HTMLMediaElement.src` y `<source>`; reporta por `@JavascriptInterface`. Cubre lo
   que el intercept nativo se pierde por service workers.
3. Si lo detectado es un `blob:` → se marca MSE y se avisa que no es casteable (N3),
   sin falsas esperanzas.
4. Varios candidatos → lista ordenada por duración/bitrate estimado, no elección
   automática silenciosa.

### 7.6 Proxy

- Escucha solo en la interfaz LAN, puerto efímero, y exige un token aleatorio en el
  path (`/s/{token}/...`). Sin token, 404.
- Inyecta los headers capturados; reescribe playlists HLS para que cada segmento y
  cada clave pasen por el proxy.
- Soporta `Range` (el gen1 hace seeks por rangos). Cuando el origen no respeta `Range`
  o la URL firmada expira, el proxy hace *spool* a un temporal en `cacheDir`, con tope
  configurable (1 GB por defecto, política LRU), que se borra al terminar la sesión.
  Sin spool no hay seek ni sondeo posible; el precio es disco liberable. Decisión
  revisada: el MVP **sí** usa disco temporal acotado.
- Se apaga con la sesión de cast o al cerrar el foreground service.

### 7.7 Compatibilidad (sondeo sin descargar)

`MediaProbe` decide en tres estados: `SUPPORTED`, `LIKELY` (heurística por
`Content-Type`/extensión, se castea y se acepta el riesgo) y `UNSUPPORTED` (con motivo
legible). El viejo estado `REMUX_REQUIRED` desaparece: la fase 0 mostró que MKV/H.264
se reproduce tal cual, así que el probe clasifica por **codec**, no por contenedor.

Cómo sondea un media remoto:

1. `HEAD` para `Content-Type`, `Content-Length` y `Accept-Ranges`.
2. Si el contenedor es MP4, se lee la **cabeza** y la **cola** con `Range` (el átomo
   `moov` puede estar al final si el archivo no fue escrito con faststart), y se
   alimenta un `MediaDataSource` propio que sirve esa ventana como si fuera el
   archivo completo. Presupuesto: 6 MB por candidato (4 MB cabeza + 2 MB cola).
3. `MediaExtractor` sobre esa ventana devuelve codec de video, codec de audio y
   duración. Con eso se decide sin bajar la película.
4. Si el origen no respeta `Range`: el proxy hace spool acotado (7.6) y se sondea
   sobre el spool. Si el spool tampoco es posible, el estado es `LIKELY` y la UI lo
   dice con todas las letras.

Con varios candidatos, el sondeo es asíncrono y cada ítem de la lista tiene su propio
estado visible ("analizando…"), nunca un bloqueo de la pantalla.

### 7.8 Archivos locales y `content://`

Tres fuentes de bytes distintas, un mismo destino:

- **`ACTION_SEND` con `video/*`** — el caso StreamFlix y el menú "Compartir" del
  sistema. Entra un `content://` con permiso de lectura temporal.
- **SAF (`ACTION_OPEN_DOCUMENT`)** — elección explícita de archivo desde la app. Se
  toma `takePersistableUriPermission` para que el foreground service pueda seguir
  leyendo el URI con la Activity muerta.
- **MediaStore** — galería, sólo como atajo de descubrimiento.

El proxy necesita una segunda fuente de bytes: `ContentResolver.openFileDescriptor`
con `Range` sobre el `fd`, que no comparte código con el fetch HTTP pero sí el
contrato del servidor. Si el archivo es HEVC o AV1, cae en `UNSUPPORTED` como
cualquier remoto; si es MKV/H.264, se castea directo.

### 7.9 Intents que recibe la app

| Intent | Mime | Qué hace |
|---|---|---|
| `ACTION_SEND` | `video/*` | Sirve el `content://` y castea |
| `ACTION_SEND` | `text/plain` | Extrae la URL; si es media directa, sondea; si es página, abre el navegador interno con detección |
| `ACTION_VIEW` | `video/*`, `http(s)` | Igual que arriba, para "abrir con" |

El caso `text/plain` es el de "compartir esta página desde Chrome" y es baratísimo de
soportar ahora, carísimo de insertar después en la navegación.

### 7.10 Calidad y repositorio

- Gradle con version catalog, KSP, sin lógica en Activities.
- Detekt + ktlint, GitHub Actions (lint, detekt, tests unitarios, `assembleDebug`).
- Tests unitarios sobre: parsing de candidatos, decisión de compatibilidad,
  construcción de headers, reescritura de `.m3u8`.
- ADRs en `docs/adr/` para: elección de receptor, proxy en el teléfono, Ktor vs
  NanoHTTPD, alcance de códec, licencia.
- README en inglés (portafolio) con diagrama de arquitectura, GIF de demo y sección
  "qué no hace"; versión en español.
- Repo privado durante el desarrollo, público al llegar a un MVP demostrable.

## 8. Lo que queda a criterio de quien implemente

- Nombres de paquetes y de clases internas.
- Diseño visual fino más allá del sistema de diseño.
- Estrategia concreta de reintentos del proxy.
- Formato exacto del historial (qué se guarda, por cuánto tiempo).
- Cómo se presenta la lista de candidatos cuando hay muchos.

## 9. Fase 0 — ejecutada (16-nov-2026)

Spike descartable, **sin Android**: una página en Chrome que usa la API legacy de Cast
(`chrome.cast.*`) para cargar medios en el Default Media Receiver, más un servidor HTTP
local con soporte de `Range` que registra cada request. Los ocho medios se generaron con
ffmpeg. El resultado está en la tabla de 5.1.

Qué se aprendió, además de la tabla:

- **El receptor responde a la API legacy, no al sender web moderno.** Con
  `cast_sender.js?loadCastFramework=1` en Chrome 157, `cast.framework.messages` no
  existe y `new LoadRequestData()` explota; con `chrome.cast.media.LoadRequest` todo
  funciona a la primera. El sender **nativo** de Android (`play-services-cast`) es otro
  stack y no debería tener el problema, pero se valida al principio de la fase 2, antes
  de construir features encima.
- **El receptor pide el final por `Range`:** `bytes=0-`, después `bytes=7995392-`.
  El proxy tiene que pasar rangos abiertos tal cual.
- **HLS con AES-128** exige servir `enc.key`; el receptor lo pide por separado.
- **Una URL de página no es media.** `ok.ru/video/123` no reproduce nada porque es
  HTML. Esto confirma que la detección dentro del navegador es el corazón del producto.

El spike no se conserva, pero el generador de medios y el servidor pasan al repo como
herramientas de verificación en `tools/testmedia/`.

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| El gen1 no reproduce HLS | **Resuelto en fase 0:** reproduce HLS simple y AES-128 |
| El sender nativo de Android no logra sesión con el receptor de 2013 | Validar `play-services-cast` contra el DMR en la primera hora de la fase 2, antes de construir features encima |
| El `WebView` no ve el media (MSE, service workers) | Inyección JS + fallback "pegar la URL" |
| MIUI mata el servicio en segundo plano | Foreground service con tipo correcto + guía de configuración |
| Router con aislamiento de clientes o sin puente entre bandas | Diagnóstico de red integrado: test de alcance al Chromecast y aviso con instrucciones por router |
| Play Store rechaza la app | Distribución por GitHub Releases / F-Droid |
| El proyecto deriva en scrapers por sitio | N5 y A2 en este documento; revisión en cada PR |

## 11. Fases

0. ~~Spike de compatibilidad del gen1~~ — **hecho** (ver 9).
1. Esqueleto: repo, módulos, CI, tema, navegación vacía.
2. Validar el sender nativo contra el DMR, y proxy + cast de una URL pegada a mano.
   Es el corazón, y es lo primero que se demuestra.
3. Navegador con detección y lista de candidatos.
4. "Compartir video" y archivos locales.
5. Historial, ajustes, diagnóstico de red, pulido y documentación.
6. **Diferido:** servicio opcional en la LAN (transcodificación de HEVC, si alguna vez
   hace falta); estudio de caja negra de Cast Browser.

## 12. Criterios de éxito

- De un link a la tele en tres toques o menos.
- ok.ru y páginas con MP4 estándar funcionan de punta a punta.
- El casteo sobrevive a bloquear el teléfono.
- Cero servidores, cero cuentas, cero telemetría.
- El repo se puede mostrar: CI verde, README claro, decisiones explicadas.
