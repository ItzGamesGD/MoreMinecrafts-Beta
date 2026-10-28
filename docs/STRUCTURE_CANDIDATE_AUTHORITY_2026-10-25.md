# MoreMinecrafts — Structure Candidate Authority Register (2026-10-25)

> **Status:** Design inventory; not an implementation authorization. Reconstructed from owner decisions in project discussions and checked against PR #147 and the two repositories' inspected main/design trees. **Candidate selection does not mean detailed specification or GitHub file previously existed.** This file is a tracking index, not a retroactive claim that every concept was already documented.
>
> **Priority:** Preserve existing owner decisions and original PR sources. Do not auto-approve unconfirmed block geometry, dimensions, mobs, loot or technical generation settings.

## Structure candidate portfolio (10 historical IDs; ST-02 cancelled)

| ID | Structure | Dimension / setting | Confirmed high-level intent | Design record status |
|---|---|---|---|---|
| ST-01 | Colossal Pyramid / 거대 피라미드 | Overworld desert | Separate large pyramid, not just vanilla desert pyramid enlargement; exploration/combat focus, no compulsory complex puzzle | [Pyramid preparation and Tomb Guardian encounter](PYRAMID_DESIGN_PREP_2026-10-25.md) plus [owner-approved regular mob roster](PYRAMID_MOB_ROSTER_DESIGN_2026-10-26.md); architecture/worldgen and exact balance remain OPEN; DESIGN ONLY |
| ST-02 | Sun Temple / 태양 신전 | Overworld desert (historical) | **CANCELLED by owner**, as recorded in ST-04's later design authority; preserve ID and history, do not count as active | CANCELLED; no implementation authorization |
| ST-03 | Pillager Headquarters / 약탈자 본거지 | Overworld flat plains/flat-ground forest | 112×112 fortified military village, detailed combat and reward policies | **Detailed source of truth:** [PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md](PILLAGER_HEADQUARTERS_DESIGN_2026-10-22.md); Draft PR #147 |
| ST-04 | Spider Colony Cave / 거미 군락 동굴 | Overworld natural cave | Cave-following spider-colony ecology, approved through Q104 | [Owner-approved Q1-Q104 design](SPIDER_COLONY_CAVE_BIOME_DESIGN_2026-10-26.md); technical feasibility OPEN; DESIGN ONLY |
| ST-05 | Ancient Colosseum / 고대 투기장 | Overworld enormous natural cave **only** | Underground combat/trial arena; not a surface colosseum | Concept only; detailed encounter OPEN |
| ST-06 | End City and Ship Remake / 엔드 도시·함선 개편 | End, existing vanilla structures | Improve existing End City and ship architecture with End-themed blocks; remodel not replacement by unrelated structure | Concept only; integration OPEN |
| ST-07 | Void Citadel / 공허의 성채 | End | Distinct large fortress | Concept only |
| ST-08 | End Observatory / 엔드 천문대 | End | End observatory-themed structure | Concept only |
| ST-09 | Void Monastery / 공허의 수도원 | End | Monastery concept; previously noted as difficult to implement | Concept only; technical feasibility OPEN |
| ST-10 | End Shipyard / 엔드 함선 조선소 | End | Separate shipbuilding site, distinct from End Ship Remake | Concept only |

## Related but not counted as 10 new selections

- **Ancient City remodeling / 고대 도시 리모델링:** separate pre-existing design and implementation work in `ItzGamesGD/MoreMinecrafts-Structure-Generator`; do not double-count as an eleventh new candidate in the 10-item selection.
- **Forgotten Mining City / underground smelting city:** discussed, but not established as part of the above selected 10.
- **Undersea metropolis / ocean monument revisions:** ideas requiring separate owner selection; do not auto-promote.
- **Overgrown greenhouse:** rejected as standalone structure in favor of lush-cave variant direction.
- **Deep sea observatory:** not selected as a standalone structure.
- **Territory system buildings:** territory system shelved; don't revive them via this registry.

## Authority and workflow

1. **Detailed authoritative record:** HQ PR #147 document; original decision history preserved in full, with newer decision index at top. The current PR body may summarize only the original scope; always read latest document HEAD, not only PR description.
2. **Supersession:** ST-02 was cancelled by the owner as recorded in [ST-04 design](SPIDER_COLONY_CAVE_BIOME_DESIGN_2026-10-26.md). Preserve the historical ID; do not reactivate it from this older register. ST-01 regular mobs and ST-04 Q97-Q104 are later owner approvals.\n3. **Candidate-only entries:** names and general environment are carried from prior owner discussions; GitHub code or independent design docs **not yet verified** for all candidates. Do not claim they were implemented.
4. **Next:** develop ST-01 pyramid through owner WHAT decisions; keep Claude's exact architecture/block shapes and SG worldgen HOW distinct.
5. **No merge or production-code change:** this index lives in PR #147 until integration review. Later move/copy into an agreed mainline authority location without silently merging.
