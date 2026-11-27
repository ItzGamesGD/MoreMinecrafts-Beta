# ST-01 Pyramid — future exploration-discovery direction (NOT IMPLEMENTED)

Status: approved **design direction**, no implementation, no new numbers. Recorded 2026-11-11 with the Policy D decision.

## Direction

ST-01 Pyramid discovery should increasingly rely on structured exploration information rather than accidental encounters
alone. Preferred channels: the existing bounty/contract system; villager trading; exploration-oriented contracts;
structure-location information obtained through gameplay.

Intended loop: obtain a contract or information → locate a pyramid → travel to it → explore or excavate → complete its encounter.
(Policy D means the player may have to excavate a buried entrance, so this loop is the expected flow, not an edge case.)

## Existing systems that are relevant (they exist today; none of them yet covers ST-01 discovery)

| System | Authority | Relevant fact |
|---|---|---|
| Bounty & Contract | `docs/BOUNTY_CONTRACT_APPROVED_2026-10-22.md` | Exploration objectives credit entry into a registered structure boundary after acceptance; objectives use the accessible Overworld subset of the BC-01 structure catalog (PROVISIONAL tier matrix OPEN). Map ownership/proximity alone do not credit. |
| Villager trading | `docs/VILLAGER_TRADE_AND_ENCHANTED_BOOK_INTEGRATION_CHECKPOINT.md` and the Villager Trading entry in the authority index | Existing trade integration; no ST-01 location trade exists. |
| ST-01 completion/registration | `docs/design/st01-p8c2b/` (completion handoff, canonical instance id) | A registered royal-chest instance has a canonical id and tomb anchor that a future system could reference. |

## Unresolved design choices (owner decisions required; none invented here)

- Whether the ST-01 Pyramid is added to the BC-01 exploration catalog, and at which tier(s).
- Whether location information is a villager trade, a contract reward, a map-like item, or a mix; which villager profession (if any); prices.
- What "location information" means exactly (exact coordinates, a direction, a region, a one-use item) and how accurate it is.
- How a contract treats a pyramid whose entrance is buried (entry credit vs. excavation; structure-boundary definition).
- Any probabilities, rarity tuning or quantities for discovery rewards.
- Interaction with the still-undecided vanilla-structure overlap policy.
