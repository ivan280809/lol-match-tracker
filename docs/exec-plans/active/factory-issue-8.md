# Issue #8: P5: Medir y reutilizar el transporte HTTP de Riot y Telegram

No se necesita una decisión funcional adicional. La corrección está limitada a aclarar la atribución de la evidencia en el informe; las métricas existentes no están verificadas para el SHA actual.

## Steps

- Actualizar únicamente docs/exec-plans/reports/factory-issue-8-benchmark.md para marcar las cifras del benchmark como ilustrativas y no verificadas para f20e2497a6a56ddc38ec710ca6a52e2a2a57d275.
- Atribuir los 210 tests exclusivamente a f20e2497a6a56ddc38ec710ca6a52e2a2a57d275 y separar esa evidencia de las métricas del benchmark.
- Dejar intactos el plan aprobado, el código fuente, pom.xml y AppConfigurationService.java.

## Acceptance

- El informe identifica el SHA al que corresponde cada evidencia y no atribuye métricas a un SHA no probado.
- Los 210 tests se atribuyen únicamente a f20e2497a6a56ddc38ec710ca6a52e2a2a57d275.
- El único archivo modificado es el informe de resultados.
