# Distributed Design Documentation — Consolidation Index (2026-10-19)

**Branch:** `design/territory-economy-bounty-approved-20261019` (based on `main` as of 2026-10-19). **Docs-only collection.** This is a convenient place for approved design checkpoints **and transparently marked drafts**. It is **NOT** a merge of the disparate implementations and not a declaration that every existing PR is merged/compatible.

## A. Added directly from the user decisions on 2026-10-19
| Record | Approval scope |
|---|---|
| [Territory/economy](TERRITORY_ECONOMY_APPROVED_2026-10-19.md) | T0–T3, settlements, guard and trading layer, biome premium market; exact trade recipes **OPEN** |
| [Bounty/commissions](BOUNTY_COMMISSION_APPROVED_2026-10-19.md) | World offers, generation, progress rules, rewards bands, backend-first UI gate; special numeric costs **OPEN** |
| [Desert artifacts/mercenaries](DESERT_ARTIFACTS_AND_MERCENARIES_2026-10-19.md) | Mercenaries **approved**, desert artifact-market direction approved, individual new effects require overlap review |

## B. Original documents **copied byte-for-byte** from scattered design branches
| File | Source branch / upstream PR | Status |
|---|---|---|
| [Accessory roster 12](ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md) | `docs/stage23-accessories-approved-20261016`; ancestor of PR #113 | Approved roster with proposed figures marked |
| [Accessory slots/+10](ACCESSORY_ENHANCEMENT_DECISIONS_2026-10-17.md) | `docs/accessory-enhancement-approved-20261017`, PR #113 | Later approved overlay; prevails on conflicts |
| [Adamantium gear](ADAMANTIUM_EQUIPMENT_DESIGN_2026-10-18.md) | `docs/world-events-patrol-design-20261018`, PR #122 | Approved design, not implemented |
| [Patrol world events](WORLD_EVENT_PILLAGER_PATROL_DESIGN_2026-10-18.md) | same branch, PR #122 | Approved patrol variant design |
| [Rare caravan / rune](RARE_CARAVAN_ENCHANTMENT_RUNE_DECISIONS_2026-10-18.md) | same branch, PR #122 | Approved caravan/rune decisions |
| [XP Quest A–D, contracts, handoff, audits](design/xp-quest/README.md) | `claude/xp-quest-authority-20261017`, Draft PR #112 | Mix of approved decisions, proposals, audits; follow statuses **inside documents** |
| [200-quest chapter proposal](design/xp-quest/structure/CHAPTER_DESIGN.md) | `design/xp-quest-structure-200-20261019`, Draft PR #129 | **PROPOSAL / owner review required**, not approved implementation |

This collection includes the XP Quest A–D core documents, interface contract, handoff, and 7 audit/analysis documents from PR #112, rather than just their titles. Original PRs/branches stay untouched.

## C. Scope, collisions and hierarchy
- Existing historical `main/docs` documentation remains unmodified. This branch adds new paths; it does **not** replace historical authority docs or silently override implementation code.
- Newest **explicit human-approved** design decisions prevail over older contrary historical proposals, but a **PROPOSAL** or **DRAFT** can never become approved merely by being copied here.
- The 2026-10-07 content-axis review's deferred economy/territory language is **superseded specifically by the newer 2026-10-19 user approvals**. Unrelated prior content remains authoritative where not contradicted.
- The accessory source records show **시간의 고리 (Time Ring)** but do not clearly establish the previously designed **시간의 모래 (Sands of Time)** provenance. User expressly says the latter was completed elsewhere. **Treat as a source-discovery gap**: do not silently equate them, register duplicates, or freeze unverified effect stats.
- **XP Quest** (existing XP quest progression/200-catalog) and **Bounty/Commission** (new shared world offers and contract items) are different systems. Do not combine their resource/acceptance counters.
- The new desert artifacts must obey existing five-category equipment and accessory enhancement design, not establish a different artifact system.
- Newly approved mercenary healing/cost contracts prevail over earlier chat's abandoned 3-hire/60-minute/high-cost drafts.

## D. Other still-open PR/branch lines (NOT MERGED)
- XP Quest UI work: PRs **#125/#126** (selector redesign), separate XP Quest catalog / progression implementation line **#112–#129**. These may update authority as tests/owner approval proceeds. Their **code and UI resources are deliberately not merged into this docs branch**.
- Research implementation, stage 23 live fixes and reconciliations: separate Draft PR stack (#90 onward). Their source and test fixtures remain on their independent branches.
- Latest PR #122 already stacks on the approved accessory doc branch; this docs collection does not mark #113 or #122 merged, and is **not** a replacement for their review.
- Other ancillary design and QA docs on those implementation branches have **not been globally swept into this branch**. A full all-branch code+docs convergence requires a separate dependency/merge-base audit and supervised integration. Thus this is a **consolidated design reading branch**, not a claim that every file from every remote branch has been copied.
- The separate `MoreMinecrafts-Structure-Generator` repo is outside this repository and not consolidated here.

## E. Recommended next gates
1. Review this docs-only PR for approval classifications and source provenance.
2. Locate original Sands of Time authoritative description, cross-check possible Time Ring naming confusion, resolve desert artifact overlap/slot allocations.
3. For future integration select a vetted implementation base (not this old `main` if later code PRs are required) and port **only approved, conflict-reviewed docs** and necessary code by PR lineage. Do not blind-merge every branch.
4. No docs-only CI green check (if any) counts as gameplay implementation or live verification.
