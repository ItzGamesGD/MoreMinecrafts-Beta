#!/usr/bin/env python3
"""Reconstruct a preserved development state using ONLY the Grand Unified tree and its history.

  recover_state.py --family sky-mob --label primary --out /path/to/new/worktree

Algorithm: take the tree of the recorded merge base (an ancestor of the baseline, so it is in Grand Unified history),
apply the recorded delta (inactive/RECOVERY_MANIFEST.tsv) with blobs found by content hash in the Grand Unified tree,
run documented regeneration recipes (inactive/RECIPES.json) and reversible transforms (inactive/TRANSFORMS.json),
write the resulting tree and compare it with the recorded original head tree. It never reads an original feature ref.
Exit 0: tree identical to the original. Exit 2: reconstructed, but tree differs (see report). Exit 1: cannot run.
"""
import argparse, csv, json, os, shutil, subprocess, sys, tempfile
def run(cmd, env=None, cwd=None, inp=None, binary=False):
    r = subprocess.run(cmd, capture_output=True, env=env, cwd=cwd, input=inp)
    if r.returncode: raise RuntimeError(f"{cmd[:3]} failed: {r.stderr.decode()[:300]}")
    return r.stdout if binary else r.stdout.decode()
ap = argparse.ArgumentParser(); ap.add_argument("--family", required=True); ap.add_argument("--label", default="primary")
ap.add_argument("--out", required=True); ap.add_argument("--repo", default="."); a = ap.parse_args()
repo = os.path.abspath(a.repo); G = ["git", "-C", repo]
rows = [r for r in csv.reader(open(f"{repo}/inactive/RECOVERY_MANIFEST.tsv"), delimiter="\t")][1:]
rows = [r for r in rows if r[0] == a.family and r[1] == a.label]
if not rows: print("no such state"); sys.exit(1)
fam, label, head, tree, mb = rows[0][:5]
recipes = {r["id"]: r for r in json.load(open(f"{repo}/inactive/RECIPES.json"))}
transforms = {(t["family"], t["label"], t["path"]): t for t in json.load(open(f"{repo}/inactive/TRANSFORMS.json"))}
run(G + ["cat-file", "-e", mb + "^{commit}"])
avail = {}
for line in run(G + ["ls-tree", "-r", "HEAD"]).splitlines():
    meta, p = line.split("\t", 1); avail.setdefault(meta.split()[2], p)
out = os.path.abspath(a.out); os.makedirs(out, exist_ok=True)
idx = tempfile.mktemp(prefix="recover-index-"); env = dict(os.environ, GIT_INDEX_FILE=idx)
run(G + ["read-tree", mb], env=env)
missing, regen, mism, applied = [], [], [], 0
def put(mode, blob, path): run(G + ["update-index", "--add", "--cacheinfo", f"{mode},{blob},{path}"], env=env)
for fam_, lab, h, t, m, st, mode, blob, path, store in rows:
    if st == "D": run(G + ["update-index", "--force-remove", "--", path], env=env); applied += 1; continue
    if store.startswith("REGEN:"): regen.append((mode, blob, path, store[6:])); continue
    if store.startswith("OMITTED"): missing.append((path, store)); continue
    if store == "TRANSFORM":
        tr = transforms[(fam, label, path)]
        data = open(f"{repo}/inactive/{tr['stored']}", "rb").read().decode().replace(tr["to"], tr["from"]).encode()
        sha = run(G + ["hash-object", "-w", "--stdin"], inp=data).strip()
        if sha != blob: mism.append((path, "transform-inverse-hash")); continue
        put(mode, sha, path); applied += 1; continue
    if blob not in avail: missing.append((path, "blob-not-in-grand-unified")); continue
    put(mode, blob, path); applied += 1
env_co = dict(env, GIT_WORK_TREE=out)
subprocess.run(G + ["checkout-index", "-a", "-f", f"--prefix={out}/"], env=env, check=True)
for rid in sorted({r[3] for r in regen}):
    rc = recipes[rid]; tmp = tempfile.mkdtemp(prefix="recipe-")
    cmd = [c.replace("{tmp}", tmp) for c in rc["cmd"]]
    subprocess.run(cmd, cwd=os.path.join(out, rc["cwd"]), check=True, capture_output=True)
    for mode, blob, path, r_ in regen:
        if r_ != rid: continue
        src = list(v for k, v in rc["outputs"].items() if k == path)[0].replace("{tmp}", tmp)
        src = src if os.path.isabs(src) else os.path.join(out, src)
        sha = run(G + ["hash-object", "-w", src]).strip()
        if sha != blob: mism.append((path, "regenerated-hash-differs")); continue
        dst = os.path.join(out, path); os.makedirs(os.path.dirname(dst), exist_ok=True)
        if os.path.abspath(src) != os.path.abspath(dst): shutil.copy(src, dst)
        put(mode, sha, path); applied += 1
    shutil.rmtree(tmp, ignore_errors=True)
got = run(G + ["write-tree"], env=env).strip()
res = dict(family=fam, label=label, recorded_head=head, recorded_tree=tree, reconstructed_tree=got, tree_identical=(got == tree),
           merge_base=mb, entries=len(rows), applied=applied, missing=missing, mismatched=mism, regenerated=[r[2] for r in regen])
os.remove(idx) if os.path.exists(idx) else None
print(json.dumps(res, indent=1))
sys.exit(0 if res["tree_identical"] else 2)
