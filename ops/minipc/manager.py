#!/usr/bin/env python3
"""Local deployment manager. Requires Python 3.10+, Docker and Compose.

Only explicitly configured files are read. Tokens and process stderr are never
printed. Runtime files belong outside the repository, with owner-only access.
"""
import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import getpass
import hashlib
import io
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
import zipfile

HERE = Path(__file__).resolve().parent
REPO = "ivan280809/lol-match-tracker"
IMAGE = "ghcr.io/ivan280809/lol-match-tracker"
SHA = re.compile(r"[0-9a-f]{40}")
DIGEST = re.compile(re.escape(IMAGE) + r"@sha256:[0-9a-f]{64}")


def write_json(path, value):
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    os.chmod(tmp, 0o600)
    os.replace(tmp, path)


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else None


@contextmanager
def lock(root):
    handle = (root / "manager.lock").open("a+b")
    try:
        if os.name == "nt":
            import msvcrt
            handle.seek(0)
            if not handle.read(1):
                handle.write(b"0")
                handle.flush()
            handle.seek(0)
            msvcrt.locking(handle.fileno(), msvcrt.LK_NBLCK, 1)
        else:
            import fcntl
            fcntl.flock(handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except (OSError, BlockingIOError):
        handle.close()
        raise RuntimeError("Another manager operation is running") from None
    try:
        yield
    finally:
        handle.close()


def validate_release(manifest, run, head):
    if (run.get("status") != "completed" or run.get("conclusion") != "success"
            or run.get("event") not in ("push", "workflow_dispatch")
            or run.get("head_branch") != "master"
            or run.get("path") != ".github/workflows/publish-ghcr.yml"
            or run.get("head_repository", {}).get("full_name") != REPO
            or run.get("head_sha") != head):
        raise ValueError("Release is not a successful workflow for current master")
    if (manifest.get("version") != 1 or manifest.get("repository") != REPO
            or manifest.get("branch") != "master" or manifest.get("sha") != head
            or not SHA.fullmatch(head) or not DIGEST.fullmatch(manifest.get("image", ""))
            or manifest.get("run_id") != run.get("id")
            or manifest.get("run_attempt") != run.get("run_attempt")
            or manifest.get("platform") != "linux/amd64"
            or not isinstance(manifest.get("tests"), int) or manifest["tests"] < 1
            or not re.fullmatch(r"[0-9a-f]{64}", manifest.get("schema_sha256", ""))):
        raise ValueError("Release identity or digest is invalid")


class Manager:
    def __init__(self, config):
        self.config = json.loads(Path(config).read_text(encoding="utf-8-sig"))
        self.root = Path(self.config["data_root"]).resolve()
        if not Path(self.config["data_root"]).is_absolute():
            raise ValueError("data_root must be absolute")
        if self.config.get("repository") != REPO or self.config.get("branch") != "master":
            raise ValueError("Only this repository and master are supported")
        if not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,40}", self.config["project"]):
            raise ValueError("Invalid Compose project")
        for field in ("public_port", "admin_port"):
            if not isinstance(self.config[field], int) or not 1024 <= self.config[field] <= 65535:
                raise ValueError("Invalid port")
        if self.config["public_port"] == self.config["admin_port"]:
            raise ValueError("Ports must differ")

    def run(self, args, **kwargs):
        result = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                timeout=kwargs.pop("timeout", 600), **kwargs)
        if result.returncode:
            # Do not leak secrets or payloads through tool error messages.
            raise RuntimeError("Local command failed: " + args[0] + " (exit " + str(result.returncode) + ")")
        return result.stdout

    def compose(self, *args, image=None, **kwargs):
        current = load_json(self.root / "current.json")
        image = image or (current or {}).get("image")
        if not image or not DIGEST.fullmatch(image):
            raise ValueError("A validated digest is required")
        env = dict(os.environ, APP_IMAGE=image, DATA_ROOT=self.root.as_posix(),
                   PUBLIC_PORT=str(self.config["public_port"]), ADMIN_PORT=str(self.config["admin_port"]))
        return self.run(["docker", "compose", "--project-name", self.config["project"],
                         "--file", str(HERE / "compose.yaml"), *args], env=env, **kwargs)

    def initialize(self):
        self.root.mkdir(parents=True, exist_ok=True)
        if os.name == "nt":
            user = self.run(["whoami"]).decode().strip()
            self.run(["icacls", str(self.root), "/inheritance:r", "/grant:r",
                      user + ":(OI)(CI)F", "*S-1-5-18:(OI)(CI)F"])
        else:
            os.chmod(self.root, 0o700)
        for folder in ("secrets/app", "secrets/updater", "backups", "history"):
            (self.root / folder).mkdir(parents=True, exist_ok=True, mode=0o700)
        for name in ("spring.datasource.password", "app.config.encryption-key", "app.dashboard.guard.password"):
            path = self.root / "secrets/app" / name
            if not path.exists():
                path.write_text(secrets.token_urlsafe(48), encoding="utf-8")
                os.chmod(path, 0o600)
        print("Initialized owner-protected secret files; existing values preserved.")

    def set_secret(self, name):
        mapping = {
            "RIOT_API_KEY": "app/riot.api.key",
            "TELEGRAM_BOT_TOKEN": "app/telegram.bot.token",
            "TELEGRAM_CHAT_ID": "app/telegram.chat.id",
            "ADMIN_PASSWORD": "app/app.dashboard.guard.password",
            "GITHUB_READ_TOKEN": "updater/github.token",
        }
        if name not in mapping:
            raise ValueError("Unsupported secret name")
        if not sys.stdin.isatty():
            raise RuntimeError("Secret entry requires an interactive terminal; do not pipe secrets")
        value = getpass.getpass(name + " (hidden): ")
        if not value.strip():
            raise ValueError("Empty secret rejected")
        path = self.root / "secrets" / mapping[name]
        tmp = path.with_suffix(path.suffix + ".tmp")
        tmp.write_text(value.strip(), encoding="utf-8")
        os.chmod(tmp, 0o600)
        os.replace(tmp, path)
        print("Secret saved. Recreate app after changing application secrets.")

    def api(self, path, raw=False):
        if not path.startswith("/"):
            raise ValueError("Expected relative GitHub API path")
        token_file = self.root / "secrets/updater/github.token"
        headers = {"Accept": "application/vnd.github+json", "X-GitHub-Api-Version": "2022-11-28",
                   "User-Agent": "lol-tracker-local-updater"}
        if token_file.exists():
            headers["Authorization"] = "Bearer " + token_file.read_text().strip()
        request = urllib.request.Request("https://api.github.com" + path, headers=headers)
        # Do not forward the GitHub token to the signed artifact-storage redirect.
        class Redirect(urllib.request.HTTPRedirectHandler):
            def redirect_request(self, req, fp, code, msg, hdrs, url):
                redirected = super().redirect_request(req, fp, code, msg, hdrs, url)
                if redirected:
                    redirected.remove_header("Authorization")
                return redirected
        try:
            with urllib.request.build_opener(Redirect()).open(request, timeout=45) as response:
                data = response.read(2_000_001)
            if len(data) > 2_000_000:
                raise ValueError("GitHub response exceeded limit")
            return data if raw else json.loads(data)
        except urllib.error.HTTPError as error:
            raise RuntimeError("GitHub request failed (HTTP " + str(error.code) + "); check read-only token and artifact retention") from None

    def candidate(self):
        base = "/repos/" + REPO
        head = self.api(base + "/commits/master")["sha"]
        runs = self.api(base + "/actions/workflows/publish-ghcr.yml/runs?branch=master&status=success&per_page=30")["workflow_runs"]
        runs = [run for run in runs if run["head_sha"] == head]
        if not runs:
            print("Current master has no successful release workflow; preserving current delivery.")
            return None
        run = self.api(base + "/actions/runs/" + str(runs[0]["id"]))
        artifacts = self.api(base + "/actions/runs/" + str(run["id"]) + "/artifacts")["artifacts"]
        name = "deployment-" + head + "-" + str(run["run_attempt"])
        found = [a for a in artifacts if a["name"] == name and not a["expired"]]
        if len(found) != 1:
            raise RuntimeError("Exactly one unexpired deployment manifest is required")
        archive = self.api(base + "/actions/artifacts/" + str(found[0]["id"]) + "/zip", raw=True)
        expected_digest = found[0].get("digest")
        if expected_digest != "sha256:" + hashlib.sha256(archive).hexdigest():
            raise ValueError("Artifact checksum does not match GitHub metadata")
        with zipfile.ZipFile(io.BytesIO(archive)) as zipped:
            if zipped.namelist() != ["release.json"] or zipped.getinfo("release.json").file_size > 16384:
                raise ValueError("Unexpected deployment artifact contents")
            manifest = json.loads(zipped.read("release.json"))
        validate_release(manifest, run, head)
        return manifest

    def db(self, sql, image=None):
        return self.compose("exec", "-T", "postgres", "psql", "-U", "loltracker", "-d", "loltracker",
                            "-At", "-v", "ON_ERROR_STOP=1", "-c", sql, image=image).decode().strip()

    def schema_state(self, image=None):
        if self.db("SELECT to_regclass('public.flyway_schema_history') IS NOT NULL", image) == "f":
            return "empty"
        rows = self.db("SELECT version, checksum, success FROM flyway_schema_history ORDER BY installed_rank", image)
        return hashlib.sha256(rows.encode()).hexdigest()

    def backup(self, image=None):
        required = max(1024 ** 3, int(self.db("SELECT pg_database_size('loltracker')", image)) * 3)
        if shutil.disk_usage(self.root).free < required:
            raise RuntimeError("Insufficient free disk space for backup and restore verification")
        stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        target = self.root / "backups" / (stamp + ".dump")
        tmp = target.with_suffix(".partial")
        command = self.compose_command(image)
        with tmp.open("wb") as output:
            result = subprocess.run(command + ["exec", "-T", "postgres", "pg_dump", "-U", "loltracker",
                                    "-d", "loltracker", "-Fc", "--no-owner", "--no-acl"],
                                    env=self.compose_env(image), stdout=output, stderr=subprocess.PIPE, timeout=600)
        if result.returncode:
            raise RuntimeError("Backup failed; partial file retained")
        os.chmod(tmp, 0o600)
        # Restore into a new throwaway database, never into the live database.
        verify_db = "restorecheck_" + secrets.token_hex(6)
        self.db('CREATE DATABASE "' + verify_db + '"', image)
        try:
            with tmp.open("rb") as source:
                self.compose("exec", "-T", "postgres", "pg_restore", "-U", "loltracker", "-d", verify_db,
                             "--exit-on-error", "--no-owner", "--no-acl", image=image, stdin=source)
        finally:
            self.db('DROP DATABASE "' + verify_db + '"', image)
        os.replace(tmp, target)
        write_json(target.with_suffix(".json"), {
            "sha256": file_hash(target), "restored_successfully": True,
            "current": load_json(self.root / "current.json"), "schema_state": self.schema_state(image),
        })
        print("Backup restored successfully in isolated database:", target.name)
        return target.name

    def compose_env(self, image=None):
        current = load_json(self.root / "current.json") or {}
        value = image or current.get("image", "")
        if not DIGEST.fullmatch(value):
            raise ValueError("A validated digest is required")
        return dict(os.environ, APP_IMAGE=value, DATA_ROOT=self.root.as_posix(),
                    PUBLIC_PORT=str(self.config["public_port"]), ADMIN_PORT=str(self.config["admin_port"]))

    def compose_command(self, image=None):
        self.compose_env(image)
        return ["docker", "compose", "--project-name", self.config["project"], "--file", str(HERE / "compose.yaml")]

    def healthy(self, image):
        self.compose("up", "-d", "--wait", "--wait-timeout", "180", "app", "public", image=image, timeout=240)
        with urllib.request.urlopen("http://127.0.0.1:" + str(self.config["admin_port"]) + "/actuator/health", timeout=10) as r:
            if json.load(r).get("status") != "UP":
                raise RuntimeError("Application health is not UP")

    def deploy(self, release, allow_schema=None):
        current = load_json(self.root / "current.json")
        if (self.root / "pending.json").exists():
            raise RuntimeError("Unfinished deployment exists; inspect pending.json and recover manually")
        if current and current.get("image") == release["image"]:
            print("Already deployed:", release["sha"])
            return
        if (self.root / "history" / (str(release["run_id"]) + "-failed.json")).exists():
            raise RuntimeError("This release already failed health; operator review required before retry")
        changes_schema = bool(current and current["schema_sha256"] != release["schema_sha256"])
        if changes_schema and allow_schema != release["sha"]:
            raise RuntimeError("Migration fingerprint changed; review compatibility and use --allow-schema-change with exact SHA")
        arch = self.run(["docker", "info", "--format", "{{.OSType}}/{{.Architecture}}"]).decode().strip()
        if arch not in ("linux/x86_64", "linux/amd64"):
            raise RuntimeError("This release supports linux/amd64 only; build for the actual target first")
        self.run(["docker", "pull", release["image"]])
        labels = json.loads(self.run(["docker", "image", "inspect", release["image"], "--format", "{{json .Config.Labels}}"]))
        if (labels.get("org.opencontainers.image.revision") != release["sha"]
                or labels.get("dev.iroberto.workflow-run") != str(release["run_id"])):
            raise ValueError("Image labels do not match the successful workflow")
        if not current:
            # Do not adopt a possibly unrelated existing Compose project or database.
            existing = self.run(["docker", "volume", "ls", "--filter", "label=com.docker.compose.project=" + self.config["project"], "-q"])
            containers = self.run(["docker", "ps", "-aq", "--filter", "label=com.docker.compose.project=" + self.config["project"]])
            if existing.strip() or containers.strip():
                raise RuntimeError("Existing project without deployment state; refusing automatic adoption")
        # Recheck branch immediately before changing anything on the host.
        if self.api("/repos/" + REPO + "/commits/master")["sha"] != release["sha"]:
            raise RuntimeError("Master changed during verification; retry on next timer")
        write_json(self.root / "pending.json", {"candidate": release, "previous": current, "stage": "starting"})
        self.compose("up", "-d", "--wait", "--wait-timeout", "120", "postgres", image=release["image"])
        if current:
            self.compose("stop", "app", image=current["image"])
        before = self.schema_state(release["image"])
        dump = self.backup(release["image"])
        write_json(self.root / "pending.json", {"candidate": release, "previous": current, "schema_before": before, "backup": dump})
        try:
            self.healthy(release["image"])
        except Exception:
            self.compose("stop", "app", image=release["image"])
            after = self.schema_state(release["image"])
            if current and not changes_schema and after == before:
                self.healthy(current["image"])
                write_json(self.root / "history" / (str(release["run_id"]) + "-failed.json"),
                           {"failed": release, "rolled_back_to": current, "backup": dump})
                (self.root / "pending.json").unlink()
                raise RuntimeError("Candidate failed health; previous image restored and healthy") from None
            raise RuntimeError("Candidate failed health; application stopped, database preserved; manual recovery required") from None
        release = dict(release, deployed_at=datetime.now(timezone.utc).isoformat(), schema_state=self.schema_state(release["image"]))
        if current:
            write_json(self.root / "previous.json", current)
        write_json(self.root / "history" / (str(release["run_id"]) + ".json"), release)
        write_json(self.root / "current.json", release)
        (self.root / "pending.json").unlink()
        print("Healthy delivery:", release["sha"], release["image"])

    def rollback(self):
        current = load_json(self.root / "current.json")
        previous = load_json(self.root / "previous.json")
        if (self.root / "pending.json").exists() or not current or not previous:
            raise RuntimeError("Rollback requires clean current and previous delivery state")
        if current["schema_sha256"] != previous["schema_sha256"] or self.schema_state() != previous["schema_state"]:
            raise RuntimeError("Schema compatibility not demonstrated; restore into separate database first")
        self.compose("stop", "app")
        self.backup()
        write_json(self.root / "pending.json", {"candidate": previous, "previous": current, "stage": "rollback"})
        self.healthy(previous["image"])
        write_json(self.root / "current.json", previous)
        write_json(self.root / "previous.json", current)
        (self.root / "pending.json").unlink()
        print("Rollback healthy:", previous["sha"])


def file_hash(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default=str(HERE / "config.json"))
    parser.add_argument("command", choices=("init", "set-secret", "check", "update", "backup", "rollback", "status", "stop", "start"))
    parser.add_argument("name", nargs="?")
    parser.add_argument("--allow-schema-change")
    args = parser.parse_args()
    manager = Manager(args.config)
    if args.command == "init":
        manager.initialize()
        return
    if not manager.root.exists():
        raise ValueError("Run init first")
    with lock(manager.root):
        if args.command == "set-secret":
            manager.set_secret(args.name)
        elif args.command in ("check", "update"):
            release = manager.candidate()
            if release:
                if args.command == "check":
                    print(json.dumps(release, indent=2))
                else:
                    manager.deploy(release, args.allow_schema_change)
        elif args.command == "backup":
            manager.backup()
        elif args.command == "rollback":
            manager.rollback()
        elif args.command == "status":
            print(json.dumps(load_json(manager.root / "current.json"), indent=2))
            if (manager.root / "pending.json").exists():
                print("ATTENTION: unfinished deployment")
            print(manager.compose("ps").decode())
        elif args.command == "stop":
            manager.compose("stop")
        elif args.command == "start":
            if (manager.root / "pending.json").exists():
                raise RuntimeError("Unfinished deployment: recover before starting")
            manager.healthy(load_json(manager.root / "current.json")["image"])


if __name__ == "__main__":
    try:
        main()
    except (Exception, KeyboardInterrupt) as error:
        # Only our deliberately sanitized messages are suitable for journal output.
        if isinstance(error, RuntimeError):
            print("Stopped:", str(error), file=sys.stderr)
        else:
            print("Stopped:", type(error).__name__, "(details suppressed to protect credentials)", file=sys.stderr)
        sys.exit(1)
