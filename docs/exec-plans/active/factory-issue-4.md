# Issue #4: P1: Optimizar el filtrado del historial en PostgreSQL

## Reparacion del 27 de octubre de 2026

La combinacion con Java 26 y PostgreSQL 15 detecta un p95 superior a 500 ms
en CI 40431533041. Se conserva el umbral y se registra la medicion antes de
afirmar su cumplimiento. El EXPLAIN usa la consulta real del repositorio con
los mismos parametros; la copia anterior no representaba todas sus expresiones.
Primero se analizara este plan y despues se aplicara la optimizacion necesaria.

CI 40433207036 registra p95 505,252 ms, Java 26.0.2.1 y PostgreSQL 15.19
en un runner Linux de cuatro CPU. EXPLAIN de la consulta real tarda 354,845 ms
y evalua el filtro de texto tras unir 50.000 filas. CONCAT conserva expresiones
de etiquetas constantes en el filtro. Se preparan los patrones de contenido una
sola vez en Java, manteniendo escapes y valores nulos, y se enlazan con CAST
para conservar el tipado y evitar que Spring Data reescriba los parametros LIKE.
No se cambia el umbral, ni se anaden indices o dependencias. Pendiente repetir
la medicion sobre la combinacion final antes de afirmar una mejora.

Se conserva el trabajo de la issue en una copia aislada. La reproduccion inicial
falla en tres pruebas de etiquetas de cola: TRIM SQL no reproduce String.isBlank.
Se pasara a SQL el conjunto de caracteres que Java considera whitespace para
conservar el fallback en el modelo normalizado y el historico. Se repetiran las
pruebas de equivalencia y se mantendran pendientes el benchmark de 100.000 filas
y PostgreSQL real mientras Docker no inicie. No se entrega la issue como cerrada.

Plan a focused repair for the startup failure: Hibernate rejects a multi-character ESCAPE literal in the legacy history query. The supplied owner clarification targets PlayerMatchRepository.java, but the verification trace identifies the failing query in TrackedMatchRepository.java. Preserve current work and repair the actual failing query; do not apply the stale replacement to PlayerMatchRepository.java.

## Steps

- In src/main/java/com/loltracker/app/match/TrackedMatchRepository.java, change the champion predicate’s ESCAPE literal from the two-character runtime value (`'\\'` in the Java text block) to a one-character backslash escape (`'\'` in the Java text block).
- Apply the same correction to every other ESCAPE literal in that query that currently evaluates to two characters, including the text-filter champion predicate. Keep LIKE escaping, Greek sigma normalization, casts, queue-label fallback, and query semantics unchanged.
- Leave src/main/java/com/loltracker/app/match/PlayerMatchRepository.java unchanged; its native query has no ESCAPE literals, and the reported stale replacement count of 1 confirms the supplied exact replacement does not match there.
- Run the targeted persistence/startup validation and the required Maven verification with Java 17 and Docker; confirm Spring Data creates the repository query and no tests are skipped.
- Review the diff and report the repair and verification evidence.

## Acceptance

- The application context starts without Hibernate’s “Escape character literals must have exactly a single character” query-validation error.
- The history query retains its existing filters, Greek sigma normalization, wildcard escaping semantics, queue-label fallback, and cast behavior.
- Relevant persistence and history-search tests pass, and required verification completes with no errors, failures, or skipped tests.
- The diff is limited to the query repair and any directly necessary regression coverage.
