#!/usr/bin/env python3
"""Exhaustive crash-state model for one wither-skull pickup (XP Quest F-1 / PR #128, #130).

Three storage domains persist independently (vanilla gives no cross-file transaction):
  C  chunk entity storage   : is the ground item entity (carrying origin token T) still present?
  A  player A's file        : skull count, quest credit, transaction receipts (one atomic file)
  L  world ledger file      : token T -> absent | PENDING(owner) | CONSUMED(owner)

Persistence semantics modelled, matching vanilla 26.3 as reviewed in docs/Q1C_PR128_REPAIR_DESIGN.md on PR #130:
  * every write creates a new in-memory version of its domain;
  * a crash may leave each domain at ANY version at or after its last explicit flush, chosen independently
    (autosave can persist any domain at any time, in any order);
  * flush(domain) makes the current version durable (synchronous save, e.g. ContractLedger.flush, PlayerList save).
After the crash the server restarts, the protocol's recovery runs, and a second player B then tries to pick up the
entity if it still exists. All crash points x all persistence choices are enumerated.

Invariants checked on the recovered world:
  INV1 no double credit      : credit(A) + credit(B) <= 1
  INV2 no uncredited receipt : A durably holds the skull  =>  credit(A) == 1
  INV3 no credit without item: credit(A) == 1  =>  A durably holds the skull
Reported, not asserted: physical skull count (loss / duplication) - vanilla cannot make that atomic.

usage: skull_protocol_model.py [--check] [--json]
"""
import itertools, json, sys
from dataclasses import dataclass, field, replace

T = "T"

@dataclass(frozen=True)
class World:
    C: bool = True                                   # entity present
    A: tuple = (0, 0, ())                            # skulls, credit, receipts
    B: tuple = (0, 0, ())
    L: tuple = ()                                    # (token, state, owner) or ()

def setA(w, **kw):
    s, c, r = w.A
    return replace(w, A=(kw.get("s", s), kw.get("c", c), kw.get("r", r)))

# --- protocols: a pickup is a list of steps ("write"|"flush", domain, fn) -------------------------------------------
# fn maps the in-memory World to the new World.
def P0_boolean_on_entity():
    # PR #130 as shipped: entity Boolean + counter in player file; no ledger.
    return [("write", "C", lambda w: replace(w, C=False)),
            ("write", "A", lambda w: setA(w, s=w.A[0] + 1, c=w.A[1] + 1))]

def P1_journal_after_insert():
    return [("write", "A", lambda w: setA(w, s=w.A[0] + 1)),
            ("write", "L", lambda w: replace(w, L=(T, "CONSUMED", "A"))),
            ("write", "C", lambda w: replace(w, C=False))]

def P2_journal_before_insert():
    return [("write", "L", lambda w: replace(w, L=(T, "CONSUMED", "A"))),
            ("write", "C", lambda w: replace(w, C=False)),
            ("write", "A", lambda w: setA(w, s=w.A[0] + 1))]

def P3_prepare_receipt_commit():
    # ContractTransactions pattern: PENDING flushed first; inventory + receipt + credit in one player file;
    # player save is the commit point; ledger commit afterwards.
    return [("write", "L", lambda w: replace(w, L=(T, "PENDING", "A"))),
            ("flush", "L", None),
            ("write", "C", lambda w: replace(w, C=False)),
            ("write", "A", lambda w: setA(w, s=w.A[0] + 1, c=w.A[1] + 1, r=("tx",))),
            ("flush", "A", None),
            ("write", "L", lambda w: replace(w, L=(T, "CONSUMED", "A"))),
            ("flush", "L", None),
            ("write", "A", lambda w: setA(w, r=())),
            ("flush", "A", None)]

# --- recovery after restart ------------------------------------------------------------------------------------------
def recover_none(w): return w

def recover_projection(w):
    # credit is a projection of the ledger: credit(A) = tokens consumed by A. Skull count is whatever is durable.
    credit = 1 if w.L and w.L[1] == "CONSUMED" and w.L[2] == "A" else 0
    return setA(w, c=credit)

def recover_receipt(w):
    # Resolve a PENDING entry from the owner's own durable receipt only.
    if w.L and w.L[1] == "PENDING":
        if w.A[2]:                                   # receipt present -> roll forward
            w = replace(w, L=(T, "CONSUMED", "A"))
        else:                                        # player data predates the transaction -> roll back
            w = replace(w, L=())
    if w.A[2] and not w.L:                           # defensive heal: receipt without ledger entry
        w = replace(w, L=(T, "CONSUMED", "A"))
    return setA(w, r=())

def second_pickup_B(proto, w):
    """Player B picks up the entity if it survived. Credit only if the protocol still considers T unconsumed."""
    if not w.C:
        return w
    s, c, r = w.B
    token_free = not w.L
    if proto == "P0":
        credited = True                              # Boolean on the entity is all P0 has
    else:
        credited = token_free
    w = replace(w, C=False, B=(s + 1, c + (1 if credited else 0), r))
    if proto != "P0" and token_free:
        w = replace(w, L=(T, "CONSUMED", "B"))
    return w

PROTOCOLS = {
    "P0": ("#130 as shipped: Boolean on entity + counter in player file", P0_boolean_on_entity, recover_none),
    "P1": ("journal appended AFTER Inventory.add", P1_journal_after_insert, recover_projection),
    "P2": ("journal appended BEFORE Inventory.add", P2_journal_before_insert, recover_projection),
    "P3": ("PENDING flush -> inventory+receipt in one player file -> player save -> ledger commit",
           P3_prepare_receipt_commit, recover_receipt),
}

def versions(history, last_flush):
    return range(last_flush, len(history))

def enumerate_crashes(steps):
    """Yield (crash_after_step_index, persisted World) for every crash point and every independent persistence choice."""
    base = World()
    for k in range(len(steps) + 1):                  # crash after k steps (k = len(steps): crash after completion)
        hist = {d: [getattr(base, d)] for d in "CALB" if d in "CAL"}
        mem = base
        last = {d: 0 for d in hist}
        for kind, dom, fn in steps[:k]:
            if kind == "write":
                mem = fn(mem)
                hist[dom].append(getattr(mem, dom))
            else:
                last[dom] = len(hist[dom]) - 1
        choices = [list(versions(hist[d], last[d])) for d in ("C", "A", "L")]
        for c, a, l in itertools.product(*choices):
            yield k, replace(base, C=hist["C"][c], A=hist["A"][a], L=hist["L"][l])

def evaluate(name):
    desc, steps_fn, recover = PROTOCOLS[name]
    steps = steps_fn()
    states = 0
    viol = {"INV1": [], "INV2": [], "INV3": []}
    physical = {"lost": 0, "duplicated": 0, "exact": 0}
    for k, persisted in enumerate_crashes(steps):
        states += 1
        w = recover(persisted)
        w = second_pickup_B(name, w)
        credit = w.A[1] + w.B[1]
        ctx = (k, persisted.C, persisted.A, persisted.L)
        if credit > 1: viol["INV1"].append(ctx)
        if w.A[0] >= 1 and w.A[1] != 1: viol["INV2"].append(ctx)
        if w.A[1] == 1 and w.A[0] < 1: viol["INV3"].append(ctx)
        skulls = w.A[0] + w.B[0] + (1 if w.C else 0)
        physical["lost" if skulls < 1 else "duplicated" if skulls > 1 else "exact"] += 1
    return desc, states, viol, physical

def main():
    results = {}
    for name in PROTOCOLS:
        desc, states, viol, physical = evaluate(name)
        results[name] = dict(description=desc, crash_states=states,
                             violations={k: len(v) for k, v in viol.items()},
                             example={k: (None if not v else dict(crash_after_step=v[0][0], chunk_has_entity=v[0][1],
                                      player_A=dict(skulls=v[0][2][0], credit=v[0][2][1], receipts=list(v[0][2][2])),
                                      ledger=list(v[0][3]) or None)) for k, v in viol.items()},
                             physical_outcomes=physical)
    if "--json" in sys.argv:
        print(json.dumps(results, indent=2)); return 0
    for name, r in results.items():
        print(f"{name}: {r['description']}")
        print(f"    crash states enumerated: {r['crash_states']}")
        print("    invariant violations   : " + ", ".join(f"{k}={v}" for k, v in r["violations"].items()))
        print(f"    physical skull outcomes: {r['physical_outcomes']}")
    if "--check" in sys.argv:
        # Findings this model is expected to keep reproducing (verified by running it, not assumed in advance):
        #   P0 double-credits (the observed A=1,B=1); P1 and P2 each break BOTH single-file-consistency invariants;
        #   only P3 (ledger PENDING flush + receipt in the player file) is clean across every crash state.
        expect = {"P0": {"INV1"}, "P1": {"INV2", "INV3"}, "P2": {"INV2", "INV3"}, "P3": set()}
        ok = True
        for name, r in results.items():
            failing = {k for k, v in r["violations"].items() if v}
            if failing != expect[name]:
                print(f"CHECK FAILED {name}: violated {sorted(failing)}, expected {sorted(expect[name])}"); ok = False
        print("MODEL CHECK", "PASS" if ok else "FAIL")
        return 0 if ok else 1
    return 0

if __name__ == "__main__":
    sys.exit(main())
