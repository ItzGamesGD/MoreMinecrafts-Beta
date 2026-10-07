# 2026-10-07 design and implementation follow-up

This follow-up pairs with [`CONTENT_FINALIZATION_CONTINUATION_2026-10-07.md`](CONTENT_FINALIZATION_CONTINUATION_2026-10-07.md), copied from the current `main` documentation line for this worktree. The latter records the design direction; this document records bounded implementation choices, provisional balance values, and remaining validation.

The repository is split: the implementation branch is based on Stage 20 / PR #69, while `origin/main` contains newer documentation commits. The copied continuation is the latest Oct 7 design record found on `origin/main`. The older `CONTENT_AUTHORITY_INDEX.md` and `CURRENT_DESIGN_FINALIZATION_CHECKLIST.md` remain useful navigation/worklist files, but their relevant open items are superseded by this follow-up and must not be read as the latest user decision.

## T2 core identity and acquisition

Five non-stackable core items are registered without recipes or additional material costs:

| Core | Acquisition | Provisional chance | Result per successful roll |
|---|---|---:|---:|
| Eye of the Typhoon | Normal Trial Chamber reward table | 2% | 1 |
| Heart of Thunder | Ominous Trial Chamber reward table | 3% | 1 |
| Burning Heart | Bastion Remnant treasure table | 4% | 1 |
| Heart of Currents | Final Elder Guardian killed in an Ocean Monument | Guaranteed once per monument | 1 |
| Ancient Heart | Ancient City chest table | 3% | 1 |

The normal/ominous split gives the wind and lightning cores distinct Trial Chamber routes, with the lightning core tied to the more dangerous reward pool. Minecraft 26.3 exposes normal and ominous reward tables but no native “Trial level 5” state. These tables are the narrow vanilla-compatible implementation of the locked source; if a concrete level-five system is introduced later, its reward event should replace this table mapping. No separate leveling system is added here.

Each generated chest gets its own chance when its loot is first generated. The generated contents are saved by vanilla, so reopening a chest or leaving and re-entering the world does not reroll it. Each Trial Vault continues to use vanilla per-player reward and key rules. There is no world-wide chest quota: a separate naturally generated chest is a separate roll.

The monument reward runs after an Elder Guardian dies inside an Ocean Monument structure. It requires the structure's chunks to be loaded and no other living Elder Guardian in the structure bounds. A saved world claim keyed by the structure start chunk prevents another reward after reconnect, restart, or a manually respawned guardian. The single item drops at the final guardian's position, so normal item pickup rules apply. This only awards a core; it does not create a recipe or equipment link.

All four chance values are **provisional balance constants**, not locked design. Automated validation covers the saved claim, but natural Trial Vault chest generation and monument completion still need in-game validation.

## Multi Catch contract

Multi Catch is one Fishing Rod skill-tree node, available after Lure III for a provisional 1,000 XP. Without the node, a successful custom catch awards one fish as usual. With it unlocked, the existing bite selects the first fish and uses that species for the minigame's difficulty and HUD; success resolves a second separate fish result with a new weighted species roll from the same Reel-accessible pool. Both permitted results independently roll Crankbait grade, species length, and length-dependent weight and receive new non-stackable Fish ItemStacks.

Minnow permission is checked separately for each result. Reel durability is consumed once when a valid custom attempt starts. A failed minigame, timeout, disconnect, death, hook removal, dimension change, or rod leaving both hands awards no fish and consumes no Minnow or Crankbait durability. Each permitted fish consumes one charge from Minnow and Crankbait. If the first fish breaks a module at its persistent broken-one state, the second result is denied by that now-inactive module. A successful challenge can therefore yield zero, one, or two fish.

Vanilla casting/retrieval remains the fallback when custom fishing is not eligible. JUnit coverage checks independent complete roll records, the lifecycle gate, per-catch durability boundaries, and value calculations; GameTests exercise exhausted modules. Full hook lifecycle and vanilla retrieval remain LIVE PENDING.

## Fisherman sale extension

Custom fish are the sale commodity because species, grade, length, and weight already feed the implemented `FishValue` model. Custom gear is not sold: the module upgrade/crafting costs have not been closed, and buying modules from villagers would skip that progression. Existing Fisherman trades remain unchanged.

The small custom path is Shift-right-click a Fisherman while holding one custom fish. Ordinary interaction still opens the vanilla trade screen. One fish is consumed per sale, the payment is paid to that player's inventory or dropped if full, and the server reads the fish's actual components. The fish tooltip explains the input. The custom sale has no stock or restock cycle: it repeats once per offered fish. Vanilla offers retain their own uses, restock, and save behavior. Multiplayer sales are independent and server-authoritative; no shared menu or per-player sale state is introduced.

**Provisional price rule:** `floor(FishValue / 100)` Emeralds, bounded to 1–8 Emeralds per fish. This keeps the existing value hierarchy while compressing the index into a readable transaction. The divisor and cap are balance values pending a live economy pass. No separate currency, shop, or custom trade catalogue is introduced.

## Activity rewards

Five vanilla-activity milestones are implemented as ordinary advancements under one MoreMinecrafts tab:

| Milestone | Trigger | XP |
|---|---|---:|
| First diamond | Acquire a diamond | 10 |
| First combat milestone | Kill a vanilla Zombie | 10 |
| Enter the Nether | Change into the Nether | 15 |
| Craft an Enchanting Table | Craft its vanilla recipe | 15 |
| Catch a custom fish | Obtain any item in the custom fish tag | 10 |

These are one-time per-player advancement rewards, stored by vanilla advancement progress. They cannot be repeated by farming the same ore, mob, or fish. XP remains ordinary Minecraft XP and feeds existing XP progression. The total for this first set is capped at 60 XP per player. There are no daily resets, rerolls, item rewards, shop currency, or large quest catalogue. Expansion of the milestone list should wait for play validation.

## PR and code-audit status

### PR #69 — 40 vanilla-model mobs

The baseline head was `a3302ad7a13c25e32ffe794f692ba1b778815978` on base `25eede9fcec899f9f827b866853d2c7e37336da7`; current read-only PR data still reports CI run #337 successful, with open review threads. The local audit fixes the Peaceful metadata gap for aggressive Feral Wolf and Cave Bat while retaining their vanilla Creature/Ambient categories, and untamed Feral Wolves now use a hostile-distance despawn cutoff while tamed ones persist. Current code also uses exact EntityType identity for stat overrides, single-source EntityType sizing, the Pillager loot table for Bandit, vanilla Wild Piglin fire vulnerability, the real beehive-inhabitors tag, post-split slime/cube stats, full spider poison effects, wolf taming, and biome/position/structure gates. Runtime GameTests cover these contracts, including the equipped armor total.

The local follow-up has not been pushed, and no GitHub review state was changed. The clean build and all 30 required GameTests passed for the current local changes. No custom mob textures, models, animations, abilities, or VFX were added.

### PR #65 — lower equipment repairs

PR #65 was created against Stage 18 and is stale for this Stage 20-based worktree. Applicable repairs were ported selectively: Greatsword sweeps intercept `Player.interactOn` before entity-specific interaction; Minecraft 26.3's server packet handler passes the target, hand, and hit location through this player method. An `ArmorStand` GameTest verifies the sweep happens before equipment dispatch while retaining current damage, enchantment, and durability. Drill fuel-menu access follows the exact source item and filters invalid placement/merge; Injector slots use the filtered menu; Portable Furnace evaluates vanilla fuel providers with a furnace loot context, enforces the container-wide slot limit, and rejects recipe outputs larger than that limit before consuming input or fuel. Regression GameTests cover Greatsword interception, Drill ownership, fuel resolution, output bounds, and the slot cap.

The remaining PR #65 changes were not transplanted wholesale. In particular, its branch also changes unrelated source files and its Greatsword sweep rewrite drops the current enhancement/enchantment/durability path. No merge or PR update was performed.

## Resource state

The 40 unique custom-mob texture paths remain pending. This work adds vanilla-model item icons for the five new core identities as clearly temporary references; it does not alter any approved master PNG. No generated artwork is recorded as production art. The mob resource backlog still needs the base-family UV sheets and in-game renderer review before production textures can be accepted. All new and changed visuals remain LIVE PENDING in an interactive client.

## Not started / deferred by current scope

- No special recipes or T2 equipment ingredient links; those wait for the content-demand pass.
- No T3 growth replacement, Lazer Arrow work, Mire Shaman or Shard Golem reward, End-boss reward, independent research axis, or independent economy axis.
- World, biome, and structure expansion remains downstream of content-axis review.
- Live Minecraft validation is not claimed by JUnit, GameTests, or CI.
