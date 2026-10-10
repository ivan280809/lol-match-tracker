#!/usr/bin/env python3
"""Run only locally. Never print tokens, token-bearing URLs, or API bodies."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import urllib.error
import urllib.request

from manager import Manager, lock, write_json


def request(url, data=None, headers=None):
    payload = json.dumps(data).encode() if data is not None else None
    hdr = dict(headers or {})
    if payload is not None:
        hdr["Content-Type"] = "application/json"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=payload, headers=hdr), timeout=20) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        # Never print the exception (Telegram puts the token in the URL).
        after = error.headers.get("Retry-After", "not provided")
        if error.code == 429:
            raise RuntimeError("Rate limited; no automatic retry. Retry-After: " + after) from None
        raise RuntimeError("Integration request failed: HTTP " + str(error.code)) from None
    except Exception:
        raise RuntimeError("Integration request uncertain or unreachable; inspect locally before retry") from None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default=str(Path(__file__).parent / "config.json"))
    parser.add_argument("command", choices=("telegram-discover", "telegram-test", "riot-check"))
    args = parser.parse_args()
    manager = Manager(args.config)
    with lock(manager.root):
        secret_dir = manager.root / "secrets/app"
        if args.command == "riot-check":
            token = (secret_dir / "riot.api.key").read_text().strip()
            request("https://euw1.api.riotgames.com/lol/status/v4/platform-data", headers={"X-Riot-Token": token})
            print("Riot key accepted for EUW1. This does not establish Production authorization.")
            return
        token = (secret_dir / "telegram.bot.token").read_text().strip()
        base = "https://api.telegram.org/bot" + token + "/"
        webhook = request(base + "getWebhookInfo")
        if not webhook.get("ok"):
            raise RuntimeError("Telegram webhook check failed")
        if args.command == "telegram-discover":
            if webhook.get("result", {}).get("url"):
                raise RuntimeError("Webhook exists; preserved. Obtain chat ID through the existing webhook receiver")
            result = request(base + "getUpdates", {"timeout": 0, "limit": 20})
            if not result.get("ok"):
                raise RuntimeError("Telegram update check failed")
            chats = set()
            for update in result.get("result", []):
                for field in ("message", "channel_post", "my_chat_member"):
                    chat = update.get(field, {}).get("chat", {})
                    if "id" in chat:
                        chats.add((chat["id"], chat.get("type", "unknown")))
            for chat_id, kind in sorted(chats):
                print("Chat ID:", chat_id, "type:", kind)
            if not chats:
                print("No chat found. Send /start directly to the bot, then retry discovery.")
            return
        chat_id = (secret_dir / "telegram.chat.id").read_text().strip()
        identity = hashlib.sha256((token + "\0" + chat_id).encode()).hexdigest()
        marker = manager.root / ("telegram-test-" + identity + ".json")
        if marker.exists():
            raise RuntimeError("A test was already attempted for this token/chat; refusing duplicate")
        write_json(marker, {"status": "attempted"})
        result = request(base + "sendMessage", {
            "chat_id": chat_id,
            "text": "LoL Match Tracker: prueba de conexión completada. Administración privada; modo demostración activo.",
            "disable_notification": True,
        })
        if not result.get("ok"):
            raise RuntimeError("Telegram test not confirmed; marker retained to avoid duplicate")
        write_json(marker, {"status": "sent", "message_id": result["result"]["message_id"]})
        print("Exactly one test message confirmed.")


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
    except Exception as error:
        print(type(error).__name__ + ": configuration incomplete (details suppressed)", file=sys.stderr)
        sys.exit(1)
