# AlwaysCast — Plan de implementación: MVP vertical (pegar URL → ver en la tele)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Una app Android que toma una URL de video pegada a mano, la sirve a través de un
proxy local con los headers correctos, y la reproduce en un Chromecast de primera
generación con el Default Media Receiver.

**Architecture:** Multi-módulo Gradle con Clean-ish layering: `core` (modelo, red, proxy,
cast) y `feature` (pantalla de casteo), unidos por Hilt. El proxy Ktor corre dentro de un
foreground service y expone una URL con token que el televisor consume por HTTP. El Cast
SDK nativo de Android (`play-services-cast`) se envuelve tras una interfaz para poder
probarlo y reemplazarlo.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Coroutines/Flow, Ktor Server (CIO), OkHttp,
`play-services-cast-framework`, JUnit4, MockWebServer, Turbine, Detekt, ktlint.

**Spec:** `docs/superpowers/specs/2026-11-16-alwayscast-design.md`

**Alcance de este plan:** fases 1 y 2 del spec — esqueleto y el corte vertical que
demuestra la idea. Las fases 3 (navegador con detección), 4 (compartir/archivos locales) y
5 (historial, ajustes, pulido) se planifican por separado, cada una sobre esta base.

## Global Constraints

- `minSdk 26`; `compileSdk`/`targetSdk`: el último estable instalado en el SDK local (36+).
- `namespace` e `applicationId`: `dev.matiribco.alwayscast`.
- Sin DRM, sin HEVC/AV1, sin transcodificación, sin cuentas, sin telemetría.
- El proxy escucha **solo** en la LAN, en puerto efímero, y exige token en el path
  (`/s/{token}/...`). Sin token: 404. Nunca sirve rutas de archivo locales.
- Presupuesto de sondeo profundo: **4 MB de cabeza + 2 MB de cola** por candidato.
- Spool en `cacheDir`: tope **1 GB**, política LRU, se borra al cerrar la sesión.
- El proxy debe propagar `Range`, incluido `bytes=N-` abierto (el gen1 lee el `moov` del
  final así), y responder `206` con `Content-Range` correcto.
- HLS: la playlist se reescribe para que segmentos **y claves** (`EXT-X-KEY`) pasen por
  el proxy.
- UI en español; nombres de código, commits y README en inglés.
- Licencia Apache-2.0. Nada de datos de usuario fuera del dispositivo.

## Review Focus

Las cinco formas en que esto va a fallar en la vida real, y el test que las cubre:

1. **MP4 sin faststart**: el televisor pide `bytes=N-` para leer el índice del final. Si
   el proxy no propaga ese rango abierto, el video no arranca nunca. → Task 4.
2. **HLS con AES-128**: si la reescritura de la playlist no incluye la URI de
   `EXT-X-KEY`, no hay imagen ni error visible. → Task 3.
3. **URL firmada con query string**: `/s/{token}/{uppercase-base64}` rompe si el encoding
   no es base64url sin padding. → Task 3.
4. **Origen que ignora `Range`**: el proxy no debe colgarse ni devolver 200 donde el
   cliente pidió 206; degrada y lo informa. → Task 4.
5. **Chromecast no descubierto (bandas o mDNS)**: la UI no puede quedarse en "conectando"
   para siempre; muestra un diagnóstico accionable. → Task 7.

---

### Task 1: Esqueleto del repo, toolchain y CI

**Files:**
- Create: `.gitignore`, `LICENSE`, `README.md`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`,
  `app/src/main/java/dev/matiribco/alwayscast/AlwaysCastApp.kt`,
  `core/common/build.gradle.kts`, `core/model/build.gradle.kts`,
  `core/network/build.gradle.kts`, `core/proxy/build.gradle.kts`, `core/cast/build.gradle.kts`,
  `core/designsystem/build.gradle.kts`, `feature/cast/build.gradle.kts`,
  `build-logic/` (convention plugins), `.github/workflows/ci.yml`,
  `docs/adr/0001-modulos-y-capas.md`
- Test: `core/common/src/test/java/dev/matiribco/alwayscast/core/common/BuildInfoTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces: `object BuildInfo { const val VERSION_NAME: String }` en `:core:common`;
  los módulos vacíos y el wrapper de Gradle sobre los que trabajan todas las tareas.

- [ ] **Step 1: Instalar el JDK de build y generar el wrapper**

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --silent --accept-package-agreements --accept-source-agreements
winget install --id Gradle.Gradle -e --silent --accept-package-agreements --accept-source-agreements
```

Verificar: `java -version` (debe poder resolverse un 21) y `gradle --version`.
En `gradle.properties` fijar `org.gradle.java.home` con el `JAVA_HOME` de Temurin 21
(el JDK 25 que trae Studio no es el que usa Gradle en CI).

- [ ] **Step 2: Escribir el test que falla**

```kotlin
package dev.matiribco.alwayscast.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildInfoTest {
    @Test
    fun `version name is the mvp version`() {
        assertEquals("0.1.0", BuildInfo.VERSION_NAME)
    }
}
```

- [ ] **Step 3: Correr el test y verlo fallar**

Run: `.\gradlew.bat :core:common:test` → Expected: FAIL, `BuildInfo` no existe.

- [ ] **Step 4: Crear el esqueleto de módulos y hacerlo pasar**

`settings.gradle.kts` incluye `:app`, `:core:common`, `:core:model`, `:core:network`,
`:core:proxy`, `:core:cast`, `:core:designsystem`, `:feature:cast`.
Cada `build.gradle.kts` de módulo declara `namespace` propio y aplica el convention plugin
correspondiente. `BuildInfo.VERSION_NAME = "0.1.0"`.
`.gitignore` cubre `.gradle/`, `build/`, `local.properties`, `.idea/`, `*.iml`, `captures/`.
`LICENSE` es el texto de Apache-2.0 sin modificar.

- [ ] **Step 5: Verificar build completo**

Run: `.\gradlew.bat assembleDebug testDebugUnitTest` → Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: CI**

`.github/workflows/ci.yml`: `actions/checkout`, JDK 21 (`temurin`), cache de Gradle,
`./gradlew detekt ktlintCheck testDebugUnitTest assembleDebug`.

- [ ] **Step 7: Commit y push**

```bash
git add -A
git commit -m "chore: android multi-module skeleton with ci"
git push
```

---

### Task 2: Clasificación de candidatos (`:core:model` + `:core:network`)

**Files:**
- Create: `core/model/src/main/java/dev/matiribco/alwayscast/core/model/MediaCandidate.kt`,
  `.../model/Probe.kt`, `.../model/CastState.kt`, `.../model/MediaSourceResolver.kt`
- Create: `core/network/src/main/java/dev/matiribco/alwayscast/core/network/MediaTypeClassifier.kt`
- Test: `core/network/src/test/java/dev/matiribco/alwayscast/core/network/MediaTypeClassifierTest.kt`

**Interfaces:**
- Consumes: nada.
- Produces:
  - `data class MediaCandidate(val url: String, val headers: Map<String,String> = emptyMap(), val contentType: String? = null, val source: CandidateSource = CandidateSource.PASTED)`
  - `enum class CandidateSource { PASTED, SHARED_INTENT, BROWSER, LOCAL_FILE }`
  - `sealed interface Verdict { data class Supported(val contentType: String); data class Unsupported(val reason: UnsupportedReason); data class Unknown(val contentType: String?) }`
  - `enum class UnsupportedReason { DRM, HEVC, AV1, MSE_BLOB, NO_MEDIA }`
  - `data class ResolvedSource(val url: String, val contentType: String)`
  - `interface MediaSourceResolver { suspend fun resolve(candidate: MediaCandidate): ResolvedSource }`
  - `object MediaTypeClassifier { fun classify(url: String, contentType: String?): Verdict }`

- [ ] **Step 1: Escribir los tests que fallan**

```kotlin
class MediaTypeClassifierTest {
    @Test fun `mp4 is supported`() =
        assertEquals(Verdict.Supported("video/mp4"), MediaTypeClassifier.classify("https://x/a.mp4", null))

    @Test fun `matroska is supported because gen1 demuxes it`() =
        assertEquals(Verdict.Supported("video/x-matroska"), MediaTypeClassifier.classify("https://x/a.mkv", "video/x-matroska"))

    @Test fun `hls content type is normalized`() =
        assertEquals(Verdict.Supported("application/x-mpegURL"), MediaTypeClassifier.classify("https://x/a.m3u8", null))

    @Test fun `blob url is mse and unsupported`() =
        assertEquals(Verdict.Unsupported(UnsupportedReason.MSE_BLOB), MediaTypeClassifier.classify("blob:https://x/1", null))

    @Test fun `hevc hint is unsupported`() =
        assertEquals(Verdict.Unsupported(UnsupportedReason.HEVC), MediaTypeClassifier.classify("https://x/a.hevc.mp4", "video/mp4; codecs=\"hvc1\""))

    @Test fun `unknown extension is unknown not unsupported`() =
        assertEquals(Verdict.Unknown(null), MediaTypeClassifier.classify("https://x/stream", null))
}
```

- [ ] **Step 2: Correr y ver fallar**

Run: `.\gradlew.bat :core:network:testDebugUnitTest` → Expected: FAIL, clase inexistente.

- [ ] **Step 3: Implementar**

Señales en este orden: `blob:`/`mediasource` → MSE_BLOB; `hvc1`/`hev1`/`.hevc` → HEVC;
`av01`/`.av1` → AV1; `widevine`/`drmtoday` → DRM; extensión o content type conocido →
Supported con el type normalizado (`.mp4`→`video/mp4`, `.webm`→`video/webm`,
`.mkv`→`video/x-matroska`, `.m3u8`→`application/x-mpegURL`, `.mp3`, `.flac`); si hay
content type de video desconocido → `Unknown(contentType)`; si no hay nada → `Unknown(null)`.

- [ ] **Step 4: Verificar**

Run: `.\gradlew.bat :core:network:testDebugUnitTest` → Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(core): classify media candidates by container and codec hints"
```

---

### Task 3: Piezas puras del proxy (`:core:proxy`)

**Files:**
- Create: `core/proxy/src/main/java/dev/matiribco/alwayscast/core/proxy/ProxyPath.kt`,
  `.../proxy/HeaderInjector.kt`, `.../proxy/HlsPlaylistRewriter.kt`
- Test: `core/proxy/src/test/java/dev/matiribco/alwayscast/core/proxy/ProxyPathTest.kt`,
  `.../HlsPlaylistRewriterTest.kt`, `.../HeaderInjectorTest.kt`

**Interfaces:**
- Consumes: `MediaCandidate` (Task 2).
- Produces:
  - `object ProxyPath { fun encode(upstream: String): String; fun decode(segment: String): String?; fun forToken(token: String, upstream: String): String }`
  - `object HeaderInjector { fun headersFor(candidate: MediaCandidate): Map<String, String> }`
  - `class HlsPlaylistRewriter(private val mapUri: (String) -> String) { fun rewrite(playlist: String, baseUrl: String): Boolean }`

- [ ] **Step 1: Tests que fallan**

```kotlin
class ProxyPathTest {
    @Test fun `signed query survives the roundtrip`() {
        val url = "https://cdn.x/v.mp4?token=a+b/c=d&exp=17"
        assertEquals(url, ProxyPath.decode(ProxyPath.encode(url)))
    }
    @Test fun `encoded segment has no padding or slashes`() {
        val s = ProxyPath.encode("https://cdn.x/v.mp4")
        assertFalse(s.contains('/')); assertFalse(s.contains('='))
    }
    @Test fun `for token builds a rooted path`() {
        assertEquals("/s/abc/${ProxyPath.encode("https://x/a.mp4")}", ProxyPath.forToken("abc", "https://x/a.mp4"))
    }
    @Test fun `invalid segment decodes to null`() =
        assertNull(ProxyPath.decode("!!!not-base64!!!"))
}

class HlsPlaylistRewriterTest {
    @Test fun `rewrites segment and key uris`() {
        val playlist = """
            #EXTM3U
            #EXT-X-KEY:METHOD=AES-128,URI="enc.key"
            #EXTINF:4,
            seg0.ts
        """.trimIndent()
        val out = HlsPlaylistRewriter { "http://127.0.0.1:9/s/t/${ProxyPath.encode(it)}" }
            .also { it.rewrite(playlist, "http://cdn.x/dir/index.m3u8") }
        assertTrue(out.toString().contains("/s/t/"))
    }
    @Test fun `relative segment resolves against playlist base`() { /* seg0.ts → http://cdn.x/dir/seg0.ts */ }
    @Test fun `non playlist text is left untouched`() { /* returns false, text unchanged */ }
}
```

- [ ] **Step 2: Correr y ver fallar** — `.\gradlew.bat :core:proxy:testDebugUnitTest`

- [ ] **Step 3: Implementar**

`ProxyPath` usa `Base64.getUrlEncoder().withoutPadding()`. `HeaderInjector` parte de los
headers del candidato, fuerza `User-Agent` de navegador si no hay, y descarta
hop-by-hop (`Connection`, `Host`, `Content-Length`, `Accept-Encoding`).
`HlsPlaylistRewriter` resuelve cada URI relativa contra `baseUrl` y aplica `mapUri`;
reemplaza URIs de `#EXT-X-KEY` y `#EXT-X-MAP` además de las líneas de segmento.

- [ ] **Step 4: Verificar** — PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(proxy): token paths, header injection and hls playlist rewriting"
```

---

### Task 4: Servidor proxy (`:core:proxy`)

**Files:**
- Create: `core/proxy/src/main/java/dev/matiribco/alwayscast/core/proxy/CastProxyServer.kt`
- Test: `core/proxy/src/test/java/dev/matiribco/alwayscast/core/proxy/CastProxyServerTest.kt`

**Interfaces:**
- Consumes: `ProxyPath`, `HeaderInjector`, `HlsPlaylistRewriter` (Task 3).
- Produces:
  - `interface SpoolStore { fun file(key: String): File }` con `class FileSpoolStore(root: File, maxBytes: Long = 1L * 1024 * 1024 * 1024) : SpoolStore` (LRU por `lastModified`, se limpia con `clear()`).
  - `class CastProxyServer(private val client: OkHttpClient, private val spool: SpoolStore, private val token: String = randomToken()) : Closeable { val boundPort: Int; suspend fun start(); fun stop(); fun urlFor(candidate: MediaCandidate): String }`

- [ ] **Step 1: Tests que fallan** (Ktor `testApplication` + MockWebServer como upstream)

```kotlin
@Test fun `wrong token is 404`() { /* GET /s/nope/x → 404 */ }
@Test fun `open ended range is forwarded and answered 206`() {
    // upstream responde 206 con Content-Range: bytes 7995392-8011775/8011776
    // se espera 206 y el mismo Content-Range en la respuesta del proxy
}
@Test fun `origin ignoring range does not hang and returns 200`() { /* upstream 200 completo */ }
@Test fun `origin ignoring range is spooled and later answers 206`() {
    // 1er GET con Range → upstream 200, el proxy guarda en spool (tope respetado)
    // 2do GET con el mismo rango → 206 servido desde el spool, sin tocar el upstream
}
@Test fun `spool evicts oldest files over the cap`() { /* FileSpoolStore con cap chico, 3 archivos → queda el más nuevo */ }
@Test fun `m3u8 response is rewritten through the proxy`() { /* body contiene /s/{token}/ */ }
@Test fun `upstream headers are injected`() { /* MockWebServer verifica Referer y User-Agent */ }
```

- [ ] **Step 2: Correr y ver fallar** — `.\gradlew.bat :core:proxy:testDebugUnitTest`

- [ ] **Step 3: Implementar**

`embeddedServer(CIO, port = 0)`; ruta `get("/s/{token}/{segment}")`; 404 si el token no
coincide o el segmento no decodifica. Propaga `Range` tal cual al upstream y copia
`status`, `Content-Range`, `Content-Length`, `Content-Type`. Si el `Content-Type` es de
playlist, bufferea el texto (tope 2 MB), lo reescribe con `HlsPlaylistRewriter` y responde
con `Content-Length` recalculado. Streaming con `respondOutputStream` para el resto.

- [ ] **Step 4: Verificar** — PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(proxy): ktor cast proxy with range passthrough and hls support"
```

---

### Task 5: Sondeo profundo de codecs (`:core:network`)

**Files:**
- Create: `core/network/src/main/java/dev/matiribco/alwayscast/core/network/WindowPlanner.kt`,
  `.../network/RangeMediaDataSource.kt`, `.../network/AndroidMediaInspector.kt`
- Test: `core/network/src/test/java/dev/matiribco/alwayscast/core/network/WindowPlannerTest.kt`

**Interfaces:**
- Consumes: `MediaCandidate`, `Verdict` (Task 2).
- Produces:
  - `object WindowPlanner { fun rangesFor(size: Long, head: Long = 4L * 1024 * 1024, tail: Long = 2L * 1024 * 1024): List<LongRange> }`
  - `class AndroidMediaInspector(client: OkHttpClient) { suspend fun inspect(candidate: MediaCandidate): Verdict }`

- [ ] **Step 1: Test que falla**

```kotlin
@Test fun `small file needs a single range`() =
    assertEquals(listOf(0L..999L), WindowPlanner.rangesFor(1000))

@Test fun `large file needs head and tail`() {
    val r = WindowPlanner.rangesFor(100_000_000)
    assertEquals(2, r.size)
    assertEquals(0L..4_194_303L, r[0])
    assertEquals(95_805_696L..99_999_999L, r[1])
}

@Test fun `unknown size yields head only`() =
    assertEquals(listOf(0L..4_194_303L), WindowPlanner.rangesFor(-1))
```

- [ ] **Step 2: Correr y ver fallar** — `.\gradlew.bat :core:network:testDebugUnitTest`

- [ ] **Step 3: Implementar**

`WindowPlanner` puro (fórmula de arriba). `RangeMediaDataSource` implementa
`MediaDataSource` sirviendo la ventana cabeza+cola como si fuera el archivo completo.
`AndroidMediaInspector` hace `HEAD`, pide las ventanas, y usa `MediaExtractor` para leer
`mime` de video/audio: `video/hevc` o `video/av01` → `Unsupported`; video no reconocido →
`Unknown`; resto → `Supported` con el content type del contenedor.

- [ ] **Step 4: Verificar** — unit tests PASS. Verificación en dispositivo (manual, al final
  de la tarea): con el servidor de `tools/testmedia` levantado, inspeccionar
  `09-hevc.mp4` desde la app de debug y confirmar `Unsupported(HEVC)` en el log.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(network): head and tail probe with media extractor"
```

---

### Task 6: Controlador de Cast (`:core:cast`)

**Files:**
- Create: `core/cast/src/main/java/dev/matiribco/alwayscast/core/cast/CastController.kt`,
  `.../cast/GoogleCastController.kt`
- Test: `core/cast/src/test/java/dev/matiribco/alwayscast/core/cast/CastStateMappingTest.kt`

**Interfaces:**
- Consumes: `ResolvedSource`, `CastState` (Task 2).
- Produces: `interface CastController { val state: Flow<CastState>; suspend fun connect(): Result<String>; suspend fun load(source: ResolvedSource, title: String?): Result<Unit>; fun playOrPause(); fun seekTo(seconds: Double); fun stop() }` y el `FakeCastController` de test.

- [ ] **Step 1: Test que falla** — mapear estados del SDK a `CastState`

```kotlin
@Test fun `session started maps to connected with device name`() { /* SessionState.STARTED → CastState.Connected("Sala familiar") */ }
@Test fun `receiver unavailable maps to an actionable error`() { /* → CastState.Error(contains "no encontr") */ }
@Test fun `idle with error maps to error with reason`() { /* → CastState.Error(contains reason) */ }
```

- [ ] **Step 2: Correr y ver fallar** — `.\gradlew.bat :core:cast:testDebugUnitTest`

- [ ] **Step 3: Implementar**

`GoogleCastController` inicializa `CastContext` con
`CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID` (`CC1AD845`), expone
`state` con `callbackFlow`, y en `load` construye
`MediaLoadRequestData.Builder().setMediaInfo(MediaInfo.Builder(url).setContentType(type).setStreamType(BUFFERED).build()).build()`
y llama `remoteMediaClient.load(...)`.

- [ ] **Step 4: Validación en dispositivo (bloqueante)**

Con el servidor de `tools/testmedia`: castear `01` y `05` desde un botón de debug.
Expected: `PLAYING` y progreso. **Si CAF no abre sesión con el DMR**, cambiar la
implementación a `castSession.getRemoteMediaPlayer().load(new MediaInfo(...))` (API legacy)
sin tocar la interfaz. Registrar la decisión en `docs/adr/0002-cast-receiver-y-api.md`.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(cast): cast controller over play services cast"
```

---

### Task 7: ViewModel de la pantalla de casteo (`:feature:cast`)

**Files:**
- Create: `feature/cast/src/main/java/dev/matiribco/alwayscast/feature/cast/CastViewModel.kt`,
  `.../cast/CastUiState.kt`, `.../cast/LocalProxyResolver.kt`
- Test: `feature/cast/src/test/java/dev/matiribco/alwayscast/feature/cast/CastViewModelTest.kt`

**Interfaces:**
- Consumes: `MediaTypeClassifier`, `MediaSourceResolver`, `CastProxyServer`, `CastController`.
- Produces:
  - `class LocalProxyResolver(private val proxy: CastProxyServer, private val classifier: MediaTypeClassifier) : MediaSourceResolver` — arranca el proxy si hace falta y devuelve `ResolvedSource(proxy.urlFor(candidate), contentType)`.
  - `data class CastUiState(val url: String, val phase: Phase, val deviceName: String?, val message: String?)`
    con `enum class Phase { IDLE, PROBING, READY, CASTING, UNSUPPORTED, ERROR }`;
  - `class CastViewModel(...) { val state: StateFlow<CastUiState>; fun onUrlChange(String); fun onCastClick(); fun onConnectClick() }`

- [ ] **Step 1: Tests que fallan**

```kotlin
@Test fun `hevc input shows unsupported and never casts`() = runTest {
    // fake classifier → Unsupported(HEVC); el fake de cast debe registrar 0 cargas
}
@Test fun `happy path probes resolves and loads`() = runTest {
    // phase pasa por PROBING → READY → CASTING, y el fake recibe la url del proxy
}
@Test fun `missing device shows diagnostic instead of spinning`() = runTest {
    // connect() devuelve failure("no encontré el Chromecast en la red")
    // state.message contiene "Chromecast" y phase es ERROR, no PROBING
}
```

- [ ] **Step 2: Correr y ver fallar** — `.\gradlew.bat :feature:cast:testDebugUnitTest`

- [ ] **Step 3: Implementar**

`onCastClick`: valida URL → `Phase.PROBING` → `classify` → si `Unsupported` →
`Phase.UNSUPPORTED` con motivo legible; si `Supported`/`Unknown` → arranca el proxy si no
está, `resolver.resolve(candidate)` → `cast.connect()` (si falla, `ERROR` con mensaje de
red) → `cast.load(...)` → `Phase.CASTING`.

- [ ] **Step 4: Verificar** — PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(cast): view model for paste and cast flow"
```

---

### Task 8: UI, app y foreground service

**Files:**
- Create: `feature/cast/src/main/java/dev/matiribco/alwayscast/feature/cast/CastScreen.kt`,
  `feature/cast/src/main/java/dev/matiribco/alwayscast/feature/cast/CastModule.kt`,
  `app/src/main/java/dev/matiribco/alwayscast/CastForegroundService.kt`,
  `core/designsystem/src/main/java/dev/matiribco/alwayscast/core/designsystem/Theme.kt`
- Modify: `app/src/main/AndroidManifest.xml` (permisos de red, servicio, `MainActivity`)
- Test: `app/src/test/java/dev/matiribco/alwayscast/ProxyUrlTest.kt` (sanity, sin Android)

**Interfaces:**
- Consumes: `CastViewModel` (Task 7), `CastProxyServer` (Task 4), `CastController` (Task 6).
- Produces: la app instalable, con una pantalla y un servicio.

- [ ] **Step 1: Implementar la pantalla**

Compose: campo de URL, botón "Castear", botón "Conectar", indicador de fase, y un bloque
de error con el mensaje tal cual viene del ViewModel. Sin lógica en la Activity.
Manifest: `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `usesCleartextTraffic="true"` (el proxy es HTTP
local), servicio con `foregroundServiceType="mediaPlayback"`, `MainActivity` como launcher.

- [ ] **Step 2: Servicio**

`CastForegroundService` arranca `CastProxyServer` y publica la notificación con
"AlwaysCast está sirviendo el video". Se detiene al terminar la sesión de cast.

- [ ] **Step 3: Verificación manual de punta a punta (bloqueante)**

1. `.\gradlew.bat installDebug`
2. Levantar `tools/testmedia`: `node tools/testmedia/server.js`
3. En la app, pegar `http://<ip-lan>:8099/01-h264-aac-faststart.mp4` → Castear.
   Expected: se ve en la tele.
4. Repetir con `02` (sin faststart) y `05`/`06` (HLS). Expected: los tres.
5. Bloquear el teléfono. Expected: el video sigue.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(app): cast screen, foreground service and device validation"
```

---

### Task 9: Documentación y herramientas

**Files:**
- Create: `tools/testmedia/gen-testmedia.ps1`, `tools/testmedia/server.js`, `tools/testmedia/sender.html`,
  `tools/testmedia/README.md`, `docs/adr/0002-cast-receiver-y-api.md`,
  `docs/adr/0003-proxy-en-el-telefono.md`, `docs/adr/0004-sin-transcodificacion.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: todo lo anterior.
- Produces: repo presentable.

- [ ] **Step 1: Portar el harness de la fase 0**

Copiar desde `C:\Users\matir\AppData\Local\Temp\opencode\alwayscast-media\` los tres
archivos, sin los binarios generados (van por `.gitignore` en `tools/testmedia/media/`).

- [ ] **Step 2: README en inglés**

Qué hace, GIF o captura, diagrama de arquitectura, "What it does not do" (DRM, HEVC, MSE),
instalación, `tools/testmedia`, y la nota de las dos generaciones de Google Home.

- [ ] **Step 3: ADRs**

0002: por qué el DMR y qué API; 0003: por qué el proxy vive en el teléfono; 0004: por qué
no se transcodifica (medición de la fase 0 incluida).

- [ ] **Step 4: Verificar y commitear**

Run: `.\gradlew.bat detekt ktlintCheck testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`.

```bash
git add -A
git commit -m "docs: readme, adrs and phase 0 test harness"
git push
```
