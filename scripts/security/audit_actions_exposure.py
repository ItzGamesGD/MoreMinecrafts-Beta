#!/usr/bin/env python3
"""Read-only audit of accessible GitHub Actions artifacts and run logs.

Only finding categories and locations are printed; never matched secret values.
"""
import collections
import io
import json
import os
import re
import sys
import urllib.error
import urllib.request
import zipfile

API = "https://api.github.com"
REPO = os.environ.get("GITHUB_REPOSITORY", "")
TOKEN = os.environ.get("GH_TOKEN", "")
MAX_ARCHIVE = 75 * 1024 * 1024
MAX_ENTRY = 45 * 1024 * 1024
HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "Accept": "application/vnd.github+json",
    "X-GitHub-Api-Version": "2022-11-28",
    "User-Agent": "MoreMinecrafts-pre-public-audit",
}
PATTERNS = {
    "private-key": rb"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----",
    "github-token": rb"\b(?:ghp_|gho_|ghu_|ghs_|ghr_|github_pat_)[A-Za-z0-9_]{16,}\b",
    "aws-access-key": rb"\b(?:AKIA|ASIA)[0-9A-Z]{16}\b",
    "google-api-key": rb"\bAIza[0-9A-Za-z_-]{35}\b",
    "slack-token": rb"\bxox[baprs]-[A-Za-z0-9-]{16,}",
    "discord-webhook": rb"https://(?:discord|discordapp)\.com/api/webhooks/\d+/[A-Za-z0-9_-]{25,}",
    "stripe-live-key": rb"\bsk_live_[A-Za-z0-9]{16,}",
    "embedded-url-password": rb"https?://[^\s/:@]+:[^\s/@]+@",
    "jwt-like-token": rb"\beyJ[A-Za-z0-9_-]{20,}\.eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{6,}",
}
PATTERNS = {name: re.compile(pattern) for name, pattern in PATTERNS.items()}
SKIP = (".png", ".jpg", ".jpeg", ".webp", ".gif", ".ogg", ".mp3", ".mp4", ".wav", ".zip")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, url):
        return None


OPENER = urllib.request.build_opener(NoRedirect)


def api_json(path):
    request = urllib.request.Request(f"{API}/repos/{REPO}{path}", headers=HEADERS)
    with urllib.request.urlopen(request, timeout=35) as response:
        return json.load(response)


def records(path, key):
    output = []
    for page in range(1, 51):
        joiner = "&" if "?" in path else "?"
        chunk = api_json(f"{path}{joiner}per_page=100&page={page}").get(key, [])
        output.extend(chunk)
        if len(chunk) < 100:
            return output
    raise RuntimeError("pagination safety bound reached")


def archive_bytes(path):
    request = urllib.request.Request(f"{API}/repos/{REPO}{path}", headers=HEADERS)
    try:
        with OPENER.open(request, timeout=35) as response:
            data = response.read(MAX_ARCHIVE + 1)
    except urllib.error.HTTPError as error:
        if error.code not in (301, 302, 303, 307, 308):
            raise
        url = error.headers.get("Location", "")
        if not url.startswith("https://"):
            raise ValueError("unexpected signed artifact download URL")
        # The bearer token is NOT sent to the GitHub download redirect host.
        with urllib.request.urlopen(urllib.request.Request(
            url, headers={"User-Agent": HEADERS["User-Agent"]}), timeout=90) as response:
            data = response.read(MAX_ARCHIVE + 1)
    if len(data) > MAX_ARCHIVE:
        raise ValueError("archive exceeds size limit")
    return data


def inspect_zip(data, label, findings, skips, depth=0):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for member in archive.infolist():
            if member.is_dir():
                continue
            if member.file_size > MAX_ENTRY:
                skips.append((label, member.filename, "oversized"))
                continue
            path = f"{label}/{member.filename}"
            lower = member.filename.lower()
            if lower.endswith(SKIP):
                continue
            body = archive.read(member)
            if lower.endswith(".jar") and depth < 2:
                try:
                    inspect_zip(body, path, findings, skips, depth + 1)
                except zipfile.BadZipFile:
                    skips.append((label, member.filename, "invalid JAR"))
            else:
                for kind, pattern in PATTERNS.items():
                    if pattern.search(body):
                        findings.append((kind, path))


def main():
    if not REPO or not TOKEN:
        raise RuntimeError("missing repository or read-only token")
    artifacts = [a for a in records("/actions/artifacts", "artifacts") if not a.get("expired")]
    runs = records("/actions/runs", "workflow_runs")
    print(f"Inventory: {len(artifacts)} active artifacts, {len(runs)} workflow runs", flush=True)
    print(f"Artifact bytes: {sum(a.get('size_in_bytes') or 0 for a in artifacts)}", flush=True)
    findings, skips, unavailable = [], [], []
    finished = collections.Counter()
    for index, artifact in enumerate(artifacts, 1):
        try:
            data = archive_bytes(f"/actions/artifacts/{artifact['id']}/zip")
            inspect_zip(data, f"artifact:{artifact['id']}", findings, skips)
            finished["artifacts"] += 1
        except (urllib.error.URLError, TimeoutError, ValueError, zipfile.BadZipFile) as error:
            unavailable.append(("artifact", artifact["id"], type(error).__name__))
        if index % 50 == 0:
            print(f"Artifact progress: {index}/{len(artifacts)}", flush=True)
    for index, run in enumerate(runs, 1):
        try:
            data = archive_bytes(f"/actions/runs/{run['id']}/logs")
            inspect_zip(data, f"run:{run['id']}", findings, skips)
            finished["runs"] += 1
        except urllib.error.HTTPError as error:
            unavailable.append(("run", run["id"], f"HTTP {error.code}"))
        except (urllib.error.URLError, TimeoutError, ValueError, zipfile.BadZipFile) as error:
            unavailable.append(("run", run["id"], type(error).__name__))
        if index % 100 == 0:
            print(f"Run-log progress: {index}/{len(runs)}", flush=True)
    print("EXPOSURE AUDIT FINAL SUMMARY", flush=True)
    print(f"Scanned artifacts={finished['artifacts']}/{len(artifacts)}, run logs={finished['runs']}/{len(runs)}")
    print(f"Credential patterns={len(findings)}, unavailable={len(unavailable)}, skipped entries={len(skips)}")
    for kind, location in findings[:40]:
        print(f"Potential credential: type={kind} location={location}")
    for kind, ident, reason in unavailable[:40]:
        print(f"Unavailable: type={kind} id={ident} reason={reason}")
    for location, name, reason in skips[:20]:
        print(f"Skipped: location={location} entry={name} reason={reason}")
    print("No matched values or downloaded files were printed or uploaded.")
    return 1 if findings else (2 if unavailable or skips else 0)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print(f"Audit infrastructure failure: {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(3)
