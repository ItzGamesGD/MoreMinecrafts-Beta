# BC-01 Registry and Objective Audit

Baseline: PR #148 Draft HEAD `b7cedf199a5f1eda341ce3bd27eabb22eeb309e1` (verified with `gh pr view 148`). Authority: `docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md` and `docs/CONTENT_AUTHORITY_INDEX.md`. This is a static source audit, not a live spawn test.

| Area | Verified static coverage | Main uncertainty |
| --- | ---: | --- |
| Custom mobs | 40 registrations / 40 spawn specifications | actual survival spawn frequency and candidate weighting |
| Vanilla delivery IDs | 87 candidate IDs | exact final eligibility, costs and caps |
| MM material IDs | 7 | yield and material opportunity cost |
| MM fish IDs | 12 | grade-specific catch effort and value |
| MM structures / biomes | 0 / 0 | future content; excluded now |
| Vanilla exploration | 12 structure names, 10 biome examples | exact registry IDs and visit definition |

Catalogs: [mobs](BC-01_MOB_CATALOG.md), [delivery](BC-01_DELIVERY_CATALOG.md), [exploration](BC-01_EXPLORATION_CATALOG.md). Candidate classification does not approve an implementation pool. Owner decisions: neutral/tameable kill eligibility, exact vanilla/MM target pool and tier weights, rare material caps, fish eligibility, exploration visit definition, direct special reward probabilities. Engineering checks: runtime registry enumeration, survival spawn trials, worldgen and server structure/biome entry detection.
