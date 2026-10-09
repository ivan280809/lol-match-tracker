# Issue #8: P5: Medir y reutilizar el transporte HTTP de Riot y Telegram

La reparación queda acotada a corregir la atribución de evidencia en el informe. No hace falta una decisión humana adicional: las métricas del benchmark no están verificadas para el SHA actual y los 210 tests deben atribuirse solo al SHA verificado.

## Steps

- Actualizar únicamente docs/exec-plans/reports/factory-issue-8-benchmark.md para identificar las cifras del benchmark como ilustrativas y pendientes de verificación para f20e2497a6a56ddc38ec710ca6a52e2a2a57d275.
- Atribuir los 210 tests exclusivamente a f20e2497a6a56ddc38ec710ca6a52e2a2a57d275, separado de las métricas del benchmark.
- Dejar intactos el plan aprobado, el código fuente, pom.xml y AppConfigurationService.java.

## Acceptance

- El informe identifica claramente a qué SHA corresponde cada evidencia y no atribuye resultados del benchmark al SHA actual.
- Los 210 tests se atribuyen solo al SHA verificado.
- El informe es el único archivo modificado.
