# Issue #6: P3: Separar el despacho de Telegram del ciclo de polling

El alcance aprobado está definido: el dispatcher programado ya desacopla la entrega de Telegram del polling en el monolito, y el plan preservado identifica la consolidación de pruebas como siguiente paso. No hace falta una decisión funcional adicional.

## Steps

- Revisar el diff preservado y las pruebas relacionadas del dispatcher y la outbox.
- Consolidar las pruebas unitarias para límites de mensajes y tiempo, reintentos, 429/backoff, pausa/reanudación y deduplicación; mantener la integración que demuestra polling terminado con Telegram lento y aviso aún pendiente.
- Conservar lock, rate limit, configuración, métricas, filtros, manejo de errores y persistencia PostgreSQL existentes.
- Ejecutar Maven verify con Docker y comprobar que no haya fallos, errores ni pruebas omitidas; verificar las métricas de duración del polling y antigüedad de notificaciones pendientes.
- Solicitar revisión independiente completa gpt-6-luna low sobre el diff dentro del límite acordado de 120 KiB. No publicar PR si falla alguna comprobación o criterio.

## Acceptance

- El polling termina aunque Telegram esté lento o caído y los avisos quedan persistidos en la outbox para reintento.
- El dispatcher tiene límites por pasada, respeta el siguiente intento y 429/backoff, y conserva reintentos y deduplicación.
- Las pruebas cubren lentitud, fallos, pausa y reanudación.
- Se miden la duración del polling y la antigüedad de notificaciones pendientes.
- Maven verify con PostgreSQL completa sin fallos, errores ni pruebas omitidas, y la revisión independiente queda completada.
