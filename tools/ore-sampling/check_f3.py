#!/usr/bin/env python3
"""Evaluate matched-sample results against the approved F-3 rule and print a summary table.

usage: check_f3.py <results-dir> [--prefix final_] [--md out.md] [--json out.json] [--strict-block-y]

F-3 (docs/preservation/PHASE2_AUTHORITY_RECONCILIATION_2026-10-26.md):
  binding   : fewer generated blocks than Ancient Debris in matched samples          -> exit status 1 if violated on any seed
  height    : Y 8-22, biased toward Y 15                                               -> reported (strict with --strict-block-y)
  vein size : 1-2 ore blocks                                                           -> reported
  tuning    : roughly 80% of Ancient Debris (a tuning target, not binding)             -> reported
"""
import argparse, collections, glob, json, os, sys

ap = argparse.ArgumentParser()
ap.add_argument("results"); ap.add_argument("--prefix", default="final_")
ap.add_argument("--md"); ap.add_argument("--json"); ap.add_argument("--strict-block-y", action="store_true")
a = ap.parse_args()

cells = collections.defaultdict(dict)
for f in sorted(glob.glob(os.path.join(a.results, a.prefix + "*__*.json"))):
    label, seed = os.path.basename(f)[len(a.prefix):-5].rsplit("__", 1)
    cells[label][int(seed)] = json.load(open(f))

rows, bad = [], False
for label, seeds in cells.items():
    deb = sum(d["ancient_debris"]["total_blocks"] for d in seeds.values())
    adm = sum(d["adamantium_debris"]["total_blocks"] for d in seeds.values())
    chunks = sum(d["chunks_counted"] for d in seeds.values())
    per_seed = {s: d["adamantium_debris"]["total_blocks"] / d["ancient_debris"]["total_blocks"] for s, d in seeds.items()}
    py = collections.Counter()
    cl = collections.Counter()
    for d in seeds.values():
        for y, n in d["adamantium_debris"]["blocks_per_y"].items(): py[int(y)] += n
        for k, n in d["adamantium_debris"]["cluster_size_histogram_26_neighbour"].items(): cl[int(k)] += n
    out_y = sum(n for y, n in py.items() if y < 8 or y > 22)
    near15 = sum(n for y, n in py.items() if 13 <= y <= 17)
    mode = max(py, key=py.get) if py else None
    big = sum(n for k, n in cl.items() if k > 2)
    binding = all(r < 1.0 for r in per_seed.values())
    height_ok = out_y == 0
    row = dict(label=label, seeds=sorted(seeds), chunks=chunks, debris=deb, adamantium=adm, ratio=adm / deb,
               ratio_min=min(per_seed.values()), ratio_max=max(per_seed.values()), per_seed={str(k): round(v, 3) for k, v in per_seed.items()},
               per_chunk=adm / chunks, min_y=min(py) if py else None, max_y=max(py) if py else None, blocks_outside_8_22=out_y,
               share_outside=out_y / adm if adm else None, mode_y=mode, share_y13_17=near15 / adm if adm else None,
               clusters=dict(sorted(cl.items())), clusters_over_2=big, binding_ok=binding, block_y_ok=height_ok)
    rows.append(row)
    if not binding and not label.endswith("baseline_current"): bad = True
    if label.endswith("baseline_current"): pass
    elif a.strict_block_y and not height_ok: bad = True

if a.json: json.dump(rows, open(a.json, "w"), indent=1)
hdr = "| Configuration | Chunks | Debris | Adamantium | Ratio (pooled; per-seed range) | Blocks/chunk | Y range | Outside Y8–22 | Mode Y | Y13–17 share | Clusters (26-nbr) | Binding rule |"
lines = [hdr, "|" + "---|" * 12]
for r in rows:
    lines.append(f"| `{r['label']}` | {r['chunks']} | {r['debris']} | {r['adamantium']} | {r['ratio']:.1%} ({r['ratio_min']:.1%}–{r['ratio_max']:.1%}) | "
                 f"{r['per_chunk']:.3f} | {r['min_y']}–{r['max_y']} | {r['blocks_outside_8_22']} ({(r['share_outside'] or 0):.1%}) | {r['mode_y']} | "
                 f"{(r['share_y13_17'] or 0):.0%} | {r['clusters']} | {'PASS' if r['binding_ok'] else 'FAIL'} |")
table = "\n".join(lines)
print(table)
if a.md: open(a.md, "w").write(table + "\n")
sys.exit(1 if bad else 0)
