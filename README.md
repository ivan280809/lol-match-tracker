# LOL Match Tracker

## Docker Compose

La forma mas simple de levantar la aplicacion completa es con Docker Compose.

### Opcion 1: desarrollo local desde el repositorio

1. Copia `.env.example` a `.env`
2. Rellena las claves de Riot y Telegram
3. Ejecuta:

```bash
docker compose up --build -d
```

### Opcion 2: despliegue en otro dispositivo sin copiar el proyecto

Cuando la rama `master` publique la imagen en `GHCR`, el otro dispositivo ya no necesitara este repositorio. Solo necesitara:

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
- `RIOT_API_BASE_URL`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `APP_POLL_FIXED_DELAY`
- `APP_POLL_INITIAL_DELAY`
