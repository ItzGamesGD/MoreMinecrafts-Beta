# ST-05-P0-PROBE-01-PREP — synthetic scoring and conflict graph execution specification

Specification: **P01-SYNTHETIC-1**, 2026-11-01. **DOCUMENTATION / STATIC ANALYSIS ONLY. NOTHING EXECUTED.**

**Readiness: PROBE_01_SPEC_INCOMPLETE.** The mathematical inputs, independent expected outcomes and negative-control signatures below are specified. A future runner, frozen input/golden artifacts, observer implementation and enforcement bindings do not yet exist on this baseline and are **UNBOUND** (§8). This is not `PROBE_01_PASS`, implementation permission or probe execution authorization. **A02 BLOCKED; A03 BLOCKED.**

## 1. Verified baseline, authority and scope

Repository: `ItzGamesGD/MoreMinecrafts-Beta`; documentation destination: `design/st05-worldgen-q01-q18-20261030`. Before drafting, the remote branch HEAD was **f9a4f870fcf28e60fb30dd3574df298bf8ee4d54**, exactly the requested oracle pin; tree `073012f021528d4f9cfddb713c074e683475a4fe`. The new specification path and Probe 01 runner were absent from that tree. The unrelated clean local `work` checkout was not used as the design baseline or changed. No baseline incompatibility was found. Recheck the authority hashes before any separately authorized implementation/execution; stop on an incompatible change rather than silently substituting a revision.

All paths in this table are pinned to that commit. Relative document links are navigational; the commit/blob identities below control the evidence baseline.

| Read source | Exact Git blob / relevant reference |
|---|---|
| [Integrated validation plan](ST05_STAGE0_INTEGRATED_VALIDATION_PLAN.md) | `6654b5af53ec600f8e45539265e7a1d4a6bb8b33`; A3/A4/A5, §§8.2/8.4, complete-population chain obligation, FT24/25 |
| [Oracle manifest P0-ORACLE-2](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/f9a4f870fcf28e60fb30dd3574df298bf8ee4d54/docs/design/ST05_STAGE0_P0_ORACLE_MANIFEST.md) | `f997308443aa6c2da348673349d91b4d5ff8d6cf`; §§3–4, CAND-01/CAV-01/CHAIN-ID-01/CHAIN-PROT-01/CHAIN-LONG-n/CYCLE-01/ROLL-01, §6 scope gate |
| [P0 API mapping](ST05_STAGE0_P0_API_MAPPING.md) | `265544a67c7a9826a2a84af9664713696d01984a`; exact runtime baseline and limitations; no Minecraft API is exercised by this proposed synthetic suite |
| [P0 feasibility gaps](ST05_STAGE0_P0_FEASIBILITY_GAPS.md) | `dce8c4b5e8f122bf3227a162727218da9292ded8`; finite-input complexity is separate from acquiring complete real caves/graphs |
| [WG-Q01–Q45 / FIX-01 A](ST05_WORLDGEN_OWNER_DECISIONS_Q01_Q18_2026-10-30.md) | `9b9331def0c041750018f43f919203ed8aa67006`; ≤8 fit checks before one75% roll; WG-Q33 within-cavern seed order; Q34–Q39 fixed-reference weights; Q41/Q43 accounting/budget policy |
| [A02-FIX-Q01–Q10 / A03-FIX-Q01–Q06](ST05_STAGE0_FIX_OWNER_DECISIONS_2026-10-31.md) | `735bca2a5af7bc4cb6e08cd72733dc72a0c7a2c7`; Q08 A accepted-only greedy; Q06 denominators; Q09 C/Q10 C research-only decisions; no same-cavern reroll/replacement after retirement |
| Current Q1–Q118 authority | `docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md`, blob `5d5e685c6d1e577419c485f0a7170e062f4f5def`; lifecycle/player/reward protections unchanged |
| Authority routing / repository instructions | `docs/CONTENT_AUTHORITY_INDEX.md` `f43f45f5a04ac06945de6ac1b29dcf40f053044d`; `docs/STRUCTURE_AUTHORITY_INDEX.md` `457bfad3451fc62904b3dcde6218437de70198dd`; `AGENTS.md` `05abb64ceeb2b84e25215e39eccfc4d89fdea94a`; no nested AGENTS files in the pinned tree |

This first proposed isolated investigation covers exact score arithmetic, supplied deterministic candidate ordering, supplied canonical-ID tie order, complete **synthetic** conflict graphs and pure event-set accounting. It is a preliminary exercise of selected A3/A4/A5 invariants, **not clearance of those work packages**. Their original DAG prerequisites and A5→I accounting prerequisite remain unchanged. Supplied qualification, fit, roll and identity labels replace their producers only as explicit synthetic inputs; they do not prove A1/A2, actual fit, RNG mapping or complete real-world populations.

Excluded: Minecraft/Fabric calls, real caves, segmentation/representative generation, raw geometric measurement transforms, spacing distances or geometric edge derivation, arena/module generation, protected voxel completeness, actual concurrency/storage recovery, publication atomicity, physical completion, mutation/player provenance, controller/reward delivery and Stage1. The manifest's RAW-L/H/J/M/G/W/R/D/A geometric transforms, PRI-01 route selection, OCC masks and FT00–FT23 runtime faults are **not selected**. Boundary arithmetic here uses only supplied 0–100 scores and the existing near-tie oracle. No new measurement transform or placement distance is adopted.

## 2. Common mathematical and graph binding

The component tuple is `(C,T,F,E)`; input submetric tuple where used is `(L,H,J; M,G; W,R; D,A)`. All are exact rational numbers. Approved weights remain:

`C=(4L+3H+3J)/10`, `T=(3M+2G)/5`, `F=(W+R)/2`, `E=(3D+2A)/5`, `S=(4C+4T+F+E)/10`.

An algebraically expanded independent reference for nine-submetric input is:

`S=(16L+12H+12J+24M+16G+5W+5R+6D+4A)/100`.

Weights sum to1 at both appropriate levels; this gives endpoints0/100 and the impulse values below without geometric normalization. No clipping, epsilon equality, decimal rounding or candidate-relative rescaling is allowed. Graph fixtures supply four components directly; raw submetrics there are **not applicable**, not invented measurements. Define the test-data shorthand **G(s)=(s,s,s,s)**, hence S=s exactly; this is an arithmetic constructor for supplied components, not a measurement transform.

**Complete graph conventions:** each fixture is its own namespace. Listed vertices/edges are the entire graph; undirected edges are unordered pairs, no self edges; no unlisted pair conflicts. Every graph vertex is a distinct **supplied** qualifying/fit-eligible unit with one frozen eligible candidate and original WIN unless a row overrides this. There are no protected sites unless explicitly listed. An edge is a synthetic conflict fact, never an inferred distance. No RNG is called to decide these outcomes.

**IDs and candidate order:** common test identity prefix is `(schema=1, seed=7, dimension="minecraft:overworld")`; a vertex with key x has full representative-coordinate suffix `(x,0,0)`. Signed numeric coordinate comparison is the proposed test order; compare complete tuples, never a stringified integer, arrival index or digest. Except explicit rows, x is the vertex's index in its declared input-label list. This does not choose a production representative/order/encoding under Q10 C. Each vertex's sole candidate has seed rank0. CAND-01 explicitly supplies two ranks. Seed7 identifies repeatable test data; it does not purport to implement an approved seed-to-candidate permutation or seed-to-roll function.

**Algorithm under test (IUT) contract:** for supplied raw values, evaluate components/totals exactly; within each unit choose highest eligible-candidate total, with supplied seed rank for exact ties. Receive the unit's single frozen roll only after its fit record is complete. For a complete winner graph, evaluate vertices by descending exact S then full canonical ID. Test each against supplied protected sites and already accepted vertices. A rejected vertex reserves no exclusion space. Output ordered `(vertex, decision, reason/witness)` records and canonicalized accepted/rejected sets. These are the rules to be checked, not a request to implement them now.

**Independent oracle contract:** immutable expected records are transcribed from the literal tables/closed forms in this specification and reviewed **before** seeing IUT output. The future observer compares IUT output against that separate expected file and checks listed edge witnesses. It must not call an IUT score/comparator/greedy/reconciliation function, generate goldens from the subject's output, or let a shared helper decide expected rankings. For raw scores use the expanded coefficient identity and literal impulse/golden values; for paths use the parity theorem; for finite small graphs use the explicit trace proofs. Independent means separate derivation/evidence, not simply a second entry point into the same code. No reference program/harness has been written in this task.

## 3. P1/P2 fixture inventory and independent expected results

Every selected fixture has a stable `P01/...` run ID below while retaining its source ID. Generated indices are decimal integers, not zero-padded lexicographic priorities. Arithmetic-only rows have graph vertices=∅, edges=∅, canonical order/qualification/roll=NOT_APPLICABLE, accepted=∅ and rejected=∅; they never create eligibility or placement events. Their processing order is the listed vector order. Graph rows use §2 defaults and the exact graph/tuple overrides below. A correct complete trace and final sets are both mandatory even when a wrong algorithm happens to produce the same unordered set.

### 3.1 Supplied arithmetic: P01/ARITH-V0–V11, source V0–V11

| ID | Complete supplied submetrics | Independently expected (C,T,F,E); exact S |
|---|---|---|
| V0 | (0,0,0);(0,0);(0,0);(0,0) | (0,0,0,0);0 |
| V1 | (100,100,100);(100,100);(100,100);(100,100) | (100,100,100,100);100 |
| V2 | (50,75,80);(75,75);(75,75);(50,50) | (133/2,75,75,50);691/10 |
| V3 | (100,0,0);(0,0);(0,0);(0,0) | (40,0,0,0);16 |
| V4 | (0,100,0);(0,0);(0,0);(0,0) | (30,0,0,0);12 |
| V5 | (0,0,100);(0,0);(0,0);(0,0) | (30,0,0,0);12 |
| V6 | (0,0,0);(100,0);(0,0);(0,0) | (0,60,0,0);24 |
| V7 | (0,0,0);(0,100);(0,0);(0,0) | (0,40,0,0);16 |
| V8 | (0,0,0);(0,0);(100,0);(0,0) | (0,0,50,0);5 |
| V9 | (0,0,0);(0,0);(0,100);(0,0) | (0,0,50,0);5 |
| V10 | (0,0,0);(0,0);(0,0);(100,0) | (0,0,0,60);6 |
| V11 | (0,0,0);(0,0);(0,0);(0,100) | (0,0,0,40);4 |

Independent checks: V0/V1 are 0/100 domain endpoints. Each impulse contributes its expanded coefficient times100/100. V2 has independent weighted numerator6910, denominator100, reduced to691/10. **N-INNER-WEIGHT** swaps L/H inner coefficients: V3 produces C30,S12 instead of C40,S16; V4 produces C40,S16 instead of C30,S12. **N-EARLY-FLOOR** floors C=133/2 to66 before the outer calculation: V2 produces689/10 instead of691/10. Literal component and total assertions reject both. These are deliberate future broken subjects, not altered approved weights.

**P01/FIXED-01**, source FIXED-01: repeat V2 alone and in ordered scalar batches `[V0,V2,V1]` and `[K4,V2]`; complete inputs for K4 are below. V2 must remain `(133/2,75,75,50), S=691/10` and have no graph decision in every batch. **N-RELATIVE** renormalizes S across batch min/max: V2 alone becomes0 under that explicitly defined bad rule instead of691/10. A different singleton convention does not excuse failures on frozen unchanged-input comparisons. All batch outputs have the expected literal totals, not only V2.

### 3.2 Within-cavern and cross-cavern order cases

Here `+` means accepted, `−X` means rejected because of accepted/protected X. A rejection witness must actually conflict and have the required accepted/protected status; when multiple such witnesses exist, accept the full correct witness set or the earliest conflicting entry in the frozen evaluation order, as declared in the output schema. It cannot cite a rejected vertex.

| Run ID / pinned source | Complete graph, components, IDs and status overrides | Exact order / accepted and rejected sets; independent reasoning | Incorrect alternative and exact separating assertion |
|---|---|---|---|
| P01/CAND-01 / CAND-01 | Outer unit graph V={U},E=∅,id U=0. Candidates c0=(100,0,0,0),c1=(0,100,0,0), both S40 and supplied hard-valid. Frozen seed ranks c1=0,c0=1. Fit record complete, then one U WIN. | Candidate score ties ordered c1,c0; selected c1. Outer trace U+, accepted{U},rejected∅. Same canonical cavern ID cannot distinguish its own candidates. | N-CAND-COMPONENT chooses c0 by C-first; N-CAND-ARRIVAL on `[c0,c1]` chooses c0. Exact selected candidate/rank must fail even though outer accepted set remains{U}. |
| P01/CAV-01 / CAV-01 | V={U,V},E={{U,V}}. U=(0,100,0,0),id−1; V=(100,0,0,0),id0. Both S40,WIN,hard-valid. Deliberately supplied candidate-seed order V<U is irrelevant across units. | U+,V−U; accepted{U},rejected{V}. Equal totals invoke canonical ID; 0.4×100=40 in different components. | N-CROSS-C-FIRST or N-CROSS-SEED: V+,U−V; accepted{V}. Assert full order and set under both arrival orders. |
| P01/EQUAL-3-C / K1–K3 + CAV-01 derived binding | Complete three-clique V={K1,K2,K3}, E={{K1,K2},{K1,K3},{K2,K3}}. K1=(100,0,0,0),id0; K2=(0,100,0,0),id−2; K3=(50,50,0,0),id−1; all S40. | K2+,K3−K2,K1−K2; accepted{K2},rejected{K1,K3}. Reference is exact equal40 then numeric IDs−2<−1<0. | N-C-FIRST sorts C descending: K1+,K3−K1,K2−K1; accepted{K1}. |
| P01/EQUAL-3-T / same source, complementary binding | Same clique and component tuples as preceding row; IDs K1=−2,K3=−1,K2=0, all S40. | K1+,K3−K1,K2−K1; accepted{K1},rejected{K2,K3}. | N-T-FIRST sorts T descending: K2+,K3−K2,K1−K2; accepted{K2}. Together the two cases reject both C-first and T-first preferences. |
| P01/NEAR-01 / K4 + K3 | V={P,Q},E={{P,Q}}. P=(50001/1000,50,0,0),id1; Q=(50,50,0,0),id0. Exact S(P)=100001/2500=40+1/2500; S(Q)=40. | P+,Q−P; accepted{P},rejected{Q}; the exact positive difference1/2500 outranks Q's earlier ID. | N-ROUND-2 rounds both totals to40 before comparison: Q+,P−Q; accepted{Q}. Reject numeric equality and rank/set errors independently. |
| P01/BIG-NEAR-01 / K4-derived arithmetic stress proposal | Same two-vertex edge and IDs as NEAR-01. P=(50+1/2^80,50,0,0),Q=(50,50,0,0). Exact S(P)=40+1/(5×2^79),S(Q)=40. This is supplied rational arithmetic data, not a geometric transform. | P+,Q−P; accepted{P},rejected{Q}. Difference is positive by exact fraction sign. Materialized files must expand integer powers to decimal integers, never floating-point expressions. | N-FLOAT64 rounds the component50+2^−80 to50: Q+,P−Q; accepted{Q}. N-FIXED64 must not silently wrap intermediate arithmetic; inability to support the bounded exact representation is reported, never a rounded tie. |
| P01/ID-ORDER-01 / ID-01 + CAV-01 derived binding | Four-clique V={a,b,c,d}, all six unordered edges. Components all(100,0,0,0),S40. IDs a=−2,b=−1,c=2,d=10. | a+,b−a,c−a,d−a; accepted{a},rejected{b,c,d}. Independent signed numeric order−2,−1,2,10. | N-ID-STRING compares coordinate decimal strings: order b,a,d,c; b+,others−b; accepted{b}. This checks the proposed synthetic comparator, not production encoding approval. |
| P01/CHAIN-3 / plan §8.2 Greedy chain | V={A,B,C},E={{A,B},{B,C}},no A–C. A=(100,0,0,0),S40,id0; B=(100,50,0,0),S60,id1; C=(100,100,0,0),S80,id2. | C+,B−C,A+; accepted{C,A},rejected{B}. A has no edge to the only previously accepted C. | N-REJECTED-VETO rejects A for its rejected neighbor B: accepted{C},rejected{B,A}. ID-first visits A,B,C and may get the same unordered set{A,C}; the order assertion still rejects it. |
| P01/CHAIN-ID-01 / CHAIN-ID-01, mandatory adversarial ID control | Same vertices/edges/component tuples/totals as CHAIN-3. IDs **B=−1,A=0,C=1**; all original WIN. | **C+,B−C,A+**, accepted **{C,A}**,rejected{B}. Unequal totals dominate the ID comparison. | **N-ID-FIRST:** **B+,A−B,C−B**, accepted **{B}**,rejected{A,C}. Both processing order and accepted set must differ and be checked. |
| P01/CHAIN-PROT-01 / CHAIN-PROT-01 | Same current candidate graph/components/IDs as CHAIN-ID-01; add existing protected retired site P,id−2,with only additional edge{P,C}. P has status RETIRED_PROTECTED; components/total/qualification/current roll are NOT_APPLICABLE. P is an input protected site, not a new roll-winning vertex. | Visit C−P,B+,A−B; accepted{B},spacing-rejected{A},protected-rejected{C}. Protected input set remains{P}. P never joins score ordering or the current roll denominator. | N-PROTECTION-IGNORE gives C+,B−C,A+,accepted{C,A}. N-REJECTED-VETO rejects for any conflicting earlier visited winner, even rejected: C−P,B−C,A−B,accepted∅. Its C/B witnesses are invalid because those candidates were not accepted. |
| P01/CYCLE-01 / CYCLE-01 | V={0,1,2,3},E={{0,1},{1,2},{2,3},{3,0}}. Components respectively G(10),G(20),G(30),G(40),IDs0,1,2,3. | 3+,2−3,1+,0−{3,1}; accepted{3,1},rejected{2,0}. Direct check:3–1 absent;0 conflicts with both accepted units. | N-ID-FIRST visits0,1,2,3, accepted{0,2}. N-REJECTED-VETO removes1 because2 was rejected, leaving only{3}. |

Every row above checks approved weight/priority/protection policy where applicable; numeric IDs/ranks, the added clique/big-number bindings and G(s) are explicit **synthetic proposals**. They change no approved score formula, measurement transform, physical spacing, roll probability or candidate cap. Failed negative-control subjects are expected; the detector passes only when it identifies the planted violation and also passes clean counterparts.

### 3.3 Complete long-chain family and late information

Source: CHAIN-LONG-n and the plan's explicit transitive-dependency counterexample. Run IDs are **P01/LONG-SCORE-n/BASE**, **P01/LONG-SCORE-n/EXT**, **P01/LONG-TIE-n/BASE**, **P01/LONG-TIE-n/EXT**, with the exact finite index set `n∈{1,2,3,4,8,16,64,256,1024}`. These constructors completely specify every vertex/edge/component/expectation; they are mathematical data definitions, not executable harness code.

| Field | SCORE family | TIE family |
|---|---|---|
| BASE vertices/edges | V={0,…,n}; E={{i,i+1}:0≤i<n} | Same complete path |
| BASE components / total | Vertex i gets G(100i/(n+1)), hence S(i)=100i/(n+1) | Every vertex gets(100,0,0,0), hence S40 |
| Canonical key | id(i)=i | id(i)=−i, with zero encoded0 |
| Qualification/fit/roll | Every vertex has supplied TRUE qualification, complete one-candidate FIT and one frozen WIN | Same |
| EXT input | Retain **all existing** tuples/totals/IDs; add vertex n+1 with G(100),S100,id=n+1, edge{n,n+1}, no other changes | Retain all; add vertex n+1 at S40,id=−(n+1), edge{n,n+1} |
| BASE evaluation order | n,n−1,…,0 | n,n−1,…,0 by equal totals then signed ID |
| BASE exact accepted/rejected | Accept i iff n−i even; reject i iff n−i odd, with adjacent accepted higher-priority i+1 as witness | Same |
| EXT evaluation order | n+1,n,…,0 | n+1,n,…,0 |
| EXT exact accepted/rejected | Accept i iff n+1−i even; reject iff odd, witness i+1. Every old vertex flips its acceptance relative to BASE. | Same |

Independent proof: the highest-priority endpoint has no previously accepted neighbor and is accepted. Its next neighbor is rejected. The next has only that rejected predecessor and no other prior accepted neighbor, so it is accepted. Induction gives the parity formulas without running the IUT greedy algorithm. In EXT the extra endpoint toggles parity at every old vertex, including0.

**N-OMIT-ENDPOINT** receives EXT but drops vertex n+1 and its incident edge: its exact incorrect trace is n..0 and accepted old indices with n−i even; expected EXT is the opposite old-index set plus its new accepted endpoint. **N-LOCAL-CAP** claims the first n+1 listed vertices suffice and produces that same wrong result. Run against every frozen n, not only a favorable small case. **N-TIE-ID-DESC** incorrectly reverses canonical-ID order on TIE and visits0..n (or0..n+1), accepting even indices; order fails even when parity accidentally matches the set. Raw arrival-order processing also fails the reversed/permuted schedules.

These finite examples can falsify a claimed cutoff within their tested graph class. Neither a1024-chain result, finite graph coordinates, nor a direct conflict edge radius proves a universal practical bound for natural caves/transitive greedy closure. No physical length or placement distance is assigned.

**P01/LATE-01**, source CHAIN-LONG-n + A02-Q07 complete-population rule: vertices{A,B,C,D}, components of A/B/C from CHAIN-ID-01, D=(100,100,100,100),S100; IDs B=−1,A=0,C=1,D=2; all qualified/FIT/WIN. Final E={{A,B},{B,C},{C,D}}. Initial observation explicitly marks edge{C,D} UNKNOWN and the population/dependency certificate OPEN, even though all four labels are supplied. Expected initial order/action=`DEFER_GRAPH`, finalized accepted=∅,finalized rejected=∅,pending{A,B,C,D}; do not confuse these empty **finalized** sets with losing all candidates. After the independent reveal certifies exactly the final complete E, trace D+,C−D,B+,A−B; accepted{D,B},rejected{C,A},pending∅. **N-UNKNOWN-EMPTY** interprets the unknown edge as absent and finalizes D+,C+,B−C,A+,accepted{D,C,A}; this is FAIL at the open stage and differs from the final expected set. This is an explicit synthetic completeness flag/reveal oracle, not a proof that a real cave scanner can supply that flag soundly.

## 4. P4 synthetic lifecycle and accounting

### 4.1 Exact fit/roll inputs and original decision identity

**P01/ROLL-01**, source ROLL-01 and plan §8.4: V={a,b,c},E={{a,b}},IDs a=0,b=1,c=2; components a=(100,100,0,0),S80; b=(100,0,0,0),S40; c=(50,0,0,0),S20. Qualification for each is supplied TRUE. Each has eight frozen candidate descriptors, seed rank equal to slot index1..8; a has only slot8 FIT, b only slot1 FIT, c only slot3 FIT; all other slots are SYNTHETIC_HARD_REJECT with score NOT_APPLICABLE. All eight checks are supplied/evaluated once per unit, before that unit's single roll. FIT candidates inherit that unit's tuple above. Selected descriptors are a8,b1,c3. Frozen roll results a=WIN,b=WIN,c=LOSS. No RNG/draw distribution is evaluated; configured policy remains3/4. The observed fixture fraction2/3 is not a calibrated probability.

Expected distinct supplied candidate fit-check totals:8 per unit,24 overall; duplicate delivery is not a new candidate search/check descriptor. Distinct fit-eligible{a,b,c},actually rolled{a,b,c},winners{a,b}. Winner evaluation order a,b; trace a+,b−a; c is ROLL_LOSS and is **not visited in the winner pass**. Accepted decision set{a},spacing exclusions{b},roll-loss{c}; current pending placement{a}. Actual physically completed placements=0 throughout this isolated probe; no world/publication exists.

**P01/NOFIT-01**, derived from ROLL-01/FIX-01 A: retain a/b/c exactly, add unit d,id3,qualification TRUE, eight hard-rejected candidates, no FIT score and status NOT_ROLLED; add no graph edges. Unit d's component/total is NOT_APPLICABLE. Qualifying labels=4 but fit-eligible=3,actually rolled=3,winners=2,spacing exclusions=1,accepted decisions=1,pending=1. Winner order/sets remain a then b /{a}/{b}. **N-QUALIFIED-DENOM** reports roll fraction2/4 or draws for d; correct roll fraction2/3 and d must never enter the winner pass. **N-ROLL-FIRST** records a roll before completion of its fit checks; reject that transition even if the later candidate happens to fit. **N-NINTH** adds/checks slot9 to create a fit; reject, no new eligible unit/roll.

All identities/event schemas here are test-only. The original decision key is `(fixture,full supplied canonical tuple,"decision-1")`. A live evidence/worker epoch annotation never changes that key. Distinct event IDs use the original key plus event kind; duplicate identical event payloads are no-ops, contradictory payloads are a FAIL/UNKNOWN knowledge conflict and freeze affected decisions, never last-writer-wins. A record-format/duplicate-delivery failure is not permission to draw again.

### 4.2 Explicit independent state/event expectations

These are separate branches of the same original ROLL-01 history, not one impossible sequence that both retires and publishes the same attempt. For this section “success” means **synthetic modeled success** from a supplied model receipt, never physically completed Minecraft placement. Counters are distinct sets, not a forced partition.

| Case ID / source | Complete added event/state input | Independent exact expected outcome / negative control |
|---|---|---|
| P01/ACCT-BASE / ROLL-01 | Full original fit/roll/winner-pass history above; no terminal event | fit3,rolled3,win2,spacing1,accepted-decisions{a},pending{a},retired∅,model-success0,actual-physical-completed0. Fractions: winning2/3; logical acceptance1/3; actual completion0/3 is explicitly NOT a measured Minecraft rate. N-ACCEPT-IS-COMPLETE falsely reports actual1. |
| P01/ACCT-INVALID / LIFE-02 | BASE plus supplied original a certificate INVALID before any modeled write; immutable selected descriptor/rank/roll unchanged | Same3/3/2/1 and accepted-decision history{a},pending{a}; model-success0,physical0. N-POSTROLL-NOFIT removes a from received-roll denominator (2 rather than3); FAIL. No new fit search/rescore/roll. |
| P01/ACCT-RETIRE / LIFE-03 | BASE plus model APPLYING, UNCERTAIN, QUARANTINED, then RETIRED_PROTECTED for a with no supplied completion receipt; no actual block data or API call | Retain fit3,rolled3,win2,spacing1,accepted-decision history{a}; pending∅,retired/protected-site{a},model-success0,physical0. No block cleanup, repair or new roll/search event is authorized. N-REROLL adds a second roll for a (attempt counter4 instead of3), or changes decision key by epoch: FAIL. This checks logical transitions only, not real partial integrity or protected-footprint persistence. |
| P01/ACCT-PROTECTED / CHAIN-PROT-01 | Use that graph case's ABC and preexisting protected/retired P, not the abc ROLL graph; all current ABC original rolls WIN | Current cohort fit3,rolled3,win3,spacing exclusions{A},protection exclusions{C},accepted decisions/pending{B}; supplied protected-site set{P} unchanged; physical0. P is external historical protected input, not a new eligibility/roll in this cohort. N-IGNORE-P gives accepted{C,A} and protection exclusions∅. |
| P01/ACCT-MODEL-SUCCESS / ROLL-01, LIFE-01 | BASE plus independently supplied **MODEL_PUBLICATION_RECEIPT(a,decision-1,complete=true,effect=true,durable=true)**; these fields are axioms of this pure model, not verified Minecraft properties | fit3,rolled3,win2,spacing1,accepted-decision history{a},pending∅,retired∅,synthetic-history-success{a}; **actual-physical-completed0**. Reconcile modeled receipt exactly once. N-LOGICAL-IS-PHYSICAL reports actual1; FAIL. No production receipt validity/atomicity is tested. |
| P01/ACCT-MODEL-EDIT / LIFE-04 | MODEL-SUCCESS plus MODEL_PLAYER_MODIFIED(a); no real world edit | Same modeled history-success{a},physical0; no generation repair/retirement/decrement/reroll event. N-ERASE-HISTORY gives modeled success0 or retires a solely for that edit; FAIL. This is not player-preservation evidence. |
| P01/ACCT-DUP / A02-Q06, plan §8.4 | Duplicate every valid BASE event once, same original IDs/payloads; separately duplicate the single MODEL-SUCCESS receipt once | All BASE sets/counts unchanged; receipt branch modeled success exactly1,physical0. N-ADDITIVE event-counting reports fit6,rolled6,win4,spacing2,accepted2 on the doubled BASE log, or model-success2 on doubled receipt: FAIL. |
| P01/ACCT-REPLAY / A02-Q06, ROLL-01 | For each prefix k=0..L of a frozen valid event log of length L, round-trip its model state through the specified JSON representation; replay remaining events, then replay the entire original log twice | Final model state/sets equal the corresponding original branch exactly, no added roll decision/candidate/receipt/reward event. Prefix expected sets are the literal events of each type in indices≤k, with a field not yet established marked PENDING, not zero invented evidence. This tests serialization and event replay only, not process/OS/Minecraft durability. N-NEW-EPOCH creates another decision on replay; FAIL. |
| P01/ACCT-LOSS / plan missing-ledger case | Start from BASE or MODEL-SUCCESS and discard materialized counters/registration projections while retaining the original input events/model receipt; separate subcase also removes source decision/receipt evidence | Projection-only loss: reconstruct the same expected sets once from retained input events. Source-evidence loss: knowledge UNKNOWN and affected operations blocked; no new roll/search, no deletion of separately known genuine model history. N-MISSING-MEANS-EMPTY retries a as unrolled; FAIL. |

Canonical BASE log has exactly12 records in the following order. Unit events use the original decision key plus the named kind; the aggregate decision uses `(fixture,"population-1","CONFLICT_DECISION")`, not a fourth unit identity. Origin-worker labels are diagnostics only, not deduplication keys.

| Record index | Complete event fact / payload | Independent set effect |
|---|---|---|
| 1,2,3 | QUALIFIED(a),QUALIFIED(b),QUALIFIED(c), each supplied true | Add exactly that unit to qualification labels. |
| 4,5,6 | FIT_PLAN_COMPLETE(a), then b, then c; payload includes all8 slot statuses/ranks and selected a8/b1/c3 from §4.1, original components/plan descriptor,fit_eligible=true | Add each once to fit-eligible; no roll fact yet. |
| 7,8,9 | ORIGINAL_ROLL(a,WIN),ORIGINAL_ROLL(b,WIN),ORIGINAL_ROLL(c,LOSS), tied to those fit records | Received-roll set{a,b,c},winner set{a,b}; c's LOSS remains distinct. |
| 10 | CONFLICT_DECISION with complete current cohort{a,b,c},winner population{a,b},edge{a,b},frozen trace[a+,b−a] | Establish accepted-decision history{a},spacing set{b},pending{a},no success. This fact is independently supplied to ledger tests, not regenerated from the ledger IUT. Graph-IUT correctness is checked separately. |
| 11,12 | ACCEPTED_PENDING(a) projection, SPACING_EXCLUDED(b) projection, both referencing record10 | Projections reproduce record10; they add no second decision/count. |

MODEL-SUCCESS appends record13 with the stated model receipt; MODEL-EDIT appends14 with the stated model edit. INVALID appends13 with CERTIFICATE_INVALID(a). RETIRE instead appends13 APPLYING(a),14 UNCERTAIN(a),15 QUARANTINED(a),16 RETIRED_PROTECTED(a), all under original decision-1. NOFIT inserts QUALIFIED(d) before the first FIT record and FIT_PLAN_COMPLETE(d,all8 rejected,fit_eligible=false,roll=NOT_ROLLED) before the first ORIGINAL_ROLL record, yielding14 records with the same winner trace; there is no ORIGINAL_ROLL(d). ACCT-DUP is the applicable full log concatenated with itself; the receipt-duplicate subcase repeats only record13. ACCT-LOSS retains or removes the source/projection records exactly as its row specifies.

Permitted event partial order: QUALIFIED(unit) → FIT_PLAN_COMPLETE(unit,all8slots) → ORIGINAL_ROLL(unit) for each rolled unit; record10's decision only after all current fit/roll inputs are complete; projection/terminal records after record10. All8 slot checks are explicit payload/trace facts before that unit's roll, not8 separate rolled eligibility events. Per-prefix expected source sets are obtained by membership of these **literal** event records in that prefix, not by calling reconciliation logic. Record10 establishes its listed decision sets;11/12 only project them. A field whose source fact is not yet delivered is PENDING. Exact event bytes/IDs and every schedule's prefix golden vectors must be materialized/reviewed under U02/U03 before execution.

Synthetic “two worker” cases duplicate the same claim/event with origin labels worker0/worker1 in each frozen interleaving. They do not exercise actual Java-thread synchronization, database uniqueness, Minecraft scheduling or cross-process races. No key/reward delivery operation belongs to this probe; expected such event count is0.

## 5. P2 independent assertions and controls

The observer must assert, for every selected case and schedule:

1. Exact component rationals, exact total, correct input-domain handling and unchanged raw input. Component=0/100 boundaries are not clipped from an invented measure.
2. All preconditions/statuses preserved: failed fit/no-roll and roll-loss vertices are not score-pass winners; at most8 checks before one original roll.
3. Exact semantic candidate priority order and chosen seed-rank descriptor where applicable; within-cavern and cross-cavern rules stay distinct. This is a ranking oracle, not a requirement that every internal comparator call or score-measurement loop use one implementation order.
4. Exact full winner processing order, each decision and a valid accepted/protected conflict witness, exact accepted/rejected/protection/pending sets. Rejected-neighbor veto fails even if it appears “more conservative.”
5. Original identities/events and distinct counters survive permitted duplicates/model replay; physical-completed count stays0. A model receipt is never upgraded to actual publication proof.

Each named mutant in §3/4 is a deliberately incorrect alternative, not an allowed fallback. Preserve separate `subject_property_verdict` and `detector_verdict`: a planted broken subject must FAIL the precise assertion, while detector PASS requires that failure **and** all corresponding clean cases to succeed. An always-reject/always-UNKNOWN detector cannot pass. No oracle computes its expected trace by calling the IUT.

The literal small-graph traces and expanded raw-weight identity constitute a second derivation of expected outcomes. Long-case expectations come from path parity, not a second greedy implementation sharing the comparator. Future review must check the frozen golden file against this text, including sign/denominator reductions and all graph edges; changing goldens to match the subject is prohibited.

## 6. P3 future execution binding proposal — NOT A COMMAND TO RUN NOW

### 6.1 Runner, runtime and exact command proposal

Propose a **standalone Java25 JAR**, JDK standard library only, with separate subject and observer/oracle packages and entry point chosen during later implementation review. It must not load Minecraft/Fabric or the production mod, run Gradle's `test/check` tasks, use GameTest or start a server/client. Repository dependencies observed statically are Java25 and JUnit5.11.4, but this proposal does not depend on JUnit/Gradle. No build task, source file or executable JAR is supplied here.

Static environment evidence: `/workspace/toolchains/jdk-25.0.4.1/release` declares Oracle Corporation Java **25.0.4.1**, runtime **25.0.4.1+1-LTS-5**, Linux x86_64/glibc. This file was read; Java was not launched. Propose this exact runtime for the first run, pending executable/archive hash verification and isolated-checkout binding U01. Another runtime requires a recorded spec amendment/review, not silent substitution. Minecraft26.3/Loader0.19.5/Loom1.17.21/Fabric0.161.0+26.3 remain repository context only.

Static SHA-256 inspection additionally found JDK archive `/workspace/setup-downloads/jdk25.tar.gz` = `dd7e7f51d23bcffe85f1a439577ce1c0190a33417b34afdb338f9b3897bdeaec` (matching the local setup hash record), and proposed executable `/workspace/toolchains/jdk-25.0.4.1/bin/java` = `352e9acdbab44ecf32bb62c6fabe2a704267189967f6a310a1f21b0b36b5dc40`. These bind the inspected bytes, not the correctness of a future environment or runtime execution. U01 requires matching/reviewing those bytes and the complete isolated environment at entry, not treating a path string as immutable identity.

The future runner CLI proposal, from the root of an explicitly authorized isolated checkout, is exactly:

```text
env -i PATH=/usr/bin:/bin LC_ALL=C.UTF-8 TZ=UTC /workspace/toolchains/jdk-25.0.4.1/bin/java -Xms32m -Xmx256m -XX:ActiveProcessorCount=1 -Dfile.encoding=UTF-8 -Duser.language=en -Duser.country=US -Duser.timezone=UTC -jar tools/st05-p0-probe-01/probe-01.jar --suite P01-SYNTHETIC-1 --inputs tools/st05-p0-probe-01/inputs-v1.json --expected tools/st05-p0-probe-01/goldens-v1.json --schedule frozen-v1 --seed 7 --mode subjects-and-negative-controls --output /tmp/st05-p0-probe-01/run-0001
```

**Command status: PROPOSED / UNBOUND / NOT RUN.** The named JAR and JSON artifacts do not exist on the inspected branch. CLI parsing/exit semantics, artifact hashes, working checkout, locale availability, heap overhead and budget instrumentation must be checked statically after separately authorized implementation and before execution. The output directory must be new; refuse overwrite. Freeze a different literal run ID before a later repetition rather than silently modifying run-0001. Do not add networking, world paths or production classpath entries to make this command work.

Proposed exit codes:0 only for a fully observed clean/negative-control suite PASS;1 for a reproducible assertion/negative-control detection FAIL;2 for UNKNOWN/incomplete evidence or a resource/environment abort;3 for invalid input/schema/baseline/authorization entry. Exit0 alone is not acceptance without independent artifacts. These CLI choices are probe proposals, not owner-approved runtime behavior.

### 6.2 Input and output serialization

Propose UTF-8 JSON, schema `P01-SYNTHETIC-1`, no duplicate object keys, no NaN/Infinity, no float/decimal numeric tokens for score/ID values. Rationals are two decimal-integer strings `{num,den}`, den>0, reduced by gcd, zero=`0/1`; reject malformed/zero-denominator or out-of-range score input before use. All IDs/coordinates, counts and seed are decimal integer strings, canonical zero0, no negative zero or leading plus/zeros. A vertex has explicit full ID, component tuple or nine raw values, qualification label, complete candidate descriptors, fit-complete marker and frozen roll status. Failed-fit score is null/NOT_APPLICABLE, not0 eligible. Protected sites and OPEN graph information are separate fields. Arrays carry the delivery schedule; they do not define semantic priority.

Every fixture record must contain: `fixture_id`, immutable source refs, `scalar_inputs` (empty for graph-only cases), `vertices`, `edges`, `protected_sites`, `unknown_edges`, `population_complete`, `events`, `delivery_schedule`, `control_overrides` (empty unless specified), `expected_ref`. Arithmetic-only cases bind their nine-submetric or four-component values in scalar_inputs and use empty graph/event arrays. Every vertex is explicit; parameterized long families are expanded into records before execution, including full decimal numerators/denominators, and both input and expected bytes are frozen. Strings such as `2^80` are mathematical constructors in this document, **not allowed numeric strings in those files**. Edge records canonicalize their endpoint tuples in numeric order; repeat/unknown/inconsistent edges cannot be silently omitted. Expected data is in a separate file, never generated from observed output.

For CAV-01 specifically, the input record has exactly two full IDs with suffixes[-1,0,0]/[0,0,0], U components[0/1,100/1,0/1,0/1], V[100/1,0/1,0/1,0/1], edge[U,V], protected=[],unknown_edges=[],population_complete=true, explicit FIT/WIN/seed-rank records, and control_overrides.cross_candidate_seed_order=[V,U] solely for the deliberate N-CROSS-SEED subject; the correct cross-cavern algorithm ignores that override. Its golden record has totals U=40/1,V=40/1, order[U,V], decisions[U:ACCEPT,V:REJECT_SPACING(U)], accepted[U],spacing_rejected[V],protected_rejected=[],pending=[],physical_completed=0. This fixes the expected content independently of any future Java class.

Output records include both arrival order and semantic candidate/winner order, reduced score rationals, immutable original decision identity, per-vertex decision with conflict witness, sorted sets for every outcome/counter, work counters, evidence references and classifications. For a rejection with several valid witnesses, output the complete conflicting accepted/protected set in canonical order; the observer also records the earliest such witness in the frozen evaluation order. Never choose a rejected vertex as witness. An interrupted trace is marked incomplete and cannot be presented as a smaller accepted population.

### 6.3 Determinism, arithmetic and overflow policy

Use exact integer numerator/positive-denominator rational arithmetic backed by **arbitrary-precision integers** for input, accumulation and comparisons. Compare a/b with c/d by exact cross-products or a mathematically equivalent exact method; no fixed64-bit wrap, saturation, rounding or epsilon. Reduce serialized results; internal reduction strategy is an implementation detail subject to the same exact outputs. Proposal input cap:4096 bits per rational integer after parsing, preventing unbounded hostile allocations; exceeding it is input rejection/UNBOUND-cap review, not score clipping. BIG-NEAR-01 fits that cap and exceeds64-bit intermediates. Score component domains remain0..100.

Seed7 and every iteration schedule are frozen. No system clock, hash-map iteration order, scheduling race, random shuffling or RNG call may influence score/rank/selection/counts. A subject may use a different internal exact algorithm, but must expose the same semantic evaluation order and final trace. Diagnostic timestamps/timing may be recorded separately and never enter semantic output hashes or decisions.

### 6.4 Frozen permutation and model replay coverage

| Case family | Mandatory delivery schedules / coverage |
|---|---|
| Arithmetic | Listed vector order, reversed vector order, FIXED-01's exact batches; expected vector identities/totals unchanged. |
| Complete small graph with≤4 candidate vertices | Every vertex-array permutation (k!), independently with edge list in canonical order and reverse order; deduplicate identical edge lists only. Protected-site list likewise canonical/reverse. Do not claim all possible internal thread schedules. |
| CAND-01 | Both candidate delivery orders, retaining the explicit seed ranks and one-unit outer graph. Rank order is invariant. |
| Each LONG base/extension | Four specified vertex sequences: increasing labels; decreasing labels; evens increasing then odds increasing; alternating low/high `[0,m−1,1,m−2,…]` for m vertices. Pair each with canonical/reversed edge order; deduplicate identical sequences with first occurrence retained. BASE and EXT have independently frozen records; do not recompute old scores using a new denominator. |
| LATE-01 | Both stages in fixed OPEN→REVEAL order; for each stage all4! vertex permutations and canonical/reverse known-edge lists. OPEN never authorizes final decisions; final full trace is D,C,B,A. |
| Accounting normal logs | All3! unit-block orders respecting each unit's qualify→fit→roll dependencies, plus fixed phase-batched order a,b,c and reverse c,b,a; conflict resolution only after the complete winner input. Two worker-origin labels duplicate each same-key event in worker0/1 and1/0 order. |
| Accounting replay/duplicates | For each frozen legal log, every prefix cut k=0..L; serialize/decode that prefix and deliver remainder plus two complete replays. Exact identical duplicate payloads are no-ops; contradictory payload and roll-before-fit are separate negative traces, not permitted permutations. |

Report the enumerated schedule IDs and exact counts after materialization; do not claim unenumerated permutations or parallel executions. The mathematical construction is fixed; byte-level schedule enumeration/list hashes remain U02. Merely getting one order right cannot pass these cases.

### 6.5 Proposed work/memory envelope and abort semantics

The following are **exploratory probe ceilings, not owner-approved gameplay/runtime budgets and not evidence of a universal worldgen bound**. They require explicit acceptance and a checked instrumentation map under U05 before execution. No value here changes approved8-candidate semantics or substitutes a cutoff for graph completeness.

- Input ceilings per expanded case:4096 candidate vertices,16384 undirected edges,8 candidates per unit,4096-bit rational integers. Largest selected path has1026 vertices/1025 edges; these exact finite input cardinalities do not bound real caves.
- Proposed deterministic work ceiling per case/schedule:10,000,000 counted subject-level operations, each exact rational add/multiply/compare, priority comparator invocation, adjacency-membership inspection and event application counted in its declared category. Nested helper calls must not disappear from counters. Decimal parse/gcd bit work and host allocation are separately recorded, not falsely constant-time internals; their mapping/enforcement is U05.
- Proposed JVM heap ceiling256MiB, initial32MiB; independent observer and evidence buffers bounded and streamed. Proposed evidence ceiling256MiB per run; exceedance aborts rather than silently truncates the oracle trace. Actual retained-heap/native-memory observation and enforcement are UNBOUND until runner/observer review.
- Proposed external watchdog60 seconds per case/schedule is a **host safety guard only**. Timeout/oom/operation/evidence-cap exhaustion yields UNKNOWN/incomplete execution with last complete artifact retained, not FAIL proof that the approved rule is mathematically wrong and not a valid partial placement. Do not accept the processed prefix, reroll or drop hard cases to finish. Correctness mismatch observed before abort remains FAIL and is not erased by the later abort.

Acceptance requires every selected determinate clean case to complete and every planted error to be specifically detected under the frozen approved-for-run envelope. An all-UNKNOWN result cannot pass. If a ceiling prevents meaningful coverage, stop and revise/review the execution specification before another run; no silent budget increase or result-sensitive cutoff.

## 7. Evidence, verdicts and reproducibility

Future evidence root `/tmp/st05-p0-probe-01/run-0001/` must contain a frozen `run-manifest.json` with all authority/spec/input/golden/runner/JDK hashes, runtime metadata, CLI/environment and selected scope. Required artifacts: `inputs-v1.json`, `goldens-v1.json`, `schedules-v1.json`, `input.sha256`, `expected.sha256`, `runner.sha256`, `stdout.txt`, `stderr.txt`, `results.json`, `observer.json`, `negative-controls.json`, `resource-counters.json`, plus one complete `traces/<fixture-id>/<schedule-id>.jsonl` and model replay snapshot/evidence file for every case/cut. Escape fixture IDs deterministically as relative path segments; reject path traversal. No artifact generated now is claimed to contain probe results.

The observer freezes and hashes inputs/goldens before reading subject results, retains original files and records any mismatch against literal expected fractions/orders/sets. A separate independent review must approve that materialization and verify the command does not load Minecraft or mutate production/world data. Future output hash reproducibility covers semantic records with stable key/set ordering; omit only explicitly listed diagnostics such as timestamps/timing, never decisions, errors, UNKNOWN cases or resource-abort information.

| Classification | Required meaning |
|---|---|
| PASS | Future scoped case fully observed; all exact clean assertions match; applicable negative controls identify their planted error; required schedules and progress complete. This is not A02/A03/package/integrated feasibility PASS. |
| FAIL | A determinate rational/order/set/accounting invariant is contradicted, an unauthorized extra roll/candidate is used, an incorrect mutant is accepted, or detector fails required clean progress despite completed observation. Keep exact discrepancy and counterexample. |
| UNKNOWN | Runtime/environment/resource abort, unavailable observation or genuinely missing evidence prevents a property verdict. Preserve any already established FAIL. Correct fail-closed handling of an intentionally missing-evidence input is the expected model disposition, not proof of real-world persistence. |
| BLOCKED / UNBOUND | Authorization or a named preparation binding is absent. No execution and no PASS/FAIL substitution. Every executable case currently has this entry state. |

Suite acceptance cannot hide FAIL/UNKNOWN with averages, skip rows or count a detector PASS as a subject-property PASS. The report must show clean subjects, planted bad subjects and the observer separately. Here **all execution remains NOT RUN**; no result status other than preparation readiness is reported.

Keep `execution_case_verdict` separate from `model_knowledge_status` and the subject property's verdict. In ACCT-LOSS's intentionally removed-source subcase, a fully observed model response UNKNOWN/no-reroll is the exact required response: the case can PASS that fail-closed assertion while its missing-history property remains UNKNOWN. In contrast, an unavailable observer/resource abort makes execution_case_verdict UNKNOWN. Such a deliberate uncertainty input cannot excuse blanket UNKNOWN on complete clean cases or be counted as evidence that missing real Minecraft history is recoverable.

## 8. UNBOUND register, STOP conditions and owner boundary

| Binding | Current status / why static preparation cannot finish it now | Required resolution before any execution |
|---|---|---|
| U01 isolated runtime/environment | Runtime version/executable/archive static hashes bound in §6.1; future isolated subject checkout, full launch environment, locale and watchdog host not frozen as runnable evidence | Separately authorize environment; match/review exact JDK and subject checkout, freeze paths/environment and confirm no world/production classpath. No Java/Minecraft run in this preparation. |
| U02 expanded input/schedule artifacts | Complete mathematical constructors are specified, but no JSON fixtures/schedules have been materialized in this docs-only commit | Separately authorized text/data materialization, independent static comparison to every table/formula, normalized schema and full file hashes; freeze enumerated schedule counts. |
| U03 golden/observer independence | Literal/formal expected outcomes exist here; executable observer/loader/serializer and reviewed golden bytes do not exist | Implement only under new authorization; independently review oracle provenance and no IUT dependency, freeze expected-file/observer hashes and per-prefix model sets. Do not ask the IUT to emit goldens. |
| U04 subject/negative-control runner | Pinned tree has no Probe01 JAR/source/CLI; command proposal cannot be executed honestly | Separate isolated implementation authorization, review API/CLI/exit-code behavior, deterministic mutation definitions and runner/source hashes; preserve production code and approved rules. |
| U05 budget/enforcement | Numeric ceilings are explicit proposals; counter boundaries, watchdog/memory/evidence enforcement and chosen acceptance envelope not approved for a run | Review instrumentation and statically justify finite-suite cost, explicitly select/freeze the exploratory limits. Empirical performance may be an experiment output later, not an invented preparation PASS. |
| U06 execution permission | This request authorizes documentation/static analysis and one documentation commit only | Owner separately authorizes the exact implementation/execution scope, command/artifacts/environment/budget. No implementation or execution follows automatically from committing this file. |

UNBOUND blocks **execution**, not independent document review or separately authorized work whose stated purpose is to resolve that binding. In particular, U03/U04 cannot require an already existing runner before a later authorized isolated implementation may create/review it. Implementation still requires a new explicit scope; this task supplies none. Execution requires every applicable artifact/environment/enforcement binding resolved and U06 authorization in place.

STOP an implementation or execution proposal if: authority/branch/spec incompatible; its needed authorization is absent; expected values are derived from IUT; canonical order or seed order is silently replaced; an excluded measurement transform/distance/world/API is introduced; limits cause graph truncation or result-sensitive timeouts; a missing roll/event becomes a fresh decision; production/world/registry/controller/reward behavior is requested; or full package/Stage1 progression is implied. Additionally STOP execution while any applicable binding remains UNBOUND. Record the precise missing binding or policy conflict, not a generic refusal or fabricated runtime result.

Owner-approved semantics remain as recorded. Q09 C/Q10 C do not approve final spacing/representatives/encoding; pure test tuples need not pretend to settle them. Supplied fit/qualification/roll statuses do not authorize integrating their producers. The proposed standalone runner, JSON schema, numeric IDs, schedules and resource ceilings are **research protocol proposals**, not approved implementation requirements. No full arena blueprint, natural cave proof or publication mechanism is required merely to review these synthetic equations/graphs, and no synthetic result may substitute for those future obligations.

Only this new specification document is intended for the documentation commit. Original plan, manifest, authority, API mapping, gap register, production code and checkpoints remain unchanged. Documentation-level review checks literal arithmetic/trace derivations, synthetic-versus-physical accounting, retained source IDs, coverage and UNBOUND disclosure; it does not execute any fixture or test.

**Final readiness: PROBE_01_SPEC_INCOMPLETE**, due specifically to U01–U05 environment/artifact/observer/runner/enforcement bindings. U06 is a separate authorization gate; lack of execution authorization alone is not a defect in a preparation specification. The proposed suite and independently auditable expected outcomes are prepared for read-only review and a separately authorized materialization/implementation step; the status does not demand A02/A03 feasibility proof in advance. **No PROBE_01_PASS. A02/A03 remain BLOCKED. No probe execution authorized.**
