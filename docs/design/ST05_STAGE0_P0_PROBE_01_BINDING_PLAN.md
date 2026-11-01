# ST-05-P0-PROBE-01-BINDING-PREP — U01–U05 binding plan

Version: **P01-BINDING-2**, 2026-11-01 (revision of `8f9580da895242a110d31fa6f83773931b28a8e8`; closes audit findings M1–M9 and the listed MINOR items, see §12). **Revision 5** (owner decision S1 R2 for LOSS-SOURCE, deferred-field assertion rule, replay/round-trip/LATE checkpoint trace records; see the revision-5 record in §12) follows revision 4. **Revision 4** (G-3 contract clarifications F2–F6 and LOSS-SOURCE, see the revision-4 record in §12) follows revision 3. **Revision 3** over `05c357e0162f8593aa811c4ed2c759d10ea2335d` closes contract items C1 (artifact hash graph) and C2 (write protection) and points to the G-1/G-2 preparation record [ST05_STAGE0_P0_PROBE_01_G1G2_PREP.md](ST05_STAGE0_P0_PROBE_01_G1G2_PREP.md). **DOCUMENTATION / STATIC ANALYSIS ONLY. NOTHING WAS EXECUTED, BUILT, GENERATED OR HASHED-INTO-AN-ARTIFACT BY THIS TASK.**

Companion to [the Probe 01 execution specification](ST05_STAGE0_P0_PROBE_01_EXECUTION_SPEC.md) (**P01-SYNTHETIC-1**). That document is not repeated here: fixture mathematics, expected outcomes (§3–§4), negative-control signatures and the proposed command (§6.1) are incorporated by reference and unchanged. This plan only supplies the *contracts* needed to turn its U01–U05 bindings into reviewable, implementable form.

**Readiness verdict: BINDING_PLAN_READY** (§11). It means the proposed contracts are specific enough for independent review. It does **not** mean any file, runner, oracle or environment exists, that any hash is frozen, or that execution is authorized. **U06 (execution permission) remains a separate owner gate. A02 BLOCKED. A03 BLOCKED. No probe execution authorized. No Stage 1 advancement.**

Vocabulary: **PROPOSED** = this plan's suggestion, not approved. **BOUND-AS-CONTRACT** = fully specified here, still needs review/approval (§9). **UNBOUND** = cannot be resolved by static documentation; stays a blocker for execution only (§8). "Approved" appears only for decisions already recorded in current authority.

## 1. Baseline and authority verification (read-only, performed)

| Check | Result |
|---|---|
| Working branch | `claude/st05-p0-probe-binding-urp8d9` (designated task branch); local `HEAD` = `origin/claude/st05-p0-probe-binding-urp8d9` = `origin/design/st05-worldgen-q01-q18-20261030` = **`5a02e5cc149d70ef6bcf2679f96f823f925a59fe`** (exact requested pin) — state at first writing; revision 2 is a later commit on the task branch only. |
| Design-branch identity | `origin/design/st05-worldgen-q01-q18-20261030` fetched; tip equals the pin; pin is an ancestor of (equal to) the tip. |
| Pin vs. spec baseline | The spec's oracle pin was `f9a4f870…`; `git diff --stat f9a4f87 5a02e5c` shows exactly one added file (the 272-line execution spec). No authority text changed between them. |
| Authority blobs (`git rev-parse HEAD:<path>`) | Every blob recorded in spec §1 matches: plan `6654b5af…`, oracle manifest `f9973084…`, API mapping `265544a6…`, feasibility gaps `dce8c4b5…`, WG decisions `9b9331de…`, FIX decisions `735bca2a…`, current Q1–Q118 authority `5d5e685c…`, `CONTENT_AUTHORITY_INDEX.md` `f43f45f5…`, `STRUCTURE_AUTHORITY_INDEX.md` `457bfad3…`, `AGENTS.md` `05abb64c…`. The spec itself is blob `40c45381…`. No nested `AGENTS.md`. |
| Spec arithmetic re-derived by hand | V2 (C=133/2, S=691/10), N-EARLY-FLOOR (689/10), NEAR-01 (S(P)=100001/2500), CHAIN-3 (40/60/80), N-ID-STRING order `b,a,d,c` — all consistent. |
| Contradiction with D1–D5 / F1–F6 | **None found.** D1–D5 and F1–F6 are not reopened. (Scope of this check: the execution spec in full, plus the authority sections it cites; no contradiction surfaced that affects U01–U05.) |
| Authority compatibility | Compatible. Nothing here changes approved score weights, WG-Q33 candidate order, Q08 A accepted-only greedy, Q09 C/Q10 C (research-only), A02/A03 BLOCKED status, or any gameplay value. |
| Revision-2 integration status | Revision 1 is commit `8f9580da…` on `claude/st05-p0-probe-binding-urp8d9`; the design branch `design/st05-worldgen-q01-q18-20261030` remains at `5a02e5cc…`. Neither revision is integrated into the design branch; integration is an owner decision and was not performed. |
| Revision-3 baseline | Revision 3 is a later commit on the task branch only, on top of `05c357e0…`; the design branch is unchanged and nothing is integrated. |

## 2. U01–U05 binding matrix

| Item | Resolved by this plan (BOUND-AS-CONTRACT) | Constrained but still depends on | Residual UNBOUND (§8) |
|---|---|---|---|
| **U01** environment and subject | Subject identity (standalone **reference subject**, never "A02"); isolated-clone workspace; three-layer production boundary (structural, wrapper-recorded recursive manifest including ignored paths, write protection verified under the execution identity (`ENFORCED_RO_MOUNT` preferred) **or an explicit `NOT_ENFORCED` report**); disposable output with wrapper-owned subdirectories; toolchain identity tuple; no-network/no-dependency policy; reusable-infrastructure verdict (§3) | Exact JDK 25 bytes — recorded by the spec from another environment; **absent in the current sandbox** (JDK 21 only; never silently substituted) | UB-01, UB-12 |
| **U02** input/schedule binding | Canonical-JSON rules; fixture inventory (78 records + 29 invalid files); rational input/string conversion; schemas; three-layer validation (preflight / subject loader / invalid cases); deterministic schedule generator with recomputed counts (1,800 clean sessions); file names and hash procedure (§4) | Counts re-verified at materialization | UB-02, UB-04, UB-09 |
| **U03** independent oracle | Two-tier oracle (hand literals + Python closed-form generator); independence and authorship rules; oracle-before-subject gate; control expectations with `SIGNATURE`/`OUTCOME_CLASS` identification; review checklist; observer schema; evidence capture (§5) | Reviewer identity; Python in execution environment | UB-03, UB-10, UB-11 |
| **U04** runner and controls | Three-phase runner (preflight / subject / observer) with wrapper; trace levels; durable hash-chained FAIL records and final classification table; terminal vs case-local aborts; frozen trace manifest; control registry with must-fail / must-pass; atomic publication and final sentinel (§6) | Spec-command amendments AM-1…AM-8 (§6.1) | UB-05, UB-08 |
| **U05** budgets | Proposed limits (separated from approved: *none approved*); derived session ceiling; evidence ceiling with trace levels and pre-run estimate; deterministic work-unit categories; per-limit enforcement; abort classes; retention (§7) | Owner acceptance of limits | UB-06, UB-07 |

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
| Disposable output `$P01_OUT/run-NNNN/` | Absolute; new; the **wrapper** creates `run-NNNN/` (fails if it exists) with empty subdirectories `subject/`, `observer/`, `wrapper/`; each JVM receives exactly one of them as `--output` and may not write elsewhere; the observer reads `subject/` read-only (AM-7). Realpath must not be inside any git worktree, the production checkout, or a directory containing `level.dat`/`saves`/`world`; no symlink components; refuse overwrite. Spec default `/tmp/st05-p0-probe-01/run-0001` stays the default; the parent is rebindable at U06 only (with the constraints above). |
| Read-only production boundary (three layers, each reported separately) | (1) *Structural*: the production tree is never on the classpath, an argument, or a working directory (argument validation and static audit). (2) *Detection*: before and after the run the **wrapper, not the JVM,** records `git status --porcelain=v2 --ignored=matching --untracked-files=all` **and** a recursive filesystem manifest of the whole production directory including ignored paths such as `run/`, `runs/`, `build/`, `logs/`, `.gradle/` (`find -P <prod> -xdev -printf '%y\t%m\t%U\t%s\t%T@\t%i\t%p\0'`, bytewise-sorted; tracked-file content identity from `git ls-files -s`). Any difference, or any traversal error, ⇒ `UNKNOWN(U-ENV)` and STOP. Metadata can miss a same-size same-mtime rewrite, so this layer is **detection only** and is never described as prevention. (3) *Prevention* — reported as exactly one of `production_write_protection ∈ {ENFORCED_RO_MOUNT, ENFORCED_PERMISSION, NOT_ENFORCED}`. **`ENFORCED_RO_MOUNT` (default requirement):** the production tree is a read-only bind mount inside the sandbox in which the wrapper and every JVM phase run (e.g. `unshare -m` followed by `mount --bind` and `mount -o remount,bind,ro`, or bubblewrap `--ro-bind`, or a read-only container volume), **and** `CAP_SYS_ADMIN` is absent from the sandbox's effective capabilities (`CapEff` bit 21 clear), so a process inside cannot remount it read-write. `ENFORCED_PERMISSION` (weaker; accepted only if the U06 authorization names it): a non-root run user with no write permission or ACL on the tree. Anything else is `NOT_ENFORCED`. **`find -writable` is not proof of immutability and is not used as a pass criterion**; it is at most an advisory diagnostic recorded in `wrapper/`. **Verification under the actual execution identity**, performed by the same launcher and inside the same sandbox that will run the phases, before preflight: record `id -u`, `id -G`, `CapEff`, the `/proc/self/mountinfo` lines for the production path and for every mount nested below it (all must carry the `ro` option), and the `statvfs` read-only flag of the production path. **Write-denial probe (future environment only):** only if those static proofs hold, attempt exclusive creation (`O_CREAT` with `O_EXCL`) of a uniquely named empty file in the production top directory; the expected result is `EROFS` (`EACCES` under `ENFORCED_PERMISSION`). The probe is **never attempted when the static proofs failed**, because it could then modify production; if it ever succeeds it is unlinked at once, the level is recorded `NOT_ENFORCED`, and the run STOPs. A canary directory under `$P01_ROOT`, protected by the same mechanism, is probed first to validate the mechanism. If protection cannot be enforced the wrapper writes `production_write_protection: NOT_ENFORCED (<reason>)` into `wrapper.json`, `results.json` and the report header, and the run may proceed only if the U06 authorization names that level explicitly; safety is never claimed silently. In the present sandbox the user is root with `CAP_SYS_ADMIN` set and no read-only sandbox exists, so the level would be `NOT_ENFORCED` until a sandbox is built. **Nothing in this task probed or modified the production checkout.** |
| Persistent-world safety | The runner has no world/Minecraft concept: its only path arguments are `--inputs`, `--schedules`, `--controls`, `--traces-manifest`, `--invalid-dir`, `--runplan`, `--expected`, `--goldens-digest-only`, `--pins`, `--subject-dir` and `--output` (§6.1), all validated as regular files or directories under `$P01_ROOT` (outputs under `$P01_OUT`). Static audit (U04 review): the jar contains no reference to `java.net.*`, `ProcessBuilder`, `Runtime.exec`, or file APIs outside those argument paths. |
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
| `expected-traces-v1.json` | Frozen manifest of every required triple (subject, `case_no`, `schedule_no`): trace file, trace level, and per-assertion `REQUIRED`/`NA`+reason. Drives the observer; hashed in pins | Derived by the materializer; re-derived by the Python verifier |
| `runplan-v1.json` | Projection of the manifest for the subject: ordinals, subject ids, trace level, trace file name only — no fixture names, assertions or expectations | Derived |
| `invalid-inputs-v1/I001.json … I029.json` | Raw invalid-input files with opaque names (§4.7); expected codes live only in the goldens | Hand-authored, reviewed |
| `pins-v1.json` | Approved SHA-256 of every file/directory listing above and below (inputs, schedules, controls, traces manifest, runplan, invalid files, goldens) + spec blob — the runner's refusal anchor | Written at freeze gate G-4 |
| `fixture-index-v1.tsv` | `fixture_id<TAB>sha256(canonical record bytes)` per fixture/golden | Materializer |

**Canonical JSON "P01-CJSON-1"** (byte-stable, so hashes are meaningful): UTF-8 without BOM; LF line ends; two-space indent; object keys in **ascending bytewise order**; arrays keep order; no insignificant whitespace beyond the indent; strings ASCII with `\uXXXX` for anything else; **no number tokens at all for domain values** (integers and rationals are strings); booleans/null allowed; exactly one trailing LF; no duplicate keys; no NaN/Infinity. A file is valid only if re-serializing its parse tree reproduces its bytes exactly (round-trip check).

**Hash-generation procedure** (no hash values are claimed anywhere in this plan): after review sign-off, run `sha256sum` over each file's exact bytes; record `<hex>  <relative path>` lines in `input.sha256` (inputs, schedules, controls, traces manifest, runplan, invalid-input files), `expected.sha256` (goldens), `runner.sha256` (jar + source manifest). `pins-v1.json` embeds the same digests; its own digest is stated in the U06 authorization text out-of-band. The output hashes `H_inv` and `H_full` (§5.6) are separate.

### 4.2 Scalar domain types

* **Integer** `"0"`, `"-12"`, `"340"`: regex `^(0|-?[1-9][0-9]*)$`; no `+`, no `-0`, no leading zeros, ≤ 4096 bits after parse.
* **Rational** `{"num":"691","den":"10"}` appears **only in input files**: `den>0`, `gcd(|num|,den)=1`, zero is `0/1`. **Unreduced input is rejected** (`P01-E-RAT-UNREDUCED`), not normalized, so bytes are unambiguous. Score components must satisfy `0 ≤ value ≤ 100`; integers such as 100 are `{"num":"100","den":"1"}`. Powers like `2^80` are expanded to decimal digits by the materializer (spec §6.2). **In goldens, traces, results and model state a rational is the canonical string `"num/den"`** (`"691/10"`, zero `"0/1"`, integers `"40/1"`). The only conversion is `num + "/" + den` on already-reduced values (and its inverse); a string rational in an input file or an object rational in a golden/trace is `P01-E-NONCANON`. The observer compares these strings byte-for-byte.
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

Rules: no fixture contains defaults — every vertex ID, rank, status and edge is explicit (the spec's "x = index" default is expanded by the materializer). Labels are unique within a fixture. Streaming and subject view: `inputs-v1.json` is a top-level array of fixture records and every reader (preflight, subject loader, Python verifier) uses a pull parser holding **one record at a time**. The subject receives a *view* with an opaque `case_no` (1-based position in the array) and an opaque `schedule_no` instead of `fixture_id`, `source_refs`, `expected_ref` and schedule names; the ordinal→name mapping exists only in `expected-traces-v1.json`. **Fixture-ID path escaping** (observer, wrapper and index files; subject traces use ordinals): every byte outside `[A-Za-z0-9._-]` becomes `%XX` with uppercase hex, so `P01/CAV-01` ⇒ `P01%2FCAV-01`; a segment may not start with `.`, may not exceed 200 bytes, and `..`, empty segments and path separators are rejected. The same fixture file is given to the clean subject and to every mutant; a mutant's deviation lives in code and in `control_overrides`, never in a differently-shaped input.

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
* `slot` = fixed 1-based identity of a candidate slot (CAND-01: `c0`→slot 1, `c1`→slot 2; ROLL-01: slots 1…8, `seed_rank` = slot index as the spec states). A ninth slot present in an input file ⇒ `P01-E-CAND-OVERFLOW` (an invalid-input case, §4.7); the N-NINTH *mutant* is a different object (§6.5).
* Graph fixtures: one candidate, `slot "1"`, `seed_rank "0"`, `status FIT`, tuple from the spec row. `G(s)` is expanded to `[s,s,s,s]` with `s` an explicit reduced rational.
* Arithmetic scalars: K4 for FIXED-01 is the four-component tuple `(50001/1000, 50, 0, 0)` (from NEAR-01 / manifest K4), `COMP4`.

### 4.5 Edge, protected-site and unknown-edge encoding

* **Edge** `{"u","v"}` between two vertex labels; `u≠v` (no self edges, `P01-E-EDGE-SELF`), both labels exist (`P01-E-EDGE-DANGLING`), no duplicate unordered pair (`P01-E-EDGE-DUPLICATE`). **Rule split (revision 4, F5):** `EDGE-DUPLICATE` means the same *unordered* pair is listed two or more times, whatever the endpoint order of each listing (the G-2 invalid file repeats the identical edge, the cleanest isolation of that code). Endpoint order and list order are *not* input errors: a loader normalizes each edge so `id(u) < id(v)` and sorts the list canonically before use; no input code exists for un-normalized edges and none is added. The pinned files are already normalized and sorted (checked by the G-2 verifier), so normalization never changes their meaning. *Canonical edge-list order*: ascending by `(id(u), id(v))` with the endpoint pair first normalized so `id(u) < id(v)`. *Reverse order* is exactly the reversed list.
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

### 4.7 Input validation, preflight and invalid-input behavior

Three layers that are never mixed (audit M8). All codes and files below are **PROPOSED test-only extensions** (gate G-1/G-2); none is approved.

| Layer | Runs in | Scope | Outcome on failure |
|---|---|---|---|
| **Preflight** (neutral) | `--phase preflight`: package `preflight`, depending only on the JDK and the `json` syntax reader — no subject class, no observer class, no golden *content* | Digest of every pinned artifact against `pins-v1.json`; file-size ceilings (§7.1); JSON well-formedness and canonical round-trip of the pinned main files; **fixture-ID set equality** between `inputs-v1.json` and `expected-traces-v1.json`; schedule list and counts equal the manifest; environment identity tuple | Exit **3**; no case run; nothing classified PASS/FAIL. Codes: `P01-E-PIN-MISMATCH`, `-ENV-MISMATCH`, `-FILE-SIZE`, `-MANIFEST-MISMATCH`, and `-NONCANON`/`-JSON-SYNTAX` **on a pinned main file** |
| **Subject loader** | the subject; every mutant uses the same loader class | Full semantic validation of each fixture record it receives. Every main-file fixture is **pinned and therefore valid by definition** (digest pin plus the independent Python verifier at G-2/G-4) | Rejecting a pinned valid fixture is a determinate **subject FAIL** `F-INPUT-REJECTED`, recorded as a `REJECTED{code}` trace record and judged by `ASR-ACCEPT`. It is never exit 3 and never "invalid suite input" |
| **Invalid-input cases** | the subject loader, given raw files `invalid-inputs-v1/I001.json … I029.json` | Deliberately invalid bytes. File names are opaque; the expected code lives only in the goldens (`invalid_expectations`), never in the file name, the runplan, or anything the subject can read | Observed code must equal the expected code and precede any scoring (`ASR-REJECT-CODE`). Accepted ⇒ `F-INPUT-ACCEPTED`; wrong code ⇒ `F-INPUT-CODE`; no code recorded ⇒ UNKNOWN |

**Invalid-input categories (29 codes, one file each):**

* *Syntax/encoding (9):* `P01-E-JSON-SYNTAX`, `-DUP-KEY`, `-NONCANON`, `-NUMBER-TOKEN`, `-INT-FORMAT`, `-INT-BITS` (>4096 bits), `-MISSING-KEY`, `-EXTRA-KEY`, `-ENUM-VALUE` (e.g. unknown `roll` or `status` value).
* *Rationals (5):* `-RAT-ZERO-DEN`, `-RAT-NEG-DEN`, `-RAT-UNREDUCED`, `-SCORE-RANGE`, `-TUPLE-ARITY` (component tuple not 4, raw tuple not 9).
* *Graph (9):* `-ID-DUPLICATE`, `-ID-PREFIX-MIXED`, `-LABEL-DUPLICATE`, `-EDGE-SELF`, `-EDGE-DANGLING`, `-EDGE-DUPLICATE`, `-EDGE-UNKNOWN-CONFLICT`, `-POP-FLAG`, `-STAGE-NOT-MONOTONE`.
* *Candidates (5):* `-CAND-OVERFLOW` (a ninth slot is **present in the input**; this is the input-validation case for the "ninth check" rule), `-CAND-NULL-MISMATCH`, `-CAND-RANK-DUP` (two candidates of one unit with equal `seed_rank`, which would leave ties undefined), `-CAND-SLOT-DUP`, `-ROLL-BEFORE-FIT-STATIC`.
* *Events (1):* `-EVENT-ID-FORMAT`.

`-CAP-VERTICES` and `-CAP-EDGES` are loader rules but are **not fixture-tested**: a cap+1 fixture would be a ≈4 MiB file. They are covered by static review of the loader and by the preflight cardinality check, and recorded as such. `N-NINTH` (§6.5) is a different object: a *mutant* that fabricates a ninth fit check on a valid ≤ 8-slot input, detected through counters, not through an input file.

**Loader detection precedence (revision 4, F4).** A file is reported with exactly one code: the first rule below that matches, tier by tier, and within a tier in the order listed. This is the order the golden `invalid_expectations` use and the order the G-5 loader must implement.

| Tier | Codes (in order) |
|---|---|
| 1 syntax | `JSON-SYNTAX` |
| 2 object keys | `DUP-KEY` |
| 3 tokens | `NUMBER-TOKEN` |
| 4 canonical bytes | `NONCANON` (the file's bytes differ from the canonical serialization of its parse tree) |
| 5 shape | `ID-PREFIX-MIXED` (an `id` object carries a prefix key `schema`/`seed`/`dimension`; this outranks `EXTRA-KEY`, because the schema has no per-vertex prefix field), then `MISSING-KEY`, `EXTRA-KEY`, `ENUM-VALUE` |
| 6 integers | `INT-FORMAT`, `INT-BITS` |
| 7 rationals | `RAT-ZERO-DEN`, `RAT-NEG-DEN`, `RAT-UNREDUCED`, `SCORE-RANGE`, `TUPLE-ARITY` |
| 8 identity and graph | `ID-DUPLICATE`, `LABEL-DUPLICATE`, `EDGE-SELF`, `EDGE-DANGLING`, `EDGE-DUPLICATE`, `EDGE-UNKNOWN-CONFLICT`, `POP-FLAG`, `STAGE-NOT-MONOTONE` |
| 9 candidates | `CAND-OVERFLOW`, `CAND-SLOT-DUP`, `CAND-RANK-DUP`, `CAND-NULL-MISMATCH`, `ROLL-BEFORE-FIT-STATIC` |
| 10 events | `EVENT-ID-FORMAT` |

For records with a non-empty `stages` array the top-level graph fields are inert and the POP-FLAG and graph rules apply per snapshot; the top-level `population_complete` is `true` (empty graph, no unknown edges), which satisfies the rule literally (revision 4 correction of finding F1).

### 4.8 Deterministic schedule generator and counts

Schedules are *delivery orders of the same fixture*; the expected projection `P_inv` (§5.6) is identical for every schedule of a fixture.

**Generator rules (normative for the materializer):**

1. `perm(labels)`: all `k!` permutations of a fixture's vertex labels, enumerated in **lexicographic order of label indices in canonical-ID order** (index 0 = smallest ID); the fixture's declared array order is the `canonical` identity schedule (a separate schedule that may coincide with one permutation and is then deduplicated). Schedule number `schedule_no` is the 1-based ordinal in generation order; schedule IDs `S-<fixture>-<tag>-<ordinal>` are only for the manifest.
2. Edge orders `CANON` then `REVERSE`; if `|E|≤1` they coincide and only `CANON` is kept (first-occurrence dedupe, spec §6.4). Protected-site orders likewise.
3. Combined schedule = `perm × edge-order × protected-order`, vertex permutation major.
4. LONG families: `INC`, `DEC`, `EVOD` (evens ascending then odds ascending), `ALT` (`[0,m−1,1,m−2,…]`) × `{CANON,REVERSE}` edges, deduplicating identical sequences and identical edge orders.
5. LATE-01: per-stage enumeration (`perm × edge-order` for OPEN and for REVEAL independently), paired by ordinal into 48 two-stage sessions delivered OPEN→REVEAL.
6. Accounting event logs: the 3! unit-block orders plus `PHASE-ABC` and `PHASE-CBA` (4 units: 4! + 2), each paired with worker-origin duplicate order `w0w1` and `w1w0`. Terminal and projection records always follow record 10 in canonical order. Replay: for each deduplicated legal log (not the worker-duplicated variants) every cut `k=0…L`.
7. **ROLL-01 and NOFIT-01 as graph fixtures** (static vertex arrays with candidate descriptors, fixture IDs `P01/ROLL-01`, `P01/NOFIT-01`) follow rules 1–3: `3!×1 = 6` and `4!×1 = 24` (their only edge is `{a,b}`). Their event-log counterparts (`P01/ACCT-*`, `P01/NOFIT-01/LOG`) receive vertex and candidate data only through events, so rule 1 does not apply to them.
8. NEG traces: `P01/NEG-CONTRADICT-01` (ROLL-01 log plus a contradictory second `ORIGINAL_ROLL(c)`) and `P01/NEG-ROLL-FIRST-01` (an `ORIGINAL_ROLL(a)` delivered immediately before `FIT_PLAN_COMPLETE(a)`, with the legal later roll of `a` omitted) each use the 8 legal orderings of rule 6 without worker duplicates: 8 sessions each.
9. Invalid-input files: 1 session each (the loader only).
10. `case_no` = 1-based position of the fixture record in `inputs-v1.json`; `schedule_no` = 1-based position in the fixture's schedule list. The subject sees only these ordinals (§4.3).

**Recomputed counts** (independently re-derived in revision 2 by arithmetic over the rules above; PROPOSED until the materialization review recounts them):

| Family | Derivation | Sessions |
|---|---|---|
| ARITH | `A-LIST` and `A-REV` (all 12 vectors), + FIXED-01 {alone, `[V0,V2,V1]`, `[K4,V2]`} | 5 |
| GRAPH ≤ 4 vertices | CAND-01 2; CAV-01 2; EQUAL-3-C 12; EQUAL-3-T 12; NEAR-01 2; BIG-NEAR-01 2; ID-ORDER-01 48; CHAIN-3 12; CHAIN-ID-01 12; CHAIN-PROT-01 12; CYCLE-01 48 | 164 |
| ROLL-01 / NOFIT-01 graph arrays | 3!×1 + 4!×1 | 30 |
| LATE-01 | 24 perms × 2 edge orders per stage, paired | 48 |
| LONG (SCORE+TIE) | per family BASE 2+6+8+8+5×8 = 64, EXT 6+8+8+8+5×8 = 70; ×2 families | 268 |
| ACCT delivery | six 3-unit logs (BASE, INVALID, RETIRE, MODEL-SUCCESS, MODEL-EDIT, PROTECTED) × (3!+2)×2 = 16 → 96; NOFIT-01/LOG (4 units) (4!+2)×2 = 52 | 148 |
| ACCT-DUP / ACCT-LOSS | DUP 2 records × 1; LOSS 4 records × 1 | 6 |
| ACCT-REPLAY cuts | BASE 8×13=104, INVALID 8×14=112, RETIRE 8×17=136, MODEL-SUCCESS 8×14=112, MODEL-EDIT 8×15=120, NOFIT-01/LOG 26×15=390, PROTECTED 8×14=112 | 1,086 |
| NEG traces | 2 fixtures × 8 | 16 |
| Invalid-input files | 29 codes × 1 | 29 |
| **Clean-subject total** | 5 + 164 + 30 + 48 + 268 + 148 + 6 + 1,086 + 16 + 29 | **1,800** |

(Revision 1 reported 1,725; the 75 difference is the 30 ROLL/NOFIT graph sessions, the 16 NEG sessions and the 29 invalid-input sessions that were previously unscheduled.) The control sessions and the total-session ceiling are derived in §6.5 and §7.1.

LONG-family schedule files store explicit `vertex_order`, `edge_order` and `protected_order` lists plus a per-family `count` and list hash, so the file is self-describing.

**Replay session semantics (revision 4, F6).** A `REPLAY` schedule with `cut = k` and `steps = [s1…sL]` means: (1) apply the first `k` steps to a fresh model state; (2) serialize that state with `P01-MODEL-STATE-1`, decode it, and require the decoded state to equal the state before serialization; (3) deliver the remaining steps `s(k+1)…sL`; (4) deliver the complete original log (all `L` steps) **twice more** from the start, every re-delivery being an identical duplicate and therefore a no-op. The expected state after step (1) is the *prefix state* in the golden; the expected state after (3) and after (4) is the full-log final state of the fixture. `DELIVERY` schedules duplicate each event (worker0/worker1) and have no cut.

### 4.9 Fixture inventory (closes the missing-inventory finding)

| Family | Fixture IDs | Records |
|---|---|---|
| ARITH | `P01/ARITH-V0` … `P01/ARITH-V11` | 12 |
| Fixed-reference | `P01/FIXED-01` (scalars V0, V1, V2 and the four-component K4 `(50001/1000,50,0,0)`) | 1 |
| Small graphs | CAND-01, CAV-01, EQUAL-3-C, EQUAL-3-T, NEAR-01, BIG-NEAR-01, ID-ORDER-01, CHAIN-3, CHAIN-ID-01, CHAIN-PROT-01, CYCLE-01, ROLL-01, NOFIT-01 (all `P01/…`) | 13 |
| Late information | `P01/LATE-01` | 1 |
| Long chains | `P01/LONG-{SCORE,TIE}-n/{BASE,EXT}`, n ∈ {1,2,3,4,8,16,64,256,1024} | 36 |
| Accounting logs | `P01/ACCT-BASE`, `-INVALID`, `-RETIRE`, `-MODEL-SUCCESS`, `-MODEL-EDIT`, `-PROTECTED`, `P01/NOFIT-01/LOG` (REPLAY is a schedule mode of these seven; its prefix tables live in their goldens) | 7 |
| Negative traces | `P01/NEG-CONTRADICT-01`, `P01/NEG-ROLL-FIRST-01` | 2 |
| Duplicate / loss | `P01/ACCT-DUP-LOG`, `P01/ACCT-DUP-RECEIPT`, `P01/ACCT-LOSS-{BASE,MODEL-SUCCESS}-{PROJECTION,SOURCE}` | 6 |
| **Main-file records** | | **78** |
| Invalid-input files (separate directory, not in `inputs-v1.json`) | `invalid-inputs-v1/I001…I029` | 29 |

`expected-traces-v1.json` (§6.5, §6.7) must list every one of the 78 records and 29 files; the preflight checks set equality, so no valid fixture can be omitted from a PASS run.

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
7. **Authorship and exposure.** The oracle (hand literals, Python generator) and the subject are authored by different sessions/agents; the subject author works from the specification and this plan, with the oracle directory excluded from that checkout (sparse checkout). The exposure that remains (the oracle exists in git before the subject) is recorded as `oracle_exposure` in `run-manifest.json`. The subject is also barred from fixture names (opaque ordinals, §4.3) and a static scan forbids golden literals in its source (§6.2), which is the protection against a hard-coded subject.

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

**Concrete golden file (revision 4; G-3 candidate, not frozen).** `oracle/goldens-v1.json`, schema `P01-GOLDENS-1`, canonical JSON (`P01-CJSON-1`), rationals as `"num/den"` strings. Top-level sections: `fixtures` (one golden per main record, schedule-invariant), `prefix_states` (per REPLAY session: the expected prefix state after `cut` steps), `invalid_expectations` (opaque file name → expected code, plus the precedence list), `negative_expectations` (per control: finalized `must_fail`/`must_pass`, literal wrong-output signatures or outcome-class definitions), `deferred` (explicit list of expectations withheld, each with a reason) and `provenance` (derivation tag per fixture: `LITERAL_SPEC` hand-written from the spec, `CLOSED_FORM` computed from a formula, `DERIVED_PROSE` derived by definition from spec prose; the cross-check results). Sets are sorted by canonical ID ascending. `ASR-COUNTERS` covers the logical counters and fractions only; `physical_completed` is judged solely by `ASR-NOPHYS`. A fraction is a pair of decimal strings `{"n","d"}` (e.g. winners/rolled = `2`/`3`), never a measured rate.

**ACCT-LOSS-SOURCE (revision 5; owner decision S1 LOSS-SOURCE: R2 approved).** *Supersedes the revision-4 reading, which had withheld the set-valued expectations.* The source-loss subcase removes **unit `a`'s `ORIGINAL_ROLL` evidence (record 7) and the evidence that depends on it**: the aggregate `CONFLICT_DECISION` (record 10), its projections (11, 12, which reference record 10) and, for MODEL-SUCCESS, the model receipt (13). **Preserved**: every `QUALIFIED` and `FIT_PLAN_COMPLETE` record (1–6, independently established FIT evidence) and the other units' histories (`ORIGINAL_ROLL(b)` = 8, `ORIGINAL_ROLL(c)` = 9). Delivered set for both SOURCE schedules: records `1,2,3,4,5,6,8,9` (the projection-loss subcase is unchanged). **Rule: missing evidence never implies an unrolled state and never authorizes a new roll.**

Golden content for the two SOURCE fixtures (status `COMPLETE`, tag `OWNER_DECISION_S1_R2`): knowledge `UNKNOWN`; affected operations `BLOCKED`; `new_roll_or_search` `NONE`; `reroll` `NONE`; no frozen decision; known history preserved. Evaluable facts: `sets.qualified` = `fit_eligible` = `{a,b,c}`; evidence-only sets `rolled_evidenced {b,c}`, `winners_evidenced {b}`, `roll_loss_evidenced {c}`; `roll_state_unknown {a}` and `unit_roll_state` = `{a: UNKNOWN_EVIDENCE_LOST, b: WIN, c: LOSS}`; counters `qualified 3`, `fit_checks 24`, `fit_eligible 3`, `roll_evidenced 2`; `physical_completed "0"`. The fields that depended on the removed evidence — `accepted_decisions`, `spacing_excluded`, `protection_excluded`, `pending`, `model_success` and the fractions (their denominator is the unknown rolled count) — carry the expected value **`UNKNOWN_EVIDENCE_LOST`** and are listed in the fixture's `unknown_fields`. `UNKNOWN_EVIDENCE_LOST` is an *evaluable expected value*, **not** a deferral: a subject that reports `[]`, `0` or any number for such a field fails. The `rolled_evidenced` sets are lower bounds named for what the retained records show; they are never read as "the units that rolled". The two SOURCE fixtures therefore leave the `deferred` list.

**N-MISSING-MEANS-EMPTY on the corrected SOURCE cases.** The mutant (`detect_on` the two SOURCE sessions only) is identified by outcome class `REROLLED_AS_UNROLLED` (unit `a` not `UNKNOWN_EVIDENCE_LOST` / missing from `roll_state_unknown` / a new roll, search or roll attempt / an event outside the delivered set) or `UNKNOWN_REPORTED_AS_EMPTY` (an `unknown_fields` entry reported as empty/zero). Output equal to the golden is `D-UNDETECTED`. The oracle self-check classifies seven constructed mutant outputs per fixture and the clean golden (`oracle/negatives.py::classify_missing_means_empty`, `oracle/oracle.py::integrity`).

**Deferred-field assertion rule (revision 5, `P01-DEFERRED-1`).** A fixture's `deferred` list (and the top-level `deferred` section) withholds expectations that no approved text determines; prefix-state values `PENDING` and `DEFERRED:*` are withheld in the same way. For every REQUIRED assertion on a triple: (1) the observer evaluates only the **evaluable** fields — those not withheld; (2) a withheld field is recorded as `excluded` with its reason and can **never contribute a PASS** (nor a FAIL); (3) with at least one evaluable field the verdict is PASS only if all evaluable fields match, FAIL if any differs; (4) if **no evaluable field remains** for that assertion on that triple the verdict is **UNKNOWN** (`U-ALL-DEFERRED`), never PASS and never `NA`; it counts as `unknown` in the coverage and, for a control triple, does not satisfy "at least one REQUIRED `must_fail` is judgeable" (§6.5). `UNKNOWN_EVIDENCE_LOST` values are evaluable and not covered by this rule.

### 5.3 Negative-control expected results and identification types

Each mutant has, per applicable fixture, an expected **wrong-output description** in `goldens-v1.json` under `negative_expectations[control_id][fixture_id]`, authored from the spec rows by the same hand/Tier process. A control is *identified* only when the observed mutant output satisfies **all** of: (a) every assertion in its registry `must_fail` list fails; (b) every assertion in its `must_pass` list passes (this localizes the planted defect: e.g. N-ID-FIRST on CHAIN-ID-01 must pass `ASR-ARITH-TOTAL` and fail `ASR-ORDER` and `ASR-FINAL-SETS`); and (c) its **identification type** matches:

* `SIGNATURE` — the mutant's literal wrong output equals the registered literal (e.g. N-ID-FIRST ⇒ order `B,A,C`, accepted `{B}`; N-EARLY-FLOOR ⇒ `689/10`).
* `OUTCOME_CLASS` — used when the spec gives a qualitative defect rather than a literal value (N-FIXED64, N-NINTH, N-NEW-EPOCH, N-MISSING-MEANS-EMPTY, N-ERASE-HISTORY). The registry lists the accepted outcome classes explicitly; any output outside them is UNKNOWN (never PASS). Definitions are in §6.5.

Signatures and outcome classes classify mutants only; they never alter or select the clean golden.

### 5.4 Golden-file review requirements (G-3 checklist)

An independent reviewer (not the author; a different session/agent or the owner) must record, per golden: (a) every literal compared with the spec table/closed form, incl. sign and reduced denominators; (b) every vertex and edge present (complete graph, no omitted pair); (c) accepted ∪ rejected ∪ pending = declared vertices, disjoint; (d) each rejection witness is an earlier-visited *accepted/protected* neighbor joined by a declared edge, never a rejected vertex; (e) Tier A/Tier B byte equality on overlaps; (f) no value copied from any subject output; (g) git-ancestry order (§5.1.4); (h) file digests recorded only after the review. Review evidence is a short markdown checklist plus the digest list; not a new authority document.

### 5.5 Observer output schema (`observer/observer.json`)

```text
{ "schema":"P01-OBSERVER-2",
  "pins_verified": true, "goldens_opened_after_subject_phase_complete": true,
  "coverage": { "required_triples":"N", "observed":"N", "passed":"N", "failed":"0", "unknown":"0", "not_run":"0" },
  "records": [ { "triple": {"subject":"CLEAN"|"<control id>", "case_no":"1", "schedule_no":"1"},
                 "fixture": "...",                      // resolved from the frozen manifest, never from the trace
                 "assertions":[ {"id":"ASR-ORDER","mode":"REQUIRED"|"NA","na_reason":null,
                                 "verdict":"PASS|FAIL|UNKNOWN",
                                 "expected_ref":"goldens#/…","observed_ref":"subject/traces/…#line","detail":"..."} ],
                 "subject_property_verdict":"PASS|FAIL|UNKNOWN", "execution_case_verdict":"…", "case_class":"…" } ],
  "detectors": [ { "control":"N-ID-FIRST", "fixture":"…", "identified":true, "unexpected_pass":false,
                   "detector_verdict":"PASS|FAIL|UNKNOWN" } ],
  "schedule_invariance": [ { "fixture":"…", "distinct_P_inv_hashes":1 } ],
  "advisories": [ { "kind":"A-BUDGET-NOTE", "triple":{…}, "detail":"…" } ] }
```

`mode` is **never chosen by the observer**: `NA` is permitted only where the frozen manifest lists `NA` with its reason for that triple (§6.7). The verdict vocabulary has no `NA`; a manifest-`NA` assertion is skipped and counted, and any other absent or skipped assertion is UNKNOWN. `subject_property_verdict`, `detector_verdict`, `execution_case_verdict` and `model_knowledge_status` remain separate fields (spec §5/§7).

### 5.6 Evidence capture, hashes and reproducibility

**Two different hashes (audit M1).**

* **`P_inv` — schedule-invariant semantic projection**, hashed as `H_inv` per (fixture, subject). It contains only quantities that must not depend on delivery order: reduced component and total rationals; candidate order and selected candidate; the evaluation order; per-vertex decisions with the complete witness set and the earliest witness; the final accepted / spacing-rejected / protection-rejected / roll-loss / pending sets; the final model-state sets and **logical** counters (fit checks, rolled, winners, spacing, accepted, pending, model-success, `physical_completed`); event dispositions as a *set keyed by `event_id`* (`APPLIED`, `DUPLICATE_NOOP`, `REJECTED(reason)`); `model_knowledge_status`; the input-rejection code. It **excludes**: arrival order, `ARRIVAL` and `EVENT_APPLIED` sequences, delivery/duplicate counts, origin labels, schedule IDs and numbers, **work counters**, timings, heap samples, abort information and trace line numbers. `ASR-SCHED-INVARIANT` requires one `H_inv` across all schedules of a fixture for the clean subject (and, per registry, for controls whose defect is schedule-independent). Per-cut prefix states of ACCT-REPLAY are cut-specific: they are compared to the golden prefix tables directly and are excluded from `H_inv`; only the final full-log state enters it.
* **`H_full` — same-schedule reproducibility hash**: SHA-256 over every record of every trace in sorted (`case_no`, `schedule_no`, subject) order, **including** arrival order, work counters and abort records, excluding only wall-clock timings and heap samples. It is compared only between repeat runs of the *same* frozen triple.

**Reproducibility acceptance (PROPOSED).** A repeat run with a different literal run ID (`run-0002`) is compared with `run-0001` triple by triple on `H_full`. Comparison applies **only if both runs are `COMPLETE` and neither contains a TIME- or MEMORY-class abort** (those depend on host speed). Otherwise the comparison result is `NOT_COMPARABLE`, which counts as UNKNOWN, never as a pass and never as `F-NONDETERMINISM`. A mismatch between two comparable runs is `F-NONDETERMINISM` (FAIL). Failed repeats are not retried silently.

**Evidence set** written under `$P01_OUT/run-NNNN/` (ownership in §3.3; the run directory contains only `subject/`, `observer/`, `wrapper/` and the final `COMPLETE`): `subject/` (traces, `resource-counters.json`, `subject-phase.json`, `abort.marker` if any); `observer/` (`observer.json`, `negative-controls.json`, `fails.jsonl`, `semantic.sha256` = all `H_inv`, `repro.sha256` = all `H_full`, and last of all `results.json`); `wrapper/` (`run-manifest.json` with authority, spec, input, golden, runner and JDK hashes, runtime metadata, CLI and environment, production-protection level and selected scope; copies of `inputs-v1.json`/`goldens-v1.json`/`schedules-v1.json`/`controls-v1.json`/`expected-traces-v1.json`/`pins-v1.json`; `input.sha256`, `expected.sha256`, `runner.sha256`; `env.txt`; the production manifests and protection record; the captured `stdout.txt`/`stderr.txt` of each phase; and last of all `wrapper.json`); and `COMPLETE` (§6.8). No artifact generated now contains probe results.

## 6. U04 — subject and negative-control runner

### 6.1 Interface (PROPOSED) and amendments to spec §6.1

The runner is **three JVM invocations** sequenced by a small wrapper `run-probe.sh`, so the subject can never be handed expected values:

```text
# P1 preflight — neutral; no subject/observer classes; hashes goldens without parsing them
java <flags> -jar probe-01.jar --phase preflight --suite P01-SYNTHETIC-1 --inputs inputs-v1.json --schedules schedules-v1.json \
      --controls controls-v1.json --traces-manifest expected-traces-v1.json --invalid-dir invalid-inputs-v1 \
      --goldens-digest-only goldens-v1.json --pins pins-v1.json --output <run>/wrapper/preflight
# P2 subject — receives inputs and a runplan only; never a goldens path, never controls' expectations
java <flags> -jar probe-01.jar --phase subject --suite P01-SYNTHETIC-1 --inputs inputs-v1.json --schedules schedules-v1.json \
      --invalid-dir invalid-inputs-v1 --runplan runplan-v1.json --pins pins-v1.json --seed 7 --output <run>/subject
# P3 observer — frozen manifest, goldens, and the subject's completed files only
java <flags> -jar probe-01.jar --phase observer --suite P01-SYNTHETIC-1 --expected goldens-v1.json --controls controls-v1.json \
      --traces-manifest expected-traces-v1.json --pins pins-v1.json --subject-dir <run>/subject --output <run>/observer
```

`<flags>` is the spec's `env -i … java -Xms32m -Xmx256m -XX:ActiveProcessorCount=1 -Dfile.encoding=UTF-8 -Duser.* …` plus AM-1. `runplan-v1.json` is a derived projection of `expected-traces-v1.json` holding only `(case_no, schedule_no, subject id, trace level, trace file name)`; it contains no assertions, no fixture names and no expectations. The spec's single-command form (`--mode subjects-and-negative-controls`) remains acceptable only if reviewers accept internal phase separation instead.

| ID | Proposed amendment to spec §6.1 (**not applied, not approved**; gate G-1) | Reason / consistency note |
|---|---|---|
| AM-1 | Add `--limit-modules java.base -XX:MaxMetaspaceSize=64m -XX:MaxDirectMemorySize=16m` | Enforces the no-extra-module rule and bounds non-heap memory. Compatible with `-jar`; SHA-256 and file I/O are in `java.base`. |
| AM-2 | Add `--pins pins-v1.json`; mismatch ⇒ exit 3 | The spec's CLI has no way to bind approved digests. The spec blob and `AGENTS.md` blob are checked by the wrapper with `git`, not by the JVM. |
| AM-3 | Replace the ambiguous `--schedule frozen-v1` by explicit `--schedules`, `--controls`, `--traces-manifest`, `--invalid-dir`, `--runplan` paths | No implicit file discovery. Only the observer receives `--controls`/`--expected`. |
| AM-4 | Split the single invocation into `--phase preflight`, `--phase subject`, `--phase observer` | Real subject/observer separation; replaces `--mode`. |
| AM-5 | Do **not** add `-XX:+ExitOnOutOfMemoryError` | HotSpot is believed to exit with code 3 on it, which would collide with exit 3 = entry invalid. **UNVERIFIED by execution (EV-3).** The design does not depend on the claim: classification never uses the raw JVM exit code alone (§6.3, §6.8). |
| AM-6 | The jar is a non-committed build product at the spec's path inside `$P01_ROOT`; its SHA-256 and source manifest enter `runner.sha256` | No binary in git. |
| AM-7 (new) | **Output ownership.** The wrapper creates `run-NNNN/` (fails if it exists) with empty subdirectories `subject/`, `observer/`, `wrapper/`. Each JVM receives exactly one pre-created, empty subdirectory as `--output`, must not create, delete or overwrite anything outside it, and the observer reads `subject/` read-only. Supersedes "the runner must create the output directory" for the split design | Resolves the conflict between "output must be new" and two writers. |
| AM-8 (new) | Add the test-only artifacts `expected-traces-v1.json`, `runplan-v1.json`, `invalid-inputs-v1/` and the `preflight` phase | Required by §4.7 and §6.7 completeness rules. |

AM-1…AM-8 are **proposals**. Nothing in this plan approves them.

### 6.2 Input and output contracts

* **Subject input**: one fixture record at a time (pull parser, §4.3) in its subject view (`case_no` instead of `fixture_id`, no `source_refs`, no `expected_ref`, `control_overrides` supplied only to the control they belong to) plus the schedule step list for `schedule_no`. No expected value, golden path, control expectation or other subject output is reachable from the subject command line. **Anti-hardcoding:** static audit (G-5) forbids in the subject source any golden literal or fixture-specific constant (a reviewer script greps every golden total, set and id string against the subject package); the opaque ordinals prevent keying by fixture name.
* **Trace levels** (bounded storage, audit M6): `FULL` (clean subject): header, then `ARRIVAL`, `CANDIDATE_ORDER`, `SELECT`, `SCORE`, `EVAL`, `DECISION`, `SET`, `COUNTER`, `EVENT_APPLIED`, terminal `END`. `SUMMARY` (mutants, revision 4 / F3): header, `CANDIDATE_ORDER`, `SELECT`, `SCORE`, `EVAL` order, `DECISION`, `SET`, `COUNTER`, one `EVENT_DISPOSITION` per `event_id` (the disposition of its first delivery), `END`; it omits only the per-delivery `ARRIVAL` and `EVENT_APPLIED` records. `SCORE` and `EVENT_DISPOSITION` are kept because the arithmetic controls are judged on components/totals and the accounting controls on event dispositions; none of the control assertions needs the omitted records. The level of every triple is fixed in the manifest, not chosen by the subject.
* **Minimum required checkpoint records (revision 5).** Present in both trace levels; a required record that is absent, misordered or malformed makes the triple `U-INCOMPLETE` (UNKNOWN), never PASS.
  * `REPLAY_CHECKPOINT` — REPLAY sessions only, exactly one, emitted after the first `cut` events are applied and before any round-trip: `{"t":"REPLAY_CHECKPOINT","cut":"k","applied_event_ids":[sorted ids],"state_sha256":"<digest of the canonical P01-MODEL-STATE-1 document>","knowledge":"KNOWN|UNKNOWN","pending_fields":[…]}`. `cut=0` is legal and has an empty applied list; the state digest is still required.
  * `ROUNDTRIP` — exactly one, immediately after `REPLAY_CHECKPOINT`: `{"t":"ROUNDTRIP","cut":"k","serialized_sha256":"…","restored_state_sha256":"…","equal":true|false}`. The observer requires `restored_state_sha256 == state_sha256` of the checkpoint and compares the digest with the golden prefix state; `equal` is the subject's own claim and is never trusted alone.
  * `REPLAY_FINAL` — exactly two, for the two complete-log replays that follow the remainder: `{"t":"REPLAY_FINAL","pass":"1"|"2","state_sha256":"…","new_decision_events":"0","rng_calls":"0"}`. Both digests must equal the golden final state.
  * `LATE_CHECKPOINT` — LATE-01 sessions, exactly two: `OPEN` (after the last OPEN-stage arrival, before the first REVEAL-stage arrival) with `action:"DEFER_GRAPH"`, `finalized_accepted:[]`, `finalized_rejected:[]`, `pending:["B","A","C","D"]` order as delivered-independent canonical order, `population_complete` as declared by the snapshot; and `REVEAL` with `action:"FINALIZE"` and the final accepted / rejected / pending sets. Shape: `{"t":"LATE_CHECKPOINT","stage":"OPEN"|"REVEAL","action":"…","finalized_accepted":[…],"finalized_rejected":[…],"pending":[…],"population_complete":true|false}`. A finalized decision before the `OPEN` checkpoint, or an `OPEN` checkpoint with a non-empty finalized set, is the N-UNKNOWN-EMPTY signature.
* **Subject output** under `subject/traces/<case_no>/<schedule_no>.<subject-id>.jsonl` (decimal ordinals, subject ids from the registry). Each file is a JSON-lines stream whose last record is `END`. During writing the file has the suffix `.partial`; it is renamed to the final name only after `END` is flushed (§6.8). A final-named file without `END`, a truncated last line, or a `.partial` file is **incomplete** and never authoritative. Witnesses are the complete conflicting accepted/protected set in canonical order plus the earliest in evaluation order; an interrupted trace is never read as a smaller accepted population.
* **Observer output** under `observer/` (§5.5): `observer.json`, `results.json`, `negative-controls.json`, `fails.jsonl` (§6.3).
* **Rationals** in traces, goldens and results are canonical strings `"num/den"` (§4.2); the observer compares them byte-for-byte after the "reduced" validity check and does no other rational arithmetic.

### 6.3 Exit codes, durable FAIL evidence and final classification

Per-phase JVM exit codes retain the spec's meaning: `0` completed with nothing to report, `1` determinate FAIL, `2` UNKNOWN/incomplete, `3` entry invalid. **The raw JVM exit code is advisory.** The authoritative outcome is determined by the wrapper from files (below).

**Durable FAIL recording (audit M3).** The observer appends each determinate FAIL, as soon as it is established, to `observer/fails.jsonl` (final name from the start; the one deliberate exception to temp-and-rename). Each line is canonical JSON `{"n":k,"prev":"<sha256 of previous line bytes, 64 zeros for k=1>","record":{…},"trace_sha256":"…","golden_sha256":"…"}` followed by LF, written and `force`d (flushed to the file system) before the observer continues. A record is **verified** by the wrapper iff: the line is complete (valid JSON, ends in LF), its `prev` chain is unbroken from line 1, the referenced trace file has its final name and ends with `END`, the recomputed SHA-256 of that trace equals `trace_sha256`, and `golden_sha256` equals the pinned digest. Anything else — truncated line, broken chain, digest mismatch, trace without `END`, a `.partial` trace — is **unverified**: it is listed in the report as `U-UNVERIFIED-FAIL` (UNKNOWN), is non-authoritative, and can neither produce FAIL nor be deleted.

**Observer always runs.** After a subject-phase terminal abort the wrapper still runs the observer, so completed traces are judged and missing ones become UNKNOWN. (If the observer itself fails its pin check, the result is exit 3.)

**Final classification (wrapper, ordered; precedence `3 > 1 > 2 > 0`):**

| Step | Condition | Final result |
|---|---|---|
| 1 | Preflight failed, or a pin/environment/protection check failed before any case | **ENTRY_INVALID**, exit **3**; nothing else is reported as a result |
| 2 | At least one *verified* FAIL record exists (even if the run is otherwise incomplete) | **FAIL**, exit **1**; the report states `complete: true` or `complete: false` and lists all UNKNOWN reasons alongside |
| 3 | Otherwise, any of: no valid `COMPLETE` sentinel; any phase UNKNOWN; any missing/unexpected/incomplete trace; any unverified FAIL; any TERMINAL abort; reconciliation mismatch (below); production-boundary difference; a `NOT_COMPARABLE` repeat when a repeat was required | **UNKNOWN**, exit **2** |
| 4 | Otherwise, **all** hold: both subject and observer phases exit 0; `results.json.suite_verdict = PASS`; `coverage.required = observed = passed` and equals the pinned triple count; no `UNKNOWN`, no manifest-unlisted `NA`, no `NOT_RUN`; all artifact digests verified; production-boundary check passed (protection level recorded) | **PASS**, exit **0** |

**Reconciliation.** The wrapper maps `results.json.suite_verdict` to the expected observer exit (`PASS→0`, `FAIL→1`, `UNKNOWN→2`); a mismatch with the observed JVM exit is `U-RECONCILE` (UNKNOWN) unless step 2 already applies. A zero JVM exit with a missing sentinel, a non-zero exit with a complete sentinel and PASS verdict, and any contradiction between phase exits and `results.json` all fall under step 3.

### 6.4 Failure classification

Determinate FAIL: `F-ARITH`, `F-ORDER`, `F-SET`, `F-WITNESS`, `F-ACCT`, `F-SCHED-VARIANT` (`H_inv` differs across schedules of one fixture), `F-EXTRA-DRAW` (any roll/candidate/event beyond the original, RNG count ≠ 0), `F-PHYS` (`physical_completed ≠ "0"`), `F-INPUT-REJECTED`, `F-INPUT-ACCEPTED`, `F-INPUT-CODE`, `F-NONDETERMINISM`, and detector FAILs `D-UNDETECTED` (unexpected pass), `D-MISIDENTIFIED`, `D-FALSE-POSITIVE`, `D-ALWAYS`.

UNKNOWN (never converted to PASS or FAIL): `U-ABORT-WORK`, `U-ABORT-TIME`, `U-ABORT-MEM`, `U-ABORT-EVIDENCE`, `U-ABORT-OTHER`, `U-ENV`, `U-OBSERVER`, `U-INCOMPLETE`, `U-MISSING-TRACE`, `U-UNEXPECTED-TRACE`, `U-UNVERIFIED-FAIL`, `U-RECONCILE`, `U-NA-UNLISTED`, `U-OUTCOME-UNMATCHED` (control output matches neither the clean golden nor its registered description), `U-NOT-COMPARABLE`.

Entry-invalid (exit 3): `P01-E-*` preflight codes (§4.7). **Advisory, never changes a verdict**: `A-BUDGET-NOTE` (clean subject work above the non-aborting envelope of §7.1). The `U-` prefix is reserved for UNKNOWN classes, which is why the note is not `U-BUDGET-NOTE`.

### 6.5 Negative-control registry (`controls-v1.json`) and applicability

Registry row: `id`, planted-defect text, `applies_to` (fixtures), `trace_level` (`SUMMARY`), `identification` (`SIGNATURE` or `OUTCOME_CLASS`), `must_fail` (all must fail), `must_pass` (all must pass), and `detect_on`. `detect_on` is `ALL_SCHEDULES`, or an **explicit schedule list** for arrival-dependent defects (N-CAND-ARRIVAL; N-ARRIVAL on schedules where arrival order differs from the golden order); on schedules outside an explicit list the mutant is *expected to coincide with the clean output*, and the registry says so, so that coincidence is never read as an undetected defect. A mutant is a selectable class of the same source tree, chosen only by registry id.

**NA application rule (revision 4, F2).** For a given triple, a `must_fail` or `must_pass` assertion whose manifest mode is `NA` is *vacuous*: it is skipped, counted in the detector record as `na_skipped`, and never turns into a failure or a pass. A control triple is judgeable only if at least one `must_fail` assertion (or, for an `OUTCOME_CLASS` control, at least one registered outcome class) is `REQUIRED` on it; a triple where nothing is judgeable is `U-NA-UNLISTED` (UNKNOWN). Checked on the G-2 artifacts: every triple of every control has at least one REQUIRED `must_fail`, except the outcome-class control N-FIXED64, which has none by design. `N-QUALIFIED-DENOM` and `N-NINTH` list `ASR-ARITH-TOTAL` in `must_pass`; it is vacuous on their 52 NOFIT-01/LOG triples and binding on their 24 NOFIT-01 graph triples. `must_fail` lists that the G-2 registry marks provisional are finalized in the golden file (`negative_expectations`), not in `controls-v1.json`.

**`OUTCOME_CLASS` definitions.**

* **N-FIXED64** (audit M7). The mutant uses Java `long` numerators/denominators only (static audit: no `BigInteger` in the class). Applicable fixture: BIG-NEAR-01, where the exact difference `1/(5·2^79)` and the component `50+1/2^80` cannot be represented in 64 bits, so a *correct* result cannot be produced. Accepted outcome classes: **(UA)** an explicit `UNSUPPORTED_ARITHMETIC{op, case_no}` record, no `DECISION` records, `END` present ⇒ identified as "bounded arithmetic reported"; the control's *property* verdict is UNKNOWN (exactness unverified), its **detector verdict is PASS**; **(WR)** completion with `DECISION`/`SET` records that differ from the golden in `ASR-ARITH-TOTAL`, `ASR-ORDER` or `ASR-FINAL-SETS` ⇒ identified as silent wrap/rounding, detector PASS; **(OK)** completion equal to the golden ⇒ `D-UNDETECTED` (detector FAIL; cannot occur without exact arithmetic, so it indicates a mislabeled mutant); anything else (no `END`, malformed, neither UA nor WR) ⇒ `U-OUTCOME-UNMATCHED`. NEAR-01 is deliberately not an applicable fixture (it fits in 64 bits).
* **N-NINTH**: counters show 27 fit checks instead of 24, or unit `d` eligible/rolled; must_fail `ASR-STATUS`, `ASR-COUNTERS`; must_pass `ASR-ARITH-*`.
* **N-NEW-EPOCH**: replay produces a second decision/roll/receipt event; must_fail `ASR-REPLAY`, `ASR-NOEXTRA`.
* **N-MISSING-MEANS-EMPTY** (revision 5): in the source-loss subcase the unit `a` whose roll evidence was removed is treated as unrolled (or a lost-evidence field is reported empty); classes `REROLLED_AS_UNROLLED`, `UNKNOWN_REPORTED_AS_EMPTY` (§5.2); must_fail `ASR-REPLAY`, `ASR-NOEXTRA` (unchanged).
* **N-ERASE-HISTORY**: modeled success becomes `0` or `a` is retired solely for the edit; must_fail `ASR-ACCT-SETS`/`ASR-COUNTERS`; either alternative is an accepted class.

**Applicability and session derivation** (every control has ≥ 1 detecting fixture; sessions are all schedules of the listed fixtures, used for the ceiling in §7.1):

| Control(s) | Fixtures | Sessions | Ident. |
|---|---|---|---|
| N-INNER-WEIGHT | ARITH `A-LIST`,`A-REV` (V3,V4) | 2 | SIG |
| N-EARLY-FLOOR | all 5 ARITH sessions (V2) | 5 | SIG |
| N-RELATIVE | 3 FIXED-01 sessions | 3 | SIG |
| N-CAND-COMPONENT, N-CAND-ARRIVAL | CAND-01 | 2 + 2 | SIG |
| N-CROSS-C-FIRST, N-CROSS-SEED | CAV-01 | 2 + 2 | SIG |
| N-C-FIRST / N-T-FIRST | EQUAL-3-C / EQUAL-3-T | 12 + 12 | SIG |
| N-ROUND-2 | NEAR-01, BIG-NEAR-01 | 4 | SIG |
| N-FLOAT64 / N-FIXED64 | BIG-NEAR-01 | 2 + 2 | SIG / OC |
| N-ID-STRING | ID-ORDER-01 | 48 | SIG |
| N-REJECTED-VETO | CHAIN-3, CHAIN-ID-01, CHAIN-PROT-01, CYCLE-01 | 84 | SIG |
| N-ID-FIRST | CHAIN-3, CHAIN-ID-01, CYCLE-01 | 72 | SIG |
| N-PROTECTION-IGNORE | CHAIN-PROT-01 | 12 | SIG |
| N-OMIT-ENDPOINT, N-LOCAL-CAP | all 36 LONG EXT (70 sessions per family) | 140 + 140 | SIG |
| N-TIE-ID-DESC | LONG-TIE (BASE+EXT) | 134 | SIG |
| N-ARRIVAL | LONG-SCORE + LONG-TIE (explicit `detect_on`) | 268 | SIG |
| N-UNKNOWN-EMPTY | LATE-01 | 48 | SIG |
| N-QUALIFIED-DENOM, N-NINTH | NOFIT-01 graph + NOFIT-01/LOG | 76 + 76 | SIG / OC |
| N-ROLL-FIRST | NEG-ROLL-FIRST-01 | 8 | SIG |
| N-ACCEPT-IS-COMPLETE, N-POSTROLL-NOFIT, N-REROLL, N-IGNORE-P, N-LOGICAL-IS-PHYSICAL, N-ERASE-HISTORY | ACCT-BASE, -INVALID, -RETIRE, -PROTECTED, -MODEL-SUCCESS, -MODEL-EDIT | 16 each = 96 | SIG / OC |
| N-ADDITIVE | ACCT-DUP-LOG, ACCT-DUP-RECEIPT | 2 | SIG |
| N-NEW-EPOCH | ACCT-REPLAY cuts | 1,086 | OC |
| N-MISSING-MEANS-EMPTY | ACCT-LOSS (4) | 4 | OC |

33 controls (the 32 named in spec §3–§4 plus PROPOSED N-ARRIVAL, cited in spec §3.3 prose) total **2,344 sessions**. The three families required by the task remain: *score-first vs ID-first* (N-ID-FIRST, N-ID-STRING; must_fail `ASR-ORDER` and `ASR-FINAL-SETS` on CHAIN-ID-01), *component-first vs total-score-first* (N-CAND-COMPONENT, N-CROSS-C-FIRST, N-C-FIRST, N-T-FIRST, N-INNER-WEIGHT, N-EARLY-FLOOR, N-ROUND-2, N-FLOAT64, N-FIXED64, N-RELATIVE), and *incorrect greedy blocking* (N-REJECTED-VETO, N-PROTECTION-IGNORE, N-IGNORE-P, N-OMIT-ENDPOINT, N-LOCAL-CAP, N-UNKNOWN-EMPTY, N-TIE-ID-DESC, N-ARRIVAL).

### 6.6 Required assertions (observer; maps to spec §5 items 1–5)

| Assertion ID | Content | Spec §5 | Applies to |
|---|---|---|---|
| ASR-ACCEPT | Loader accepts every pinned valid fixture (no `REJECTED`) | — | all main records |
| ASR-REJECT-CODE | Invalid file rejected with exactly the golden code, before scoring | — | invalid-input files |
| ASR-ARITH-COMP / ASR-ARITH-TOTAL | Exact reduced component and total rational strings; no clipping | 1 | ARITH, GRAPH, LONG |
| ASR-INPUT-UNCHANGED | Subject's echoed digest of the fixture record equals the pinned fixture digest | 1 | all |
| ASR-STATUS | Qualification/fit/roll statuses preserved; failed-fit and roll-loss never in the winner pass; ≤ 8 checks per unit before exactly one roll | 2 | GRAPH, ACCT |
| ASR-CAND-ORDER / ASR-CAND-SELECTED | Candidate priority order and selected slot/seed rank | 3 | CAND-01, ROLL-01 |
| ASR-ORDER | Exact full evaluation order (descending exact S, then full ID) | 4 | GRAPH, LONG, LATE |
| ASR-DECISION / ASR-WITNESS / ASR-WITNESS-SET | Per-vertex decision; witness is an earlier *accepted or protected* neighbor joined by a declared edge, never a rejected vertex | 4 | GRAPH, LONG |
| ASR-FINAL-SETS | accepted / spacing-rejected / protected-rejected / pending sets | 4 | GRAPH, LONG, LATE |
| ASR-SCHED-INVARIANT | One `H_inv` across all schedules of a fixture | 3,4 | all |
| ASR-COUNTERS / ASR-ACCT-SETS | Logical counters and fractions (2/3, 1/3 — not rates); accounting sets | 5 | ACCT |
| ASR-EVENT-IDEM / ASR-REPLAY | Duplicate no-op; contradiction ⇒ UNKNOWN+frozen; prefix/cut replay equals literal set membership | 5 | ACCT, NEG |
| ASR-NOEXTRA | RNG calls = 0, no extra roll/candidate/reward event, key/reward events = 0 | 2,5 | all |
| ASR-NOPHYS | `physical_completed = "0"` always; a model receipt is never upgraded | 5 | ACCT |
| ASR-COMPLETE | Trace final-named with `END`; interrupted trace not treated as a result | 4 | all |
| ASR-LATE | OPEN ⇒ `DEFER_GRAPH`, finalized ∅, pending {A,B,C,D}; REVEAL ⇒ final trace | 4 | LATE-01 |

### 6.7 Completeness, unexpected passes and subject/observer separation

* **Completeness from the frozen manifest (audit M5).** `expected-traces-v1.json` lists every required triple `(subject, case_no, schedule_no)` with trace path, trace level, and its assertion list where each assertion is `REQUIRED` or `NA` + reason. The observer iterates **this list, never the directory listing**. For each triple: missing file ⇒ `U-MISSING-TRACE`; `.partial`, no `END`, header mismatch, or digest mismatch ⇒ `U-INCOMPLETE`; a trace present on disk but not in the manifest ⇒ `U-UNEXPECTED-TRACE`. `NA` is legitimate only where the manifest says so; an assertion the observer skips otherwise is `U-NA-UNLISTED`. The manifest is derived from inputs + schedules + controls by the materializer, independently re-derived by the Python verifier at G-2, and pinned; the preflight checks its fixture set against `inputs-v1.json`. `results.json.coverage` must satisfy `required = pinned triple count` (clean 1,800 plus control sessions); PASS requires `observed = passed = required`.
* **Unexpected pass.** For each (control, applicable fixture, `detect_on` schedule) the observer applies §5.3: if every `must_fail` assertion passes (the mutant behaved like the correct subject) ⇒ `D-UNDETECTED`, detector FAIL, exit 1; if only some `must_fail` fail, or a `must_pass` assertion fails ⇒ `D-MISIDENTIFIED`; if the output matches neither the clean golden nor the registered description ⇒ `U-OUTCOME-UNMATCHED`; a clean case flagged ⇒ `D-FALSE-POSITIVE`. A detector verdict that is constant across inputs ⇒ `D-ALWAYS`. A missing or incomplete mutant trace is UNKNOWN, so omitting a control can never yield PASS.
* **Separation (enforceable).** (1) Different JVM invocations; the subject command line contains no goldens path and no control expectation (§6.1); (2) the observer reads subject *files* only; packages `subject` and `observer` have no dependency on each other, checked with `jdeps -verbose:class`, and share only the `json` syntax reader and JDK SHA-256; (3) the observer verifies `pins` and the goldens digest before opening any trace and records that order; (4) the observer performs no rational arithmetic beyond the "reduced" validity check.

### 6.8 Atomic artifact publication, digest coverage and final sentinel (audit M9)

1. **Atomic publish.** Every artifact except `fails.jsonl` is written to `<name>.partial` in its final directory, flushed and forced, then published with `Files.move(…, ATOMIC_MOVE)`; the directory is forced best-effort. This claims safety against **process kill and JVM abort only**; power-loss durability is not claimed (consistent with spec §6.5/G08). Trace files stream into `<name>.partial` and are renamed when `END` has been written, so a final-named trace is always whole.
2. **Artifact publication graph (acyclic; closes C1).** The run directory contains only `subject/`, `observer/`, `wrapper/` and `COMPLETE`. Each artifact is hashed by exactly one *later* artifact, never by itself and never by an artifact written earlier than it: (a) the subject phase writes `subject/` and hashes nothing; (b) the observer writes its files in `observer/` and **last** `observer/results.json`, which embeds `{path, bytes, sha256}` for every regular file under `subject/` and `observer/` **except `results.json` itself**; no listed file is created after `results.json`, and the observer's own console output is captured by the wrapper into `wrapper/`, not into `observer/`; (c) the wrapper writes everything under `wrapper/` and **last** `wrapper/wrapper.json`, which embeds `{path, bytes, sha256}` for every regular file under `wrapper/` **except `wrapper.json` itself**, plus the digest of `results.json`, the exit codes of all phases, the production-protection level and the pre/post production-manifest digests; all phase console files are closed before `wrapper.json` is written; (d) `COMPLETE` is excluded from every earlier manifest and holds `results_sha256`, `wrapper_sha256` (so the two self-excluding manifests are themselves covered) and `tree_sha256`, the SHA-256 of the lines `<relative path> TAB <sha256> LF`, sorted bytewise by path, for **every regular file under the three directories** (`COMPLETE` itself excluded). Dependency order: subject files → observer files → `results.json` → wrapper files → `wrapper.json` → `COMPLETE`. **Final verification** (the wrapper before writing `COMPLETE`, and independently `verify_run.py` afterwards): enumerate the files on disk; require nothing outside the three directories except `COMPLETE`; require `results.json`'s list = disk(`subject/` ∪ `observer/`) minus `results.json` with equal digests; require `wrapper.json`'s list = disk(`wrapper/`) minus `wrapper.json` with equal digests; recompute `results_sha256`, `wrapper_sha256`, `tree_sha256` and compare. Extra, missing or `.partial` files are anomalies (`U-INCOMPLETE`), except `.partial` traces already explained by a recorded abort. The wrapper's own console output is not evidence and is not hashed.
3. **Order of creation.** preflight artifacts → subject artifacts → observer artifacts → `results.json` → production post-run manifest and other wrapper files → `wrapper.json` → **`COMPLETE`**, always last, written by the wrapper via temp-and-rename. `COMPLETE` contains `{schema, results_sha256, wrapper_sha256, tree_sha256, final_verdict, final_exit, protection}`. Its absence means the run is incomplete regardless of any other file. `COMPLETE` records the verdict; it does **not** itself mean PASS.
4. **Wrapper integrity.** `run-probe.sh` is POSIX `sh` using only `mkdir`, `find`, `sha256sum`, `timeout`, `git`; its digest is in `runner.sha256` and in `wrapper.json`. An independent post-run script `verify_run.py` (Python stdlib, reads only the run directory) recomputes digests and the classification of §6.3 for reviewers; disagreement with the wrapper is a review finding.


## 7. U05 — resource budgets and enforcement

**No limit below is approved.** "Spec proposal" restates the exploratory ceilings of execution spec §6.5; "Plan proposal" is this plan's PROPOSED value. Acceptance of any value is gate G-6.

### 7.1 Limits and enforcement

| Budget | Spec proposal | Plan proposal (**PROPOSED**) | Basis (static estimate, unmeasured) | Enforcement in a later authorized run |
|---|---|---|---|---|
| Vertices / edges / candidates per unit / rational bits | 4096 / 16384 / 8 / 4096 | unchanged | Largest selected path 1026 vertices / 1025 edges (≈ 4× vertex headroom) | Loader checks (`-CAP-*`, `-INT-BITS`) and preflight cardinality check |
| File sizes (audit minor) | — | `inputs`, `goldens`, `schedules`, `expected-traces`: ≤ 16 MiB each; each invalid file ≤ 64 KiB | LONG fixtures alone recomputed at 5,566 vertex records × 800–1,300 B = **4.2–6.9 MiB**; other families < 1 MiB. Revision 1's 8 MiB cap left no margin; 16 MiB gives ≥ 2× | Preflight file-size check (exit 3) |
| Heap use of parsing | — | Streaming pull parser holds one fixture record (largest ≈ 1 MiB text); no whole-file DOM | Avoids DOM overhead scaling with file size | Static review (G-5); `-Xmx256m` |
| **Session count** | (enumerated at U02) | Clean subject **1,800** (§4.8). Control sessions **2,344** (§6.5). **Derived upper bound 4,144.** **Ceiling PROPOSED: 5,000** (≈ 20% above the derivation) | Sum of per-control applicable sessions assumes every control runs on all schedules of its fixtures | Preflight: the runplan and manifest triple count must equal `pins`; more than the ceiling ⇒ exit 3. Adding a control or fixture requires re-derivation, not a silent raise |
| Deterministic work per case/schedule | 10,000,000 counted operations | 10,000,000 hard abort. Non-aborting **advisory** envelope for the clean subject: ≤ `64·V·⌈log₂(V+1)⌉ + 16·E + 64·V`; a breach records `A-BUDGET-NOTE` and changes no verdict | Clean greedy ≲ 150 K ops at V=1026 ⇒ ≳ 60× headroom | `WorkMeter` throws `WorkBudgetExceeded` at the crossing increment (case-local, §7.3) |
| Heap | `-Xms32m -Xmx256m` | unchanged; plus AM-1 metaspace/direct-memory caps | Structural caps keep live data to tens of MiB | JVM flags (hard) |
| **Evidence size (audit M6)** | 256 MiB per run | Trace levels (§6.2): `FULL` for the clean subject, `SUMMARY` for mutants. **Per-run ceiling PROPOSED 512 MiB**, per trace file 64 MiB. **Pre-run static estimate** must be ≤ 50% of the ceiling (256 MiB) | Pessimistic: LONG clean FULL 44,480 vertex-schedule instances × 1,200 B = 53 MB; LONG controls SUMMARY (N-OMIT-ENDPOINT + N-LOCAL-CAP 22,324 each, N-TIE-ID-DESC 22,240, N-ARRIVAL 44,480 instances = 111,368 instances × 550 B after the F3 correction that keeps `SCORE`/`EVENT_DISPOSITION`) = 61 MB; other families ≤ 35 MB (assumed); total ≈ **149 MB** (≈ 29% of 512 MiB; still below the 50% pre-run line of 256 MiB). Revision 1's "≈ 10–100 MB" and 256 MiB ceiling were not supported: with FULL traces for every mutant the estimate reached ≈ 240–356 MiB | Wrapper computes the estimate from the manifest (Σ instances × per-level bytes) and refuses to start if above the 50% line (exit 3). Byte-counting trace writer: exceeding the ceiling is a TERMINAL abort `U-ABORT-EVIDENCE` (§7.3), never truncation |
| Wall-clock | 60 s per case/schedule (host guard) | 60 s per case/schedule (in-JVM watchdog); phase budgets: subject ≤ 1,200 s, observer ≤ 600 s, **total 1,800 s** | Expected CPU ≪ 1 minute | Watchdog thread plus wrapper `timeout --signal=KILL`; a wall-clock expiry yields **UNKNOWN only** |
| Repeat-run determinism | — | One repeat `run-0002`, compared on `H_full` only if both runs are complete without TIME/MEMORY aborts (§5.6) | Doubles cost, still small | Wrapper / `verify_run.py` comparison; otherwise `NOT_COMPARABLE` ⇒ UNKNOWN |

### 7.2 Work-unit accounting (deterministic)

All subject arithmetic and comparison goes through one kernel type (`ExactRational` / `WorkMeter`). One unit is added per call in exactly one category and **nested calls are counted, not hidden**: `RAT_ADD`, `RAT_MUL`, `RAT_CMP`, `PRIO_CMP` (comparator invocation; its internal `RAT_CMP` units are counted separately), `ADJ_PROBE`, `EVENT_APPLY`, plus recorded-but-uncapped `PARSE_DIGITS` and `GCD_STEPS`. The cap applies to the sum of the capped categories. Counters reset per case/schedule and are written into the trace header and `resource-counters.json`. **They are not part of `P_inv`/`H_inv`**: sort comparisons and adjacency probes legitimately depend on delivery order. They are part of `H_full`, which is compared only between repeat runs of the same schedule. Because mutants share the kernel, their counters are comparable; a non-cooperating subject is caught by the wall-clock and heap backstops. That every arithmetic path routes through the kernel is a static review item (UB-06).

### 7.3 Abort behavior: case-local versus terminal (audit M4)

| Class | Triggers | Behavior |
|---|---|---|
| **Case-local** | `WorkBudgetExceeded` only (deterministic, JVM healthy) | The case is marked UNKNOWN `U-ABORT-WORK`; its trace is closed as `.partial` with an `ABORT` record (never renamed to a final name); the runner continues with the next triple in frozen order. No result-sensitive cutoff. |
| **TERMINAL** | `OutOfMemoryError`, `StackOverflowError`, any other unexpected `Throwable`, per-case wall-clock expiry, evidence-ceiling exhaustion | **Ordinary execution does not continue.** The handler uses only state pre-allocated at start-up (an open `FileChannel` to the phase's `abort.marker` and a pre-built byte array of fixed size holding `{phase, kind, case_no, schedule_no}`), makes one best-effort write, then calls `Runtime.halt`. No further case, no catch-and-continue after an OOM, no new object graph. All remaining triples are `NOT_RUN`. |
| Outer kill | wrapper `timeout --signal=KILL`, OS kill | No in-process record is possible; the wrapper records `U-ABORT-TIME`/`U-ABORT-OTHER` from the absence of `END`/`COMPLETE` and the phase exit status. |

After any terminal abort: the abort record is evidence of UNKNOWN only. Missing triples are `U-MISSING-TRACE` (UNKNOWN). The observer still runs on the completed traces (§6.3), so a determinate FAIL already established stays FAIL; nothing aborted is ever PASS. A `.partial` trace left by an abort is retained as R1 diagnostic evidence and is never authoritative. The UNKNOWN raised by a deliberately fail-closed input (e.g. ACCT-LOSS source-evidence loss) is an *expected disposition* of a completed case and is not an abort (spec §7). A clean determinate case that aborts means the limit is judged insufficient ⇒ STOP; revise under review before any new run. Hard cases are never dropped, retried, or "completed by prefix".

### 7.4 Evidence retention policy (PROPOSED)

| Class | Contents | Retention |
|---|---|---|
| **R0 (compact, always kept)** | `COMPLETE`, `run-manifest.json`, `results.json`, `observer.json`, `negative-controls.json`, `fails.jsonl`, `wrapper.json`, `resource-counters.json`, `semantic.sha256`, `repro.sha256`, `input.sha256`, `expected.sha256`, `runner.sha256`, production manifests/digests, `env.txt` | Kept with the run; only R0 is a candidate for repository inclusion, under separate approval |
| **R1 (traces)** | `subject/traces/**`, `.partial` diagnostics, model replay snapshots | Kept until independent review accepts the run; never deleted for a FAIL/UNKNOWN run until the owner accepts the diagnosis |
| **R2 (console)** | `stdout.txt`, `stderr.txt` | Kept with R0; `stderr` is expected empty under `env -i` — any content is recorded |
| Disposal | The disposable `$P01_OUT` tree lives only for the container/session; durable retention outside it requires an authorized copy step. Nothing is deleted autonomously. | |


## 8. Remaining UNBOUND items and execution-evidence items

| ID | Item | Why static documentation cannot resolve it | Needed to bind |
|---|---|---|---|
| UB-01 | JDK 25.0.4.1 availability and byte identity | Only JDK 21 exists in the present sandbox and `/workspace` is absent; the spec's hashes were recorded elsewhere. **Setup work, not an architectural blocker**: the repository's CI already provisions Temurin 25. JDK 21 is never silently substituted | Provision the pre-staged archive; verify archive, `bin/java`, `release` digests; or an approved spec amendment to another runtime |
| UB-02 | Materialized `inputs-v1.json`, `controls-v1.json`, `expected-traces-v1.json`, `runplan-v1.json`, `invalid-inputs-v1/`, fixture index and their SHA-256 | Nothing authored; no hash exists or is claimed | Authorized materialization + independent comparison (G-2) |
| UB-03 | Materialized `goldens-v1.json` (Tier A literals, Tier B generator output) and review record | Not authored | G-3 |
| UB-04 | Enumerated `schedules-v1.json` and recount of §4.8 (1,800 clean) and §6.5 (2,344 control) sessions | Counts are hand-derived proposals | G-2 recount by the independent Python verifier |
| UB-05 | Runner/subject/mutant/observer/preflight source, jar, wrapper, `runner.sha256` | Nothing implemented | G-5 |
| UB-06 | Static proof that all arithmetic routes through `WorkMeter`; counter category mapping; anti-hardcoding scan | Needs source | G-5 review |
| UB-07 | Owner acceptance of §7.1 limits (none approved) | Policy decision | G-6 |
| UB-08 | Approval of AM-1…AM-8 spec-command amendments (AM-7/AM-8 are new in revision 2) | Changes the exact proposed command and artifact set | G-1 |
| UB-09 | Reviewer acceptance of PROPOSED test-only additions: `PROTECTION_EXCLUDED` event, `NEG-CONTRADICT-01`, `NEG-ROLL-FIRST-01`, the 29 invalid-input cases, N-ARRIVAL, `P01-MODEL-STATE-1`, trace levels, `preflight` phase, `OUTCOME_CLASS` definitions | Derived from but not literally stated in the spec | G-2 |
| UB-10 | Python 3 version in the execution environment (3.13.18 here, unverified there) | Environment-dependent | G-1 |
| UB-11 | Independent reviewer identity; separate oracle and subject authors | Organizational | G-1 |
| UB-12 | `$P01_ROOT`, `$P01_OUT`, run user, branch name, read-only mount | Environment-dependent. The sandbox mechanism and run identity must satisfy §3.3 (`ENFORCED_RO_MOUNT` default); if none is available, protection is reported `NOT_ENFORCED` | U06 |
| **U06** | Execution permission | Separate owner authorization (unchanged) | Owner |

**Items that need actual execution evidence (cannot be settled on paper):** EV-1 clean-subject work counters against the §7.1 advisory envelope; EV-2 heap and trace sizes against the §7.1 estimates; **EV-3 JVM exit codes under OOM, `Runtime.halt` and `SIGKILL`** (including the claim in AM-5); EV-4 `jdeps --print-module-deps` prints exactly `java.base` and `jdeps -verbose:class` shows no subject↔observer dependency; EV-5 repeat-run `H_full` equality; EV-6 the mount-flag, capability and write-denial probes and the production manifests under the real execution identity; EV-7 atomic-move and `.partial` behavior on the actual file system; EV-8 wall-clock headroom of the 60 s / 1,800 s limits.

Not UNBOUND but unprovable by design: nothing in Probe 01 can prove natural-cave completeness, a universal bound for transitive greedy closure, actual fit/roll production, publication atomicity or physical completion (spec §1 exclusions unchanged).

## 9. Explicit approval gates

Sequential; none is satisfied by this document. Each requires an explicit, separately recorded authorization.

| Gate | Authorizes | Entry conditions |
|---|---|---|
| **G-0** | Documentation commits of this plan (revisions 1–3) | Baseline §1 verified — done |
| **G-1** Plan acceptance | Accept/reject §3–§7 contracts; AM-1…AM-8; JDK/Python choice; reviewer identity; separate oracle/subject authors (UB-01, -08, -10, -11) | Independent review of revision 3 and a written owner authorization record against the approval matrix in the G-1/G-2 preparation record (not yet issued) |
| **G-2** Input/schedule materialization | Create `inputs-v1.json`, `schedules-v1.json`, `controls-v1.json`, `expected-traces-v1.json`, `runplan-v1.json`, `invalid-inputs-v1/` (text only, no probe execution) and compare to spec tables; the Python verifier recounts §4.8/§6.5 and proves manifest ⊇ inputs; accept UB-09 additions | G-1 authorization record issued; procedure in the G-1/G-2 preparation record, Part C |
| **G-3** Oracle first | Create and review `goldens-v1.json` (+ Python generator). Ancestry-ordered before any subject code. The subject author's workspace excludes the oracle directory (sparse checkout); where that is not achievable the exposure is recorded as `oracle_exposure` in `run-manifest.json` | G-2; independent reviewer |
| **G-4** Freeze | Compute digests, write `pins-v1.json`/`*.sha256` | G-3 |
| **G-5** Implementation | Author the reference subject, mutants, preflight, observer, wrapper, `verify_run.py` in `tools/st05-p0-probe-01/`; build the jar in `$P01_ROOT`; static audits (`jdeps`, WorkMeter coverage, anti-hardcoding scan, no `BigInteger` in N-FIXED64) — **still no probe run** | G-4 |
| **G-6** Budgets | Accept §7.1 values (or edited values) | G-5 review |
| **U06** Execution | Exact command, artifacts, environment, protection level (including whether `NOT_ENFORCED` is acceptable), budgets, output location | G-1…G-6 closed; owner decision |

Implementer-selectable without owner approval (stay inside the contracts above): Java package layout, JSON reader internals, `WorkMeter` implementation, wrapper scripting details, file layout within the named directories, counter increment sites. Merging, moving checkpoints, modifying `main`, and advancing Stage 1/A02/A03 are never implied by any gate (AGENTS.md §3, §8).

## 10. STOP conditions

Stop and report (do not substitute, guess or continue) if any of: the design branch/pin or any pinned authority blob changes incompatibly; a required authorization or gate is absent; goldens are generated from, or influenced by, subject output, or a golden is edited after a subject exists; canonical ID order or candidate seed order is silently replaced; an excluded measurement transform, placement distance, Minecraft/Fabric API, world path or production class is introduced; a limit causes graph truncation or a result-sensitive cutoff, or a clean determinate case aborts; a missing roll/event becomes a fresh decision; the output path resolves inside the production checkout or a world directory; the production manifests differ before/after a run, or their traversal reports any error; production write protection is `NOT_ENFORCED` and U06 did not accept that level; the JDK or Python identity differs from the recorded tuple; `jdeps` reports any module other than `java.base`; the manifest triple count, fixture set or session count differs from the pins; a verified FAIL record is discarded, overwritten or reclassified; production, registry, controller or reward behavior is requested; execution is requested while any applicable UB item or gate is open; the write-protection proof of §3.3 fails or its denial probe unexpectedly succeeds; the artifact graph of §6.8 is cyclic or a file exists outside the three run directories and `COMPLETE`; or package/Stage 1 progression is implied. Repeated deterministic failures follow AGENTS.md §4 (three bounded repair attempts, then the Repair Failure Handoff).

## 11. Readiness verdict

**BINDING_PLAN_READY** (revision 2).

For U01–U05 the document gives concrete, individually reviewable contracts — subject identity and a three-layer production boundary, byte-exact schemas and error codes, deterministic schedule rules with recomputed counts, an independence-constrained oracle, a manifest-driven runner/control design with durable-FAIL, terminal-abort, atomic-publication and sentinel rules, and limits with a named enforcement mechanism each. What remains (§8) is materialization, hashing, implementation, environment provisioning, execution evidence and owner acceptance; those block **execution**, not the contracts. Independent review should specifically challenge: the AM-7/AM-8 amendments and trace levels, the `OUTCOME_CLASS` definitions (N-FIXED64 in particular), the verified/unverified FAIL rule, and the recomputed counts. U06 remains a separate owner authorization gate. **A02 BLOCKED. A03 BLOCKED. No probe execution authorized. No `PROBE_01_PASS`.** The reference subject is a standalone test subject written from the specification and is never an A02 implementation.

## 12. Revision record (revision 2 over `8f9580da895242a110d31fa6f83773931b28a8e8`)

| Finding | Closed in | Summary |
|---|---|---|
| M1 | §5.6, §6.6, §7.2 | `P_inv`/`H_inv` excludes arrival order, work counters and delivery counts; `H_full` is compared only for the same schedule across repeat runs |
| M2 | §3.3 | Ignored-path coverage (`--ignored`), recursive filesystem manifest by the wrapper, `find -writable` prevention check, root ⇒ `NOT_ENFORCED`, level reported in every report header |
| M3 | §6.3 | Hash-chained `fails.jsonl`; verified vs unverified records; FAIL beats UNKNOWN only when verified; observer always runs |
| M4 | §7.3 | Case-local (`WorkBudgetExceeded`) versus terminal (OOM, watchdog, evidence cap) abort; pre-allocated abort record, `halt`, remaining `NOT_RUN` |
| M5 | §6.7, §4.9 | Frozen `expected-traces-v1.json` drives the observer; missing/extra/incomplete traces and unlisted `NA` ⇒ UNKNOWN; preflight set-equality with inputs |
| M6 | §6.2, §7.1 | `FULL`/`SUMMARY` trace levels; ceiling 512 MiB PROPOSED with a pre-run 50% estimate check and byte-counting writer |
| M7 | §5.3, §6.5 | `OUTCOME_CLASS` for N-FIXED64: UA / WR accepted, OK ⇒ `D-UNDETECTED`, anything else UNKNOWN; BIG-NEAR-01 only |
| M8 | §4.7 | Neutral preflight (exit 3) vs subject loader (`F-INPUT-REJECTED` is a subject FAIL) vs 29 invalid-input files |
| M9 | §6.8, §6.3 | Temp-and-rename publication, complete digest coverage, `COMPLETE` last, wrapper reconciliation and `verify_run.py` |
| MINOR | as listed | Input ceiling and inventory (§4.9, §7.1); ROLL-01/NOFIT-01 scope (§4.8 rule 7); rational conversion (§4.2); path escaping (§4.3); invalid categories (§4.7); `A-BUDGET-NOTE` (§6.4); must-fail/must-pass (§5.3, §6.5); output ownership (AM-7, §6.1); wrapper hashing and exit aggregation (§6.3, §6.8); anti-hardcoding (§6.2); authorship (§5.1 item 7, G-1/G-3); repeat only when complete (§5.6); streaming parse (§4.3, §7.1); N-NINTH separation (§4.7, §6.5); session ceiling derivation (§7.1); JVM exit-code claims marked unverified (AM-5, EV-3) |

Recomputed counts: clean **1,800** (was 1,725), control sessions **2,344**, derived upper bound **4,144**, PROPOSED ceiling **5,000**; main-file fixture records **78**, invalid-input files **29**.


### Revision 3 record (over `05c357e0162f8593aa811c4ed2c759d10ea2335d`)

| Item | Closed in | Summary |
|---|---|---|
| C1 | §6.8 items 2–3, §5.6 evidence set | Acyclic publication graph: `results.json` hashes `subject/`+`observer/` minus itself; `wrapper.json` hashes `wrapper/` minus itself plus `results.json`; `COMPLETE` (excluded from all earlier manifests) carries `results_sha256`, `wrapper_sha256` and `tree_sha256` over the whole tree; explicit final-verification steps; console output of the observer lives in `wrapper/` |
| C2 | §3.3 | `find -writable` demoted to a diagnostic; protection levels `ENFORCED_RO_MOUNT` / `ENFORCED_PERMISSION` / `NOT_ENFORCED`; verification under the execution identity (mountinfo `ro`, `CapEff`, `statvfs`); conditional write-denial probe, canary-first; production untouched in this task |
| G-1/G-2 preparation | [ST05_STAGE0_P0_PROBE_01_G1G2_PREP.md](ST05_STAGE0_P0_PROBE_01_G1G2_PREP.md) | Approval matrix (Part B) and materialization procedure (Part C); no artifact created or frozen |

### Revision 4 record (over `4ce5e23992abbfb3fabe274f6c22765f24fd635b`)

| Item | Closed in | Summary |
|---|---|---|
| F1 | G-2 artifacts, §4.7 | LATE-01 top-level `population_complete` is `true`; staged records evaluate POP-FLAG per snapshot |
| F2 | §6.5 | NA assertions are vacuous per triple; every control triple keeps at least one REQUIRED `must_fail` |
| F3 | §6.2, §7.1 | `SUMMARY` keeps `SCORE` and per-event dispositions; size estimate refreshed (≈ 149 MB) |
| F4 | §4.7 | Single-code loader precedence in ten tiers; `ID-PREFIX-MIXED` outranks `EXTRA-KEY` |
| F5 | §4.5 | `EDGE-DUPLICATE` is the unordered-pair repeat; endpoint/list order is normalized, not an input error, no new code |
| F6 | §4.8 | Replay = apply `k`, round-trip state, deliver remainder, replay the complete log twice |
| G-2 FIX-2 | controls registry | While preparing control expectations, two `detect_on` entries were corrected: `N-RELATIVE` is detected on FIXED-01 sessions 1 and 3 only (on `[V0,V2,V1]` min/max renormalization coincides with the correct output) and `N-MISSING-MEANS-EMPTY` on the two SOURCE sessions only (its defect needs missing source evidence). Session counts unchanged |
| LOSS-SOURCE | §5.2 | Authority wording is ambiguous; certain expectations kept, set-valued expectations deferred for the two SOURCE fixtures only (**superseded by revision 5, owner decision R2**) |
| G-3 | §5.2 | Concrete golden schema; oracle sources under `tools/st05-p0-probe-01/oracle/`; goldens remain CANDIDATE and unpinned |


### Revision 5 record (over `285c7d40b515f327a72efc2d01ee8378842fecc5`)

| Item | Closed in | Summary |
|---|---|---|
| S1 LOSS-SOURCE = R2 (owner-approved) | §5.2 | Remove `a`'s `ORIGINAL_ROLL` (record 7) and dependent decision/projection/receipt records (10, 11, 12, and 13 for MODEL-SUCCESS); keep FIT evidence (1–6) and `b`,`c` histories (8, 9). Missing evidence never means unrolled and never authorizes a roll. SOURCE schedules deliver records `1–6,8,9`; SOURCE goldens now carry exact evaluable expectations with `UNKNOWN_EVIDENCE_LOST` fields |
| N-MISSING-MEANS-EMPTY | §5.2, §6.5 | Outcome classes redefined against the corrected SOURCE cases; oracle self-check classifies constructed mutants |
| NEG-CONTRADICT-01 | golden `deferred` | Uncertain `sets.rolled` moved into the fixture's deferred fields (`winners`, `roll_loss`, decision sets and other counters were already deferred) |
| Deferred-field assertion rule | §5.2 | `P01-DEFERRED-1`: excluded fields never PASS; no evaluable field ⇒ UNKNOWN (`U-ALL-DEFERRED`) |
| Checkpoint trace records | §6.2 | Minimum `REPLAY_CHECKPOINT`, `ROUNDTRIP`, `REPLAY_FINAL`, `LATE_CHECKPOINT` records |
| Unchanged | | S3's 24 deferred pending prefix states (`ACCT-RETIRE`, intermediate lifecycle), `NEG-ROLL-FIRST-01` decision-set deferral, all unrelated fixtures, schedules, counts (1,800 clean / 2,344 control / 4,144 triples) and invalid-input files |
