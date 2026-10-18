# XP Quest Implementation Handoff (2026-10-17)

Status: **documentation and implementation planning — no production code changed, Q1 not started.** Base: PR #112 (`da90f24` → this commit). Approved decisions remain in authority (`UI_ADVANCEMENT_INTEGRATION_APPROVED_2026-10-10.md` XP Quest amendment, `STAGE_2_GAMEPLAY_RECONCILIATION.md` Enhancement). Where this document and authority disagree, authority wins; required authority changes are listed in §8 for owner approval.

Status vocabulary: **CODE VERIFIED** (checked in repository/26.3 bytecode) · **DESIGN SPECIFIED** (mechanism defined, not implemented) · **TEST PENDING** · **LIVE PENDING** · **BLOCKED**. A design decision is never a passed test.

## 1. Owner-locked policies (2026-10-17)

- **Ore mining (revised and approved 2026-10-17):** player-placed ores never count. Only **verified naturally generated** ores count. Unknown origin is **not** assumed natural (§3.4).
- **Building:** every successful server-authoritative block placement counts, including repeats; failed/cancelled placement does not.
- **Retroactivity:** full recognition only from reliable persisted evidence; no invented history.
- **Ocean monument:** all verified participants are credited (Elder Guardian damage OR verified combat inside the monument; presence alone never counts).
- **Enhancement quests:** possession of qualifying equipment is sufficient, regardless of who enhanced it.
- **Research expenditure:** evaluated as the cumulative approved raw-XP value of currently owned research.

## 2. Final 200-quest status and ledger

| Class | Count | XP | Meaning |
|---|---:|---:|---|
| NOW | 160 | 156,050 | Survival-achievable; server detection DESIGN SPECIFIED; register only after its automated test passes |
| VERIFY | 20 | 23,950 | Held — mechanism or data still unverified (§5) |
| FUTURE | 20 | 29,100 | SURVIVAL-LOCKED (no recipe/loot route — CODE VERIFIED absent) |
| **Active (NOW+VERIFY)** | **180** | **180,000** | Approved ledger |
| **Total** | **200** | **209,100** | Approved ledger |

Independently re-verified from the regenerated catalog A: 200 unique IDs; every per-quest XP identical to `da90f24`; all values multiples of 50. Reclassification since the audit: 62 NOW\* → NOW (DESIGN SPECIFIED); `exp.biomes_all_overworld`, `boss.monument_clear`, `boss.monuments_3` NOW → VERIFY; `alc.shock_potion`, `eqp.custom_enchant` VERIFY → NOW (shock: 26.3 `BrewingRecipe` + `recipe/brewing/shock.json`; custom enchant: all 11 MM enchantments are `EnchantmentTrees` nodes — test still required). NOW/VERIFY moves do not change any reward or the active total.

**Retroactivity corrections applied to A:** 13 capped-counter quests are now *partial* (`R부분`); 3 ore-count and 3 mining-type quests lose retroactivity (Knowledge/vanilla history cannot exclude placed ores); 5 stat quests and 4 bestiary quests read vanilla statistics directly (full).

## 3. Shared tracking architecture (minimal)

### 3.1 Storage

Reuse the existing player attachment `KnowledgeRuntime.STATE` (`ResearchProgressState`, flat `Map<String,Integer>`, persistent + copyOnDeath, already snapshot/delta-synced). New key families only: `quest.counter.<key>` (uncapped; saturate at `Integer.MAX_VALUE`), `quest.seen.<questId>.<entry>` (distinct sets, bounded by each quest's target), existing `quest.progress/complete/claimed.<id>`. No codec change. World-scoped data (ore provenance, monument participants, pending offline credits) uses chunk attachments / `SavedData` following `K5NaturalTreeProvenance` and `MonumentRewardData`.

### 3.2 Uncapped counters (fixes D-01)

`QuestCounters.add(player, key, n)` is called *beside* each existing Knowledge recorder (one added line per hook; Knowledge counters, caps and unlocks unchanged). Seeding: on first evaluation, `counter = max(counter, condition.<key>)` for counters whose Knowledge history counts the same events (13 quests); never for ore counts.

### 3.3 Vanilla statistic reader

`QuestStats` reads `ServerStatsCounter`: CUSTOM (*_ONE_CM, ANIMALS_BRED, TRADED_WITH_VILLAGER), ENTITY_KILLED, BLOCK_MINED, and ITEM_USED summed over `BlockItem`s (26.3 `ItemStack.useOn` awards ITEM_USED only on success — CODE VERIFIED). Evaluated on login and on the existing 100-tick refresh. Full retroactivity.

### 3.3a Block placement — Q1-B replacement requirement (verified 2026-10-17 against 26.3 bytecode)

How the current Q1-A tracker works: `stat_block_items_used` sums `Stats.ITEM_USED` over `BlockItem`s.

- **No over-counting:**
  - `ItemStack.useOn` awards `ITEM_USED` only when `Item.useOn` returns `InteractionResult.Success` with `wasItemInteraction()`.
  - `BlockItem.useOn` returns the result of `BlockItem.place`.
  - `place` returns `SUCCESS` only after `placeBlock` succeeds; every other path returns `FAIL`.
  - So every counted use is a real successful placement.
- **Under-counting — not guaranteed complete, violates policy 2:**
  - `PlaceOnWaterBlockItem.use` (lily pad, frogspawn) calls `BlockItem.useOn` directly. It never goes through `ItemStack.useOn`, so those placements never award `ITEM_USED`.
  - Any mod item that calls `BlockItem.useOn`/`place` directly is missed the same way.
- **Q1-B requirement:**
  - Hook `BlockItem.place` at RETURN. When the result is `InteractionResult.Success` and the context player is a `ServerPlayer`, add 1 to `quest.counter.blocks_placed`, and record the placed block for the variety quests.
  - This hook is the live source of truth. It covers the `useOn` and `use` paths alike.
  - Vanilla `ITEM_USED` over `BlockItem`s stays the retroactive seed (policy 3). Q1-B owner task (2026-10-17): the seed is captured **once** per player as `quest.baseline.block_items_used` (at join, or before the first live event), and progress = baseline + live counter, so every confirmed placement after the update counts and none is counted twice.
  - Required GameTests:
    - normal placement +1;
    - lily pad on water +1;
    - occupied or refused placement +0;
    - door = one placement;
    - stat seed applied once and never double-counted against the hook.
- **Registration:**
  - `xpq.bld.place_1000/10000/50000/200000` and `xpq.bld.variety_50/150` stay **unregistered** until this hook and its tests land.
  - The `catalog.json` trackers for these six quests change from `stat_block_items_*` to the counter-plus-stat-seed form in Q1-B.

### 3.4 Ore provenance (revised 2026-10-17 — supersedes the earlier "unknown counts as natural" text)

`K5NaturalTreeProvenance` establishes the positive-marking precedent. Q1-B originally used a tracked-chunk flag plus absence from a `nonNatural` set. The independent review rejected that negative evidence: unobserved writes could acquire a false natural verdict. The correction uses a new persistent `moreminecrafts:quest_ore_provenance_v2 {schema, natural: Set<Long>}` attachment. The v1 attachment is ignored on existing saves.

- **Positive evidence:** the vanilla `OreFeature` direct chunk-section write and `ScatteredOreFeature` world-generation write record a position only after the write, inside natural biome decoration. Positions are recorded in the chunk actually written, including adjacent chunks.
- **Invalidation:** `Level.setBlock` and `WorldGenRegion.setBlock` consume positive evidence on block-identity replacement. This covers ordinary placement, piston departure, explosions, commands, structure writes and later decoration. Same-block state changes, including lit redstone ore, preserve it. A piston destination receives no marker even if the source was natural; this owner decision applies to every tracked ore.
- **Break verdict:** only a recorded position is natural. Missing, old-schema or unmarked positions fail closed. The pre-break verdict is captured before block removal and credited only on the corresponding successful `PlayerBlockBreakEvents.AFTER` event.
- **Trust boundary:** an unrelated mod or direct chunk-section writer that bypasses both setters can replace a marked position without invalidation. This is not proven exact for arbitrary mod writes; tests cover the vanilla paths above. Unknown/pre-update ore remains ineligible rather than receiving inferred history.
- **Existing-world compatibility:**
  - In pre-update chunks, ores without v2 positive markers are *unknown* and never count. This is conservative and consistent with policy 3: no invented history.
  - Players progress in newly generated terrain. Reported as a gameplay consequence: LIVE PENDING, balance.
  - No reliable evidence exists to classify pre-update ores. Chunk `InhabitedTime` and vanilla statistics cannot tell placed ores from natural ones.
- **Status:**
  - The mining-count and mining-type quests stay **NOW (TEST PENDING)** only if the corrected positive-marker GameTests pass for natural, placed, piston, setblock, explosion, restart and unknown-origin cases.
  - If those tests cannot prove exactness, the affected conditions move to VERIFY per policy 1.
- **Batch:** implemented in **Q1-B**. It does not block Q1-A/A2, which only provide the counter they increment.

### 3.5 Distinct-ID tracking

`QuestRules.observeDistinct(state, quest, entry)` writes `quest.seen.*` once and sets progress = seen count. Reads Knowledge `condition.<cond>.seen.<id>` keys (uncapped — CODE VERIFIED) for biomes, structures, fish species, water biomes, potion types, effects, brewing ingredients, log types.

### 3.6 Event hooks

Block placement success; block break (provenance); `ServerLivingEntityEvents.AFTER_DEATH` with `getKillCredit()`; `LootTableTrigger.trigger(ServerPlayer, key)` (covers block containers and chest-vehicle entities — both call GENERATE_LOOT, CODE VERIFIED); `VaultServerData.addToRewardedPlayers`; `EnderDragonFight.setDragonKilled` (capture `hasPreviouslyKilledDragon()` first); raid victory (`Raid.isVictory`, `getRaidOmenLevel`, heroes set); `Player.awardStat(Identifier,int)` for per-dimension movement; TAME_ANIMAL sites (`TamableAnimal`, `AbstractHorse`); crafting result pickup (recipe ID); `FishingChallenges` catch, `FishermanFishSale` payment; brewing output; `EnhancementOperation` success. All server-side; creative not specially handled (owner decision).

### 3.7 Combat & structure attribution

Monument ID = dimension + structure start chunk (existing MonumentRewardData key). Evidence recorded while the monument is uncompleted: (a) Elder Guardian inside the monument bounds damaged by a player (`DamageSource.getEntity()` resolves projectiles/owned entities), or (b) the player, inside the bounds, deals damage to or receives damage from a Guardian/Elder Guardian. Window: every player with any Elder-Guardian-damage evidence, plus players with combat evidence within the last 12,000 game ticks before completion. Completion includes the existing deferred (chunk-unload) path. Participants stored in the pending entry; offline participants receive a pending credit applied on next login; one credit per player per monument.

### 3.8 Equipment possession

Scan main inventory (36), armour (4), offhand (1) every 100 ticks, on login and after a successful enhancement. Excluded: nested containers (shulker boxes, bundles, backpacks), ender chest, world containers, cursor/crafting grids and other transient menu slots. Completion is monotonic (losing the item later does not revoke).

### 3.9 Research value

`Σ owned research levels × approved ledger rawXp` from `ResearchCatalog` definitions (includes migrated roots). Derived; not stored.

### 3.10 Retroactive reconciliation

On every login: evaluate STAT/STATE/SEEN quests, apply seeds once, apply pending monument credits, then emit a single snapshot. Completion flags only move 0→1. Existing-world updates therefore complete eligible quests at first login (policy 3); rewards still require manual claim.

### 3.11 Exactly-once completion and claim

Completion: set `quest.complete.<id>` once (server thread). Claim: existing `QuestProgressService.claim` order kept (check → mark claimed → grant). Grant uses the approved exact raw-XP path: compute new total, `setExperienceLevels(L)` then `setExperiencePoints(r)` and set `totalExperience` to the same total (setters reset `lastSentExp` → `doTick` sends `ClientboundSetExperiencePacket`; CODE VERIFIED). If the grant throws, restore level/progress/total and clear `claimed` before returning `INVALID_STATE`. Refuse while dead/disconnected (`UNAVAILABLE`, appended to the enum). Unknown/incomplete IDs refused (existing).

### 3.12 Sync & UI

State reaches the client through the existing snapshot/delta payloads. The client synthesises a `ClientAdvancements` subclass and opens vanilla `AdvancementsScreen` (D document). Toast only on a delta flipping `quest.complete` 0→1, never on snapshot (fixes D-20). Pending claim cleared on result payload, claimed delta, 5 s timeout, screen reopen or reconnect (fixes D-21).

### 3.13 Q1-B implementation record (2026-10-17)

- **Baselines:** history enters once per key through `quest.baseline.<key>` (placement: the `ITEM_USED` sum over `BlockItem`s; capped counters: the stored `condition.<key>`). The live counter then adds only events after capture. This is equivalent to the `max(counter, condition)` seed of §3.2 for the counters, and it counts post-update lily pads and other `use`-path placements that `ITEM_USED` misses.
- **Uncapped counters:** `KnowledgeRuntime.recordCondition` forwards each report to `XpQuestHooks`. For the twelve increment-style collectors (all report `stored + n`), the delta = reported − stored is the event amount, including past the Knowledge cap.
- **Ore provenance:** §3.4 now records the owner-approved piston rule and the positive-evidence v2 repair. Piston-moved ore does not count, including cross-chunk movement. Explosion, piston departure and replacement consume the source marker. The eight MoreMinecrafts ore variants count for `min.first_custom_ore`; adamantium debris is excluded there and remains eligible for `min.adamantium_10`. Rewards and the catalog ledger are unchanged.
- **Registered in Q1-B after independent source gate (30):**
  - **Placement:** `bld.place_1000/10000/50000/200000`, `bld.variety_50/150`.
  - **Ore:** `min.ore_1000/5000/20000`, `min.diamond_ore_64`, `min.debris_16`, `min.adamantium_10`, `min.first_custom_ore`, `min.custom_ore_survey`, `min.deepslate_custom`.
  - **Counters:** `min.logs_1000`, `min.pickaxe_5000`, `frm.till_1000`, `frm.barter_200`, `cft.anvil_50`, `cft.redstone_100`, `exp.archaeology_30`, `cmb.shield_500`, `cmb.trident_50`, `alc.potions_250`.
  - **Other events:** `frm.tame_5`, `dim.nether_travel_50k` (every CUSTOM `*_one_cm` statistic awarded in the Nether), `fsh.first_sale/sale_64/emerald_500`.
  - **Source-gated/unregistered (9):** `frm.harvest_1000/5000`, `fsh.catch_500`, `cft.sleeping_bag`, `cft.parachute`, `cft.all_daggers`, `cft.all_greatswords`, `cft.mm_blocks`, `frm.composite_foods`. Their IDs, targets, rewards and any saved evidence remain in the catalog. Mature-crop harvest, successful custom fish catch and all approved crafting-result statistics need focused live player-attribution and rejection tests before registration.
  - Every other NOW quest stays unregistered until its own detection and test exist.
- **Catalog target correction:** `catalog.json` had set-quest targets defaulted to 1 by the generator. They now match catalog A's entry counts, for example `all_daggers` 7. Rewards and the ledger are unchanged.

## 4. 82-quest reconciliation (62 audit NOW\* + 20 VERIFY)

Common columns: persistence is the player attachment unless stated; deduplication is `quest.complete` once + bounded `quest.seen`. Registration: NOW rows register only after their listed test passes; VERIFY rows stay unregistered.

| # | ID | Audit → now | XP | Completion condition | Server detection | Counter / set semantics | Retroactivity | Attribution | Persistence | Dependency | Required automated test | Status |
|---:|---|---|---:|---|---|---|---|---|---|---|---|---|
| 1 | `xpq.exp.first_structure_loot` | NOW* → NOW | 250 | 생성 구조물 전리품 상자(loot table 경로 chests/*)를 처음 열어 전리품 생성 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 2 | `xpq.exp.mineshaft_loot` | NOW* → NOW | 250 | chests/abandoned_mineshaft 상자 전리품 생성 1회 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 3 | `xpq.exp.dungeon_loot` | NOW* → NOW | 250 | chests/simple_dungeon 상자 전리품 생성 1회 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 4 | `xpq.exp.temples` | NOW* → NOW | 350 | chests/desert_pyramid 와 chests/jungle_temple 전리품 각 1회 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 5 | `xpq.exp.ocean_relics` | NOW* → NOW | 500 | shipwreck(supply/map/treasure 중 1), underwater_ruin(1), buried_treasure 전리품 각 1회 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 6 | `xpq.exp.archaeology_30` | NOW* → NOW | 500 | 고고학 솔질로 아이템 30개 획득 | 기존 Knowledge 훅(`archaeology_items_brushed` 기록 지점)에 `QuestCounters.add(player,"archaeology_items_brushed",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.archaeology_items_brushed` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 7 | `xpq.exp.sleeping_bag_biomes` | VERIFY → VERIFY | 500 | 탐험용 침낭으로 서로 다른 생물군계 5곳에서 수면(밤 넘김) | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 침낭 수면이 밤을 넘기는지·이벤트 지점 미확인 | VERIFY 유지 |
| 8 | `xpq.exp.parachute_landing` | VERIFY → VERIFY | 500 | 낙하산을 펼친 상태로 60블록 이상 낙하 후 피해 없이 착지 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 낙하산 착지 판정 지점 미확인 | VERIFY 유지 |
| 9 | `xpq.exp.sail_20k` | NOW* → NOW | 1,150 | 보트 이동 거리 20,000블록 | 바닐라 통계 직접 조회(BOAT/HORSE/AVIATE_ONE_CM ÷100, ANIMALS_BRED, TRADED_WITH_VILLAGER) | 통계 값 | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 통계 → 진행 일치, Knowledge 상한과 무관 | DESIGN SPECIFIED · TEST PENDING |
| 10 | `xpq.exp.ride_10k` | NOW* → NOW | 1,000 | 말 이동 거리 10,000블록 | 바닐라 통계 직접 조회(BOAT/HORSE/AVIATE_ONE_CM ÷100, ANIMALS_BRED, TRADED_WITH_VILLAGER) | 통계 값 | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 통계 → 진행 일치, Knowledge 상한과 무관 | DESIGN SPECIFIED · TEST PENDING |
| 11 | `xpq.exp.elytra_25k` | NOW* → NOW | 1,100 | 겉날개 비행 25,000블록 | 바닐라 통계 직접 조회(BOAT/HORSE/AVIATE_ONE_CM ÷100, ANIMALS_BRED, TRADED_WITH_VILLAGER) | 통계 값 | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 통계 → 진행 일치, Knowledge 상한과 무관 | DESIGN SPECIFIED · TEST PENDING |
| 12 | `xpq.exp.biomes_all_overworld` | VERIFY → VERIFY | 3,150 | 고정 목록(26.3 바닐라 자연 생성 생물군계 — 오버월드·네더·엔드, the_void 제외) 전부 방문. 목록 확정 전 미등록 | Knowledge 바이옴 관측 키 읽기 | 고정 목록(26.3 바닐라 생성 바이옴; the_void 제외) 대비 고유 집합 — 이름을 "모든 차원의 생물군계"로 정정 필요 | 완전 소급(관측 키) | 관측 플레이어 | 기존 키 | 26.3 생성 바이옴 목록 추출·고정 | Unit: 목록 완전성 / 데이터팩 바이옴 무시 | VERIFY 유지 — 목록 확정 전 등록 금지 |
| 13 | `xpq.dim.fortress_loot` | NOW* → NOW | 350 | chests/nether_bridge 전리품 1회 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 14 | `xpq.dim.trial_vaults_5` | NOW* → NOW | 500 | 일반 금고(vault, ominous=false) 서로 다른 5개 개방 | mixin `VaultServerData.addToRewardedPlayers(Player)`(26.3 확인) — ominous 상태로 분기 | 고유 금고 키(차원+좌표) | 소급 없음 | 보상받은 플레이어 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 금고당 1회, 일반/불길 분리 | DESIGN SPECIFIED · TEST PENDING |
| 15 | `xpq.dim.ancient_city_loot` | NOW* → NOW | 900 | chests/ancient_city 전리품 3회(서로 다른 상자) | 루트 훅 — ancient_city(+ice_box), 고유 상자 3 | 고유 상자 3 | 소급 없음 | 개봉자 | `quest.seen` | Q1-C | GameTest | DESIGN SPECIFIED · TEST PENDING |
| 16 | `xpq.dim.bastion_all` | NOW* → NOW | 1,350 | 보루 상자 4종(bridge/hoglin_stable/other/treasure) 전부 전리품 생성 | `LootTableTrigger.trigger(ServerPlayer, ResourceKey)` 중앙 훅 — 블록 상자(RandomizableContainer.unpackLootTable)와 상자 수레(ContainerEntity, 26.3 확인) 모두 경유 | 조건별 loot table 고유 집합 / 상자 수는 고유 키(블록=차원+좌표, 엔티티=UUID) | 소급 없음(바닐라 GENERATE_LOOT 이력은 발전과제 일부에만 존재) | 최초 개봉(전리품 생성) 플레이어만. 호퍼 추출(null 플레이어)은 무집계 — 의도된 제한 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 블록·수레 각각 / 재개봉 무집계 / 다른 플레이어 선개봉 시 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 17 | `xpq.dim.ominous_vaults_5` | NOW* → NOW | 1,350 | 불길한 금고 서로 다른 5개 개방 | mixin `VaultServerData.addToRewardedPlayers(Player)`(26.3 확인) — ominous 상태로 분기 | 고유 금고 키(차원+좌표) | 소급 없음 | 보상받은 플레이어 | `quest.seen.<id>.<key>` | Q1-C | GameTest: 금고당 1회, 일반/불길 분리 | DESIGN SPECIFIED · TEST PENDING |
| 18 | `xpq.dim.end_cities_3` | NOW* → NOW | 1,100 | 서로 다른 엔드 도시 구조물(시작 청크 기준) 3곳 진입 | Knowledge 구조물 관측 루프에서 end_city 구조물 시작 청크 키 기록 | 고유 시작 청크 집합 | 소급 없음(기존 관측은 구조물 종류만) | 진입 플레이어 | `quest.seen` | Q1-C | GameTest: 같은 도시 재진입 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 19 | `xpq.dim.end_city_loot_10` | NOW* → NOW | 3,150 | chests/end_city_treasure 서로 다른 상자 10개 전리품 생성 | 루트 훅(위) — end_city_treasure, 고유 상자 키 | 고유 상자 10 | 소급 없음 | 개봉자 | `quest.seen` | Q1-C | GameTest | DESIGN SPECIFIED · TEST PENDING |
| 20 | `xpq.dim.nether_travel_50k` | NOW* → NOW | 2,500 | 네더 차원에서 지급된 이동 통계(cm) 합계 50,000블록 | mixin `Player.awardStat(Identifier,int)` HEAD: 이동 통계(WALK/SPRINT/CROUCH/SWIM/FLY/AVIATE/BOAT/HORSE/STRIDER… _ONE_CM)이고 현재 차원이 the_nether면 같은 cm를 카운터에 더함 | `quest.counter.nether_travel_cm` | 소급 없음(바닐라 통계에 차원 구분 없음 — 확인) | 이동한 플레이어 | 플레이어 어태치먼트 | Q1-B | GameTest: 네더 이동만 증가, 오버월드 무증가, 텔레포트(통계 미지급) 무증가 | DESIGN SPECIFIED · TEST PENDING |
| 21 | `xpq.cmb.mm_arms` | NOW* → NOW | 250 | 단검과 대검으로 각각 적대 몹 1마리 처치 | 처치 크레딧 + 주손 아이템을 신규 아이템 태그 `#moreminecrafts:daggers` / `#moreminecrafts:greatswords`(레지스트리 ID 7종씩) 로 판정 | 종류별 처치 카운터/집합 | 소급: 바닐라 ENTITY_KILLED는 무기 정보 없음 → 소급 없음 | 킬 크레딧 플레이어(투사체 아님 — 근접 직접 공격) | `quest.counter`/`quest.seen` | Q1-B + 데이터 태그 | GameTest: 검 처치 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 22 | `xpq.cmb.bestiary_5` | NOW* → NOW | 250 | MoreMinecrafts 적대 몹 5종 처치 | 바닐라 `Stats.ENTITY_KILLED`에서 moreminecrafts 네임스페이스·적대(MobCategory.MONSTER) 엔티티 타입 집계 | 고유 타입 수 | 완전 소급(통계) | 킬 크레딧 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest | DESIGN SPECIFIED · TEST PENDING |
| 23 | `xpq.cmb.shield_500` | NOW* → NOW | 450 | 방패로 공격 500회 막기 | 기존 Knowledge 훅(`shield_blocks` 기록 지점)에 `QuestCounters.add(player,"shield_blocks",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.shield_blocks` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 24 | `xpq.cmb.dagger_100` | NOW* → NOW | 500 | 단검으로 적대 몹 100마리 처치 | 처치 크레딧 + 주손 아이템을 신규 아이템 태그 `#moreminecrafts:daggers` / `#moreminecrafts:greatswords`(레지스트리 ID 7종씩) 로 판정 | 종류별 처치 카운터/집합 | 소급: 바닐라 ENTITY_KILLED는 무기 정보 없음 → 소급 없음 | 킬 크레딧 플레이어(투사체 아님 — 근접 직접 공격) | `quest.counter`/`quest.seen` | Q1-B + 데이터 태그 | GameTest: 검 처치 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 25 | `xpq.cmb.greatsword_100` | NOW* → NOW | 500 | 대검으로 적대 몹 100마리 처치 | 처치 크레딧 + 주손 아이템을 신규 아이템 태그 `#moreminecrafts:daggers` / `#moreminecrafts:greatswords`(레지스트리 ID 7종씩) 로 판정 | 종류별 처치 카운터/집합 | 소급: 바닐라 ENTITY_KILLED는 무기 정보 없음 → 소급 없음 | 킬 크레딧 플레이어(투사체 아님 — 근접 직접 공격) | `quest.counter`/`quest.seen` | Q1-B + 데이터 태그 | GameTest: 검 처치 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 26 | `xpq.cmb.bestiary_10` | NOW* → NOW | 500 | MoreMinecrafts 적대 몹 10종 처치 | 바닐라 `Stats.ENTITY_KILLED`에서 moreminecrafts 네임스페이스·적대(MobCategory.MONSTER) 엔티티 타입 집계 | 고유 타입 수 | 완전 소급(통계) | 킬 크레딧 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest | DESIGN SPECIFIED · TEST PENDING |
| 27 | `xpq.cmb.cave_dwellers` | VERIFY → VERIFY | 500 | cave_zombie, cave_skeleton, cave_creeper, small_spider, swamp_spider, cave_bat 각 1종 처치(총 6종; cave_bat은 비적대 AMBIENT지만 이 퀘스트에 한해 포함) | 플레이어별 `Stats.ENTITY_KILLED` 종별 조회(`stat_killed_each`); 별도 AFTER_DEATH 집계는 사용하지 않음 | 확정된 여섯 타입 중 통계가 1 이상인 타입 수; cave_bat만 이 퀘스트의 명시적 AMBIENT 예외 | 완전 소급(영속 바닐라 통계) | 통계를 보유한 플레이어의 최종 타격 처치 | 바닐라 stats 파일 | Owner roster와 여섯 등록 ID 확정; evaluator는 이미 `stat_killed_each` 지원 | Unit: ID/VERIFY 가드 + GameTest: 6종/중복/타인/비대상/소급/재시작 | VERIFY 유지; 구현 검증 완료, 등록은 별도 승인 필요 |
| 28 | `xpq.cmb.trident_50` | NOW* → NOW | 450 | 삼지창 투척으로 적대 몹 50마리 처치 | 기존 Knowledge 훅(`trident_hostile_kills` 기록 지점)에 `QuestCounters.add(player,"trident_hostile_kills",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.trident_hostile_kills` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 29 | `xpq.cmb.bestiary_25` | VERIFY → VERIFY | 1,350 | MoreMinecrafts 적대 몹 25종 처치 | 플레이어별 `Stats.ENTITY_KILLED`에서 MoreMinecrafts namespace의 `MobCategory.MONSTER` 타입 고유 집계 | 실제 레지스트리 Monster 타입 중 통계가 1 이상인 타입 수 | 완전 소급(영속 바닐라 통계) | 통계를 보유한 플레이어의 최종 타격 처치 | 바닐라 stats 파일 | 37종 모두 실제 등록, 자연 스폰 추가, 실존 바이옴 태그·배치 유형·바닐라 통계를 런타임 감사; 도달 가능 25종 이상 | GameTest: 37종 registry/tag/biome/placement/stat audit; source audit: family spawn rules와 차원·구조물 제약 | VERIFY 유지; 달성 가능, 등록 승격은 owner 승인 대기 |
| 30 | `xpq.cmb.habitats_5` | VERIFY → VERIFY | 1,100 | 다섯 개의 서로 다른 타입 기반 서식지에서 각각 1종 이상 처치 | 후보 분류는 몹의 등록 타입으로 고정; 좌표나 소환 위치로 분류하지 않음 | 검증 후보: desert `{desert_spider, sand_slime}`; swamp `{swamp_zombie, swamp_spider, mud_slime}`; snowy `{frozen_zombie, ice_spider}`; fungal `{fungal_zombie}`; wild slime `{wild_slime}`. 각 타입은 한 서식지에만 귀속 | 완전 소급은 승인된 타입 매핑을 통계로 조회할 때 가능 | 통계를 보유한 플레이어의 최종 타격 처치 | 바닐라 stats 파일; 현 카탈로그 tracker는 `pending` | 실제 사용자 바이옴 태그와 각 바이옴·스폰 배치 런타임 확인; 태그 구성원은 여러 개여도 퀘스트 서식지는 타입별 단일 매핑 | GameTest: 다섯 태그와 후보 타입 확인; tracker/classification 구현 전 별도 검증 필요 | VERIFY·미등록 유지; 후보 매핑은 owner 승인 대기 |
| 31 | `xpq.cmb.netherite_blades` | NOW* → NOW | 1,100 | 네더라이트 단검과 네더라이트 대검으로 각각 적대 몹 처치 | 처치 크레딧 + 주손 아이템을 신규 아이템 태그 `#moreminecrafts:daggers` / `#moreminecrafts:greatswords`(레지스트리 ID 7종씩) 로 판정 | 종류별 처치 카운터/집합 | 소급: 바닐라 ENTITY_KILLED는 무기 정보 없음 → 소급 없음 | 킬 크레딧 플레이어(투사체 아님 — 근접 직접 공격) | `quest.counter`/`quest.seen` | Q1-B + 데이터 태그 | GameTest: 검 처치 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 32 | `xpq.boss.monument_clear` | VERIFY → VERIFY | 1,450 | 해저 신전 1곳 공략 완료(엘더 가디언 전멸) 시 검증된 참여자 전원 — 엘더 가디언 피해 또는 신전 내 가디언 전투 증거 필요, 단순 체류 불인정 | 신전 ID=차원+구조물 시작 청크(기존 MonumentRewardData 키). 증거: (a) 엘더 가디언이 그 신전 경계 안에서 플레이어(투사체·소환물 원인 포함 `DamageSource.getEntity`)로부터 피해, 또는 (b) 플레이어가 그 신전 경계 안에서 가디언/엘더 가디언에게 피해를 주거나 받음. 단순 체류는 불인정 | 참여자 UUID → 마지막 증거 게임틱. 완료(마지막 엘더 사망, 기존 지연 완료 포함) 시 창=완료 전 12,000틱(10분) 이내 증거 또는 엘더 가디언 피해 이력이 있는 전원 인정 | 소급 없음(기존 완료 기록에 참여자 없음) | 검증된 참여자 전원(정책 4) | 대기 항목에 참여자 맵 저장(월드 SavedData), 오프라인 참여자는 보류 크레딧→로그인 시 반영, 신전당 1회 | T2CoreAcquisition/MonumentRewardData 확장(프로덕션) | GameTest: 2인 참여 모두 인정 / 체류만 무인정 / 청크 언로드 후 지연 완료 / 재시작 / 같은 신전 재완료 무집계 | VERIFY 유지 — 신전 코드 확장·검증 전 등록 금지 |
| 33 | `xpq.boss.dragon_rematch` | NOW* → NOW | 3,150 | 재소환된 엔더 드래곤 처치(본인 소환 여부 무관, 두 번째 이후 드래곤) | mixin `EnderDragonFight.setDragonKilled` HEAD에서 `hasPreviouslyKilledDragon()` 캡처(26.3 확인); true면 킬 크레딧 플레이어 인정 | 1회 | 소급 없음 | 킬 크레딧 플레이어 | complete | Q1-C | GameTest: 첫 드래곤 무인정, 두 번째 인정 | DESIGN SPECIFIED · TEST PENDING |
| 34 | `xpq.boss.omen_raid` | NOW* → NOW | 3,150 | 흉조 레벨 III 이상으로 시작된 습격 승리 | 습격 승리 시 `Raid.getRaidOmenLevel()≥3`, `isVictory()`, heroes 집합(26.3 확인)의 플레이어 | 1회 | 소급 없음 | 습격 영웅 집합 전원 | complete | Q1-C | GameTest: 레벨 2 무인정 | DESIGN SPECIFIED · TEST PENDING |
| 35 | `xpq.boss.monuments_3` | VERIFY → VERIFY | 3,450 | 서로 다른 해저 신전(차원+구조물 시작 청크) 3곳에서 참여자로 공략 완료 인정 | 신전 ID=차원+구조물 시작 청크(기존 MonumentRewardData 키). 증거: (a) 엘더 가디언이 그 신전 경계 안에서 플레이어(투사체·소환물 원인 포함 `DamageSource.getEntity`)로부터 피해, 또는 (b) 플레이어가 그 신전 경계 안에서 가디언/엘더 가디언에게 피해를 주거나 받음. 단순 체류는 불인정 | 참여자 UUID → 마지막 증거 게임틱. 완료(마지막 엘더 사망, 기존 지연 완료 포함) 시 창=완료 전 12,000틱(10분) 이내 증거 또는 엘더 가디언 피해 이력이 있는 전원 인정 | 소급 없음(기존 완료 기록에 참여자 없음) | 검증된 참여자 전원(정책 4) | 대기 항목에 참여자 맵 저장(월드 SavedData), 오프라인 참여자는 보류 크레딧→로그인 시 반영, 신전당 1회 | T2CoreAcquisition/MonumentRewardData 확장(프로덕션) | GameTest: 2인 참여 모두 인정 / 체류만 무인정 / 청크 언로드 후 지연 완료 / 재시작 / 같은 신전 재완료 무집계 | VERIFY 유지 — 신전 코드 확장·검증 전 등록 금지 |
| 36 | `xpq.min.obsidian_64` | NOW* → NOW | 300 | 흑요석 64개 채굴 | 바닐라 `Stats.BLOCK_MINED` 직접 합산(돌 계열 = `#minecraft:base_stone_overworld` + 조약돌·심층암 조약돌 목록 고정) | 통계 합 | 완전 소급(바닐라 통계는 영속 기록) | 통계 소유 플레이어 | 바닐라 stats 파일 | Q1-A 통계 리더 | Unit: 대상 목록 고정 / GameTest: 통계 반영 | DESIGN SPECIFIED · TEST PENDING · 설치·재채굴 반복 = LIVE BALANCE PENDING(정책 1은 광석만 적용) |
| 37 | `xpq.min.ore_1000` | NOW* → NOW | 500 | 자연 생성 광석 블록(플레이어 설치 광석 제외) 1,000개 채굴 | `#moreminecrafts:quest_tracked_ores` 블록 파괴 BEFORE에서 OreProvenance 양성 표시를 보관하고 성공한 AFTER에서만 +1 | `quest.counter.natural_ore_mined` | 소급 없음: 기존 `ore_items_mined`·BLOCK_MINED는 설치 광석을 구분하지 못함(정책 1·3) | 파괴한 서버 플레이어 | 카운터=플레이어 어태치먼트; 출처=청크 어태치먼트 | Q1-B OreProvenance | GameTest: 실크터치 설치 광석 재채굴 무집계 / 피스톤 이동 후 도착지 무표시 / 재시작 후 표시 유지 / 자연 광석 집계 | DESIGN SPECIFIED · TEST PENDING |
| 38 | `xpq.min.stone_10000` | NOW* → NOW | 500 | 돌 계열 블록 10,000개 채굴 | 바닐라 `Stats.BLOCK_MINED` 직접 합산(돌 계열 = `#minecraft:base_stone_overworld` + 조약돌·심층암 조약돌 목록 고정) | 통계 합 | 완전 소급(바닐라 통계는 영속 기록) | 통계 소유 플레이어 | 바닐라 stats 파일 | Q1-A 통계 리더 | Unit: 대상 목록 고정 / GameTest: 통계 반영 | DESIGN SPECIFIED · TEST PENDING · 설치·재채굴 반복 = LIVE BALANCE PENDING(정책 1은 광석만 적용) |
| 39 | `xpq.min.logs_1000` | NOW* → NOW | 500 | 자연 생성 원목 1,000개 채굴 | 기존 Knowledge 훅(`natural_vanilla_logs_mined` 기록 지점)에 `QuestCounters.add(player,"natural_vanilla_logs_mined",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.natural_vanilla_logs_mined` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 40 | `xpq.min.pickaxe_5000` | NOW* → NOW | 700 | 곡괭이 내구도 5,000 사용 | 기존 Knowledge 훅(`pickaxe_durability_used` 기록 지점)에 `QuestCounters.add(player,"pickaxe_durability_used",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.pickaxe_durability_used` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 41 | `xpq.min.vein_500` | VERIFY → VERIFY | 750 | Vein Mining 인챈트로 추가 파괴된 블록 500개 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): Vein Mining 추가 파괴 식별 지점 미확인 | VERIFY 유지 |
| 42 | `xpq.min.lumberjack_200` | VERIFY → VERIFY | 750 | Lumberjack 인챈트로 추가 파괴된 원목 200개 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): Lumberjack 추가 파괴 식별 지점 미확인 | VERIFY 유지 |
| 43 | `xpq.min.ore_5000` | NOW* → NOW | 1,100 | 자연 생성 광석 블록(플레이어 설치 광석 제외) 5,000개 채굴 | `#moreminecrafts:quest_tracked_ores` 블록 파괴 BEFORE에서 OreProvenance 양성 표시를 보관하고 성공한 AFTER에서만 +1 | `quest.counter.natural_ore_mined` | 소급 없음: 기존 `ore_items_mined`·BLOCK_MINED는 설치 광석을 구분하지 못함(정책 1·3) | 파괴한 서버 플레이어 | 카운터=플레이어 어태치먼트; 출처=청크 어태치먼트 | Q1-B OreProvenance | GameTest: 실크터치 설치 광석 재채굴 무집계 / 피스톤 이동 후 도착지 무표시 / 재시작 후 표시 유지 / 자연 광석 집계 | DESIGN SPECIFIED · TEST PENDING |
| 44 | `xpq.min.diamond_ore_64` | NOW* → NOW | 1,100 | 자연 생성 다이아몬드 광석(일반+심층암) 64개 채굴(설치 광석 제외) | 위와 동일, 대상 블록별 카운터 | `quest.counter.natural_mined.<block>`(다이아=일반+심층암 합산) | 소급 없음(동일 사유) | 파괴한 서버 플레이어 | 동일 | Q1-B OreProvenance | GameTest: 고대/아다만티움 잔해 설치·재채굴 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 45 | `xpq.min.adamantium_10` | NOW* → NOW | 1,250 | 자연 생성 아다만티움 잔해 10개 채굴(설치 잔해 제외) | 위와 동일, 대상 블록별 카운터 | `quest.counter.natural_mined.<block>`(다이아=일반+심층암 합산) | 소급 없음(동일 사유) | 파괴한 서버 플레이어 | 동일 | Q1-B OreProvenance | GameTest: 고대/아다만티움 잔해 설치·재채굴 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 46 | `xpq.min.debris_16` | NOW* → NOW | 1,100 | 자연 생성 고대 잔해 16개 채굴(설치 잔해 제외) | 위와 동일, 대상 블록별 카운터 | `quest.counter.natural_mined.<block>`(다이아=일반+심층암 합산) | 소급 없음(동일 사유) | 파괴한 서버 플레이어 | 동일 | Q1-B OreProvenance | GameTest: 고대/아다만티움 잔해 설치·재채굴 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 47 | `xpq.min.ore_20000` | NOW* → NOW | 3,150 | 자연 생성 광석 블록(플레이어 설치 광석 제외) 20,000개 채굴 | `#moreminecrafts:quest_tracked_ores` 블록 파괴 BEFORE에서 OreProvenance 양성 표시를 보관하고 성공한 AFTER에서만 +1 | `quest.counter.natural_ore_mined` | 소급 없음: 기존 `ore_items_mined`·BLOCK_MINED는 설치 광석을 구분하지 못함(정책 1·3) | 파괴한 서버 플레이어 | 카운터=플레이어 어태치먼트; 출처=청크 어태치먼트 | Q1-B OreProvenance | GameTest: 실크터치 설치 광석 재채굴 무집계 / 피스톤 이동 후 도착지 무표시 / 재시작 후 표시 유지 / 자연 광석 집계 | DESIGN SPECIFIED · TEST PENDING |
| 48 | `xpq.frm.tame_5` | NOW* → NOW | 350 | 늑대·고양이·앵무새·말·라마 5종 모두 길들이기(여우는 길들일 수 없어 제외) | TAME_ANIMAL 발생 지점(TamableAnimal.tame, AbstractHorse.tameWithName — 26.3 확인) mixin/advancement 트리거 리스너 | 고유 집합 {wolf, cat, parrot, horse, llama}(여우 제외: Fox extends Animal) | 소급 없음(종별 길들임 기록 부재) | 길들인 플레이어 | `quest.seen.frm.tame_5.<type>` | Q1-B | GameTest: 5종 각각 / 여우 신뢰 무집계 | DESIGN SPECIFIED · TEST PENDING |
| 49 | `xpq.frm.harvest_1000` | NOW* → NOW | 500 | 작물 1,000회 수확 | 기존 Knowledge 훅(`crops_harvested` 기록 지점)에 `QuestCounters.add(player,"crops_harvested",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.crops_harvested` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 50 | `xpq.frm.till_1000` | NOW* → NOW | 400 | 경작지 1,000칸 만들기 | 기존 Knowledge 훅(`farmland_tilled` 기록 지점)에 `QuestCounters.add(player,"farmland_tilled",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.farmland_tilled` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 51 | `xpq.frm.breed_200` | NOW* → NOW | 500 | 동물 200회 번식 | 바닐라 통계 직접 조회(BOAT/HORSE/AVIATE_ONE_CM ÷100, ANIMALS_BRED, TRADED_WITH_VILLAGER) | 통계 값 | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 통계 → 진행 일치, Knowledge 상한과 무관 | DESIGN SPECIFIED · TEST PENDING |
| 52 | `xpq.frm.villager_discount_20` | VERIFY → VERIFY | 750 | Villager Trading 연구 할인이 적용된 거래 20회 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 할인 적용 거래 식별 지점 미확인 | VERIFY 유지 |
| 53 | `xpq.frm.harvest_5000` | NOW* → NOW | 1,100 | 작물 5,000회 수확 | 기존 Knowledge 훅(`crops_harvested` 기록 지점)에 `QuestCounters.add(player,"crops_harvested",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.crops_harvested` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 54 | `xpq.frm.trades_200` | NOW* → NOW | 1,100 | 주민 거래 200회 | 바닐라 통계 직접 조회(BOAT/HORSE/AVIATE_ONE_CM ÷100, ANIMALS_BRED, TRADED_WITH_VILLAGER) | 통계 값 | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 통계 → 진행 일치, Knowledge 상한과 무관 | DESIGN SPECIFIED · TEST PENDING |
| 55 | `xpq.frm.barter_200` | NOW* → NOW | 1,100 | 피글린 물물교환 200회 | 기존 Knowledge 훅(`piglin_barters` 기록 지점)에 `QuestCounters.add(player,"piglin_barters",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.piglin_barters` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 56 | `xpq.frm.soil_study_harvest` | VERIFY → VERIFY | 1,100 | Soil Study 연구 효과가 적용된 수확 500회 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): Soil Study 적용 수확 식별 미확인 | VERIFY 유지 |
| 57 | `xpq.fsh.catch_500` | NOW* → NOW | 1,100 | 낚시 성공 500회 | 기존 Knowledge 훅(`fishing_successes` 기록 지점)에 `QuestCounters.add(player,"fishing_successes",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.fishing_successes` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 58 | `xpq.fsh.giant_catch` | VERIFY → VERIFY | 1,100 | 어종 최대 길이의 90% 이상인 물고기 낚기 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 종별 최대 길이 기준값 확정 필요 | VERIFY 유지 |
| 59 | `xpq.cft.anvil_50` | NOW* → NOW | 450 | 모루 수리 50회 | 기존 Knowledge 훅(`anvil_repairs` 기록 지점)에 `QuestCounters.add(player,"anvil_repairs",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.anvil_repairs` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 60 | `xpq.cft.redstone_100` | NOW* → NOW | 450 | 레드스톤 부품 100개 설치 | 기존 Knowledge 훅(`redstone_components_placed` 기록 지점)에 `QuestCounters.add(player,"redstone_components_placed",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.redstone_components_placed` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 61 | `xpq.cft.recipes_200` | NOW* → NOW | 750 | 서로 다른 레시피 ID 200종 제작(결과 수거 기준) | 제작 결과 슬롯 수거 시 RecipeHolder ID 기록(KnowledgeRecipeCraftCountMixin 지점에 병렬) | 고유 레시피 ID 200 | 소급 없음(바닐라는 아이템별 제작 수만 보관, 레시피 ID 이력 없음) | 제작 플레이어 | `quest.seen` (최대 200 저장 후 중단) | Q1-B | GameTest: 같은 레시피 반복 무증가 | DESIGN SPECIFIED · TEST PENDING |
| 62 | `xpq.bld.place_1000` | NOW* → NOW | 250 | 블록 1,000개 설치 | 바닐라 `Stats.ITEM_USED` 중 `BlockItem` 항목 합(26.3 `ItemStack.useOn` 성공 시 지급 — 실패·취소 설치는 미지급) | 통계 합(반복 설치 포함 — 정책 2) | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 성공 설치 +1, 설치 실패(점유 칸) 0, 반복 설치 누적 | DESIGN SPECIFIED · TEST PENDING · 200,000 완료 시간 = LIVE PLAYTEST PENDING |
| 63 | `xpq.bld.place_10000` | NOW* → NOW | 500 | 블록 10,000개 설치 | 바닐라 `Stats.ITEM_USED` 중 `BlockItem` 항목 합(26.3 `ItemStack.useOn` 성공 시 지급 — 실패·취소 설치는 미지급) | 통계 합(반복 설치 포함 — 정책 2) | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 성공 설치 +1, 설치 실패(점유 칸) 0, 반복 설치 누적 | DESIGN SPECIFIED · TEST PENDING · 200,000 완료 시간 = LIVE PLAYTEST PENDING |
| 64 | `xpq.bld.place_50000` | NOW* → NOW | 1,100 | 블록 50,000개 설치 | 바닐라 `Stats.ITEM_USED` 중 `BlockItem` 항목 합(26.3 `ItemStack.useOn` 성공 시 지급 — 실패·취소 설치는 미지급) | 통계 합(반복 설치 포함 — 정책 2) | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 성공 설치 +1, 설치 실패(점유 칸) 0, 반복 설치 누적 | DESIGN SPECIFIED · TEST PENDING · 200,000 완료 시간 = LIVE PLAYTEST PENDING |
| 65 | `xpq.bld.place_200000` | NOW* → NOW | 3,150 | 블록 200,000개 설치 | 바닐라 `Stats.ITEM_USED` 중 `BlockItem` 항목 합(26.3 `ItemStack.useOn` 성공 시 지급 — 실패·취소 설치는 미지급) | 통계 합(반복 설치 포함 — 정책 2) | 완전 소급 | 통계 소유 플레이어 | 바닐라 stats | Q1-A 통계 리더 | GameTest: 성공 설치 +1, 설치 실패(점유 칸) 0, 반복 설치 누적 | DESIGN SPECIFIED · TEST PENDING · 200,000 완료 시간 = LIVE PLAYTEST PENDING |
| 66 | `xpq.alc.ingredients_all` | NOW* → NOW | 450 | 양조 재료 10종(네더 사마귀·블레이즈 가루·레드스톤·발광석 가루·가스트 눈물·마그마 크림·거미 눈·복어·황금 당근·토끼 발) 획득 | Knowledge `brewing_ingredients_acquired` 관측 키 | 고정 10종(네더 사마귀, 블레이즈 가루, 레드스톤, 발광석 가루, 가스트 눈물, 마그마 크림, 거미 눈, 복어, 황금 당근, 토끼 발) | 완전 소급 | 관측 플레이어 | 기존 키 | 없음 | Unit | DESIGN SPECIFIED · TEST PENDING |
| 67 | `xpq.alc.shock_10` | VERIFY → VERIFY | 700 | Shock 효과를 몹 10마리에게 부여 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): Shock 부여 식별 지점 미확인 | VERIFY 유지 |
| 68 | `xpq.alc.potions_250` | NOW* → NOW | 1,100 | 물약 250개 양조 | 기존 Knowledge 훅(`potions_brewed` 기록 지점)에 `QuestCounters.add(player,"potions_brewed",n)` 병렬 호출 — Knowledge 의미 불변 | `quest.counter.potions_brewed` 무상한 누적(long 저장·int 포화) | 부분 소급: 최초 평가 시 counter=max(counter, `condition.{k}`) — Knowledge 값은 같은 이벤트의 상한 처리된 신뢰 하한. 상한 초과분 이력은 없으므로 만들지 않음 | 이벤트 수행 서버 플레이어 | 플레이어 어태치먼트(persistent, copyOnDeath); 목표 도달 시 complete 1회 | Q1-A 카운터, Q1-B 훅 | GameTest: Knowledge 상한 이후에도 증가 / 시드=기존값 / 재로그인 보존 | DESIGN SPECIFIED · TEST PENDING |
| 69 | `xpq.eqp.enhance_5` | NOW* → NOW | 250 | 강화 +5 이상 장비를 인벤토리·착용 슬롯에 보유(강화 수행자 무관) | EquipmentPossession 스캔: 플레이어 인벤토리 36칸 + 방어구 4 + 보조손(장착 포함). 중첩 컨테이너(셜커·번들·배낭)·엔더 상자·월드 컨테이너·커서/제작 격자 등 일시 슬롯은 제외. 100틱 주기 + 로그인 + 강화 성공 직후 | 달성 시 complete(이후 아이템 이동과 무관 — 단조) | 현재 보유분만 소급(과거 보유는 기록 없음) | 보유 플레이어(누가 강화했는지 무관 — 정책 5). 양도 시 양쪽 모두 달성 가능 | complete 플래그 | Q1-D | GameTest: 인벤토리/착용 각각 인정, 셜커 내부 불인정, 커서 불인정 | DESIGN SPECIFIED · TEST PENDING |
| 70 | `xpq.eqp.enhance_10` | NOW* → NOW | 500 | 강화 +10 이상 장비 보유 | EquipmentPossession 스캔: 플레이어 인벤토리 36칸 + 방어구 4 + 보조손(장착 포함). 중첩 컨테이너(셜커·번들·배낭)·엔더 상자·월드 컨테이너·커서/제작 격자 등 일시 슬롯은 제외. 100틱 주기 + 로그인 + 강화 성공 직후 | 달성 시 complete(이후 아이템 이동과 무관 — 단조) | 현재 보유분만 소급(과거 보유는 기록 없음) | 보유 플레이어(누가 강화했는지 무관 — 정책 5). 양도 시 양쪽 모두 달성 가능 | complete 플래그 | Q1-D | GameTest: 인벤토리/착용 각각 인정, 셜커 내부 불인정, 커서 불인정 | DESIGN SPECIFIED · TEST PENDING |
| 71 | `xpq.eqp.enhance_trio` | NOW* → NOW | 850 | 무기·도구·방어구 각 +10 이상을 동시에 보유 | 동일 스캔, 무기·도구·방어구 분류는 EnhancementCatalog 카테고리 사용 | 세 분류 각각 +10 이상 — **동시 보유** | 현재 보유분 | 보유 플레이어 | complete | Q1-D | GameTest | DESIGN SPECIFIED · TEST PENDING |
| 72 | `xpq.eqp.enhance_15` | NOW* → NOW | 750 | 강화 +15 이상 장비 보유 | EquipmentPossession 스캔: 플레이어 인벤토리 36칸 + 방어구 4 + 보조손(장착 포함). 중첩 컨테이너(셜커·번들·배낭)·엔더 상자·월드 컨테이너·커서/제작 격자 등 일시 슬롯은 제외. 100틱 주기 + 로그인 + 강화 성공 직후 | 달성 시 complete(이후 아이템 이동과 무관 — 단조) | 현재 보유분만 소급(과거 보유는 기록 없음) | 보유 플레이어(누가 강화했는지 무관 — 정책 5). 양도 시 양쪽 모두 달성 가능 | complete 플래그 | Q1-D | GameTest: 인벤토리/착용 각각 인정, 셜커 내부 불인정, 커서 불인정 | DESIGN SPECIFIED · TEST PENDING |
| 73 | `xpq.eqp.custom_enchant_5` | VERIFY → VERIFY | 1,100 | 서로 다른 MM 인챈트 5종 보유 이력 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 11종 인챈트의 생존 획득 실측 필요 | VERIFY 유지 |
| 74 | `xpq.eqp.enhance_20` | NOW* → NOW | 1,250 | 강화 +20 장비 보유 | EquipmentPossession 스캔: 플레이어 인벤토리 36칸 + 방어구 4 + 보조손(장착 포함). 중첩 컨테이너(셜커·번들·배낭)·엔더 상자·월드 컨테이너·커서/제작 격자 등 일시 슬롯은 제외. 100틱 주기 + 로그인 + 강화 성공 직후 | 달성 시 complete(이후 아이템 이동과 무관 — 단조) | 현재 보유분만 소급(과거 보유는 기록 없음) | 보유 플레이어(누가 강화했는지 무관 — 정책 5). 양도 시 양쪽 모두 달성 가능 | complete 플래그 | Q1-D | GameTest: 인벤토리/착용 각각 인정, 셜커 내부 불인정, 커서 불인정 | DESIGN SPECIFIED · TEST PENDING |
| 75 | `xpq.eqp.enhance_20_armor` | NOW* → NOW | 3,750 | 방어구 4부위 각 +20을 동시에 보유(착용 또는 인벤토리) | 동일 스캔 | 투구·흉갑·각반·부츠 각 +20을 **동시에** 보유(착용 또는 인벤토리) | 현재 보유분 | 보유 플레이어 | complete | Q1-D | GameTest | DESIGN SPECIFIED · TEST PENDING |
| 76 | `xpq.eqp.custom_enchant_all` | VERIFY → VERIFY | 3,150 | MM 인챈트 11종 전부 보유 이력 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 동일 | VERIFY 유지 |
| 77 | `xpq.grw.climbing_100` | VERIFY → VERIFY | 450 | Climbing 연구 효과로 벽 100블록 등반 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 연구 효과 런타임(PARTIAL) 검증 필요 | VERIFY 유지 |
| 78 | `xpq.grw.spent_10k` | NOW* → NOW | 750 | 보유 연구의 승인 원장 원시 XP 합계(보유 연구 가치) 10,000 이상 | 상태 질의: Σ(보유 연구 각 레벨 1..L의 승인 원장 rawXp) — ResearchCatalog 정의 기준(루트 이관분 포함, 정책 6) | 현재 보유 가치(레벨 하락 없음 전제; 감소 시에도 complete는 유지) | 완전 소급 | 보유 플레이어 | 파생값(저장 불필요) | Q1-D | Unit: 원장 합계 / 이관 루트 포함 | DESIGN SPECIFIED · TEST PENDING · 명칭을 "보유 연구 가치"로 정정 |
| 79 | `xpq.grw.wall_jump_50` | VERIFY → VERIFY | 700 | Wall Jump 50회 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 동일 | VERIFY 유지 |
| 80 | `xpq.grw.fall_roll_20` | VERIFY → VERIFY | 700 | Fall Roll로 낙하 피해 경감 20회 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 동일 | VERIFY 유지 |
| 81 | `xpq.grw.throwable_pickaxe` | VERIFY → VERIFY | 700 | Throwable Pickaxe로 원거리 블록 100개 파괴 | 해당 메커니즘의 서버 이벤트 지점 확인 후 정의 | — | 설치 이후 | 행위 플레이어 | `quest.counter` | 메커니즘 검증 | GameTest(메커니즘): 동일 | VERIFY 유지 |
| 82 | `xpq.grw.spent_50k` | NOW* → NOW | 1,250 | 보유 연구 가치 50,000 이상 | 상태 질의: Σ(보유 연구 각 레벨 1..L의 승인 원장 rawXp) — ResearchCatalog 정의 기준(루트 이관분 포함, 정책 6) | 현재 보유 가치(레벨 하락 없음 전제; 감소 시에도 complete는 유지) | 완전 소급 | 보유 플레이어 | 파생값(저장 불필요) | Q1-D | Unit: 원장 합계 / 이관 루트 포함 | DESIGN SPECIFIED · TEST PENDING · 명칭을 "보유 연구 가치"로 정정 |

## 5. Remaining genuine blockers (do not block Q1-A)

| Item | Affects | Blocked on |
|---|---|---|
| 26.3 naturally generated biome list | exp.biomes_all_overworld | Extract & fix list (data task) |
| Registered/naturally spawning MM hostile types | cmb.bestiary_25, cmb.habitats_5, cmb.cave_dwellers | Ecology audit completed; VERIFY registration/mapping approval remains |
| Monument participant tracking | boss.monument_clear, boss.monuments_3 | T2CoreAcquisition/MonumentRewardData extension + GameTest |
| Research effect runtime (PARTIAL nodes) | grw.climbing_100, wall_jump_50, fall_roll_20, throwable_pickaxe | Effect implementation acceptance |
| Enchant/research effect event points | min.vein_500, min.lumberjack_200, frm.villager_discount_20, frm.soil_study_harvest, alc.shock_10, eqp.custom_enchant_5/_all | Identify server hook; survival obtainability test |
| MM item mechanics | exp.sleeping_bag_biomes, exp.parachute_landing, fsh.giant_catch | Mechanic verification / reference length |
| Survival acquisition routes | 20 FUTURE quests | Content work (not XP Quest scope) |
| Selector enabled-card art | XP Quest entry visual | Asset decision (D-30); does not block Q1-A–C |

## 6. Audit finding disposition (34)

Categories: **A** owner decision · **B** technical specification · **C** implementation · **D** automated verification · **E** live/balance · **F** blocked by missing functionality/data.

| ID | Sev | Category | Status | Disposition |
|---|---|---|---|---|
| D-01 | P0 | B, C, D | DESIGN SPECIFIED · TEST PENDING | Uncapped `quest.counter.*` + seeding (§3.2) |
| D-02 | P1 | B, C, D | DESIGN SPECIFIED · TEST PENDING | Direct vanilla stat reads (§3.3) |
| D-03 | P1 | A, B, C, D | DESIGN SPECIFIED · TEST PENDING | Policy 1 + ore provenance (§3.4); stone/obsidian repetition = E |
| D-04 | P1 | A | DESIGN SPECIFIED · LIVE PENDING (balance) | Policy 2: repeats count; place_200000 kept at 3,150 |
| D-05 | P1 | B, C, D | DESIGN SPECIFIED · TEST PENDING | `awardStat` per-dimension hook (§3.6) |
| D-06 | P1 | D, F | BLOCKED (list) | Quest held in VERIFY until list fixed |
| D-07 | P1 | B | DESIGN SPECIFIED · TEST PENDING | Fox removed; 5 tameable species |
| D-08 | P1 | A, B, C, D | DESIGN SPECIFIED · TEST PENDING | Policy 4 participation model (§3.7); quests held in VERIFY |
| D-09 | P1 | B, C, D | DESIGN SPECIFIED · TEST PENDING | `LootTableTrigger` hook; opener attribution |
| D-10 | P2 | A | DESIGN SPECIFIED | Policy 6 owned value |
| D-11 | P1 | B, C, D | DESIGN SPECIFIED · TEST PENDING | Distinct recipe IDs, install-forward |
| D-12 | P2 | F | BLOCKED (IDs) | VERIFY |
| D-13 | P2 | B, D | DESIGN SPECIFIED · TEST PENDING | Keys: dim+pos / start chunk / UUID |
| D-14 | P2 | B, D | DESIGN SPECIFIED · TEST PENDING | Capture flag before setDragonKilled |
| D-15 | P1 | F | ECOLOGY AUDITED · VERIFY | bestiary_25 has 37 reachable Monster candidates; habitats_5 candidates and tags audited; owner mapping/promotion approval still required |
| D-16 | P2 | B | DESIGN SPECIFIED | 10 ingredients listed |
| D-17 | P2 | A | DESIGN SPECIFIED · TEST PENDING | Policy 5 possession scan (§3.8) |
| D-18 | P2 | B, C | DESIGN SPECIFIED · TEST PENDING | Item tags for daggers/greatswords |
| D-19 | P2 | B, D | DESIGN SPECIFIED · TEST PENDING | `Raid.getRaidOmenLevel()` (CODE VERIFIED API) |
| D-20 | P1 | B, D | DESIGN SPECIFIED · TEST PENDING | Toast on delta only |
| D-21 | P1 | B, D | DESIGN SPECIFIED · TEST PENDING | Pending timeout/reopen/reconnect |
| D-22 | P2 | E | LIVE PENDING | Mouse-only claim accepted for now |
| D-23 | P1 | D | TEST PENDING | Grant rollback specified (§3.11); crash-save test required |
| D-24 | P1 | C, D | DESIGN SPECIFIED · TEST PENDING | Enhancement transaction rollback (authority) — Q1-A2 |
| D-25 | P2 | B | FIXED (doc) | B §7 table repaired |
| D-26 | P2 | B | FIXED (doc) | B recommendation marked superseded |
| D-27 | P2 | B | FIXED (doc) | D §2 marked superseded |
| D-28 | P2 | B | FIXED (doc) | C note strengthened |
| D-29 | P2 | B | OWNER APPROVAL (authority) | Inline superseded marks proposed — §8 |
| D-30 | P1 | E, F | BLOCKED (art) — UI entry only | Does not block Q1-A–C |
| D-31 | P2 | B | FIXED (doc) | FUTURE t3_* use LOCKED T3 7 |
| D-32 | P1 | A | DESIGN SPECIFIED · TEST PENDING | Policy 3: full retro from reliable evidence at first login |
| D-33 | P2 | E | LIVE PENDING | Mid-game bottleneck — playtest |
| D-34 | P2 | — | OUT OF SCOPE | laser_arrow tree node without enchantment JSON — report only |

## 7. Test requirements

- **Unit:** QuestRules capping/claim/non-positive; catalog invariants (200 IDs, ×50, 180,000 / 29,100 / 209,100, VERIFY+FUTURE unregistered); exact XP total↔(level, points) round-trip 0–1,000,000; research-value sum; fixed lists (biomes, ingredients, tame species, crop set, loot tables).
- **GameTest — XP transactions:** grant/deduct at 0, exact balance, 1 short, 15/16, 30/31, ±100,000 (assert `RawXp.available` and `totalExperience`); duplicate/replayed/random-ID claims; exception during grant → full rollback; enhancement rollback after XP deduction, material removal and item modification (authority); insufficient XP/materials → no mutation; same enhancement cost at any level; no Research multiplier / Mending on quest XP.
- **GameTest — tracking:** each hook in §3.6 fires once; post-completion re-trigger no-op; capped-counter seeding; placed-ore exclusion incl. piston move and restart; successful vs failed placement; loot block vs minecart vs re-open vs other player; vault per-player per-vault; monument multi-participant, presence-only rejection, deferred completion, offline credit, restart; possession scan inclusions/exclusions; retroactive login evaluation on a pre-populated world.
- **Integration (client):** synthetic tree build, three states, toast once (delta, not snapshot), pending lifecycle, no `ServerboundSeenAdvancementsPacket`/telemetry from the quest screen.
- **Durability — keep these two separate:**
  - *In-process crash simulation* (implemented in #115): save the player record before and after a claim, then reload each into a fresh player. This proves the claim flag and the XP are stored in the same record, so they are lost or kept together.
  - *Actual JVM-kill durability* (**NOT RUN, TEST PENDING**): `kill -9` the server between a claim and the next autosave, restart on the same world, and assert there is no duplicate and no loss. It needs a two-process harness like the K5 Production Restart workflow. In-process results must never be reported as JVM-kill results.
- **Live (LIVE PENDING):** 12 tabs, badges, click vs drag, Korean tooltips, scroll retention, balance incl. place_200000 duration and mid-game bottleneck.

## 8. Authority changes (approved and applied 2026-10-17, commit `6417f95`)

- UI amendment says "Until then the registered set is the 161 NOW quests." → proposed: "160 NOW quests, each registered only after its automated test passes."
- Record the six owner-locked policies (§1) in the XP Quest amendment (required by AGENTS.md design-finalization rule once approved for authority).
- Inline "superseded by 2026-10-17 amendment" marks on CONTENT_AUTHORITY_INDEX line 1 and UI doc lines 18/29/39 (D-29).
- Add the grant-failure rollback rule (§3.11) to the claim rules.

## 9. Implementation batches

Dependency analysis supports the requested order with one change: the exact-XP/claim hardening and the enhancement cost conversion move into an independent **Q1-A2**, because every later batch's claims depend on them and they touch different files.

| Batch | Scope | Prerequisites | Tests | Merge gate |
|---|---|---|---|---|
| Q1-A | Catalog (200 IDs, ledger validation, registration filter), `quest.counter`/`quest.seen`, QuestStats reader, seeding, login reconciliation | Owner approval of Q1; §8 decisions | Unit catalog/ledger/rules; GameTest seeding + retro login | CI green; ledger 180,000/29,100/209,100 asserted in test |
| Q1-A2 | `RawXp` exact grant/deduct with `totalExperience`; claim rollback + `UNAVAILABLE`; Enhancement raw-XP table + transactional rollback | Q1 approval | All XP-transaction GameTests | CI green; rollback tests pass |
| Q1-B | Placement, ore provenance, mining, farming, crafting (recipe IDs), fishing, brewing, taming, per-dimension travel hooks | Q1-A | Tracking GameTests for these hooks | CI green; Knowledge tests unchanged |
| Q1-C | Loot trigger, vaults, unique structures, kill attribution, dragon rematch, omen raid; monument participation (VERIFY stays unregistered until its test passes) | Q1-A, Q1-B | Attribution/structure GameTests | CI green |
| Q1-D | Equipment possession, research value; synthetic Advancements UI, click-to-claim, toast/pending, sync | Q1-A2, Q1-C | Integration client tests | CI green; UI LIVE PENDING recorded |
| Q1-E | Full regression, existing-world migration, multiplayer, live validation; VERIFY promotions one by one | All | Full suite + live checklist | Owner live acceptance |

## 10. Verdict

**READY FOR IMPLEMENTATION APPROVAL** for Q1-A and Q1-A2, conditional on the §8 authority items being approved. No P0 remains unresolved at design level; every NOW quest has a specified server detection mechanism; remaining blockers affect only VERIFY/FUTURE quests and UI art. Nothing here is implemented or runtime-verified.

### Owner decision (2026-10-18): `xpq.cmb.wither_skulls_3` acquisition semantics

- **Approved event:** server-authoritative player pickup of an `minecraft:wither_skeleton_skull` item entity into the player's inventory. Count the number of items **actually transferred** by a successful pickup, not item entities, kill events, drops, crafting, trading, inventory scans, or merely approaching the item.
- **Progress:** cumulative successful pickup quantity across events (target **3**). Re-picking up the same physical skull after dropping it **does count again**: the owner selected **item pickup**, not unique skull acquisition. This is deliberate and should be covered by a regression test; no deduplication or origin tracking is authorized.
- **Attribution:** the player who actually receives the picked-up items. A failed pickup, full inventory, cancelled interaction, or pickup with zero transferred items grants no progress. Split/partial stacks count only the transferred quantity. Other players receive no credit.
- **History:** only the vanilla `minecraft:nether/find_fortress`-independent advancement `get_wither_skull` (where present and provable) may seed the existing historical lower bound of 1; do not infer quantity 3 from a single advancement or current inventory. Never double-count a seeded pickup; document the exact seed/first-live-event boundary.
- **Scope:** implementation and GameTests are deferred to the next authorized quest implementation batch. Until passing tests, keep catalog `registered=false`, `tracker.kind=pending`, `status=NOW`, `target=3`, and `rewardRawXp=1100`. Do not alter the 200-quest reward ledger.

### Q1-C ecology audit (2026-10-18)

The runtime ecology GameTest inspects the actual 26.3 entity registry and confirms exactly 37 `moreminecrafts:` entries have `MobCategory.MONSTER`. For each entry it verifies the reviewed `CustomMobCatalog.initialize()` biome-tag mapping resolves to at least one registered biome, a `SpawnPlacements` placement is installed, and `Stats.ENTITY_KILLED` is available. `CustomMobCatalog.initialize()` adds each entry through `BiomeModifications.addSpawn`; the test exercises the resolved biome tags and spawn-placement registrations. The audit prints the resolved biome ids for each species. The other three registered custom mobs are `feral_wolf` and `giant_bee` (`CREATURE`) and `cave_bat` (`AMBIENT`), so the bestiary's Monster-category tracker excludes them by definition; `cave_bat` remains in the exact cave quest roster. No Monster-category type needs exclusion for a 25-type target, so the evidence supports **25+ naturally reachable kill opportunities** and `bestiary_25` is attainable.

Natural spawn restrictions found in source are all compatible with ordinary survival: GROUND requires air above solid floor; CAVE requires below sea level, no sky access, and solid floor; NETHER requires air above solid floor; WATER requires water; SKY requires exposed sky above sea level + 12. Family predicates use the corresponding vanilla rules (Monster/slime, Drowned, Magma Cube, Piglin, Silverfish, Endermite, or Stray). The structure-associated spawners are miner_zombie/miner_skeleton inside a Mineshaft, lesser_wither_skeleton inside a Nether Fortress, desert_bug in the generated underground portion of a Desert Pyramid, and drowned_sailor inside a Shipwreck. All five structures generate in ordinary survival. No candidates were excluded; GameTest does not simulate a long natural-spawn session or balance rates.

| Habitat candidate | Registered type(s), source biome tag and resolved biome ids | Spawn condition |
|---|---|---|
| desert | `desert_spider`, `sand_slime` — `moreminecrafts:desert_habitat` — `minecraft:desert` | GROUND / vanilla Monster rules |
| swamp | `swamp_zombie`, `swamp_spider`, `mud_slime` — `moreminecrafts:swamp_habitat` — `minecraft:swamp`, `minecraft:mangrove_swamp` | GROUND / vanilla Monster rules |
| snowy | `frozen_zombie`, `ice_spider` — `moreminecrafts:snowy_habitat` — snowy_plains, ice_spikes, snowy_taiga, grove, snowy_slopes, frozen_peaks, jagged_peaks, frozen_river | GROUND / vanilla Monster rules |
| fungal | `fungal_zombie` — `moreminecrafts:fungal_habitat` — lush_caves, mangrove_swamp, mushroom_fields, swamp | CAVE / vanilla Monster rules |
| wild slime | `wild_slime` — `moreminecrafts:wild_slime_habitat` — forest-tag biomes plus swamp and mangrove_swamp | GROUND / vanilla slime rules |

Habitat group assignment is by registered entity type, not kill coordinates. Each candidate belongs to one proposed quest habitat, even where its spawn biome tag is broader (for example, wild_slime has forest and swamp spawn biomes). This avoids assigning one summoned or moved entity to multiple habitat buckets. These remain review candidates: `xpq.cmb.habitats_5` stays `VERIFY`, unregistered, with a pending tracker until the owner approves the mapping and implementation.

`xpq.cmb.cave_dwellers` now uses the existing exact-type `stat_killed_each` evaluator and the owner-approved six IDs. GameTests cover each kill, AMBIENT cave_bat inclusion without changing other hostile filters, duplicate kills, another player, an unlisted type, retroactive vanilla statistics, and persistence across rejoin/restart. The tracker has no new `AFTER_DEATH` implementation and production registration remains gated by `registered=false`/`VERIFY`.

These findings do not authorize any catalog status or reward change. The 72 registered quests, all 200 quest ids, total 209,100 XP, active 180,000 XP, both unregistered monument VERIFY quests, and the pending item-pickup basis for wither_skulls_3 remain covered by catalog data tests.


### Owner approval (2026-10-18): ecology registration, habitat mapping, and in-game checklist UI

The owner explicitly approved promotion of `xpq.cmb.cave_dwellers` and the five habitat groups below, **with the additional mandatory requirement that players can view the complete target roster and their own completion state inside the quest UI**. This is not merely a tooltip showing "3/6" or "2/5".

**Approved quest semantics**
- `xpq.cmb.cave_dwellers`: exact six species `cave_zombie`, `cave_skeleton`, `cave_creeper`, `small_spider`, `swamp_spider`, `cave_bat`. The AMBIENT cave bat counts only here. Completion means at least one credited kill of **each** species, including retroactive `ENTITY_KILLED` statistics. No repeat credit per species.
- `xpq.cmb.bestiary_25`: distinct registered MoreMinecrafts MONSTER-category species killed; goal 25 of the 37 audited candidates. The UI must show the eligible species roster, which ones have been killed, and a 25-species completion threshold. The three non-MONSTER custom types remain excluded. Existing `stat_killed_monsters_distinct` is reusable; owner approval covers implementation and registration only after UI+tests pass.
- `xpq.cmb.habitats_5`: five independent groups; a group completes once the player has killed **any one** of its eligible species. Approved groups:
  - Desert: `desert_spider`, `sand_slime`.
  - Swamp: `swamp_zombie`, `swamp_spider`, `mud_slime`.
  - Snowy: `frozen_zombie`, `ice_spider`.
  - Fungal: `fungal_zombie`.
  - Wild slime: `wild_slime`.
  Group identity is based on the entity type, **not** the kill location or biome. One entity type must not satisfy multiple groups; duplicates do not add progress. Use durable player `ENTITY_KILLED` statistics where possible and retain retroactivity. Target 5 groups.

**Mandatory quest UI acceptance criteria**
1. Selecting any of the three quests opens a readable **target checklist** in the quest detail view (scrollable if necessary), showing all required species or all eligible species per group; localized display names with the stable registry ID available as fallback.
2. Each required species in `cave_dwellers` shows **done/not done** independently; total `n/6`. An uncredited cave bat kill must not appear done.
3. `habitats_5` shows five named groups, each with its eligible mob list and **done/not done**; total `n/5`. Within each group, indicate which eligible species the player has actually killed, rather than hiding the other choices.
4. `bestiary_25` shows the full eligible MONSTER species roster with per-species discovered/undiscovered completion, count `n/25` and an unambiguous threshold (more than 25 possible). The UI must not falsely imply that all 37 are mandatory.
5. Server owns progress and syncs checklist states to the client; no client-side kill inference or reward grant. Persisted and retroactive evidence should render correctly on first opening and after reconnect. No private per-player data leakage to another player.
6. Keyboard/controller navigation, scroll clipping, small-screen legibility, and non-overlapping buttons/text must be tested. The XP Quest selector card is currently disabled in the four-card progression menu: **verify the actual navigation path**; do not claim a functioning in-game checklist merely because data or localization exists.
7. GameTests/unit tests for tracker math, category exception, disjoint group mapping, persistence and multiplayer attribution; client smoke or real UI screenshots for the actual checklist. Label visual status `VISUAL PENDING` when not directly observed.

**Implementation gate:** The owner authorized the *direction*, not an untested immediate release. Until the checklist UI and tracker tests pass, keep existing `VERIFY`/unregistered flags where applicable. Do not change the 200-quest ledger or rewards, and do not merge PR #121 without separate approval. The checklist requirement applies to other future multi-target XP quests as a reusable UI component, not just these three.
