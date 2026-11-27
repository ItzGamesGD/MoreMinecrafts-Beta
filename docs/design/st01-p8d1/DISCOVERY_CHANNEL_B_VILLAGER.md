# ST-01 P8-D1 — Channel B: villager-trade discovery (PROPOSAL, not approved)

Independent of bounty contracts: no contract is needed to buy, read or use the discovery item. Prices, professions, levels, limits and
accuracy are **unapproved**.

## 1. Feasibility in 26.3
Explorer-map trades are data-driven (see audit §3): a `villager_trade` JSON with an `exploration_map` item modifier whose `destination`
is a structure tag, added to a profession level tag. For ST-01 this needs: a structure tag `moreminecrafts:on_st01_pyramid_maps`
containing `moreminecrafts:st01_pyramid`; one trade JSON; an additive entry in `#minecraft:cartographer/level_N`; a map item
(`filled_map` or a new item) and a decoration (an existing icon, or a code-registered one). Whether the mod's additive tag entry merges
into the vanilla tag as expected is **UNVERIFIED in this repo**; it is the first item of the proposed feasibility probe (roadmap P8-D2a).

## 2. Information formats compared

| Format | How | Precision | Pros | Cons |
|---|---|---|---|---|
| B1 Explorer map | vanilla `exploration_map` modifier | marker at the located chunk (±), centered map | no new UI, vanilla-familiar, persists with the item, trade drops itself if nothing found | locate at offer time is synchronous (see §4); buyers cannot tell which of several pyramids; marker is *not* the entrance; a sea of map items if repeatable |
| B2 Written clue | custom item with text/component | any (region name, bearing+distance band) | accuracy fully under mod control; can be fuzzy on purpose | new item, UI/text, translation, tooltip; needs the location service |
| B3 Approximate coordinates | book/paper with coordinates | tunable | trivial to read | feels like a command; spoils exploration; needs the location service |
| B4 Compass-style item | lodestone-like tracker | continuous | strong guidance | most new code; changes the feel; out of proportion |

**Recommendation for owner choice:** B1 *with a deliberately coarse result*: either a map of reduced zoom/size or a clue (B2) built from the
same located position. B1 is the lowest-risk path because the game already owns trade generation, persistence and the failure rule
(trade discarded when nothing is found). B2 is preferable if the owner wants imprecision the vanilla map cannot express.

## 3. Profession and level (candidates, not decisions)
Cartographer is the natural seller (vanilla explorer maps; also an ELIGIBLE contract seller — no conflict, the two items are unrelated).
Level: vanilla explorer maps sit at level 2–3 (the jungle/swamp ones are level 2 in 26.3); a higher level would make ST-01 maps an upgrade
milestone. Other professions would need their own trade sets. Owner decision.

## 4. Mechanics and risks
- **Target selection.** Nearest valid pyramid to the villager (vanilla behaviour). `skip_existing_chunks` could prefer unexplored areas
  (it checks whether the located chunk already exists); it does *not* know whether a Guardian was defeated.
- **Persistence.** The map item stores its id and decorations in the saved offer; stable across restarts and identical for every buyer.
- **Repeatability.** `max_uses` and restocking make the same map repeatable; vanilla uses 12 uses. Owner to cap (e.g. `max_uses`) —
  also relevant because "villager supply is effectively repeatable" (villager/book checkpoint §8).
- **Server cost.** The locate runs on the server thread when the offer is created (first inventory open or level-up). It is bounded only by
  `search_radius`. Measurements and a bound proposal are in SHARED_LOCATION_ARCHITECTURE.md; a villager in a Desert far from any pyramid
  makes the trade disappear, in a non-Desert area the search can be expensive per call.
- **Multiplayer.** The target is the villager's, so several players buying from one villager receive the same pyramid; players at
  different villagers get different ones. No per-player copies.
- **Failure handling.** No pyramid found → vanilla discards the offer (the trade simply is not shown); owner may prefer a visible
  "sold out"/clue fallback instead (needs custom code). Player changes dimension → the map is a world map item (Overworld only);
  target on a vanilla structure → allowed.
- **Compatibility.** `T1Supply.normalizeTrades` runs on every `getOffers`; it only removes/normalizes enchanted-book and smith offers
  and would not touch a map offer (code-read; UNVERIFIED by test). The XP quest entry `xpq.exp.explorer_maps` may interact with a new
  map item (not audited).
- **Spoiler/exploration balance.** A pinpoint marker removes search; a coarse clue keeps it. Because the entrance may be buried
  (Policy D), a marker should not promise an entrance — wording and icon are owner decisions.
