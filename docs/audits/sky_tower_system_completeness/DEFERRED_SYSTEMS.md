# SKY TOWER AUDIT — DEFERRED SYSTEMS & FUTURE INTEGRATION CONTRACTS

This document records all Sky Tower systems and features that were **explicitly postponed by user decision or formal checkpoint agreements**.

In accordance with `AGENTS.md` ("Group D — Explicitly deferred: Approved future systems... Do not automatically classify Group D as a defect"), these items are documented with their specific dependencies, authority sources, and future integration contracts.

---

### Deferred System 1: Custom Sky Realm Mob Pools (`REQ-SPAWN-07`)

#### 1. Authority & Postponement History
- **Source:** 
  - `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §160 intended for Sky Islands trial spawners to spawn custom mobs:
    - Spirit of the Sky (하늘의 정령)
    - Shulkers (셜커)
    - Floating Jellyfish (부유 해파리)
    - Sky variants of standard monsters (sky zombies, sky skeletons, sky creepers, sky slimes).
  - Explicit Postponement:
    - `docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Q07: *"신규 하늘섬 전용 몹 연동은 추후 별도 작업이다."* (Integration of new sky island-exclusive mobs is a separate later task).
    - `docs/planning/sky_tower_integration_02/PHASE_C4_CHECKPOINT.md`: *"Deferred by design: Custom Trial Spawner mob pools (after the custom mob roster / spawn configs exist)."*

#### 2. Technical Dependencies & Integration Contract
- **Blockers:**
  1. The custom Sky Realm mob entities currently live on separate feature branches (e.g., `implement/sky-mob-p1-entities-20261106`, `feature/sky-mob-p2b-cloud-wraith`). They are not yet integrated into the mainline mod registry (`ModRegistries.java`).
  2. Vanilla Trial Spawners configure mob pools via data-driven registry files in `data/<namespace>/trial_spawner/`. Currently, only `data/moreminecrafts/trial_spawner/sky_islands/shulker/` exists.
- **Future Integration Contract:**
  - When the sky mob roster merges, create dedicated trial spawner configurations under `data/moreminecrafts/trial_spawner/sky_tower/`.
  - Update `full_01.nbt` spawner block entities to reference `moreminecrafts:sky_tower/<mob_type>/normal` and `ominous`.
  - Ensure that `SkyTowerCompletionObserver` continues to listen to the vanilla `TrialSpawnerState.COOLDOWN` transition without changes (since vanilla handles custom mob lifecycles identically).

---

### Deferred System 2: Rooftop Special Reward Finalization (`REQ-VAULT-04`)

#### 1. Authority & Postponement History
- **Source:**
  - Q08: "각 층 일반 금고 + 최상층 특별 보상"
  - Q24: "최상층 특별 보상은 외곽 성역 제단"
  - `docs/planning/sky_tower_integration_01/DECISION_REQUIRED.md` Item 5: Flagged as requiring owner decision.
  - `docs/planning/sky_tower_integration_02/PHASE_C4_CHECKPOINT.md`: Listed under *"Deferred by design: Rooftop special rewards / whole-tower completion event."*

#### 2. Technical Dependencies & Integration Contract
- **Blockers:**
  - The container type, coordinates, key requirements, and interaction rules were never approved by the owner.
  - In `full_01.nbt`, the rooftop currently contains zero reward entities.
- **Future Integration Contract:**
  - Owner must select one of the engineering options in `DESIGN_GAPS.md` Gap 1.
  - Upon decision, place the designated container block in `sky_tower/full_01.nbt` linking to `moreminecrafts:chests/sky_islands/vault_tower_top.json`.
  - Regenerate the six composite `tower.nbt` templates via `scripts/structures/sky_islands/generate.py`.

---

### Deferred System 3: Second-Stage Aesthetic Weathering Pass (`REQ-ARCH-06`)

#### 1. Authority & Postponement History
- **Source:**
  - `docs/planning/SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Q71:
    > *"완성된 신전을 먼저 건축·구조·동선·기능 검증 후 별도 풍화 단계. 풍화는 재질 교체 및 비구조적 장식 손상까지만 허용. 원본 보존."*
  - `FULL_BUILD_01_REPORT.md` line 21 explicitly states:
    > *"풍화 단계는 수행하지 않았다. 부유 유물 이외의 석재는 하나의 면 연결 구조로 묶었다."*

#### 2. Technical Dependencies & Integration Contract
- **Future Integration Contract:**
  - Must not alter any structural boundary, walkable corridor width, spawner/vault anchor position, or barrier coordinate.
  - Substitute clean `minecraft:stone_bricks` with `minecraft:cracked_stone_bricks`, `minecraft:mossy_stone_bricks`, and `minecraft:andesite` according to the environmental zoning approved in Q69 and Q72 (moss on shaded lower levels, cracked/eroded stone on exposed upper walls).

---

### Deferred System 4: Live Graphical Client Walkthrough QA

#### 1. Authority & Postponement History
- **Source:**
  - `docs/planning/sky_tower_integration_02/PHASE_C4_CHECKPOINT.md` and `PHASE_C4_POST_QA_CHECKLIST.md`.
  - `AGENTS.md` §7: *"Human-perceptual or interaction-dependent checks such as gameplay feel, VFX quality, UI readability, or control feel may remain: LIVE VALIDATION PENDING."*

#### 2. Technical Dependencies & Integration Contract
- **Future Integration Contract:**
  - Requires a client environment with display/graphics acceleration (or interactive human tester).
  - Walk all 6 island variants from bridge through rooftop; record screenshots and gameplay footage.
