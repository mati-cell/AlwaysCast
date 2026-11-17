# ADR 0004 — Decisión de no transcodificar medios en el teléfono

Estado: aceptado
Fecha: 2026-11-16

## Contexto

El Chromecast de 1ª generación (lanzado en 2013) cuenta con un SoC Marvell Armada 1500-mini (88DE3005) con decodificación de video por hardware limitada a H.264 (High Profile hasta 1080p a 30 fps) y VP8. Carece de soporte por hardware para estándares más recientes como HEVC (H.265) y AV1.

Se evaluó la posibilidad de transcodificar flujos no compatibles (por ejemplo, decodificar HEVC y re-codificar a H.264 en tiempo real) directamente en el procesador del teléfono móvil utilizando bibliotecas basadas en FFmpeg:

1. **Estado del ecosistema**: El proyecto de referencia para Android, `ffmpeg-kit`, fue discontinuado y archivado a comienzos de 2025, careciendo de soporte oficial y parches de seguridad activos.
2. **Impacto en el dispositivo**: La transcodificación de video en tiempo real sin hardware dedicado produce un consumo masivo de batería, elevadas temperaturas (*thermal throttling*) y latencias elevadas incompatibles con la experiencia de uso en segundo plano.
3. **Mediciones de compatibilidad real en la Fase 0**:
   Se ejecutó una batería empírica de 9 pruebas directamente contra el dispositivo objetivo del usuario (Chromecast Gen 1, firmware `CrKey/1.36.157768`, Default Media Receiver):

| Medio de prueba | Características | Resultado observado |
|---|---|---|
| `01` | MP4 H.264 + AAC (faststart) | Reproducción fluida y seek instantáneo. |
| `02` | MP4 H.264 + AAC (sin faststart) | Reproducción correcta mediante peticiones `Range` al final del archivo. |
| `03` | MP4 H.264 + AC3 | Audio reproducido en passthrough compatible. |
| `04` | WebM VP8 + Vorbis | Reproducción nativa correcta. |
| `05` | HLS plain (VOD) | Reproducción nativa correcta de segmentos TS. |
| `06` | HLS AES-128 | Descarga de clave y descifrado nativo exitoso. |
| `07` | MKV H.264 + AAC | **Reproducción exitosa**: El DMR demuxea contenedores Matroska nativamente. |
| `08` | MP4 1080p 12 Mbps | Reproducción sin cortes de ancho de banda. |
| `09` | MP4 HEVC / H.265 | **Fallo**: Sin soporte en hardware; pantalla negra / congelada. |

El hallazgo clave fue que los contenedores Matroska (`.mkv`) con H.264 no requieren proceso de remuxing en el teléfono, reduciendo los casos incompatibles exclusivamente a codecs de compresión no soportados (HEVC, AV1).

## Decisión

**No transcodificar en el MVP de AlwaysCast**:

1. El analizador de medios (`:core:network`) inspecciona las pistas de video utilizando `MediaExtractor` y cabeceras del contenedor.
2. Si se detecta un codec no soportado (HEVC / H.265 o AV1), el sistema emite tempranamente un veredicto estructurado: `Verdict.Unsupported(UnsupportedReason.HEVC)`.
3. La interfaz de usuario muestra un mensaje claro explicativo sobre la limitación física del Chromecast 1ª gen, en lugar de intentar una transcodificación inestable.
4. Cualquier arquitectura de transcodificación (por ejemplo, delegación a un servidor o daemon en una PC potente de la red local) queda diferida para versiones futuras fuera del alcance del MVP.

## Consecuencias

- **Tamaño y simplicidad del APK**: La aplicación no incluye binarios nativos masivos de FFmpeg, manteniendo el tamaño del APK por debajo de 10 MB y simplificando el proceso de compilación y empaquetado.
- **Rendimiento predecible**: El teléfono funciona a carga mínima de CPU, limitándose al reenvío de paquetes en memoria (*zero-copy socket streaming*).
- **Limitación asumida**: Los videos codificados en HEVC o AV1 no se podrán reproducir en el Chromecast de 2013, lo cual refleja la limitación intrínseca de hardware del dispositivo.
