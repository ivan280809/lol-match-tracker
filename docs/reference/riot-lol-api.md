# Riot League of Legends API Reference

Last reviewed: 2026-05-06.

This document is the local entry point for Riot/League of Legends API knowledge in this project. It is intentionally practical: what the API exposes, where each endpoint is routed, what matters for `lol-match-tracker`, and which official pages to revisit when Riot changes the surface.

## Official Sources

- Riot API reference: <https://developer.riotgames.com/apis>
- Riot Developer Portal guide: <https://developer.riotgames.com/docs/portal>
- League of Legends developer guide: <https://developer.riotgames.com/docs/lol>
- League of Legends support/reference article: <https://support-developer.riotgames.com/hc/en-us/articles/22698698001939-League-of-Legends>
- Riot Developer FAQ: <https://developer.riotgames.com/docs/faqs>
- Data Dragon versions: <https://ddragon.leagueoflegends.com/api/versions.json>
- Data Dragon CDN root: <https://ddragon.leagueoflegends.com/cdn/>
- Static game constants:
  - <https://static.developer.riotgames.com/docs/lol/seasons.json>
  - <https://static.developer.riotgames.com/docs/lol/queues.json>
  - <https://static.developer.riotgames.com/docs/lol/maps.json>
  - <https://static.developer.riotgames.com/docs/lol/gameModes.json>
  - <https://static.developer.riotgames.com/docs/lol/gameTypes.json>

## Identity Model

Riot moved player-facing lookup away from Summoner Names and toward Riot ID (`gameName#tagLine`) plus PUUID. For this application:

- PUUID is the stable functional identity.
- `gameName` and `tagLine` are mutable display/search fields.
- `summonerId` remains useful for some platform APIs but should not be the primary identity.
- Avoid new dependencies on player-facing Summoner Name behavior. Current Riot guidance recommends PUUID endpoints when available.

Useful identity endpoints:

| API | Routing | Endpoint | Use |
| --- | --- | --- | --- |
| `account-v1` | regional cluster | `GET /riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}` | Resolve Riot ID to PUUID. Primary player add/search flow. |
| `account-v1` | regional cluster | `GET /riot/account/v1/accounts/by-puuid/{puuid}` | Refresh mutable Riot ID display fields from PUUID. |
| `account-v1` | regional cluster | `GET /riot/account/v1/accounts/me` | RSO-only lookup for authenticated users. |
| `summoner-v4` | platform | `GET /lol/summoner/v4/summoners/by-puuid/{encryptedPUUID}` | Resolve LoL summoner profile fields from PUUID. |
| `summoner-v4` | platform | `GET /lol/summoner/v4/summoners/me` | RSO-only summoner lookup. |

Note: the API reference currently labels `account-v1` as `Riftbound RSO`, but the LoL support/reference docs still use Account-V1 for Riot ID and RSO identity. Treat it as a Riot account identity API shared by products, not as LoL-specific match data.

## Routing Values

Riot uses the host to route requests. Some APIs are platform-routed and others are regional-cluster-routed.

Platform routing values:

| Platform | Host |
| --- | --- |
| `BR1` | `br1.api.riotgames.com` |
| `EUN1` | `eun1.api.riotgames.com` |
| `EUW1` | `euw1.api.riotgames.com` |
| `JP1` | `jp1.api.riotgames.com` |
| `KR` | `kr.api.riotgames.com` |
| `LA1` | `la1.api.riotgames.com` |
| `LA2` | `la2.api.riotgames.com` |
| `NA1` | `na1.api.riotgames.com` |
| `OC1` | `oc1.api.riotgames.com` |
| `TR1` | `tr1.api.riotgames.com` |
| `RU` | `ru.api.riotgames.com` |
| `SG2` | `sg2.api.riotgames.com` |
| `TW2` | `tw2.api.riotgames.com` |
| `VN2` | `vn2.api.riotgames.com` |

Regional routing values:

| Region | Host | Serves |
| --- | --- | --- |
| `AMERICAS` | `americas.api.riotgames.com` | NA, BR, LAN, LAS |
| `ASIA` | `asia.api.riotgames.com` | KR, JP |
| `EUROPE` | `europe.api.riotgames.com` | EUNE, EUW, ME1, TR, RU |
| `SEA` | `sea.api.riotgames.com` | OCE, SG2, TW2, VN2 |

Project default today: `RIOT_API_REGION=EUROPE` and a player platform such as `EUW1`.

## Operational Rules

- API keys should be sent in `X-Riot-Token`; avoid query-string API keys except when manually debugging in the Riot portal.
- Development keys expire quickly. Personal keys are for private/personal products. Public or broader products need a production key and a verified app/website.
- Rate limits exist at application, method, and service levels, and are enforced per region.
- `429` must respect the `Retry-After` response header. Do not retry it immediately.
- Do not retry functional `4xx` responses such as bad request, invalid/forbidden key, missing account, or unsupported path.
- `5xx`, network timeouts, and malformed transient responses can use short backoff retries.
- Riot warns that error response bodies are not a stable contract. Application behavior should primarily branch on HTTP status, not exact body text.
- One API key is scoped to one product/game use case. Do not use multiple apps or keys to bypass rate limits.

## API Families

| API | Main value | Current project relevance |
| --- | --- | --- |
| `account-v1` | Riot account, Riot ID, PUUID, RSO identity. | Core. Use for `gameName#tagLine -> PUUID` and display refresh. |
| `summoner-v4` | LoL summoner profile from PUUID or RSO token. | Useful. Platform validation and optional profile fields. |
| `match-v5` | Match IDs, match detail, timelines, replays. | Core. Polling, reduced match storage, stats, dedupe. |
| `league-v4` | Ranked entries and high-tier league lists. | Core. Player rank display and refresh. |
| `league-exp-v4` | Paginated league entries by queue/tier/division. | Potential future import/backfill from ranked ladders. |
| `champion-mastery-v4` | Champion mastery totals and per-champion data. | Potential player profile enrichment. |
| `champion-v3` | Free champion rotation. | Low relevance for this tracker. |
| `lol-challenges-v1` | Challenge configs, percentiles, player challenge progress, challenge leaderboards. | Potential achievements/profile view. |
| `spectator-v5` | Current live game by PUUID. | Potential "currently in game" dashboard state. |
| `clash-v1` | Clash players, teams, tournaments. | Low to medium relevance for team/social features. |
| `lol-status-v4` | Platform service/incidents/status. | Useful for ops diagnostics when Riot is down. |
| `lol-rso-match-v1` | Authenticated match IDs/detail/timeline including custom matches. | Future RSO feature, not for API-key-only polling. |
| `tournament-v5` | Real tournament providers, tournaments, codes, callbacks, games, lobby events. | Out of current scope unless product becomes tournament tooling. |
| `tournament-stub-v5` | Test/stub tournament methods. | Dev/testing only for tournament flows. |
| Data Dragon | Static patch data and assets. | Useful for champion names/assets, items, spells, profile icons, localized labels. |
| Static constants | Seasons, queues, maps, modes, types, ranked assets. | Useful for queue labels and UI copy. |

## Endpoint Catalog

The list below was extracted from Riot's `api-details` endpoints on 2026-05-06. Re-check the official API reference before implementing new integrations.

### Account-V1

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/riot/account/v1/accounts/by-puuid/{puuid}` | Get account by PUUID. |
| `GET` | `/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}` | Get account by Riot ID. |
| `GET` | `/riot/account/v1/accounts/me` | Get account by RSO access token. |
| `GET` | `/riot/account/v1/active-shards/by-game/{game}/by-puuid/{puuid}` | Get active shard for a player. |
| `GET` | `/riot/account/v1/region/by-game/{game}/by-puuid/{puuid}` | Get active region for LoL/TFT. |

### Champion-Mastery-V4

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/champion-mastery/v4/champion-masteries/by-puuid/{encryptedPUUID}` | Get all champion mastery entries sorted by champion points. |
| `GET` | `/lol/champion-mastery/v4/champion-masteries/by-puuid/{encryptedPUUID}/by-champion/{championId}` | Get one champion mastery entry. |
| `GET` | `/lol/champion-mastery/v4/champion-masteries/by-puuid/{encryptedPUUID}/top` | Get top champion mastery entries. |
| `GET` | `/lol/champion-mastery/v4/scores/by-puuid/{encryptedPUUID}` | Get total champion mastery score. |

### Champion-V3

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/platform/v3/champion-rotations` | Get free-to-play champion rotations. |

### Clash-V1

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/clash/v1/players/by-puuid/{puuid}` | Get Clash players by PUUID. |
| `GET` | `/lol/clash/v1/teams/{teamId}` | Get Clash team by ID. |
| `GET` | `/lol/clash/v1/tournaments` | Get active/upcoming Clash tournaments. |
| `GET` | `/lol/clash/v1/tournaments/by-team/{teamId}` | Get tournament by team ID. |
| `GET` | `/lol/clash/v1/tournaments/{tournamentId}` | Get tournament by ID. |

### League-Exp-V4

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/league-exp/v4/entries/{queue}/{tier}/{division}` | Get league entries. |

### League-V4

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/league/v4/challengerleagues/by-queue/{queue}` | Get challenger league for a queue. |
| `GET` | `/lol/league/v4/entries/by-puuid/{encryptedPUUID}` | Get ranked entries in all queues for a PUUID. |
| `GET` | `/lol/league/v4/entries/{queue}/{tier}/{division}` | Get league entries by queue/tier/division. |
| `GET` | `/lol/league/v4/grandmasterleagues/by-queue/{queue}` | Get grandmaster league for a queue. |
| `GET` | `/lol/league/v4/leagues/{leagueId}` | Get league by ID, including inactive entries. |
| `GET` | `/lol/league/v4/masterleagues/by-queue/{queue}` | Get master league for a queue. |

### LoL-Challenges-V1

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/challenges/v1/challenges/config` | Get all challenge configuration and translations. |
| `GET` | `/lol/challenges/v1/challenges/percentiles` | Get challenge level percentiles by challenge, season, and level. |
| `GET` | `/lol/challenges/v1/challenges/{challengeId}/config` | Get one challenge configuration. |
| `GET` | `/lol/challenges/v1/challenges/{challengeId}/leaderboards/by-level/{level}` | Get top challenge players for Master, Grandmaster, or Challenger level. |
| `GET` | `/lol/challenges/v1/challenges/{challengeId}/percentiles` | Get percentiles for one challenge. |
| `GET` | `/lol/challenges/v1/player-data/{puuid}` | Get one player's challenge progress. |

### LoL-RSO-Match-V1

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/rso-match/v1/matches/ids` | Get match IDs for the authenticated player; includes custom matches. |
| `GET` | `/lol/rso-match/v1/matches/{matchId}` | Get one authenticated match. |
| `GET` | `/lol/rso-match/v1/matches/{matchId}/timeline` | Get one authenticated match timeline. |

### LoL-Status-V4

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/status/v4/platform-data` | Get League of Legends platform status. |

### Match-V5

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/match/v5/matches/by-puuid/{puuid}/ids` | Get match IDs by PUUID. Supports `startTime`, `endTime`, `queue`, `type`, `start`, and `count`. |
| `GET` | `/lol/match/v5/matches/by-puuid/{puuid}/replays` | Get player replay files. |
| `GET` | `/lol/match/v5/matches/{matchId}` | Get match detail by match ID. |
| `GET` | `/lol/match/v5/matches/{matchId}/timeline` | Get match timeline by match ID. |

Match-V5 detail is the main source for reduced persisted match data. For `lol-match-tracker`, keep extracting only the fields needed by dedupe, UI, stats, and notifications: match ID, queue, duration, game end, platform/region, participants, champion, lane/role, KDA, CS, gold, damage, vision, result, and tracked-player participation.

### Spectator-V5

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/spectator/v5/active-games/by-summoner/{encryptedPUUID}` | Get current game information for a PUUID. |

### Summoner-V4

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/lol/summoner/v4/summoners/by-puuid/{encryptedPUUID}` | Get summoner profile by PUUID. |
| `GET` | `/lol/summoner/v4/summoners/me` | Get summoner profile by RSO access token. |

### Tournament-Stub-V5

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/lol/tournament-stub/v5/codes` | Create a stub tournament code. |
| `GET` | `/lol/tournament-stub/v5/codes/{tournamentCode}` | Get a stub tournament code DTO. |
| `GET` | `/lol/tournament-stub/v5/lobby-events/by-code/{tournamentCode}` | Get stub lobby events by tournament code. |
| `POST` | `/lol/tournament-stub/v5/providers` | Create a stub tournament provider. |
| `POST` | `/lol/tournament-stub/v5/tournaments` | Create a stub tournament. |

### Tournament-V5

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/lol/tournament/v5/codes` | Create tournament codes. |
| `GET` | `/lol/tournament/v5/codes/{tournamentCode}` | Get tournament code DTO. |
| `PUT` | `/lol/tournament/v5/codes/{tournamentCode}` | Update pick type, map, spectator type, or allowed PUUIDs for a code. |
| `GET` | `/lol/tournament/v5/games/by-code/{tournamentCode}` | Get game details by tournament code. |
| `GET` | `/lol/tournament/v5/lobby-events/by-code/{tournamentCode}` | Get lobby events by tournament code. |
| `POST` | `/lol/tournament/v5/providers` | Create tournament provider and callback URL. |
| `POST` | `/lol/tournament/v5/tournaments` | Create tournament under a provider. |

Tournament codes are for custom game tournament workflows. Riot's docs note that provider/callback setup is strict, and Match-V5 can be used with match IDs produced by completed tournament games.

## Data Dragon And Static Data

Data Dragon provides versioned static League data and assets:

- Latest versions list: `https://ddragon.leagueoflegends.com/api/versions.json`.
- Champion summaries: `/cdn/{version}/data/{locale}/champion.json`.
- Champion details: `/cdn/{version}/data/{locale}/champion/{championName}.json`.
- Champion square icons: `/cdn/{version}/img/champion/{championName}.png`.
- Champion splash/loading images: `/cdn/img/champion/splash/{championName}_{skinNum}.jpg` and `/cdn/img/champion/loading/{championName}_{skinNum}.jpg`.
- Items: `/cdn/{version}/data/{locale}/item.json` and `/cdn/{version}/img/item/{itemId}.png`.
- Summoner spells: `/cdn/{version}/data/{locale}/summoner.json` and `/cdn/{version}/img/spell/{spellKey}.png`.
- Profile icons: `/cdn/{version}/data/{locale}/profileicon.json` and `/cdn/{version}/img/profileicon/{iconId}.png`.
- Languages list: `/cdn/languages.json`.

Latest Data Dragon version observed on 2026-05-06: `16.9.1`.

For queues, maps, game modes, game types, seasons, and ranked emblems, use the static developer files listed in "Official Sources". These are especially important for `queueId` display and filtering.

## Recommended Next Uses For This Project

P1:

- Keep Account-V1 + Match-V5 + League-V4 as the stable core.
- Keep PUUID as the unique player key in DB and UI flows.
- Add a small periodic task or manual admin action to refresh Data Dragon version and champion/queue metadata if UI assets become important.

P2:

- Add `lol-status-v4` to dashboard diagnostics so Riot outages are visible before blaming API keys or app bugs.
- Consider `spectator-v5` for a lightweight "in game now" state.
- Consider `champion-mastery-v4` for player profile cards.
- Consider `lol-challenges-v1` only after the main match/rank UX is mature.

P3:

- Keep `clash-v1`, `champion-v3`, and tournament APIs documented but out of the implementation path unless the product direction changes.
- Use RSO APIs only if the application becomes multi-user and needs authenticated player consent.

## Refresh Checklist

When this document needs updating:

1. Open <https://developer.riotgames.com/apis> and review all League of Legends API families.
2. For each family, inspect `https://developer.riotgames.com/api-details/{api-name}`.
3. Re-check routing values in the League of Legends support/reference article.
4. Re-check rate-limit and error handling guidance in the Developer Portal guide.
5. Fetch `https://ddragon.leagueoflegends.com/api/versions.json` and update the latest observed Data Dragon version.
6. Revisit this project's Riot adapters and decide whether new endpoints belong behind existing ports or new explicit ports.

