# Issue #8: P5: Medir y reutilizar el transporte HTTP de Riot y Telegram

El alcance está suficientemente definido y cuenta con un plan aprobado. El informe aportado registra 1.000 solicitudes por transporte y resultados que no justifican cambiar el transporte de producción, por lo que este debe permanecer igual mientras se completa la verificación y revisión.

## Steps

- Revisar el benchmark, los casos de rotación de token/región, errores remotos, 429 y timeout; mantenerlos como pruebas simuladas independientes.
- Validar que el benchmark síncrono con RestClient ejecuta exactamente 1.000 solicitudes por transporte, comprueba los conteos del servidor y calcula p95 con 1.000 latencias por alternativa.
- Confirmar que el informe separado registra CPU y memoria del proceso, conexiones observadas y sus limitaciones, timeout y cierre.
- Conservar el plan aprobado sin cambios, no modificar dependencias y no integrar un transporte de producción que las mediciones no justifican.
- Si la revisión detecta defectos, corregirlos en la fase de escritura autorizada y actualizar la evidencia en el informe separado.

## Acceptance

- Servidor simulado acredita 1.000 solicitudes por cada transporte y se calcula p95 sobre cada lote completo.
- Casos independientes prueban rotación de token y región, errores remotos, 429 y timeout usando solo valores ficticios.
- Evidencia registra recursos, conexiones observadas o limitaciones de medición, y comportamiento de cierre.
- Sin cambios de dependencias ni adopción del transporte compartido en producción sin evidencia favorable.
- El plan aprobado permanece inmutable y el informe de resultados está bajo docs/exec-plans/reports/.
