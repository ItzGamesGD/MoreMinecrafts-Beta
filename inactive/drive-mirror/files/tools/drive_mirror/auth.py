"""Access-token providers for the Drive API.

Modes (env DRIVE_AUTH_MODE):
  adc     Application Default Credentials via google-auth (Workload Identity
          Federation credential file written by google-github-actions/auth).
          No long-lived key exists anywhere. Scope: drive.
  oauth   OAuth client + refresh token of a Google account (needed when the
          destination is a personal My Drive, which service accounts cannot
          own files in). Scope: drive.file — the app sees only files it created.
  static  A pre-minted access token in DRIVE_ACCESS_TOKEN (tests, manual runs).

Token values are never logged or written anywhere.
"""
from __future__ import annotations

import json
import os
import threading
import time
import urllib.error
import urllib.parse
import urllib.request

SCOPE_FULL = "https://www.googleapis.com/auth/drive"
SCOPE_FILE = "https://www.googleapis.com/auth/drive.file"


class AuthUnavailable(RuntimeError):
    """No usable credentials are configured (reported as BLOCKED, never as PASS)."""


class StaticToken:
    def __init__(self, token: str):
        self._token = token

    def token(self) -> str:
        return self._token

    def invalidate(self) -> None:
        pass


class OAuthRefreshToken:
    def __init__(self, client_id: str, client_secret: str, refresh_token: str,
                 token_uri: str = "https://oauth2.googleapis.com/token"):
        self._cfg = (client_id, client_secret, refresh_token, token_uri)
        self._token = None
        self._expiry = 0.0
        self._lock = threading.Lock()

    def token(self) -> str:
        with self._lock:
            if self._token and time.time() < self._expiry - 120:
                return self._token
            cid, secret, refresh, uri = self._cfg
            body = urllib.parse.urlencode({"client_id": cid, "client_secret": secret,
                                           "refresh_token": refresh, "grant_type": "refresh_token"}).encode()
            req = urllib.request.Request(uri, data=body, method="POST",
                                         headers={"Content-Type": "application/x-www-form-urlencoded"})
            try:
                with urllib.request.urlopen(req, timeout=60) as r:
                    data = json.load(r)
            except urllib.error.HTTPError as e:
                # Body may echo request fields; do not include it.
                raise AuthUnavailable(f"OAuth refresh failed with HTTP {e.code}") from None
            self._token = data["access_token"]
            self._expiry = time.time() + int(data.get("expires_in", 3600))
            return self._token

    def invalidate(self) -> None:
        with self._lock:
            self._token = None


class GoogleAuthADC:
    def __init__(self, scopes: list[str]):
        try:
            import google.auth  # type: ignore
            import google.auth.transport.requests  # type: ignore
        except ImportError as e:
            raise AuthUnavailable("google-auth is not installed (pip install google-auth requests)") from e
        try:
            self._creds, _ = google.auth.default(scopes=scopes)
        except Exception as e:
            raise AuthUnavailable(f"Application Default Credentials unavailable: {type(e).__name__}") from None
        self._request = google.auth.transport.requests.Request()
        self._lock = threading.Lock()

    def token(self) -> str:
        with self._lock:
            if not self._creds.valid or (self._creds.expiry and
                                         self._creds.expiry.timestamp() - time.time() < 120):
                self._creds.refresh(self._request)
            return self._creds.token

    def invalidate(self) -> None:
        with self._lock:
            self._creds.expiry = None
            self._creds.token = None


def from_env(env=os.environ):
    mode = env.get("DRIVE_AUTH_MODE", "").strip().lower()
    if not mode:
        if env.get("DRIVE_ACCESS_TOKEN"):
            mode = "static"
        elif env.get("DRIVE_OAUTH_REFRESH_TOKEN"):
            mode = "oauth"
        elif env.get("GOOGLE_APPLICATION_CREDENTIALS"):
            mode = "adc"
        else:
            raise AuthUnavailable("no Drive credentials configured (DRIVE_AUTH_MODE unset)")
    if mode == "static":
        tok = env.get("DRIVE_ACCESS_TOKEN")
        if not tok:
            raise AuthUnavailable("DRIVE_ACCESS_TOKEN is empty")
        return StaticToken(tok)
    if mode == "oauth":
        need = ["DRIVE_OAUTH_CLIENT_ID", "DRIVE_OAUTH_CLIENT_SECRET", "DRIVE_OAUTH_REFRESH_TOKEN"]
        missing = [k for k in need if not env.get(k)]
        if missing:
            raise AuthUnavailable(f"missing {', '.join(missing)}")
        return OAuthRefreshToken(*(env[k] for k in need))
    if mode == "adc":
        return GoogleAuthADC([env.get("DRIVE_SCOPE", SCOPE_FULL)])
    raise AuthUnavailable(f"unknown DRIVE_AUTH_MODE {mode!r}")
