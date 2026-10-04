# Escribir tareas para Software Factory

Crea una [issue en este proyecto](https://github.com/ivan280809/lol-match-tracker/issues/new).
Incluye objetivo, criterios de aceptación y restricciones. Una idea puede quedarse
en el backlog hasta que tenga información suficiente para ejecutarla.

## Ejemplo

```markdown
## Objetivo
Documentar cómo consultar los últimos partidos de un jugador.

## Criterios de aceptación
- La guía indica el endpoint existente y un ejemplo de respuesta.
- Los ejemplos coinciden con la implementación actual.
- Las pruebas existentes pasan.

## Restricciones
- Cambiar únicamente documentación.
- No añadir dependencias ni modificar el despliegue.
```

## Autorizar y controlar la ejecución

El propietario `ivan280809` aplica estas **etiquetas a la issue**:

| Etiqueta | Efecto |
| --- | --- |
| `factory:ready` | Autoriza el trabajo. Sin ella, la fábrica no lo inicia. |
| `factory:cancel` | Cancela la tarea. Es un estado terminal; un comentario no la reactiva. |
| `factory:retry` | Permite un reintento de una tarea `FAILED`, conservando su fase y reiniciando su presupuesto. La autorización se consume una vez. |

Si la fábrica espera una decisión humana o confirmación de un cambio de alcance,
el propietario responde con un **comentario** `factory:resume` seguido de su aclaración.
Esto devuelve la tarea a planificación. No sirve para reactivar tareas canceladas
ni para saltarse bloqueos técnicos, dependencias o cuarentenas.

La fábrica trabaja de forma secuencial. Asigna su propia rama
`codex/factory-issue-N`, ejecuta las pruebas y publica el resultado como una
**PR de borrador para revisión humana**. El comentario de estado de la issue
incluye fase, bloqueos, modelo, fecha de observación y enlace a la PR.
Una observación antigua no garantiza que el PC siga encendido.

## Revisar el resultado

Revisa el diff y las pruebas antes de aceptar los cambios. La fábrica no fusiona
PRs ni despliega automáticamente. El despliegue requiere una decisión separada.
La rama principal del proyecto es `master`.

La [plantilla de tareas](https://github.com/ivan280809/lol-match-tracker/tree/codex/factory-setup/.github/ISSUE_TEMPLATE)
y el [flujo del proyecto](https://github.com/ivan280809/lol-match-tracker/blob/codex/factory-setup/WORKFLOW.md)
están preparados en la PR de configuración. La plantilla aparecerá en la pantalla
de nuevas issues cuando esa PR se integre en `master`; entretanto puedes abrir
una issue en blanco con la estructura anterior.
