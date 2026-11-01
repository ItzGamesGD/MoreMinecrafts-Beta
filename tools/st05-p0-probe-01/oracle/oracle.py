#!/usr/bin/env python3
"""ST-05 Probe 01 - G-3 golden oracle (CANDIDATE goldens only; nothing is frozen or pinned).

Independent Python 3 standard-library oracle:
  Tier A  tier_a.py / negatives.py   hand-authored literals transcribed from the execution spec
  Tier B  this file                  closed-form / set-membership derivations (exact Fractions):
                                     arithmetic via the expanded coefficient identity, long chains via
                                     the parity theorem, accounting prefix states via event-set membership
  Cross-check                        Tier A and Tier B must agree byte-for-byte on every overlap, graph
                                     goldens must satisfy the decision-legality invariant against the input
                                     graph, invalid-input expectations must agree with an independent
                                     loader-rule engine run on the actual files.

It never runs, imports or compares against a subject/mutant/runner and does not use the materializer's
code. The materialized inputs/schedules/manifest/controls are read ONLY as data (to enumerate sessions
and to check referential integrity); expected values are never taken from them.
Usage: python3 oracle.py   (writes goldens-v1.json and CANDIDATE-GOLDEN-DIGESTS.sha256 next to this file)
"""
import hashlib
import json
import os
import sys
from fractions import Fraction

HERE = os.path.dirname(os.path.abspath(__file__))
TOOL = os.path.dirname(HERE)
sys.path.insert(0, HERE)
import tier_a as TA  # noqa: E402
import negatives as NG  # noqa: E402
import loader_rules as LR  # noqa: E402

PROBLEMS = []


def check(cond, msg):
    if not cond:
        PROBLEMS.append(msg)
    return cond


def fs(f):
    f = Fraction(f)
    return "%d/%d" % (f.numerator, f.denominator)


def parse_q(s):
    """Parse 'n', 'n/d' or the two FORMULA: forms used in tier_a."""
    if s.startswith("FORMULA:"):
        body = s[8:]
        if body == "50+1/2^80":
            return Fraction(50) + Fraction(1, 2 ** 80)
        if body == "40+1/(5*2^79)":
            return Fraction(40) + Fraction(1, 5 * 2 ** 79)
        raise ValueError(body)
    n, _, d = s.partition("/")
    return Fraction(int(n), int(d or 1))


def tot(t):
    """S = (4C + 4T + F + E) / 10 for a component tuple (spec 2)."""
    C, T, F, E = [parse_q(x) for x in t]
    return (4 * C + 4 * T + F + E) / 10


def rd(name):
    with open(os.path.join(TOOL, name), "rb") as stream:
        return stream.read()


def load(name):
    return json.loads(rd(name).decode())


# ===================================================================================== Tier B: arithmetic
RAW = {0: [0] * 9, 1: [100] * 9, 2: [50, 75, 80, 75, 75, 75, 75, 50, 50]}
for k in range(9):
    RAW[3 + k] = [100 if j == k else 0 for j in range(9)]
COEF = [16, 12, 12, 24, 16, 5, 5, 6, 4]  # expanded coefficients of S over (L,H,J,M,G,W,R,D,A), /100


def arith_b(n):
    L, H, J, M, G, W, R, Dd, Aa = [Fraction(x) for x in RAW[n]]
    C, T, F, E = (4 * L + 3 * H + 3 * J) / 10, (3 * M + 2 * G) / 5, (W + R) / 2, (3 * Dd + 2 * Aa) / 5
    S_nested = (4 * C + 4 * T + F + E) / 10
    S_expanded = sum(Fraction(c) * Fraction(x) for c, x in zip(COEF, RAW[n])) / 100
    check(S_nested == S_expanded, "V%d: nested vs expanded identity" % n)
    return [fs(C), fs(T), fs(F), fs(E)], fs(S_nested)


def arith_goldens():
    out, prov = {}, {}
    for n in range(12):
        comps, total = arith_b(n)
        ta_c, ta_s = TA.ARITH["V%d" % n]
        check([fs(parse_q(x)) for x in ta_c] == comps and fs(parse_q(ta_s)) == total, "V%d: Tier A vs Tier B" % n)
        out["P01/ARITH-V%d" % n] = {"kind": "ARITH", "scalars": {"V%d" % n: {"components": comps, "total": total}}, "tag": "LITERAL_SPEC+CLOSED_FORM"}
    scal = {}
    for n in (0, 1, 2):
        comps, total = arith_b(n)
        scal["V%d" % n] = {"components": comps, "total": total}
    kc = [fs(parse_q(x)) for x in TA.K4[0]]
    check(fs(tot(TA.K4[0])) == fs(parse_q(TA.K4[1])), "K4 total")
    scal["K4"] = {"components": kc, "total": fs(tot(TA.K4[0]))}
    out["P01/FIXED-01"] = {"kind": "ARITH", "scalars": scal, "batch_invariant": True, "tag": "LITERAL_SPEC+CLOSED_FORM"}
    return out


# ===================================================================================== graphs
def sort_ids(labels, ids):
    return sorted(labels, key=lambda l: ids[l])


def graph_goldens(inputs):
    ids_of = {}
    out = {}
    for fid, g in TA.GRAPHS.items():
        tuples = TA.TUPLES[fid]
        totals_b = {l: fs(tot(t)) for l, t in tuples.items()}
        lit = {l: fs(parse_q(v)) for l, v in g["totals"].items()}
        check(totals_b == lit, "%s: Tier A totals vs formula(tuples)" % fid)
        comps = {l: [fs(parse_q(x)) for x in t] for l, t in tuples.items()}
        out[fid] = {"kind": "GRAPH", "totals": lit, "components": comps, "evaluation_order": g["evaluation_order"],
                    "decisions": g["decisions"], "accepted": g["accepted"], "spacing_rejected": g["spacing_rejected"],
                    "protected_rejected": g["protected_rejected"], "roll_loss": [], "not_rolled": [], "pending": [],
                    "protected_input": g.get("protected_input", []), "model_knowledge_status": "KNOWN",
                    "input_rejection": None, "tag": g["tag"]}
    c = TA.CAND01
    out["P01/CAND-01"] = {"kind": "GRAPH", "candidate_totals": c["candidate_totals"], "candidate_order": c["candidate_order"],
                          "selected": c["selected"], "totals": c["totals"], "components": c["components"],
                          "evaluation_order": c["evaluation_order"], "decisions": c["decisions"], "accepted": c["accepted"],
                          "spacing_rejected": [], "protected_rejected": [], "roll_loss": [], "not_rolled": [], "pending": [],
                          "model_knowledge_status": "KNOWN", "input_rejection": None, "tag": c["tag"]}
    # candidate totals via formula
    check(fs(tot(("0", "100", "0", "0"))) == "40/1" and fs(tot(("100", "0", "0", "0"))) == "40/1", "CAND-01 candidate totals")
    return out


def roll_goldens():
    out = {}
    base = TA.ROLL_COMMON
    for fid, with_d in (("P01/ROLL-01", False), ("P01/NOFIT-01", True)):
        st = TA.FINAL["P01/NOFIT-01/LOG" if with_d else "P01/ACCT-BASE"]
        t = {"a": ("100", "100", "0", "0"), "b": ("100", "0", "0", "0"), "c": ("50", "0", "0", "0")}
        check({k: fs(tot(v)) for k, v in t.items()} == base["totals"], fid + ": totals vs formula")
        totals = dict(base["totals"])
        if with_d:
            totals["d"] = "NOT_APPLICABLE"
        g = {"kind": "GRAPH", "totals": totals, "components": dict(base["components"]), "selected": base["selected"],
             "candidate_order": base["candidate_order"], "evaluation_order": base["evaluation_order"],
             "decisions": base["decisions"], "accepted": base["accepted"], "spacing_rejected": base["spacing_rejected"],
             "protected_rejected": [], "roll_loss": base["roll_loss"], "not_rolled": ["d"] if with_d else [],
             "pending": ["a"], "counters": counters_of(st), "fractions": fractions_of(st),
             "physical": {"physical_completed": "0", "actual_completion_fraction": {"n": "0", "d": str(len(st["rolled"]))}},
             "model_knowledge_status": "KNOWN", "input_rejection": None, "tag": "LITERAL_SPEC+DERIVED_PROSE"}
        if with_d:
            g["note"] = "fit-eligible 3 of 4 qualifying; roll fraction is 2/3, never 2/4 (spec NOFIT-01)"
        out[fid] = g
    return out


def late_golden():
    t = {k: fs(tot(v)) for k, v in TA.LATE_TUPLES.items()}
    check(t == TA.LATE["totals"], "LATE totals vs formula")
    return {"P01/LATE-01": {"kind": "LATE", "totals": TA.LATE["totals"], "checkpoints": TA.LATE["checkpoints"],
                            "model_knowledge_status": "KNOWN", "input_rejection": None, "tag": TA.LATE["tag"]}}


# ===================================================================================== long chains (closed form)
def long_b(kind, n, ext):
    """Parity theorem: the top vertex has no accepted neighbour and is accepted; each next vertex is rejected iff
    its higher neighbour is accepted. Accept i iff (top - i) is even; rejected i has witness {i+1}."""
    top = n + 1 if ext else n
    m = top + 1
    if kind == "SCORE":
        totals = [fs(Fraction(100 * i, n + 1)) if i <= n else "100/1" for i in range(m)]
    else:
        totals = ["40/1"] * m
    order = [str(i) for i in range(top, -1, -1)]
    dec, acc, rej = [], [], []
    for i in range(top, -1, -1):
        if (top - i) % 2 == 0:
            dec.append(TA.D(str(i), TA.A))
            acc.append(str(i))
        else:
            dec.append(TA.D(str(i), TA.RS, [str(i + 1)]))
            rej.append(str(i))
    if kind == "SCORE":      # ids ascending == labels ascending
        key = lambda s: int(s)
    else:                    # ids are -label: ascending id == labels descending
        key = lambda s: -int(s)
    return {"totals": totals, "evaluation_order": order, "decisions": dec, "accepted": sorted(acc, key=key),
            "spacing_rejected": sorted(rej, key=key)}


def parse_hand(h):
    dec = []
    for tok_ in h["dec"].split():
        v, _, rest = tok_.partition(":")
        dec.append(TA.D(v, TA.A) if rest == "A" else TA.D(v, TA.RS, [rest[1:]]))
    return {"totals": h["totals"], "evaluation_order": [d["v"] for d in dec], "decisions": dec,
            "accepted": h["acc"], "spacing_rejected": h["rej"]}


def long_goldens():
    out = {}
    for kind in ("SCORE", "TIE"):
        for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024):
            for ext in (False, True):
                g = long_b(kind, n, ext)
                tag = "CLOSED_FORM"
                if n <= 4:
                    hand = parse_hand(TA.LONG_HAND[(kind, n, "EXT" if ext else "BASE")])
                    check(hand == g, "LONG %s n=%d ext=%s: hand literal vs closed form" % (kind, n, ext))
                    tag = "HAND_LITERAL+CLOSED_FORM"
                out["P01/LONG-%s-%d/%s" % (kind, n, "EXT" if ext else "BASE")] = {
                    "kind": "LONG", "totals": {str(i): v for i, v in enumerate(g["totals"])}, "components_equal_total": True,
                    "evaluation_order": g["evaluation_order"], "decisions": g["decisions"], "accepted": g["accepted"],
                    "spacing_rejected": g["spacing_rejected"], "protected_rejected": [], "roll_loss": [], "not_rolled": [],
                    "pending": [], "model_knowledge_status": "KNOWN", "input_rejection": None, "tag": tag}
    return out


# ===================================================================================== accounting
def log_units_sorted(log):
    return TA.LOG_UNIT_ORDER.get(log, None)


def prefix_state(log, delivered):
    ev = TA.LOGS[log]
    dl = set(delivered)
    got = [e for e in ev if e[0] in dl]
    unit_ids = [e[2] for e in ev if e[1] == "QUALIFIED"]
    order = {u: i for i, u in enumerate(TA.LOG_UNIT_ORDER.get(log, sorted(unit_ids)))}
    srt = lambda s: sorted(s, key=lambda u: order[u])
    qual = srt({e[2] for e in got if e[1] == "QUALIFIED"})
    fit = srt({e[2] for e in got if e[1] == "FIT" and e[3]["eligible"]})
    rolled = srt({e[2] for e in got if e[1] == "ROLL"})
    wins = srt({e[2] for e in got if e[1] == "ROLL" and e[3]["result"] == "WIN" and e[2] in fit})
    loss = srt({e[2] for e in got if e[1] == "ROLL" and e[3]["result"] == "LOSS"})
    dec = [e for e in got if e[1] == "DECISION"]
    kinds = {k: srt({e[2] for e in got if e[1] == k}) for k in ("CERT_INVALID", "APPLYING", "UNCERTAIN", "QUARANTINED", "RETIRED", "RECEIPT", "EDIT")}
    if dec:
        d = dec[0][3]
        acc, spc, prt = srt(d["accepted"]), srt(d["spacing"]), srt(d["protection"])
        pend = [u for u in d["pending"] if u not in kinds["RETIRED"] and u not in kinds["RECEIPT"]]
        if (kinds["APPLYING"] or kinds["UNCERTAIN"] or kinds["QUARANTINED"]) and not kinds["RETIRED"]:
            pend = "DEFERRED:INTERMEDIATE_LIFECYCLE"
        else:
            pend = srt(pend)
    else:
        acc = spc = prt = pend = "PENDING"
    return {
        "qualified": qual, "fit_eligible": fit, "rolled": rolled, "winners": wins, "roll_loss": loss,
        "accepted_decisions": acc, "spacing_excluded": spc, "protection_excluded": prt, "pending": pend,
        "projections_delivered": sorted(e[3]["kind"] for e in got if e[1] == "PROJ"),
        "certificate_invalid": kinds["CERT_INVALID"], "applying": kinds["APPLYING"], "uncertain": kinds["UNCERTAIN"],
        "quarantined": kinds["QUARANTINED"], "retired_protected": kinds["RETIRED"], "model_success": kinds["RECEIPT"],
        "history_modified": kinds["EDIT"],
        "fit_checks": str(sum(e[3]["checks"] for e in got if e[1] == "FIT")),
    }


def counters_of(st):
    cnt = lambda k: str(len(st[k]))
    return {"qualified": cnt("qualified"), "fit_checks": st["fit_checks"], "fit_eligible": cnt("fit_eligible"),
            "rolled": cnt("rolled"), "winners": cnt("winners"), "roll_loss": cnt("roll_loss"),
            "spacing_exclusions": cnt("spacing_excluded"), "protection_exclusions": cnt("protection_excluded"),
            "accepted_decisions": cnt("accepted_decisions"), "pending": cnt("pending"), "retired": cnt("retired_protected"),
            "model_success": cnt("model_success"), "history_modified": cnt("history_modified"), "roll_attempts": cnt("rolled")}


def fractions_of(st):
    r = str(len(st["rolled"]))
    f = st["frac"] if "frac" in st else None
    return {"winning": {"n": str(len(st["winners"])), "d": r}, "logical_acceptance": {"n": str(len(st["accepted_decisions"])), "d": r}}


def final_state_b(log):
    st = prefix_state(log, [e[0] for e in TA.LOGS[log]])
    st["frac"] = None
    return st


def acct_goldens():
    out = {}
    finals = {}
    for log, lit in TA.FINAL.items():
        b = final_state_b(log)
        for k, v in lit.items():
            if k in ("frac", "fit_checks"):
                continue
            check(b[k] == v, "%s: final %s Tier A %s vs Tier B %s" % (log, k, v, b[k]))
        check(b["fit_checks"] == lit["fit_checks"], "%s: fit_checks" % log)
        wn, wd, ln, ld = lit["frac"]
        fr = fractions_of(b)
        check((fr["winning"]["n"], fr["winning"]["d"], fr["logical_acceptance"]["n"], fr["logical_acceptance"]["d"]) == (wn, wd, ln, ld), "%s: fractions" % log)
        finals[log] = b
    for h, fields in TA.PREFIX_HAND.items():
        log, idxs = h
        b = prefix_state(log, idxs)
        for k, v in fields.items():
            check(b[k] == v, "prefix hand %s %s field %s: hand %s vs derived %s" % (log, idxs[:3], k, v, b[k]))

    def mk(fid, st, disp, derived_from=None, tag="LITERAL_SPEC+DERIVED_PROSE", extra=None):
        g = {"kind": "ACCT", "sets": {k: st[k] for k in ("qualified", "fit_eligible", "rolled", "winners", "roll_loss", "spacing_excluded",
                                                         "protection_excluded", "accepted_decisions", "pending", "retired_protected",
                                                         "certificate_invalid", "model_success", "history_modified")},
             "counters": counters_of(st), "fractions": fractions_of(st),
             "physical": {"physical_completed": "0", "actual_completion_fraction": {"n": "0", "d": str(len(st["rolled"]))}},
             "event_dispositions": disp, "model_knowledge_status": "KNOWN", "frozen_decisions": [], "input_rejection": None,
             "tag": tag}
        if "protected_sites" in st:
            g["protected_sites"] = st["protected_sites"]
        if extra:
            g.update(extra)
        out[fid] = g

    allapplied = lambda log: {str(e[0]): "APPLIED" for e in TA.LOGS[log]}
    for log, st in finals.items():
        extra = {}
        if log == "P01/ACCT-RETIRE":
            extra["protected_sites"] = ["a"]
            extra["lifecycle_final"] = "RETIRED_PROTECTED"
        if log == "P01/ACCT-PROTECTED":
            extra["protected_sites"] = ["P"]
        mk(log, st, allapplied(log), extra=extra)
    # duplicates: sets and counters unchanged; the receipt branch models success exactly once
    mk("P01/ACCT-DUP-LOG", finals["P01/ACCT-BASE"], allapplied("P01/ACCT-BASE"), tag="DERIVED_PROSE")
    mk("P01/ACCT-DUP-RECEIPT", finals["P01/ACCT-MODEL-SUCCESS"], allapplied("P01/ACCT-MODEL-SUCCESS"), tag="DERIVED_PROSE")
    # projection loss: projections rebuilt exactly once from the retained decision (and receipt)
    for name, log in (("BASE", "P01/ACCT-BASE"), ("MODEL-SUCCESS", "P01/ACCT-MODEL-SUCCESS")):
        delivered = [e[0] for e in TA.LOGS[log] if e[1] != "PROJ"]
        st = prefix_state(log, delivered)
        full = finals[log]
        for k in ("accepted_decisions", "spacing_excluded", "pending", "model_success", "winners"):
            check(st[k] == full[k], "LOSS projection %s field %s must reconstruct" % (name, k))
        disp = {str(i): "APPLIED" for i in delivered}
        mk("P01/ACCT-LOSS-%s-PROJECTION" % name, full, disp, tag="DERIVED_PROSE",
           extra={"projection_rebuilds": "1", "note": "projection records discarded; sets/counters reconstructed once from retained records (spec ACCT-LOSS)"})
    # source loss (owner decision S1 R2): a's ORIGINAL_ROLL and the dependent decision/projection/receipt evidence are removed
    for fid, sl in TA.SOURCE_LOSS.items():
        log, delivered = sl["base"], sl["delivered"]
        b = prefix_state(log, delivered)          # Tier B: membership of the literal retained events
        ex = TA.SOURCE_LOSS_EXPECT
        check(b["qualified"] == ex["qualified"] and b["fit_eligible"] == ex["fit_eligible"], "%s: qualified/fit_eligible Tier A vs B" % fid)
        check(b["rolled"] == ex["rolled_evidenced"] and b["winners"] == ex["winners_evidenced"] and b["roll_loss"] == ex["roll_loss_evidenced"], "%s: roll evidence Tier A vs B" % fid)
        check(b["fit_checks"] == ex["counters"]["fit_checks"], "%s: fit_checks Tier A vs B" % fid)
        check(all(i in delivered for i in (1, 2, 3, 4, 5, 6)) and 7 not in delivered and not set(delivered) & {10, 11, 12, 13}, "%s: delivered record set" % fid)
        out[fid] = {
            "kind": "ACCT", "status": "COMPLETE", "tag": "OWNER_DECISION_S1_R2",
            "model_knowledge_status": "UNKNOWN", "affected_operations": "BLOCKED", "new_roll_or_search": "NONE",
            "reroll": "NONE", "frozen_decisions": [], "known_history_preserved": True,
            "lost_evidence": {"removed_records": sl["removed"], "removed_unit_roll": "a"},
            "event_dispositions": {str(i): "APPLIED" for i in delivered},
            "sets": {k: ex[k] for k in ("qualified", "fit_eligible", "rolled_evidenced", "winners_evidenced", "roll_loss_evidenced", "roll_state_unknown")},
            "unit_roll_state": ex["unit_roll_state"],
            "unknown_fields": ex["unknown_fields"],
            "counters": ex["counters"], "physical": {"physical_completed": "0"},
            "deferred": [], "input_rejection": None}
    # negative traces
    base = finals["P01/ACCT-BASE"]
    cd = {str(e[0]): "APPLIED" for e in TA.LOGS["P01/ACCT-BASE"]}
    cd["13"] = "CONFLICT"
    out["P01/NEG-CONTRADICT-01"] = {
        "kind": "ACCT", "status": "PARTIAL_DEFERRED", "model_knowledge_status": "UNKNOWN", "frozen_decisions": ["c"],
        "new_roll_or_search": "NONE", "last_writer_wins": False, "event_dispositions": cd,
        "sets": {"qualified": base["qualified"], "fit_eligible": base["fit_eligible"]},
        "counters": {"roll_attempts": "3"}, "deferred": ["sets." + k for k in ("rolled", "winners", "roll_loss", "accepted_decisions", "spacing_excluded", "protection_excluded", "pending")] + ["counters." + k for k in counters_of(base) if k != "roll_attempts"],
        "input_rejection": None, "tag": "DERIVED_PROSE"}
    rf = {str(i): "APPLIED" for i in (1, 2, 3, 5, 6, 7, 8, 9)}
    rf["4"] = "REJECTED(ORDER_VIOLATION)"
    out["P01/NEG-ROLL-FIRST-01"] = {
        "kind": "ACCT", "status": "PARTIAL_DEFERRED", "model_knowledge_status": "KNOWN",
        "sets": {"qualified": ["a", "b", "c"], "fit_eligible": ["a", "b", "c"], "rolled": ["b", "c"], "winners": ["b"], "roll_loss": ["c"]},
        "counters": {"roll_attempts": "2"}, "event_dispositions": rf, "frozen_decisions": [],
        "deferred": ["sets." + k for k in ("accepted_decisions", "spacing_excluded", "protection_excluded", "pending")],
        "input_rejection": None, "tag": "DERIVED_PROSE"}
    return out, finals



MODEL_SETS = ("qualified", "fit_eligible", "rolled", "winners", "accepted_decisions", "spacing_excluded",
              "protection_excluded", "roll_loss", "pending", "retired_protected", "model_success", "history_modified")
MODEL_COUNTERS = {"fit_checks": "fit_checks", "fit_eligible": "fit_eligible", "rolled": "rolled", "winners": "winners",
                  "spacing": "spacing_exclusions", "accepted": "accepted_decisions", "pending": "pending", "model_success": "model_success"}


def complete_model_state(golden, events):
    # Only full-log states: every defining fact delivered, no pending/deferred field.
    return {"applied_event_ids": sorted(e["event_id"] for e in events),
            "sets": {k: golden["sets"][k] for k in MODEL_SETS},
            "counters": dict({k: golden["counters"][v] for k, v in MODEL_COUNTERS.items()}, physical_completed="0"),
            "pending_fields": [], "knowledge": golden["model_knowledge_status"],
            "frozen_decisions": golden["frozen_decisions"]}


def state_digest(document):
    return {"status": "AVAILABLE", "sha256": hashlib.sha256((cj(document) + "\n").encode("utf-8")).hexdigest(),
            "serialization": "P01-CJSON-1"}


def digest_matches(document, expected_sha256):
    """Compare a supplied digest against canonical logical state bytes."""
    return state_digest(document)["sha256"] == expected_sha256


def partial_model_state(state, events, delivered):
    """Owner-approved P01-MODEL-STATE-1 projection of a prefix state."""
    by_index = {e["index"]: e["event_id"] for e in events}
    sets = {key: state[key] for key in MODEL_SETS}
    counter_sets = {"fit_eligible": "fit_eligible", "rolled": "rolled", "winners": "winners",
                    "spacing": "spacing_excluded", "accepted": "accepted_decisions",
                    "pending": "pending", "model_success": "model_success"}
    counters = {"fit_checks": state["fit_checks"]}
    for key, source in counter_sets.items():
        value = sets[source]
        counters[key] = "PENDING" if value == "PENDING" else str(len(value))
    counters["physical_completed"] = "0"
    pending_paths = ["sets." + key for key, value in sets.items() if value == "PENDING"]
    pending_paths += ["counters." + key for key, value in counters.items() if value == "PENDING"]
    return {"applied_event_ids": sorted(by_index[str(i)] for i in delivered),
            "sets": sets, "counters": counters, "pending_fields": sorted(pending_paths, key=lambda p: p.encode("utf-8")),
            "knowledge": "KNOWN", "frozen_decisions": []}


def blocked_digest():
    return {"status": "BLOCKED", "reason": "ACCT-RETIRE intermediate state.pending is explicitly deferred; a unique model-state document and digest cannot be derived without a lifecycle decision"}


def compare_fields(expected, observed, deferred):
    """Oracle contract utility, not a G-5 observer. Paths are dot-separated keys.

    Missing expected paths are withheld only by explicit deferred entries.
    PENDING is an ordinary, type-sensitive, evaluable value.
    """
    def leaves(value, prefix=""):
        if isinstance(value, dict) and value:
            for key, item in value.items():
                yield from leaves(item, prefix + "." + key if prefix else key)
        else:
            yield prefix, value
    missing = object()
    def get(path):
        value = observed
        for key in path.split("."):
            if not isinstance(value, dict) or key not in value:
                return missing
            value = value[key]
        return value
    checked, failed = [], []
    for path, want in (leaves(expected) if expected else []):
        if any(path == d or path.startswith(d + ".") for d in deferred):
            continue
        checked.append(path)
        got = get(path)
        if type(got) is not type(want) or got != want:
            failed.append(path)
    return {"verdict": "FAIL" if failed else "PASS" if checked else "UNKNOWN",
            "reason": "U-ALL-DEFERRED" if not checked else "COMPARISON",
            "evaluated": checked, "failed": failed, "excluded": list(deferred)}


def compare_late_checkpoints(expected, observed):
    """Contract self-check: associate state by ordered stage, never final state alone."""
    if len(observed) != 2 or [x.get("stage") for x in observed] != ["OPEN", "REVEAL"]:
        return {"verdict": "UNKNOWN", "reason": "U-INCOMPLETE"}
    fields = ("stage", "action", "finalized_accepted", "finalized_rejected", "pending", "population_complete", "unknown_edges")
    results = [compare_fields({key: want[key] for key in fields}, got, [])
               for want, got in zip(expected, observed)]
    return {"verdict": "FAIL" if any(r["verdict"] == "FAIL" for r in results) else "PASS", "checkpoints": results}


def prefix_states(finals):
    sched = load("schedules-v1.json")
    records = {r["fixture_id"]: r for r in load("inputs-v1.json")}
    final_goldens, _ = acct_goldens()
    out = {}
    n = 0
    for f in sched["families"]:
        fid = f["fixture_id"]
        if fid not in TA.LOGS:
            continue
        for s in f["schedules"]:
            if s["cut"] is None:
                continue
            k = int(s["cut"])
            delivered = [int(x["event"]) for x in s["steps"][:k]]
            st = prefix_state(fid, delivered)
            deferred_paths = [k for k, v in st.items() if isinstance(v, str) and v.startswith("DEFERRED:")]
            for path in deferred_paths:
                del st[path]
            out.setdefault(fid, {})[s["schedule_no"]] = {
                "cut": s["cut"], "delivered": [str(i) for i in delivered], "state": st,
                "deferred": deferred_paths,
                "state_sha256": blocked_digest(), "restored_state_sha256": blocked_digest(),
                "roundtrip_state": st, "roundtrip_deferred": deferred_paths,
                "final_state": {k: v for k, v in finals[fid].items() if k != "frac"},
                "final_state_sha256": blocked_digest(),
                "counters": {"qualified": str(len(st["qualified"])), "fit_checks": st["fit_checks"],
                             "fit_eligible": str(len(st["fit_eligible"])), "rolled": str(len(st["rolled"])),
                             "winners": str(len(st["winners"])), "roll_loss": str(len(st["roll_loss"])),
                             "roll_attempts": str(len(st["rolled"]))}}
            item = out[fid][s["schedule_no"]]
            model = complete_model_state(final_goldens[fid], records[fid]["events"])
            item["final_model_state"] = model
            item["final_state_sha256"] = state_digest(model)
            if k == len(s["steps"]):
                item["model_state"] = model
                item["state_sha256"] = state_digest(model)
                item["restored_state_sha256"] = state_digest(model)
            elif not deferred_paths:
                partial = partial_model_state(st, records[fid]["events"], delivered)
                item["model_state"] = partial
                item["state_sha256"] = state_digest(partial)
                item["restored_state_sha256"] = state_digest(partial)
            n += 1
        # the full-log prefix must equal the final state
    return out, n


# ===================================================================================== invalid-input expectations
CODES = ["JSON-SYNTAX", "DUP-KEY", "NONCANON", "NUMBER-TOKEN", "INT-FORMAT", "INT-BITS", "MISSING-KEY", "EXTRA-KEY", "ENUM-VALUE",
         "RAT-ZERO-DEN", "RAT-NEG-DEN", "RAT-UNREDUCED", "SCORE-RANGE", "TUPLE-ARITY", "ID-DUPLICATE", "ID-PREFIX-MIXED",
         "LABEL-DUPLICATE", "EDGE-SELF", "EDGE-DANGLING", "EDGE-DUPLICATE", "EDGE-UNKNOWN-CONFLICT", "POP-FLAG",
         "STAGE-NOT-MONOTONE", "CAND-OVERFLOW", "CAND-NULL-MISMATCH", "CAND-RANK-DUP", "CAND-SLOT-DUP",
         "ROLL-BEFORE-FIT-STATIC", "EVENT-ID-FORMAT"]


def invalid_expectations():
    check(len(CODES) == 29 and sorted(CODES) == sorted(TA.PRECEDENCE), "invalid code list vs precedence list")
    order = sorted(CODES, key=lambda c: hashlib.sha256(("P01-INVALID-1|" + c).encode()).hexdigest())
    exp = {}
    for i, code in enumerate(order, 1):
        fn = "I%03d" % i
        raw = open(os.path.join(TOOL, "invalid-inputs-v1", fn + ".json"), "rb").read()
        got = LR.classify(raw)
        check(got == code, "%s expected %s but the independent loader-rule engine reports %s" % (fn, code, got))
        exp[fn] = {"code": code, "precedence_rank": str(TA.PRECEDENCE.index(code) + 1)}
    # every pinned valid record must be accepted by the engine
    for r in load("inputs-v1.json"):
        c = LR.semantic(r)
        check(c is None, "valid record %s rejected by the engine with %s" % (r["fixture_id"], c))
    return {"precedence": TA.PRECEDENCE, "files": exp}


# ===================================================================================== negatives
def long_negatives():
    out = {}
    for ctl in ("N-OMIT-ENDPOINT", "N-LOCAL-CAP"):
        d = {}
        for kind in ("SCORE", "TIE"):
            for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024):
                base = long_b(kind, n, False)  # the wrong subject evaluates the BASE graph only (vertices 0..n)
                d["P01/LONG-%s-%d/EXT" % (kind, n)] = {"src": "LITERAL_SPEC", "evaluation_order": base["evaluation_order"],
                                                       "decisions": base["decisions"], "accepted": base["accepted"],
                                                       "spacing_rejected": base["spacing_rejected"],
                                                       "note": "vertex n+1 and its edge are absent from the output"}
        out[ctl] = d
    d = {}
    for kind in ("TIE",):
        for n in (1, 2, 3, 4, 8, 16, 64, 256, 1024):
            for ext in (False, True):
                top = n + 1 if ext else n
                order = [str(i) for i in range(0, top + 1)]
                dec, acc, rej = [], [], []
                for i in range(0, top + 1):
                    if i % 2 == 0:
                        dec.append(TA.D(str(i), TA.A)); acc.append(str(i))
                    else:
                        dec.append(TA.D(str(i), TA.RS, [str(i - 1)])); rej.append(str(i))
                key = lambda s: -int(s)
                d["P01/LONG-TIE-%d/%s" % (n, "EXT" if ext else "BASE")] = {
                    "src": "LITERAL_SPEC", "evaluation_order": order, "decisions": dec,
                    "accepted": sorted(acc, key=key), "spacing_rejected": sorted(rej, key=key),
                    "note": "equal totals visited in ascending label order (reversed ID order); even labels accepted"}
    out["N-TIE-ID-DESC"] = d
    out["N-ARRIVAL"] = {"P01/LONG-*": {"src": "LITERAL_SPEC", "rule": "evaluation_order equals the schedule's vertex_order; the correct order is descending label",
                                       "applies_where": "schedule.vertex_order != descending labels"}}
    return out


def negative_expectations(controls):
    out = {}
    long_n = long_negatives()
    for c in controls["controls"]:
        cid = c["id"]
        mf, mp = list(c["must_fail"]), list(c["must_pass"])
        final = False
        if cid in NG.FINAL_LISTS:
            mf, mp0 = NG.FINAL_LISTS[cid]
            mp = list(dict.fromkeys(["ASR-ACCEPT", "ASR-COMPLETE", "ASR-INPUT-UNCHANGED"] + mp0))
            final = True
        fx = {}
        fx.update(NG.NEG.get(cid, {}))
        fx.update(long_n.get(cid, {}))
        entry = {"identification": c["identification"], "must_fail": mf, "must_pass": mp, "must_fail_finalized": final,
                 "fixtures": fx}
        if cid in NG.OUTCOME:
            entry["outcome_classes"] = NG.OUTCOME[cid]
        check(bool(fx) or cid in NG.OUTCOME, "control %s has neither a signature nor an outcome-class definition" % cid)
        out[cid] = entry
    return out


# ===================================================================================== integrity (data only)
KMAP = {"QUALIFIED": "QUALIFIED", "FIT_PLAN_COMPLETE": "FIT", "ORIGINAL_ROLL": "ROLL", "CONFLICT_DECISION": "DECISION",
        "ACCEPTED_PENDING": "PROJ", "SPACING_EXCLUDED": "PROJ", "PROTECTION_EXCLUDED": "PROJ", "CERTIFICATE_INVALID": "CERT_INVALID",
        "APPLYING": "APPLYING", "UNCERTAIN": "UNCERTAIN", "QUARANTINED": "QUARANTINED", "RETIRED_PROTECTED": "RETIRED",
        "MODEL_PUBLICATION_RECEIPT": "RECEIPT", "MODEL_PLAYER_MODIFIED": "EDIT"}


def integrity(inputs, goldens, manifest, controls):
    recs = {r["fixture_id"]: r for r in inputs}
    check(set(goldens["fixtures"]) == set(recs), "golden fixture set differs from the 78 input records: %s" % sorted(set(recs) ^ set(goldens["fixtures"]))[:5])
    # accounting logs: the oracle's own event tables agree with the materialized events (facts only)
    for log, ev in TA.LOGS.items():
        mine = [(e[0], e[1], e[2]) for e in ev]
        theirs = [(int(e["index"]), KMAP[e["kind"]], e["unit"]) for e in recs[log]["events"]]
        check(mine == theirs, "%s: event (index,kind,unit) table differs from inputs-v1.json" % log)
        for e, r in zip(ev, recs[log]["events"]):
            if e[1] == "ROLL":
                check(r["payload"]["result"] == e[3]["result"], "%s: roll result of event %d" % (log, e[0]))
            if e[1] == "FIT":
                check(r["payload"]["fit_eligible"] == e[3]["eligible"] and len(r["payload"]["slots"]) == e[3]["checks"], "%s: fit facts of event %d" % (log, e[0]))
            if e[1] == "DECISION":
                p = r["payload"]
                got = {"accepted": sorted([t["v"] for t in p["trace"] if t["d"] == "ACCEPT"]),
                       "spacing": sorted([t["v"] for t in p["trace"] if t["d"] == "REJECT_SPACING"]),
                       "protection": sorted([t["v"] for t in p["trace"] if t["d"] == "REJECT_PROTECTED"])}
                for k in got:
                    check(got[k] == sorted(e[3][k]), "%s: decision %s" % (log, k))
    # graph goldens: decision-legality invariant against the input graph
    for fid, g in goldens["fixtures"].items():
        if g["kind"] not in ("GRAPH", "LONG", "LATE"):
            continue
        r = recs[fid]
        if g["kind"] == "LATE":
            snap = r["stages"][1]
            verts, edges, prot = snap["vertices"], snap["edges"], []
            gg = g["checkpoints"][1]
            totals = g["totals"]
        else:
            verts, edges, prot = r["vertices"], r["edges"], r["protected_sites"]
            gg = g
            totals = g["totals"]
        ids = {v["label"]: int(v["id"]["x"]) for v in verts}
        adj = {l: set() for l in ids}
        for e in edges:
            adj[e["u"]].add(e["v"]); adj[e["v"]].add(e["u"])
        pconf = {p["label"]: set(p["conflicts_with"]) for p in prot}
        winners = [v["label"] for v in verts if v["roll"] == "WIN" or r["family"] in ("LONG", "LATE")]
        winners = [w for w in winners if w in totals and totals[w] != "NOT_APPLICABLE"]
        order = gg["evaluation_order"]
        check(sorted(order) == sorted(winners), "%s: evaluation order is not the winner set" % fid)
        keyf = lambda l: (-parse_q(totals[l]), ids[l])
        check(order == sorted(order, key=keyf), "%s: evaluation order violates (-total, ID)" % fid)
        accepted = []
        decisions = {d["v"]: d for d in gg["decisions"]}
        check([d["v"] for d in gg["decisions"]] == order, "%s: decision sequence differs from the order" % fid)
        for v in order:
            pid = {q["label"]: int(q["id"]["x"]) for q in prot}
            wit_p = sorted([p for p, cs in pconf.items() if v in cs], key=lambda p: pid[p])
            wit_a = [a for a in accepted if a in adj[v]]
            d = decisions[v]
            if wit_p:
                check(d["d"] == TA.RP and d["witness"] == wit_p, "%s: %s protected decision" % (fid, v))
            elif wit_a:
                check(d["d"] == TA.RS and d["witness"] == sorted(wit_a, key=lambda l: ids[l]) and d["earliest_witness"] == wit_a[0], "%s: %s spacing decision %s vs %s" % (fid, v, d, wit_a))
            else:
                check(d["d"] == TA.A and d["witness"] == [], "%s: %s must be ACCEPT" % (fid, v))
            if d["d"] == TA.A:
                accepted.append(v)
        srt = lambda s: sorted(s, key=lambda l: ids[l])
        check(gg["accepted"] == srt(accepted), "%s: accepted set" % fid)
        check(gg["spacing_rejected"] == srt([v for v in order if decisions[v]["d"] == TA.RS]), "%s: spacing set" % fid)
        check(gg["protected_rejected"] == srt([v for v in order if decisions[v]["d"] == TA.RP]), "%s: protected set" % fid)
        # components in the golden equal the materialized selected tuples
        if g["kind"] == "GRAPH" and "components" in g:
            for v in verts:
                if v["label"] in g["components"]:
                    fit = [c for c in v["candidates"] if c["status"] == "FIT"]
                    sel = g.get("selected", {}).get(v["label"])
                    pick = [c for c in fit if sel is None or c["cand"] == sel["cand"]]
                    if pick:
                        cs = ["%s/%s" % (o["num"], o["den"]) for o in pick[0]["components"]]
                        check(cs == g["components"][v["label"]], "%s: components of %s" % (fid, v["label"]))
    # owner decision S1 R2: SOURCE goldens must agree with the corrected SOURCE schedules and detect N-MISSING-MEANS-EMPTY
    sched0 = load("schedules-v1.json")
    for fid, sl in TA.SOURCE_LOSS.items():
        fam = [f for f in sched0["families"] if f["fixture_id"] == fid]
        check(len(fam) == 1 and len(fam[0]["schedules"]) == 1, "%s: expected exactly one SOURCE schedule" % fid)
        steps = [int(x["event"]) for x in fam[0]["schedules"][0]["steps"]]
        check(steps == sl["delivered"], "%s: schedule events %s != hand-authored delivered set %s" % (fid, steps, sl["delivered"]))
        base_ev = recs[sl["base"]]["events"]
        gone = [e["index"] for e in base_ev if e["index"] not in {str(i) for i in steps}]
        check(gone == sl["removed"], "%s: removed records %s != %s" % (fid, gone, sl["removed"]))
        check(all(e["kind"] != "ORIGINAL_ROLL" or e["unit"] != "a" for e in base_ev if int(e["index"]) in steps), "%s: a's ORIGINAL_ROLL still delivered" % fid)
        check(all(any(int(e["index"]) == i for e in base_ev if e["kind"] in ("FIT_PLAN_COMPLETE", "QUALIFIED")) for i in (1, 2, 3, 4, 5, 6)) and all(i in steps for i in (1, 2, 3, 4, 5, 6)), "%s: FIT evidence not preserved" % fid)
        check(all(i in steps for i in (8, 9)), "%s: unrelated unit histories not preserved" % fid)
        g = goldens["fixtures"][fid]
        check(set(g["event_dispositions"]) == {str(i) for i in steps}, "%s: golden dispositions differ from the schedule" % fid)
        clean = NG.classify_missing_means_empty(g, {})
        check(clean == "OK", "%s: clean golden classified %s" % (fid, clean))
        mutants = {
            "a_unrolled": {"unit_roll_state": {"a": "NOT_ROLLED", "b": "WIN", "c": "LOSS"}},
            "new_roll": {"new_roll_or_search": "ROLL", "counters": dict(g["counters"], roll_evidenced="3")},
            "reroll": {"reroll": "A"},
        }
        for name, m in mutants.items():
            cls = NG.classify_missing_means_empty(g, m)
            check(cls == "REROLLED_AS_UNROLLED", "%s: mutant %s not detected (%s)" % (fid, name, cls))
        for obs in ({"sets": dict(g["sets"], roll_state_unknown=[])},
                    {"event_dispositions": dict(g["event_dispositions"], **{"7": "APPLIED"})}):
            check(NG.classify_missing_means_empty(g, obs) == "U-OUTCOME-UNMATCHED", "%s: unexpected evidence mislabeled" % fid)
    # manifest / controls consistency: finalized assertions are REQUIRED on every judgeable triple
    mode = {}
    for t in manifest["triples"]:
        mode[(t["subject"], t["case_no"], t["schedule_no"])] = {a["id"]: a["mode"] for a in t["assertions"]}
    for cid, e in goldens["negative_expectations"].items():
        c = [x for x in controls["controls"] if x["id"] == cid][0]
        for t in c["targets"]:
            for s in t["schedule_nos"]:
                m = mode[(cid, t["case_no"], s)]
                if e["must_fail"]:
                    check(any(m[a] == "REQUIRED" for a in e["must_fail"]), "%s: no REQUIRED must_fail on triple %s/%s" % (cid, t["case_no"], s))
    # schedule coverage of prefix states
    sched = load("schedules-v1.json")
    replays = sum(1 for f in sched["families"] if f["fixture_id"] in TA.LOGS for s in f["schedules"] if s["cut"] is not None)
    npfx = sum(len(v) for v in goldens["prefix_states"].values())
    check(replays == npfx == 1086, "prefix state coverage %d / %d replay sessions (expect 1086)" % (npfx, replays))
    # the full-log prefix must equal the final state for every log
    for f in sched["families"]:
        if f["fixture_id"] in TA.LOGS:
            fid = f["fixture_id"]
            last = [s for s in f["schedules"] if s["cut"] is not None and int(s["cut"]) == len(s["steps"])]
            for s in last:
                st = goldens["prefix_states"][fid][s["schedule_no"]]["state"]
                for k, v in goldens["fixtures"][fid]["sets"].items():
                    check(st[k] == v, "%s: full-log prefix field %s differs from final state" % (fid, k))


# ===================================================================================== assemble and write
def cj(v, ind=0):
    pad = "  " * ind
    if v is None: return "null"
    if v is True: return "true"
    if v is False: return "false"
    if isinstance(v, str): return json.dumps(v, ensure_ascii=True)
    if isinstance(v, list):
        return "[]" if not v else "[\n" + ",\n".join(pad + "  " + cj(x, ind + 1) for x in v) + "\n" + pad + "]"
    if isinstance(v, dict):
        return "{}" if not v else "{\n" + ",\n".join(pad + "  " + json.dumps(k, ensure_ascii=True) + ": " + cj(v[k], ind + 1) for k in sorted(v)) + "\n" + pad + "}"
    raise TypeError(type(v))


def main():
    if os.path.lexists(os.path.join(TOOL, "pins-v1.json")):
        raise SystemExit("Frozen bundle: Golden regeneration forbidden")
    inputs = load("inputs-v1.json")
    controls = load("controls-v1.json")
    manifest = load("expected-traces-v1.json")
    fixtures = {}
    fixtures.update(arith_goldens())
    fixtures.update(graph_goldens(inputs))
    fixtures.update(roll_goldens())
    fixtures.update(late_golden())
    fixtures.update(long_goldens())
    acct, finals = acct_goldens()
    fixtures.update(acct)
    pfx, n_pfx = prefix_states(finals)
    inv = invalid_expectations()
    neg = negative_expectations(controls)
    deferred = [{"fixture": fid, "fields": g["deferred"], "reason": "authoritative decision sets/counters not uniquely established"}
                for fid, g in fixtures.items() if g.get("deferred")]
    deferred += [{"fixture": fid, "schedule_no": no, "fields": ["state." + p for p in g["deferred"]],
                  "reason": "ACCT-RETIRE intermediate pending lifecycle not established"}
                 for fid, schedules in pfx.items() for no, g in schedules.items() if g["deferred"]]
    golden = {"schema": "P01-GOLDENS-2", "status": "CANDIDATE - not frozen, not pinned", "fixtures": fixtures,
              "replay_mapping": {"schema": "P01-REPLAY-MAPPING-1",
                  "sets": {"sets." + k: "state." + k for k in MODEL_SETS},
                  "counters": {"counters." + k: "fixtures.<fixture>.counters." + v for k, v in MODEL_COUNTERS.items()},
                  "physical_completed": "0", "knowledge": "model_knowledge_status",
                  "applied_event_ids": "inputs event_id of delivered schedule indices; bytewise sorted",
                  "supplemental_state_checks": ["projections_delivered", "certificate_invalid", "applying", "uncertain", "quarantined"],
                  "trace_state_values": {"REPLAY_CHECKPOINT": "state", "ROUNDTRIP": "roundtrip_state", "REPLAY_FINAL": "final_state"},
                  "serialization": "P01-CJSON-1; exact document under model_state/final_model_state, no added schema key"},
              "prefix_states": pfx, "invalid_expectations": inv, "negative_expectations": neg, "deferred": deferred}
    integrity(inputs, golden, manifest, controls)
    tags = {}
    for fid, g in fixtures.items():
        tags[g["tag"]] = tags.get(g["tag"], 0) + 1
    golden["provenance"] = {"fixture_tags": {k: str(v) for k, v in tags.items()}, "fixtures": str(len(fixtures)), "prefix_states": str(n_pfx),
                            "invalid_files": str(len(inv["files"])), "controls": str(len(neg)),
                            "cross_checks": "tier A vs tier B on arithmetic, small graphs (formula totals), LONG n<=4, accounting finals and 10 hand prefix states; decision-legality invariant on every graph golden; independent loader-rule engine on 29 invalid files and 78 valid records; manifest REQUIRED coverage of every control",
                            "problems_found": str(len(PROBLEMS))}
    if PROBLEMS:
        print("ORACLE CROSS-CHECK FAILED (%d)" % len(PROBLEMS))
        for p in PROBLEMS[:40]:
            print(" -", p)
        sys.exit(1)
    data = (cj(golden) + "\n").encode()
    with open(os.path.join(HERE, "goldens-v1.json"), "wb") as fh:
        fh.write(data)
    names = ["goldens-v1.json", "oracle.py", "tier_a.py", "negatives.py", "loader_rules.py", "test_corrections.py"]
    lines = ["%s  %s\n" % (hashlib.sha256(open(os.path.join(HERE, n), "rb").read()).hexdigest(), n) for n in sorted(names)]
    open(os.path.join(HERE, "CANDIDATE-GOLDEN-DIGESTS.sha256"), "w").write("".join(lines))
    print("goldens written: fixtures=%d prefix_states=%d invalid=%d controls=%d size=%.2f MiB" %
          (len(fixtures), n_pfx, len(inv["files"]), len(neg), len(data) / 2 ** 20))
    print("provenance tags:", tags)


if __name__ == "__main__":
    main()
