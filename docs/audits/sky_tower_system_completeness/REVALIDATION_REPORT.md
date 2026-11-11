# SKY TOWER REVALIDATION REPORT
## Independent Systems & Design Authority Revalidation

**Revalidation Date:** 2026-11-09  
**Lead Systems Auditor:** Independent Senior Systems Auditor & Integration Engineer (Antigravity Agent)  
**Target Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Target Branch:** `feature/sky-tower-integration-02`  
**Integration Baseline (C-4):** `fdaa7effe96746ffe5e6e4454acc916cba277ff4`  
**Audit Intake Commit:** `6c48b8d7179eb3c04efdb1f6e2f8e046041056b1`  

---

## 1. Executive Summary & Audit Independence Statement

In accordance with `AGENTS.md` ("WHAT = Human, HOW = Agent"), an independent, second-stage revalidation of the Sky Tower audit was conducted. The initial audit (`6c48b8d7`) performed an extensive survey of the Sky Tower implementation, identifying 36 functional and structural requirements. However, critical evaluation reveals that the initial audit suffered from several **false positives**, **unreconciled authority conflicts**, and **confusion between implementation defects and deferred design scopes**.

### Core Revalidation Conclusions:

1. **The Technical State Machine is Sound & Stable:**
   The C-4 checkpoint (`fdaa7eff`) successfully and reliably implements the technical foundation:
   - Jigsaw single-composite world generation across all 6 sky island biomes.
   - 16 vanilla Trial Spawners with player scaling, cooldown, and quorum tracking.
   - 72-cell non-destructive barrier lifecycle (placement, entity avoidance, removal on clear, persistence across server restart).
   - SavedData persistence and corrupt data quarantining.
   - Fault-tolerant spawner destruction handling that prevents dungeon softlocks.

2. **The True Blocker is Authority Disconnect in the Progression Economy (Not Code Brokenness):**
   - The central gameplay issue in the Sky Tower is that the 8 Vaults use vanilla trial rewards (`minecraft:chests/trial_chambers/reward`) instead of the custom Sky Islands loot table (`moreminecrafts:chests/sky_islands/vault`).
   - The initial audit classified this as an outright engineering bug (`NOT_IMPLEMENTED`). In reality, this is an **Authority Conflict & Deferred Decision**: Q07 explicitly mandated preserving vanilla trial rewards to avoid unapproved custom managers, and INTEGRATION-02 preserved vanilla NBT to maintain SHA immutability for candidate stabilization. However, `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 establishes the Tier 2 progression requirement for Sky Feathers (15%) and Gale Shards (5%). The two authorities were never formally reconciled.

3. **Significant False Positives Dismissed:**
   - **`allFloorsCleared()` Caller Absence (`REQ-ROOF-03`):** The initial audit claimed that the lack of production callers for `SkyTowerProgress.allFloorsCleared()` was a critical defect. Revalidation confirms that no approved design authority ever specified a whole-tower completion event, title, broadcast, or sound. The method was implemented as a domain query helper. Calling its absence an implementation defect conflated an unused helper with an approved requirement.
   - **Worldgen Registration Denial on Entity Collision (`REQ-WGEN-07`):** The initial audit claimed that an entity standing in a doorway causes permanent silent failure. Revalidation of `SkyTowerWorldgenRegistration.java` disproves this: entities do not alter block state air checks, and `tick()` retries the pending registration queue every server tick.
   - **Advancements and XP Quests (`REQ-CROSS-01`):** The initial audit penalized the implementation for lacking Sky Tower advancements/quests. No approved design document ever mandated an advancement for the tower. This requirement was invented by the previous auditor.

4. **Genuine Implementation & Design Gaps Confirmed:**
   - **Rooftop Reward Absence (`REQ-VAULT-04`):** The rooftop sanctuary (Y=64..79) contains zero reward entities, leaving `vault_tower_top.json` orphaned.
   - **Mountain Biome Ceiling Truncation Risk (`REQ-WGEN-03`):** High mountain terrain (surface Y >= 200) paired with `start_height` up to 70 causes the 97-block composite to exceed Y=320.
   - **Weathering Phase Omission (`REQ-ARCH-06`):** Stage 2 weathering was omitted during FULL BUILD 01, leaving surfaces pristine clean stone brick.

---

## 2. Revalidation Triage of Disputed Findings

| Finding ID | Domain | Initial Audit Classification | Revalidation Classification | Implementation Status | Core Technical & Authority Rationale |
|---|---|---|---|---|---|
| **REQ-VAULT-02** | Rewards | NOT_IMPLEMENTED (Critical) | **AUTHORITY_CONFLICT** | DEFERRED_APPROVED / CONFLICT | Q07 explicitly froze vanilla rewards, while §275 mandated Sky Feather drops. INTEGRATION-02 froze NBT to maintain candidate stability. It is an authority conflict, not an engineering bug. |
| **REQ-ARCH-01** | Geometry | CONFLICT / PARTIAL (Medium) | **CORRECTED** | SUPERSEDED | Outer diameter expansion (D45 -> D50.16) was required to fit 4-block stairs and 3x3 pillars. Approved in FULL BUILD 01 candidate. |
| **REQ-ARCH-02** | Geometry | PARTIAL_IMPLEMENTATION | **CORRECTED** | SUPERSEDED | Central mosaic reduction (D17 -> D11) was required for 14-block spawner detection and mob navigation. Superseded by artifact approval. |
| **REQ-ARCH-04** | Geometry | PARTIAL_IMPLEMENTATION | **CORRECTED** | SUPERSEDED | Balconies aligned north/south rather than spiral offset for structural support. Superseded by artifact approval. |
| **REQ-WGEN-03** | Worldgen | DESIGN_INCOMPLETE (High) | **CONFIRMED** | DESIGN_INCOMPLETE (Medium) | 97-block height over mountain surface Y >= 200 with start_height up to 70 can exceed Overworld build limit Y=320. |
| **REQ-WGEN-07** | Worldgen | PARTIAL_IMPLEMENTATION (High) | **CORRECTED** | PARTIAL_IMPLEMENTATION (Low) | Entity collision claim is a false positive (entities are not blocks). Pending queue continuously retries every tick. Minor fragility if permanent block blocks doorway. |
| **REQ-ROOF-03** | Progression | NOT_IMPLEMENTED (High) | **REJECTED_FALSE_POSITIVE** | DESIGN_INCOMPLETE | No design document ever approved a whole-tower completion event. `allFloorsCleared()` is an internal query helper. Design remains incomplete. |
| **REQ-BARR-06** | Barriers | DESIGN_INCOMPLETE (High) | **CORRECTED** | COMPLETE_VERIFIED / DESIGN_INCOMPLETE | Phase B explicitly approved vanilla `minecraft:barrier`. Invisible collision is working as approved. Visual accents are a future design enhancement. |
| **REQ-SPAWN-06** | Combat | PARTIAL_IMPLEMENTATION (High) | **CORRECTED** | DEFERRED_APPROVED | Q07 explicitly prohibited unapproved custom mob/spawner pools. All-zombie baseline was an approved candidate simplification. |
| **REQ-VAULT-04** | Rooftop | DESIGN_INCOMPLETE (Critical) | **CONFIRMED** | NOT_IMPLEMENTED / DESIGN_INCOMPLETE | Rooftop has zero vaults/chests in NBT; `vault_tower_top.json` is completely unreferenced. |
| **REQ-CROSS-01** | Cross-System | NOT_IMPLEMENTED (Medium) | **REJECTED_FALSE_POSITIVE** | NOT_DESIGNED | Sky Tower advancement/quest was never requested or approved in any design authority. Fabricated by previous audit. |
| **REQ-ARCH-06** | Architecture | NOT_IMPLEMENTED (Medium) | **CONFIRMED** | NOT_IMPLEMENTED | Stage 2 weathering was explicitly omitted in FULL BUILD 01 report; stone remains pristine clean stone bricks. |

---

## 3. Deep Dive into the 10 Priority Audit Areas

### A. Sky Tower Vault Loot Tables (`REQ-VAULT-02`)
- **Authority Analysis:**
  - `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275: "하늘섬 금고 독립 보상: 돌풍 파편 5% ×1, 천공의 깃털 15% ×1... 기존 하늘섬 금고의 깃털 10% 또는 바닐라 시련 금고 돌풍 파편 지급 제안은 SUPERSEDED."
  - Data table `moreminecrafts:chests/sky_islands/vault.json` was authored to implement §275.
  - However, in `SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Q07, the owner ruled: "바닐라 시련 스포너 규칙과 보상을 유지... 커스텀 보상 풀은 별도 합의 전까지 추가하지 않는다."
  - In FULL BUILD 01, `full_01.nbt` placed 8 vanilla `minecraft:vault` blocks with `config.loot_table = "minecraft:chests/trial_chambers/reward"`.
  - In INTEGRATION-01 and INTEGRATION-02 (`INTEGRATION_02_REPORT.md` line 11), the developer deliberately preserved vanilla NBT to maintain SHA immutability (`16ad9d5`) and ensure test repeatability across C-1 through C-4.
- **Revalidation Finding:**
  This was an **approved engineering stabilization decision** addressing an unresolved authority conflict. It is not an unintentional defect or forgotten asset.
- **Resolution Path:**
  The owner must decide whether to update the Vault NBT in `full_01.nbt` to `moreminecrafts:chests/sky_islands/vault` (or inject the loot table via runtime BlockEntity modification) during Phase D.

### B. Approved Tower Geometry (`REQ-ARCH-01`, `REQ-ARCH-02`, `REQ-ARCH-04`)
- **Authority Analysis:**
  - Original Q03/Q25 specified D45 outer diameter.
  - Original Q42 specified D17 central mosaic pattern.
  - Original Q73 specified spiral offset balconies.
  - During FULL BUILD 01, the builder found that maintaining D45 made the 4-block wide continuous exterior staircase physically clip into the 3x3 structural columns and the 26-block combat arena. The outer diameter was adjusted to center D50.16 (D51 boundary). The central mosaic was reduced from D17 to D11 to preserve a 14-block clear detection and combat perimeter around the 4 Trial Spawners. Balconies were mirrored north/south to anchor into structural piers.
  - The builder documented all these changes in `docs/planning/sky_tower_full_build_01/DESIGN_CHANGES.md` and `FULL_BUILD_01_REPORT.md`.
  - The user approved moving forward from FULL BUILD 01 to INTEGRATION-01, C-2, C-3, and C-4, which explicitly locked `full_01.nbt` (`16ad9d5`).
- **Revalidation Finding:**
  The approval of the candidate artifact `full_01.nbt` **supersedes** the preliminary theoretical dimensions of Q01–Q118. In Minecraft voxel construction, physical clearance constraints override abstract diameter targets.
- **Resolution Path:**
  Update `docs/CONTENT_AUTHORITY_INDEX.md` and master architecture notes to record D50.16, D11 central plaza, and N/S balconies as approved permanent design authority.

### C. Mountain Worldgen Height Band (`REQ-WGEN-03`)
- **Technical Analysis:**
  - Composite structure height is 97 blocks (Y=0..96).
  - In `src/main/resources/data/moreminecrafts/worldgen/structure/sky_island_mountain_*.json`:
    `start_height: { "min_inclusive": 18, "max_inclusive": 70 }`, projected to `WORLD_SURFACE_WG`.
  - In mountain biomes (meadow, rocky, snowy peaks), `WORLD_SURFACE_WG` can generate at Y=180 to Y=230.
  - When `start_height` rolls near its maximum of 70 over peak terrain (e.g. Y=210 + 70 = Y=280), the top of the tower reaches Y = 280 + 96 = Y=376.
  - Vanilla Minecraft build height is capped at Y=320. Blocks above Y=320 cannot be generated by the chunk generator and are clipped.
- **Revalidation Finding:**
  **CONFIRMED (MEDIUM SEVERITY)**. This is a genuine worldgen configuration defect that can cause the rooftop sanctuary to be truncated when generating over high mountain peaks.
- **Resolution Path:**
  Adjust `sky_island_mountain_*.json` start_height to `min: 15, max: 40`, or configure a height clamp preventing structure start above Y=215.

### D. Worldgen Registration Denial (`REQ-WGEN-07`)
- **Technical Analysis:**
  - `SkyTowerWorldgenRegistration.anchorsPresent()` checks 16 spawners, 8 vaults, and 72 barrier cells (`!level.getBlockState(pos).isAir()`).
  - The initial audit claimed that an entity walking into a doorway causes permanent silent failure.
  - Revalidation shows:
    1. `getBlockState(pos).isAir()` checks the block state, NOT entity bounding boxes. Entities standing in doorways have zero effect on this check.
    2. `SkyTowerWorldgenRegistration.tick()` maintains pending sites in `PENDING` (a `Map<String, Pending>`). If chunks are not loaded or a temporary block is present, `tick()` executes `continue`, leaving the site in the queue to be evaluated again on the next server tick.
- **Revalidation Finding:**
  **CORRECTED (FALSE POSITIVE ON ENTITY BLOCKING & PERMANENT FAILURE)**. The registration system is an active polling retry loop. If snow falls or a player places a block in a doorway before all chunks finish loading, registration is deferred until the block is broken. This is a minor resilience issue, not a fatal design defect.

### E. Whole-Tower Completion Event (`REQ-ROOF-03`)
- **Technical Analysis:**
  - `SkyTowerProgress.java` provides `public boolean allFloorsCleared() { return completedMask == 0xffff; }`.
  - The class header explicitly notes: `"Combat fact only; sanctum arrival / overall-content completion is a separate undecided policy."`
  - Neither Q01–Q118, nor Context §158–§161, nor any C-stage integration document ever specified what a whole-tower completion event should do (sound, title, broadcast, or reward).
- **Revalidation Finding:**
  **REJECTED_FALSE_POSITIVE**. The lack of callers for `allFloorsCleared()` is not an implementation omission of an approved feature; it is an internal query method provided for future use once the policy is decided. The design for tower completion remains incomplete.

### F. Physical Barrier Visual Feedback (`REQ-BARR-06`)
- **Technical Analysis:**
  - `SkyTowerBarriers` places vanilla `minecraft:barrier` blocks (invisible in survival).
  - Q02 specified blocking movement. Q36 mentioned cyan highlights in design ideation.
  - During INTEGRATION-01 and INTEGRATION-02, the team explicitly selected vanilla `minecraft:barrier` to implement non-destructive collision without introducing custom block entities or client rendering dependencies (`BARRIER_LIFECYCLE.md`).
- **Revalidation Finding:**
  **CORRECTED**. The implementation is 100% compliant with approved Phase B engineering. Adding particles or translucent glass is a valid future aesthetic polish item, but calling the current invisible barrier an unapproved bug is incorrect.

### G. Spawner Mob Distribution (`REQ-SPAWN-06`)
- **Technical Analysis:**
  - In `full_01.nbt`, all 16 spawners are configured with `trial_chamber/melee/zombie/normal`.
  - Q07 explicitly forbade custom mob managers and unapproved mob pools.
  - FULL BUILD 01 used vanilla zombie spawners as a uniform test baseline.
  - No authoritative breakdown of vanilla mob roles per floor was ever finalized.
- **Revalidation Finding:**
  **CORRECTED**. The all-zombie baseline was an approved candidate simplification, not an unauthorized defect. Floor-by-floor diversification is a pending design decision.

### H. Rooftop Sanctuary Rewards (`REQ-VAULT-04`)
- **Technical Analysis:**
  - `sky_tower/full_01.nbt` contains zero vaults, chests, or interactive block entities on the rooftop.
  - `moreminecrafts:chests/sky_islands/vault_tower_top.json` exists in data assets with double unique drop rates.
  - Q08 and Q24 mandate a rooftop special reward.
- **Revalidation Finding:**
  **CONFIRMED IMPLEMENTATION & DESIGN GAP**. The rooftop is structurally complete but functionally empty. An owner decision is required to specify the container type and exact location.

### I. Weathering Phase (`REQ-ARCH-06`)
- **Technical Analysis:**
  - Q69–Q72 planned a two-stage construction: Stage 1 clean structure, Stage 2 environmental weathering pass (cracked, mossy stone bricks).
  - `FULL_BUILD_01_REPORT.md` line 21 noted: *"풍화 단계는 수행하지 않았다."*
  - The template was frozen in clean stone bricks.
- **Revalidation Finding:**
  **CONFIRMED (LOW/MEDIUM SEVERITY)**. Weathering was omitted during candidate construction. While not affecting gameplay or collision, it remains an unfulfilled visual design target.

### J. Advancements and Quests (`REQ-CROSS-01`)
- **Technical Analysis:**
  - The previous audit flagged `REQ-CROSS-01` as an implementation gap because no advancement or quest references `sky_tower`.
  - Repository search confirms that no task or design authority ever requested an advancement for Sky Tower.
- **Revalidation Finding:**
  **REJECTED_FALSE_POSITIVE**. Inventing an advancement requirement and penalizing the mod for not having it violates `AGENTS.md` ("WHAT = Human"). This is an out-of-scope proposal.

---

## 4. Summary of System Completeness

```
[==================== C-4 IMPLEMENTATION BASELINE ====================]
[  Worldgen Composite  ] -> COMPLETE_VERIFIED
[  16 Trial Spawners   ] -> COMPLETE_VERIFIED (Baseline Zombie)
[  72-Cell Barriers    ] -> COMPLETE_VERIFIED (Vanilla Invisible)
[  SavedData Persist   ] -> COMPLETE_VERIFIED (Server Restart Tested)
[  Spawner Destruction ] -> COMPLETE_VERIFIED (Quorum Protection)
-----------------------------------------------------------------------
[======================== GAPS REQUIRING ACTION =======================]
[  Vault Loot Table    ] -> AUTHORITY_CONFLICT (Q07 vs §275 / Deferral)
[  Rooftop Reward      ] -> DESIGN_INCOMPLETE (Missing Container in NBT)
[  Mountain Height WG  ] -> DESIGN_INCOMPLETE (start_height clamp needed)
[  Weathering Pass     ] -> NOT_IMPLEMENTED (Clean stone baseline)
-----------------------------------------------------------------------
[=========================== FALSE POSITIVES ==========================]
[  allFloorsCleared()  ] -> REJECTED (Unused helper, not missing hook)
[  Registration Denial ] -> CORRECTED (Active polling retry, not permanent)
[  Advancement/Quests  ] -> REJECTED (Never mandated by authority)
[  Geometry D50 vs D45 ] -> SUPERSEDED (Physical artifact approval)
```
