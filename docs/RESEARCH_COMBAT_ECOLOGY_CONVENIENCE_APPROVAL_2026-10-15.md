# Research combat, ecology, and convenience — user approval (2026-10-15)

Status: **USER-APPROVED DESIGN**. This record approves mechanics and balance below, but does not claim implementation, live validation, or merge. It is additive to the Stage 22 research topology and current research authority. Do not overwrite existing progression, cost ledgers, or unrelated content.

## T3 Counter-Shield vs T2 Shield Counter Preparation

- **T3 Counter-Shield** is a craftable shield that preserves vanilla shield blocking and automatically knocks back the attacker after a successful block. The pre-existing delegated effect proposal is: eligible attacker within 3 blocks, approximately 2 blocks knockback, 2-second cooldown. These exact numbers are *existing proposals*, not independently reaffirmed in this turn.
- **T2 Shield Counter Preparation** is a distinct purchased research effect: after a successful timed shield defense, the first melee attack within 1.5 seconds deals +20% damage, with a 5-second reactivation cooldown. It is not automatic knockback.
- User selects **iron + obsidian** as the Counter-Shield recipe ingredient family. Exact quantities and 3x3 shaped layout are **not yet specified**; do not invent an approved recipe or silently reuse the T2 research purchase price.

## Approved detailed research rules

- **Ballistic Control:** While aiming with a bow or crossbow, show a small translucent marker at the predicted first projectile collision. Prediction must follow the actual projectile trajectory and collision semantics; this does not auto-aim, change gravity, or change projectile speed.
- **Bow Charging Mobility:** Reduce the existing bow-draw movement-speed penalty by **50%**; no crossbow extension is implied.
- **Falling Axe Smash:** On direct axe hit while falling from at least **2 blocks**, add **25% damage**; **3-second cooldown**. Existing critical-hit and fall-damage interactions require compatibility testing; do not add unapproved AoE or fall-damage immunity.
- **Weapon Swap Handling:** Preserve ordinary attack-charge progress on switching weapons, without granting instant full charge; calculate against the newly equipped weapon's attack-speed constraints and prevent swap-spam bypass. No arbitrary new swap animation or extra damage is authorized.
- **Mob Aggro Redirection:** Preserve the earlier approved rule: **30%** redirect chance at initial hostile target acquisition against a player, nearest eligible hostile within **12 blocks**, **200-tick** retry cooldown. Exclude friendly mobs, villagers and animals. Respect hostile custom mobs where safely classifiable.
- **Hopper Filtering:** Keep the approved hopper-panel UI with allow-list, filter slots and NBT-ignore toggle. Only registered matching items may transfer when enabled; evaluate eligibility server-side before item movement, preserving the stack on denied transfers. Exact filter storage, comparator behavior, permissions, cross-mod matching and unload/restart safety require verification; do not invent additional player-facing controls.

## Validation gates

- Confirm purchase-gating and interaction with enchantments, existing Counter-Shield knockback, vanilla combat, multiplayer ownership and custom mobs.
- Test projectile prediction against actual hits; shield timed-block and cooldown; axe crit stacking and fall damage; weapon swapping across different attack speeds; hopper full destinations, simultaneous access, unload/reload and NBT/component matching.
- All effect-level live acceptance remains **PENDING** until independently verified. This approval does not authorize merging PR #88/#89 or modifying `main`.
