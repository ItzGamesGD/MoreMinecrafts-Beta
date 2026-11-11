# SKY TOWER AUDIT — REQUIREMENTS TRACEABILITY & AUTHORITY RESOLUTION

This document traces Sky Tower requirements from their original design sources through owner approvals, engineering adaptations, codebase implementation, and test verification. It also documents all **superseded requirements** and explains their authoritative resolution.

---

### 1. Requirements Traceability Matrix (Design -> Code -> Test)

| Req ID | Design Authority Source | Owner Approval Ref | Code / Asset Implementation Location | Test / Verification Artifact |
|---|---|---|---|---|
| **REQ-WGEN-01** | `SKY_ISLANDS_T2_CONTEXT` §159 | Approved 2026-10-27 | `data/.../template_pool/sky_islands/*/start.json` | C-3/C-4 dedicated server probe (`PHASE_C4_WORLDGEN_MATRIX.md`) |
| **REQ-WGEN-02** | `WORLDGEN_REPLACEMENT.md` | Approved in C-3/C-4 | `scripts/structures/sky_islands/tower_composite.py`<br>`data/.../structure/sky_islands/*/tower.nbt` | `test_tower_composite.py` (5 tests PASS); 0 legacy children verified |
| **REQ-WGEN-05** | `PHASE_C3_REPORT.md` | Approved in C-3/C-4 | `dev.itzgamesgd.moreminecrafts.skytower.SkyTowerWorldgenRegistration` | `SkyTowerWorldgenRegistrationTest.java`; dedicated server logs |
| **REQ-WGEN-06** | `ARCHITECTURE.md` | Approved in INT-01 | `SkyTowerData.java` line 79 | `SkyTowerDataTest.testOverlappingTowerRegistrationRejected` |
| **REQ-SPAWN-01** | Q05, Q17, Q25 | Approved 2026-11-05 | `data/.../structure/sky_tower/full_01.nbt` | Static NBT inspection (16 spawners); `verify_template.py` |
| **REQ-SPAWN-02** | Q07 | Approved 2026-11-05 | `net.minecraft.world.level.block.entity.trialspawner.TrialSpawner` (vanilla) | `SkyTowerGameTests.java` (real 1-player & 2-player combat PASS) |
| **REQ-SPAWN-03** | Q07 | Approved 2026-11-05 | Vanilla `TrialSpawner.tickServer` | `SkyTowerGameTests.java` (reward ejection & COOLDOWN state transition observed) |
| **REQ-SPAWN-04** | Q19 | Approved 2026-11-05 | `dev.itzgamesgd.moreminecrafts.mixin.SkyTowerTrialCompletionMixin`<br>`dev.itzgamesgd.moreminecrafts.skytower.SkyTowerCompletionObserver` | `SkyTowerCompletionObserverTest.java`<br>`SkyTowerProgressTest.java`<br>`SkyTowerGameTests.java` |
| **REQ-SPAWN-05** | Q22, `SPAWNER_DESTRUCTION.md` | Approved in INT-02 | `dev.itzgamesgd.moreminecrafts.mixin.SkyTowerBlockChangeMixin`<br>`dev.itzgamesgd.moreminecrafts.skytower.SkyTowerBlockChanges` | `SkyTowerGameTests.realVanillaTrialCombatSpawnerRemovedWhileActiveStillOpensFloor` |
| **REQ-SPAWN-08** | Q53–Q61 | Approved 2026-11-06 | `data/.../structure/sky_tower/full_01.nbt` | Static coordinates `(17\|31, floorY+2, 17\|31)`; in-game screenshots |
| **REQ-BARR-01** | Q02, Q21, `BARRIER_LIFECYCLE.md` | Approved in INT-02 | `dev.itzgamesgd.moreminecrafts.skytower.SkyTowerBarriers.java` | `SkyTowerGameTests.java`; `SkyTowerLifecycleTest.java` |
| **REQ-BARR-02** | `BARRIER_LIFECYCLE.md` | Approved in INT-02 | `SkyTowerBarriers.java` lines 54–60 | `SkyTowerLifecycleTest.testForeignBlockDefersPlacement` |
| **REQ-BARR-03** | Q19, Q20, `BARRIER_LIFECYCLE.md` | Approved in INT-02 | `SkyTowerBarriers.java` lines 41–53 | Real combat GameTests; restart probe (`floorclear_resume_7202.json`) |
| **REQ-BARR-04** | `BARRIER_LIFECYCLE.md` | Approved in INT-02 | `SkyTowerBlockChanges.java`<br>`SkyTowerBarriers.java` line 38 | `SkyTowerGameTests` waterlogged preservation test |
| **REQ-BARR-05** | Q22 | Approved 2026-11-05 | `SkyTowerProgress.java` (no protection/teleport hooks) | `SkyTowerGameTests.java` (player-placed blocks survive) |
| **REQ-VAULT-01** | Q08, Q18, Q25 | Approved 2026-11-05 | `data/.../structure/sky_tower/full_01.nbt` | Static NBT inspection (8 vaults at local `x=16, z=15\|33`) |
| **REQ-VAULT-03** | Q07, Q08 | Approved 2026-11-05 | Vanilla `VaultBlockEntity` | FULL BUILD 01 smoke test (`live_vaults.json`) |
| **REQ-ROOF-01** | Q09, Q40, Q41 | Approved 2026-11-05 | `data/.../structure/sky_tower/full_01.nbt` | Static NBT inspection (12 arches to Y=79; roofless) |
| **REQ-ROOF-02** | Q38, Q39 | Approved 2026-11-05 | `data/.../structure/sky_tower/full_01.nbt` | `connectivity.json` (intentional floating relic exception) |
| **REQ-ARCH-03** | Q04, Q29, Q43 | Approved 2026-11-05 | `data/.../structure/sky_tower/full_01.nbt` | `width.json` (walkable width 4–6 blocks); `live_walk.json` |
| **REQ-ARCH-05** | Q110–Q118 | Approved 2026-11-06 | `data/.../structure/sky_tower/full_01.nbt` | Gateway reliefs F1–F4 verified in NBT and screenshots |
| **REQ-DATA-01** | Q20, `ARCHITECTURE.md` | Approved in INT-01 | `dev.itzgamesgd.moreminecrafts.skytower.SkyTowerData.java` | `SkyTowerDataTest.java`; dedicated server restart probe |
| **REQ-DATA-02** | `ARCHITECTURE.md` | Approved in INT-01 | `SkyTowerData.java` lines 41–55 (`CODEC.decode`) | `SkyTowerDataTest` (corrupted mask, invalid version, JSON quarantine) |

---

### 2. Superseded Requirements & Authority Evolution

The design of the Sky Tower evolved significantly between the initial high-level vision (October 2026) and the detailed architectural Q01–Q118 process (November 2026). Below is the authoritative resolution of all superseded requirements:

#### A. Tower Structure Shape & Theme
- **Superseded Requirement:** `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §158 described the aerial trial tower as an *"이끼 낀 오래된 폐허 석탑"* (mossy old ruined square stone tower).
- **Authoritative Replacement:** `SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Initial Direction B & Q03 explicitly superseded the square tower with a **"풍화된 고대 원형 천공 신전탑"** (weathered ancient circular sky temple tower) featuring colonnades, grand arches, and a roofless sanctuary.

#### B. Floor Count
- **Superseded Requirement:** `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §158 proposed **5~7 stories**.
- **Authoritative Replacement:** Q25 explicitly locked the floor count to **exactly 4 combat floors + stone base + rooftop sanctuary** (6 distinct vertical stages, Y budget 10 + 11 + 12 + 14 + 17 + 16 = 80 blocks).

#### C. Floor Progression & Locking
- **Superseded Requirement:** `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §158 originally stated: *"별도 보스나 강제 잠금 층은 만들지 않음"* (Do not create separate bosses or forced locked floors).
- **Authoritative Replacement:** Q01 and Q02 explicitly established **staged progression with magical temple barriers**: players must defeat all 4 spawners on each floor to unlock the ascending stairway barrier to the next floor.

#### D. Rooftop Ceiling / Roof
- **Superseded Requirement:** Q09 originally described an annular arch sanctuary with *"외곽 부분 지붕"* (perimeter partial roof).
- **Authoritative Replacement:** Q41 (2026-11-05) explicitly revoked the partial roof in favor of Option D: **"무지붕 개방형 환형 성역"** (roofless open annular sanctuary), allowing the 12 arches to open directly to the sky.

#### E. Structure Protection Policy
- **Superseded Question:** Question Q23 proposed varying levels of adventure-mode style block protection.
- **Authoritative Replacement:** Q23 was **completely discarded** and Q22 was amended to confirm: **barriers block normal walking movement only**; players are fully permitted to bypass barriers via block breaking, block placement, ender pearls, or flight. No block-protection or teleport-return logic is allowed.

#### F. Sky Islands Vault Loot Table
- **Superseded Value:** Early drafts proposed a 10% Sky Feather chance in sky island vaults.
- **Authoritative Replacement:** `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §275 formally locked the authoritative rates: **15% Sky Feather ×1, 5% Gale Shard ×1**.

#### G. Balcony Outer Diameter Exception
- **Superseded Constraint:** Q26 established a strict **maximum outer diameter of 45 blocks** (D45).
- **Authoritative Replacement:** Q79 created an explicit, approved exception: the basic tower body retains D45, but **scenic balconies are permitted up to D51** (outer diameter 51 blocks).
