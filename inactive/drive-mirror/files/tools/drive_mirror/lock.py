"""Drive-side writer lease (second line of defence behind Actions `concurrency`).

Drive offers no compare-and-swap, so acquisition is write-then-read-back: a
writer that does not read its own token back after a settle delay backs off.
The GitHub Actions concurrency group is the primary single-writer guarantee;
this lease protects against manual runs from other machines.
"""
from __future__ import annotations

import json
import os
import time

from .layout import LOCK_FILE, P_ROLE, P_MIRROR


class LockHeld(RuntimeError):
    pass


class DriveLease:
    def __init__(self, drive, meta_folder_id: str, mirror_token: str, owner: str, ttl_s: int = 1200,
                 settle_s: float = 2.0, existing_id: str | None = None):
        self.drive = drive
        self.meta_folder_id = meta_folder_id
        self.mirror_token = mirror_token
        self.owner = owner
        self.ttl_s = ttl_s
        self.settle_s = settle_s
        self.file_id = existing_id
        self.token = os.urandom(8).hex()
        self._renewed_at = 0.0

    def _read(self) -> dict | None:
        if not self.file_id:
            return None
        try:
            return json.loads(self.drive.download(self.file_id) or b"null")
        except Exception:
            return None

    def _write(self, record: dict) -> None:
        data = json.dumps(record, indent=1).encode()
        props = {P_ROLE: "metafile", P_MIRROR: self.mirror_token}
        if self.file_id:
            self.drive.update_content(self.file_id, data, "application/json", props)
        else:
            self.file_id = self.drive.create_file(LOCK_FILE, self.meta_folder_id, data, "application/json",
                                                  props)["id"]

    def acquire(self, break_lock: bool = False) -> None:
        cur = self._read()
        now = time.time()
        if cur and cur.get("released") is False and cur.get("expires_at", 0) > now and not break_lock:
            raise LockHeld(f"mirror lease held by {cur.get('owner')} until "
                           f"{time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(cur['expires_at']))}")
        self._write(self._record(now))
        time.sleep(self.settle_s)
        back = self._read()
        if not back or back.get("token") != self.token:
            raise LockHeld(f"lost lease race to {back.get('owner') if back else 'unknown'}")
        self._renewed_at = now

    def renew(self, force: bool = False) -> None:
        now = time.time()
        if force or now - self._renewed_at > self.ttl_s / 3:
            cur = self._read()
            if cur and cur.get("token") != self.token:
                raise LockHeld(f"lease taken over by {cur.get('owner')}")
            self._write(self._record(now))
            self._renewed_at = now

    def release(self) -> None:
        cur = self._read()
        if cur and cur.get("token") == self.token:
            rec = self._record(time.time())
            rec["released"] = True
            self._write(rec)

    def _record(self, now: float) -> dict:
        return {"owner": self.owner, "token": self.token, "acquired_at": now,
                "expires_at": now + self.ttl_s, "released": False}
