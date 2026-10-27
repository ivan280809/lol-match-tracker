# Recuperacion de la issue #7

Criterios y alcance: https://github.com/ivan280809/lol-match-tracker/issues/7.
Se conserva la copia original y la semantica del polling y del read model.

- Deduplicar por jugador/pagina, conservando el punto de corte.
- Guardar solo valores en el buffer; no tocar JPA sin flush.
- Llamadas a 5 s: 20 flushes para t=0..99, mas el write final inmediato.
- Scheduler propio; reservar 500 ms para confirmar antes del plazo de 5 s.
- Limpiar pendientes al completar, fallar o recibir rate limit.
- Conservar pruebas de rollback, carreras, reintento y visibilidad real.
- Medir SQL y UPDATE reales para 100 jugadores, incluida comparacion baseline.
- Ejecutar Maven y CI con PostgreSQL, sin fallos ni omisiones; sin despliegue.
