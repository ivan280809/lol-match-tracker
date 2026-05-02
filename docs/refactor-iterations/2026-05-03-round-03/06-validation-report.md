# Round 03 Validation Report

## Local Tests

- `.\mvnw.cmd test`
- Result: 40 tests, 0 failures, 0 errors.

## Docker Validation

- `docker compose up --build -d`
- Result: application and PostgreSQL containers started successfully.
- Dashboard verified at `http://localhost:8080/`.
- Player form now renders a `Servidor` selector with `EUW - Europe West` selected by default.
- Player table now includes a `SERVIDOR` column.

## Database Validation

- `players.platform` exists as `varchar(16) not null default 'EUW1'`.
- Existing `game_name + tag_line` uniqueness remains in place.

## Notes

- The platform selector does not replace the Riot ID tag. Riot Account-V1 still requires `gameName#tagLine` to resolve PUUID.
