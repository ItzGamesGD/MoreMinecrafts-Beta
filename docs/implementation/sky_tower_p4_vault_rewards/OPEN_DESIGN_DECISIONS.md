# SKY TOWER P4 — OPEN DESIGN DECISIONS
## Precise Owner Decision Requests for Human Authority

**Repository:** `ItzGamesGD/MoreMinecrafts-Beta`  
**Base Commit:** `5013989dfd4a9575ace5dccf922d28dea9d52150` (`feature/sky-tower-integration-02`)  
**Implementation Branch:** `implementation/sky-tower-p4-vault-rewards`  
**Date:** 2026-11-10  
**Current Gate Status:** **PARTIAL** (Awaiting Owner Decision)

---

In accordance with `AGENTS.md` ("WHAT = Human, HOW = Agent"), coding agents are strictly prohibited from inventing gameplay rules, container types, balance percentages, or structural block coordinates. The following decisions must be made by the human project owner before the rooftop special vault can be placed and P4 marked COMPLETE.

---

### Decision 1: Rooftop Sanctuary Special Reward Container & Location
- **Affected Requirement:** `REQ-VAULT-04` (from `docs/audits/sky_tower_system_completeness/OWNER_DECISIONS_REQUIRED.md` Decision 2)
- **Question for Owner:**  
  What type of container should house the rooftop special reward, and where precisely in the rooftop sanctuary should it be placed?

#### Context
1. `SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` §158/§161 and `SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md` Q08/Q24 mandate a pinnacle special reward on the rooftop with doubled unique drop rates. The loot table `moreminecrafts:chests/sky_islands/vault_tower_top.json` exists in data assets.
2. In the approved candidate NBT (`full_01.nbt`, commit `16ad9d5`), the rooftop sanctuary (Y=64..79) contains the 12 arches and the decorative floating relic, but **zero vault or chest blocks** exist.
3. Section 9 of the P4 directive strictly prohibits modifying Sky Tower NBT geometry without explicit authorization.

#### Non-Binding Engineering Options

- **Option A (South Perimeter Ominous Vault — Recommended):**  
  Place an `ominous_vault` on the south perimeter altar pedestal at local coordinates `(24, 65, 10)` relative to the tower origin, configured with `loot_table: "moreminecrafts:chests/sky_islands/vault_tower_top"` and `key_item: "minecraft:ominous_trial_key"`.  
  *Pros:* Matches Q24 ("외곽 성역 제단"); creates high-stakes climax requiring players to bring an Ominous Key.  
  *Cons:* Players without an Ominous Key cannot loot the pinnacle reward immediately.

- **Option B (South Perimeter Standard Vault):**  
  Place a standard `minecraft:vault` on the south perimeter altar pedestal at local coordinates `(24, 65, 10)` configured with `loot_table: "moreminecrafts:chests/sky_islands/vault_tower_top"` and requiring a standard `minecraft:trial_key`.  
  *Pros:* Guaranteed payout for any player who reaches the rooftop with an ordinary trial key.  
  *Cons:* Less distinct visual climax from lower-floor vaults.

- **Option C (Relic Base Sanctuary Chest):**  
  Place a decorated stone chest at the base of the central floating relic `(24, 65, 24)` that unlocks when `allFloorsCleared()` is true.  
  *Pros:* Dramatic visual centerpiece; no key required.  
  *Cons:* Conflicts with Q38 (which established the floating relic was purely decorative) and replaces vanilla vault mechanics with a custom container.

---

### Decision 2: Implementation Mechanism for Rooftop Placement
- **Question for Owner:**  
  Once the container type and position are chosen, should the block be added directly into `full_01.nbt` (requiring regenerating composite templates and updating `SkyTowerLayout.SHA256`), OR placed programmatically at runtime during `SkyTowerWorldgenRegistration`?

#### Context
- Modifying `full_01.nbt` modifies the frozen SHA `16ad9d5`, requiring an updated baseline across all 6 biome composite templates.
- Programmatic placement in `SkyTowerWorldgenRegistration` keeps `full_01.nbt` completely frozen, but requires a small block placement step during tower registration.

---

### Decision 3: Star Covenant Delivery Ratification
- **Affected Requirement:** D13-03-Q11/Q12 (`docs/planning/D13_03_DECISIONS_Q01-Q47.md`)
- **Question for Owner:**  
  Should the personal 10% Star Covenant draw be triggered automatically when a player unlocks the rooftop special vault?

#### Context
- D13-03-Q11: *"별의 서약 최상층 개인 금고에서 플레이어별 10%; 깃털 15%·돌풍 파편 5% 보존"* (Star Covenant 10% per player from top-floor personal vault; feather 15%, gale shard 5% preserved).
- `StarCovenantVaultRewards.java` is implemented and wired into `SkyTowerVaultRewards.onVaultRewarded()`.
- Confirmation from owner will ratify this integration into the final merged baseline.
