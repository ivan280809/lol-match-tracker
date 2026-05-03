# Decisions

## Decision 1: Store Current Rank Snapshot On Player

The application will persist current ranked display fields and an internal score on `PlayerEntity`.

Reason: roster average can be calculated without calling Riot for every player on every notification.

Rejected alternative: calculate all rank data live per notification. This is slower and more rate-limit prone.

## Decision 2: Prefer SoloQ Over Flex

When Riot returns multiple ranked entries, SoloQ is preferred and Flex is used as fallback.

Reason: SoloQ is the clearest individual rank signal for the Telegram message.

## Decision 3: Generic Front Error Toast

Unexpected MVC exceptions will be logged with details and shown to the user as `Un error ha ocurrido`.

Reason: the user asked to avoid Whitelabel and keep exception details in logs, not in the UI.
