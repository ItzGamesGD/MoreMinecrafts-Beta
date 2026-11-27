# ST-01 P8-D1 — Channel A: bounty-contract discovery (PROPOSAL, not approved)

Everything below is a proposal for owner decision. Probabilities, tiers, rewards and location precision are **unapproved** and are only
named as decision points. Channel A must work for a player who never uses Channel B (and vice versa).

## 1. What the player needs and what exists
Existing contracts credit "enter N distinct instances of structure T after acceptance" and give **no location** (audit F-1). The ST-01
Pyramid is rare (0.548 / 100 km² measured; Desert only), so an unaided ST-01 objective is mostly an exploration burden (F-2).
"Leads to a pyramid" therefore needs *some* lead information. The alternatives differ in how much lead they give and what they cost.

## 2. Alternatives

| | A1 Add ST-01 to the existing pool (no lead) | A2 Located contract (specific pyramid) | A3 Regional-information contract | A4 Visit-completes (completion rule only) |
|---|---|---|---|---|
| What the contract stores | target id `moreminecrafts:st01_pyramid`, count | target id + resolved instance key + position | target id + a coarse hint (e.g. region cell / distance band / bearing) of one or more real pyramids | n/a: it is the *completion* mechanism, usable with A1–A3 |
| Lead for the player | none | exact (or near-exact) | approximate; player still searches | none |
| Code change | one list entry + one translation key; detector unchanged for target match | new objective fields (`optionalFieldOf`), a resolver, UI | like A2 but with a lossy hint; same resolver | already exists (`ContractObjectives.detectStructure`) |
| Needs the location service | no | **yes** (at acquisition/acceptance) | **yes** | no |
| Server-thread risk | none | yes unless resolution is deferred/cached | same | none |
| Rarity fit | poor (F-2) | good | good | — |
| Buried-entrance fit | player must find and dig | player is *told* a place; still digs | player must find and dig | credit is by piece-box (F-4) |
| Failure when no pyramid nearby | contract is just hard | must handle "no target" | must handle "no hint" | — |
| Cross-channel coupling | none | none (own resolver call) | none | none |

**Do not** let possession of a contract complete anything. Completion stays "physical entry after acceptance" (A4), the already approved
semantics; a located contract (A2/A3) only adds a lead.

## 3. Recommended shape (for owner approval)
A4 completion plus **A3 as the default lead, with A2 held back as an owner choice**:
- *Why A3 over A2:* an exact coordinate removes the exploration the owner wants players to do ("explore or excavate"), and a pyramid
  has a 81×81 footprint with a possibly buried entrance; a region hint ("a pyramid lies about N km in direction D", or a named area)
  keeps the search while removing the unbounded wandering. This preference is a design opinion, not an approved rule.
- *A1 alone is not recommended* as the Channel A mechanism because of F-2; it may remain as the "no lead" fallback.

## 4. Proposed mechanics (each a decision point, none approved)
- **Acquisition.** Reuse the existing sources (villager contract offers, wandering trader, loot tables). No new source is *required*.
  Open: whether ST-01-leading contracts appear only from selected professions/levels or only from structure loot; whether the ST-01
  royal chest itself may hold contracts (not today). Independence note: a *contract* bought from a villager is a different item from
  a Channel B *map*; neither needs the other, but both use villagers as a market — owner to confirm that this is acceptable
  ("independent" here means no prerequisite relation, not "no common seller").
- **Target selection.** At materialization the generator cannot see a position (F-5). Options: (i) resolve lazily at **acceptance**
  (the ACCEPT action has the player, level and position) via the shared location service; (ii) store an *unresolved* ST-01 objective and
  resolve it on the first tick after acceptance, bounded per tick; (iii) pick from a pre-built index of known pyramids. Not at pickup.
- **Location information.** Stored on the item with the objective so it survives death, transfer and restart without a world lookup;
  hint precision is an owner decision.
- **Exploration objective.** Existing `EXPLORATION` category with target `moreminecrafts:st01_pyramid`; count rule: keep the approved D2 bands
  or use 1 for ST-01 (owner decision; D2 is approved for the pool, ST-01 is a new member).
- **Completion validation.** Existing server-side entry detection, corrected for overlap (F-3). Whether credit requires entering a
  *specific resolved* pyramid or *any* pyramid is an owner decision; "any instance, distinct" matches the approved model.
- **Persistence.** Item component only (no new world state), optional fields for backward compatibility (F-6).
- **Multiplayer.** Each contract is its own item. Two players may hold the same target; a pyramid being visited, looted or defeated
  by one player must not change another player's contract (see OWNER_DECISIONS_REQUIRED.md §D5).
- **Failure handling.** No pyramid in range → the objective is *unresolved*: the contract stays valid and behaves like A1 (no lead), or is
  refused at acceptance (owner choice). Interrupted resolution → retry on later ticks; never block the tick. Dimension change → ST-01
  is Overworld-only; a contract can be held anywhere and credited only in the Overworld. Target on a vanilla structure → allowed;
  credit must still resolve to ST-01 (F-3).
- **Rewards.** Unchanged: existing tier rewards and `ContractTransactions`. No new reward is proposed.

## 5. Dependencies and risks
Requires the shared location service for A2/A3 (SHARED_LOCATION_ARCHITECTURE.md); the overlap fix (F-3) is needed for A4 correctness in all variants.
