# ST-05-P0-PROBE-01-BINDING-PREP — U01–U05 binding plan

Version: **P01-BINDING-1**, 2026-11-01. **DOCUMENTATION / STATIC ANALYSIS ONLY. NOTHING WAS EXECUTED, BUILT, GENERATED OR HASHED-INTO-AN-ARTIFACT BY THIS TASK.**

Companion to [the Probe 01 execution specification](ST05_STAGE0_P0_PROBE_01_EXECUTION_SPEC.md) (**P01-SYNTHETIC-1**). That document is not repeated here: fixture mathematics, expected outcomes (§3–§4), negative-control signatures and the proposed command (§6.1) are incorporated by reference and unchanged. This plan only supplies the *contracts* needed to turn its U01–U05 bindings into reviewable, implementable form.

**Readiness verdict: BINDING_PLAN_READY** (§11). It means the proposed contracts are specific enough for independent review. It does **not** mean any file, runner, oracle or environment exists, that any hash is frozen, or that execution is authorized. **U06 (execution permission) remains a separate owner gate. A02 BLOCKED. A03 BLOCKED. No probe execution authorized. No Stage 1 advancement.**

Vocabulary: **PROPOSED** = this plan's suggestion, not approved. **BOUND-AS-CONTRACT** = fully specified here, still needs review/approval (§9). **UNBOUND** = cannot be resolved by static documentation; stays a blocker for execution only (§8). "Approved" appears only for decisions already recorded in current authority.

## 1. Baseline and authority verification (read-only, performed)

| Check | Result |
|---|---|
| Working branch | `claude/st05-p0-probe-binding-urp8d9` (designated task branch); local `HEAD` = `origin/claude/st05-p0-probe-binding-urp8d9` = `origin/design/st05-worldgen-q01-q18-20261030` = **`5a02e5cc149d70ef6bcf2679f96f823f925a59fe`** (exact requested pin). |
| Design-branch identity | `origin/design/st05-worldgen-q01-q18-20261030` fetched; tip equals the pin; pin is an ancestor of (equal to) the tip. |
| Pin vs. spec baseline | The spec's oracle pin was `f9a4f870…`; `git diff --stat f9a4f87 5a02e5c` shows exactly one added file (the 272-line execution spec). No authority text changed between them. |
| Authority blobs (`git rev-parse HEAD:<path>`) | Every blob recorded in spec §1 matches: plan `6654b5af…`, oracle manifest `f9973084…`, API mapping `265544a6…`, feasibility gaps `dce8c4b5…`, WG decisions `9b9331de…`, FIX decisions `735bca2a…`, current Q1–Q118 authority `5d5e685c…`, `CONTENT_AUTHORITY_INDEX.md` `f43f45f5…`, `STRUCTURE_AUTHORITY_INDEX.md` `457bfad3…`, `AGENTS.md` `05abb64c…`. The spec itself is blob `40c45381…`. No nested `AGENTS.md`. |
| Spec arithmetic re-derived by hand | V2 (C=133/2, S=691/10), N-EARLY-FLOOR (689/10), NEAR-01 (S(P)=100001/2500), CHAIN-3 (40/60/80), N-ID-STRING order `b,a,d,c` — all consistent. |
| Contradiction with D1–D5 / F1–F6 | **None found.** D1–D5 and F1–F6 are not reopened. (Scope of this check: the execution spec in full, plus the authority sections it cites; no contradiction surfaced that affects U01–U05.) |
| Authority compatibility | Compatible. Nothing here changes approved score weights, WG-Q33 candidate order, Q08 A accepted-only greedy, Q09 C/Q10 C (research-only), A02/A03 BLOCKED status, or any gameplay value. |

## 2. U01–U05 binding matrix

| Item | Resolved by this plan (BOUND-AS-CONTRACT) | Constrained but still depends on | Residual UNBOUND (§8) |
|---|---|---|---|
| **U01** environment and subject | Subject identity (standalone **reference subject**, never "A02"); isolated-clone workspace; read-only production boundary with 3-layer enforcement; disposable output rules; toolchain identity tuple; no-network/no-dependency policy; reusable-infrastructure verdict (§3) | Exact JDK 25 bytes — recorded by the spec from another environment; **absent in the current sandbox** (JDK 21 only) | UB-01, UB-12 |
| **U02** input/schedule binding | Canonical-JSON rules; fixture/vertex/edge/ID/rational/candidate/protected/event/model-state schemas; invalid-input code table; deterministic schedule generator with statically derived counts; file names and hash-generation procedure (§4) | Counts re-verified at materialization | UB-02, UB-04, UB-09 |
| **U03** independent oracle | Two-tier oracle contract (hand literals + Python closed-form generator); independence rules; oracle-before-subject ordering gate; negative-control expected-result registry; review checklist; observer assertion catalogue and output schema; evidence capture (§5) | Reviewer identity; Python in execution environment | UB-03, UB-10, UB-11 |
| **U04** runner and controls | Two-invocation runner (subject / observer) interface; I/O contracts; exit codes with precedence; failure taxonomy; control registry and applicability/detection rules; unexpected-pass rule; required assertions per case family (§6) | Spec-command amendments AM-1…AM-6 (§6.1) | UB-05, UB-08 |
| **U05** budgets | Proposed limits (separated from approved: *none approved*); deterministic work-unit categories and formulas; per-limit enforcement mechanism; abort semantics; evidence-retention classes (§7) | Owner acceptance of limits | UB-06, UB-07 |

## 3. U01 — isolated environment and subject

### 3.1 Findings from read-only inspection (facts, not assumptions)

| Fact | Evidence |
|---|---|
| **No ST05 production implementation exists** at the pin. `src/` contains no occurrence of `colosseum`/`st05`; `worldgen/` holds only `ModOreGeneration.java` and `structure/`. | `grep -rli` over `src/` returned nothing; directory listing. |
| Therefore the only possible subject is a **standalone reference subject** written from the spec. Its results can never be reported as A02/A03 implementation evidence; they only test the *specified rules* and the *detector*. | Consequence of the above. |
| Repository toolchain: `options.release = 25`, `sourceCompatibility`/`targetCompatibility` 25, JUnit Jupiter 5.11.4 (`build.gradle:82-83,111-117`); Minecraft 26.3, Loader 0.19.5, Loom 1.17.21, Fabric API 0.161.0+26.3 (`gradle.properties`). Gradle resolves Loom/Fabric from remote Maven repositories → network and a large dependency graph. | File inspection. |
| **Current sandbox**: `java` = OpenJDK **21.0.12.1** (`/usr/lib/jvm/java-21-openjdk-amd64`); no JDK 25; `/workspace` **does not exist**; Python 3.13.18 present. | `java -version`, `ls`. |
| The spec's runtime (`/workspace/toolchains/jdk-25.0.4.1`, archive and `bin/java` SHA-256 values) therefore **could not be re-verified here**. Those hashes stay "spec-asserted, unverified in this environment". | `ls /workspace` failed. |
| Existing precedent for off-Gradle tooling: `tools/protocol-model` (Python stdlib), `tools/sky-islands-probe/run_probe.sh`, `tools/etherium-sampling`, `tools/ore-sampling`, `scripts/tooling` (stdlib `unittest`, tested in `.github/workflows/tooling-readiness.yml`). | Directory listing, workflow file. |
| `JAVA_TOOL_OPTIONS` in this sandbox injects proxy/truststore properties and makes the JVM print `Picked up JAVA_TOOL_OPTIONS` on stderr. The spec's `env -i` removes it. | `java -version` output. |

### 3.2 Subject and revision contract (PROPOSED)

* **Authority subject**: the *specification* — repository `ItzGamesGD/MoreMinecrafts-Beta`, design commit `5a02e5cc…`, spec blob `40c45381…`, plus the blob list in §1. This is the only "pinned design authority".
* **Implementation subject**: *Probe 01 reference subject* ("P01-REF"), to be authored later under a separate authorization. Its identity is the tuple `(source commit SHA, source tree SHA, SHA-256 manifest of every source file, jar SHA-256)`. None exists; all are UNBOUND until implementation review.
* **Relationship rule**: the implementation commit must descend from `5a02e5cc…` (checked with `git merge-base --is-ancestor`) and may add files only under `tools/st05-p0-probe-01/`. It must not edit any file already present at the pin. Any change to a pinned authority blob after the pin is an incompatibility → STOP (§10).
* **Mutants** (negative-control subjects) are part of the same source tree and are named in the frozen control registry (§6.5); a mutant is never a fallback implementation.
* Wording rule for every future report: "P01-REF subject", never "A02 implementation", "production", or "passes A3/A4/A5".

### 3.3 Workspace, boundary and disposable output (PROPOSED)

| Element | Contract |
|---|---|
| Isolated workspace `$P01_ROOT` | An absolute path **outside** the production checkout and outside any Minecraft `run/`, `saves/` or world directory, chosen at U06 (the spec's `/workspace/...` layout is not available here, so no literal path is assumed). Created fresh; must not pre-exist. |
| Workspace construction | `git clone --no-hardlinks <production-path> $P01_ROOT/repo` (offline, objects copied, **no** `git worktree add`, which would write into the production `.git`), then detached checkout of the reviewed subject commit. Hooks are not cloned. No push remote is configured for the workspace. |
| Branch for source artifacts | PROPOSED name `probe/st05-p0-probe-01` created from a descendant of `5a02e5cc…`; never `main`, never the design branch, never merged by this program (merging stays under AGENTS.md §3/§8). Branch creation is itself a U06-listed action. |
| Build products | The jar is a **non-committed** build product: `javac --release 25` + `jar --date <fixed> --create` into `$P01_ROOT/build/`. No binary is committed. Gradle, Loom, Maven and `./gradlew` are never invoked. |
| Dependencies / network | **None.** Source uses `java.base` only (no JUnit, no JSON library — a minimal syntax-only JSON reader is part of the source). Network is needed only to *provision* the JDK, out-of-band, from a pre-staged archive verified by SHA-256; build and run are offline. Static gate: `jdeps --print-module-deps probe-01.jar` must print exactly `java.base`; AM-1 adds `--limit-modules java.base` to the launch. |
| Disposable output `$P01_OUT/run-NNNN/` | Absolute; new; created by `Files.createDirectory` (fails if it exists); realpath must not be inside any git worktree, the production checkout, or a directory containing `level.dat`/`saves`/`world`; no symlink components; refuse overwrite. Spec default `/tmp/st05-p0-probe-01/run-0001` stays the default; the parent is rebindable at U06 only (with the constraints above). |
| Read-only production boundary | (1) *Structural*: the production tree is never on the classpath, never an argument, never a working directory. (2) *Pre/post proof*: wrapper records `git -C <production> rev-parse HEAD`, `git status --porcelain=v2`, and `git write-tree`-equivalent tree hash before and after; any difference ⇒ UNKNOWN(U-ENV) and STOP. (3) *Permission layer (SHOULD)*: production checkout mounted/`chmod`-ed read-only for the run user; if unavailable, record "not enforced" in `run-manifest.json` rather than claim it. |
| Persistent-world safety | The runner has no world/Minecraft concept: no path argument other than `--inputs/--expected/--schedules/--controls/--pins/--output`, all validated as regular files under `$P01_ROOT` (outputs under `$P01_OUT`). Static audit (U04 review): jar contains no reference to `java.net.*`, `ProcessBuilder`, `Runtime.exec`, or file APIs outside the argument paths. |
| Toolchain identity tuple (to be recorded, not yet verified) | `java.vendor`, `java.version`, `java.runtime.version`, `os.arch`, SHA-256 of `bin/java`, of the `release` file, and of the provisioning archive. The spec's values (Oracle 25.0.4.1, runtime 25.0.4.1+1-LTS-5, `bin/java` `352e9acd…`, archive `dd7e7f51…`) are the **expected** values; a mismatch or absence ⇒ exit 3 / STOP. **No silent substitution of JDK 21 or any other runtime**; using another runtime requires a recorded amendment to the execution spec (§9 G-1). |

### 3.4 Reusable existing infrastructure (verdict)

| Candidate | Reuse? | Reason |
|---|---|---|
| Gradle `test` / JUnit 5.11.4 | **No** | Pulls Loom/Minecraft/Fabric and network; its `Test` tasks declare production inputs (`build.gradle:90-92`); would blur the production boundary. |
| `tools/<name>/` directory convention | **Yes** | Same pattern as existing off-Gradle tools; destination `tools/st05-p0-probe-01/` (already named by the spec). |
| Python-stdlib precedent (`scripts/tooling`, `tools/protocol-model`) | **Yes, for the oracle generator and fixture verifier** | Gives a second language/library (`fractions.Fraction`, `json`) independent of the Java subject (§5). |
| `.github/workflows/*` | **No** | CI is out of scope; adding a workflow would be a production change. |
| Existing `src/test` worldgen tests | **No** | They touch production classes. |

## 4. U02 — input and schedule binding

All schemas below are **test-only protocol proposals** (spec §8 last paragraph). They create no production encoding (Q10 C untouched).

### 4.1 Artifact names and canonical form

| Artifact (under `tools/st05-p0-probe-01/`) | Content | Authored by |
|---|---|---|
| `inputs-v1.json` | All expanded fixtures (clean + negative-control inputs + invalid-input fixtures) | Input materializer (Python), reviewed |
| `goldens-v1.json` | Expected outputs (§5) | Oracle (§5), never the subject |
| `schedules-v1.json` | Enumerated delivery schedules (§4.8) | Schedule materializer (Python), reviewed |
| `controls-v1.json` | Negative-control registry (§6.5) | Hand-authored |
| `pins-v1.json` | Approved SHA-256 of the four files above (+ spec blob) — the runner's refusal anchor | Written at the freeze gate G-4 |
| `fixture-index-v1.tsv` | `fixture_id<TAB>sha256(canonical record bytes)` per fixture/golden | Materializer |

**Canonical JSON "P01-CJSON-1"** (byte-stable, so hashes are meaningful): UTF-8 without BOM; LF line ends; two-space indent; object keys in **ascending bytewise order**; arrays keep order; no insignificant whitespace beyond the indent; strings ASCII with `\uXXXX` for anything else; **no number tokens at all for domain values** (integers and rationals are strings); booleans/null allowed; exactly one trailing LF; no duplicate keys; no NaN/Infinity. A file is valid only if re-serializing its parse tree reproduces its bytes exactly (round-trip check).

**Hash-generation procedure** (no hash values are claimed anywhere in this plan): after review sign-off, run `sha256sum` over each file's exact bytes; record `<hex>  <relative path>` lines in `input.sha256` (inputs, schedules, controls), `expected.sha256` (goldens), `runner.sha256` (jar + source manifest). `pins-v1.json` embeds the same digests; its own digest is stated in the U06 authorization text out-of-band. The semantic output hash (§5.6) is separate.

### 4.2 Scalar domain types

* **Integer** `"0"`, `"-12"`, `"340"`: regex `^(0|-?[1-9][0-9]*)$`; no `+`, no `-0`, no leading zeros, ≤ 4096 bits after parse.
* **Rational** `{"num":"691","den":"10"}`: `den>0`, `gcd(|num|,den)=1`, zero is `0/1`. **Unreduced input is rejected** (`P01-E-RAT-UNREDUCED`), not normalized, so bytes are unambiguous. Score components must satisfy `0 ≤ value ≤ 100`; integers such as 100 are `{"num":"100","den":"1"}`. Powers like `2^80` are expanded to decimal digits by the materializer (spec §6.2).
* **Component tuple** `[C,T,F,E]` — four rationals. **Raw tuple** `[L,H,J,M,G,W,R,D,A]` — nine rationals. A fixture uses one or the other, never both.

### 4.3 Fixture record

```text
{ "fixture_id": "P01/CAV-01",            // run ID; deterministic path-segment escaping in evidence dirs
  "family": "ARITH|GRAPH|LONG|LATE|ACCT|NEG",
  "source_refs": ["CAV-01"],              // oracle-manifest / plan IDs
  "scalar_inputs": [ {"label":"V2","kind":"RAW9"|"COMP4","values":[rational,...]} ],   // ARITH only, else []
  "id_prefix": {"schema":"1","seed":"7","dimension":"minecraft:overworld"},            // graph/acct families
  "vertices": [ Vertex, ... ],
  "edges": [ {"u":"U","v":"V"} ],         // labels; endpoints canonicalized so id(u) < id(v)
  "protected_sites": [ Protected ],
  "unknown_edges": [ {"u":"C","v":"D"} ],
  "population_complete": true,
  "stages": [ Snapshot, Snapshot ],       // LATE-01 only; else absent. Each Snapshot repeats vertices/edges/unknown_edges/population_complete/certificate
  "events": [ Event, ... ],               // ACCT/NEG only, canonical record order; else []
  "default_schedule": "canonical",        // identity delivery; other schedules live in schedules-v1.json
  "control_overrides": {},                // e.g. {"cross_candidate_seed_order":["V","U"]} for CAV-01 N-CROSS-SEED only
  "expect_input_rejection": null,         // or a P01-E-* code for invalid-input fixtures
  "expected_ref": "P01/CAV-01" }
```

Rules: no fixture contains defaults — every vertex ID, rank, status and edge is explicit (the spec's "x = index" default is expanded by the materializer). Labels are unique within a fixture. The same fixture file is given to the clean subject and to every mutant; a mutant's deviation lives in code and in `control_overrides`, never in a differently-shaped input.

### 4.4 Vertex, canonical ID and candidate encoding

```text
Vertex = { "label": "K2",
           "id": {"x":"-2","y":"0","z":"0"},        // suffix; prefix comes from id_prefix
           "qualified": true,
           "candidates": [ Candidate, ... ],         // ≤ 8
           "fit_complete": true,
           "roll": "WIN"|"LOSS"|"NOT_ROLLED"|"NOT_APPLICABLE" }
Candidate = { "cand": "c1", "slot": "2", "seed_rank": "0",
              "status": "FIT"|"HARD_REJECT",
              "components": [rational x4] | null }    // null iff HARD_REJECT (score is NOT_APPLICABLE, not 0)
```

* **Canonical ID total order (test-only)**: compare `(schema, seed)` numerically, `dimension` by UTF-8 bytes, then `x, y, z` as signed integers; never a stringified integer, arrival index or digest. To avoid inventing cross-dimension behavior, a fixture whose vertices do not share one `id_prefix` is rejected (`P01-E-ID-PREFIX-MIXED`). Full-ID equality between two vertices is rejected (`P01-E-ID-DUPLICATE`).
* `slot` = fixed 1-based identity of a candidate slot (CAND-01: `c0`→slot 1, `c1`→slot 2; ROLL-01: slots 1…8, `seed_rank` = slot index as the spec states). Slot 9 anywhere ⇒ `P01-E-CAND-OVERFLOW` (this is the N-NINTH input).
* Graph fixtures: one candidate, `slot "1"`, `seed_rank "0"`, `status FIT`, tuple from the spec row. `G(s)` is expanded to `[s,s,s,s]` with `s` an explicit reduced rational.
* Arithmetic scalars: K4 for FIXED-01 is the four-component tuple `(50001/1000, 50, 0, 0)` (from NEAR-01 / manifest K4), `COMP4`.

### 4.5 Edge, protected-site and unknown-edge encoding

* **Edge** `{"u","v"}` between two vertex labels; `u≠v` (no self edges, `P01-E-EDGE-SELF`), both labels exist (`P01-E-EDGE-DANGLING`), no duplicate unordered pair (`P01-E-EDGE-DUPLICATE`). *Canonical edge-list order*: ascending by `(id(u), id(v))` with the endpoint pair first normalized so `id(u) < id(v)`. *Reverse order* is exactly the reversed list.
* **Protected site** `{"label":"P","id":{...},"status":"RETIRED_PROTECTED","conflicts_with":["C"]}`. A protected site is **not** a vertex: it has no components, no qualification, no roll, never enters score ordering or the roll denominator. Its conflicts are listed *here*, not in `edges`, which is how CHAIN-PROT-01's extra `{P,C}` fact is encoded. Canonical order: ascending by site ID.
* **Unknown edge** `{"u","v"}` listed in `unknown_edges` and absent from `edges`; a pair may not appear in both (`P01-E-EDGE-UNKNOWN-CONFLICT`). `population_complete:false` is required whenever `unknown_edges` is non-empty, and the converse is rejected.
* **LATE-01** is one record whose `stages` array is `[OPEN, REVEAL]`: OPEN has edges `{A,B},{B,C}`, unknown `{C,D}`, `population_complete:false`; REVEAL repeats the same vertices with edges `{A,B},{B,C},{C,D}`, no unknown edges, `population_complete:true`. The loader checks REVEAL ⊇ OPEN. Stage order is fixed; stages are never merged by arrival.

### 4.6 Event, duplicate and replay encoding (ACCT/NEG families)

```text
Event = { "index": "7",                                   // canonical log record index (spec §4.2 table)
          "kind": "QUALIFIED|FIT_PLAN_COMPLETE|ORIGINAL_ROLL|CONFLICT_DECISION|ACCEPTED_PENDING|SPACING_EXCLUDED
                   |PROTECTION_EXCLUDED*|CERTIFICATE_INVALID|APPLYING|UNCERTAIN|QUARANTINED|RETIRED_PROTECTED
                   |MODEL_PUBLICATION_RECEIPT|MODEL_PLAYER_MODIFIED",
          "unit": "a" | null,                              // null only for the aggregate decision
          "event_id": "<fixture>|<schema>|<seed>|<dimension>|<x>|<y>|<z>|decision-1|<kind>",
          "payload": { ... } }                             // kind-specific, exact fields per spec §4.2 table
```

* Aggregate decision id: `<fixture>|population-1|CONFLICT_DECISION` (spec §4.2). `|` is forbidden inside `dimension`/labels (validator rule).
* `PROTECTION_EXCLUDED*` is **not in the spec**: it is a PROPOSED addition needed only so `P01/ACCT-PROTECTED` has a literal log (3 units × QUALIFIED/FIT/ROLL = 9 records, `CONFLICT_DECISION` = 10, `ACCEPTED_PENDING(B)`, `SPACING_EXCLUDED(A)`, `PROTECTION_EXCLUDED(C)` = 11–13; `L=13`). Flagged UB-09 for reviewer acceptance; if rejected, `ACCT-PROTECTED` is checked at the graph level only.
* **Duplicate delivery** is expressed in the *schedule*, never by inventing a second event: a schedule step is `{"event":"7","origin":"worker0"}`; the same `event` index appearing twice is a duplicate of the identical `event_id` and byte-identical payload. ACCT-DUP = schedule listing the full log twice; its receipt-only subcase repeats record 13 only.
* **Contradictory payload** and **roll-before-fit** are two additional frozen negative traces: `P01/NEG-CONTRADICT-01` (ROLL-01 + a second `ORIGINAL_ROLL(c)` with `WIN` against the original `LOSS`) and `P01/NEG-ROLL-FIRST-01` (an `ORIGINAL_ROLL(a)` delivered before `FIT_PLAN_COMPLETE(a)`). Their expected dispositions follow spec §4.1/§7: contradiction ⇒ `model_knowledge_status=UNKNOWN`, affected decision frozen, no new roll, no last-writer-wins; roll-before-fit ⇒ the event is rejected as an order violation and no roll is recorded. The *expected values are derived from the spec text* but the two fixture IDs are PROPOSED (UB-09).
* **Replay**: model state is serialized with schema `P01-MODEL-STATE-1`: `{"applied_event_ids":[sorted],"sets":{qualified, fit_eligible, rolled, winners, accepted_decisions, spacing_excluded, protection_excluded, roll_loss, pending, retired_protected, model_success, history_modified},"counters":{fit_checks, fit_eligible, rolled, winners, spacing, accepted, pending, model_success, physical_completed},"pending_fields":[...],"knowledge":"KNOWN|UNKNOWN","frozen_decisions":[...]}` — sets sorted by canonical ID, `physical_completed` always the string `"0"`. A field whose source fact is undelivered is listed in `pending_fields`, never as `0`.

### 4.7 Invalid-input behavior

Two levels, never mixed:

* **Suite-level invalid** (the file, schema, pin, or environment is bad): runner exits **3** before running any case; nothing is classified PASS/FAIL.
* **Case-level invalid input** (deliberate): fixtures with `expect_input_rejection` set. The *loader* must reject exactly that code; the case PASSes only if the observed code equals the expected code, and the rejection must precede any scoring. A fixture that is rejected without an expectation is suite-level invalid.

Stable error codes (all PROPOSED): `P01-E-JSON-SYNTAX`, `-DUP-KEY`, `-NONCANON` (round-trip mismatch), `-NUMBER-TOKEN`, `-INT-FORMAT`, `-INT-BITS` (>4096), `-RAT-ZERO-DEN`, `-RAT-NEG-DEN`, `-RAT-UNREDUCED`, `-SCORE-RANGE`, `-ID-DUPLICATE`, `-ID-PREFIX-MIXED`, `-LABEL-DUPLICATE`, `-EDGE-SELF`, `-EDGE-DANGLING`, `-EDGE-DUPLICATE`, `-EDGE-UNKNOWN-CONFLICT`, `-POP-FLAG`, `-CAND-OVERFLOW`, `-CAND-NULL-MISMATCH`, `-ROLL-BEFORE-FIT-STATIC`, `-CAP-VERTICES`, `-CAP-EDGES`, `-STAGE-NOT-MONOTONE`, `-EVENT-ID-FORMAT`, `-PIN-MISMATCH`, `-ENV-MISMATCH`. An invalid-input *control set* (one fixture per code above that is data-expressible) is PROPOSED as family `NEG`; each is trivially small.

### 4.8 Deterministic schedule generator and counts

Schedules are *delivery orders of the same fixture*; the expected semantic output is identical for every schedule of a fixture (the observer asserts this).

**Generator rules (normative for the materializer):**

1. `perm(labels)`: all `k!` permutations of the fixture's vertex labels, enumerated in **lexicographic order of the label indices in canonical ID order** (index 0 = smallest ID). Schedule ID `S-<fixture>-<family-tag>-<ordinal>` with a 1-based decimal ordinal in generation order.
2. Edge orders: `CANON` then `REVERSE`; if `|E|≤1` they coincide and only `CANON` is kept (first-occurrence dedupe, spec §6.4). Protected-site orders likewise.
3. Combined schedule = `perm × edge-order × protected-order`, enumerated vertex-permutation major, then edge order, then protected order.
4. LONG families: the four named vertex sequences `INC`, `DEC`, `EVOD` (evens ascending then odds ascending), `ALT` (`[0,m−1,1,m−2,…]`) × `{CANON,REVERSE}` edges, deduplicating identical sequences (first occurrence kept) and identical edge orders.
5. LATE-01: per-stage enumeration (`perm × edge-order` for OPEN and for REVEAL independently), **paired by ordinal** into 48 two-stage sessions delivered OPEN→REVEAL (every per-stage permutation appears exactly once in its stage; cross-product pairing is not needed for the stated coverage).
6. Accounting: unit-block orders (the 3! orderings of whole `QUALIFIED→FIT→ROLL` blocks), plus `PHASE-ABC` and `PHASE-CBA`; each paired with worker-origin duplicate order `w0w1` and `w1w0`. Terminal/projection records always follow record 10 in canonical order. Replay: for each deduplicated legal log (the 8 or 26 orderings, not the worker-duplicated variants) every cut `k=0…L`.

**Statically derived counts (PROPOSED; hand-derived by this task, must be recounted by the independent materialization review — they are not materialized numbers):**

| Family | Derivation | Sessions |
|---|---|---|
| ARITH | listed, reversed, + FIXED-01 {alone, `[V0,V2,V1]`, `[K4,V2]`} | 5 |
| GRAPH ≤4 vertices | CAND-01 2; CAV-01 2; EQUAL-3-C 12; EQUAL-3-T 12; NEAR-01 2; BIG-NEAR-01 2; ID-ORDER-01 48; CHAIN-3 12; CHAIN-ID-01 12; CHAIN-PROT-01 12; CYCLE-01 48 | 164 |
| LATE-01 | 24 perms × 2 edge orders per stage, paired | 48 |
| LONG (SCORE+TIE) | BASE per family: n=1→2, 2→6, 3→8, 4→8, {8,16,64,256,1024}→8 each = 64; EXT per family: 6+8+8+8+40 = 70 (EXT has m=n+2 vertices); ×2 families | 268 |
| ACCT delivery | BASE/INVALID/RETIRE/MODEL-SUCCESS/MODEL-EDIT/PROTECTED: (3!+2)×2 = 16 each = 96; NOFIT (4 units): (4!+2)×2 = 52 | 148 |
| ACCT-DUP / ACCT-LOSS | DUP 2 (log doubling; receipt repeat); LOSS 2 variants × 2 subcases | 6 |
| ACCT-REPLAY cuts | Σ over deduplicated logs: BASE 8×13=104, INVALID 8×14=112, RETIRE 8×17=136, MODEL-SUCCESS 8×14=112, MODEL-EDIT 8×15=120, NOFIT 26×15=390, PROTECTED 8×14=112 | 1,086 |
| **Clean total (excl. NEG, mutants)** | | **1,725** |

The JSON stores the enumerated `vertex_order`, `edge_order`, `protected_order`, `events` ordering explicitly (≈ up to a few MiB for LONG), plus a `count` field per family and a list-hash line, so the file is self-describing.

## 5. U03 — independent golden oracle

### 5.1 Independence requirements (normative)

1. The oracle **never imports, links to, or reads output of** the subject, any mutant, or the runner. No shared score, comparator, greedy or reconciliation code. No JVM dependency of any kind.
2. **Language and library independence**: the oracle is **Python 3 standard library** (`fractions.Fraction`, `json`), not Java. The subject is Java `BigInteger`. A bug would have to be duplicated across two languages and two arithmetic libraries.
3. **Separate derivation** per quantity:
   * *Expected score/components*: the expanded coefficient identity `S=(16L+12H+12J+24M+16G+5W+5R+6D+4A)/100` and the stated impulse values (spec §2/§3.1), evaluated with `Fraction`; for graph tuples `S=(4C+4T+F+E)/10`. This is a derivation distinct from the subject's nested `C,T,F,E → S` pipeline.
   * *Expected order*: sorting the exact `(−S, ID)` key with Python's `sorted` over `Fraction` — a comparison of rationals, not an implementation of the Q08 greedy.
   * *Expected accepted/rejected sets and witnesses*: **not** produced by running a greedy. Small graphs are **hand-authored literals** transcribed from the spec's traces; LONG families use the closed-form parity theorem (accept `i` iff `(top−i)` even); prefix states use literal set membership of the delivered events.
4. **Order of creation (gate G-3)**: the goldens commit must precede, in git history, the first commit that adds subject source. Reviewers verify this with ancestry. Goldens are never regenerated after a subject exists; a corrected golden needs a new versioned file (`goldens-v2.json`) and a recorded reason, not an overwrite.
5. **Two tiers** (cross-check): *Tier A* — hand-authored literal goldens for every fixture except parametric ones, plus LONG n ∈ {1,2,3,4} fully by hand (BASE and EXT, both families). *Tier B* — a closed-form generator for the parametric/enumerative parts (LONG n ∈ {8,16,64,256,1024}, per-prefix model-state tables). The generator must reproduce every overlapping Tier A golden **byte-for-byte**; this is the generator's own acceptance test and a second derivation for the small cases.
6. Oracle outputs never encode the schedule: a fixture has one golden; the observer asserts that every schedule matches it.

### 5.2 Golden record schema (per fixture; expected, not observed)

```text
{ "expected_ref": "P01/CHAIN-ID-01", "kind": "GRAPH",
  "totals": {"A":"40/1","B":"60/1","C":"80/1"},                  // reduced rational strings
  "components": {"A":["100/1","0/1","0/1","0/1"], ...},
  "candidate_order": {"U":["c1","c0"]}, "selected": {"U":{"cand":"c1","slot":"2","seed_rank":"0"}},   // when applicable
  "evaluation_order": ["C","B","A"],
  "decisions": [ {"v":"C","d":"ACCEPT","witness":[]},
                 {"v":"B","d":"REJECT_SPACING","witness":["C"],"earliest_witness":"C"},
                 {"v":"A","d":"ACCEPT","witness":[]} ],
  "accepted":["C","A"], "spacing_rejected":["B"], "protected_rejected":[], "roll_loss":[], "pending":[],
  "counters": { ... }, "physical_completed":"0",
  "checkpoints": [ ... ],          // LATE-01 only: after OPEN (DEFER_GRAPH, finalized ∅, pending {A,B,C,D}) and after REVEAL
  "model_knowledge_status": "KNOWN", "input_rejection": null }
```

ARITH goldens carry `totals`/`components` per vector label and empty graph fields exactly as spec §3 ("NOT_APPLICABLE" literals preserved as the string `"NOT_APPLICABLE"`). Every previously approved outcome in spec §3–§4 is transcribed verbatim; the oracle adds nothing to them.

### 5.3 Negative-control expected results

Each mutant has, per applicable fixture, an **expected wrong output signature** (the planted defect's literal trace/set/total, e.g. N-ID-FIRST on CHAIN-ID-01 ⇒ order `B,A,C`, accepted `{B}`), stored in `goldens-v1.json` under `negative_expectations[control_id][fixture_id]` and authored from the spec rows by the same hand/Tier process. A control is *identified* only when the observed mutant output matches this signature **and** the intended assertion fails (§6.6). Signatures are used for classification, never to alter or select the clean golden.

### 5.4 Golden-file review requirements (G-3 checklist)

An independent reviewer (not the author; a different session/agent or the owner) must record, per golden: (a) every literal compared with the spec table/closed form, incl. sign and reduced denominators; (b) every vertex and edge present (complete graph, no omitted pair); (c) accepted ∪ rejected ∪ pending = declared vertices, disjoint; (d) each rejection witness is an earlier-visited *accepted/protected* neighbor joined by a declared edge, never a rejected vertex; (e) Tier A/Tier B byte equality on overlaps; (f) no value copied from any subject output; (g) git-ancestry order (§5.1.4); (h) file digests recorded only after the review. Review evidence is a short markdown checklist plus the digest list; not a new authority document.

### 5.5 Observer output schema (`observer.json`)

```text
{ "schema":"P01-OBSERVER-1",
  "pins_verified": true, "goldens_opened_after_subject_complete": true,
  "records": [ { "subject":"CLEAN"|"<control id>", "fixture":"...", "schedule":"...",
                 "assertions":[ {"id":"ASR-ORDER","verdict":"PASS|FAIL|NA|UNKNOWN",
                                 "expected_ref":"goldens#/…","observed_ref":"traces/…#line","detail":"..."} ],
                 "subject_property_verdict":"PASS|FAIL|UNKNOWN", "case_class":"…" } ],
  "detectors": [ { "control":"N-ID-FIRST", "fixture":"…", "identified":true, "unexpected_pass":false,
                   "detector_verdict":"PASS|FAIL|UNKNOWN" } ],
  "schedule_invariance": [ { "fixture":"…", "distinct_semantic_hashes":1 } ] }
```

`subject_property_verdict`, `detector_verdict`, `execution_case_verdict` and `model_knowledge_status` are separate fields everywhere (spec §5/§7).

### 5.6 Evidence capture and reproducibility

The runner writes the artifact set listed in spec §7 into `$P01_OUT/run-NNNN/`; this plan adds: `env.txt` (`java -XshowSettings:properties -version`-equivalent identity properties captured by the runner itself, not by sourcing the shell), `prod-boundary.txt` (pre/post tree proof), `fixture-index-v1.tsv` copy, and `semantic.sha256` — SHA-256 over the concatenation, in sorted (fixture, schedule) order, of the stable-key canonical JSON semantic records (decisions, sets, counters, verdicts, error/UNKNOWN/abort records) with only the diagnostics listed in spec §7 (timestamps, timings, heap samples) excluded. **Reproducibility acceptance (PROPOSED):** a second run with a different literal run ID (`run-0002`) must yield an identical `semantic.sha256`; a mismatch is FAIL (non-determinism), never retried silently.

## 6. U04 — subject and negative-control runner

### 6.1 Interface (PROPOSED) and required amendments to spec §6.1

The runner is **two JVM invocations** so the subject can never be handed the answers, plus a wrapper `run-probe.sh` that sequences them:

```text
# P2 — subject (inputs only; no goldens path anywhere in the command line)
java … -jar probe-01.jar --phase subject --suite P01-SYNTHETIC-1 --inputs inputs-v1.json --schedules schedules-v1.json \
      --controls controls-v1.json --pins pins-v1.json --seed 7 --output $P01_OUT/run-NNNN
# P3 — observer (frozen trace dir + goldens; no subject classes loaded)
java … -jar probe-01.jar --phase observer --suite P01-SYNTHETIC-1 --expected goldens-v1.json --controls controls-v1.json \
      --pins pins-v1.json --output $P01_OUT/run-NNNN
```

The spec's single-command form (`--mode subjects-and-negative-controls`) remains acceptable only if reviewers accept internal phase separation instead; this plan recommends the two-invocation form.

| ID | Proposed amendment to spec §6.1 (not applied; needs gate G-1) | Reason |
|---|---|---|
| AM-1 | Add `--limit-modules java.base -XX:MaxMetaspaceSize=64m -XX:MaxDirectMemorySize=16m` | Enforces the no-extra-module/dependency rule and bounds non-heap memory the spec left UNBOUND. |
| AM-2 | Add `--pins pins-v1.json`; refuse (exit 3) on any digest mismatch | The spec's CLI has no way to bind approved hashes. |
| AM-3 | Replace the ambiguous `--schedule frozen-v1` by explicit `--schedules`/`--controls` paths | Remove implicit file discovery. |
| AM-4 | Split into `--phase subject` and `--phase observer` | Real subject/observer separation (§6.7). |
| AM-5 | Do **not** add `-XX:+ExitOnOutOfMemoryError`: HotSpot exits with code 3 on it, colliding with exit 3 = invalid input. OOM handling is by catch + sentinel (§6.3, §7.3). | Exit-code integrity. |
| AM-6 | Jar is a non-committed build product at the spec path inside `$P01_ROOT`; its SHA-256 and source manifest enter `runner.sha256` | No binary in git. |

### 6.2 Input and output contracts

* **Subject input**: parsed fixture + a schedule step list. The subject receives *no* expected value, golden path, or other subject's output.
* **Subject output** (one JSONL trace per fixture×schedule×subject at `traces/<escaped fixture>/<schedule>.<subject>.jsonl`): header record (fixture hash, schedule id, subject id, work-unit counters at end), then ordered records `{"seq","kind":"ARRIVAL|CANDIDATE_ORDER|SELECT|SCORE|EVAL|DECISION|SET|COUNTER|EVENT_APPLIED|ABORT","…"}` carrying both arrival order and semantic order, reduced rationals, witnesses (complete accepted/protected conflicting set in canonical order + earliest in evaluation order), final sorted sets, and a terminal `END` record. A trace without `END` is *incomplete* and can never be read as a smaller accepted population (spec §6.2).
* **Observer input**: pinned goldens + trace directory only. **Observer output**: `observer.json`, `results.json`, `negative-controls.json` (§5.5).

### 6.3 Exit codes and precedence (spec §6.1 codes retained)

`0` fully observed, all clean cases PASS, every control specifically identified. `1` any determinate FAIL (clean-subject assertion, detector FAIL, or unexpected pass). `2` UNKNOWN/incomplete (resource abort, missing observation, observer unavailable) with no determinate FAIL. `3` entry invalid (suite-level invalid input, pin/env/authorization mismatch) — no case run. **Precedence** `3 > 1 > 2 > 0`; a FAIL observed before an abort stays FAIL (spec §6.5/§7). Exit status is advisory: a run is valid evidence only if the sentinel `COMPLETE` file (hash of `results.json`) exists; otherwise the wrapper classifies it `UNKNOWN(U-INCOMPLETE)` regardless of the raw JVM exit code.

### 6.4 Failure classification

`F-ARITH`, `F-ORDER`, `F-SET`, `F-WITNESS`, `F-ACCT`, `F-SCHED-VARIANT` (output differs between schedules of one fixture), `F-EXTRA-DRAW` (any roll/candidate/event beyond the original, RNG count ≠ 0), `F-PHYS` (physical_completed ≠ 0), `D-UNDETECTED` (control produced the clean-correct output ⇒ unexpected pass), `D-MISIDENTIFIED` (control failed only an unintended assertion), `D-FALSE-POSITIVE` (clean case flagged), `D-ALWAYS` (a detector verdict independent of input), `U-ABORT-WORK|TIME|MEM|EVIDENCE`, `U-ENV`, `U-OBSERVER`, `U-INCOMPLETE`, `I-<P01-E-code>` (input rejection). Each classification appears with its fixture, schedule and exact expected/observed values.

### 6.5 Negative-control selection (`controls-v1.json`)

A frozen registry; one row per control: `id`, planted-defect text, `applies_to` (fixture list), `must_detect` = `ALL_SCHEDULES` or `AT_LEAST_ONE_SCHEDULE` (with the named schedule for arrival-dependent defects, e.g. N-CAND-ARRIVAL is detected only when `[c0,c1]` is delivered), and `expected_failing_assertions`. Mutants are classes of the same source tree selected **only by registry id**, never by input data. Coverage is mandatory: every control in the spec (listed below) must have ≥1 applicable fixture, and the three **required families** are:

| Family required by task | Controls (spec IDs) | Fixtures | Failing assertion(s) |
|---|---|---|---|
| Score-first vs ID-first | N-ID-FIRST; N-ID-STRING (comparator on stringified ID) | CHAIN-ID-01, CYCLE-01, ID-ORDER-01 | `ASR-ORDER`, `ASR-FINAL-SETS` (order and set must both differ on CHAIN-ID-01) |
| Component-first vs total-score-first | N-CAND-COMPONENT, N-CROSS-C-FIRST, N-C-FIRST, N-T-FIRST, N-INNER-WEIGHT, N-EARLY-FLOOR, N-ROUND-2, N-FLOAT64, N-FIXED64, N-RELATIVE | CAND-01, CAV-01, EQUAL-3-C/T, NEAR-01, BIG-NEAR-01, V2–V4, FIXED-01 | `ASR-ARITH-*`, `ASR-ORDER`, `ASR-CAND-*` |
| Incorrect greedy blocking | N-REJECTED-VETO (rejected neighbor blocks), N-PROTECTION-IGNORE, N-IGNORE-P, N-OMIT-ENDPOINT, N-LOCAL-CAP, N-UNKNOWN-EMPTY, N-TIE-ID-DESC, N-ARRIVAL (raw arrival-order processing, cited in spec §3.3) | CHAIN-3, CHAIN-PROT-01, CYCLE-01, LONG-*, LATE-01 | `ASR-DECISION`, `ASR-WITNESS`, `ASR-FINAL-SETS`, `ASR-ORDER` |

Remaining spec controls (accounting/lifecycle and fixed-reference): N-CROSS-SEED, N-CAND-ARRIVAL, N-QUALIFIED-DENOM, N-ROLL-FIRST, N-NINTH, N-ACCEPT-IS-COMPLETE, N-POSTROLL-NOFIT, N-REROLL, N-LOGICAL-IS-PHYSICAL, N-ERASE-HISTORY, N-ADDITIVE, N-NEW-EPOCH, N-MISSING-MEANS-EMPTY — each registered with its spec fixture and the accounting assertions in §6.6. N-ARRIVAL is the only registry entry not named as a control in the spec's list (it is named in §3.3 prose) and is PROPOSED.

### 6.6 Required assertions (observer; maps to spec §5 items 1–5)

| Assertion ID | Content | Spec §5 item | Applies to |
|---|---|---|---|
| ASR-ARITH-COMP / ASR-ARITH-TOTAL | Exact reduced component and total rational strings; no clipping | 1 | ARITH, GRAPH, LONG |
| ASR-INPUT-UNCHANGED | Subject's echoed input digest equals the pinned fixture digest | 1 | all |
| ASR-STATUS | Qualification/fit/roll statuses preserved; failed-fit and roll-loss never in winner pass; ≤8 checks per unit before exactly one roll | 2 | GRAPH, ACCT |
| ASR-CAND-ORDER / ASR-CAND-SELECTED | Candidate priority order and selected slot/seed rank | 3 | CAND-01, ROLL-01 |
| ASR-ORDER | Exact full evaluation order (descending exact S, then full ID) | 4 | GRAPH, LONG, LATE |
| ASR-DECISION / ASR-WITNESS / ASR-WITNESS-SET | Per-vertex decision; witness is an earlier *accepted or protected* neighbor joined by a declared edge; never a rejected vertex | 4 | GRAPH, LONG |
| ASR-FINAL-SETS | accepted / spacing-rejected / protected-rejected / pending sets | 4 | GRAPH, LONG, LATE |
| ASR-SCHED-INVARIANT | One semantic hash across all schedules of a fixture | 3,4 | all |
| ASR-COUNTERS | fit/rolled/win/spacing/accepted/pending counters and fractions (2/3, 1/3 etc., not rates) | 5 | ACCT |
| ASR-EVENT-IDEM / ASR-REPLAY | Duplicate no-op; contradiction ⇒ UNKNOWN+frozen; prefix/cut replay equals literal set membership | 5 | ACCT, NEG |
| ASR-NOEXTRA | RNG calls = 0, no extra roll/candidate/reward event, key/reward events = 0 | 2,5 | all |
| ASR-NOPHYS | `physical_completed = "0"` always; model receipt never upgraded | 5 | ACCT |
| ASR-COMPLETE | Trace has `END`; interrupted trace not treated as a result | 4 | all |
| ASR-LATE | OPEN ⇒ `DEFER_GRAPH`, finalized ∅, pending {A,B,C,D}; REVEAL ⇒ final trace | 4 | LATE-01 |

### 6.7 Unexpected passes and subject/observer separation

* **Unexpected pass**: for each (control, applicable fixture, required schedule) the observer compares the mutant's trace to the clean golden. If every assertion in `expected_failing_assertions` passes (the mutant behaved like the correct subject) ⇒ `D-UNDETECTED`, `detector_verdict=FAIL`, exit 1. If the control fails some assertion but not the registered one ⇒ `D-MISIDENTIFIED`. If a control matches neither the clean golden nor its registered wrong-output signature (§5.3) ⇒ `UNKNOWN` (the planted defect is not what the mutant produced). A detector passes only on *identification and* clean-case success; an always-FAIL or always-UNKNOWN observer fails the clean cases and therefore cannot pass (spec §5).
* **Separation (enforceable)**: (1) different JVM invocations; the subject command line contains no goldens path; (2) the observer reads subject *files*, never subject objects; compile-time rule that package `observer` has no dependency on `subject` and vice-versa — checked by `jdeps -verbose:class`; allowed shared code is only `json` (syntax) and JDK SHA-256; (3) observer verifies `pins` and digests of inputs/goldens before opening any trace and records that order in `observer.json`; (4) no observer code calls subject score/compare/greedy code; observer compares canonical reduced rational *strings*, performing no rational arithmetic of its own beyond the "reduced" validity check.

## 7. U05 — resource budgets and enforcement

**No limit below is approved.** The "Spec proposal" column restates the exploratory ceilings of execution spec §6.5; the "Plan proposal" column adds derived headroom and enforcement. Acceptance of any value is gate G-6.

### 7.1 Limits and enforcement

| Budget | Spec proposal | Plan proposal (PROPOSED) | Basis (static estimate, unmeasured) | Enforcement mechanism in a later authorized run |
|---|---|---|---|---|
| Max vertices / edges / candidates per unit / rational bits per case | 4096 / 16384 / 8 / 4096 | unchanged; plus input files ≤ 8 MiB (`inputs`), ≤ 8 MiB (`goldens`), ≤ 8 MiB (`schedules`) | Largest selected path 1026 vertices/1025 edges ⇒ ≈ 4× vertex headroom; LONG input estimated ≈ 3–5 MiB total | Loader pre-checks (`P01-E-CAP-*`, `-INT-BITS`) before any scoring; file-size check at suite entry (exit 3) |
| Permutation/schedule count | (enumerated at U02) | Clean 1,725 sessions (§4.8); total sessions (clean + registered mutants + invalid-input controls) ≤ 25,000; schedule file lists exact counts | Mutants apply to ≤ 33 controls on subsets of fixtures | Count in `schedules-v1.json` vs `pins`; runner refuses (exit 3) if the executed list differs from the frozen list |
| Deterministic work per case/schedule | 10,000,000 counted operations | 10,000,000 hard abort; **plus** a *non-aborting* "expected-envelope" check: clean subject work ≤ `64·V·⌈log₂(V+1)⌉ + 16·E + 64·V` (recorded, a breach marks `U-BUDGET-NOTE`, not FAIL) | Clean greedy ≲ 150 K ops at V=1026 ⇒ ≳ 60× headroom under the ceiling | `WorkMeter` (§7.2) throws `WorkBudgetExceeded` at the crossing increment |
| Heap | `-Xms32m -Xmx256m` | unchanged; plus AM-1 `MaxMetaspaceSize=64m`, `MaxDirectMemorySize=16m` | Structural caps bound live data to ≲ tens of MiB | JVM flags (hard); `OutOfMemoryError` caught at the case boundary → `U-ABORT-MEM` |
| Evidence size | 256 MiB per run | 256 MiB per run and 64 MiB per trace file; estimated ≈ 10–100 MB | ≈ 44.5 K LONG vertex-decisions × ≈ 200 B per clean run; mutants multiply by the control count | Byte-counting writer; exceedance ⇒ abort with `ABORT` record, never truncation |
| Wall-clock | 60 s per case/schedule (host guard) | 60 s per case/schedule (in-JVM daemon watchdog) and 1,800 s for the whole run (outer `timeout --signal=KILL`) | Expected total ≪ 1 minute of CPU | Watchdog thread + wrapper; result is **UNKNOWN only**, never PASS/FAIL |
| Repeat-run determinism | — | one repeat (`run-0002`) per accepted run | Doubles cost, still tiny | Wrapper compares `semantic.sha256` |

### 7.2 Work-unit accounting (deterministic)

All subject arithmetic and comparison goes through a single kernel type (`ExactRational` / `WorkMeter`). One unit is added per call in exactly one category, and **nested calls are counted, not hidden**: `RAT_ADD`, `RAT_MUL`, `RAT_CMP`, `PRIO_CMP` (comparator invocation; its internal `RAT_CMP` units are counted separately), `ADJ_PROBE` (each adjacency/protected-membership inspection), `EVENT_APPLY`, plus recorded-but-uncapped `PARSE_DIGITS` and `GCD_STEPS` (bit work, proportional to ≤4096-bit inputs). The cap applies to the sum of the capped categories. Counters reset per case/schedule, are written into the trace header and `resource-counters.json`, and are semantic (they enter the semantic hash). Because mutants share the kernel and differ only by planted behavior, their counters are comparable; a non-cooperating subject is caught by the wall-clock and heap backstops. Verifying that **every** arithmetic path routes through the kernel is a static review item at the implementation gate (UB-06).

### 7.3 Timeout and abort behavior

* **Work/evidence/memory abort inside the JVM**: the case is marked `UNKNOWN` (`U-ABORT-WORK|EVIDENCE|MEM`), its partial trace is retained with an `ABORT` record and no `END`, and the runner **continues** with the remaining cases in the frozen order (deterministic; no result-sensitive cutoff). Overall result is UNKNOWN (exit 2) unless a determinate FAIL exists (exit 1).
* **Wall-clock per case**: the watchdog records `U-ABORT-TIME`, then calls `Runtime.halt` (a spinning case cannot be interrupted cooperatively). Remaining cases are `NOT_RUN`; the wrapper reports exit 2.
* **No silent budget change**: a clean determinate case that aborts means the limit is judged insufficient ⇒ STOP; revise the spec/limits under review before any new run. Hard cases are never dropped, retried or "completed by prefix".
* A deliberately fail-closed input (e.g. ACCT-LOSS source-evidence loss) is *expected* UNKNOWN; its case PASSes that assertion (spec §7) and is not an abort.

### 7.4 Evidence retention policy (PROPOSED)

| Class | Contents | Retention |
|---|---|---|
| **R0 (compact, always kept)** | `run-manifest.json`, `results.json`, `observer.json`, `negative-controls.json`, `resource-counters.json`, `semantic.sha256`, `input.sha256`, `expected.sha256`, `runner.sha256`, `prod-boundary.txt`, `env.txt`, sentinel `COMPLETE` | Kept with the run; only R0 is a candidate for inclusion in the repository, and only under a separate approval |
| **R1 (traces)** | `traces/**.jsonl`, model replay snapshots | Kept until the independent review has accepted the run; never deleted for a FAIL/UNKNOWN run until the owner accepts the diagnosis |
| **R2 (console)** | `stdout.txt`, `stderr.txt` | Kept with R0; `stderr` is expected empty (no `JAVA_TOOL_OPTIONS` under `env -i`) — any content is itself recorded |
| Disposal | The disposable `$P01_OUT` tree lives only for the container/session; durable retention outside it requires an authorized copy step. Nothing is deleted autonomously. |

## 8. Remaining UNBOUND items

| ID | Item | Why static documentation cannot resolve it | Needed to bind |
|---|---|---|---|
| UB-01 | JDK 25.0.4.1 availability and byte identity | `/workspace` is absent and only JDK 21 exists here; the spec's hashes were recorded elsewhere and are unverified | Provision the pre-staged archive; verify archive, `bin/java` and `release` digests; or approved spec amendment to another runtime |
| UB-02 | Materialized `inputs-v1.json`, `controls-v1.json`, `fixture-index` and their SHA-256 | No fixture file is authored; no hash exists or is claimed | Authorized materialization + independent comparison (G-2) |
| UB-03 | Materialized `goldens-v1.json` (Tier A literals, Tier B generator output) and review record | Not authored; oracle not implemented | G-3 |
| UB-04 | Enumerated `schedules-v1.json` and recount of §4.8 | Counts are hand-derived proposals | G-2 recount |
| UB-05 | Runner/subject/mutant/observer source, jar, `runner.sha256` | Nothing implemented | G-5 |
| UB-06 | Static proof that all arithmetic routes through `WorkMeter`; counter category mapping | Needs source | G-5 review |
| UB-07 | Owner acceptance of §7.1 limits (none approved) | Policy decision | G-6 |
| UB-08 | Approval of AM-1…AM-6 spec-command amendments | Changes the exact proposed command | G-1 |
| UB-09 | Reviewer acceptance of PROPOSED additions: `PROTECTION_EXCLUDED` event, `NEG-CONTRADICT-01`, `NEG-ROLL-FIRST-01`, invalid-input control set, N-ARRIVAL, `P01-MODEL-STATE-1` | They are derived from but not literally stated in the spec | G-2 |
| UB-10 | Python 3 version in the execution environment (3.13.18 here, unverified there) and its use for the oracle | Environment-dependent | G-1 |
| UB-11 | Independent reviewer identity and process | Organizational | G-1 |
| UB-12 | `$P01_ROOT`, `$P01_OUT`, branch name, user, permission layer | Environment-dependent | U06 |
| **U06** | Execution permission | Separate owner authorization (unchanged) | Owner |

Not UNBOUND but still unproven by design: nothing in Probe 01 can prove natural-cave completeness, a universal bound for transitive greedy closure, actual fit/roll production, publication atomicity or physical completion (spec §1 exclusions unchanged).

## 9. Explicit approval gates

Sequential; none is satisfied by this document. Each requires an explicit, separately recorded authorization.

| Gate | Authorizes | Entry conditions |
|---|---|---|
| **G-0** (this task) | Documentation commit of this plan | Baseline §1 verified — done |
| **G-1** Plan acceptance | Accepts/rejects §3–§7 contracts; AM-1…AM-6; JDK/Python choice; reviewer identity (UB-01, -08, -10, -11) | Independent review of this plan |
| **G-2** Input/schedule materialization | Create `inputs-v1.json`, `schedules-v1.json`, `controls-v1.json` (text only, no execution of the probe) and compare to spec tables; recount §4.8; accept UB-09 additions | G-1 |
| **G-3** Oracle first | Create and review `goldens-v1.json` (+ Python generator); ancestry-ordered before any subject code | G-2; independent reviewer |
| **G-4** Freeze | Compute digests, write `pins-v1.json`/`*.sha256` | G-3 |
| **G-5** Implementation | Author the reference subject, mutants, observer, wrapper in `tools/st05-p0-probe-01/`; build the jar in `$P01_ROOT`; static audits (`jdeps`, WorkMeter coverage) — **still no probe run** | G-4 |
| **G-6** Budgets | Accept §7.1 values (or edited values) | G-5 review |
| **U06** Execution | The exact command, artifacts, environment, budgets and output location | G-1…G-6 all closed; owner decision |

Merging, moving checkpoints, modifying `main`, or advancing Stage 1/A02/A03 are never implied by any gate (AGENTS.md §3, §8).

## 10. STOP conditions

Stop and report (do not substitute, guess or continue) if any of: the design branch/pin or any pinned authority blob changes incompatibly; a required authorization or gate is absent; goldens are generated from, or influenced by, subject output, or a golden is edited after a subject exists; canonical ID order or candidate seed order is silently replaced; an excluded measurement transform, placement distance, Minecraft/Fabric API, world path or production class is introduced; a limit causes graph truncation or a result-sensitive cutoff, or a clean determinate case aborts; a missing roll/event is turned into a fresh decision; the output path resolves inside the production checkout or a world directory; the production tree hash differs before/after a run; the JDK or Python identity differs from the recorded tuple; `jdeps` reports any module other than `java.base`; production, registry, controller or reward behavior is requested; execution is requested while any applicable UB item or gate is open; or package/Stage 1 progression is implied. Repeated deterministic failures follow AGENTS.md §4 (three bounded repair attempts, then the Repair Failure Handoff).

## 11. Readiness verdict

**BINDING_PLAN_READY.**

Rationale (no execution implied): for U01–U05 the document gives concrete, individually reviewable contracts — subject identity and boundary, byte-exact schemas and error codes, deterministic schedule rules with statically derived counts, an independence-constrained oracle, a registry-driven runner/control design with exit-code precedence and an unexpected-pass rule, and limits with a named enforcement mechanism for each. What remains (§8) is materialization, hashing, implementation, environment provisioning and owner acceptance; those are blockers for **execution**, not defects of the contracts. Independent review should specifically challenge: UB-09 additions, the AM-1…AM-6 amendments, hand-derived counts in §4.8, and the two-tier oracle independence argument. U06 remains a separate owner authorization gate. **A02 BLOCKED. A03 BLOCKED. No probe execution authorized. No `PROBE_01_PASS`.**
