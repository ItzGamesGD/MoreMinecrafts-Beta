# Ancient City whole-city variant architecture probe — 2026-10-14

## Scope and authority

This branch is an architecture probe only. It does not implement Death, Nether, or Living Ancient City content.

Verified implementation base: `stage-20/checkpoint` at
`20121ca7cceae03602038de31076bd9ace115208`.

Approved Structure design: `docs/ANCIENT_CITY_STRUCTURE_EXPANSION_2026-10-13.md`
on `docs/2026-10-07-content-first-axis-review`.

PR #83 remains a separate, unmerged independent-structure prototype. Its current observed HEAD during this probe was
`e7dc53f9f48dfece8a5723debd5526dc061107d1`. This branch does not modify PR #83 and does not reuse its pool contribution as the whole-city variant mechanism.

## Observed target-version lifecycle

Minecraft 26.3 exposes the generated Ancient City through the structure registry as
`minecraft:ancient_city`. At runtime, `ServerLevel.findNearestMapStructure` can locate a city, the chunk at that located structure start can be loaded through `ChunkStatus.STRUCTURE_STARTS`, and `StructureManager.getStartForStructure` exposes one `StructureStart` containing the assembled piece list.

That gives a stable city-scoped boundary after structure-start assembly: one start chunk identifies one Ancient City instance and the start owns the relevant pieces. The probe deliberately uses that structure-start boundary rather than individual template-pool selections.

## Candidate architectures

### Rejected: variant buildings mixed into vanilla Ancient City pools

Rejected for whole-city variants. Pool-level random selection can mix unrelated themes inside one city and is therefore suitable only for independent structures such as the Arena.

### Rejected: process-global mutable current variant

Rejected because chunk generation, structure queries, reloads, and multiple cities cannot safely depend on one mutable global state.

### Selected probe architecture: deterministic city identity + city-scoped resolver

Stable city identity:

`Ancient City structure-start chunk`

Selection input:

`world seed + structure-start chunk + fixed MoreMinecrafts domain salt`

The selection function is pure and has no mutation or execution-order dependency. The prototype variants are:

- NORMAL
- DEATH
- NETHER
- LIVING

The four-way distribution exists only for architectural validation. Gameplay probabilities are not locked by this probe.

## Why this model

The structure start is the correct scope for a whole-city decision because it represents one assembled Ancient City rather than one jigsaw candidate. Every operation that begins from that `StructureStart` can query the same deterministic result.

No persistent mutable save record is required merely to remember the variant: the world seed and city start identity reproduce it. This also avoids state corruption from chunk load order.

Future content may still persist additional per-city progression state if gameplay requires it; that is separate from variant identity.

## Future extension path

### Death / remains

Recommended first real implementation. Resolve the city variant from the structure start, then transform or decorate only blocks/areas belonging to that city's assembled pieces or a carefully bounded city-level pass. This avoids touching unrelated caves merely because they happen to be nearby.

### Nether corruption

Use the same city identity and variant resolution, plus the stable city origin/start as the distance origin. A future pass can derive corruption intensity from world-space distance to that center. The exact canonical visual center should be verified when the Nether implementation starts; the structure-start identity itself remains independent of that choice.

### Living / peak city

Variant identity remains usable, but Living is likely to need a stronger pre-placement hook or alternate pool/template tree because restoration can change large parts of assembly rather than only decorate placed pieces. This probe does not claim that the final Living substitution hook is already implemented.

## Independent structure compatibility

Whole-city variant and independent landmarks are orthogonal axes:

`city variant = NORMAL / DEATH / NETHER / LIVING`

`independent landmarks = Arena / future landmarks / none`

PR #83's Arena pool contribution does not participate in the resolver, and the resolver does not remove independent structures.

## Probe implementation

Files implement:

- `AncientCityVariant` — four probe identities.
- `AncientCityVariantResolver` — deterministic city-scoped selection.
- `AncientCityWholeVariantProbe` — environment-gated real-world observer; never edits terrain.
- unit tests for determinism, order independence, identity, and multi-variant reachability.
- GameTest coverage for deterministic/non-global selection.
- a GitHub Actions workflow that performs the full build, samples real Ancient Cities, restarts the same world, re-samples, and compares city→variant mappings.

Environment gate:

`MOREMINECRAFTS_WHOLE_VARIANT_PROBE=1`

Normal gameplay does not run the observer.

## Required architecture questions

1. **Can a variant be selected logically once per Ancient City?**  
   Selected model: yes, by deriving one value from the structure-start identity.

2. **What uniquely identifies that city?**  
   The Ancient City structure-start chunk within the world.

3. **Can every piece determine which city it belongs to?**  
   A city-level operation starting from `StructureStart` owns the piece list and therefore supplies the parent city identity. This probe does not add a global reverse lookup from arbitrary standalone piece to city.

4. **Can the result survive chunk unload/reload?**  
   The resolver is independent of loaded state.

5. **Can the result survive server restart?**  
   Intended yes because it is deterministically reconstructed. The dedicated workflow validates this on the same saved world.

6. **Does generation order matter?**  
   No mutable selection state is used.

7. **Can two cities have different variants?**  
   Yes by design; tests and runtime sampling verify reachability.

8. **Can one city accidentally contain two whole-city variants?**  
   Operations scoped to one structure start use one resolver result. The runtime probe checks every observed piece under each sampled start against that single value.

9. **Can Arena coexist with every variant?**  
   Architecturally yes; landmark pool selection is separate from city identity.

10. **Can Nether reliably know the city center and compute distance?**  
    The structure-start identity is stable. A specific visual center/origin for radial corruption must be chosen and verified during the Nether implementation.

11. **Can Living restore/substitute large areas without rewriting everything?**  
    Variant identity is sufficient, but the exact pre-placement substitution hook remains future implementation work.

12. **Can Death transform the city without confusing nearby caves?**  
    Yes if the first implementation scopes transformation to structure-start pieces/bounds rather than a raw radius. That containment rule is part of the recommended next slice.

## Validation status

- INVESTIGATED: **PASS** — target runtime structure-start boundary and piece access are used by the probe.
- ARCHITECTURE_SELECTED: **PASS** — deterministic structure-start identity/resolver selected.
- STATICALLY_VERIFIED: **PENDING CI**
- RUNTIME_VERIFIED: **PENDING CI**
- WORLDGEN_VERIFIED: **PENDING CI**
- RESTART_VERIFIED: **PENDING CI**

These statuses must be updated only from actual workflow evidence.

## Current limitations

- This probe does not visually alter cities.
- It does not implement variant-specific block replacement.
- It does not prove the final Living City assembly substitution hook.
- The four-way weights are probe-only and are not gameplay balance.
- The city identity intentionally uses the generated structure-start chunk, not an arbitrary nearby piece chunk.

## Recommended next slice after PASS

Implement **Death / Remains Ancient City** first, using the selected city identity and strictly city-bounded transformation. Validate real generation and native client screenshots before progressing to Nether or Living.
