#!/usr/bin/env python3
"""Negative controls for the P7 verifier: deliberately faulty state machines and deliberately broken geometry must FAIL the expected checks.
Usage: python3 -I st01_p7_mutation_tests.py [--out results.json]"""
import os, sys, json
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import st01_p7_verify as V, st01_p7_geometry as G

def mfail(bug):
    return {c["id"] for c in V.model_checks(frozenset([bug])) if c["verdict"] == "FAIL"}
def gfail(mut, side="E"):
    return {c["id"] for c in G.analyse(side, mutate=mut).checks if c["verdict"] == "FAIL"}
def chest_shift(p4, p3): p3["elements"]["CHEST_PRIMARY"]["boxes"][0][4] += 1; p3["elements"]["CHEST_PRIMARY"]["boxes"][0][5] += 1
def fa_head3(p4, p3): [b.__setitem__(3, b[3] - 1) for b in p4["spaces"]["FA"]["boxes"]]
def fa_drop_last(p4, p3): p4["spaces"]["FA"]["boxes"].pop()
def widen_cut(p4, p3): p4["spaces"]["CB1"]["boxes"].append([-20, -18, -23, -23, 10, 12])
def lower_start(p4, p3):
    for b in p4["spaces"]["FA"]["boxes"][:1]: b[2] -= 1; b[3] -= 1
MM = [
 ("a defeat event is trusted without re-checking the defeated record (spoofed/stale/wrong-tomb event opens the entrance)", "trust_event", {"SM1", "SM5"}),
 ("timer starts when the chest is touched before the kill", "chest_starts_timer", {"SM1", "SM5"}),
 ("fallback timer also marks the guardian defeated / unlocks the chest (reward bypass)", "fallback_unlocks_chest", {"SM1"}),
 ("duplicate defeat events spawn a second golem", "dup_spawn", {"SM1"}),
 ("golem loss cancels the fallback timer", "cancel_timer_on_loss", {"SM1"}),
 ("server restart forgets the entrance record", "restart_resets_record", {"SM1", "SM3"}),
 ("server restart forgets the remaining time", "restart_loses_deadline", {"SM1", "SM3"}),
 ("opening never applied physically when the cover chunk is unloaded at open time", "no_physical_reconcile", {"SM1"}),
 ("lost defeat event is never reconciled from the boss record", "no_reconcile_record", {"SM1", "SM2"}),
 ("timer opens a SEALED entrance", "open_from_sealed_timer", {"SM1", "SM5"}),
 ("early chest interaction opens the entrance", "chest_opens_entrance", {"SM1", "SM5"}),
 ("opening buries a player standing on the cover", "bury_on_open", {"SM1"}),
 ("cover is restored automatically when the player leaves (no restore rule is approved)", "auto_restore", {"SM1"}),
 ("auto-restore under a player", "auto_restore_buries", {"SM1"}),
 ("duplicate open request opens a SEALED entrance", "open_from_sealed_request", {"SM1", "SM5"}),
 ("tombs share one record", "shared_record", {"SM4"}),
]
GM = [
 ("anchor-relative cover offsets wrong (chest anchor moved one block)", chest_shift, {"GA5"}),
 ("stair void top lowered by one (the cut region changes)", fa_head3, {"GA5"}),
 ("last stair tread missing (landing 2 below, no climb-out)", fa_drop_last, {"GA1"}),
 ("cover region grows to 15 cells (stray floor cuts)", widen_cut, {"GA2", "GA5"}),
]
def run():
    out = []; bad = 0
    base = {c["id"] for c in V.model_checks() if c["verdict"] == "FAIL"} | {c["id"] for c in G.analyse("E").checks if c["verdict"] == "FAIL"}
    print(f"{'OK  ' if not base else 'BAD '} control (unmutated) -> FAIL set {sorted(base)} (expected none)"); bad += 1 if base else 0
    for name, bug, exp in MM:
        f = mfail(bug); hit = exp <= f; bad += 0 if hit else 1
        out.append(dict(mutation=name, kind="state machine", expected=sorted(exp), failed=sorted(f), detected=hit))
        print(f"{'DETECTED ' if hit else 'MISSED   '} [SM] {name} -> expected {sorted(exp)} got {sorted(f)}")
    for name, mut, exp in GM:
        try: f = gfail(mut); err = None
        except Exception as e: f, err = set(), repr(e)
        hit = err is None and exp <= f; bad += 0 if hit else 1
        out.append(dict(mutation=name, kind="geometry", expected=sorted(exp), failed=sorted(f), detected=hit))
        print(f"{'DETECTED ' if hit else 'MISSED   '} [GEO] {name} -> expected {sorted(exp)} got {sorted(f)}{' ERR ' + err if err else ''}")
    nd = sum(o["detected"] for o in out); print(f"{nd}/{len(out)} mutations detected; control {'clean' if not base else 'NOT clean'}")
    if "--out" in sys.argv: json.dump(dict(control_clean=not base, results=out), open(sys.argv[sys.argv.index("--out") + 1], "w"), indent=1)
    return 0 if bad == 0 else 1
if __name__ == "__main__": sys.exit(run())
