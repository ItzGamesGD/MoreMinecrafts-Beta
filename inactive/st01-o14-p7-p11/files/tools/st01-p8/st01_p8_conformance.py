"""Generates differential-test sequences from the UNMODIFIED P7 reference model (tools/st01-p7/p7_statemachine.py), D=2.
Line format: 'S' starts a sequence; 'E <event> <rec> <cover> <ngolem> <boss> <remaining> <spawn_pending>' is the expected state AFTER the event."""
import os, sys, random
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, "..", "st01-p7"))
import p7_statemachine as M
def generate(path, seqs=4000, depth=14, seed=11):
    rng = random.Random(seed); out = []
    for _ in range(seqs):
        s = M.INIT; out.append("S")
        for _ in range(rng.randint(1, depth)):
            ev = [e for e in M.EVENTS if M.enabled(s, e)]; e = rng.choice(ev)
            s = M.step(s, e)
            out.append(f"E {e} {s.rec} {int(s.cover)} {s.ngolem} {int(s.boss)} {s.remaining} {int(s.spawn_pending)}")
    open(path, "w").write("\n".join(out) + "\n"); return len(out)
if __name__ == "__main__": print(generate(sys.argv[1]))
