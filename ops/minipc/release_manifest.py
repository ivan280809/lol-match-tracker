"""Trusted build metadata; no application credentials are needed at build time."""
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET


def check_tests(root=Path("target/surefire-reports")):
    reports = list(root.glob("TEST-*.xml"))
    if not reports:
        raise ValueError("No test reports found")
    total = 0
    for path in reports:
        suite = ET.parse(path).getroot()
        total += int(suite.get("tests", "0"))
        if any(int(suite.get(key, "0")) for key in ("failures", "errors", "skipped")):
            raise ValueError("Failed or skipped tests: " + path.name)
    if total == 0:
        raise ValueError("Zero tests executed")
    return total


def schema_hash(root=Path("src/main/resources/db/migration")):
    files = sorted(root.glob("*.sql"))
    if not files:
        raise ValueError("No migration files found")
    digest = hashlib.sha256()
    for path in files:
        digest.update(path.name.encode() + b"\0")
        digest.update(path.read_bytes().replace(b"\r\n", b"\n") + b"\0")
    return digest.hexdigest()


def create():
    sha = os.environ["GITHUB_SHA"]
    digest = os.environ["IMAGE_DIGEST"]
    if not re.fullmatch(r"[0-9a-f]{40}", sha) or not re.fullmatch(r"sha256:[0-9a-f]{64}", digest):
        raise ValueError("Invalid source or image digest")
    manifest = {
        "version": 1,
        "repository": "ivan280809/lol-match-tracker",
        "branch": "master",
        "sha": sha,
        "image": "ghcr.io/ivan280809/lol-match-tracker@" + digest,
        "run_id": int(os.environ["GITHUB_RUN_ID"]),
        "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "schema_sha256": schema_hash(),
        "tests": check_tests(),
        "platform": "linux/amd64",
    }
    Path("release.json").write_text(json.dumps(manifest, indent=2) + "\n")


if __name__ == "__main__":
    if sys.argv[1:] == ["check-tests"]:
        print("Tests passed without skips:", check_tests())
    elif sys.argv[1:] == ["create"]:
        create()
    else:
        raise SystemExit("Use check-tests or create")
