# Caché por lote de avisos: issue #5

Alcance y criterios: https://github.com/ivan280809/lol-match-tracker/issues/5.

- Reutilizar rango por jugador/cola y media del roster por lote.
- Aplicar TTL configurable, refresh explícito y fallback ante fallo de Riot.
- Mostrar fecha de consulta y conservar las ventanas estadísticas existentes.
- Respetar máximo de mensajes, deadlines, rate limit, outbox y métricas.
- Aislar schedulers solo en las pruebas de medición, sin condicionales de test en producción.
- Mantener PollingService y PollingServiceTest iguales a master.
- Usar esas nueve regresiones originales; retirar la copia duplicada que conservaba stubs de la deduplicación individual anterior a #7.
- Medir 20 avisos: una lectura de rango, una media del roster y cero llamadas Riot cuando la caché sigue vigente.
- Exigir Maven con PostgreSQL y build de imagen en CI sin fallos ni omisiones.
