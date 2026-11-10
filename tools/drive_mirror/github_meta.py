"""Read-only GitHub REST metadata (PRs, workflows, CI runs).

Only GET requests are issued. The token, when present, needs no more than
`contents: read` / `pull-requests: read` / `actions: read`.
"""
from __future__ import annotations

import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request


class GitHubMeta:
    def __init__(self, repo: str, token: str | None = None, api: str = "https://api.github.com"):
        self.repo = repo
        self.token = token if token is not None else os.environ.get("GITHUB_TOKEN")
        self.api = api.rstrip("/")

    def get(self, path: str, params: dict | None = None):
        url = f"{self.api}{path}"
        if params:
            url += "?" + urllib.parse.urlencode(params)
        headers = {"Accept": "application/vnd.github+json", "User-Agent": "drive-mirror",
                   "X-GitHub-Api-Version": "2022-11-28"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        for attempt in range(5):
            try:
                with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as r:
                    return json.load(r)
            except urllib.error.HTTPError as e:
                if e.code in (403, 429, 500, 502, 503, 504) and attempt < 4:
                    time.sleep(2 ** attempt * 2)
                    continue
                raise

    def paged(self, path: str, key: str | None = None, params: dict | None = None, limit_pages: int = 50):
        out = []
        for page in range(1, limit_pages + 1):
            p = dict(params or {}, per_page=100, page=page)
            data = self.get(path, p)
            items = data[key] if key else data
            out.extend(items)
            if len(items) < 100:
                break
        return out

    def pulls(self) -> list[dict]:
        raw = self.paged(f"/repos/{self.repo}/pulls", params={"state": "all", "sort": "created", "direction": "asc"})
        return [{
            "number": p["number"], "title": p["title"], "state": p["state"], "draft": p.get("draft"),
            "merged": bool(p.get("merged_at")), "merged_at": p.get("merged_at"),
            "created_at": p["created_at"], "updated_at": p["updated_at"], "closed_at": p.get("closed_at"),
            "author": (p.get("user") or {}).get("login"),
            "head_ref": p["head"]["ref"], "head_sha": p["head"]["sha"],
            "head_repo": (p["head"].get("repo") or {}).get("full_name"),
            "base_ref": p["base"]["ref"], "base_sha": p["base"]["sha"],
            "merge_commit_sha": p.get("merge_commit_sha"), "html_url": p["html_url"],
        } for p in raw]

    def workflows(self) -> list[dict]:
        data = self.get(f"/repos/{self.repo}/actions/workflows", {"per_page": 100})
        return [{"id": w["id"], "name": w["name"], "path": w["path"], "state": w["state"]}
                for w in data["workflows"]]

    def runs_for_sha(self, sha: str) -> list[dict]:
        data = self.get(f"/repos/{self.repo}/actions/runs", {"head_sha": sha, "per_page": 100})
        return [{"id": r["id"], "workflow": r["name"], "event": r["event"], "status": r["status"],
                 "conclusion": r["conclusion"], "head_branch": r["head_branch"],
                 "created_at": r["created_at"], "html_url": r["html_url"]}
                for r in data["workflow_runs"]]
