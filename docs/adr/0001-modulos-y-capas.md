# ADR 0001 — Módulos y capas

Estado: aceptado
Fecha: 2026-11-16

## Contexto

La app tiene tres responsabilidades que cambian por motivos distintos: redes y
protocolos (proxy, sondeo de medios), integración con el Cast SDK, y presentación. Un
proyecto de portafolio además tiene que poder mostrarse: separar esas
responsabilidades en módulos hace visibles las dependencias y permite testear las
piezas con lógica sin emulador.

## Decisión

Módulos Gradle con una sola dirección de dependencias:

```
:app
:feature:cast          →  :core:*
:core:cast             →  :core:model
:core:proxy            →  :core:model
:core:network          →  :core:model
:core:designsystem
:core:common
:core:model            (no depende de nadie)
```

- `:core:model` contiene los modelos de dominio y los contratos (`MediaCandidate`,
  `Verdict`, `MediaSourceResolver`, `CastController`), sin dependencias de Android.
- `:core:common` es una librería JVM con utilidades sin Android.
- `:core:proxy` es una librería JVM: Ktor y OkHttp corren en la JVM, y sus tests
  unitarios no necesitan emulador.
- `:core:network` es Android porque usa `MediaExtractor` para leer codecs.
- `:core:cast` envuelve `play-services-cast` detrás de una interfaz propia.
- `:feature:cast` es la única feature del MVP; no conoce a otras features.
- `:app` solo compone: arranca el servicio, monta la pantalla y apunta el grafo.

Los plugins de convención (`build-logic/`) fijan `compileSdk`, `minSdk`, la versión de
Java y el target de Kotlin en un solo lugar.

## Consecuencias

- Una feature no puede depender de otra; si dos features comparten algo, ese algo baja
  a `:core`.
- Cambiar el backend de casting o el resolver de medios no toca la UI: la UI depende
  de las interfaces de `:core:model`.
- El costo es un grafo de módulos que hay que mantener; se acepta porque el objetivo
  incluye mostrar arquitectura.
