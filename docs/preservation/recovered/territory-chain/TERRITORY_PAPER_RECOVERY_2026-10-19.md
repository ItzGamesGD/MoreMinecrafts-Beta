# Territory Paper design recovery — 2026-10-19

Stage D2-A. Documentation only; no gameplay implementation or merge. This is a fresh task branch, `design/territory-registration-recovery-d2a-20261019`, created from the freshly verified Draft PR #131 HEAD **`befa4c8a44c8e3556be2dca9af502d36b0cf11ad`**, targeting `design/territory-economy-bounty-approved-20261019`. The older D1 initial HEAD was not used as the branch base. Dates in this report use Asia/Seoul.

Read the latest remote authority matrix, conflict register, branch coverage, implementation plan, territory checkpoint, authority router and AGENTS.md. Remote GitHub refs/files were the recovery authority; the local checkout was an older `work` branch. Shell Git fetch failed because its configured proxy was unreachable, so GitHub connector reads and writes were used. No previous conversation or scratch audit was needed as evidence.

## Evidence classes

- **USER-CONFIRMED HISTORICAL INPUT:** explicitly supplied in the D2-A task. This records historical intent, not approval to activate a Fabric feature or fill omitted policy.
- **RECOVERED PAPER RECORD:** a cited historical repository document corroborates a statement. A secondary sync summary is identified as such; it is not the missing original design conversation.
- **APPROVED FABRIC:** the scoped current territory checkpoint already approves the rule.
- **PROPOSED / DEFERRED:** new technical design or unresolved policy; neither is silently promoted.
- **SOURCE MISSING:** the original artifact/precise rule was not located in the bounded search. A user-confirmed historical fact may still have SOURCE MISSING original provenance.

## Source ledger and search scope

| Source | Immutable reference | What was recovered |
|---|---|---|
| Paper latest design sync, dated 2026-08-19 | [latest-design-sync.md](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/d2944f78cfb3f0d214f6bbbc85010facddf14643/builds/exploration-package-v1/latest-design-sync.md), blob `a52b9def6a58adb92b464fbb113645a8bbdab6a4`; path history identifies `d2944f78cfb3f0d214f6bbbc85010facddf14643` | Physical liberation deposits, separate territory ownership, bell/well anchor direction, Display/Interaction, lazy updates, protection/normal interactions, territory implementation deliberately excluded |
| Paper package review | [exploration-package-review.md](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/0e0d1d4e02acb528b40fb1f4cbcaed92ec72a94f/builds/exploration-package-v1/exploration-package-review.md), origin `3fe79a5a84271dfdb68ab10818deacd84f37e11e`, latest edit `0e0d1d4e02acb528b40fb1f4cbcaed92ec72a94f` | References first/second/third territory–exploration finalizations, player/biome attribution and item deposits, but explicitly excludes territory from implementation package |
| Paper pre-territory freeze | [19-pre-territory-freeze-and-regression-audit.md](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/fb6880a034860cc3ba00fad3553f8f4d5db39364/builds/exploration/19-pre-territory-freeze-and-regression-audit.md) | E1–E8 exploration freeze, territory excluded, no claim of territory implementation or live validation |
| Later Paper retirement | [gameplay-subsystem-simplification-stage-6.md](https://github.com/ItzGamesGD/rpg-plugin-for-minecraft-26.1.2-paper/blob/0db90dba631a7cbd49adb7e7a58e1e0d63bbce91/docs/gameplay-subsystem-simplification-stage-6.md); earlier retirement edits `6aee06630b5058e96107a31fd0af703c05112f17`, `45861bd12d432a1317548feb5d857d35e0a257c7` | Generic Exploration, Quest and Farming/Cooking runtime retired. Old exploration/farming wiring is not a live territory dependency or reusable approval |
| Earlier Fabric axis review | [CONTENT_AXIS_REVIEW_2026-10-07.md](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/befa4c8a44c8e3556be2dca9af502d36b0cf11ad/docs/CONTENT_AXIS_REVIEW_2026-10-07.md) | Settlement/economy were deferred candidates at that time |
| Current scoped Fabric approval | [TERRITORY_ECONOMY_APPROVED_2026-10-19.md](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/befa4c8a44c8e3556be2dca9af502d36b0cf11ad/docs/TERRITORY_ECONOMY_APPROVED_2026-10-19.md) | Optional T0–T3 progression, building/guard/economy scope; §5 explicitly leaves claiming/limits/liberation for reconfirmation |
| D1 reconciliation | [matrix](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/befa4c8a44c8e3556be2dca9af502d36b0cf11ad/docs/DESIGN_AUTHORITY_MATRIX.md), [conflicts](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/befa4c8a44c8e3556be2dca9af502d36b0cf11ad/docs/DESIGN_CONFLICT_REGISTER.md) | A03/F02 and A04/F03 remain implementation gates; this recovery refines their evidence, not blanket-closes them |
| D2-A task statement | User-confirmed historical inputs reproduced in the table below | Primary authority for what the user says was historically designed; no fabricated chat URL or original commit supplied |

Paper remote `main` was `10cbdc99d069d910006b21111a83ac0c307e2d7f`. All seven available Paper branch tips were inventoried through non-truncated recursive trees; all **161 distinct Markdown/YAML blobs** across them were read/searched for territory/영지, registration, Display and state names. Branches: `main`, `ops/delete-public-noise-branches`, `backup/final-main-to-drive` (`a50895e951bdd14834f2a409f482549872aa58b0`), `chore/public-repo-prep` (`ac43d3c10f22b0895d42a1761891ca696a5c36ad`), `ci/build-plugin-artifact` (`68e1a60abf82792703cfe51990badc8f3df93300`), `codex/inspect-and-correct-pr-#51-d6r0fv` (`272cdf097e0c8797cafa50d33d52da0864cc5fea`), and `design/moreminecrafts-handoff-2026-10-07` (`6ef8c3766f473342589c3d043dd182f91e7feb21`).

Also inspected relevant path histories, six pages of 100 recent reachable Paper commit messages, repository commit searches for `territory`, `영지`, `registration`, `liberation`, available Paper PR metadata, and the historical sync tree. Search returned the freeze commit for `territory`; the other three commit-message queries returned none. These searches do not prove that deleted/unreachable commits or external chats contain nothing.

The archived exploration ZIP exists at sync commit under `builds/exploration-package-v1/HyunseoRPG_exploration_package_v1.zip`, blob `efec5c740d8ebe85bb7e2e58814d43d2a06805a9`. The GitHub UTF-8 connector rejected binary retrieval (HTTP 400); its internal contents were **NOT inspected**. The adjacent review explicitly says territory was not included. The referenced original first/second/third territory finalization documents/conversations and detailed registration code were **not located**. There is no claim that an implemented Paper territory engine was recovered.

## Recovered decisions and later disposition

| ID | Historical statement | Evidence and classification | Fabric disposition |
|---|---|---|---|
| H01 | Player directly registers a village; unregistered villages remain fully usable | USER-CONFIRMED HISTORICAL INPUT. Current Fabric §1 corroborates optional participation and vanilla use | Preserve optionality. Eligibility/detection, discovery and exact registration initiation remain DEFERRED |
| H02 | Text Display + Interaction entities form the on-site interface | USER-CONFIRMED HISTORICAL INPUT; Paper sync corroborates Display/Interaction but does not spell out TextDisplay subtype | Recover as interface direction. Actual Fabric class feasibility assessed in companion spec; no final layout/range selected |
| H03 | Players physically throw vanilla items into the interaction area | USER-CONFIRMED HISTORICAL INPUT; Paper sync says liberation uses actual item deposits rather than combat | Registration is not a GUI inventory debit, NPC purchase or combat liberation. Exact collection geometry, intentional-drop rules and capacities remain DEFERRED |
| H04 | First valid contributor becomes owner | USER-CONFIRMED HISTORICAL INPUT; original exact rule SOURCE MISSING in scanned Paper records | Preserve historical rule. Define first by serialized valid accepted contribution in proposed spec; tie/eligibility/ownership consequences require review |
| H05 | AVAILABLE → CLAIMING → LIBERATED | USER-CONFIRMED HISTORICAL INPUT; no matching original state enum/spec located | Historical registration states only. Do not equate with T0/T1/T2 or Exploration's VANILLA/UNDISCOVERED/ACTIVE/CLEARED/ABANDONED |
| H06 | Historically one territory per biome per player | USER-CONFIRMED HISTORICAL INPUT; package review corroborates player/biome attribution, not the exact cardinality | Historical cap, not a newly finalized Fabric cap. Biome key, dimensions, counting pending claims, transfer/loss/reset still DEFERRED |
| H07 | Materials include vanilla wood, cobblestone, sticks, planks, coal and iron; quantities not finalized | USER-CONFIRMED HISTORICAL INPUT; precise original list/quantities SOURCE MISSING | Do not invent counts or expand “wood/iron” into a registry/tag list. Logs vs wood blocks, species, charcoal and iron form remain DEFERRED |
| H08 | Bell/well anchor, separate territory vs shared exploration ownership; liberation not a fight and raid cores not liberation costs | RECOVERED PAPER RECORD: sync/review | Anchor selection remains PROPOSED for Fabric; no bell placement grants ownership. Do not import exploration state or charge raid cores for registration |
| H09 | Lazy Display/Interaction/shop updates, no always-running per-territory scheduler | RECOVERED PAPER RECORD: sync/review | Compatible performance direction. A bounded loaded-only collector still needs a trigger; purely click-only code cannot receive thrown items |
| H10 | Protection prevents permanent world changes but permits doors/buttons/levers/territory interaction | RECOVERED PAPER RECORD: sync | Current Fabric §1 narrows protection to territory-managed buildings/blocks and forbids overwriting resident construction. Blanket historical region protection must not be imported |
| H11 | No raid-failure ruin, rebuilding costs or economic loss | RECOVERED PAPER RECORD; current Fabric §1–2 independently approves no raid regression/upkeep | Compatible. Does not define ownership loss after death/logout/abandonment |
| H12 | Historical on-site farming deliveries, repeat stock/raid details and territory T1–T9 work units | RECOVERED PAPER RECORD only; later Paper retirement and current Fabric T2/T3 boundaries differ | Do not import removed Farming/Cooking/Quest runtime, repeat procurement, raid scheduler or T1–T9 labels as Fabric tiers. Only scoped current approved services/bounties apply |

No scanned later decision expressly supplies a Fabric ownership transfer/loss rule or says that historical LIBERATED automatically means T1. The Oct 19 approval supersedes the earlier axis deferral for its stated scope, but explicitly leaves claiming reconfirmation open. D2-A confirms the historical inputs without silently finalizing those outstanding Fabric choices.

## Registration states versus T0–T3

Recommended **PROPOSED** representation: registration status and development tier are separate fields. Registration explains who is contributing and whether the registration requirements are satisfied; tier explains whole-territory development. This avoids an invented bijection.

| Historical state | What is known | What must not be inferred |
|---|---|---|
| AVAILABLE | No accepted registration owner under the historical flow; ordinary village remains usable | Not a new tier or proof of T0 eligibility/identity algorithm |
| CLAIMING | First valid contributor has ownership under historical H04; requirements incomplete | Not T1, not communal ownership, not a fully upgraded settlement |
| LIBERATED | Historical registration/material completion milestone | Not T1/T2/T3, no auto-construction/population/guard entitlement established |
| T0–T3 | Current whole-territory development system; T0 ordinary village, T1 settlement, T2 developed, T3 prosperous | Not replacements for three historical registration states |

Alternatives requiring explicit approval: (a) completed registration separately opens a T0→T1 upgrade; (b) registration completion is itself the approved T1 purchase; (c) reword/remove historical labels after preserving migration semantics. **Recommendation:** keep the axes separate and leave production transition from LIBERATED to development entitlement disabled until chosen. This is a modeling recommendation, not a gameplay decision.

See [Fabric specification](TERRITORY_REGISTRATION_FABRIC_SPEC.md) for the proposed state/transaction design and [ownership conflicts](TERRITORY_OWNERSHIP_CONFLICTS.md) for the remaining decision ledger. Exact material counts remain unfinalized. Original source documents, including the territory checkpoint, remain unchanged.
