#!/usr/bin/env python3
"""ST-01 P8 verifier: compiles and runs the isolated Java prototype tests (in-memory harness, NOT a Minecraft server), replays random sequences against the
UNMODIFIED P7 reference model, and runs the static golem geometry analysis on the P6/P7 world (E and W).
Usage: python3 -I st01_p8_verify.py <out_dir>   (exit 1 on any FAIL)"""
import os, sys, json, re, subprocess, tempfile, shutil
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import st01_p8_conformance as CF

def java_tests(java_dir, conf):
    with tempfile.TemporaryDirectory() as td:
        srcs = [os.path.join(java_dir, "st01proto", f) for f in sorted(os.listdir(os.path.join(java_dir, "st01proto"))) if f.endswith(".java")]
        c = subprocess.run(["javac", "-d", td] + srcs, capture_output=True, text=True)
        if c.returncode != 0: return None, c.stderr
        r = subprocess.run(["java", "-cp", td, "st01proto.P8Tests", conf], capture_output=True, text=True)
        return r.stdout, r.stderr

def parse(out):
    res = []; extra = []
    for l in out.splitlines():
        m = re.match(r"(PASS|FAIL) (T\d+\w*) (.*)", l)
        if m: res.append(dict(id=m.group(2), verdict=m.group(1), title=m.group(3)))
        elif l.startswith(("SELECTOR", "MATRIX")): extra.append(l)
    return res, extra

def main(outdir):
    os.makedirs(outdir, exist_ok=True); conf = os.path.join(outdir, "p8_conformance_sequences.txt"); n = CF.generate(conf)
    out, err = java_tests(os.path.join(HERE, "java"), conf)
    if out is None: print("COMPILE ERROR", err[:800]); return 1
    res, extra = parse(out)
    import st01_p8_golem as GG
    gE = GG.analyse("E"); gW = GG.analyse("W")
    ch = [dict(side="Java", **dict(r, evidence="in-memory harness run (javac 21); no Minecraft server")) for r in res]
    ch += [dict(side="E", **c) for c in gE.checks] + [dict(side="W", **c) for c in gW.checks]
    ch += [dict(side=sd, id="P7." + c["id"], title="(re-run of the P7 geometry on the same world) " + c["title"], verdict=c["verdict"], evidence=c["evidence"]) for sd, g_ in (("E", gE), ("W", gW)) for c in g_.g.checks if c["id"] in ("GA1", "GA6")]
    mir = gE.metrics["scales"] == gW.metrics["scales"] and gE.metrics["passages"] == gW.metrics["passages"]
    ch.append(dict(side="E/W", id="EW1", title="East and west golem-size/stop-safety results are identical (mirror)", verdict="PASS" if mir else "FAIL", evidence=f"scales/passages equal: {mir}"))
    ch.append(dict(side="-", id="LV1", title="Real Minecraft server: entity collision/pathfinding/scale attribute, block updates, chunk reload, multiplayer, real boss death sources, damage events", verdict="UNVERIFIED", evidence="needs Java 25 + Minecraft 26.3 toolchain and the gametest runner; not available here; no production code was changed"))
    json.dump(dict(checks=ch, selectors=[e for e in extra if e.startswith("SELECTOR")], matrix=[e for e in extra if e.startswith("MATRIX")], conformance_lines=n, golem=dict(east=gE.metrics, west=gW.metrics), p7geo=dict(zones=gE.g.metrics["zones"], Lmax=gE.g.metrics["Lmax"], route_len=gE.g.metrics["route_len"], max_fall=gE.g.metrics["max_fall"], anchor=gE.g.metrics["anchor"])), open(os.path.join(outdir, "p8_verification.json"), "w"), indent=1, default=str)
    for c in ch: print(c["side"], c["id"], c["verdict"])
    return 1 if any(c["verdict"] == "FAIL" for c in ch) else 0

if __name__ == "__main__": sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "/tmp/p8out"))
