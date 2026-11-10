"""Drive-side naming and metadata conventions (see README.md, "Drive layout")."""
from __future__ import annotations

import re
import string

FOLDER_MIME = "application/vnd.google-apps.folder"
GOOGLE_APPS_PREFIX = "application/vnd.google-apps."

META_FOLDER = "_mirror"
MANIFESTS_FOLDER = "manifests"
RETIRED_FOLDER = "_retired"
STATE_FILE = "state.json"
REFS_FILE = "refs.json"
PULLS_FILE = "pulls.json"
LOCK_FILE = "lock.json"

# Ref kind -> container folder under the mirror root.
CONTAINERS = {"heads": "branches", "tags": "tags", "pull": "pulls"}

# appProperties keys (each key+value must stay <= 124 bytes UTF-8).
P_ROLE = "mm_role"        # root | meta | container | ns | dir | file | metafile | retired
P_MIRROR = "mm_mirror"    # random token identifying one mirror root
P_NS = "mm_ns"            # random token identifying one namespace incarnation
P_REF = "mm_ref"          # full ref name (only when it fits)
P_BLOB = "mm_blob"        # git blob object id
P_SHA256 = "mm_sha256"    # SHA-256 of the bytes
P_MODE = "mm_mode"        # git file mode
P_SCHEMA = "mm_schema"

_SAFE = set(string.ascii_letters + string.digits + "._-")


def encode_component(name: str) -> str:
    """Reversible, filesystem-safe single folder name for a ref short name.

    Every byte outside [A-Za-z0-9._-] is percent-encoded, so `work/st01-p1`
    becomes `work%2Fst01-p1` and no two refs can collide.
    """
    out = []
    for ch in name:
        if ch in _SAFE:
            out.append(ch)
        else:
            out.extend(f"%{b:02X}" for b in ch.encode("utf-8"))
    return "".join(out)


def decode_component(enc: str) -> str:
    raw = bytearray()
    i = 0
    while i < len(enc):
        if enc[i] == "%" and re.fullmatch(r"[0-9A-F]{2}", enc[i + 1:i + 3] or ""):
            raw.append(int(enc[i + 1:i + 3], 16))
            i += 3
        else:
            raw.extend(enc[i].encode())
            i += 1
    return raw.decode("utf-8")


def ref_namespace(ref: str) -> tuple[str, str]:
    """refs/heads/a/b -> ("branches", "a%2Fb"); refs/pull/12/head -> ("pulls", "12")."""
    if ref.startswith("refs/heads/"):
        return CONTAINERS["heads"], encode_component(ref[len("refs/heads/"):])
    if ref.startswith("refs/tags/"):
        return CONTAINERS["tags"], encode_component(ref[len("refs/tags/"):])
    m = re.fullmatch(r"refs/pull/(\d+)/head", ref)
    if m:
        return CONTAINERS["pull"], m.group(1)
    raise ValueError(f"ref {ref!r} has no namespace")


def namespace_ref(container: str, folder_name: str) -> str:
    if container == CONTAINERS["heads"]:
        return "refs/heads/" + decode_component(folder_name)
    if container == CONTAINERS["tags"]:
        return "refs/tags/" + decode_component(folder_name)
    if container == CONTAINERS["pull"]:
        return f"refs/pull/{folder_name}/head"
    raise ValueError(container)


def manifest_name(ref: str) -> str:
    c, n = ref_namespace(ref)
    return f"{c}__{n}.json"


def fit_props(props: dict[str, str]) -> dict[str, str]:
    """Drop optional properties that exceed Drive's 124-byte key+value limit."""
    out = {}
    for k, v in props.items():
        if len(k.encode()) + len(v.encode()) <= 124:
            out[k] = v
        elif k in (P_ROLE, P_MIRROR, P_NS, P_BLOB, P_SHA256, P_MODE):
            raise ValueError(f"required property {k} does not fit")
    return out


def q_str(value: str) -> str:
    """Quote a value for a Drive `q` expression."""
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'"
