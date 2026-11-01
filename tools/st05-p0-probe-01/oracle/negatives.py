"""Hand-authored wrong-output descriptions for the negative controls (G-3 candidate).

Each entry lists only the fields in which the planted-defect subject DIFFERS from the
correct golden. "src": LITERAL_SPEC = stated in the execution specification;
DERIVED_BY_DEFINITION = follows from the control's defined defect applied to the
fixture; OUTCOME_CLASS controls carry class definitions instead of literals.
Sets in canonical-ID order. Wrong witnesses may cite rejected vertices (that is the defect).
"""
from tier_a import D, A, RS, RP

def L(src, **kw):
    kw["src"] = src
    return kw

SPEC, DEF = "LITERAL_SPEC", "DERIVED_BY_DEFINITION"

NEG = {
    "N-INNER-WEIGHT": {"P01/ARITH-V2": L(DEF, components=["69/1", "75/1", "75/1", "50/1"], total="701/10"),
                       "P01/ARITH-V3": L(SPEC, components=["30/1", "0/1", "0/1", "0/1"], total="12/1"),
                       "P01/ARITH-V4": L(SPEC, components=["40/1", "0/1", "0/1", "0/1"], total="16/1")},
    "N-EARLY-FLOOR": {"P01/ARITH-V2": L(SPEC, components=["66/1", "75/1", "75/1", "50/1"], total="689/10"),
                      "P01/FIXED-01#K4": L(DEF, components=["50/1", "50/1", "0/1", "0/1"], total="40/1")},
    "N-RELATIVE": {"P01/FIXED-01#1": L(SPEC, totals={"V2": "0/1"}),
                   "P01/FIXED-01#3": L(DEF, totals={"K4": "0/1", "V2": "100/1"}),
                   "P01/FIXED-01#2": L(DEF, coincides=True)},
    "N-CAND-COMPONENT": {"P01/CAND-01": L(SPEC, candidate_order={"U": ["c0", "c1"]}, selected={"U": {"cand": "c0", "slot": "1", "seed_rank": "1"}})},
    "N-CAND-ARRIVAL": {"P01/CAND-01": L(SPEC, candidate_order={"U": ["c0", "c1"]}, selected={"U": {"cand": "c0", "slot": "1", "seed_rank": "1"}},
                                         only_on_candidate_delivery=["c0", "c1"])},
    "N-CROSS-C-FIRST": {"P01/CAV-01": L(SPEC, evaluation_order=["V", "U"], decisions=[D("V", A), D("U", RS, ["V"])],
                                         accepted=["V"], spacing_rejected=["U"])},
    "N-CROSS-SEED": {"P01/CAV-01": L(SPEC, evaluation_order=["V", "U"], decisions=[D("V", A), D("U", RS, ["V"])],
                                      accepted=["V"], spacing_rejected=["U"])},
    "N-C-FIRST": {"P01/EQUAL-3-C": L(SPEC, evaluation_order=["K1", "K3", "K2"],
                                      decisions=[D("K1", A), D("K3", RS, ["K1"]), D("K2", RS, ["K1"])],
                                      accepted=["K1"], spacing_rejected=["K2", "K3"])},
    "N-T-FIRST": {"P01/EQUAL-3-T": L(SPEC, evaluation_order=["K2", "K3", "K1"],
                                      decisions=[D("K2", A), D("K3", RS, ["K2"]), D("K1", RS, ["K2"])],
                                      accepted=["K2"], spacing_rejected=["K1", "K3"])},
    "N-ROUND-2": {"P01/NEAR-01": L(SPEC, totals={"P": "40/1", "Q": "40/1"}, evaluation_order=["Q", "P"],
                                    decisions=[D("Q", A), D("P", RS, ["Q"])], accepted=["Q"], spacing_rejected=["P"]),
                  "P01/BIG-NEAR-01": L(DEF, totals={"P": "40/1", "Q": "40/1"}, evaluation_order=["Q", "P"],
                                        decisions=[D("Q", A), D("P", RS, ["Q"])], accepted=["Q"], spacing_rejected=["P"])},
    "N-FLOAT64": {"P01/BIG-NEAR-01": L(SPEC, components={"P": ["50/1", "50/1", "0/1", "0/1"]}, totals={"P": "40/1", "Q": "40/1"},
                                        evaluation_order=["Q", "P"], decisions=[D("Q", A), D("P", RS, ["Q"])],
                                        accepted=["Q"], spacing_rejected=["P"])},
    "N-ID-STRING": {"P01/ID-ORDER-01": L(SPEC, evaluation_order=["b", "a", "d", "c"],
                                          decisions=[D("b", A), D("a", RS, ["b"]), D("d", RS, ["b"]), D("c", RS, ["b"])],
                                          accepted=["b"], spacing_rejected=["a", "c", "d"])},
    "N-REJECTED-VETO": {
        "P01/CHAIN-3": L(SPEC, decisions=[D("C", A), D("B", RS, ["C"]), D("A", RS, ["B"])], accepted=["C"], spacing_rejected=["A", "B"]),
        "P01/CHAIN-ID-01": L(SPEC, decisions=[D("C", A), D("B", RS, ["C"]), D("A", RS, ["B"])], accepted=["C"], spacing_rejected=["B", "A"]),
        "P01/CHAIN-PROT-01": L(SPEC, decisions=[D("C", RP, ["P"]), D("B", RS, ["C"]), D("A", RS, ["B"])], accepted=[],
                               spacing_rejected=["B", "A"], protected_rejected=["C"]),
        "P01/CYCLE-01": L(SPEC, decisions=[D("3", A), D("2", RS, ["3"]), D("1", RS, ["2"]), D("0", RS, ["1", "3"], "3")],
                          accepted=["3"], spacing_rejected=["0", "1", "2"])},
    "N-ID-FIRST": {
        "P01/CHAIN-ID-01": L(SPEC, evaluation_order=["B", "A", "C"], decisions=[D("B", A), D("A", RS, ["B"]), D("C", RS, ["B"])],
                             accepted=["B"], spacing_rejected=["A", "C"]),
        "P01/CHAIN-3": L(SPEC, evaluation_order=["A", "B", "C"], decisions=[D("A", A), D("B", RS, ["A"]), D("C", A)],
                         accepted=["A", "C"], spacing_rejected=["B"], note="same accepted set; only the order differs"),
        "P01/CYCLE-01": L(SPEC, evaluation_order=["0", "1", "2", "3"],
                          decisions=[D("0", A), D("1", RS, ["0"]), D("2", A), D("3", RS, ["0", "2"], "0")],
                          accepted=["0", "2"], spacing_rejected=["1", "3"])},
    "N-PROTECTION-IGNORE": {"P01/CHAIN-PROT-01": L(SPEC, decisions=[D("C", A), D("B", RS, ["C"]), D("A", A)],
                                                    accepted=["A", "C"], spacing_rejected=["B"], protected_rejected=[])},
    "N-UNKNOWN-EMPTY": {"P01/LATE-01": L(SPEC, at_stage="OPEN", action="FINALIZE", evaluation_order=["D", "C", "B", "A"],
                                          decisions=[D("D", A), D("C", A), D("B", RS, ["C"]), D("A", A)],
                                          accepted=["A", "C", "D"], spacing_rejected=["B"], pending=[])},
    "N-QUALIFIED-DENOM": {"P01/NOFIT-01": L(SPEC, fractions={"winning": {"n": "2", "d": "4"}}),
                          "P01/NOFIT-01/LOG": L(SPEC, fractions={"winning": {"n": "2", "d": "4"}})},
    "N-ROLL-FIRST": {"P01/NEG-ROLL-FIRST-01": L(SPEC, rolled=["a", "b", "c"], event_dispositions={"4": "APPLIED"}, roll_attempts="3")},
    "N-ACCEPT-IS-COMPLETE": {"P01/ACCT-BASE": L(SPEC, physical_completed="1")},
    "N-POSTROLL-NOFIT": {"P01/ACCT-INVALID": L(SPEC, counters={"rolled": "2"}, fractions={"winning": {"n": "2", "d": "2"}})},
    "N-REROLL": {"P01/ACCT-RETIRE": L(SPEC, counters={"roll_attempts": "4"})},
    "N-IGNORE-P": {"P01/ACCT-PROTECTED": L(SPEC, accepted_decisions=["A", "C"], spacing_excluded=["B"], protection_excluded=[], pending=["A", "C"])},
    "N-LOGICAL-IS-PHYSICAL": {"P01/ACCT-MODEL-SUCCESS": L(SPEC, physical_completed="1")},
    "N-ADDITIVE": {"P01/ACCT-DUP-LOG": L(SPEC, counters={"fit_eligible": "6", "rolled": "6", "winners": "4", "spacing_exclusions": "2", "accepted_decisions": "2"}),
                   "P01/ACCT-DUP-RECEIPT": L(SPEC, counters={"model_success": "2"})},
}

# finalized assertion lists for the six accounting controls whose G-2 lists were provisional
FINAL_LISTS = {
    "N-ACCEPT-IS-COMPLETE": (["ASR-NOPHYS"], ["ASR-COUNTERS", "ASR-ACCT-SETS"]),
    "N-POSTROLL-NOFIT": (["ASR-COUNTERS"], ["ASR-ACCT-SETS", "ASR-NOPHYS", "ASR-STATUS"]),
    "N-REROLL": (["ASR-NOEXTRA", "ASR-COUNTERS"], ["ASR-NOPHYS"]),
    "N-IGNORE-P": (["ASR-ACCT-SETS", "ASR-COUNTERS"], ["ASR-NOPHYS"]),
    "N-LOGICAL-IS-PHYSICAL": (["ASR-NOPHYS"], ["ASR-COUNTERS", "ASR-ACCT-SETS"]),
    "N-ERASE-HISTORY": (["ASR-ACCT-SETS", "ASR-COUNTERS"], ["ASR-NOPHYS"]),
}

# outcome-class definitions (qualitative controls); anything outside the listed classes is UNKNOWN
OUTCOME = {
    "N-FIXED64": {"fixtures": ["P01/BIG-NEAR-01"], "classes": {
        "UA": "UNSUPPORTED_ARITHMETIC record present, no DECISION records, END present (property verdict UNKNOWN, detector PASS)",
        "WR": "completed with totals/order/sets differing from the golden (silent wrap or rounding; detector PASS)",
        "OK": "completed equal to the golden => D-UNDETECTED (cannot occur without exact arithmetic)"},
        "static_precondition": "mutant source uses java long only; the exact S difference 1/(5*2^79) needs a denominator > 2^63"},
    "N-NINTH": {"fixtures": ["P01/NOFIT-01", "P01/NOFIT-01/LOG"], "classes": {
        "COUNTER_EXCESS": "counters.fit_checks greater than the golden fit_checks (32), OR unit d appears in fit_eligible/rolled"}},
    "N-ERASE-HISTORY": {"fixtures": ["P01/ACCT-MODEL-EDIT"], "classes": {
        "MODELED_SUCCESS_ZERO": "model_success is empty after the modeled player edit",
        "RETIRED_FOR_EDIT": "unit a appears in retired_protected solely because of the modeled edit"}},
    "N-NEW-EPOCH": {"fixtures": "REPLAY sessions of the seven accounting logs", "classes": {
        "SECOND_DECISION": "after replay a second decision, roll or receipt event exists: any event_disposition key outside the golden set, or roll_attempts above the golden value"}},
    "N-MISSING-MEANS-EMPTY": {"fixtures": ["P01/ACCT-LOSS-BASE-SOURCE", "P01/ACCT-LOSS-MODEL-SUCCESS-SOURCE"], "classes": {
        "REROLLED_AS_UNROLLED": "unit a is reported unrolled (anything other than UNKNOWN_EVIDENCE_LOST in unit_roll_state.a), is absent from roll_state_unknown, or a new roll / candidate search / roll attempt for a appears (spec: retries a as unrolled)",
        "OK": "every checked field equals the golden => D-UNDETECTED (the mutant would be indistinguishable from the correct subject)"}},
}

# Oracle-side classifier for the N-MISSING-MEANS-EMPTY outcome classes (used by the oracle self-check only; the observer
# implementation is G-5 and is not part of this task). `obs` uses the golden field names; unmentioned fields equal the golden.
def classify_missing_means_empty(golden, obs):
    o = dict(golden)
    o.update(obs)
    state = o["unit_roll_state"]
    sets = o["sets"]
    if (state.get("a") != "UNKNOWN_EVIDENCE_LOST" or "a" not in sets["roll_state_unknown"]
            or o["new_roll_or_search"] != "NONE" or o["reroll"] != "NONE"
            or o["counters"].get("roll_evidenced") != golden["counters"]["roll_evidenced"]
            or set(o["event_dispositions"]) != set(golden["event_dispositions"])):
        return "REROLLED_AS_UNROLLED"
    if any(o.get("field_values", {}).get(f, "UNKNOWN_EVIDENCE_LOST") != "UNKNOWN_EVIDENCE_LOST"
           for f in golden["unknown_fields"]):
        return "U-OUTCOME-UNMATCHED"
    return "OK"
