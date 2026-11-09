# Territory owner decisions — 2026-10-20

Status: OWNER-APPROVED DESIGN RECORD. Documentation only; not implemented or live-verified.
Scope: MoreMinecrafts Fabric territory, commerce, guards, artifacts, roads. This addendum supplements the D1/D2-A authority documents; it does not silently resolve SOURCE MISSING historical evidence.
Source of authority: direct user decisions in ChatGPT conversation. In conflicts with prior proposals, these approvals take precedence for the listed scope.

## Explicit withdrawal (important)
D67–D78 are **WITHDRAWN**, not pending and not approved. Do not implement or retain requirements for per-player block edit history, chunk bitsets, persistent provenance, territory-release history inheritance, 15-minute backup, 16-backup retention, restore verification, administrator reapproval, or per-change journals. These were speculative overengineering. D66 still allows limited removal of clearly safe natural obstacles; if provenance is uncertain, preserve the block and reroute or fail. Use existing building/territory protection and conservative non-destructive placement; no new history subsystem.

## Approved decisions D01–D66
D01 One owner per territory; no co-owner/manager.
D02 One territory maximum per player.
D03a Manual registration requires liberation/challenge; exact challenge pending.
D03b Owner can voluntarily relinquish; no direct ownership transfer or inactivity repossession.
D04 Tier upgrades require resources AND room for every mandatory new facility; prevalidate all and apply atomically; failure consumes no resources and makes no partial placement.
D05a Protect entire managed building volume.
D05b Owner may directly edit managed buildings; preserve edits on upgrades.
D05c Protection covers explosions, pistons and liquids, with owner edits permitted.
D06 Cumulative T3 growth targets by size: Small +7 villagers / 4 guards / ~5 buildings; Medium +8 / 6 / ~6; Large +10 / 8 / ~7. Exact tier distribution pending.
D07 Existing research villager-trade discount applies only to explicitly allowlisted new transactions; unchanged formula.
D08a–c Mercenary contract 120,000 actual progressing server ticks only while employer online; logout and shutdown pause; persist remainder; sleep/time commands do not accelerate.
D09 T1 barter available to all 13 trading villager professions; SKUs/rates pending.
D10 T2 recycling only designated items with item-specific material returns.
D11 T2 food service sells prepared food and processes ingredients.
D12 T2 map service only allowlisted structures.
D13 T3 exclusive merchandise plus expansion of T1/T2 services.
D14-1 XP Beetle: protects part of XP lost on death.
D14-2 Fire Talisman: temporary shield triggered by fire damage.
D14-3 Nomad Anklet: speed increases during sustained long-distance travel.
D14-4 Lapis Necklace: chance to reflect incoming negative status effects to inflictor.
D14-5 Ancient Coin: chance of bonus rewards on designated trades.
D14-6 Undying Talisman: temporary damage mitigation in danger/low health, NOT totem resurrection.
D14-7 Sun Heart: solar-charged healing. Exact numbers/cooldowns pending.
D15 Ancient Coin bonus can stack with allowlisted research discounts; prevent duplicate processing/exploit loops.
D16 Enhancement catalysts from dungeon/boss rewards and limited T3 trades.
D17 Time Ring slows nearby hostile mob movement and attack speed, not world/server ticks.
D18 Guards and mercenaries share base combat stats; AI roles differ.
D19 Mercenary right-click Follow/Stay toggle, wolf-like; time still progresses while employer online and mercenary stays.
D20 Normal commissions: curated allowlisted objectives with gradual expansion. Earlier audited proposal (not necessarily separately ratified): 6 offers, 2/difficulty, refresh every 3 game days, 3 normal + 1 special active per player, direct player kill attribution after acceptance, preowned delivery, exploration dedup, 45/35/20 difficulty mix, objective counts 1 / 1–2 / 2–3, reward bands 2–4 / 5–9 / 10–18 emerald blocks. Verify proposal status before implementation.
D21 Special commissions individually authored and verified fixed missions.
D22 Dedicated territory/commission GUIs alongside physical world interactions. Registration remains physical item deposit by throwing items at Text Display + Interaction setup, not GUI-only. XP quest chapter visualization unresolved.
D23 Caravans carry regional specialties and rare items.
D24 Caravan slot category mix 70% regional specialties / 30% rare; item probabilities and stock pending.
D25 Guards are melee-focused patrol units.
D26 Guards attack qualifying hostile mobs threatening territory, owner, residents or guards; no indiscriminate farming.
D27 Patrol interior and adjacent boundary.
D28 Maximum chase 32 blocks outside nearest territory boundary, then disengage/return.
D29 Guards respawn automatically after delay, free.
D30 All guards patrol; no separate patrol/guard entity or assignment.
D31 Respawn after 12,000 progressing server ticks.
D32 Respawn time runs while server runs, owner online not required; server shutdown pauses; persist remainder.
D33 Respawn near actual guard-related building at safe location; fallback pending.
D34 Guards protect only owner, villagers, guards; not visiting players.
D35 Auto-generate patrol routes around buildings and roads.
D36 Recalculate routes after building/road changes via batched/debounced update; exact timing pending.
D37 Patrol day and night.
D38 Nearby guards assist guard engaged with qualifying hostile mob; distant guards continue patrol.
D39 Assist range 24 blocks from engaged guard.
D40 Up to 3 additional assistants, 4 total guards per encounter.
D41 Regenerate health only out of combat.
D42 Regenerate 1% maximum health per 5 seconds (100 ticks); this supersedes erroneous 2-second draft.
D43 Regeneration starts 20 seconds (400 ticks) after combat; new damage/combat resets delay.
D44 No guard health bar/display.
D45 Guard equipment improves automatically by territory tier; same base combat stats as mercenaries.
D46 T1 chainmail armor + iron weapon; T2 iron armor + iron weapon; T3 diamond armor + diamond weapon.
D47 Swords only; iron sword T1/T2, diamond sword T3.
D48 Guards drop no gear or loot.
D49 Vanilla-style physical entity collision/pushing.
D50 Owner hitting guard damages it but guard does not retaliate.
D51 Other player hitting guard triggers retaliation from that guard alone.
D52 Retaliation hostility ends 30 seconds (600 ticks) after last attack, respecting D28.
D53 Player-killed guard follows ordinary free delayed respawn; no drops.
D54 Relinquishment immediately removes guards and clears respawn timers, no drops.
D55 Relinquishment preserves buildings and edits in world, removes protection.
D56 Relinquishment deactivates territory-specific commerce, services, upgrades and management; ordinary vanilla blocks work.
D57 Re-registration starts at T0; still requires liberation/challenge and manual physical registration.
D58 Old buildings remain ordinary world structures, not auto-recognized as official facilities.
D59 New mandatory facilities auto-search alternative sites avoiding old structures; require whole layout prevalidation, otherwise D04 atomic rejection.
D60 Among valid placement candidates prioritize connectivity to existing roads/buildings.
D61 Preserve existing roads and player edits; add only necessary connecting roads.
D62 Same new-road material palette for T1–T3.
D63 New-road palette mixed dirt path and gravel.
D64 New-road default width 3 blocks.
D65 Automatically build ramps/stairs for manageable elevation differences; exclude unsafe/too-steep sites.
D66 Clear only a conservative allowlist of safe natural obstacles for new roads; protect existing structures and owner edits. No uncertain-provenance block removal.

## Open owner decisions (not approved by this document)
Exact liberation challenge; registration materials and limits from missing Paper source; T1–T3 building and resident distribution; trade SKUs and prices; recycling allowlist/refunds; map allowlist; T3 exclusives; fixed special commissions; caravan stock and odds; artifact probabilities and cooldowns; guard spawn fallback; road routing specifics; unresolved 24-rune prices and Sands of Time source. Do not invent approvals.

## Implementation and QA boundaries
No gameplay code authorized by this documentation PR. Keep Draft, do not merge, do not change main or other active branches. For later implementation, rely on established protection and non-destructive preflight validation. Do not silently reintroduce withdrawn D67–D78 as technical prerequisites. Treat proposed numeric defaults as pending unless specifically approved above.
