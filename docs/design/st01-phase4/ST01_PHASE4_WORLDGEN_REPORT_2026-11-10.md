# ST-01 Pyramid — Phase 4: production worldgen integration (royal-hall fixture) — implementation report

Status: **PARTIAL** (see §9). Branch `claude/st01-pyramid-phase4-worldgen-roke2l`. Not merged; Draft PR only.

> **Scope decision recorded by the owner for this task.** The repository holds no complete ST-01 pyramid asset. The only structure NBT on any branch is
> `pyramid/royal_hall.nbt` (39×20×39, P8-A + D1). The 81×81×40 shell, entrances, L1–L3 chambers, connections and vault exist only as PROPOSED design
> coordinates. Owner instruction: implement the approved placement/eligibility/registration, use the royal hall **only as a temporary, explicitly
> identified integration fixture**, invent nothing, and report completed-pyramid generation, full-structure terrain adaptation, chamber continuity and
> visual acceptance as UNVERIFIED.

## 1. Recovery findings (read-only phase)

| Item | Finding |
|---|---|
| `origin/main` | `cde842dd` (2026-10-23). None of the ST-01 work is on `main`; PRs #249/#251/#252 are Drafts *targeting* `main` but their branches are 57–63 commits ahead of a merge base that is absent from this (shallow) clone. |
| Checked-out `HEAD` at start | `5c22f9fb` — a **Sky Tower** line, unrelated to ST-01. The designated branch was reset onto the P8-B2 baseline (the old tip is contained in `feature/sky-tower-integration-02`, nothing lost). |
| PR #249 / #251 / #252 (O-14 Phase 1-2 / 3 / 3.1) | **Diagnostic only**: `St01DesertWorldgenProbe`, `St01Phase3CalibrationProbe`, python analysers, probe workflow. No structure registration, no structure set, no structure piece. Their own owner-decision document states "production worldgen source was not modified". |
| Two divergent ST-01 lineages | (a) O-14 probes sit on P11-R (`6d7856b4`), which does **not** contain P8-A/B2; (b) the royal-hall NBT + royal-chest runtime (P8-A, D1, P8-B2, PR #254 `03685656`) does **not** contain O-14. Neither lineage contains the other. |
| Integration baseline chosen | `03685656` (P8-B2 head: latest `royal_hall.nbt` incl. D1, `St01RoyalChestPlacement`, `RoyalChest*`). From O-14 only the *definitions* (81×81 footprint, WORLD_SURFACE_WG, median foundation, cut/skirt, fluid volume) were taken; no diagnostic code was merged. |
| Pre-existing facts the brief's §3 relies on | P8 plan §7 lists spacing 32/separation 8 as UNAPPROVED and a different salt (`0x53543031`); the brief's locked values (salt **91401**) supersede it. The approved values are not yet written into the routed current-authority documents (see §10). |
| Geometry consistency check (owner's pre-implementation requirement) | **Consistent.** P1 frame: world = (cx+X, groundY+Y, cz+Z), anchor X=Z=0, Y=0 = lowest above-ground layer. Hall reservation X[-40,-2] Y[-24,-5] Z[2,40] lies wholly inside the ±40 footprint for all four rotations (unit-tested), so one custom `Structure` anchored at the footprint centre represents both the 81×81 eligibility rule and the buried hall. No blocker for the placement mechanism; the blocker is only the missing pyramid asset. |

## 2. What was implemented (`src/main/java/.../pyramid/worldgen`)

| Class | Role | Replaced when the pyramid exists? |
|---|---|---|
| `St01PlacementConfig` | Locked values: spacing 32, separation 8, triangular, salt 91401, footprint 81, Δh ≤ 16, cut ≤ 10, skirt ≤ 8 | no |
| `St01Eligibility` | Pure, deterministic qualification: (1) full-footprint Desert, (2) MODERATE terrain, (3) underground fluid | no |
| `St01PyramidStructure` | Custom `Structure`: runs `St01Eligibility` against the real generator (no chunk loading) and emits pieces only if eligible | no |
| `St01FootprintCorrectionPiece` | MODERATE correction over exactly the 81×81 footprint | no |
| `St01RoyalHallFixture` (+ `Piece`, `FluidSealPiece`) | **Temporary fixture adapter**: places `royal_hall.nbt` at its approved pyramid-relative position, rotation about the anchor, fluid seal | **yes — only `UNDERGROUND_VOLUME` and `addPieces` change** |
| `St01Worldgen` | registers structure type + 3 piece types (called from `MoreMinecrafts.onInitialize`) | no |
| `data/moreminecrafts/worldgen/structure/st01_pyramid.json`, `structure_set/st01_pyramid.json` | `random_spread`, spacing 32, separation 8, `triangular`, salt 91401, biome `minecraft:desert`, step `surface_structures`, `terrain_adaptation: none` | no |

### Definitions (established O-14 definitions, one deliberate strengthening)
* height difference = max − min of `WORLD_SURFACE_WG` base heights; foundation level `yf` = median; cut = max(0, max − yf); skirt = max(0, yf − min).
* **Strengthening:** O-14 sampled a 4-block grid (441 columns). A sampled grid cannot guarantee the limits for the skipped columns, so production samples **every** footprint column (6561). Cost is bounded (only anchors that already passed the Desert stage get here). This can only make acceptance stricter than Phase 3.1's predictions.
* **Full-footprint Desert.** `BiomeManager` resolves a block's biome to one of the 2×2×2 quart cells `floor((x−2)/4)+{0,1}` (and same for y, z). Requiring *every* such cell around *every* footprint column (22–23 cells per axis, at the cell's surface Y, both vertical candidates) to be Desert guarantees every footprint block resolves to Desert regardless of the jitter; the covering property is unit-tested for 601 anchors incl. negative coordinates. The anchor cell used by vanilla's stub check (`isValidBiome`) is additionally required so a stub accepted here cannot be silently dropped later.
* **Fluid safety.** Every generator base column over the fixture's underground volume **+1 block margin** must be fluid-free (reject otherwise). Because carvers/aquifers can still leave fluid that the noise column cannot show, a `FluidSealPiece` replaces fluid in the 1-block shell around the hall with sandstone (deterministic; never touches the box).
* **Correction.** Above `yf`: cut (to `base+8` to remove already-placed plants). Skirt band `[yf−8, yf−1]`: every non-solid block (air, fluid, replaceable) becomes sand (top 4) / sandstone, so a carved cave or lower column cannot leave a pit. Inputs are pure functions of the seed (generator base height), not of chunk order. Runs only for accepted starts ⇒ a rejected candidate leaves nothing.
* **Rotation.** Four rotations, no mirror, derived from the structure's seeded `WorldgenRandom` (vanilla convention). *This is an implementation choice, not an approved design value — flagged in §10.*

## 3. Tests and CI
RESULTS_PLACEHOLDER
