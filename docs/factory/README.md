# Escribir tareas para la fábrica

Abre una issue en https://github.com/ivan280809/lol-match-tracker/issues/new . Describe objetivo, criterios de aceptación y restricciones. La plantilla aparecerá al integrar esta PR; mientras tanto usa una issue normal.

Ejemplo:

```markdown
## Objetivo
Filtrar el historial en base de datos para evitar cargar todas las partidas.
## Criterios de aceptación
- Se conservan los resultados de búsqueda y límites de fechas UTC.
- Una búsqueda no materializa todo el historial antes de filtrar.
- Hay regresiones de filtros combinados y pasan los tests existentes.
## Restricciones
Mantener el monolito y los contratos de pantalla. Sin claves reales de Riot/Telegram.
```

Añade `factory:ready` cuando quieras que el PC la recoja. Sin esa etiqueta permanece como idea. Para dependencias escribe `Depends on: #12, #15` en su propia línea. No incluyas secretos en issues públicas.

El comentario de la fábrica muestra estado, fase, bloqueo, modelo y última observación. Esa fecha permite detectar información antigua si el PC está apagado. Contesta decisiones con `factory:resume` seguido de tu respuesta. Para cancelar añade `factory:cancel`.

El resultado es una PR revisable, no una fusión automática. La V1 usa una tarea simultánea, Luna con tu suscripción y un modelo local validado. Credenciales y logs se quedan en el PC. Agotar cuota causa espera sin compra de API.
