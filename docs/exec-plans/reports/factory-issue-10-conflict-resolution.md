# PR #15: resolución con master

Se incorpora master 181d76f sin perder las actualizaciones del wrapper/plugins ni las pruebas de transporte. Se conserva Spring Boot 4.1.1 y su mapper de compatibilidad Jackson 2; se retira el bean duplicado de HttpClientConfig. El plugin Spring Boot hereda la versión del parent. La fixture de rotación Telegram inicializa su URL configurable.

Validación: Java 17, `./mvnw.cmd -B -ntp clean verify`, BUILD SUCCESS; 228 pruebas, cero fallos, errores u omisiones, con PostgreSQL/Testcontainers ejecutado.
