# ADR 0002 — Elección de receptor Cast y API de envío

Estado: aceptado
Fecha: 2026-11-16

## Contexto

AlwaysCast busca reproducir flujos de video web en un Chromecast de 1ª generación (2013). En el ecosistema Google Cast existen tres opciones de aplicación receptora (Receiver Application) en el dispositivo de destino:

1. **Custom Web Receiver**: Aplicación HTML5/JavaScript alojada en un servidor web propio HTTPS y registrada en la Google Cast Developer Console (requiere registro de \$5 USD y mantenimiento de hosting).
2. **Styled Media Receiver**: Receptor alojado por Google con CSS personalizado configurable.
3. **Default Media Receiver (DMR)**: Aplicación estándar provista por Google con ID `CC1AD845`, disponible en todos los dispositivos Cast del mercado sin configuración previa.

Adicionalmente, en el emisor (Android) es necesario elegir entre implementar el protocolo de bajo nivel (DIAL / mDNS + sockets TLS en puerto 8009 con protobufs de CastV2) o utilizar la biblioteca oficial de Google (`play-services-cast-framework`).

## Decisión

1. **Receptor**: Usar el **Default Media Receiver (`CC1AD845`)**.
   - No requiere infraestructura de hosting externo, certificados SSL ni registro en la consola de Google.
   - En las pruebas de compatibilidad de la fase 0 contra el hardware objetivo (firmware `CrKey/1.36.157768`), el DMR demostró reproducir sin modificaciones contenedores MP4, MKV, WebM y listas HLS tanto planas como con cifrado AES-128.
2. **API Sender**: Usar la biblioteca oficial **`play-services-cast-framework`**, abstrayéndola detrás del contrato de dominio `CastController` en el módulo `:core:cast`.
   - Garantiza descubrimiento robusto por mDNS, reconexión de sesión y sincronización del estado de reproducción (`RemoteMediaClient`).
   - El resto de la aplicación y la capa de presentación no conocen clases de Google Play Services, facilitando pruebas unitarias con fakes en la JVM sin emulador.

## Consecuencias

- **Sin lógica personalizada en el televisor**: El Default Media Receiver no permite inyectar scripts en el navegador del Chromecast ni manipular cabeceras HTTP en las solicitudes de medios que realiza el reproductor (`<video>`).
- **Necesidad de proxy local**: Debido a que el DMR no permite añadir encabezados HTTP arbitrarios (`Referer`, `Origin`, `User-Agent`) a las peticiones del stream, el televisor debe solicitar el contenido a través de un proxy local que inyecte dichas cabeceras antes de llegar al servidor de origen (ver ADR 0003).
- **Interfaz del receptor predeterminada**: La interfaz visual en pantalla durante la reproducción muestra el reproductor estándar de Google.
