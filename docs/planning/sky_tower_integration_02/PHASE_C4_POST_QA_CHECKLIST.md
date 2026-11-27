# C-4 Post-QA checklist (what still needs a human / client / later phase)

## CLIENT QA PENDING (no GUI client in this environment)
- [ ] `CLIENT WALKTHROUGH QA PENDING` — walk island → bridge → entrance → floor 0 → floors 1–3 → rooftop on each of the six styles; screenshots. Static BFS says the route exists (all six styles, rooftop y=84 reachable; with barriers only floor 0 reachable) but collision shapes of stairs/slabs/walls are approximated.
  - Reproduce positions: natural samples in `PHASE_C4_WORLDGEN_MATRIX.md` (e.g. desert 7202 site `1008,20,-160` rot 2; tower-local entrance `(38, floorY+1, 24)`, floor Y = 10/21/33/47/64, spawners `(17|31, floorY+2, 17|31)`, wall cells `(39..44, floorY+1..+3, 27)`; rotate/translate with `SkyTowerLayout.Site`). Use `/mmskytower status` to read masks.
  - Server for that: `LEVEL_TYPE=minecraft:flat` + the desert settings, seed 7202, or any world with `moreminecrafts` and `/locate structure moreminecrafts:sky_island_desert` (then look for the tower variant; ~14% of islands).
- [ ] Visual support / supports under the bridge, forest leaf-clearing (425 voxels cleared in the forest composite), cosmetic look of the six composites.
- [ ] Real multiplayer clients on one spawner; join/leave during an encounter.
- [ ] Real chunk unload/reload by player movement mid-progression (server restart is covered; streaming in/out in one session is not).

## LIVE COMBAT QA PENDING
- [ ] Real combat on floors 1–3 (only floor 0 exercised). Same spawner config/anchors, so low risk, but not observed.
- [ ] Natural cooldown expiry/reactivation of a completed spawner (30 min vanilla default) and ominous conversion.
- [ ] Real combat followed by a restart in a single run (restart was verified with real spawner *removals*, the combat itself with GameTest).
- [ ] Vault opening/loot by a player (untouched vanilla; not re-tested).

## Open verification gaps (server-side)
- [ ] Same-tick completion/destruction overlap (logic is idempotent; not run).
- [ ] Desert/badlands/snowy on **default overworld terrain** (only a controlled single-biome flat world was sampled for these three).
- [ ] Optional: confirm the Colosseum absence-grace flake against the older baseline commit.

## Deferred by design (not C-4 blockers)
- Custom Trial Spawner mob pools (after the custom mob roster / spawn configs exist).
- Rooftop special rewards / whole-tower completion event.

## Decisions for a human (none blocking)
- Barrier installation silently waits for chunk/entity readiness and for cells to be free of entities; the only signal is a one-time warn log for foreign blocks/entities. If a visible indicator is wanted, that is a design request, not a defect.
