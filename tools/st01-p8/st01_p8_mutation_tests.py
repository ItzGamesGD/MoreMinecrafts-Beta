#!/usr/bin/env python3
"""Negative controls for the P8 prototype: broken copies of the Java sources must make the expected tests FAIL. Usage: python3 -I st01_p8_mutation_tests.py [--out results.json]"""
import os, sys, json, shutil, tempfile, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import st01_p8_conformance as CF, st01_p8_verify as V
EC = "EntranceCoordinator.java"
MUT = [
 ("timer only runs while guiding (golem spawn failure stalls it)", EC, [("if ((state == State.GUARDIAN_DEFEATED || state == State.GOLEM_GUIDING) && remaining >= 0)", "if (state == State.GOLEM_GUIDING && remaining >= 0)")], {"T4b"}),
 ("entrance progresses on any death, not on a valid victory", EC, [("if (store.isVictory(id) && state == State.SEALED)", "if (store.hasDied(id) && state == State.SEALED)")], {"T2", "T3", "T3b"}),
 ("boss marks victory/grants reward for invalid kills", "DummyBoss.java", [("if (v.valid()) { store.markVictory(id); reward.grant(id); } else retryAllowed = true;", "store.markVictory(id); reward.grant(id); retryAllowed = !v.valid();")], {"T2", "T3", "T3b"}),
 ("administrative kill accepted", "ParticipationTracker.java", [('if (cause == DeathCause.COMMAND_KILL) return new Verdict(false, "administrative kill is never a victory", parts);', "")], {"T3"}),
 ("second golem can be spawned by a repeated signal (coordinator and world-port guards both removed)", "*", [(EC, "if (!spawnPending || !(state == State.GUARDIAN_DEFEATED || (state == State.GOLEM_GUIDING && guide == null))) return;", "if (!spawnPending) return;"), (EC, "if (golem.aliveCount(id) > 0) { spawnPending = false; return; }       // uniqueness guard", ""), (EC, "public void onDefeatSignal() { reconcile(); }", "public void onDefeatSignal() { spawnPending = state != State.SEALED; reconcile(); }"), ("FakeWorld.java", "if (g != null && g.alive) return new GolemPort.SpawnResult(true, g.id);      // a real port never creates a second golem", "")], {"T5a"}),
 ("restart forgets the remaining time", EC, [('+ state + "|" + remaining + "|"', '+ state + "|" + cfg.timerTicks() + "|"')], {"T7a"}),
 ("restart forgets the entrance state", EC, [('+ "|" + state + "|"', '+ "|" + State.SEALED + "|"')], {"T7a"}),
 ("cover applied while the chunk is unloaded", EC, [("if (state.open() && cover.chunkLoaded(id)) {", "if (state.open()) {")], {"T8"}),
 ("OPEN record no longer repairs reverted blocks", EC, [("if (state.open() && cover.chunkLoaded(id)) { if (!cover.isOpen(id)) cover.openCells(id); coverApplied = true; }", "if (state.open() && !coverApplied && cover.chunkLoaded(id)) { cover.openCells(id); coverApplied = true; }")], {"T7d"}),
 ("stalled golem can still arrive", "FakeWorld.java", [("g.alive && !g.stalled && g.atStop", "g.alive && g.atStop")], {"T6a"}),
 ("victory flag is global instead of per instance", "FakeWorld.java", [("public boolean isVictory(InstanceId id) { return victories.contains(id); }", "public boolean isVictory(InstanceId id) { return !victories.isEmpty(); }")], {"T10c"}),
 ("OPEN record without a victory still opens the cover", EC, [("if (state.open() && !store.isVictory(id)) { anomaly = true; return; }", "")], {"T15"}),
 ("timer expiry off by one", EC, [("if (remaining <= 0 && store.isVictory(id)) state = State.FALLBACK_OPEN;", "if (remaining < 0 && store.isVictory(id)) state = State.FALLBACK_OPEN;")], {"T13"}),
 ("chest interaction opens the entrance", EC, [("public void onChestInteraction() { /* the entrance never depends on chest interaction */ }", "public void onChestInteraction() { state = State.ENTRANCE_OPEN; }")], {"T1"}),
 ("a TEST-ONLY looking timer becomes the production default", "Config.java", [("new Config(-1, 1, false, false)", "new Config(600, 1, false, false)")], {"T0"}),
 ("guide selector follows offline players", "GuideTargetSelector.java", [("return ps.stream().filter(P::online).min(Comparator.comparingDouble(p -> Math.hypot(p.x() - gx, p.z() - gz))).map(P::id);\n        }\n    }\n    /** Keeps", "return ps.stream().min(Comparator.comparingDouble(p -> Math.hypot(p.x() - gx, p.z() - gz))).map(P::id);\n        }\n    }\n    /** Keeps")], {"T11"}),
]
def run():
    conf = os.path.join(tempfile.gettempdir(), "p8_mut_conf.txt"); CF.generate(conf)
    out, err = V.java_tests(os.path.join(HERE, "java"), conf); res, _ = V.parse(out)
    ctrl = [r["id"] for r in res if r["verdict"] == "FAIL"]; print(f"{'OK  ' if not ctrl else 'BAD '} control -> FAIL {ctrl}")
    results = []; bad = 1 if ctrl else 0
    for name, fn, patches, expect in MUT:
        with tempfile.TemporaryDirectory() as td:
            shutil.copytree(os.path.join(HERE, "java"), os.path.join(td, "java")); okp = True
            for pt in patches:
                f_, a, b = pt if len(pt) == 3 else (fn, pt[0], pt[1])
                p = os.path.join(td, "java", "st01proto", f_); s = open(p).read()
                if a not in s: okp = False; break
                open(p, "w").write(s.replace(a, b, 1))
            if not okp: print("PATCH NOT APPLIED", name); bad += 1; results.append(dict(mutation=name, expected=sorted(expect), failed=[], detected=False, note="patch not applied")); continue
            out, err = V.java_tests(os.path.join(td, "java"), conf)
        if out is None: failed = {"COMPILE"}
        else: failed = {r["id"] for r in parse_(out) if r["verdict"] == "FAIL"}
        if not expect: print(f"NEUTRAL   {name} (redundant guard; failed {sorted(failed)})"); continue
        hit = expect <= failed; bad += 0 if hit else 1
        results.append(dict(mutation=name, expected=sorted(expect), failed=sorted(failed), detected=hit))
        print(f"{'DETECTED ' if hit else 'MISSED   '} {name} -> expected {sorted(expect)} got {sorted(failed)}")
    nd = sum(r["detected"] for r in results); print(f"{nd}/{len(results)} mutations detected; control {'clean' if not ctrl else 'NOT clean'}")
    if "--out" in sys.argv: json.dump(dict(control_clean=not ctrl, results=results), open(sys.argv[sys.argv.index("--out") + 1], "w"), indent=1)
    return 0 if bad == 0 else 1
def parse_(out): return V.parse(out)[0]
if __name__ == "__main__": sys.exit(run())
