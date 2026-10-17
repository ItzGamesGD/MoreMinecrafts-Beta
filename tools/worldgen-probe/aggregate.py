#!/usr/bin/env python3
"""Aggregates build/worldgen-probe/results/*.json into markdown tables + verdicts.

Usage: python3 -I tools/worldgen-probe/aggregate.py <results_dir> > report.md

Terrain-condition labels are derived from the `none` run of each pair (unmodified natural terrain around an
identical layout), then applied to both variants so encapsulate/none are compared on the same samples.
"""
import glob
import json
import os
import statistics
import sys
from collections import defaultdict

# --- condition thresholds (ring = 10-block halo sampled every 2nd block, ~40k samples) -----------------
CAVE_FRAC = 0.05      # >=5% of halo samples are open air
WATER_MIN = 100       # >=100 halo samples are water (aquifer / underground water)
LAVA_MIN = 20         # >=20 halo samples are lava
MOUNTAIN_Y = 110      # surface above this, or a mountain biome


def load(d):
    cells = {}
    for f in sorted(glob.glob(os.path.join(d, "*.json"))):
        name = os.path.basename(f)[:-5]
        shell, adapt, profile, placement, seed = name.split("__")
        doc = json.load(open(f))
        if "probe_error" in doc:
            print(f"<!-- ERROR in {name}: {doc['probe_error']} -->", file=sys.stderr)
            continue
        cells[(shell, adapt, profile, placement, seed)] = doc
    return cells


def labels(ctx):
    n = max(ctx["ring_samples"], 1)
    out = []
    if ctx["ring_air"] / n >= CAVE_FRAC:
        out.append("caves")
    if ctx["ring_water"] >= WATER_MIN:
        out.append("water")
    if ctx["ring_lava"] >= LAVA_MIN:
        out.append("lava")
    if ctx["mountain_biome"] or ctx["surface_y"] >= MOUNTAIN_Y:
        out.append("mountain")
    if ctx["overlapping_structures"]:
        out.append("overlap")
    elif ctx["nearby_structures_16"]:
        out.append("nearby")
    if ctx["depth_below_surface"] <= 0:
        out.append("breaks_surface")
    return out or ["plain_solid"]


def pct(a, b):
    return "n/a" if b == 0 else f"{100.0 * a / b:.0f}%"


def med(xs):
    return "n/a" if not xs else f"{statistics.median(xs):.0f}"


def mean(xs):
    return "n/a" if not xs else f"{statistics.mean(xs):.0f}"


def stats(samples):
    n = len(samples)
    ok = [s for s in samples if s.get("generated")]
    full = [s for s in ok if s["core_pieces_present"] == 5]
    return {
        "n": n,
        "generated": len(ok),
        "all5": len(full),
        "disc": sum(1 for s in full if s["disconnected_core_pieces"] > 0),
        "leak": sum(1 for s in full if s["breach_leak_cells"] > 0),
        "holes_pos": sum(1 for s in full if s["shell_holes_total"] > 0),
        "holes": [s["shell_holes_total"] for s in full],
        "leakc": [s["breach_leak_cells"] for s in full],
        "intr": sum(1 for s in full if s["interior_foreign_total"] > 0 and not s["context"]["overlapping_structures"]),
        "intr_overlap": sum(1 for s in full if s["interior_foreign_total"] > 0 and s["context"]["overlapping_structures"]),
        "fluid": sum(1 for s in full if s["interior_fluid_total"] > 0),
        "decor": sum(1 for s in full if sum(v.get("interior_decoration_features", 0) for v in s["pieces"].values()) > 0),
        "absent": sum(1 for s in samples if not s.get("generated") and s.get("start_state") == "absent"),
        "exp_air": [s["shell_exposed_faces_air"] for s in full],
        "exp_wl": [s["shell_exposed_faces_water"] + s["shell_exposed_faces_lava"] for s in full],
        "full": full,
    }


def main(d):
    cells = load(d)
    if not cells:
        print("no results")
        return 1
    seeds = sorted({k[4] for k in cells}, key=lambda s: int(s))
    out = []
    w = out.append

    # ---- 1. per configuration (aggregated over seeds) --------------------------------------------
    w("### 1. Per configuration (all seeds pooled)\n")
    w("| shell | placement | profile | adaptation | samples | start valid | 5/5 core pieces | disconnected | leak (sealed? no) | shell-hole structs | median holes | interior foreign blocks, excl. decoration (no overlap) | interior fluid | median exposed faces (air) | median exposed faces (water+lava) | gen s |")
    w("|---|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    groups = defaultdict(list)
    gen = defaultdict(list)
    for (shell, adapt, profile, placement, seed), doc in cells.items():
        groups[(shell, placement, profile, adapt)].extend(doc["samples"])
        gen[(shell, placement, profile, adapt)].append(doc["gen_ms"] / 1000.0)
    for key in sorted(groups):
        shell, placement, profile, adapt = key
        st = stats(groups[key])
        w(f"| {shell} | {placement} | {profile} | {adapt} | {st['n']} | {pct(st['generated'], st['n'])} | {pct(st['all5'], st['n'])} | "
          f"{st['disc']} | {st['leak']} | {st['holes_pos']} | {med(st['holes'])} | {st['intr']} | {st['fluid']} | "
          f"{med(st['exp_air'])} | {med(st['exp_wl'])} | {mean(gen[key])} |")

    # ---- 2. per seed -----------------------------------------------------------------------------
    w("\n### 2. Per seed (profile `mid`/`deep`/`high` pooled, placement `dense`)\n")
    w("| seed | shell | adaptation | samples | 5/5 core | disconnected | sealed (leak=0) | median breach cells | median exposed air faces |")
    w("|---:|---|---|---:|---:|---:|---:|---:|---:|")
    for seed in seeds:
        for shell in ("built", "bare"):
            for adapt in ("encapsulate", "none"):
                ss = []
                for profile in ("mid", "deep", "high"):
                    doc = cells.get((shell, adapt, profile, "dense", seed))
                    if doc:
                        ss.extend(doc["samples"])
                if not ss:
                    continue
                st = stats(ss)
                sealed = st["all5"] - st["leak"]
                w(f"| {seed} | {shell} | {adapt} | {st['n']} | {pct(st['all5'], st['n'])} | {st['disc']} | {sealed}/{st['all5']} | {med(st['leakc'])} | {med(st['exp_air'])} |")

    # ---- 3. by terrain condition, paired encapsulate vs none -------------------------------------
    w("\n### 3. By terrain condition, paired on identical layouts (placement `dense`)\n")
    w("Labels come from the unmodified (`none`) terrain around each sample; a sample can carry several labels.\n")
    w("| shell | condition | pairs | layout identical | sealed enc | sealed none | median breach cells enc / none | median exposed air faces enc / none | interior intrusion enc / none | interior fluid enc / none |")
    w("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|")
    pairs = defaultdict(list)
    ident = defaultdict(lambda: [0, 0])
    for (shell, adapt, profile, placement, seed), doc in cells.items():
        if adapt != "none" or placement != "dense":
            continue
        enc = cells.get((shell, "encapsulate", profile, placement, seed))
        if not enc:
            continue
        enc_by = {s["start_chunk"]: s for s in enc["samples"]}
        for s in doc["samples"]:
            e = enc_by.get(s["start_chunk"])
            if not (s.get("generated") and e and e.get("generated")):
                continue
            same = s["layout_signature"] == e["layout_signature"]
            for lab in labels(s["context"]) + ["ALL"]:
                pairs[(shell, lab)].append((e, s))
                ident[(shell, lab)][0] += int(same)
                ident[(shell, lab)][1] += 1
    for key in sorted(pairs):
        shell, lab = key
        ps = pairs[key]
        es = [p[0] for p in ps]
        ns = [p[1] for p in ps]
        sealed_e = sum(1 for s in es if s["breach_leak_cells"] == 0)
        sealed_n = sum(1 for s in ns if s["breach_leak_cells"] == 0)
        w(f"| {shell} | {lab} | {len(ps)} | {ident[key][0]}/{ident[key][1]} | {sealed_e}/{len(es)} | {sealed_n}/{len(ns)} | "
          f"{med([s['breach_leak_cells'] for s in es])} / {med([s['breach_leak_cells'] for s in ns])} | "
          f"{med([s['shell_exposed_faces_air'] for s in es])} / {med([s['shell_exposed_faces_air'] for s in ns])} | "
          f"{sum(1 for s in es if s['interior_foreign_total'] > 0)} / {sum(1 for s in ns if s['interior_foreign_total'] > 0)} | "
          f"{sum(1 for s in es if s['interior_fluid_total'] > 0)} / {sum(1 for s in ns if s['interior_fluid_total'] > 0)} |")

    # ---- 4. overlap with vanilla Trial Chambers --------------------------------------------------
    w("\n### 4. Placement against vanilla Trial Chambers\n")
    w("| placement | shell | adaptation | samples | overlapping a vanilla/other structure | within 16 blocks | overlapping vanilla `trial_chambers` | 5/5 core | disconnected | sealed (bare only meaningful) |")
    w("|---|---|---|---:|---:|---:|---:|---:|---:|---:|")
    byp = defaultdict(list)
    for (shell, adapt, profile, placement, seed), doc in cells.items():
        if placement in ("coincident", "excluded") or (placement == "dense" and profile == "mid"):
            byp[(placement, shell, adapt)].extend(doc["samples"])
    for key in sorted(byp):
        placement, shell, adapt = key
        ss = [s for s in byp[key] if s.get("generated")]
        ov = sum(1 for s in ss if s["context"]["overlapping_structures"])
        nb = sum(1 for s in ss if s["context"]["nearby_structures_16"])
        tc = sum(1 for s in ss if "minecraft:trial_chambers" in s["context"]["overlapping_structures"])
        st = stats(byp[key])
        w(f"| {placement} | {shell} | {adapt} | {len(ss)} | {ov} | {nb} | {tc} | {pct(st['all5'], st['n'])} | {st['disc']} | {st['all5'] - st['leak']}/{st['all5']} |")

    shared = [(k, doc) for k, doc in cells.items() if k[3] == "shared"]
    if shared:
        w("\n#### Shared vanilla `minecraft:trial_chambers` structure_set (probe added with weight 1)\n")
        w("| seed | start chunks of the set | ours generated | vanilla chosen instead | overlapping a vanilla trial chamber |")
        w("|---:|---:|---:|---:|---:|")
        for (shell, adapt, profile, placement, seed), doc in sorted(shared, key=lambda x: int(x[0][4])):
            ss = doc["samples"]
            ours = [s for s in ss if s.get("generated")]
            w(f"| {seed} | {len(ss)} | {len(ours)} | {sum(1 for s in ss if s.get('start_state') == 'absent')} | "
              f"{sum(1 for s in ours if 'minecraft:trial_chambers' in s['context']['overlapping_structures'])} |")

    # ---- 5. what structures were overlapped (names) ----------------------------------------------
    names = defaultdict(int)
    for doc in cells.values():
        for s in doc["samples"]:
            if s.get("generated"):
                for n in s["context"]["overlapping_structures"]:
                    names[n] += 1
    w("\n### 5. Other structures overlapped by probe footprints (count of sample-runs)\n")
    for n, c in sorted(names.items(), key=lambda x: -x[1]):
        w(f"- `{n}`: {c}")

    # ---- 6. paired reductions --------------------------------------------------------------------
    w("\n### 6. Paired effect of `encapsulate` vs `none` (all `dense` pairs)\n")
    for shell in ("built", "bare"):
        enc_h, non_h, enc_x, non_x = [], [], [], []
        better = worse = equal = 0
        for (sh, lab), ps in pairs.items():
            if sh != shell or lab != "ALL":
                continue
            for e, n in ps:
                enc_h.append(e["breach_leak_cells"]); non_h.append(n["breach_leak_cells"])
                enc_x.append(e["shell_exposed_faces_air"]); non_x.append(n["shell_exposed_faces_air"])
                if e["breach_leak_cells"] < n["breach_leak_cells"]: better += 1
                elif e["breach_leak_cells"] > n["breach_leak_cells"]: worse += 1
                else: equal += 1
        if enc_h:
            w(f"- **{shell}**: breach cells mean {mean(enc_h)} (enc) vs {mean(non_h)} (none); exposed air faces mean {mean(enc_x)} vs {mean(non_x)}; "
              f"encapsulate fewer/equal/more breach cells in {better}/{equal}/{worse} pairs.")

    # ---- 7. start failures by profile (dimension padding) ----------------------------------------
    w("\n### 7. Start failures by height profile (dimension_padding = 10 except `deepnopad`)\n")
    w("| profile | start Y range | samples | start absent | central box minY (min..max of successes) |")
    w("|---|---|---:|---:|---|")
    byprof = defaultdict(list)
    for (shell, adapt, profile, placement, seed), doc in cells.items():
        if placement == "dense":
            byprof[profile].extend(doc["samples"])
    rng = {"mid": "-40..-20", "deep": "-56..-44", "deepnopad": "-56..-44", "high": "20..40"}
    for profile in sorted(byprof):
        ss = byprof[profile]
        ys = [s["central_min_y"] for s in ss if s.get("generated")]
        w(f"| {profile} | {rng.get(profile, '?')} | {len(ss)} | {sum(1 for s in ss if not s.get('generated'))} | "
          f"{min(ys) if ys else 'n/a'}..{max(ys) if ys else 'n/a'} |")

    # ---- 8. performance --------------------------------------------------------------------------
    w("\n### 8. World-generation time, matched cells (same seed/profile/shell; 12 start regions of 11x11 chunks each)\n")
    w("| shell | profile | cells | encapsulate total s | none total s | ratio |")
    w("|---|---|---:|---:|---:|---:|")
    perf = defaultdict(lambda: [0.0, 0.0, 0])
    for (shell, adapt, profile, placement, seed), doc in cells.items():
        if adapt != "none" or placement != "dense":
            continue
        enc = cells.get((shell, "encapsulate", profile, placement, seed))
        if enc:
            p_ = perf[(shell, profile)]
            p_[0] += enc["gen_ms"] / 1000.0
            p_[1] += doc["gen_ms"] / 1000.0
            p_[2] += 1
    for (shell, profile), (e, n, c) in sorted(perf.items()):
        w(f"| {shell} | {profile} | {c} | {e:.0f} | {n:.0f} | {e / n:.2f} |")

    # ---- 8b. mountain-targeted ---------------------------------------------------------------------
    mt = [(k, d) for k, d in cells.items() if k[3] == "densemtn"]
    if mt:
        w("\n### 8b. Mountain-targeted runs (start chunks with base terrain height >= 120; placement `densemtn`)\n")
        w("| shell | profile | adaptation | samples | surface Y median (min..max) | depth below surface median | 5/5 core | sealed | median breach cells | median exposed air faces | interior fluid | foreign (no overlap) |")
        w("|---|---|---|---:|---|---:|---:|---:|---:|---:|---:|---:|")
        g = defaultdict(list)
        for (shell, adapt, profile, placement, seed), doc in mt:
            g[(shell, profile, adapt)].extend(doc["samples"])
        for key in sorted(g):
            shell, profile, adapt = key
            st = stats(g[key])
            ys = [x["context"]["surface_y"] for x in st["full"]]
            ds = [x["context"]["depth_below_surface"] for x in st["full"]]
            w(f"| {shell} | {profile} | {adapt} | {st['n']} | {med(ys)} ({min(ys) if ys else 'n/a'}..{max(ys) if ys else 'n/a'}) | {med(ds)} | "
              f"{pct(st['all5'], st['n'])} | {st['all5'] - st['leak']}/{st['all5']} | {med(st['leakc'])} | {med(st['exp_air'])} | {st['fluid']} | {st['intr']} |")

    # ---- 8c. later structure step ------------------------------------------------------------------
    late = [(k, d) for k, d in cells.items() if k[3] == "denselate"]
    if late:
        w("\n### 8c. Structure step `underground_structures` (default) vs `strongholds` (paired, encapsulate, same start chunks)\n")
        w("| shell | pairs | layout identical | sealed (default step) | sealed (late step) | foreign blocks, no overlap (default) | foreign blocks, no overlap (late) | interior fluid (default / late) |")
        w("|---|---:|---:|---:|---:|---:|---:|---:|")
        agg = defaultdict(lambda: defaultdict(int))
        for (shell, adapt, profile, placement, seed), doc in late:
            base = cells.get((shell, adapt, profile, "dense", seed))
            if not base:
                continue
            bmap = {x["start_chunk"]: x for x in base["samples"]}
            for x in doc["samples"]:
                b = bmap.get(x["start_chunk"])
                if not (b and b.get("generated") and x.get("generated")):
                    continue
                a = agg[shell]
                a["pairs"] += 1
                a["same"] += int(b["layout_signature"] == x["layout_signature"])
                a["sealed_b"] += int(b["breach_leak_cells"] == 0)
                a["sealed_l"] += int(x["breach_leak_cells"] == 0)
                a["intr_b"] += int(b["interior_foreign_total"] > 0 and not b["context"]["overlapping_structures"])
                a["intr_l"] += int(x["interior_foreign_total"] > 0 and not x["context"]["overlapping_structures"])
                a["fl_b"] += int(b["interior_fluid_total"] > 0)
                a["fl_l"] += int(x["interior_fluid_total"] > 0)
        for shell, a in sorted(agg.items()):
            w(f"| {shell} | {a['pairs']} | {a['same']}/{a['pairs']} | {a['sealed_b']}/{a['pairs']} | {a['sealed_l']}/{a['pairs']} | {a['intr_b']} | {a['intr_l']} | {a['fl_b']} / {a['fl_l']} |")

    # ---- 9. verdicts -----------------------------------------------------------------------------
    w("\n### 9. Verdicts (criteria fixed before the data were read)\n")

    def verdict(name, cond, detail):
        w(f"- **{'PASS' if cond else 'FAIL'}** — {name}: {detail}")

    for shell in ("built", "bare"):
        for adapt in ("encapsulate", "none"):
            allsub = [s for k, doc in cells.items() if k[0] == shell and k[1] == adapt and k[3] == "dense" and k[2] in ("mid", "high")
                      for s in doc["samples"]]
            if not allsub:
                continue
            sub = [s for s in allsub if not (s.get("generated") and s["context"]["overlapping_structures"])]
            st = stats(sub)
            tag = f"[{shell}/{adapt}, mid+high, {len(allsub) - len(sub)} overlap samples excluded]"
            verdict(f"{tag} generation success (>=99% valid, 5/5 core pieces)", st["all5"] >= 0.99 * st["n"], f"{st['all5']}/{st['n']}")
            verdict(f"{tag} pieces connected (0 disconnected)", st["disc"] == 0, f"{st['disc']} structures with a disconnected core piece")
            verdict(f"{tag} sealed from outside (0 leak cells in every sample)", st["leak"] == 0, f"{st['all5'] - st['leak']}/{st['all5']} sealed")
            verdict(f"{tag} no foreign blocks inside rooms (excl. biome decoration; samples not overlapping another structure)", st["intr"] == 0,
                    f"{st['intr']} structures; {st['intr_overlap']} more only where another structure overlaps; {st['decor']} contain vanilla biome decoration (vines/lichen/...)")
            verdict(f"{tag} no fluid inside rooms", st["fluid"] == 0, f"{st['fluid']} structures with interior fluid")
    for adapt in ("encapsulate", "none"):
        sub = [s for k, doc in cells.items() if k[1] == adapt and k[2] == "deep" and k[3] == "dense" for s in doc["samples"]]
        if sub:
            st = stats(sub)
            verdict(f"[{adapt}, profile deep (padding 10)] generation success", st["all5"] >= 0.99 * st["n"], f"{st['all5']}/{st['n']} (absent starts: {st['absent']})")
        sub = [s for k, doc in cells.items() if k[1] == adapt and k[2] == "deepnopad" and k[3] == "dense" for s in doc["samples"]]
        if sub:
            st = stats(sub)
            verdict(f"[{adapt}, profile deepnopad (padding 0)] generation success", st["all5"] >= 0.99 * st["n"], f"{st['all5']}/{st['n']}")
    print("\n".join(out))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
