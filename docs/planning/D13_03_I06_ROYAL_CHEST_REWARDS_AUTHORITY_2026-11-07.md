# D13-03-I06 — ST-01 Royal Chest Reward Authority (Q01–Q45)

Status: **OWNER-APPROVED DESIGN ONLY; IMPLEMENTATION NOT VERIFIED** (2026-11-07). This record synchronizes the approved I06 conversation decisions. It does not assert that a royal chest, structure NBT, worldgen hookup, or runtime persistence has been implemented or validated.

## Scope and precedence

Applies **only** to two royal reward chests in ST-01 Colossal Pyramid: `PRIMARY` and `SECONDARY`. The separate large treasury chest in the secret room, Tomb Guardian's own drops, and other D13 structures are excluded. Preserve the existing ST-01 exterior, chest appearance, physical placement, and D13 gameplay decisions. ST-01 P4 **B** is the owner-selected spatial candidate; do not infer an east/west orientation or runtime acceptance from that selection. The spatial model is tracked independently at `work/st01-p4-secret-area-20261107@0d486fd131638bc08de20b5be8f2846bab3c9177`; later integration must reconcile the current ST-01 P5 authority before placing blocks.

This document controls **I06-specific royal chest rewards, access, and persistence** where older D13-03 Q25–Q47 or PR #237's provisional evaluation semantics conflict. In particular, automated access (including hoppers) must **never** trigger loot generation. It does not supersede unrelated D13 mechanics, item recipes, accessory effects, or structure architecture. Existing code is not design authority.

## Royal chest behavior

- Exactly two role-designated royal chests, each with **54 inventory slots**, using the already approved physical appearance and placement; do not infer a vanilla double-chest visual.
- Both chests remain sealed until **normal defeat of Tomb Guardian**. Preserve existing PRIMARY interaction encounter trigger and interrupted-fight retry semantics. Boss absence, unload, or interruption is not victory; a valid defeat is persisted and prevents refarming.
- Each chest generates its own base rewards and T2 accessory result **once, on its first valid direct player opening after defeat**. Merely defeating the boss, generating the world, opening the other chest, or accessing via automation must not generate it.
- One shared inventory per chest for all players; item withdrawal uses normal chest semantics, with ordinary slot synchronization and item serialization reused where practical. Players may **withdraw only**, never insert items, including shift-click, drag, hotbar swap, or other insertion paths.
- Block automated insertion and extraction (hopper, hopper minecart, and equivalent paths) permanently, before and after defeat. Block player mining and explosion destruction permanently, even after the rewards are depleted.
- Loot is permanently depleted: no restock, reroll, respawn-based refarm, or personal inventory copies.
- Authority key: stable `structureInstanceId` plus role `PRIMARY` or `SECONDARY`. Coordinates may locate a chest but are not the sole identity. Repair, NBT replacement, chunk reload, and restart of the **same instance** preserve results and remaining items; a genuinely new structure instance receives a new draw.
- First-generation operations must be idempotent and crash-recoverable without duplicate or lost rewards. A simple claim-before-roll flag or `SavedData.setDirty()` alone does not establish atomicity. Preserve generated results (including failed rolls) and their application state as needed; reuse normal inventory persistence for remaining slots, with a recovery contract for save-boundary failures.
- Do not design a separate general-purpose chest inventory engine or new concurrency rules; inherit ordinary Minecraft shared-inventory behavior where compatible.

## PRIMARY — guaranteed and independent ordinary minerals

Each quantity is an **inclusive discrete uniform integer range**, conditional on that item's successful independent probability check.

| Item ID | Chance | Quantity |
|---|---:|---:|
| `minecraft:gold_ingot` | 100% | 64–128 |
| `minecraft:diamond` | 85% | 10–18 |
| `minecraft:emerald` | 75% | 12–24 |
| `moreminecrafts:mythril` | 25% | 2–3 |

### PRIMARY — rare pool (one success gate)

Exactly **one** rare-pool success gate with **10% total chance**, on PRIMARY only. If it succeeds, choose exactly **one item type** using these conditional weights; if it fails, award none.

| Item ID | Conditional chance given gate success | Unconditional chance | Quantity |
|---|---:|---:|---:|
| `moreminecrafts:adamantium_ingot` | 20% | 2% | 1 |
| `minecraft:netherite_ingot` | 10% | 1% | 1 |
| `minecraft:netherite_scrap` | 35% | 3.5% | 1–2 |
| `minecraft:ancient_debris` | 35% | 3.5% | 1–2 |

Rare-pool failure is 90%. None of these four items also appears in the PRIMARY ordinary mineral pool or the SECONDARY pool. Adamantium is classified T3; netherite ingot/scrap/ancient debris may be classified T2 materials, but that classification **does not** move them out of this PRIMARY-only 10% pool.

## SECONDARY — materials

Each probability check is independent; quantities are inclusive discrete uniform integer ranges on success.

| Item ID | Chance | Quantity |
|---|---:|---:|
| `moreminecrafts:enhancement_stone` | 100% | 64–96 |
| `minecraft:redstone` | 100% | 64–128 |
| `minecraft:lapis_lazuli` | 100% | 64–128 |
| `minecraft:diamond` | 100% | 10–18 |
| `moreminecrafts:titanium_ingot` | 75% | 32–64 |
| `moreminecrafts:tungsten_ingot` | 75% | 32–64 |

**Q33 correction:** titanium and tungsten are **32–64**, not the superseded 4–8. **Q45 B:** give the **ingots**, never `raw_titanium` or `raw_tungsten`. These are ordinary metal-tier materials, not promoted to the PRIMARY rare pool.

## Shared special-reward rules

**T2 accessories:** Each of the two chests independently has a **20%** chance of exactly one T2 accessory. On success choose `moreminecrafts:seed_of_life` or `moreminecrafts:ring_of_time` with **50:50** conditional probability. Duplicates across chests are allowed. Joint probability: neither 64%; exactly one chest succeeds 32%; both succeed 4% (same type 2%; distinct types 2%). Do not tie either chest's result to the other.

**Relic Catalyst:** Exactly one **structure-level** 5% success roll per pyramid instance, with at most **one** `moreminecrafts:relic_catalyst`. On success preassign it to PRIMARY or SECONDARY **50:50**, persist that assignment, and deliver it only through the assigned chest when that chest is validly first opened. Marginal assignment probability per chest is 2.5%. Opening order must not change the outcome. This is independent of ordinary minerals, rare pool, and accessories; it does not replace them.

No common monster drops, completed Etherium ingots, elemental cores, unrelated accessories, or newly invented ST-01-exclusive items are added to these pools. Q32 audit of other registered crafting materials remains **DEFERRED**, not permanently rejected.

## Implementation boundaries and verification

- Reuse existing `TombGuardianEncounters` / `TombEncounterData` defeat authority, rather than inferring victory from boss absence or inventing a second encounter state machine.
- Reconcile tomb-anchor-based encounter identity with the stable instance-and-role reward identity; do not assume coordinate-only or PR #237's chest ID resolver is already connected.
- Existing PR #237 `PyramidRoyalChestRewards` 20% accessory pool is a **reuse candidate**, not proof of live chest hookup or crash-safe reward persistence. Existing catalyst sources for Ancient City/Bastion do not automatically implement ST-01.
- Ensure PRIMARY and SECONDARY are both independently sealed until defeat; preserve the third treasury chest's separate contract.
- Verify stack splitting for quantities over 64 and actual registered stack sizes against each 54-slot inventory; do not silently truncate rewards.
- Test probability/quantity boundaries, structure-wide catalyst exclusivity, independence, concurrent opening, bypasses of insertion and automation, mining/explosion protection, permanent depletion, chunk reload/restart, structure repair, and crash/recovery boundaries.
- The pyramid's actual worldgen rarity, the physical ST-01 connection, exact east/west spatial orientation, and runtime/gameplay validation remain **UNVERIFIED**. A model or static test PASS is not a live gameplay PASS.

## Decision history and traceability

- I06 Q04, Q07–Q14, Q34–Q40, Q43: seal, independent first-open generation, world-shared 54-slot withdraw-only chests, stable identities, persistence, non-breakable and no automation; Q37 supersedes Q07's earlier hopper-first-access possibility. Q40 adds no custom concurrency rule.
- I06 Q15–Q28: PRIMARY ordinary/rare pool, weights, quantities, exclusions.
- I06 Q29–Q33: SECONDARY pool; corrected Q33 metal quantities are 32–64.
- I06 Q06, Q41–Q42: catalyst and independent accessories; no mob drops.
- I06 Q32 and Q44 A: do not add further materials now; candidate audit deferred.
- I06 Q45 B: titanium and tungsten **ingots**.
- Spatial selection: ST-01 P4 **B**, with orientation and final integration authority tracked separately.

**Design synchronized; code implementation, integration, CI, GameTest, and live acceptance are not asserted by this document.**
