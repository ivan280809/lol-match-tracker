# Issue #6: P3: Separar el despacho de Telegram del ciclo de polling

El fallo de compilación está localizado en NotificationOutboxDispatcherService.java: se usa PlayerEntity sin importar su tipo. El plan aprobado ya cubre los criterios funcionales y las verificaciones pendientes; no se necesita una decisión adicional del propietario.

## Steps

- En la siguiente fase de escritura, reparar el símbolo de compilación en NotificationOutboxDispatcherService.java sin alterar el alcance ni los cambios preservados.
- Ejecutar Maven verify con Java 17 y PostgreSQL disponible; tratar pruebas omitidas como verificación incompleta.
- Revisar lentitud y caída simuladas, límites por pasada, backoff/429, pausa y reanudación, deduplicación y persistencia de outbox; comprobar duración de polling y antigüedad de avisos pendientes.
- Solicitar revisión independiente gpt-6-luna low sobre el diff completo hasta 120 KiB; no publicar PR si falla una comprobación o criterio.

## Acceptance

- El dispatcher programado procesa la outbox de forma independiente del polling, con límites de mensajes y tiempo y respeto de reintentos/backoff.
- Con Telegram lento o caído, el polling termina y los avisos permanecen persistidos en outbox para reintento.
- Persistencia, deduplicación y protecciones PostgreSQL se conservan; no se afirma entrega exactly-once.
- Las métricas de duración de polling y antigüedad de notificaciones pendientes están disponibles.
- Maven verify con PostgreSQL termina sin fallos, errores ni pruebas omitidas, y la revisión independiente queda completada.
