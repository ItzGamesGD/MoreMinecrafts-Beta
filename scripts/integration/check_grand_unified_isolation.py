#!/usr/bin/env python3
"""Validate the Grand Unified working branch invariants (read-only).

1. Only inactive/, docs/ and scripts/integration/ differ from the frozen baseline.
2. Every inactive/ file is listed in inactive/MANIFEST.tsv with a matching git blob hash.
3. With --verify-source, every manifest row is byte-identical to the blob at its source SHA.
4. Build files and workflows never reference inactive/.
5. Every registry row marked SOURCE CONSOLIDATED points at an existing inactive/ family.
"""
import csv, hashlib, os, subprocess, sys
BASE = "49747981ecc49f7f6b0a39fe62b23bd1a8d42d02"
ROOT = subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
def git(*a): return subprocess.run(["git", "-C", ROOT, *a], capture_output=True, text=True)
errors = []
def bad(m): errors.append(m)
changed = git("diff", "--name-only", BASE).stdout.split()
untracked = git("ls-files", "--others", "--exclude-standard").stdout.split()
for p in changed + untracked:
    if not p.startswith(("inactive/", "docs/", "scripts/integration/")):
        bad(f"path outside the allowed set differs from baseline: {p}")
rows = list(csv.reader(open(f"{ROOT}/inactive/MANIFEST.tsv"), delimiter="\t"))[1:]
listed = set()
for relu, blob12, size, branch, sha8, srcpath, note in rows:
    rel = "inactive/" + relu
    listed.add(rel)
    fp = f"{ROOT}/{rel}"
    if not os.path.isfile(fp): bad(f"missing: {rel}"); continue
    data = open(fp, "rb").read()
    h = hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()
    if not h.startswith(blob12) or len(data) != int(size): bad(f"hash/size mismatch: {rel}")
    if "--verify-source" in sys.argv and note != "TRANSFORMED":
        r = git("rev-parse", "-q", "--verify", f"{sha8}:{srcpath}")
        if r.stdout.strip() != h: bad(f"differs from source {sha8}: {rel}")
on_disk = set()
for d, _, fs in os.walk(f"{ROOT}/inactive"):
    for f in fs:
        rel = os.path.relpath(os.path.join(d, f), ROOT)
        if os.path.basename(rel) in ("README.md", "MANIFEST.tsv", "RECOVERY_MANIFEST.tsv", "RECIPES.json", "TRANSFORMS.json", "OMITTED.json", "RECOVERY_RESULTS.tsv") and rel.count("/") == 1: continue
        if os.path.basename(rel) in ("SOURCE.md", "shared-file-hunks.patch") and rel.count("/") == 2: continue
        on_disk.add(rel)
for rel in sorted(on_disk - listed): bad(f"unlisted file under inactive/: {rel}")
for rel in ("build.gradle", "settings.gradle", "gradle.properties"):
    if "inactive" in open(f"{ROOT}/{rel}").read(): bad(f"{rel} references inactive")
wf = f"{ROOT}/.github/workflows"
for f in os.listdir(wf):
    if "inactive/" in open(os.path.join(wf, f)).read(): bad(f"workflow references inactive/: {f}")
reg = list(csv.reader(open(f"{ROOT}/docs/integration/FEATURE_STATUS_REGISTRY_20261112.tsv"), delimiter="\t"))[1:]
for r in reg:
    if r[2].endswith("SOURCE CONSOLIDATED"):
        fam = r[4].split("/")[1] if r[4].startswith("inactive/") else None
        if not fam or not os.path.isdir(f"{ROOT}/inactive/{fam}/files"): bad(f"registry row {r[0]} target missing")
# recovery manifest: every A/M delta blob must be available (GU tree / inactive / regen / transform / documented omission)
avail = set()
for line in git("ls-tree", "-r", BASE).stdout.splitlines():
    avail.add(line.split("\t")[0].split()[2])
for dp, _, fs in os.walk(f"{ROOT}/inactive"):
    for f in fs:
        d = open(os.path.join(dp, f), "rb").read(); avail.add(hashlib.sha1(b"blob %d\0" % len(d) + d).hexdigest())
rec = list(csv.reader(open(f"{ROOT}/inactive/RECOVERY_MANIFEST.tsv"), delimiter="\t"))[1:]
unavailable = 0
for fam, label, head, tree, mb, st, mode, blob, path, store in rec:
    if st == "D" or store.startswith(("REGEN:", "OMITTED", "TRANSFORM")): continue
    if blob not in avail: bad(f"recovery blob unavailable: {fam}/{label}:{path}"); unavailable += 1
print(f"recovery rows {len(rec)}, unavailable {unavailable}")
print(f"manifest rows {len(rows)}, inactive files on disk {len(on_disk)}, registry rows {len(reg)}")
if errors:
    print("FAIL"); [print(" ", e) for e in errors]; sys.exit(1)
print("OK")
