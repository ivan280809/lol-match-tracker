# Guía de Contribución a Software Factory

Esta guía describe cómo crear **tareas ejecutables** (issues) que el **Software Factory** de este repositorio pueda procesar.  Es el piloto aprobado y no cambia el comportamiento de la aplicación.

---

## Enlace a los Issues del proyecto

Todas las tareas deben crearse como *issues* dentro de este repositorio:

- https://github.com/ivan280809/lol-match-tracker/issues

---

## Estructura mínima de una tarea

Cada issue que represente una tarea debe incluir, al menos, los siguientes bloques:

1. **Objetivo** – qué se quiere conseguir con la tarea.
2. **Criterios de aceptación** – condiciones que deben cumplirse para que la tarea se considere completada.
3. **Restricciones / Contenido mínimo** – requisitos obligatorios (por ejemplo, nombre de la rama, formato de los cambios, test que deben añadirse, etc.).

> **Nota:** No es necesario incluir código dentro del issue; el objetivo es describir la tarea que será desarrollada.

---

## Flujo de ejecución en Software Factory

El Software Factory sigue el siguiente protocolo de control de flujo:

1. **factory:ready** – Se publica el issue con la etiqueta `factory:ready`.  El Factory inicia la tarea.
2. **factory:cancel** – Si durante la ejecución se determina que la tarea no debe continuar (por ejemplo, se encuentra un error crítico), se publica un comentario con la etiqueta `factory:cancel` y la tarea se detiene.
3. **factory:resume** – Una vez corregido el motivo de cancelación, se comenta `factory:resume` para re‑iniciar la ejecución.

La respuesta `factory:resume` indica que el Factory ha re‑iniciado la tarea y continuará hasta que se completen todos los pasos o se genere una nueva cancelación.

---

## Entregable final

El resultado de la tarea es **una Pull Request** que requiere revisión humana.  El Software Factory **no** fusiona ni despliega automáticamente la PR.  Solo después de que un responsable apruebe la PR y se haga merge, los cambios se desplegarán siguiendo el flujo habitual de CI/CD.

---

## Buenas prácticas adicionales

- Mantén los títulos de los issues claros y breves.
- Utiliza plantillas de issue si están disponibles (por ejemplo, `Issue Template` en la sección `templates`).
- Añade la etiqueta `software-factory` para identificar rápidamente los issues que deben ser procesados por el Factory.
- Si la tarea depende de recursos externos (por ejemplo, credenciales), crea una rama de trabajo y enlaza el issue con la PR.

---

## Referencias

- **Software Factory**: https://github.com/ivan280809/lol-match-tracker/tree/main/docs
- **Plantillas de Issues**: https://github.com/ivan280809/lol-match-tracker/tree/main/.github/ISSUE_TEMPLATE
- **Política de Revisión de PR**: https://github.com/ivan280809/lol-match-tracker/blob/main/CONTRIBUTING.md (este mismo documento).

---

> Esta guía se mantiene actualizada y revisada periódicamente para reflejar las mejores prácticas de desarrollo.
