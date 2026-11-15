# ST-05-P0-PROBE-01 — G-1 approval matrix and G-2 materialization specification

Version: **P01-G1G2-PREP-1**, 2026-11-01. **DOCUMENTATION ONLY. NOTHING WAS EXECUTED, GENERATED, MATERIALIZED, HASHED OR FROZEN.**

Baseline: `05c357e0162f8593aa811c4ed2c759d10ea2335d` on `claude/st05-p0-probe-binding-urp8d9` (design branch `design/st05-worldgen-q01-q18-20261030` still at `5a02e5cc…`; nothing integrated). Companion to [the binding plan](ST05_STAGE0_P0_PROBE_01_BINDING_PLAN.md) (revision 3) and [the execution specification](ST05_STAGE0_P0_PROBE_01_EXECUTION_SPEC.md). Section numbers `§n` refer to the binding plan unless prefixed `spec §`.

**Status.** No written G-1 authorization record exists. Therefore **G-1 is NOT ESTABLISHED**, every `PROPOSED_FOR_OWNER` item below is still only proposed, and **no G-2 artifact has been created**. Part C is a ready-to-execute procedure, not an executed one. U06, G-3…G-6 are untouched. A02 BLOCKED. A03 BLOCKED. No probe execution authorized. No Stage 1 advancement. The reference subject is a standalone test subject; nothing here is A02 implementation evidence.

## Part A — Contract corrections C1 and C2 (applied to the binding plan, revision 3)

* **C1 (artifact hash dependency).** The publication graph is now acyclic (§6.8 item 2): `observer/results.json` hashes `subject/` and `observer/` minus itself and is the observer's last file; `wrapper/wrapper.json` hashes `wrapper/` minus itself plus the digest of `results.json` and is the wrapper's last file before `COMPLETE`; `COMPLETE` is excluded from every earlier manifest and carries `results_sha256`, `wrapper_sha256` and `tree_sha256` over the whole tree. Final verification by the wrapper and by `verify_run.py` is enumerated step by step. The only things hashed by nothing are the wrapper's own console output (declared non-evidence) and `COMPLETE` (it is the root).
* **C2 (production write protection).** `find -writable` is demoted to an advisory diagnostic (§3.3). Protection is reported as `ENFORCED_RO_MOUNT` (read-only bind mount in the sandbox that runs the phases, plus `CAP_SYS_ADMIN` absent), `ENFORCED_PERMISSION` (weaker; only if U06 names it) or `NOT_ENFORCED`. It is verified under the actual execution identity from `id`, `CapEff`, `/proc/self/mountinfo` (`ro` on the production path and every nested mount) and `statvfs`; a write-denial probe (`O_CREAT` with `O_EXCL`, expected `EROFS`) runs only after those proofs hold and only in a future environment, canary first. Read-only inspection in this sandbox: user root, `CapEff` has bit 21 set, no read-only sandbox exists ⇒ the level would be `NOT_ENFORCED` today. Setting that up is operational work (OS-04, OS-05), not an architectural blocker. The production checkout was neither probed nor modified.

## Part B — G-1 approval matrix

Classes: **ALREADY_APPROVED** (recorded in current authority; preserved, not re-asked) · **PROPOSED_FOR_OWNER** (needs an explicit authorization record; nothing here is approved) · **IMPLEMENTER_SELECTABLE** (HOW detail inside the contracts; no approval needed) · **OPERATIONAL_SETUP** (environment work, no design content). "Basis" cites the plan or the spec authority table.

### B.1 ALREADY_APPROVED (preserved)

| ID | Item | Basis |
|---|---|---|
| AP-01 | Approved weights `C=(4L+3H+3J)/10`, `T=(3M+2G)/5`, `F=(W+R)/2`, `E=(3D+2A)/5`, `S=(4C+4T+F+E)/10`; exact rationals; no rounding | Spec §2; WG-Q34–Q39 |
| AP-02 | Q08 A: descending exact suitability, tie by canonical ID, accepted-only blocking (rejected candidates do not veto) | A02-FIX-Q08 |
| AP-03 | WG-Q33 within-cavern seed order; ≤ 8 fit checks before one 75% roll (FIX-01 A) | Spec §1 authority table |
| AP-04 | No same-cavern reroll or replacement after retirement (A02-FIX) | Spec §1 |
| AP-05 | Q09 C and Q10 C are research-only: no spacing metric/value, representative algorithm, ordering or encoding is approved | Spec §1, §8 |
| AP-06 | A02 BLOCKED, A03 BLOCKED, no probe execution authorized, U06 separate | Spec §8, plan header |
| AP-07 | AGENTS.md: human decides WHAT, agent decides HOW; no merge, no `main` change, no checkpoint change, no unrequested PR | `AGENTS.md` §1, §3, §8 |

### B.2 PROPOSED_FOR_OWNER (not approved)

| ID | Proposal | Reference | Default if rejected |
|---|---|---|---|
| PO-01 | AM-1: add `--limit-modules java.base -XX:MaxMetaspaceSize=64m -XX:MaxDirectMemorySize=16m` | §6.1 | Spec command unchanged; non-heap memory stays UNBOUND |
| PO-02 | AM-2: `--pins pins-v1.json`, mismatch ⇒ exit 3 | §6.1 | No in-runner digest anchor; digests checked only by the wrapper |
| PO-03 | AM-3: explicit file arguments instead of `--schedule frozen-v1` | §6.1 | Implicit discovery (rejected by the plan) |
| PO-04 | AM-4: three phases `preflight`, `subject`, `observer` replacing `--mode` | §6.1 | Single invocation with internal phases; weaker separation |
| PO-05 | AM-5: do not use `-XX:+ExitOnOutOfMemoryError` (exit-code collision with 3 believed, **unverified**, EV-3) | §6.1 | Flag omitted anyway; design does not depend on it |
| PO-06 | AM-6: jar is a non-committed build product | §6.1 | — |
| PO-07 | AM-7: wrapper-owned output subdirectories `subject/`, `observer/`, `wrapper/` | §6.1, §3.3 | Spec's single writable output directory |
| PO-08 | AM-8: `expected-traces-v1.json`, `runplan-v1.json`, `invalid-inputs-v1/`, `preflight` phase | §6.1 | No manifest-driven completeness (M5 reopens) |
| PO-09 | Test-only event kind `PROTECTION_EXCLUDED` for `ACCT-PROTECTED` | §4.6 | `ACCT-PROTECTED` checked at graph level only |
| PO-10 | Negative traces `NEG-CONTRADICT-01`, `NEG-ROLL-FIRST-01` with their derived dispositions (contradiction ⇒ `UNKNOWN` + frozen decision; early roll ⇒ rejected, no roll recorded) | §4.6, §4.8 rule 8 | Spec §4.1/§7 prose only, no fixtures |
| PO-11 | 29 invalid-input cases, their codes, and the three-layer validation (preflight / subject loader / invalid files) | §4.7 | — |
| PO-12 | Control N-ARRIVAL (cited in spec §3.3 prose, not in its control list) | §6.5 | 32 controls, 2,344 → 2,076 control sessions |
| PO-13 | Model-state replay schema `P01-MODEL-STATE-1` | §4.6 | Replay serialization UNBOUND |
| PO-14 | Trace levels `FULL`/`SUMMARY` | §6.2 | Revert to FULL for all; evidence ceiling must then be revised |
| PO-15 | `OUTCOME_CLASS` identification and the N-FIXED64 definition (UA / WR / OK) | §5.3, §6.5 | N-FIXED64 has no deterministic detector |
| PO-16 | Test-only canonical ID order (`schema`, `seed` numeric, `dimension` UTF-8 bytes, `x,y,z` signed) and rejection of mixed prefixes; **not** a production encoding | §4.4 | Owner supplies another test order |
| PO-17 | Canonical JSON `P01-CJSON-1`; input rationals as `{num,den}` objects, all other rationals as `"num/den"` strings | §4.1, §4.2 | — |
| PO-18 | Runtime choice: JDK 25 (spec expects 25.0.4.1; `build.gradle:111-117` already targets 25); any other runtime needs a recorded amendment, JDK 21 is never silently substituted | §3.3 | Owner names another runtime |
| PO-19 | Python 3 standard library as the language of the oracle, materializer and verifiers (second language for independence) | §3.4, §5.1 | Owner names another independent derivation |
| PO-20 | Independent authorship: oracle, materializer and verifier authors differ from the subject author; subject checkout excludes the oracle directory or records `oracle_exposure` | §5.1 item 7 | Same-author work carries an explicit independence caveat |
| PO-21 | Production protection: `ENFORCED_RO_MOUNT` required by default; `ENFORCED_PERMISSION` or `NOT_ENFORCED` only if U06 names the level | §3.3 | — |
| PO-22 | Schedule-coverage choices: LATE-01 zip pairing (48 sessions), replay on every deduplicated legal log (1,086 cuts), ROLL-01/NOFIT-01 vertex-array permutations (30), NEG 8+8 | §4.8 | Counts change; recount required |
| PO-23 | Reproducibility acceptance: repeat run `run-0002`, `H_full` compared only when both runs complete without TIME/MEMORY aborts | §5.6 | — |
| PO-24 | Wording rule: reports say "P01-REF subject", never "A02 implementation" or "passes A3/A4/A5"; synthetic results are never A02/A03 evidence | §3.2 | — |
| PO-25 | Reviewer identities for G-2/G-3 and the independent verifier author | §8 UB-11 | — |
| PO-26 | Budgets (**decision deferred to G-6; listed so nothing is implied**): 5,000-session ceiling, 512 MiB evidence with pre-run 50% estimate, 16 MiB files, 10,000,000 work units, 60 s per case, 1,200 s + 600 s phases, 1,800 s total, heap 256 MiB | §7.1 | All remain PROPOSED |

### B.3 IMPLEMENTER_SELECTABLE (no approval needed)

| ID | Item |
|---|---|
| IS-01 | Java package/class layout (inside the `subject`/`observer`/`preflight`/`json` separation rules) |
| IS-02 | JSON pull-parser internals (must satisfy the one-record-at-a-time and canonical round-trip rules) |
| IS-03 | `WorkMeter` implementation and counter increment sites (categories fixed by §7.2) |
| IS-04 | Wrapper scripting details inside the §6.3/§6.8 contract |
| IS-05 | Materializer and verifier code structure and file names under `tools/st05-p0-probe-01/` |
| IS-06 | Choice of sandbox tool (`unshare`, bubblewrap, container) provided PO-21 proof holds |
| IS-07 | Hash tool (`sha256sum` or `hashlib`), provided digests are identical |

### B.4 OPERATIONAL_SETUP

| ID | Item |
|---|---|
| OS-01 | Provision the JDK 25 archive out-of-band and verify archive, `bin/java`, `release` digests (setup, not a blocker) |
| OS-02 | Record the Python 3 version in the execution environment |
| OS-03 | Create `$P01_ROOT`, `$P01_OUT` per §3.3 |
| OS-04 | Build the read-only sandbox/mount and the canary directory |
| OS-05 | Run identity and capability drop (`CapEff` bit 21 clear) |
| OS-06 | Sparse checkout excluding the oracle directory for the subject author |
| OS-07 | Create the probe branch `probe/st05-p0-probe-01` when U06 lists it |
| OS-08 | Authorized copy of R0 evidence out of the disposable tree |

### B.5 Authorization record (template; **not issued**)

```text
G-1 AUTHORIZATION RECORD
Authorizer: <name>     Date: <date>     Baseline: 05c357e0162f8593aa811c4ed2c759d10ea2335d (or later commit named here)
PO-01 … PO-25: APPROVE | REJECT | AMEND(<text>)   (one line each; unlisted items remain PROPOSED)
PO-26: DEFERRED to G-6
Reviewers / authors named for PO-20 and PO-25: <names>
Scope granted: G-2 artifact materialization only (no execution, no implementation, no freeze)
```

An item with no line in a signed record stays proposed. Approval of a subset leaves the rest proposed; Part C proceeds only if the items it depends on are approved (C.0).

### B.6 Exact remaining owner decisions

1. **D1** — AM-1…AM-8 (PO-01…PO-08).
2. **D2** — test-only extension bundle (PO-09…PO-17, PO-22, PO-23, PO-24), approvable as a set or item by item.
3. **D3** — toolchain choices: JDK 25 and Python 3 stdlib (PO-18, PO-19).
4. **D4** — authorship, exposure rule and reviewer names (PO-20, PO-25).
5. **D5** — default production-protection level and whether any weaker level may ever be named in U06 (PO-21).
6. **D6** — the G-2 authorization itself (a separate sentence in the record) and whether the materialized artifacts are committed on the task branch or the probe branch.
7. **D7** — budgets (PO-26), needed only at G-6.
8. **D8** — whether and when the task branch is integrated into the design branch (not requested; not done).

## Part C — G-2 materialization readiness specification

All expected numbers below are **expected values to be independently recounted**, not assumed truths. A mismatch means STOP and report; it never means "adjust the expectation".

### C.0 Preconditions and stop conditions

Proceed only if all hold: a signed G-1 record approves at least PO-04, PO-08, PO-11, PO-16, PO-17, PO-19, PO-20, PO-22 and any other PO item whose artifact is being produced (PO-09, -10, -12, -13, -14, -15); G-2 is separately granted (D6); the baseline commit is recorded. STOP if: a spec table and this specification disagree; any recount differs from the expectation; an artifact would require a value the spec and plan do not state (list it as an open decision, do not invent it); any step needs execution of the probe, Java, or Minecraft; the work would create goldens, pins or hashes marked as frozen.

### C.1 Environment and roles

* Python 3 standard library only, offline, no network, run as the unprivileged user; output to a work directory `$P01_MAT` outside the production checkout and outside `$P01_OUT`. After review, files are committed under `tools/st05-p0-probe-01/` on the authorized branch only. Nothing is committed to `main` or the design branch.
* **Materializer** (`materialize.py`, author A) produces the artifacts. **Verifier** (`verify_materialization.py`, author B ≠ A) reads only the specification, the binding plan and this document, **imports nothing from the materializer**, and re-derives every count, order and identity from its own transcription. The two never share constants beyond the documents.
* No goldens, no pins, no freeze, no Java. Artifact digests may be computed for the review report; they are **candidate digests**, not pins (pins are G-4).

### C.2 Canonical serialization (P01-CJSON-1; normative for both tools)

UTF-8, no BOM, LF. Objects: keys sorted bytewise ascending (UTF-8), `"key": value`, two-space indent, one member per line, `{}` and `[]` for empty. Arrays keep their order, one element per line. Strings printable ASCII with JSON escapes (`\"`, `\\`, `\n`, `\uXXXX` for anything else); none of the artifacts needs more than ASCII. No number tokens for domain values: integers are decimal strings (`^(0|-?[1-9][0-9]*)$`), input rationals are `{"num","den"}` objects of such strings (reduced, `den>0`, zero `0/1`), booleans and `null` are literals. Exactly one trailing LF. A file is valid only if re-serializing its parse tree reproduces its bytes. **Record bytes** (for the index) are the record serialized alone with the same rules, plus one trailing LF.

### C.3 `inputs-v1.json` — 78 records, fixed `case_no` order

A top-level array. `case_no` = 1-based position:

| case_no | Records |
|---|---|
| 1–12 | `P01/ARITH-V0` … `P01/ARITH-V11` |
| 13 | `P01/FIXED-01` |
| 14–26 | `P01/CAND-01`, `CAV-01`, `EQUAL-3-C`, `EQUAL-3-T`, `NEAR-01`, `BIG-NEAR-01`, `ID-ORDER-01`, `CHAIN-3`, `CHAIN-ID-01`, `CHAIN-PROT-01`, `CYCLE-01`, `ROLL-01`, `NOFIT-01` |
| 27 | `P01/LATE-01` |
| 28–63 | `P01/LONG-SCORE-n/{BASE,EXT}` for n ascending over {1,2,3,4,8,16,64,256,1024} (BASE then EXT per n; 18 records), then `P01/LONG-TIE-n/…` (18 records) |
| 64–70 | `P01/ACCT-BASE`, `-INVALID`, `-RETIRE`, `-MODEL-SUCCESS`, `-MODEL-EDIT`, `-PROTECTED`, `P01/NOFIT-01/LOG` |
| 71–72 | `P01/NEG-CONTRADICT-01`, `P01/NEG-ROLL-FIRST-01` |
| 73–74 | `P01/ACCT-DUP-LOG`, `P01/ACCT-DUP-RECEIPT` |
| 75–78 | `P01/ACCT-LOSS-BASE-PROJECTION`, `-BASE-SOURCE`, `-MODEL-SUCCESS-PROJECTION`, `-MODEL-SUCCESS-SOURCE` |

Common graph defaults (written out explicitly in every record, never left implicit): `id_prefix = {"schema":"1","seed":"7","dimension":"minecraft:overworld"}`; id suffix `{"x","y","z"}` with `y=z="0"`; a graph vertex has `qualified:true`, one candidate `{cand:"c0", slot:"1", seed_rank:"0", status:"FIT", components:[C,T,F,E]}`, `fit_complete:true`, `roll:"WIN"`; `protected_sites=[]`, `unknown_edges=[]`, `population_complete:true`; `expect_input_rejection:null`. Labels are the spec labels. Edge records are `{"u","v"}` normalized so `id(u) < id(v)` and listed in canonical order.

**Construction from the spec (vertices in declared order: label id components):**

| Record | Construction |
|---|---|
| ARITH-Vn | `scalar_inputs=[{"label":"Vn","kind":"RAW9","values":[L,H,J,M,G,W,R,D,A]}]` from spec §3.1 (V0 all 0 … V11); no vertices/edges/events |
| FIXED-01 | `scalar_inputs` V0, V1, V2 (RAW9) and K4 (`COMP4`: `[50001/1000, 50, 0, 0]` as reduced rational objects); the three sessions of §4.8 are in the schedules file |
| CAND-01 | one vertex `U` id 0; two candidates: `c0` slot 1 rank 1 `[100,0,0,0]`, `c1` slot 2 rank 0 `[0,100,0,0]`, both `FIT`; no edges |
| CAV-01 | `U` −1 `[0,100,0,0]`; `V` 0 `[100,0,0,0]`; edge `{U,V}`; `control_overrides` empty (the N-CROSS-SEED override `cross_candidate_seed_order:["V","U"]` is attached by the controls file, not the clean record) |
| EQUAL-3-C | `K1` 0 `[100,0,0,0]`; `K2` −2 `[0,100,0,0]`; `K3` −1 `[50,50,0,0]`; edges K1K2, K1K3, K2K3 |
| EQUAL-3-T | same tuples; `K1` −2, `K2` 0, `K3` −1 |
| NEAR-01 | `P` 1 `[50001/1000,50,0,0]`; `Q` 0 `[50,50,0,0]`; edge PQ |
| BIG-NEAR-01 | as NEAR-01 with `P` component C = `60446290980731458735308801 / 1208925819614629174706176` (= 50 + 1/2^80; both integers expanded to decimal by the materializer; the verifier recomputes `2^80` and `50·2^80+1`) |
| ID-ORDER-01 | `a` −2, `b` −1, `c` 2, `d` 10, all `[100,0,0,0]`; all six edges |
| CHAIN-3 | `A` 0 `[100,0,0,0]`; `B` 1 `[100,50,0,0]`; `C` 2 `[100,100,0,0]`; edges AB, BC |
| CHAIN-ID-01 | same tuples; `A` 0, `B` −1, `C` 1 (declared order A, B, C) |
| CHAIN-PROT-01 | CHAIN-ID-01 plus protected site `P` id −2, `status:"RETIRED_PROTECTED"`, `conflicts_with:["C"]` |
| CYCLE-01 | labels `"0"`…`"3"`, ids 0…3, tuples `G(10),G(20),G(30),G(40)` (`G(s)=[s,s,s,s]`); edges {0,1},{1,2},{2,3},{0,3} |
| ROLL-01 | `a` 0 tuple `[100,100,0,0]`, `b` 1 `[100,0,0,0]`, `c` 2 `[50,0,0,0]`; each has 8 candidates, slot k, `seed_rank` k, all `HARD_REJECT` (`components:null`) except `a` slot 8, `b` slot 1, `c` slot 3 which are `FIT` with the unit tuple; rolls a `WIN`, b `WIN`, c `LOSS`; edge `{a,b}` |
| NOFIT-01 | ROLL-01 plus `d` id 3: 8 `HARD_REJECT` candidates, `fit_complete:true`, `roll:"NOT_ROLLED"`; no extra edge |
| LATE-01 | `stages=[OPEN,REVEAL]`; vertices `A` 0 `[100,0,0,0]`, `B` −1 `[100,50,0,0]`, `C` 1 `[100,100,0,0]`, `D` 2 `[100,100,100,100]`; OPEN edges AB, BC, unknown `{C,D}`, `population_complete:false`, `certificate:"OPEN"`; REVEAL edges AB, BC, CD, no unknown, `population_complete:true`, `certificate:"COMPLETE"` |
| LONG-SCORE-n/BASE | vertices `"0"`…`"n"`, id x = i, tuple `G(100i/(n+1))` reduced; edges `{i,i+1}`; EXT adds vertex `n+1` with `G(100)`, id n+1, edge `{n,n+1}`, **all existing tuples unchanged** |
| LONG-TIE-n/BASE | same labels; id x = −i (zero as `"0"`); tuple `[100,0,0,0]`; EXT adds `n+1` with id −(n+1), edge `{n,n+1}` |
| ACCT-* and NOFIT-01/LOG | event logs; vertex/candidate data only through events (C.3.1) |
| NEG-CONTRADICT-01 | ACCT-BASE events 1–12 plus a second `ORIGINAL_ROLL(c)` with payload `WIN` (contradicting the original `LOSS`) delivered last |
| NEG-ROLL-FIRST-01 | ACCT-BASE events with `ORIGINAL_ROLL(a)` placed before `FIT_PLAN_COMPLETE(a)` and no later roll of `a` |
| ACCT-DUP-LOG / -DUP-RECEIPT | `base_ref` ACCT-BASE (log doubled) / ACCT-MODEL-SUCCESS (record 13 repeated) |
| ACCT-LOSS-* | `base_ref` ACCT-BASE or ACCT-MODEL-SUCCESS with `loss_mode` `PROJECTION` (counters/registration projections discarded, input events and receipt kept) or `SOURCE` (decision/receipt evidence also removed) |

**C.3.1 Accounting event payloads** (spec §4.2): records 1–3 `QUALIFIED{unit,value:true}`; 4–6 `FIT_PLAN_COMPLETE{unit, slots:[8 × {slot,status,seed_rank}], selected_slot, components, fit_eligible:true}` (selected a8, b1, c3); 7–9 `ORIGINAL_ROLL{unit, result, fit_ref}` (a WIN, b WIN, c LOSS); 10 `CONFLICT_DECISION{cohort:[a,b,c], winners:[a,b], edges:[{a,b}], trace:[{a,ACCEPT},{b,REJECT_SPACING,[a]}]}` keyed `population-1`; 11 `ACCEPTED_PENDING{unit:a, ref:10}`; 12 `SPACING_EXCLUDED{unit:b, ref:10}`; variants: INVALID +13 `CERTIFICATE_INVALID{a}`; RETIRE +13 `APPLYING`, 14 `UNCERTAIN`, 15 `QUARANTINED`, 16 `RETIRED_PROTECTED` (all `a`); MODEL-SUCCESS +13 `MODEL_PUBLICATION_RECEIPT{unit:a, complete:true, effect:true, durable:true}`; MODEL-EDIT = MODEL-SUCCESS + 14 `MODEL_PLAYER_MODIFIED{a}`; NOFIT/LOG inserts `QUALIFIED(d)` before the first FIT record and `FIT_PLAN_COMPLETE(d, all 8 HARD_REJECT, fit_eligible:false, roll:NOT_ROLLED)` before the first roll (14 records); PROTECTED uses the CHAIN-PROT-01 graph units A, B, C (all rolls `WIN`): records 1–3 QUALIFIED, 4–6 FIT_PLAN_COMPLETE (single candidate), 7–9 ORIGINAL_ROLL WIN, 10 CONFLICT_DECISION (winners A,B,C; protected site P; trace `C REJECT_PROTECTED[P]`, `B ACCEPT`, `A REJECT_SPACING[B]`), 11 `ACCEPTED_PENDING{B}`, 12 `SPACING_EXCLUDED{A}`, 13 `PROTECTION_EXCLUDED{C}` (L=13). Every event has `event_id` per §4.6 and `index` as a decimal string.

### C.4 `schedules-v1.json`

Schema `P01-SCHEDULES-1`: `{ "families":[ { "fixture_id", "case_no", "tag", "count", "schedules":[ { "schedule_no", "vertex_order":[labels], "edge_order":[[u,v],…], "protected_order":[labels], "steps":[{"event","origin"}]  } ], "list_sha256" } ] }`. Generation follows §4.8 rules 1–10 exactly. ARITH sessions are **batch sessions** (rule R-ARITH): `A-LIST` and `A-REV` are attached to `case_no` 1 and list in `batch` the `case_no`s 1…12 in listed/reversed order; FIXED-01 sessions are attached to `case_no` 13 with `batch` naming the delivered scalar labels; in the manifest a batch session is one triple whose owner `case_no` is its first element. Accounting `steps` list `{event: index, origin: worker0|worker1|none}`; replay sessions carry `cut: k`. `list_sha256` is the SHA-256 of the canonical bytes of `schedules` (a candidate digest). **Expected counts (to be recounted):** ARITH 5; small graphs 164; ROLL/NOFIT graph arrays 30; LATE 48; LONG 268; ACCT delivery 148; DUP+LOSS 6; REPLAY 1,086; NEG 16; invalid 29 ⇒ **1,800**.

### C.5 `controls-v1.json`

Schema `P01-CONTROLS-1`, one row per control (33 expected): `id`, `text`, `applies_to` (case_no list), `trace_level:"SUMMARY"`, `identification` (`SIGNATURE`/`OUTCOME_CLASS`), `must_fail` (default list), `must_pass` (default list), `detect_on` (`ALL_SCHEDULES` or explicit `schedule_no` list), `control_overrides` (only N-CROSS-SEED), `sessions` (integer). Default `must_pass` is `{ASR-ACCEPT, ASR-COMPLETE, ASR-INPUT-UNCHANGED}` plus the control-specific entries below; **per-fixture overrides and literal wrong-output signatures are G-3 content** (goldens), not G-2.

| Control | applies_to (fixtures) | default must_fail | extra must_pass | sessions |
|---|---|---|---|---|
| N-INNER-WEIGHT | ARITH batches A-LIST, A-REV | ASR-ARITH-COMP, ASR-ARITH-TOTAL | — | 2 |
| N-EARLY-FLOOR | all 5 ARITH sessions | ASR-ARITH-COMP, ASR-ARITH-TOTAL | — | 5 |
| N-RELATIVE | 3 FIXED-01 sessions | ASR-ARITH-TOTAL | — | 3 |
| N-CAND-COMPONENT / N-CAND-ARRIVAL | CAND-01 | ASR-CAND-ORDER, ASR-CAND-SELECTED | ASR-FINAL-SETS | 2 + 2 |
| N-CROSS-C-FIRST / N-CROSS-SEED | CAV-01 | ASR-ORDER, ASR-DECISION, ASR-FINAL-SETS | ASR-ARITH-TOTAL | 2 + 2 |
| N-C-FIRST / N-T-FIRST | EQUAL-3-C / EQUAL-3-T | ASR-ORDER, ASR-DECISION, ASR-FINAL-SETS | ASR-ARITH-TOTAL | 12 + 12 |
| N-ROUND-2 | NEAR-01, BIG-NEAR-01 | ASR-ARITH-TOTAL, ASR-ORDER, ASR-FINAL-SETS | — | 4 |
| N-FLOAT64 / N-FIXED64 | BIG-NEAR-01 | ASR-ARITH-COMP, ASR-FINAL-SETS / OUTCOME_CLASS | — | 2 + 2 |
| N-ID-STRING | ID-ORDER-01 | ASR-ORDER, ASR-FINAL-SETS | ASR-ARITH-TOTAL | 48 |
| N-REJECTED-VETO | CHAIN-3, CHAIN-ID-01, CHAIN-PROT-01, CYCLE-01 | ASR-DECISION, ASR-WITNESS, ASR-FINAL-SETS | ASR-ORDER, ASR-ARITH-TOTAL | 84 |
| N-ID-FIRST | CHAIN-3, CHAIN-ID-01, CYCLE-01 | ASR-ORDER | ASR-ARITH-TOTAL | 72 |
| N-PROTECTION-IGNORE | CHAIN-PROT-01 | ASR-DECISION, ASR-FINAL-SETS | ASR-ORDER | 12 |
| N-OMIT-ENDPOINT / N-LOCAL-CAP | all LONG EXT | ASR-ORDER, ASR-FINAL-SETS | ASR-ARITH-TOTAL | 140 + 140 |
| N-TIE-ID-DESC | LONG-TIE BASE+EXT | ASR-ORDER | ASR-ARITH-TOTAL | 134 |
| N-ARRIVAL | LONG-SCORE, LONG-TIE | ASR-ORDER; `detect_on` = schedules whose arrival order differs from the golden order (computed by the materializer, recounted by the verifier) | ASR-ARITH-TOTAL | 268 |
| N-UNKNOWN-EMPTY | LATE-01 | ASR-LATE, ASR-FINAL-SETS | — | 48 |
| N-QUALIFIED-DENOM / N-NINTH | NOFIT-01 graph + NOFIT-01/LOG | ASR-COUNTERS, ASR-STATUS (N-NINTH: OUTCOME_CLASS) | ASR-ARITH-TOTAL | 76 + 76 |
| N-ROLL-FIRST | NEG-ROLL-FIRST-01 | ASR-STATUS, ASR-REPLAY | — | 8 |
| N-ACCEPT-IS-COMPLETE, N-POSTROLL-NOFIT, N-REROLL, N-IGNORE-P, N-LOGICAL-IS-PHYSICAL, N-ERASE-HISTORY | ACCT-BASE, -INVALID, -RETIRE, -PROTECTED, -MODEL-SUCCESS, -MODEL-EDIT respectively | ASR-COUNTERS / ASR-ACCT-SETS / ASR-NOPHYS / ASR-NOEXTRA (per control, finalized at G-3) | ASR-COMPLETE | 16 each |
| N-ADDITIVE | ACCT-DUP-LOG, -DUP-RECEIPT | ASR-EVENT-IDEM, ASR-COUNTERS | — | 2 |
| N-NEW-EPOCH | REPLAY cuts of the 7 logs | ASR-REPLAY, ASR-NOEXTRA (OUTCOME_CLASS) | — | 1,086 |
| N-MISSING-MEANS-EMPTY | ACCT-LOSS-* | ASR-REPLAY, ASR-NOEXTRA (OUTCOME_CLASS) | — | 4 |

Expected: 33 controls, **2,344** control sessions. Any control without an applicable fixture is a materialization STOP.

### C.6 `expected-traces-v1.json` and `runplan-v1.json`

* **Triples.** One entry per (subject, `case_no`, `schedule_no`): subject `CLEAN` for every clean session (1,800: the 1,771 fixture sessions plus the 29 invalid-input files, whose `case_no` namespace is `I001…I029`), and one entry per control session (2,344). Expected total **4,144** entries.
* **Entry fields:** `subject`, `case_no`, `schedule_no`, `trace_file` (`subject/traces/<case_no>/<schedule_no>.<subject>.jsonl`), `trace_level` (`FULL` for CLEAN, `SUMMARY` for controls), `batch` (for ARITH/FIXED sessions), `assertions:[{id, mode:"REQUIRED"|"NA", na_reason}]`.
* **Assertion modes by family** (from §6.6 "applies to"; everything not applicable is `NA` with a stated reason, never omitted): ARITH — ASR-ACCEPT, ASR-ARITH-COMP, ASR-ARITH-TOTAL, ASR-INPUT-UNCHANGED, ASR-SCHED-INVARIANT, ASR-NOEXTRA, ASR-COMPLETE; GRAPH/LONG — those plus ASR-STATUS, ASR-ORDER, ASR-DECISION, ASR-WITNESS, ASR-WITNESS-SET, ASR-FINAL-SETS (ASR-CAND-ORDER and ASR-CAND-SELECTED only for CAND-01, ROLL-01, NOFIT-01); LATE-01 adds ASR-LATE; ACCT/NEG — ASR-ACCEPT, ASR-INPUT-UNCHANGED, ASR-STATUS, ASR-COUNTERS, ASR-ACCT-SETS, ASR-EVENT-IDEM, ASR-REPLAY, ASR-NOEXTRA, ASR-NOPHYS, ASR-SCHED-INVARIANT, ASR-COMPLETE; invalid files — ASR-REJECT-CODE and ASR-COMPLETE only.
* **`runplan-v1.json`** is the projection `(case_no, schedule_no, subject, trace_level, trace file name)`; it contains no `assertions`, no fixture names and no `na_reason`. The verifier proves the projection by recomputation.

### C.7 `invalid-inputs-v1/` (29 files)

Each file is a minimal otherwise-valid fixture (the CAV-01 record) with exactly one defect, written as raw bytes (not always valid JSON, so these files are **not** passed through the canonical serializer except where the defect is not a canonicality defect). **Numbering is opaque:** sort the 29 codes ascending by `sha256("P01-INVALID-1|" + code)` (hex) and name the files `I001…I029` in that order; the code-to-file mapping is not written anywhere in the directory, and goes into the goldens at G-3.

| Code | Defect recipe |
|---|---|
| `JSON-SYNTAX` | Unterminated string |
| `DUP-KEY` | Two `fixture_id` members in one object |
| `NONCANON` | Valid JSON with keys out of bytewise order |
| `NUMBER-TOKEN` | A bare number token (`100`) where a string is required |
| `INT-FORMAT` | `"+5"` / leading zero in an integer string |
| `INT-BITS` | Integer string of 4,097 bits |
| `MISSING-KEY` | Vertex without `qualified` |
| `EXTRA-KEY` | Undeclared member in a vertex |
| `ENUM-VALUE` | `roll:"MAYBE"` |
| `RAT-ZERO-DEN` | `{"num":"1","den":"0"}` |
| `RAT-NEG-DEN` | `{"num":"1","den":"-2"}` |
| `RAT-UNREDUCED` | `{"num":"2","den":"4"}` |
| `SCORE-RANGE` | Component `101/1` |
| `TUPLE-ARITY` | Component tuple with 3 elements |
| `ID-DUPLICATE` | Two vertices with the same full ID |
| `ID-PREFIX-MIXED` | A vertex carrying a different `dimension` |
| `LABEL-DUPLICATE` | Two vertices labelled `U` |
| `EDGE-SELF` | Edge `{U,U}` |
| `EDGE-DANGLING` | Edge to a missing label |
| `EDGE-DUPLICATE` | `{U,V}` and `{V,U}` |
| `EDGE-UNKNOWN-CONFLICT` | `{U,V}` both known and unknown |
| `POP-FLAG` | `unknown_edges` non-empty with `population_complete:true` |
| `STAGE-NOT-MONOTONE` | LATE-01 REVEAL missing an OPEN edge |
| `CAND-OVERFLOW` | Nine candidate slots |
| `CAND-NULL-MISMATCH` | `FIT` candidate with `components:null` |
| `CAND-RANK-DUP` | Two candidates with equal `seed_rank` |
| `CAND-SLOT-DUP` | Two candidates with equal `slot` |
| `ROLL-BEFORE-FIT-STATIC` | `roll:"WIN"` with `fit_complete:false` |
| `EVENT-ID-FORMAT` | Event id lacking the `decision-1` segment |

### C.8 `fixture-index-v1.tsv`

Header-less, LF-terminated, tab-separated: `fixture_id<TAB>sha256(record bytes)` for the 78 main records in `case_no` order, then `I001…I029<TAB>sha256(file bytes)`. Expected **107 lines** (78 + 29). Candidate digests only.

### C.9 Procedure (ready to execute after G-1 and G-2 authorization)

1. Record baseline commit, tool versions (`python3 --version`), and verify `tools/st05-p0-probe-01/` does not already exist (it does not at `05c357e0…`).
2. Author A runs the materializer into `$P01_MAT`; no artifact is edited by hand afterwards.
3. Author B, independently, runs the verifier: it recomputes **(a)** record count 78 and the `case_no` order of C.3; **(b)** invalid count 29 and the opaque numbering of C.7; **(c)** every schedule count per family and the total 1,800; **(d)** control sessions 2,344 and 33 controls with ≥ 1 applicable fixture each; **(e)** triples 4,144; **(f)** fixture-set equality between `inputs-v1.json` plus `invalid-inputs-v1/` and `expected-traces-v1.json`; **(g)** runplan = projection of the manifest; **(h)** canonical round-trip of every JSON artifact; **(i)** tuple, ID, edge, protected and event data of every fixture against the spec tables (spec §3–§4) and the C.3 constructions; **(j)** the LONG fixtures against the closed forms (§4.8 rules 1–4); **(k)** inputs ≤ 16 MiB, each invalid file ≤ 64 KiB; **(l)** no domain number tokens; **(m)** candidate digests recomputed and equal to the materializer's report.
4. A materialization report (inside the review comment/commit message, not a new authority document) lists the recounted values next to the expectations, the candidate digests, any open decision found, and states explicitly: goldens NOT created, pins NOT written, nothing frozen.
5. Any mismatch ⇒ STOP and report (bounded repair under AGENTS.md §4 applies to *tool defects* only, never to changing an expected value).
6. Review by the independent reviewer(s) named in the G-1 record; G-2 closes only on their written acceptance. G-3 (oracle first) is a separate authorization.

### C.10 Expected values (not assumed truths)

| Quantity | Expected | Recount owner |
|---|---|---|
| Main records | 78 | Verifier |
| Invalid-input files | 29 | Verifier |
| Clean sessions | 1,800 (5 + 164 + 30 + 48 + 268 + 148 + 6 + 1,086 + 16 + 29) | Verifier |
| Control sessions | 2,344 across 33 controls | Verifier |
| Triples in the manifest | 4,144 | Verifier |
| Index lines | 107 | Verifier |
| Session ceiling (PROPOSED, G-6) | 5,000 | Reviewer |

The recount may legitimately differ if the verifier finds a rule ambiguity: that is a finding to report (and, if it changes expected results, a STOP-and-ask under AGENTS.md §2), not something to be reconciled silently.

### C.11 Open points found while preparing (none changes an approved outcome)

1. The `(case_no, schedule_no)` triple granularity for ARITH batch sessions is resolved by rule R-ARITH (C.4); reviewers should confirm it.
2. Per-fixture `must_fail` overrides depend on wrong-output signatures that exist only at G-3.
3. The opaque numbering of C.7 is computable by anyone holding this document; it prevents positional guessing only, and the opacity is not a security boundary (anti-hardcoding relies on the static audit of §6.2).
4. The PROTECTED accounting log (C.3.1) uses the proposed `PROTECTION_EXCLUDED` event (PO-09); without that approval the ACCT-PROTECTED record is dropped and the expected counts change (77 records; clean sessions fall by 128, i.e. 16 delivery + 112 replay cuts, to 1,672; control N-IGNORE-P loses its only fixture and is dropped, control sessions fall by 16 to 2,328; recount required).
