# ST-01 P8-D2 — Channel A4 feasibility: obtaining a vanilla-style ST-01 explorer map through bounty contracts

Feasibility assessment only. No acquisition mechanism is selected (owner decision). All code references are to the P8-C2-C head
(`src/main/java/dev/itzgamesgd/moreminecrafts/bounty/`).

## 1. What the contract lifecycle offers (verified by reading the code)
- **Acquire:** a trade/loot *placeholder* becomes a real contract on the player's tick (`ContractMaterializer.materialize`, :53-66), fixed at that moment.
- **Accept:** `ContractService.act(ACCEPT)` (:32-36) is a pure item edit `OFFERED → ACCEPTED`; **no ledger, receipt or transaction** is involved.
- **Complete:** `act(COMPLETE)` → `ContractTransactions.redeem` (:65-118): world-ledger PREPARE (flushed), consume delivery items and the contract, insert
  `emeralds` + one bonus item (`new ItemStack(bonus, count)`), record a per-player **receipt** (carries overflow stacks as `ItemStack`s), save the player,
  drop overflow, COMMIT. Crash points are covered by tests (`ContractTransactions.fault` seams).
- **Reward model:** `ContractData.Reward(emeralds, item id, count)` and `ContractRewards` pools of **plain item ids** (coal … adamantium). A reward cannot
  carry per-instance data, so a *map* (needs a `map_id`, saved map data, a target) is not expressible as a current reward.
- **Identity/duplication:** `ContractLedger` blocks every copy of a contract id (PENDING/COMMITTED); abandonment destroys only that stack.
- **Death/transfer:** progress lives on the item; receipts are `copyOnDeath`.

## 2. Vanilla facility usable for map creation outside villager trading (measured)
`ExplorationMapFunction` is a loot function. P8-D2 applied it to a plain `minecraft:map` with a **chest** loot context carrying only `ORIGIN`
(`VANILLA_EXPLORER_MAP_AUDIT.md`, section "Outside trading"); it executes without a villager. So a contract-side map can be made by the same vanilla code,
either through a loot table (data-driven) or through `ExplorationMapFunction.makeExplorationMap(...)` (API), with no new rendering system. It needs
`ORIGIN` (any position), runs `findNearestMapStructure` **synchronously**, and returns an unchanged stack (no `map_id`) when no target is found.

## 3. Options
Common criteria first, then the table. "Cost" = the synchronous locate (see `STRUCTURE_SEARCH_BENCHMARK.md`).

| Criterion | A4-1 map on **acceptance** | A4-2 map on **completion of a separate qualifying contract** | A4-3 contract-specific **exchange / reward mechanism** | A4-4 contract **carries map-equivalent information** (technical alternative only) |
|---|---|---|---|---|
| Where it hooks | `ContractService.act(ACCEPT)` (:32) | `ContractTransactions.redeem` insert step (:94-95) | new `Action` (enum :17), network payload `ContractActionPayload`, UI | `ContractData.Objective` extension |
| Existing code reuse | low: acceptance has no transaction | high: receipts/overflow/ledger already handle durable reward delivery of `ItemStack`s | low: new action + payload + screen | medium: objective codec (`optionalFieldOf`) |
| Contract identity | map has no link to the contract id unless one is added | contract id is consumed with the reward, the map can be tied to the redeemed id in the receipt | explicit | native |
| Duplicate issuance | **risk**: nothing stops accept→abandon→(another copy)…; needs a per-contract "map issued" flag stored on the item and checked atomically | **covered** by the ledger (one redemption per id); the map creation must happen once inside the prepared transaction (tx id) | needs its own idempotence | n/a |
| Death/transfer | map is a normal item and may be lost; the contract stays ACCEPTED so a replacement cannot be justified without extra state | reward delivered once; later loss is the player's | depends | information stays on the contract |
| Multiplayer | each map is separate; two players may receive the same target | same | same | same |
| Restart persistence | `map_id` + saved map data persist (vanilla) | same + receipts | same | trivial |
| Map-generation timing | on acceptance (player present, UI open) → server-thread spike at click time | on completion (spike inside a durability-sensitive transaction; a slow/failed locate must not break the PREPARE/COMMIT protocol) | at click time | when the contract is generated/accepted |
| Server performance | cold-search spike (benchmarks) | same, **inside** the redeem path | same | same, or none if cached |
| Fits "approved vanilla-style map" | yes | yes | yes | **no** — not a map; only if the owner relaxes the requirement |
| Main risk | no durable atomicity; loss of the map strands the player | a locate failure at redemption (no target) must fall back (e.g. emeralds only) without double-paying | most new code | contradicts the approved format |

## 4. Technical constraints common to every map option
1. **No position at materialization** (P8-D1 F-5): contracts materialize without level/position, so any map must be made at acceptance, completion or exchange — where the
   player and level are known.
2. **Synchronous locate** unless the non-blocking design of `NONBLOCKING_SEARCH_OPTIONS.md` is adopted; either way it must not run inside the ledger PREPARE→COMMIT window
   unmanaged (a stall holds the PENDING entry and the player's receipt).
3. **Supply:** with the vanilla default `skip_existing_chunks=true` each pyramid can be issued once, so a *contract-granted* map and a *villager* map would **compete for the
   same finite targets** (a coupling that is not a prerequisite but is a shared resource); with `false` neither consumes the other (VANILLA_EXPLORER_MAP_AUDIT.md §5).
4. **No-target case:** the function returns a map without `map_id`; the contract path must decide: refuse acceptance, grant nothing and keep the contract valid, or pay an
   alternative (owner decision, P8-D1 D8).
5. **Independence:** none of the options needs a villager purchase; a villager map never needs a contract. They share only the location utilities/tag.

## 5. Assessment
Feasible with existing vanilla machinery for A4-1, A4-2, A4-3; **A4-2 reuses the most existing durability code** but needs a new reward kind and a safe locate step; A4-1 is the
simplest but has the weakest duplicate-issuance guarantee; A4-3 is the most new code; A4-4 is feasible but is not the approved format. No selection is made. Status: **PARTIAL**
(feasibility reasoned from code and the vanilla-function probe; no contract-side prototype was built or tested).
