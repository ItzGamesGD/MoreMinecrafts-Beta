# Colossal Pyramid — mob roster design authority (2026-10-26)

**Status:** OWNER-APPROVED DESIGN. **Implemented; owner review passed (2026-11-14 sync)** on branch `claude/pyramid-mummy-variants-fu6ijm` (not yet merged into `work/grand-unified-20261112`; natural spawning and pyramid encounter placement are not part of that implementation). *(Superseded wording: "implementation DEFERRED. Independent design branch based on main; do not merge or modify Wave 9 release freeze PR #181.")*

**2026-11-14 owner decisions recorded here:** (1) the four-variant + four-vanilla roster is unchanged; (2) HP 20 / 20 / 20 / 28 (Buried Mummy / Crypt Archer / Funeral Servant / Gilded Mummy) is **owner-confirmed**, no longer "proposed"; (3) conversion rule: no water or powder-snow conversion rule is needed; variants are **not converted** and are left to die naturally. Code check below.

## Approved new visual variants (4)
1. **Buried Mummy / 매장된 미라** — Husk-based model/AI, aged sandy wrappings; ordinary crypts and corridors; 20 HP (owner-confirmed 2026-11-14).
2. **Crypt Archer / 묘실 궁수** — Skeleton-based bow AI, dark bone and lapis/gold decorative texture; guard rooms and grave-goods storage; 20 HP (owner-confirmed 2026-11-14).
3. **Funeral Servant / 장례 시종** — Zombie-based AI, weathered ceremonial blue clothing; offering/ritual rooms; 20 HP (owner-confirmed 2026-11-14).
4. **Gilded Mummy / 금장 미라** — Husk-based AI, gold mask motif and dark bandages; royal antechamber and premium guarded rooms; 28 HP (owner-confirmed 2026-11-14); "slightly increased damage" remains a proposal, not locked.

Reuse vanilla model families, movement, animation and AI with textures/minor stat adjustments. Do not add special skills or unrelated combat mechanics. Verify overlap with the earlier 40-mob roster before item/entity ID allocation.

## Approved accompanying vanilla mobs
- **Husk / 허스크** — common burial corridors and crypts.
- **Skeleton / 스켈레톤** — guard posts and armories.
- **Bogged / 보그드** — sparse older damp crypts; poison-arrow difficulty must be telegraphed and not spammed.
- **Spider / 거미** — sparse neglected corners and optional hidden routes.

**Tomb Guardian / 왕릉 수호자** is the separate unique royal boss, not part of the eight regular mob types. Total design roster: eight ordinary mob types (four variants plus four vanilla) and one boss. Existing vanilla mobs are selected by theme; no new Bogged ability is invented.

## Encounter distribution and constraints
- Grand above-ground ceremonial halls remain calm and largely enemy-free.
- Ordinary crypts: mainly Buried Mummy, Husk, Funeral Servant; avoid placing enemies in every room.
- Guarded grave goods: Crypt Archer and Skeleton, with selected other guardians.
- Old/damp crypts: occasional Bogged; optional neglected/secret routes may contain Spider.
- Royal antechamber / valuable treasure: Gilded Mummy with sparse supporting enemies.
- **Boss chamber during boss encounter: Tomb Guardian only; no incidental mob waves or unavoidable adds.**

These variants **replace** some of the previously proposed vanilla spawn allocations; do not automatically add their counts on top. The earlier 16–25 underground-mob range is a historical PROPOSAL, superseded by the owner decision below.

**Natural spawning — owner decision (2026-11-21 sync):** No custom per-room spawn manager, quota, or persistence system is used. Like a normal vanilla structure, `st01_pyramid.json` (`spawn_overrides.monster` with `"bounding_box": "piece"`) acts as a **mob-pool filter** for the 8-mob pool (the 4 variants + Husk, Skeleton, Bogged, Spider; excluding Creeper, Camel/Zombie Horseman, etc.), and actual spawning follows **vanilla natural spawn rules**. Only the royal hall (boss room) excludes natural monster spawns (`St01RoyalHallNaturalSpawnMixin`).

## Loot design boundaries
For now retain corresponding vanilla baseline drops for the four variants. A shared optional thematic trophy (e.g. old seal fragment) feeding the common Relic Catalyst recipe is a future proposal only; no new item or recipe approved here. No elite signature gear for these ordinary variants by default.

## Implementation boundary
Merged into `checkpoint/grand-unified-20261116` (via `claude/pyramid-mummy-variants-fu6ijm`).

## Conversion rule & HP — owner decisions (2026-11-14 / 2026-11-21 sync)
No water / powder-snow conversion rule is required. The variants are not converted (Husk-based → Zombie, Zombie-based → Drowned, Skeleton-based → Stray); they are left to die naturally. `PyramidHusk.convertsInWater()` returns false, `PyramidZombie` blocks the Drowned conversion, `CryptArcher` extends `AbstractSkeleton` (no powder-snow Stray conversion), with GameTests per blocked conversion. Gilded Mummy max health is 28 HP (`8721b199` in `PyramidMobs.java`); the other three variants keep vanilla 20 HP.
