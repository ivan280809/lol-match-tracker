# Issue #8: P5: Medir y reutilizar el transporte HTTP de Riot y Telegram

El alcance está definido y las instrucciones autorizan continuar. El cambio debe centrarse en el benchmark reproducible de 1.000 solicitudes por transporte, las pruebas simuladas independientes de rotación y errores, y un informe separado con resultados; no hay evidencia disponible para decidir ahora si usar el transporte compartido en producción.

## Steps

- Añadir el benchmark con APIs síncronas de RestClient para el transporte actual y JDK HttpClient compartido, sin modificar dependencias.
- Ejecutar exactamente 1.000 solicitudes por alternativa contra un servidor simulado que contabilice cada lote; calcular p95 por lote y registrar CPU y memoria del proceso antes y después.
- Registrar conexiones observadas o puertos remotos y explicar la limitación para medir handshakes; documentar también timeouts y cierre.
- Añadir pruebas simuladas separadas para rotación de token y región, errores remotos y respuestas 429, usando únicamente valores ficticios.
- Validar el benchmark implementado y guardar sus resultados en un documento bajo docs/exec-plans/reports/. Mantener el plan aprobado sin cambios.
- Decidir el uso en producción solo según la evidencia del benchmark; si no lo justifica, documentar los hallazgos y dejar la decisión pendiente de revisión humana.

## Acceptance

- El servidor simulado acredita 1.000 solicitudes por alternativa y se miden 1.000 latencias para cada cálculo de p95.
- Se registran CPU y memoria del proceso con método repetible, conexiones observadas o limitaciones explícitas, timeout y cierre.
- Las pruebas de rotación, errores y 429 están separadas del benchmark y usan fixtures ficticios.
- No se cambian dependencias ni se integra un transporte de producción sin evidencia favorable.
- El informe con los resultados queda en docs/exec-plans/reports/ y el plan aprobado permanece inmutable.
