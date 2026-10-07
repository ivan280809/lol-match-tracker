# Issue #11: Actualizar Maven Wrapper y plugins de build

El problema del 404 ya está diagnosticado: la URL y las versiones 3.10.13/3.3.12 del plan no existen. El worktree contiene la URL válida de Maven 3.10.0, el wrapper 3.3.4 y un SHA-256 verificado; también consta una verificación completa satisfactoria con caché limpia. El POM ya fija compiler 3.16.0 y surefire 3.6.0; el plugin Spring Boot hereda su versión del parent 4.0.1. Puede continuar la fase de implementación autorizada para revisar el estado actual, reparar solo lo necesario dentro del alcance y ejecutar la verificación solicitada.

## Steps

- Conservar los cambios existentes y comprobar la configuración actual del wrapper, del POM y de la documentación de build; no aplicar las versiones inexistentes del plan protegido.
- Revisar si los plugins efectivamente usados están fijados de forma reproducible y si la versión heredada del plugin Spring Boot corresponde al parent 4.0.1; ajustar solo si la revisión identifica una necesidad concreta dentro del issue.
- Documentar cómo actualizar el wrapper de forma reproducible, incluyendo checksum y compatibilidad Maven 3.x/Java 17.
- Ejecutar `./mvnw.cmd -B -ntp verify` desde el checkout con caché limpia y sin usar Maven global; registrar el resultado para revisión independiente.

## Acceptance

- La configuración descarga Maven 3.10.0 desde la URL oficial correcta y verifica su SHA-256 conocido.
- Los plugins usados tienen versiones mantenidas fijadas o heredadas explícitamente de manera comprobable; no se modifican dependencias de aplicación ni código funcional.
- La documentación describe el procedimiento reproducible de actualización del wrapper.
- La verificación del wrapper concluye satisfactoriamente desde caché limpia; sin resultado verde, no abrir PR.
