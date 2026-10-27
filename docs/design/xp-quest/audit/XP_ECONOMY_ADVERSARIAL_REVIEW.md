# XP ECONOMY ADVERSARIAL REVIEW

Base: `ee37afe`.
- **[VERIFIED]** = recomputed from repository data.
- **[ASSUMPTION]** = estimate, not a measured value.

## 1. Ledgers and sinks [VERIFIED]

| Item | Claimed | Recomputed | Result |
|---|---:|---:|---|
| V3 research (`research_v3.json` `approvedTotalRawXp`) | 130,360 | 130,360 | PASS |
| V4 additions (`ResearchCatalog` `add(entries, …)`) | 16,910 | 16,910 | PASS |
| Stage 22 roots (5 × 80) | 400 | 400 | PASS |
| **Total research cost** | 147,670 | 147,670 | PASS |
| Enhancement +0→+20 (authority table) | 1,900 | 1,900 | PASS |
| T1/T2 use the same cost | Recorded in authority | Same in code (shared `EnhancementRules.costFor`) | PASS |
| Current Enhancement code | — | Still charged in **levels** (`XpLevels.pay`); authority has not been implemented | Implementation gap (expected) |

## 2. Supply by stage (active 180 quests, scenario D) [VERIFIED, parsed from A]

| Stage | Quests | XP | Cumulative |
|---|---:|---:|---:|
| Early | 18 | 4,350 | 4,350 |
| Early–mid | 10 | 3,450 | 7,800 |
| Mid | 55 | 26,450 | 34,250 |
| Mid–late | 27 | 20,700 | 54,950 |
| Late | 49 | 57,800 | 112,750 |
| Endgame | 21 | 67,250 | 180,000 |

**This supply is overstated in practice.** Of the 180,000:
- Only **95,300 XP (98 quests) can be registered right away** (see CLASSIFICATION_RECONCILIATION).
- 60,750 XP depends on defect fixes.
- The 17 quests affected by D-01 are unachievable as currently designed.

## 3. Cash flow by stage [ASSUMPTION-based]

- **Early:**
  - Demand: seven roots 560 plus T1 first levels (34 nodes, median 245, about 8,300 in total).
  - Quest supply: 7,800 by the end of early–mid, so quests cover roughly 85%.
  - Enhancement +0→+5 costs 200 per item. That is small.
- **Mid:**
  - Cumulative supply is 34,250.
  - The full T1 tier costs 55,880, so the gap is 21,600, which has to come from ordinary play income.
  - The first T2 research levels have a median of 900 each.
  - **The mid-game bottleneck is real.** Endgame quests hold 37% of the XP, so the averages hide an early-to-mid shortfall.
- **Late and endgame:** supply exceeds MoreMinecrafts demand, and the surplus flows into vanilla enchanting and anvil use.
- **Hourly XP income in ordinary play** (300–800/hour) is an **unmeasured assumption**. The play-time conclusions from earlier documents cannot be accepted until it is measured.

## 4. Circularity and bottlenecks

| Check | Result |
|---|---|
| Research prerequisites requiring XP from quests the same research rewards | **None** [VERIFIED]. No quest has a true prerequisite, and research costs are checked against the player's XP at purchase time. Growth quests (`grw.*`) reward completed spending, so they are not a precondition for that spending. |
| A quest that can only be completed after claiming another quest's XP | **None as a hard dependency.** The research-use quests (`grw.climbing_100` and similar, all VERIFY) need the research to be purchased first, but that XP can come from any source. |
| Infinite XP | **Not possible** [VERIFIED by design]. Every quest is claim-once, and the `spent_*` quests are one-time. |
| Reward far exceeds the effort | **Present (P1):** `bld.place_200000` (D-04), the mining-count quests (D-03), `frm.till_1000`, and `min.debris_16`. Placing and breaking the same block can complete them, and each is worth several hundred to several thousand XP. |
| Reward too small | Early quests (most at 200–250 XP) are about 0.8× a first T1 level, which is borderline. Not a defect. |
| No-farm progress | Possible on the supply side. **Unverified**, because it depends on the hourly-income assumption. |
| Avoiding death loss by holding XP unclaimed | **Intended, approved behaviour** [authority]. Vanilla death drops at most 7×level (capped at 100), so holding XP unclaimed defends against nearly all death loss. This is accepted. |
| **Lump-sum payout when an existing world is updated (D-32)** | **P1.** Retroactive quests (stat- and Knowledge-backed) can complete tens of thousands of XP at once on the first login after the update. That is the same as receiving the endgame payout early. Needs a user decision. |
| Overflow | Movement statistics are stored in centimetres as an int. 250,000 blocks = 25,000,000 cm, which is within range. Total reward 209,100 is within range. `RawXp.available` caps at Integer.MAX. **No issue** [VERIFIED by calculation]. |

## 5. Vanilla systems

- **Enchanting table:** costs 1–3 levels and requires 30 levels. Quest supply makes reaching 30 levels easy. This is an expected effect, not a defect.
- **Anvil:** the level-based cost and the 40-level cap are unchanged. A player who stays at a high level pays more raw XP for the same anvil operation. This is inherent to vanilla.
- **Mending:** quest XP is paid directly, so it does not repair gear [VERIFIED: `ResearchXpAcquisitionMixin` applies only to orb pickup]. Mending still depends on orbs.

## 6. Verdict

The economy arithmetic is **PASS**. The economy is **FAIL** as an implementation-ready design, because:
- the actually registrable supply (95,300 now) is far below 180,000;
- exploits exist (D-03, D-04);
- lump-sum retroactive payout is undecided (D-32);
- no measured income data exists.
