# ST-01 P8-D2 — Owner decision record: Channel A4, Channel B1, Architecture 3 (hybrid)

Date: 2026-11-11 · Source: P8-D2 brief §2 · Status of this record: the three directions below are **approved**; everything marked *provisional* is an
unapproved implementation detail. Nothing in P8-D2 implements any of it. Builds on `docs/design/st01-p8d1/` (proposals there that are not repeated
here remain proposals).

## Approved
1. **Channel A4 — bounty explorer map.** The bounty system must eventually support obtaining an ST-01 Pyramid **vanilla-style explorer map**.
2. **Channel B1 — villager explorer map.** Villager trading must eventually provide ST-01 Pyramid explorer maps, using vanilla explorer-map
   behavior wherever feasible.
3. **Architecture 3 — hybrid.** Prefer vanilla structure-location and explorer-map behavior where technically safe; add custom infrastructure only
   where necessary for correctness, performance, persistence or integration. It is **not** an authorization for a new map-rendering system.
4. **Independence.** A4 and B1 are independent acquisition channels. They may share structure-location infrastructure, structure tags, map-creation
   utilities, search caching and world-level location facts. They must not share mandatory gameplay prerequisites; both may target the same pyramid.

## Provisional (explicitly not decided)
- **A4 mechanics.** Not assumed: map granted on contract acceptance; map granted on contract completion; map bought with contract rewards; map embedded
  in the contract item. Each is evaluated separately in `BOUNTY_MAP_INTEGRATION.md`. A "contract carries map-equivalent information" variant (A4-4) is a
  technical alternative only and does not replace the approved vanilla-style explorer-map requirement.
- **B1 mechanics.** Profession, level, price, stock limits, refresh behavior, search radius, map icon, whether a map may be sold repeatedly, whether a map
  points at an *unclaimed* pyramid (see the `skip_existing_chunks` finding in `VANILLA_EXPLORER_MAP_AUDIT.md`).

## Relationship to earlier records
Supersedes, for the *format* question only, P8-D1 decisions D2/D3 of `OWNER_DECISIONS_REQUIRED.md` (information format and Channel A lead): the owner chose an
explorer map for both channels. P8-D1's other open items (precision, seller, caps, "prefer unexplored", visit definition, failure behavior) remain open.
Vanilla overlap permission (`AUTHORITY-AMENDMENT-VANILLA-OVERLAP-PERMITTED.md`), Policy D and the placement parameters are unchanged.
