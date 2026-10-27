# Despliegue verificado del miniPC

Este paquete prepara Linux amd64 con Docker Engine, Compose y Python 3.10+.
No supone que el Windows desde el que se preparó sea el miniPC de producción.
No instalar desde una PR: revisar, fusionar manualmente y usar exclusivamente
`master`. No hace falta ejecutar código de PR en un runner del miniPC.

## Estado y límite inicial

La imagen fusionada `c81bf83169c1a8103c417ec38f22533a54c11b22` falla al arrancar
sobre PostgreSQL vacío: falta la integración automática Flyway de Spring Boot 4.
Esta propuesta añade `spring-boot-starter-flyway` y cambia la prueba PostgreSQL
para usar migración automática en el arranque. El test reproduce el fallo sin
el cambio y pasa con él. No se publicó una imagen de esta propuesta.

La página pública es una demostración sin datos ni llamadas Riot. La aplicación
real y todas las operaciones quedan en `127.0.0.1:18086`, con Basic Auth.
El guard del código actual protege todo el dashboard; **no hay todavía una UI
de consultas en vivo separada de administración**. No se debe quitar el guard
ni conectar el túnel al puerto administrativo para resolver esto.

## 1. Inspección del destino (sin mostrar secretos)

Antes de instalar, comprobar `uname -m`, `/etc/os-release`, `docker info`,
`docker compose version`, `free -h`, `df -h`, `ss -lnt`, nombres/imágenes/puertos
de `docker ps`, y nombres/estado de servicios cloudflared, SWAG, nginx o Caddy.
No imprimir `docker inspect` completo, variables, comandos de cloudflared,
credenciales JSON, `.env`, ni ficheros de configuración sin filtrar.

Inventariar los hostnames y códigos HTTPS existentes para comparar después.
Determinar si el túnel se gestiona en Cloudflare o por ingress local. Conservar
copia protegida de su configuración antes de añadir una ruta. No reemplazar
el fichero o la lista de ingress completos. No abrir puertos del router.

Esta versión se detiene si Docker no es linux/amd64. Si el miniPC es ARM,
adaptar la plataforma de publicación, probarla y fusionar ese cambio primero.

## 2. Preparación después de fusionar

Cancelar en GitHub cualquier job antiguo `deploy-minipc` que siga en cola;
no poner un runner antiguo online para consumirlo. Confirmar que el nuevo
workflow de master termina completamente con éxito.

En una ubicación NUEVA, sin sobrescribir un despliegue previo:

```sh
sudo git clone --single-branch --branch master https://github.com/ivan280809/lol-match-tracker.git /opt/lol-tracker
cd /opt/lol-tracker/ops/minipc
sudo cp config.example.json config.json
sudo chmod 600 config.json
sudo python3 manager.py init
sudo python3 manager.py set-secret ADMIN_PASSWORD
sudo python3 manager.py set-secret GITHUB_READ_TOKEN
sudo python3 manager.py check
sudo python3 manager.py update
sudo python3 manager.py status
```

Los valores de las claves se solicitan con entrada oculta, sin argumentos ni
historial. `init` conserva valores existentes. Los secretos y estado van a
`/var/lib/lol-tracker`, fuera de Git, con directorio 700 y archivos 600. En
Windows, solo la validación local está soportada; `init` aplica ACL al usuario
actual y SYSTEM. No cambiar permisos de rutas ajenas.

Token GitHub: fine-grained PAT limitado a este repositorio, **Actions: read** y
**Contents: read**, sin escritura. Es necesario para descargar el artifact de
Actions aunque la imagen GHCR sea pública. El actualizador no lo pasa al
redirect del almacenamiento de artifacts ni a contenedores.
Los artifacts caducan a los 90 días; ejecutar nuevamente el workflow de master
si hace falta una entrega fresca. No relajar verificación por caducidad.

GHCR es público en la comprobación realizada: no requiere token de registro.
Si se vuelve privado, usar una credencial de lectura `read:packages` con acceso
solo a los paquetes necesarios, introducirla por `docker login --password-stdin`
desde entrada local oculta y almacenar el config Docker protegido. En ese caso,
adaptar `DOCKER_CONFIG` a `/var/lib/lol-tracker/registry` en las unidades (el
hardening de systemd oculta `/root`); no guardar credenciales en Compose.

`init` genera DB_PASSWORD, APP_CONFIG_ENCRYPTION_KEY y contraseña del guard.
Spring lee archivos montados por `SPRING_CONFIG_IMPORT=configtree:/run/secrets/`.
Los nombres de archivo son propiedades Spring reales; no se espera una variable
`RIOT_API_KEY_FILE` inexistente. No introducir una contraseña nueva de PostgreSQL
solo cambiando el archivo sobre una base existente: el usuario SQL también debe
rotarse coordinadamente. No adoptar bases o volúmenes anteriores automáticamente.

## 3. Actualización y arranque

```sh
sudo systemctl enable --now docker
sudo install -m 644 systemd/lol-tracker-*.service systemd/lol-tracker-*.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now lol-tracker-update.timer lol-tracker-backup.timer
sudo systemctl list-timers 'lol-tracker-*'
```

Instalar timers solo después del primer `update` satisfactorio. No modificar
servicios Docker/cloudflared existentes si ya están configurados correctamente.
Los contenedores usan `unless-stopped`; el túnel existente debe tener arranque
automático gestionado por el sistema. Un reinicio real de todo el equipo afecta
otras webs: coordinarlo y comprobarlas antes/después; no afirmar que está probado
solo por reiniciar estos contenedores.

El updater selecciona un workflow exitoso de **la cabeza actual de master**,
rechaza PRs/forks/otros workflows, verifica checksum del artifact, SHA, intento,
digest, plataforma y labels de imagen. Un lock excluye despliegue/backup simultáneos.
Conserva estado anterior y las imágenes; no ejecuta `docker image prune` ni borra
volúmenes. Nunca ejecuta scripts descargados del artifact y no autoactualiza el
propio manager/Compose; las modificaciones de infraestructura requieren revisión
y actualización manual desde master con timers parados.

Antes de reemplazar la aplicación, para sus escrituras, hace backup y lo restaura
en una base temporal. Solo registra la entrega después de salud UP. Si falla y
el esquema y las migraciones no han cambiado, recupera la imagen previa y verifica
salud. La misma entrega fallida queda bloqueada hasta revisión. Si cambió el
esquema o hubo una interrupción, deja `pending.json`, preserva datos y exige
recuperación manual; no arranca una imagen antigua contra un esquema incompatible.

Cuando cambian migraciones, el timer se detiene antes de modificarlas. Revisar
compatibilidad y ventana de mantenimiento, y ejecutar conscientemente:

```sh
sudo python3 manager.py update --allow-schema-change SHA_COMPLETO_REVISADO
```

Ese permiso no habilita rollback automático de una migración. Conservar la
imagen anterior y el backup para restauración coordinada si hace falta.

## 4. Cloudflare sin alterar otras webs

Con Tunnel existente, añadir **solo** `lol.iroberto.dev` apuntando a
`http://127.0.0.1:18085` si cloudflared corre en el host. El trayecto remoto
Cloudflare→cloudflared va cifrado por Tunnel; ese HTTP es exclusivamente loopback,
no SSL Flexible. Si cloudflared corre en contenedor, `127.0.0.1` es su contenedor:
integrar solo el proxy público en la red apropiada y usar su nombre y puerto,
o el proxy TLS existente. No conectar la base de datos ni el puerto administrativo.

En túnel gestionado remotamente, añadir la ruta publicada desde el dashboard
conservando todas las rutas previas; verificar el CNAME creado. En túnel local,
insertar la nueva regla antes del catch-all, validar con
`cloudflared tunnel ingress validate` y comprobar el hostname con
`cloudflared tunnel ingress rule https://lol.iroberto.dev` antes de recargar.
No imprimir credenciales del túnel. El DNS debe apuntar al mismo túnel existente.

Si el esquema usa un origen HTTPS fuera de loopback, reutilizar su certificado
válido y validación de nombre/cadena: Cloudflare Full (strict), nunca Flexible
ni `noTLSVerify`. Si hace falta token DNS, limitarlo a la zona `iroberto.dev`,
Zone Read / DNS Edit; no usar Global API Key. La API de configuración de túnel
necesitaría permisos distintos a DNS: preferir el dashboard ya autenticado.

Añadir regla Cache Bypass para todo `lol.iroberto.dev`; comprobar que no la anula
una regla Cache Everything. El proxy envía `Cache-Control: no-store, private`.

Administración inicial: túnel SSH privado a 127.0.0.1:18086 o sesión en el miniPC.
No exponer Docker/PostgreSQL/actuator. Cloudflare Access puede proteger en el
futuro un hostname administrativo separado, con identidad del propietario,
política deny-by-default y validación de Access en el origen; no usar Access
sobre toda la web si se habilitan consultas públicas. Ese cambio está pendiente
de inspección y del diseño de autorización, no se presupone configurado aquí.

## 5. Operación

```sh
cd /opt/lol-tracker/ops/minipc
sudo python3 manager.py status
sudo journalctl -u lol-tracker-update.service -n 60 --no-pager
sudo journalctl -u lol-tracker-backup.service -n 60 --no-pager
sudo docker logs --tail 100 lol-tracker-app-1
# Parar: primero evitar que un timer aplique otra entrega.
sudo systemctl stop lol-tracker-update.timer lol-tracker-backup.timer
sudo python3 manager.py stop
# Recuperar la entrega registrada si no hay transacción pendiente.
sudo python3 manager.py start
sudo systemctl start lol-tracker-update.timer lol-tracker-backup.timer
# Rollback solo si los hashes de esquema coinciden.
sudo systemctl stop lol-tracker-update.timer
sudo python3 manager.py rollback
```

Mantener parado el timer tras rollback hasta revisar la entrega problemática;
de otro modo podría volver a seleccionar master. Los logs de aplicación deben
consultarse localmente y no compartirse sin revisar, pues pueden contener datos
de jugadores o respuestas externas. No habilitar logs HTTP de cuerpos/headers.

## 6. Backup y restauración

```sh
sudo python3 manager.py backup
```

Se crean `.dump` PostgreSQL custom y metadatos SHA-256 en
`/var/lib/lol-tracker/backups`; cada backup se restaura en una base temporal y
solo se considera completo si esa restauración tiene éxito. Nunca se restaura
sobre la base viva durante la comprobación. Los dumps parciales se conservan
para diagnóstico. Los datos están en volumen `lol-tracker_pgdata`, fuera del
contenedor; backups y secretos están fuera del volumen.

No hay borrado automático de copias. Monitorizar espacio; el manager exige
espacio libre para dump y restauración. Definir una retención y destino externo
cifrado cuando el propietario indique el almacenamiento de backup. Una copia
en el mismo disco no cubre avería del miniPC. Copiar también, cifrados y con ACL
restrictiva, secrets, current/previous/pending y la revisión de infraestructura.

Restauración manual sin borrar la base existente (operador en el miniPC):

1. Parar ambos timers y aplicación; conservar un backup del estado actual.
2. Verificar SHA-256 del dump contra su `.json`, identificar la imagen exacta
   registrada en `current` dentro de esos metadatos y recuperar su clave de cifrado.
3. Crear una base NUEVA, por ejemplo `loltracker_restore_20261011`, con
   `docker exec lol-tracker-postgres-1 createdb -U loltracker NOMBRE_NUEVO`.
4. Restaurar sin `--clean`: `docker exec -i lol-tracker-postgres-1 pg_restore -U loltracker -d NOMBRE_NUEVO --exit-on-error --no-owner --no-acl < COPIA.dump`.
   Revisar tablas, recuentos y `flyway_schema_history` con psql local.
5. Con todas las conexiones de aplicación cerradas, desde la base `postgres`,
   renombrar `loltracker` a un nombre NUEVO de conservación y renombrar la base
   restaurada a `loltracker`. Si hay conexiones, investigar primero; no borrar
   ni forzar una base que pueda pertenecer a otro servicio.
6. Guardar aparte el estado fallido; restaurar `current.json` a la entrega del
   backup, conservar `pending.json` en historia para diagnóstico y retirarlo de
   la ruta activa solo tras resolver la transacción. Recuperar los secretos que
   correspondan. Iniciar la imagen exacta con `manager.py start`; validar salud
   y consultas antes de reactivar timers y publicación.

La aplicación puede contener cambios en datos además de esquema: la recuperación
de un backup pierde los cambios posteriores a esa copia. No hacer ese cambio
automáticamente ni sin revisar el alcance.

## 7. Riot y Telegram: entrada local y rotación

Riot: acceder a https://developer.riotgames.com con tu cuenta, registrar el producto
y solicitar Production para una aplicación pública, con descripción, URL y flujo
demostrable. Development caduca cada 24 horas. Personal es para uso privado,
no para ofrecer acceso público vivo. Mantener esta demostración sin llamadas
Riot hasta aprobación. Referencias: https://developer.riotgames.com/docs/portal y
https://support-developer.riotgames.com/hc/en-us/articles/22801383038867-Production-Key-Applications .

Propiedades confirmadas: RIOT_API_KEY → `riot.api.key`, RIOT_API_REGION →
`riot.api.region` (EUROPE). La plataforma se elige por jugador, por defecto EUW1;
el cliente usa el host regional para Account/Match y el de plataforma para los
endpoints correspondientes. El código trata 429/Retry-After. No reducir backoff
ni paralelizar indiscriminadamente polling; verificar las cuotas de tu producto.

Telegram: `/newbot` en @BotFather; abrir el chat del bot y enviar `/start`, o
añadirlo a un grupo con permiso de enviar mensajes y sin privilegios de admin
innecesarios. Dejar Privacy Mode salvo necesidad justificada. Introducir el
token localmente y descubrir el chat sin bots de terceros:

```sh
sudo python3 manager.py set-secret RIOT_API_KEY
sudo python3 manager.py set-secret TELEGRAM_BOT_TOKEN
sudo python3 integrations.py telegram-discover
sudo python3 manager.py set-secret TELEGRAM_CHAT_ID
sudo python3 integrations.py telegram-test
sudo python3 integrations.py riot-check
```

`telegram-discover` comprueba primero getWebhookInfo, no muestra su URL y no
elimina webhooks. Si existe uno, usar su receptor para identificar el chat.
Sin webhook, getUpdates extrae solo ID/tipo, sin mensajes ni usuarios. El test
envía un único mensaje silencioso al chat configurado; guarda una marca antes
de llamar para no duplicarlo ni siquiera ante una respuesta de red incierta.
No reintentar quitando esa marca sin comprobar antes el chat.
Referencia: https://core.telegram.org/bots/api . No se necesitan claves en builds.

Rotación: generar reemplazo Riot en su portal y revocar el anterior; revocar y
regenerar Telegram con BotFather; usar `set-secret` localmente y recrear app con
la misma imagen digest para que vuelva a montar/leer secretos. Un simple restart
no garantiza sustituir un bind mount tras escritura atómica del fichero.
Las claves persistidas desde el dashboard tienen prioridad sobre los archivos:
rotar también esos valores desde la administración privada. La clave
APP_CONFIG_ENCRYPTION_KEY cifra datos persistidos: no cambiarla sin recifrado
probado y backup de la anterior.

Revocar/renovar el PAT GitHub y sustituir `GITHUB_READ_TOKEN`; el manager lo lee
en cada consulta. Rotar token Cloudflare con los mismos permisos mínimos;
rotar la credencial del túnel siguiendo el procedimiento de ese túnel, validar
conectividad y luego revocar la anterior. Proteger siempre archivos de túnel
y backups de credenciales, nunca publicarlos en PR, frontend, logs o correo.

## Verificación final necesaria en destino

- master fusionado → Actions completo exitoso → artifact → digest → updater → salud.
- DNS público y HTTPS desde una red externa; certificado y comportamiento cache.
- Hostnames existentes antes/después, sin regresión.
- PostgreSQL sin puertos publicados; administración/actuator bloqueados públicamente.
- Reinicio real del equipo, Docker, cloudflared y timers recuperados.
- Restauración con datos reales en base aislada y prueba controlada de rollback.
- Riot autorizado y Telegram confirmado solo cuando se configuren secretos.

Hasta completar esas comprobaciones, este paquete no equivale a un despliegue
público terminado.
