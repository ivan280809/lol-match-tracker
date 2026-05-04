# LOL Match Tracker

## Docker Compose

La forma mas simple de levantar la aplicacion completa es con Docker Compose.

### Opcion 1: desarrollo local desde el repositorio

1. Copia `.env.example` a `.env`
2. Rellena las claves de Riot, Telegram y `APP_CONFIG_ENCRYPTION_KEY`
3. Ejecuta:

```bash
docker compose up --build -d
```

### Opcion 2: despliegue en otro dispositivo sin copiar el proyecto

Cuando la rama `master` o `main` publique la imagen en `GHCR`, el otro dispositivo ya no necesitara este repositorio para ejecutar la aplicacion. Solo necesitara:

- `docker-compose.deploy.yml`
- un fichero `.env` o cualquier `--env-file`
- acceso a Docker

Ejemplo:

```bash
docker compose --env-file /ruta/a/lol-tracker.env -f docker-compose.deploy.yml up -d
```

El fichero de variables puede incluir:

- `APP_IMAGE=ghcr.io/ivan280809/lol-match-tracker:latest`
- el resto de credenciales y configuracion del servicio

Importante:

- si el paquete de `GHCR` es privado, en el otro equipo tendras que hacer `docker login ghcr.io`
- si lo marcas como publico en GitHub Packages, no necesitara login para descargar la imagen
- GitHub no me ha dejado automatizar ese cambio de visibilidad desde Actions: hazlo en la pagina del paquete, `Package settings` -> `Danger Zone` -> `Change visibility` -> `Public`

## Despliegue automatico en el mini PC

El workflow `.github/workflows/publish-ghcr.yml` hace tres cosas al recibir un push a `master` o `main`, o al lanzarlo manualmente desde una de esas ramas:

1. Ejecuta los tests con Maven.
2. Construye y publica la imagen en `GHCR` con dos tags: `latest` y el SHA completo del commit.
3. Ejecuta un job de despliegue en un runner self-hosted con labels `self-hosted`, `linux` y `minipc`.

El job de despliegue usa un project name estable de Docker Compose: `lol-match-tracker`. Tambien copia `docker-compose.deploy.yml` a `/opt/lol-match-tracker/docker-compose.deploy.yml`, carga secretos desde `/opt/lol-match-tracker/lol-tracker.env`, hace `docker compose pull`, reinicia con `docker compose up -d --remove-orphans` y comprueba `/actuator/health`.

### Preparar el mini PC

El mini PC debe tener Docker Engine, el plugin `docker compose`, `curl` y un usuario para ejecutar el runner. Ese usuario debe poder usar Docker sin `sudo`.

Ejemplo:

```bash
sudo usermod -aG docker $USER
```

Despues de cambiar el grupo, cierra sesion y vuelve a entrar, o reinicia el servicio del runner si ya existe.

Crea la carpeta de despliegue y deja que el usuario del runner pueda escribir en ella:

```bash
sudo mkdir -p /opt/lol-match-tracker
sudo chown -R $USER:$USER /opt/lol-match-tracker
```

Crea el fichero real de entorno fuera del repo:

```bash
cp .env.example /opt/lol-match-tracker/lol-tracker.env
chmod 600 /opt/lol-match-tracker/lol-tracker.env
```

Edita `/opt/lol-match-tracker/lol-tracker.env` y rellena las variables reales. No subas este fichero a GitHub.

### Registrar el self-hosted runner

En GitHub:

1. Entra en `Settings` -> `Actions` -> `Runners`.
2. Pulsa `New self-hosted runner`.
3. Elige Linux y copia los comandos oficiales que te da GitHub.
4. Al ejecutar `config.sh`, anade el label custom `minipc`.

GitHub anade normalmente los labels `self-hosted` y `linux` de forma automatica. El workflow requiere tambien `minipc`.

Despues, instala el runner como servicio:

```bash
sudo ./svc.sh install
sudo ./svc.sh start
sudo ./svc.sh status
```

Cuando el runner aparezca `online` en GitHub, el siguiente push a `master` o `main` deberia publicar la imagen y desplegarla en el mini PC.

### GHCR privado o publico

El workflow hace login en `ghcr.io` con `GITHUB_TOKEN` antes del deploy, asi que normalmente no necesitas guardar credenciales de GHCR en el mini PC para el despliegue automatico.

Para pulls manuales desde el mini PC, tienes dos opciones:

- hacer publico el paquete en GitHub Packages,
- o ejecutar `docker login ghcr.io` con un token personal que tenga permiso de lectura de paquetes.

### Comandos de comprobacion en el mini PC

```bash
docker compose --env-file /opt/lol-match-tracker/lol-tracker.env \
  --file /opt/lol-match-tracker/docker-compose.deploy.yml \
  --project-name lol-match-tracker ps

docker compose --env-file /opt/lol-match-tracker/lol-tracker.env \
  --file /opt/lol-match-tracker/docker-compose.deploy.yml \
  --project-name lol-match-tracker logs -f lol-match-tracker

curl -fsS http://localhost:8085/actuator/health
```

### Opcion 3: usar un fichero de secretos en otra ruta para build local

No hace falta que el archivo se llame `.env`. Tambien puedes usar:

```bash
docker compose --env-file /ruta/a/lol-tracker.env up --build -d
```

### Comandos utiles

```bash
docker compose logs -f lol-match-tracker
docker compose ps
docker compose down
```

### Que necesitas en otro dispositivo

Si usas el `docker-compose.yml` principal, `docker compose` construye la imagen desde este proyecto. Eso significa que en el otro dispositivo necesitas al menos una copia del repositorio con:

- `Dockerfile`
- `docker-compose.yml`
- el codigo fuente
- un fichero `.env` o cualquier `--env-file` con tus secretos

Si usas `docker-compose.deploy.yml` con una imagen publicada en `GHCR`, el otro dispositivo solo necesita:

- `docker-compose.deploy.yml`
- el fichero de secretos
- y opcionalmente `docker login ghcr.io` si la imagen no es publica

### Variables necesarias

- `APP_IMAGE`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `RIOT_API_KEY`
- `RIOT_API_REGION`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `APP_CONFIG_ENCRYPTION_KEY`
- `APP_POLL_FIXED_DELAY`
- `APP_POLL_INITIAL_DELAY`

### Configuracion desde la UI

El dashboard permite guardar Riot API key, region Riot, Telegram bot token y Telegram chat id.
Los secretos se almacenan cifrados en PostgreSQL. Manten estable `APP_CONFIG_ENCRYPTION_KEY`,
porque si cambia no se podran descifrar los valores ya guardados.
