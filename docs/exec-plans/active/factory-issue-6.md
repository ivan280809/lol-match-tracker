# Issue #6: P3: Separar el despacho de Telegram del ciclo de polling

El alcance está suficientemente definido y la aclaración del owner resuelve el tamaño de revisión. El siguiente paso local puede compactar las pruebas del dispatcher/outbox, conservar intactas las protecciones y métricas existentes, y verificar la solución con las puertas indicadas.

## Steps

- Revisar el diff preservado y los tests existentes relevantes para identificar duplicación sin alterar el comportamiento aprobado.
- Consolidar las pruebas nuevas del dispatcher/outbox en un único suite unitario que cubra límites de batch y tiempo, reintentos, 429/backoff, pausa/reanudación y deduplicación.
- Mantener un test de integración focalizado que pruebe que polling termina con Telegram lento mientras el aviso sigue durablemente pendiente en outbox; reutilizar fixtures existentes.
- Conservar las protecciones PostgreSQL, configuración, filtros, manejo de errores y métricas, incluidas duración de polling y edad del backlog.
- Reducir el diff total por debajo de 55 KB con margen y ejecutar ./mvnw verify con Docker.
- Exigir cero fallos, errores u omisiones y revisión completa gpt-6-luna low; no crear PR si alguna puerta falla.

## Acceptance

- Diff total inferior a 55 KB con margen, sin retirar criterios ni ampliar alcance.
- Pruebas consolidada unitarias e integración focalizada cubren los comportamientos aprobados y demuestran que polling termina mientras las notificaciones permanecen pendientes durante un envío lento.
- ./mvnw verify con Docker termina sin fallos, errores ni pruebas omitidas.
- Revisión completa gpt-6-luna low completada; no se crea PR si una puerta falla.
