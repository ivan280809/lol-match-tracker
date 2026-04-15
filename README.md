# LOL Match Tracker

## Docker Compose

La forma mas simple de levantar la aplicacion completa es con Docker Compose.

### Opcion 1: usar `.env` en la raiz

1. Copia `.env.example` a `.env`
2. Rellena las claves de Riot y Telegram
3. Ejecuta:

```bash
docker compose up --build -d
```

### Opcion 2: usar un fichero de secretos en otra ruta

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

Con la configuracion actual, `docker compose` construye la imagen desde este proyecto. Eso significa que en el otro dispositivo necesitas al menos una copia del repositorio con:

- `Dockerfile`
- `docker-compose.yml`
- el codigo fuente
- un fichero `.env` o cualquier `--env-file` con tus secretos

Si quieres no copiar el proyecto entero, la alternativa es publicar una imagen ya construida en Docker Hub o GHCR. En ese caso, el otro dispositivo solo necesitaria:

- un `docker-compose.yml` que apunte a la imagen publicada
- el fichero de secretos

### Variables necesarias

- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `RIOT_API_KEY`
- `RIOT_API_BASE_URL`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `APP_POLL_FIXED_DELAY`
- `APP_POLL_INITIAL_DELAY`
