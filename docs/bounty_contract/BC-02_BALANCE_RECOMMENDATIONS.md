# BC-02 Balance Recommendations

**All values below are PROPOSED for owner review, not approved design.** Keep Q1–Q62, especially unlimited accepted contracts, duplicate objective credit, shared combat progress, purchase/payout bands, rarity and quantity bands. These proposals do not authorize gameplay implementation.

## Candidate delivery caps

The maximum advanced emerald-only gain is 38, with midpoint 21.5. If an item costs hypothetical `v` emerald equivalents, a conservative candidate cap is `floor(21.5/v)` before allowing an unverified extra reward to offset cost. This is a screening threshold, not a verified market price. Reject a bundle when total modeled delivery cost exceeds its tier-adjusted payout capacity.

| Candidate | BC-01 ID | Hypothetical value/unit | Midpoint break-even quantity | Proposed cap for sensitivity | Status |
| --- | --- | ---: | ---: | ---: | --- |
| Wheat | `minecraft:wheat` | 0.04 | 112 at beginner midpoint 4.5 | approved beginner ordinary band 16–32 | PROPOSED selection |
| Diamond | `minecraft:diamond` | 3 | 7 at advanced midpoint | 2–4 | PROPOSED; actual value UNKNOWN |
| Adamantium ingot | `moreminecrafts:adamantium_ingot` | 12 | 1 at advanced midpoint | 1, or exclude until value measured | PROPOSED; actual value UNKNOWN |
| Mythril | `moreminecrafts:mythril` | UNKNOWN | UNKNOWN | 1–2 sensitivity only | PROPOSED |
| MM fish, each stack 1 | 12 verified `moreminecrafts:` fish IDs in BC-01 | UNKNOWN by species/grade | UNKNOWN | 1–2, species-specific | PROPOSED |

Approved rare delivery count bands are 2–4 / 4–12 / 8–24 by tier. A cap of 1 or 2 for a rare item would conflict if that item were placed in a rare-delivery band requiring at least 8 at advanced tier. **Owner decision required:** exclude such items from that tier, provide a separately approved item-specific count exception, or select a different objective. Do not silently override the approved count bands. The simulated 8-adamantium case is a deliberate stress test of that conflict, not an implementation candidate.

## Candidate objective effort weights

These weights are relative sampling/price-adjustment proposals, not observed completion times. Use only after measuring survival spawn and travel rates. The approved objective count bands remain fixed.

| Objective class | Proposed relative effort per unit | Basis / validation needed |
| --- | ---: | --- |
| Common Overworld mob kill | 1 | baseline; spawn frequency UNKNOWN |
| Structure-gated or Nether mob kill | 2–4 | access and spawn search; measure |
| Farmable delivery item | 0.2–0.5 | renewable supply and automation; measure |
| Common mined item | 0.5–1 | mining/tool availability; measure |
| Diamond or high-value ore delivery | 4–12 | opportunity cost dominates; owner selection |
| Known nearby structure revisit | 1–2 | accepted revisit rule; detection must be precise |
| First difficult structure visit | 8–20 | travel and hazards; world-seed sensitivity |
| Conditional trial special | 12–24 | encounter requirement unspecified; owner definition |

Weight target selection away from low-effort repeatable pairs, but preserve shared credit. For high-value material, prefer exclusion until real acquisition rates and item sinks are known. Keep all unavailable MM structures, meteors and unverified events out of implementation-ready pools. Record the final pool, cap and reward item selections in design authority only after owner approval.

## Owner decisions for BC-03 input

Decide exact eligible mob/item/structure IDs and tier weights; neutral/tameable mob kill eligibility; item-specific caps and how they coexist with approved count bands; reward item pool and item-specific payout caps; visit/entry criteria; exact structure tier matrix; direct special probabilities. Require empirical spawn, fishing, mining and travel samples before treating the proposed values as balance facts.
