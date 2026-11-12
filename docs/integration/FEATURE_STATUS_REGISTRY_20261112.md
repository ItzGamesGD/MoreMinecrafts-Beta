# Grand Unified feature registry — 2026-11-12 (Phase 2)

Working branch `work/grand-unified-20261112`, based on `49747981ecc49f7f6b0a39fe62b23bd1a8d42d02`. Machine-readable copy: [FEATURE_STATUS_REGISTRY_20261112.tsv](FEATURE_STATUS_REGISTRY_20261112.tsv). Recovery method and per-state results: [RECOVERY_RUNBOOK_20261112.md](RECOVERY_RUNBOOK_20261112.md).

**Rules.** `INACTIVE — SOURCE CONSOLIDATED` means the source assets are present under `inactive/` and a fresh clone of Grand Unified (no feature refs) rebuilds the original head tree with `scripts/integration/recover_state.py`; the `recovery_class` column states the outcome (EXACT = reconstructed tree identical to the original head tree; byte-identical files alone would not qualify). Nothing under `inactive/` is compiled, registered, loaded as a resource or run by CI. `ACTIVE` rows describe the unchanged baseline tree.

Counts: ACTIVE: 6, DOCUMENTED: 5, INACTIVE — SOURCE CONSOLIDATED: 11, INACTIVE — EXTERNAL REF PRESERVED: 7, SUPERSEDED: 5, REVERTED: 3, OWNER DECISION REQUIRED: 11


## ACTIVE

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| ACT-01 | ST-01 natural worldgen, Policy D | work/st01-p8c2c-policy-d-20261111@c376488d | src/main/java/.../pyramid/worldgen | active in baseline tree | docs/CONTENT_AUTHORITY_INDEX.md (ST-01 Policy D) | live-client acceptance pending per baseline manifest; CI evidence is on tree-identical f5777244 | n/a |
| ACT-02 | ST-01 complete pyramid assembly (G0-R1 / P8-C2-A) | work/st01-p8c2a-complete-assembly-20261110-r1@c25f9d44 | src/main/** (pyramid) | active in baseline tree | docs/CONTENT_AUTHORITY_INDEX.md (G0-R1 approval) | live-client acceptance pending per baseline manifest; CI evidence is on tree-identical f5777244 | n/a |
| ACT-03 | ST-01 royal chest runtime (P8-B2) | work/st01-p8b2-royal-chest-runtime-20261110@03685656 | src/main/** (pyramid chest) | active in baseline tree | docs/planning/D13_03_I06_ROYAL_CHEST_REWARDS_AUTHORITY_2026-11-07.md | live-client acceptance pending per baseline manifest; CI evidence is on tree-identical f5777244 | n/a |
| ACT-04 | D13-01/02/03 accessory acquisition (i01-i06) | implement/d13-03-i06-p4-st01-integration-20261108@6c7923e0 | src/main/** (accessory) | active in baseline tree | docs/planning/D13_03_DECISIONS_Q01-Q47.md | live-client acceptance pending per baseline manifest; CI evidence is on tree-identical f5777244 | n/a |
| ACT-05 | Sky Tower (worldgen, vault rewards) | feature/sky-tower-integration-02@25e6ed6e | src/main/** (worldgen) | active in baseline tree | docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md | live-client acceptance pending per baseline manifest; CI evidence is on tree-identical f5777244 | n/a |
| ACT-06 | ST-05 Colosseum Stage 1 runtime (ArenaController, recovery) | checkpoint/wave16-unified-20261104@65072bca | src/main/java/.../colosseum | active in baseline tree | docs/STRUCTURE_AUTHORITY_INDEX.md (ST-05 lifecycle override 2026-11-02) | live-client acceptance pending per baseline manifest; CI evidence is on tree-identical f5777244 | n/a |

## DOCUMENTED

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| DOC-01 | Piglin trading and mob-tracker UI decisions (2026-10-10) | docs/ui-piglin-mob-tracker-20261010@6fe1a308 | docs/UI_PIGLIN_TRADE_MOB_TRACKER_APPROVED_2026-10-10.md | none (no implementation exists in baseline) | the document itself (user decisions); consistent with U04/U07 in docs/UI_SYSTEM_FULL_AUDIT_2026-10-10.md | currency vs later UI authority not confirmed by owner: routed as PENDING in the index | EXACT |
| DOC-02 | ST-01 Policy B terrain-adaptation research (draft PR #274) | research/st01-p8c3a-terrain-adaptation-20261111@357fc936 | inactive/st01-terrain-adaptation/ | none; research only | none (gates G1-G7 open; Policy D is final) | must not override Policy D; index hunk kept only in shared-file-hunks.patch | EXACT |
| DOC-03 | ST-05 P0 probe review package docs (8 docs) | audit/st05-po25-review-package-20261102@11c09064 | inactive/validation-st05-p0-probe/files/docs | none | pending: G-1/G-2 approval matrix not approved | approval status unconfirmed | EXACT |
| DOC-04 | Alchemy plot notes (non-authoritative) | docs/alchemy-plot-notes@cc1a96b0 | docs/preservation/grand-unified-20261112/ideas/alchemy-plot-notes/ | none | none (explicitly non-authoritative) | none | EXACT |
| DOC-05 | XP Quest UI integration checkpoint 2026-10-20 (historical) | codex/xp-quest-ui-integration-20261020@a124ccfc | docs/preservation/grand-unified-20261112/xp-quest/ | none | none (historical record of a blocked gate) | none | EXACT |

## INACTIVE — SOURCE CONSOLIDATED

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| SRC-01 | Sky Mob normal mobs (Cloud Slime, Cloud Wraith, Crystal Bug, Fallen Sentinel, Skyguard Archer) + client renderers, loot, 5 validation workflows | work/sky-mob-tower-integration-20261111@bd5ba43f + variants claude/sky-mob-p2b-p2c-vwuur8@55884b22, claude/sky-mob-gametest-timing-nn7o1h@9a54ae0b | inactive/sky-mob/(55 files, 295133 bytes) | OWNER-PAUSED; not compiled, not registered; registration hunks only in shared-file-hunks.patch | docs/design/SKY_MOB_NORMAL_MOBS_AUTHORITY_2026-11-06.md (inside inactive/sky-mob) | owner has not resumed mob development; variants of 2 further heads included under variants/ | EXACT (3 states) |
| SRC-02 | ST-01 P7-P11/O-14: secret entrance, gold guide golem, victory split, O-14 measurements | work/st01-o14-phase3-1-validation-20261109@7e37b786 | inactive/st01-o14-p7-p11/(158 files, 2925230 bytes) | BLOCKED; not compiled; its P7 docs/tools share paths with baseline's East-Royal-Hall P7 and are kept under the inactive prefix | docs/design/st01-p7..p11 (inside inactive/st01-o14-p7-p11) | runtime not reconciled with Policy D (see O2) | EXACT |
| SRC-03 | ST-01 Phase 4 production worldgen registration, probes, fixture, evidence | claude/st01-pyramid-phase4-worldgen-roke2l@ada10ba3 | inactive/st01-phase4/(11 files, 147225 bytes) | not compiled; its eligibility/placement changes conflict with Policy D lineage and are only in the patch | docs/design/st01-phase4 (inside inactive/st01-phase4) | owner acceptance checklist pending (O7) | EXACT |
| SRC-04 | ST-05 alternative Colosseum controller and structure-mask contract (incl. ancestor 17261362) | feat/st05-c4-structure-mask-20261030@1ee08d44 | inactive/st05-controller-structure-mask/(35 files, 332239 bytes) | REQUIRES_ISOLATION; not compiled; baseline Stage 1 ArenaController stays the active runtime | docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md | adoption/port decision pending (O3) | EXACT |
| SRC-05 | Q1C Wither-skull provenance (mixins, provenance class, tests, repair design) | codex/q1c-pr128-p1-p2-repair-20261019@9c9f2257 | inactive/q1c-wither-skull/(9 files, 57294 bytes) | not compiled; quest stays unregistered | docs/audits/wave13/WITHER_SKULL_F1_ANALYSIS.md (baseline) | forced-halt double-credit defect reproduced; D-F1-1 and O-2 not approved (O4) | EXACT |
| SRC-06 | Stage 23-B cloud live client-QA harness (clientQa source set) | codex/stage23b-cloud-live-client-qa@dad054c7 | inactive/client-qa-stage23b/(5 files, 71191 bytes) | not compiled; workflow not installed | none | overlap with current client validation lanes not compared | EXACT |
| SRC-07 | Underground-kingdom worldgen probe (experiment) | experiment/underground-kingdom-worldgen-probe@991b20c8 | inactive/validation-underground-kingdom-probe/(15 files, 380672 bytes) | not compiled; experiment only | docs/STRUCTURE_AUTHORITY_INDEX.md: experiment-only | none | EXACT |
| SRC-08 | ST-05 Stage 0 recovery/identity probes and tools | validation/st05-stage0-a02-a03-20261030@838e1366 | inactive/validation-st05-stage0-probes/(45 files, 251577 bytes) | not compiled; BLOCKED probes | docs/st05 decision records (baseline) | none beyond owner scheduling | EXACT |
| SRC-09 | Deferred Google Drive mirror tools, workflow, tests | claude/moreminecrafts-mirror-gate-race-20261111@79f10059 | inactive/drive-mirror/ (+ state-blobs) | DEFERRED; workflow stored as data, never installed | owner: mirroring deferred until major integration completes | 4 files omitted by security policy (EXT-04); runtime quarantine data approved_findings.json must come from the owner's security process | FUNCTIONALLY EQUIVALENT |
| SRC-10 | ST-05 P0 probe tooling (tools/st05-p0-probe-01): oracle, materializer, verifier, tests, digests | audit/st05-po25-review-package-20261102@11c09064 | inactive/validation-st05-p0-probe/state-blobs/primary/ + files/docs | REQUIRES_ISOLATION; not run by CI; workflow stored as data only | pending G-1/G-2 approval matrix | freeze_integrity.py (gitleaks generic-api-key false positive, line 23) is NOT in the repository; original bytes live in the external history archive; owner decides on a targeted suppression. 5 deterministic artifacts (~37 MB) are regenerated by recipes | PARTIAL |
| SRC-11 | PRC-P7, P7B, P7C security audit documents (3 of 4) | audit/prc-p7d-security-verification-20261027@20eb1d8e | inactive/prc-p7-security-audit/state-blobs/primary/docs/audits/ | none | audit evidence only (no authority) | P7D document stays external (EXT-02) | PARTIAL |

## INACTIVE — EXTERNAL REF PRESERVED

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| EXT-02 | PRC-P7D security remediation verification document | audit/prc-p7d-security-verification-20261027@20eb1d8e | (not in tree) | none | audit evidence | contains complete gitleaks fingerprints (an exception matrix); needed only as historical evidence | n/a |
| EXT-03 | Free-model delegation experiment (PR #141) | experiment/cloud-free-model-delegation-phase0@aa376713 | (not in tree) | none | experiment | OPTIONAL EXPERIMENT excluded from consolidation scope by the owner. contains scripts/free_model_delegation/test_delegation.py, the first entry of .gitleaksignore | n/a |
| EXT-04 | Mirror files with fingerprints / ignore entries (4 files) | claude/moreminecrafts-mirror-gate-race-20261111@79f10059 | (not in tree) | none | owner: mirror deferred | tools/drive_mirror/security/approved_findings.json, D-6_APPROVAL_PROPOSAL.md, docs/audit/REPO-STATE-20261110/evidence/secgate_result.json, patches/security/0001-PROPOSED-gitleaksignore-cec8bff8-NOT-APPROVED.patch | n/a |
| EXT-05 | Ponytail minimal-reporting skill experiment | experiment/ponytail-skill-2-20261021@f864df45 | (not in tree) | none | experiment | OPTIONAL EXPERIMENT excluded from consolidation scope by the owner. agent-instruction files under .github not reviewed; not copied | n/a |
| EXT-06 | Vanilla sweep VFX prototypes | prototype/stage20-vanilla-sweep-texture@b1830ce6; prototype/vanilla-sweep-texture@fe9d441f | (not in tree) | none | none found for the prototype VFX | OPTIONAL EXPERIMENT excluded from consolidation scope by the owner. modifies FlameSwordItem (conflict); no approval record found | n/a |
| EXT-07 | ST-01 O-14 empty git-auth check commit | test/git-auth-check@5927cddc | (not in tree) | none | none | empty commit, no content | n/a |
| EXT-09 | Original commit histories and interior stack tips of every family | all family refs listed in this registry | (not in tree) | none | none | history, authorship and interior states are NOT recoverable from Grand Unified; head states are. Not needed to resume development | n/a |

## SUPERSEDED

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| SUP-01 | Old production line (stage-12/13/14, stage-22/23, Q1C base, wave9-rc; stage-ci is contained in the baseline) | stage-14/checkpoint@94a264c2; release/wave9-rc-20261025@268ec65b | (history only) | none | superseded by baseline | evidence: baseline newer in 18-28 modified files per head and branch newer in 0; ancestry + tree, not full semantic review | n/a |
| SUP-02 | Patch-equivalent branches (wave10 integration/checkpoint, d13-03 authority, colosseum clock seam, stage-audit wave15) | integration/wave10-unified-20261025@73dcc8f4; docs/d13-03-authority-q01-q47-20261106@3ad9f283; remediation/st01-colosseum-clock-seam-20261110@f2c0a6c8; stage-audit/wave15-candidate-20261028@68de9706 | (history only) | none | patch-equivalent (git cherry) | commits not reachable from baseline: keep refs | n/a |
| SUP-03 | Pyramid mob roster and spider colony design docs | design/pyramid-mob-roster-20261026@a86c6d5b; design/spider-colony-biome-20261026@4aabb33e | docs/PYRAMID_MOB_ROSTER_DESIGN_2026-10-26.md; docs/SPIDER_COLONY_CAVE_BIOME_DESIGN_2026-10-26.md | none | docs identical to baseline copies | own commit history remains on the branches | EXACT |
| SUP-04 | ST-05 owner design record 2026-10-28 (historical copy) | design/st05-colosseum-owner-decisions-20261028@dd193d78 | docs/preservation/grand-unified-20261112/st05/ | none | superseded by docs/ST05_ANCIENT_COLOSSEUM_CURRENT_AUTHORITY_2026-10-29.md (Q1-Q118) | copy is historical only | EXACT |
| SUP-05 | Superseded doc states: sky-islands design context, content-first axis review, vanilla roster | docs/sky-islands-design-context-20261027; docs/2026-10-07-content-first-axis-review; recovery/vanilla-model-custom-mob-roster-base | inactive/docs-*/state-blobs/ | none | baseline versions newer | recoverable (tree-exact) as historical states | EXACT |

## REVERTED

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| REV-01 | ST-01 counterfactual rarity audit probe and tooling | audit/st01-p8c2b-counterfactual-rarity-20261111@569fc789 | (report doc already in baseline) | none | docs/integration/GRAND_UNIFIED_20261111_MANIFEST.md (REQUIRES_ISOLATION) | merged 2a1c23a0, reverted 01bf033d: compile failure vs Policy D St01PlacementConfig; rebase decision O8 | n/a |
| REV-02 | Colosseum absence-grace GameTest timing fix (PR #267) | fix/colosseum-absence-grace-ci-timing@576e7890 | (history only) | none | none recorded | original change reverted by 4db4f244 (reverts 6277e292); branch tip differs from baseline in 9 files: intent of re-application undecided (O8) | n/a |
| REV-03 | XpLevels class (main-only), replaced by exact RawXp | main@cde842dd | (absent in baseline) | none | exact raw-XP authority (CONTENT_AUTHORITY_INDEX XP Quest) | main-only file removed on the integration line by a13d9777 | n/a |

## OWNER DECISION REQUIRED

| ID | Feature | Source ref@SHA | Target | Activation | Authority | Blockers | Recovery |
|---|---|---|---|---|---|---|---|
| OWN-01 | Resume or register Sky Mob entities (paused) | see related rows | (none) | not activated | (owner) | owner has paused mob development; resume, register or spawn needs an explicit decision | n/a |
| OWN-02 | ST-01 P7-P11/O-14 secret-entrance runtime versus Policy D | see related rows | (none) | not activated | (owner) | Policy D accepts buried entrances; relation to gold-guide-golem runtime undecided | n/a |
| OWN-03 | ST-05 structure-protection mask versus baseline Stage 1 runtime | see related rows | (none) | not activated | (owner) | no baseline equivalent of StructureMaskSnapshot / ColosseumPlacementMixin; adopt, port or reject | n/a |
| OWN-04 | Q1C Wither-skull quest registration | see related rows | (none) | not activated | (owner) | D-F1-1 / O-2 unapproved; crash-consistency defect must be fixed first | n/a |
| OWN-05 | Policy B research gates G1-G7 | see related rows | (none) | not activated | (owner) | Policy D is final; Policy B stays research | n/a |
| OWN-06 | Star Covenant vault logic branch (i04a, 1 commit) | see related rows | (none) | not activated | (owner) | baseline has an evolved StarCovenantVaultRewards; semantic equivalence not verified | EXACT |
| OWN-07 | Phase 4 owner acceptance checklist | see related rows | (none) | not activated | (owner) | pending | n/a |
| OWN-08 | Counterfactual probe rebase; absence-grace fix re-application | see related rows | (none) | not activated | (owner) | see REV-01 / REV-02 | n/a |
| OWN-09 | Piglin-trading / mob-tracker UI: currency of the 2026-10-10 record and start of implementation | see related rows | (none) | not activated | (owner) | document routed as pending | n/a |
| OWN-10 | Google Drive mirror deployment | see related rows | (none) | not activated | (owner) | deferred until major integration complete | n/a |
| OWN-11 | Colosseum GameTest batch isolation hunk (inside claude/sky-mob-gametest-timing-nn7o1h) | see related rows | (none) | not activated | (owner) | separable from Sky Mob; overlap with baseline clock-seam work unchecked | n/a |
