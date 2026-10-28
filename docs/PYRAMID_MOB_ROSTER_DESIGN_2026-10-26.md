# Colossal Pyramid — mob roster design authority (2026-10-26)

**Status:** OWNER-APPROVED DESIGN, implementation DEFERRED. Independent design branch based on main; do not merge or modify Wave 9 release freeze PR #181.

## Approved new visual variants (4)
1. **Buried Mummy / 매장된 미라** — Husk-based model/AI, aged sandy wrappings; ordinary crypts and corridors; baseline 20 HP proposed.
2. **Crypt Archer / 묘실 궁수** — Skeleton-based bow AI, dark bone and lapis/gold decorative texture; guard rooms and grave-goods storage; baseline 20 HP proposed.
3. **Funeral Servant / 장례 시종** — Zombie-based AI, weathered ceremonial blue clothing; offering/ritual rooms; baseline 20 HP proposed.
4. **Gilded Mummy / 금장 미라** — Husk-based AI, gold mask motif and dark bandages; royal antechamber and premium guarded rooms; 28 HP and slightly increased damage proposed, not locked.

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

These variants **replace** some of the previously proposed vanilla spawn allocations; do not automatically add their counts on top. The earlier 16–25 underground-mob range is a PROPOSAL, not an approved quota. Exact per-room counts, probabilities, difficulty scaling, spawn persistence and AI combat balance remain OPEN.

## Loot design boundaries
For now retain corresponding vanilla baseline drops for the four variants. A shared optional thematic trophy (e.g. old seal fragment) feeding the common Relic Catalyst recipe is a future proposal only; no new item or recipe approved here. No elite signature gear for these ordinary variants by default.

## Implementation boundary
DESIGN ONLY: no gameplay code, entity registration, textures, recipes, loot tables, structure NBT, worldgen or tests changed. Prepare a later implementation brief only after owner requests it. Preserve separate release freeze and do not mark PRs Ready or merge.
