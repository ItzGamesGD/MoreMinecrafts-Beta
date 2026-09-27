# Stage 7 Fishing vertical slice

Stage 7 intercepts `FishingHook.retrieve(ItemStack)` only when vanilla has set the hook's `biting` state. Casting, hook simulation, water checks, waiting, Lure, and non-catch reel behavior remain vanilla. The original retrieve call reports one durability point while the custom challenge owns catch resolution; ordinary treasure/junk is replaced only for this valid custom-fish catch path.

The server sends its authoritative level game-time start tick for the client HUD, while the normal Fishing Rod use confirms on the server. The retained vanilla hook is discarded on resolution or cancellation. The server records a snapshot of Reel, Crankbait, Minnow, measured depth, and Luck input. The fixed 20-tick half-cycle produces a deterministic ping-pong marker, the inclusive center window is 0.42–0.58, and 160 ticks is the timeout. A subsequent rod-use is intent only: the server derives marker position from its tick and never accepts position, outcome, species, grade, or length from the client.

Reel clamps measured water-depth tier and never changes weights. Minnow removes species whose hard requirement is not met. Only after those filters, Crankbait biases the five fish-grade weights. Species and grade selection plus centimeter length generation use server RNG. Quality is reproduced from the species factor, grade, and normalized length.

The dedicated one-row server menu validates the three rod slots and locks the source rod while open. Its container immediately writes changes back to the exact rod stack. The component has both persistent and network codecs. A rod's three real equipment stacks are stored in the persistent `rod_equipment` data component. Fish use item identity for species and persistent `fish_grade` / `fish_length` components. Equipment uses vanilla damage fields; the isolated wear rule is one point per started valid catch.


## Post-Stage-7 design reconciliation — 2026-09-26

Stage 7 remains the implementation baseline, but the following later user-approved design decisions supersede parts of the original vertical slice and must be treated as future repair/reconciliation work:

- The module UI control is **left click**, not Shift+Right Click.
- Reel = access possibility/discovery reach; Minnow = capture permission; Crankbait = grade-distribution bias only.
- Reel/Minnow/Crankbait each have grades I–V as higher-grade results within the same equipment family, not enchantment-style internal levels. Higher Reel/Minnow grades include all lower-grade capability.
- Biome/water ecology belongs in species definitions, but biome filtering stays temporarily disabled for broad live testing until the species roster is finalized.
- Fish remain ItemStacks only; no fish entities are planned.
- Per-fish data expands to species + grade + length + weight. Length is displayed in cm with decimals; weight is an additional generated variable.
- Crankbait may reduce lower-grade weights strongly enough that the lowest grades disappear from the active grade distribution. Exact grade weights remain a balance decision.
- Module durability no longer follows the original one-wear-for-every-started-valid-catch rule:
  - Reel consumes durability when the cast/attempt begins.
  - Minnow and Crankbait consume durability only when an actually permitted capture succeeds.
  - On minigame failure, the fish is lost and only Reel wear remains.
  - Modules never vanish from durability exhaustion; durability 1 means broken/inactive until repaired.
  - Repair direction is Anvil-based.
  - Reel maximum durability should be higher than Minnow/Crankbait; exact I–V values remain open.
- The minigame uses green/yellow/orange/red qualitative zones. Green is 100% capture, yellow is already substantially reduced, orange is lower again, and red fails. Exact probabilities and geometry await UI/live balancing.
- Minigame difficulty is species-driven only. Grade, length, weight and all three modules do not alter difficulty or mutate fish quality.
- Add a portable fish-only container item (working-name family: `어망`; final name TBD). Each fish occupies one internal slot, never stacks, and preserves individual species/grade/length/weight data. The container itself occupies one inventory slot and retains contents like a Shulker Box.
- Fish tooltip/display order is fish name first, then grade, length and weight.
- Fish cooking remains allowed.
- Fisherman integration should preserve vanilla trades and add a separate fishing-trade area capable of reading the fish container, valuing its contents through the later value formula, removing sold fish, and paying the result. Emerald is only the provisional payout baseline.
- A separate rod/frame capacity mechanic tied to fish weight is under design review. Candidate direction: the fishing rod becomes the frame/carrier whose grade controls maximum supported fish weight while Reel/Crankbait/Minnow remain mounted modules.
- Exact species roster, ecology, grade distribution, length/weight generation, value formula, rod/frame decision, module durability values, recipes and final visual assets remain subsequent design steps.


## Fishing design finalization reconciliation — 2026-09-26

This reconciliation supersedes the earlier Stage 7 provisional assumptions where they conflict.

- Equipment progression is Tier I–III only for Reel / Minnow / Crankbait. Fish Grade remains I–V and is unrelated to equipment Tier.
- Fish instances persist Species / Grade / Length / Weight as independent variables. Each species owns separate Length and Weight distributions.
- Reel affects Species access only; Minnow affects capture permission only; Crankbait affects Fish Grade distribution only.
- No Rod Frame/weight-capacity system. Fishing Rod is only the three-module carrier.
- Crankbait Grade distributions (I→V): none 45/30/16/7/2; Tier I 30/32/22/12/4; Tier II 10/25/32/24/9; Tier III 0/8/34/38/20.
- Value baseline: Species Base Value × Grade Multiplier × (1 + Length Bonus + Weight Bonus), with Grade multipliers 1.00/1.15/1.40/1.80/2.50 and Length/Weight bonuses based on percentiles in that species' own distributions (baseline each percentile ×0.75).
- Minigame zones remain light-green/yellow/orange/red. Live-test probability baseline is 100/45/15/0. Only Species controls difficulty, via deterministic marker speed.
- Resolution order becomes: custom-fishing/ecology eligibility → Reel-filtered Species roll → minigame → Minnow validation → Grade roll → Length roll → Weight roll → fish item.
- Reel wears on valid custom-fishing attempt start. Minnow/Crankbait wear only on successful permitted capture. Durability 1 means broken/inactive but persistent and Anvil-repairable.
- UI input changes from the Stage 7 Shift+Right Click implementation to left-click air while holding the rod. Right click remains vanilla casting/retrieval.
- Portable fish-only 어망 is Tier I–III with 9/18/27 slots; upgrades preserve contents. Fish remain non-stackable individual items.
- Fisherman keeps vanilla trades and gains a separate fish-sale path with individual/bulk sale and preview; Emerald conversion remains balance work.
- Per-species hard depth filtering is removed. Custom fishing activation instead requires a sufficiently large local water body, approximately 2×2 chunks with about 8 blocks/meters depth, implemented tolerantly for natural terrain. Species ecology then uses biome/water environment + Reel + encounter weights.
- Resource Stage D now explicitly implements/reconciles these code/data/UI changes before final fish/module/어망/HUD resource art.
