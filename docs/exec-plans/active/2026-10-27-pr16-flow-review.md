# Revision de flujo de la PR #16

Encargo: revisar issues y PR pendientes, reproducir fallos y corregirlos.

## Alcance

- Anadir validacion de PR en GitHub sin publicacion ni acceso al miniPC.
- Mantener Java alineado con `pom.xml` para integrar posteriormente la PR #17.
- Rechazar informes Maven con errores, fallos, omisiones o sin pruebas.
- Probar que el helper local de Telegram no propaga cabeceras Retry-After arbitrarias.
- Repetir pruebas del gestor y del flujo de la aplicacion sin llamadas reales.

## Comprobaciones y limites

Docker Desktop no inicia en este equipo por un socket local inaccesible. La
limpieza forzada fue rechazada por la revision automatica. Las pruebas que
necesitan PostgreSQL real, el build de imagen y la activacion del miniPC siguen
pendientes; una ejecucion parcial no acredita el release completo.

La copia principal del propietario y las factorias anteriores se conservan.
Las PR siguen requiriendo revision humana; este trabajo no autoriza despliegue.
