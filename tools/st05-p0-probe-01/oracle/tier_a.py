"""Tier A - HAND-AUTHORED literal expectations for ST-05 Probe 01 (G-3 candidate).

Every value below was transcribed by hand from the execution specification
(docs/design/ST05_STAGE0_P0_PROBE_01_EXECUTION_SPEC.md sections 3-4) or derived
by definition from its prose, and is tagged accordingly. Nothing here is produced
by, or compared against, any subject, mutant, runner or materializer output.

Conventions: rationals are canonical strings "num/den"; sets are listed in
canonical-ID ascending order; "witness" is the complete set of earlier-evaluated
ACCEPTED neighbours (or protected sites), "earliest" the first of them in
evaluation order.
"""

def D(v, d, w=(), earliest=None):
    return {"v": v, "d": d, "witness": list(w), "earliest_witness": earliest if earliest is not None else (w[0] if w else None)}

A, RS, RP = "ACCEPT", "REJECT_SPACING", "REJECT_PROTECTED"

# ----------------------------------------------------------------------------- arithmetic (spec 3.1 table)
ARITH = {  # label: (C, T, F, E), S
    "V0": (("0", "0", "0", "0"), "0/1"),
    "V1": (("100", "100", "100", "100"), "100/1"),
    "V2": (("133/2", "75", "75", "50"), "691/10"),
    "V3": (("40", "0", "0", "0"), "16/1"),
    "V4": (("30", "0", "0", "0"), "12/1"),
    "V5": (("30", "0", "0", "0"), "12/1"),
    "V6": (("0", "60", "0", "0"), "24/1"),
    "V7": (("0", "40", "0", "0"), "16/1"),
    "V8": (("0", "0", "50", "0"), "5/1"),
    "V9": (("0", "0", "50", "0"), "5/1"),
    "V10": (("0", "0", "0", "60"), "6/1"),
    "V11": (("0", "0", "0", "40"), "4/1"),
}
K4 = (("50001/1000", "50", "0", "0"), "100001/2500")  # spec K4 / NEAR-01: S = 40 + 1/2500

# ----------------------------------------------------------------------------- small graphs (spec 3.2)
GRAPHS = {
    "P01/CAV-01": {
        "totals": {"U": "40/1", "V": "40/1"},
        "evaluation_order": ["U", "V"],
        "decisions": [D("U", A), D("V", RS, ["U"])],
        "accepted": ["U"], "spacing_rejected": ["V"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/EQUAL-3-C": {  # ids K2=-2 < K3=-1 < K1=0
        "totals": {"K1": "40/1", "K2": "40/1", "K3": "40/1"},
        "evaluation_order": ["K2", "K3", "K1"],
        "decisions": [D("K2", A), D("K3", RS, ["K2"]), D("K1", RS, ["K2"])],
        "accepted": ["K2"], "spacing_rejected": ["K3", "K1"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/EQUAL-3-T": {  # ids K1=-2 < K3=-1 < K2=0
        "totals": {"K1": "40/1", "K2": "40/1", "K3": "40/1"},
        "evaluation_order": ["K1", "K3", "K2"],
        "decisions": [D("K1", A), D("K3", RS, ["K1"]), D("K2", RS, ["K1"])],
        "accepted": ["K1"], "spacing_rejected": ["K3", "K2"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/NEAR-01": {
        "totals": {"P": "100001/2500", "Q": "40/1"},
        "evaluation_order": ["P", "Q"],
        "decisions": [D("P", A), D("Q", RS, ["P"])],
        "accepted": ["P"], "spacing_rejected": ["Q"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/BIG-NEAR-01": {  # S(P) = 40 + 1/(5*2^79)
        "totals": {"P": "FORMULA:40+1/(5*2^79)", "Q": "40/1"},
        "evaluation_order": ["P", "Q"],
        "decisions": [D("P", A), D("Q", RS, ["P"])],
        "accepted": ["P"], "spacing_rejected": ["Q"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/ID-ORDER-01": {  # ids a=-2, b=-1, c=2, d=10, all S=40
        "totals": {"a": "40/1", "b": "40/1", "c": "40/1", "d": "40/1"},
        "evaluation_order": ["a", "b", "c", "d"],
        "decisions": [D("a", A), D("b", RS, ["a"]), D("c", RS, ["a"]), D("d", RS, ["a"])],
        "accepted": ["a"], "spacing_rejected": ["b", "c", "d"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/CHAIN-3": {  # ids A=0 B=1 C=2
        "totals": {"A": "40/1", "B": "60/1", "C": "80/1"},
        "evaluation_order": ["C", "B", "A"],
        "decisions": [D("C", A), D("B", RS, ["C"]), D("A", A)],
        "accepted": ["A", "C"], "spacing_rejected": ["B"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/CHAIN-ID-01": {  # ids B=-1 A=0 C=1
        "totals": {"A": "40/1", "B": "60/1", "C": "80/1"},
        "evaluation_order": ["C", "B", "A"],
        "decisions": [D("C", A), D("B", RS, ["C"]), D("A", A)],
        "accepted": ["A", "C"], "spacing_rejected": ["B"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
    "P01/CHAIN-PROT-01": {
        "totals": {"A": "40/1", "B": "60/1", "C": "80/1"},
        "evaluation_order": ["C", "B", "A"],
        "decisions": [D("C", RP, ["P"]), D("B", A), D("A", RS, ["B"])],
        "accepted": ["B"], "spacing_rejected": ["A"], "protected_rejected": ["C"],
        "protected_input": ["P"],
        "tag": "LITERAL_SPEC"},
    "P01/CYCLE-01": {  # labels = ids 0..3, totals 10..40
        "totals": {"0": "10/1", "1": "20/1", "2": "30/1", "3": "40/1"},
        "evaluation_order": ["3", "2", "1", "0"],
        "decisions": [D("3", A), D("2", RS, ["3"]), D("1", A), D("0", RS, ["1", "3"], "3")],
        "accepted": ["1", "3"], "spacing_rejected": ["0", "2"], "protected_rejected": [],
        "tag": "LITERAL_SPEC"},
}

# CAND-01 (spec CAND-01): both candidates S=40, seed ranks c1=0 < c0=1
CAND01 = {
    "candidate_totals": {"U": {"c0": "40/1", "c1": "40/1"}},
    "candidate_order": {"U": ["c1", "c0"]},
    "selected": {"U": {"cand": "c1", "slot": "2", "seed_rank": "0"}},
    "totals": {"U": "40/1"},
    "components": {"U": ["0/1", "100/1", "0/1", "0/1"]},
    "evaluation_order": ["U"],
    "decisions": [D("U", A)],
    "accepted": ["U"], "spacing_rejected": [], "protected_rejected": [],
    "tag": "LITERAL_SPEC",
}

# ROLL-01 / NOFIT-01 (spec 4.1): a=80 (slot 8), b=40 (slot 1), c=20 (slot 3); rolls a WIN, b WIN, c LOSS
ROLL_COMMON = {
    "totals": {"a": "80/1", "b": "40/1", "c": "20/1"},
    "components": {"a": ["100/1", "100/1", "0/1", "0/1"], "b": ["100/1", "0/1", "0/1", "0/1"], "c": ["50/1", "0/1", "0/1", "0/1"]},
    "selected": {"a": {"cand": "s8", "slot": "8", "seed_rank": "8"}, "b": {"cand": "s1", "slot": "1", "seed_rank": "1"},
                 "c": {"cand": "s3", "slot": "3", "seed_rank": "3"}},
    "candidate_order": {"a": ["s8"], "b": ["s1"], "c": ["s3"]},
    "evaluation_order": ["a", "b"],
    "decisions": [D("a", A), D("b", RS, ["a"])],
    "accepted": ["a"], "spacing_rejected": ["b"], "protected_rejected": [], "roll_loss": ["c"],
    "tag": "LITERAL_SPEC",
}

# LATE-01 (spec LATE-01): stage OPEN defers, stage REVEAL finalizes D,C,B,A
LATE = {
    "totals": {"A": "40/1", "B": "60/1", "C": "80/1", "D": "100/1"},
    "checkpoints": [
        {"stage": "OPEN", "action": "DEFER_GRAPH", "finalized_accepted": [], "finalized_rejected": [],
         "pending": ["B", "A", "C", "D"]},
        {"stage": "REVEAL", "action": "FINALIZE", "evaluation_order": ["D", "C", "B", "A"],
         "decisions": [D("D", A), D("C", RS, ["D"]), D("B", A), D("A", RS, ["B"])],
         "accepted": ["B", "D"], "spacing_rejected": ["A", "C"], "protected_rejected": [], "pending": []}],
    "tag": "LITERAL_SPEC",
}

# ----------------------------------------------------------------------------- long chains n = 1..4 (hand-derived by parity)
# notation: "3:A" accepted, "2:R3" rejected with witness 3.  Sets already in canonical-ID order.
LONG_HAND = {
    ("SCORE", 1, "BASE"): dict(totals=["0/1", "50/1"], dec="1:A 0:R1", acc=["1"], rej=["0"]),
    ("SCORE", 2, "BASE"): dict(totals=["0/1", "100/3", "200/3"], dec="2:A 1:R2 0:A", acc=["0", "2"], rej=["1"]),
    ("SCORE", 3, "BASE"): dict(totals=["0/1", "25/1", "50/1", "75/1"], dec="3:A 2:R3 1:A 0:R1", acc=["1", "3"], rej=["0", "2"]),
    ("SCORE", 4, "BASE"): dict(totals=["0/1", "20/1", "40/1", "60/1", "80/1"], dec="4:A 3:R4 2:A 1:R2 0:A", acc=["0", "2", "4"], rej=["1", "3"]),
    ("SCORE", 1, "EXT"): dict(totals=["0/1", "50/1", "100/1"], dec="2:A 1:R2 0:A", acc=["0", "2"], rej=["1"]),
    ("SCORE", 2, "EXT"): dict(totals=["0/1", "100/3", "200/3", "100/1"], dec="3:A 2:R3 1:A 0:R1", acc=["1", "3"], rej=["0", "2"]),
    ("SCORE", 3, "EXT"): dict(totals=["0/1", "25/1", "50/1", "75/1", "100/1"], dec="4:A 3:R4 2:A 1:R2 0:A", acc=["0", "2", "4"], rej=["1", "3"]),
    ("SCORE", 4, "EXT"): dict(totals=["0/1", "20/1", "40/1", "60/1", "80/1", "100/1"], dec="5:A 4:R5 3:A 2:R3 1:A 0:R1", acc=["1", "3", "5"], rej=["0", "2", "4"]),
    # TIE: all S = 40, ids -i, so priority = labels descending; ID-ascending sets list labels descending
    ("TIE", 1, "BASE"): dict(totals=["40/1"] * 2, dec="1:A 0:R1", acc=["1"], rej=["0"]),
    ("TIE", 2, "BASE"): dict(totals=["40/1"] * 3, dec="2:A 1:R2 0:A", acc=["2", "0"], rej=["1"]),
    ("TIE", 3, "BASE"): dict(totals=["40/1"] * 4, dec="3:A 2:R3 1:A 0:R1", acc=["3", "1"], rej=["2", "0"]),
    ("TIE", 4, "BASE"): dict(totals=["40/1"] * 5, dec="4:A 3:R4 2:A 1:R2 0:A", acc=["4", "2", "0"], rej=["3", "1"]),
    ("TIE", 1, "EXT"): dict(totals=["40/1"] * 3, dec="2:A 1:R2 0:A", acc=["2", "0"], rej=["1"]),
    ("TIE", 2, "EXT"): dict(totals=["40/1"] * 4, dec="3:A 2:R3 1:A 0:R1", acc=["3", "1"], rej=["2", "0"]),
    ("TIE", 3, "EXT"): dict(totals=["40/1"] * 5, dec="4:A 3:R4 2:A 1:R2 0:A", acc=["4", "2", "0"], rej=["3", "1"]),
    ("TIE", 4, "EXT"): dict(totals=["40/1"] * 6, dec="5:A 4:R5 3:A 2:R3 1:A 0:R1", acc=["5", "3", "1"], rej=["4", "2", "0"]),
}

# ----------------------------------------------------------------------------- accounting logs (spec 4.2 table)
# event tuples: (index, kind, unit, facts). kinds use the oracle's own vocabulary.
def _base(units, rolls, checks, dec, with_d=False):
    ev, i = [], 1
    for u in units:
        ev.append((i, "QUALIFIED", u, {})); i += 1
    if with_d:
        ev.append((i, "QUALIFIED", "d", {})); i += 1
    for u in units:
        ev.append((i, "FIT", u, {"eligible": True, "checks": checks})); i += 1
    if with_d:
        ev.append((i, "FIT", "d", {"eligible": False, "checks": 8})); i += 1
    for u in units:
        ev.append((i, "ROLL", u, {"result": rolls[u]})); i += 1
    ev.append((i, "DECISION", None, dec)); i += 1
    return ev, i

_ROLLS = {"a": "WIN", "b": "WIN", "c": "LOSS"}
_DEC = {"accepted": ["a"], "spacing": ["b"], "protection": [], "pending": ["a"]}

def _roll_log(variant):
    ev, i = _base(["a", "b", "c"], _ROLLS, 8, _DEC, with_d=(variant == "NOFIT"))
    ev.append((i, "PROJ", "a", {"kind": "ACCEPTED_PENDING"})); i += 1
    ev.append((i, "PROJ", "b", {"kind": "SPACING_EXCLUDED"})); i += 1
    tails = {"INVALID": ["CERT_INVALID"], "RETIRE": ["APPLYING", "UNCERTAIN", "QUARANTINED", "RETIRED"],
             "MODEL-SUCCESS": ["RECEIPT"], "MODEL-EDIT": ["RECEIPT", "EDIT"]}.get(variant, [])
    for k in tails:
        ev.append((i, k, "a", {})); i += 1
    return ev

def _prot_log():
    rolls = {"B": "WIN", "A": "WIN", "C": "WIN"}
    dec = {"accepted": ["B"], "spacing": ["A"], "protection": ["C"], "pending": ["B"]}
    ev, i = _base(["B", "A", "C"], rolls, 1, dec)
    ev.append((i, "PROJ", "B", {"kind": "ACCEPTED_PENDING"})); i += 1
    ev.append((i, "PROJ", "A", {"kind": "SPACING_EXCLUDED"})); i += 1
    ev.append((i, "PROJ", "C", {"kind": "PROTECTION_EXCLUDED"}))
    return ev

LOGS = {
    "P01/ACCT-BASE": _roll_log("BASE"),
    "P01/ACCT-INVALID": _roll_log("INVALID"),
    "P01/ACCT-RETIRE": _roll_log("RETIRE"),
    "P01/ACCT-MODEL-SUCCESS": _roll_log("MODEL-SUCCESS"),
    "P01/ACCT-MODEL-EDIT": _roll_log("MODEL-EDIT"),
    "P01/ACCT-PROTECTED": _prot_log(),
    "P01/NOFIT-01/LOG": _roll_log("NOFIT"),
}
LOG_UNIT_ORDER = {  # canonical-ID ascending
    "P01/ACCT-PROTECTED": ["B", "A", "C"],
}

# hand-written final accounting states (spec 4.2 rows). Sets in canonical-ID order.
FINAL = {
    "P01/ACCT-BASE": dict(qualified=["a", "b", "c"], fit_eligible=["a", "b", "c"], rolled=["a", "b", "c"], winners=["a", "b"],
                          roll_loss=["c"], spacing_excluded=["b"], protection_excluded=[], accepted_decisions=["a"], pending=["a"],
                          retired_protected=[], certificate_invalid=[], model_success=[], history_modified=[],
                          fit_checks="24", frac=("2", "3", "1", "3")),
    "P01/ACCT-INVALID": dict(qualified=["a", "b", "c"], fit_eligible=["a", "b", "c"], rolled=["a", "b", "c"], winners=["a", "b"],
                             roll_loss=["c"], spacing_excluded=["b"], protection_excluded=[], accepted_decisions=["a"], pending=["a"],
                             retired_protected=[], certificate_invalid=["a"], model_success=[], history_modified=[],
                             fit_checks="24", frac=("2", "3", "1", "3")),
    "P01/ACCT-RETIRE": dict(qualified=["a", "b", "c"], fit_eligible=["a", "b", "c"], rolled=["a", "b", "c"], winners=["a", "b"],
                            roll_loss=["c"], spacing_excluded=["b"], protection_excluded=[], accepted_decisions=["a"], pending=[],
                            retired_protected=["a"], certificate_invalid=[], model_success=[], history_modified=[],
                            fit_checks="24", frac=("2", "3", "1", "3")),
    "P01/ACCT-MODEL-SUCCESS": dict(qualified=["a", "b", "c"], fit_eligible=["a", "b", "c"], rolled=["a", "b", "c"], winners=["a", "b"],
                                   roll_loss=["c"], spacing_excluded=["b"], protection_excluded=[], accepted_decisions=["a"], pending=[],
                                   retired_protected=[], certificate_invalid=[], model_success=["a"], history_modified=[],
                                   fit_checks="24", frac=("2", "3", "1", "3")),
    "P01/ACCT-MODEL-EDIT": dict(qualified=["a", "b", "c"], fit_eligible=["a", "b", "c"], rolled=["a", "b", "c"], winners=["a", "b"],
                                roll_loss=["c"], spacing_excluded=["b"], protection_excluded=[], accepted_decisions=["a"], pending=[],
                                retired_protected=[], certificate_invalid=[], model_success=["a"], history_modified=["a"],
                                fit_checks="24", frac=("2", "3", "1", "3")),
    "P01/ACCT-PROTECTED": dict(qualified=["B", "A", "C"], fit_eligible=["B", "A", "C"], rolled=["B", "A", "C"], winners=["B", "A", "C"],
                               roll_loss=[], spacing_excluded=["A"], protection_excluded=["C"], accepted_decisions=["B"], pending=["B"],
                               retired_protected=[], certificate_invalid=[], model_success=[], history_modified=[],
                               fit_checks="3", frac=("3", "3", "1", "3")),
    "P01/NOFIT-01/LOG": dict(qualified=["a", "b", "c", "d"], fit_eligible=["a", "b", "c"], rolled=["a", "b", "c"], winners=["a", "b"],
                             roll_loss=["c"], spacing_excluded=["b"], protection_excluded=[], accepted_decisions=["a"], pending=["a"],
                             retired_protected=[], certificate_invalid=[], model_success=[], history_modified=[],
                             fit_checks="32", frac=("2", "3", "1", "3")),
}

# hand-written prefix states for selected prefixes (cross-check of the Tier B derivation).
# key: (log, tuple(delivered indices)) -> subset of fields
PREFIX_HAND = {
    ("P01/ACCT-BASE", ()): dict(qualified=[], rolled=[], accepted_decisions="PENDING", pending="PENDING"),
    ("P01/ACCT-BASE", (1, 4, 7)): dict(qualified=["a"], fit_eligible=["a"], rolled=["a"], winners=["a"], roll_loss=[],
                                       accepted_decisions="PENDING", spacing_excluded="PENDING", pending="PENDING"),
    ("P01/ACCT-BASE", (1, 2, 3, 4, 5, 6, 7, 8, 9)): dict(rolled=["a", "b", "c"], winners=["a", "b"], roll_loss=["c"],
                                                         accepted_decisions="PENDING", pending="PENDING"),
    ("P01/ACCT-BASE", (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)): dict(accepted_decisions=["a"], spacing_excluded=["b"], pending=["a"]),
    ("P01/ACCT-RETIRE", tuple(range(1, 14))): dict(accepted_decisions=["a"], pending="DEFERRED:INTERMEDIATE_LIFECYCLE", retired_protected=[]),
    ("P01/ACCT-RETIRE", tuple(range(1, 17))): dict(pending=[], retired_protected=["a"]),
    ("P01/ACCT-MODEL-SUCCESS", tuple(range(1, 14))): dict(pending=[], model_success=["a"]),
    ("P01/ACCT-MODEL-SUCCESS", tuple(range(1, 13))): dict(pending=["a"], model_success=[]),
    ("P01/ACCT-MODEL-EDIT", tuple(range(1, 15))): dict(model_success=["a"], history_modified=["a"], pending=[]),
    ("P01/ACCT-INVALID", tuple(range(1, 14))): dict(certificate_invalid=["a"], pending=["a"]),
}

# ----------------------------------------------------------------------------- invalid-input precedence (plan 4.7, rev. 4)
PRECEDENCE = [
    "JSON-SYNTAX", "DUP-KEY", "NUMBER-TOKEN", "NONCANON",
    "ID-PREFIX-MIXED", "MISSING-KEY", "EXTRA-KEY", "ENUM-VALUE",
    "INT-FORMAT", "INT-BITS",
    "RAT-ZERO-DEN", "RAT-NEG-DEN", "RAT-UNREDUCED", "SCORE-RANGE", "TUPLE-ARITY",
    "ID-DUPLICATE", "LABEL-DUPLICATE", "EDGE-SELF", "EDGE-DANGLING", "EDGE-DUPLICATE", "EDGE-UNKNOWN-CONFLICT",
    "POP-FLAG", "STAGE-NOT-MONOTONE",
    "CAND-OVERFLOW", "CAND-SLOT-DUP", "CAND-RANK-DUP", "CAND-NULL-MISMATCH", "ROLL-BEFORE-FIT-STATIC",
    "EVENT-ID-FORMAT",
]

# ----------------------------------------------------------------------------- component tuples (spec 3.1-3.2), hand-copied
CHAIN_T = {"A": ("100", "0", "0", "0"), "B": ("100", "50", "0", "0"), "C": ("100", "100", "0", "0")}
TUPLES = {
    "P01/CAV-01": {"U": ("0", "100", "0", "0"), "V": ("100", "0", "0", "0")},
    "P01/EQUAL-3-C": {"K1": ("100", "0", "0", "0"), "K2": ("0", "100", "0", "0"), "K3": ("50", "50", "0", "0")},
    "P01/EQUAL-3-T": {"K1": ("100", "0", "0", "0"), "K2": ("0", "100", "0", "0"), "K3": ("50", "50", "0", "0")},
    "P01/NEAR-01": {"P": ("50001/1000", "50", "0", "0"), "Q": ("50", "50", "0", "0")},
    "P01/BIG-NEAR-01": {"P": ("FORMULA:50+1/2^80", "50", "0", "0"), "Q": ("50", "50", "0", "0")},
    "P01/ID-ORDER-01": {k: ("100", "0", "0", "0") for k in "abcd"},
    "P01/CHAIN-3": dict(CHAIN_T),
    "P01/CHAIN-ID-01": dict(CHAIN_T),
    "P01/CHAIN-PROT-01": dict(CHAIN_T),
    "P01/CYCLE-01": {"0": ("10",) * 4, "1": ("20",) * 4, "2": ("30",) * 4, "3": ("40",) * 4},
}
LATE_TUPLES = {"A": CHAIN_T["A"], "B": CHAIN_T["B"], "C": CHAIN_T["C"], "D": ("100",) * 4}
