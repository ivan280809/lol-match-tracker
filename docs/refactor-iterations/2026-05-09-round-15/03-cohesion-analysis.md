# Cohesion Analysis

## Scope

Check whether the aesthetic card change belongs in the notification module.

## Findings

- Presentation logic for Telegram already lives in `NotificationMessageFactory`.
- Stats calculation and delivery are separate and should remain untouched.
- Adding a new renderer class would add ceremony without reducing complexity enough for this small change.

## Recommendation

Evolve `NotificationMessageFactory` with small helper methods:

- summary block,
- `pre` metrics table,
- details blockquote,
- reusable line helpers.

## Risks

- The factory can grow too much if future Telegram previews or image cards are added.

## Handoff

If future work adds real image cards, split a dedicated renderer at that point.
