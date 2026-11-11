"""P6 approved-coordinate overlay for P2-F1 candidate C.
The overlay is EXTRACTED mechanically from the P5 candidate-C definition (tools/st01-p5/p2f1_variants.py, read-only) by diffing it against the
UNMODIFIED P2 east model; no coordinate is typed in by hand. The P2 model itself is never written to (deep copies only).
extract() -> overlay dict; apply(base, overlay) -> new model (deep copy) built ONLY from the overlay data (so the overlay is a standalone artifact)."""
import copy, os, sys, json
HERE = os.path.dirname(os.path.abspath(__file__)); TOOLS = os.path.dirname(HERE)
for d in ("st01-p5", "st01-p2"): sys.path.insert(0, os.path.join(TOOLS, d))
from st01_p2_model_east import make_east, mirror_model
import p2f1_variants as PV

def extract():
    base = make_east(); c = PV.variant_C(base); ov = {"source": "tools/st01-p5/p2f1_variants.py::variant_C diffed against tools/st01-p2/st01_p2_model_east.py::make_east", "changed_spaces": {}}
    for k in base["spaces"]:
        b, a = base["spaces"][k], c["spaces"][k]
        if b["boxes"] != a["boxes"] or b.get("treads") != a.get("treads"):
            ent = {"before": copy.deepcopy(b["boxes"]), "after": copy.deepcopy(a["boxes"])}
            if b.get("treads") != a.get("treads"): ent["treads_before"] = copy.deepcopy(b.get("treads")); ent["treads_after"] = copy.deepcopy(a.get("treads"))
            ov["changed_spaces"][k] = ent
    other_keys = [k for k in base if k != "spaces"]
    ov["non_space_keys_changed"] = [k for k in other_keys if base[k] != c[k]]
    ov["unchanged_spaces"] = sorted(k for k in base["spaces"] if k not in ov["changed_spaces"])
    return ov

def apply(base, ov):
    m = copy.deepcopy(base)
    for k, e in ov["changed_spaces"].items():
        assert m["spaces"][k]["boxes"] == e["before"], f"overlay precondition failed for {k}"
        m["spaces"][k]["boxes"] = copy.deepcopy(e["after"])
        if "treads_after" in e: m["spaces"][k]["treads"] = copy.deepcopy(e["treads_after"])
    return m

if __name__ == "__main__":
    out = sys.argv[1]; ov = extract(); json.dump(ov, open(out, "w"), indent=1)
    print("changed:", sorted(ov["changed_spaces"]), "non-space keys changed:", ov["non_space_keys_changed"])
