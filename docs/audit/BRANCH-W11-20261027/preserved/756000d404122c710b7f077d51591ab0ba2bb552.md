# MoreMinecrafts — 세계 이벤트: 약탈자 순찰대 5종 상세 설계 (2026-10-18)

Status: **DESIGN FINALIZED (user-approved direction and current balancing baseline); DOCUMENTATION ONLY / NOT IMPLEMENTED**. This document closes the pillager-patrol variant design, not the rare merchant caravan, meteor visual feasibility, or all world events. No code, assets, loot tables, or game rules were changed.

## 0. Boundaries and authority

- World event occurrence policy: **hybrid** (natural and condition-triggered). Patrols are **vanilla-adapter events**, not independently timed events. Rare merchant caravan is separately managed and may be only slightly more frequent than ordinary wandering traders; not scoped here.
- **NEVER create a new 60-second patrol spawn cycle.** The 60-second WorldEventCoordinator evaluation discussed earlier is only for independently scheduled events; it cannot create patrols. Preserve the precise Minecraft **26.3** vanilla patrol initiation timing, spawn chance, spawn gamerule, game-day/biome/location checks and failure behavior. Verify mapped Minecraft sources before implementation; older-version reference numbers are background, not a version-locked contract.
- Select one of five variants **only once an existing vanilla patrol spawn opportunity succeeds**; preserve vanilla frequency, not multiply rolls per nearby player. Implementation must not create a second patrol alongside the vanilla one or independently re-roll on chunk reload.
- **Normal** variant is unmodified vanilla patrol, including its vanilla captain; special captain below applies to **four custom variants** only. This resolves the conflict between the commitment to preserve a true vanilla baseline and the new custom-captain direction.
- The user explicitly chose **elite escort progress gating**, **mixed vanilla + existing-mod-item rewards**, and finalized **custom captain = Vindicator-based, Pillager movement speed, axe + shield**, with the **Ravager** as temporary elite in elite escort. No Evoker or Totem-farming route.
- Specific numerical weights, extra loot quantities, retreat timings and Nether-entry milestone were offered as balancing baselines; this document freezes them as **current v1 design targets** for implementation and testing, subject to explicitly documented playtest adjustment. Do not misrepresent these as already verified or shipped.

## 1. Variant roster

| Variant | Conditional variant weight | NPC composition | Design identity |
| --- | ---: | --- | --- |
| NORMAL | 45% | **Exactly vanilla generated patrol**, including normal vanilla captain | Reference vanilla experience |
| SCOUT | 20% | **Custom captain 1 + pillager 1–2** | Mobile skirmish, limited retreats |
| HEAVY | 18% | **Custom captain 1 + pillager 2 + ordinary vindicator 1–2** | Melee pressure with ranged cover |
| SUPPLY | 12% | **Custom captain 1 + pillager 2–3 + chest-bearing donkey 1** | Escort, optional supply capture |
| ELITE_ESCORT | 5% | **Custom captain 1 + pillager 2 + ordinary vindicator 1 + Ravager 1 (temporary placeholder)** | High-danger escort encounter |

The 45/20/18/12/5 percentages are **conditional on one successful patrol spawn event**, NOT per-minute spawn rates. Do not force absolute NPC counts onto NORMAL; delegate to vanilla. Scout/Heavy/Supply/Elite compositions are fixed-range targets requiring a safe spawn footprint; if a special member cannot spawn, rollback special spawn group or downgrade to a valid lower-risk variant without bonus spawning.

### Custom captain (all non-NORMAL variants)

- **Base identity/AI:** Vindicator family melee attacker, **movement speed matched to Pillager vanilla base attribute** in the actual 26.3 registry/mappings, rather than a hardcoded guessed speed value.
- **Equipment:** Axe in main hand and Shield in offhand. Must visibly render equipment and **actually block** according to a bounded, testable custom shield-defense behavior; equipping an offhand shield alone does NOT guarantee a mob can block in vanilla.
- **Role:** Group leader/captain with identifiable appearance (banner/visual mark as technically appropriate), never an Evoker. Confirm how vanilla raid-omen bottle behavior attaches to variant captains in 26.3; retain the existing captain reward semantics **without duplicating rewards**, if possible.
- **Attack pattern:** Retain Vindicator's basic melee priorities; shield blocking only in bounded windows, with counterplay and no invulnerability loop. Exact block angle, frequency, duration and shield-disable interaction require gameplay testing and are deliberately **not falsely finalized**.
- Avoid spawning two leaders: vanilla captain should be replaced or transformed as part of the same vanilla spawn transaction; no extra captain spawned alongside it.
- The ordinary vindicators in HEAVY and ELITE_ESCORT are separate axe melee NPCs; **they are not shield captains**.

### Temporary elite: Ravager

- Use vanilla **Ravager (파괴수)** as the single elite-slot placeholder of ELITE_ESCORT until independent elite mobs are defined. Do not add an Evoker, other boss, unique token, exclusive drop or ad hoc elite AI.
- Ravager has large collision dimensions: validate the target spawn location, pathing, and clearance **before** committing the variant. Never spawn it inside terrain or buildings.
- No default rider or custom saddle requirement; these are not approved.
- Ravager is still gated behind progression to keep newly created worlds safer.

## 2. Progression and frequency

- Tentative concrete **v1 progress gate = candidate target player has visited the Nether at least once**. This was proposed as the explicit initial threshold, not yet independently tested. Gate evaluates the vanilla patrol **target player**; do not unlock adjacent new-player patrols merely because a distant multiplayer veteran progressed.
- If ELITE_ESCORT is ineligible, resample the remaining four variant weights **proportionally** (NORMAL 45/95, SCOUT 20/95, HEAVY 18/95, SUPPLY 12/95). **No extra vanilla spawn opportunities**.
- If the elite registry/config or Ravager spawn footprint is invalid, downgrade to HEAVY only if a valid patrol can still be formed; otherwise choose safe vanilla fallback. Fallback may not issue loot twice.
- Hard protection: existing vanilla patrolling game rule off or Peaceful/invalid spawn checks => no mod patrol variant. Do not circumvent vanilla spawn restrictions or gamerules.
- Changing variant probabilities via future config should affect **only variant mixture**, never the vanilla spawn chance.

## 3. Behaviors

| Variant | v1 AI target | Boundaries |
| --- | --- | --- |
| NORMAL | Vanilla patrol AI | No modification |
| SCOUT | Captain + ranged escorts approach and skirmish; escorts approx. +15% move speed | Retreat only when enemy too close, candidate retreat distance 6–10 blocks and maximum 3 sec per retreat burst; no infinite retreat loop, no forced chunk loading |
| HEAVY | Ordinary vindicators melee frontline, pillagers ranged support | Vanilla combat goals prioritized; no bespoke squad tactics or instant swarm teleport |
| SUPPLY | Pillagers escort chest donkey (preferred follow radius 12 blocks), fight when attacked | Combat has priority; donkey is a non-hostile animal, not a mobile endless loot generator |
| ELITE_ESCORT | Ravager's existing attack AI + same cover units | One Ravager max; no Evoker; no modification to global Ravager or raid behavior |

Numbers here are configuration-level balance baselines; final retreat navigation and captain shield behavior need real-client validation.

## 4. Reward policy (user selected vanilla + existing mod materials)

- Keep original entity XP and vanilla drops, avoid global loot changes. Add only **variant-level bounded** extras, tied to group/event identity; never roll group treasure once per member.
- NORMAL: unchanged.
- SCOUT: optional additional 2–4 arrows as a **group reward**.
- HEAVY: group reward 1–2 iron ingots plus a **small amount of an already registered low-tier MoreMinecrafts crafting material** (choose exact real registry ID after source review).
- SUPPLY: one chest-donkey inventory generated **once at patrol creation**: food 3–8, arrows 4–12, emeralds 0–2, plus 1–3 units of existing crafting/lifestyle material, respecting stack limits. The same stored inventory may be acquired by access or dropped on donkey death, **not both**.
- ELITE_ESCORT: existing Ravager vanilla drops and bounded iron/emerald group reward; do not introduce a unique elite drop, relic catalyst, or guaranteed rare accessory here. Future independent elite drop policy supersedes placeholder handling only after user approval.
- Captain's ominous-bottle/raid linkage should respect current vanilla 26.3 mechanics; no duplicate bottles per group and no accidental **Evoker/Totem** drops.
- **Do not invent mod-item registry IDs.** Inspect production registries and documented game progression first; if none are appropriate, keep the item pool unresolved instead of silently creating items.
- Inventories and claims must be server-authoritative and stable across chunk unload/reload, entity death, two-process server restart, and multiple players.

## 5. Exception and persistence contract

1. Existing vanilla successful patrol spawn is the **only** frequency trigger; never initialize custom independent patrol timers or spawn bonus patrols on reload.
2. Use a unique `patrol_group_id` and `patrol_variant` for special groups. Leader and member UUID ownership allows diagnostics, but do **not** introduce forced-loading global per-tick scans.
3. If custom captain, donkey or Ravager creation fails, cleanly fallback or abort the modification without phantom NPCs, duplicates, stolen vanilla spawns or duplicated reward state.
4. Partial group deaths: survivors continue vanilla-compatible combat; captain death does not respawn a leader, reward roll, or group.
5. Donkey release, player access and death must never clone inventory contents. Group supplementary loot claim is single-use, including across restarts.
6. Peaceful/difficulty, spawn gamerule, biome, time, terrain and target player gating follow **verified 26.3 vanilla behavior**; do not assume older implementation details.
7. Distinguish naturally generated special patrols from command-spawned mobs and raid-spawned Ravagers. The latter **must not** inherit special patrol rewards/AI.
8. Track performance metrics: actual patrol mix, special spawn failure/fallback, average mob count, server tick impact, and reward grants. No area chunk tickets for event management.

## 6. Acceptance gates for future implementation

- Resolve Minecraft 26.3 mapped vanilla sources; identify native patrol creation, player gate and captain/omen mechanics.
- Java 25 clean build, unit tests and GameTests for conditional variant weighting, progress gate, fallback, group extras and exactly-once reward issuance.
- Server smoke and two-process persistence for donkey stock and claimed loot; verify no repeat group spawn after reload.
- Real-client visual/combat checks: axe/shield captain (shield **functional**, not merely equipped), Pillager-equivalent movement speed, visual captain distinction, Ravager clearance, scout pathing and supply donkey capture.
- Multiplayer test with an unprogressed and progressed target player, nearby multiple players, spectator, Peaceful, and disabled patrol spawning.
- Final pass on actual acquisition economy before choosing existing mod crafting materials. Until then, do not guess.
- **This PR documents a design; no test passes or gameplay implementation are claimed.**

## 7. Adjacent ideas explicitly excluded from this document

- Rare merchant caravan: high-cost exclusive goods + rare materials/equipment, slightly more frequent than regular wandering trader as a **direction**, but exact spawn policy, NPCs, stock, prices and persistence still need separate design.
- Meteor: **conditional on high-quality rendered falling sphere**; no particle-only downgrade forced. User initially considered extremely rare night occurrence; actual probability unit and rendering proof pending.
- Monster migration: idea bank, potentially mobs fleeing ahead of elite/boss appearance; not a standalone migration event yet.
- Elite mobs: independent rare natural spawns plus biome-specific summon option; full roster/mechanics pending; Ravager is only **temporary patrol placeholder**.
- Abandoned supplies / mountain cabin / river bridge / small farmland+farmer / huts / shrines: move to ambient small world structure catalog, not WorldEventManager runtime events.
- Rare goods auction: parked for economy design. Ancient relic resonance original concept rejected; environment events 10–12 from previous ideation not adopted.
