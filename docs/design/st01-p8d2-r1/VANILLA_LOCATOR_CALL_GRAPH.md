# ST-01 P8-D2-R1 — Source-backed call graph of the vanilla locator for ST-01

Source: `javap -c -l` of `minecraft-common-deobf-26.3.jar` (line numbers are the jar's `LineNumberTable`), and the repo at `04a44ffa`.

```
ServerLevel.findNearestMapStructure(set, pos, radius, skip)                         ServerLevel L1403-1404
└─ ChunkGenerator.findNearestMapStructure(level, set, pos, radius, skip)            ChunkGenerator L163-231
   ├─ group the set by StructurePlacement (getGeneratorState().getPlacementsForStructure)   L173-177
   ├─ (concentric-ring placements first; none for ST-01)                                   L189-204
   └─ for r = 0 .. radius                                     (rings, in placement regions)  L211
        for each random-spread placement (ST-01: one)                                      L213
          getNearestGeneratedStructure(set, level, sm, chunkX, chunkZ, r, skip, seed, pl)   L215  (static, L258-280)
            for k = -r..r (x), for l = -r..r (z), only ring edge cells                     L260-265
              pc = pl.getPotentialStructureChunk(seed, chunkX + spacing*k, chunkZ + spacing*l)   L271
                   (RandomSpreadStructurePlacement L62-72: floorDiv to the region, WorldgenRandom
                    setLargeFeatureWithSalt(seed, rx, rz, salt 91401), TRIANGULAR offsets — arithmetic only)
              getStructureGeneratingAt(set, level, sm, skip, pl, pc)                         L273, body L284-301
                ├─ sm.checkStructurePresence(pc, st01, pl, skip)                             L285
                │  └─ StructureCheck.checkStart(pc, st01, pl, skip)                          StructureCheck L87-127
                │     ├─ loadedChunks cache (chunks already loaded)                          L88-90
                │     ├─ tryLoadFromStorage (chunk already on disk)                          L94-98
                │     ├─ pl.applyAdditionalChunkRestrictions (frequency; 1.0 for ST-01)      L112-113
                │     └─ featureChecks[st01].computeIfAbsent(pc, canCreateStructure)         L117
                │        └─ canCreateStructure → new GenerationContext(...)                  L131
                │           └─ Structure.findValidGenerationPoint(ctx)                      Structure L295
                │              ├─ St01PyramidStructure.findGenerationPoint(ctx)             repo :56-66
                │              │  └─ St01Candidate.decide → St01Eligibility.evaluate        repo St01Candidate :27-35
                │              │     (full ST-01 eligibility, see ST01_FILTER_EXECUTION_ORDER.md)   ← EVALUATION #1 (server thread)
                │              └─ .filter(isValidBiome(stub))                                 Structure L295
                │     → START_NOT_PRESENT (rejected) | CHUNK_LOAD_NEEDED (passed)            L121-127
                ├─ START_NOT_PRESENT → next candidate                                        L287-288
                └─ else getChunk(pc, STRUCTURE_STARTS)                                       L293
                   └─ chunk pipeline (worldgen worker; server thread blocked in managedBlock)
                      └─ ChunkGenerator.createStructures (all possible structure sets of the chunk)   L473-540
                         └─ tryGenerateStructure → Structure.generate                       L543-554; Structure L204-223
                            └─ findValidGenerationPoint → St01PyramidStructure.findGenerationPoint   ← EVALUATION #2 (same candidate, recomputed)
                               └─ addPieces (template lookup + 4 pieces)                    repo :68-75
                   getStartForStructure(...).isValid() → (tryAddReference if skip) → getLocatePos(pc)   L294-297
          return the FIRST valid start in this ring's scan order                            L273-275
        keep the closest across placements; return as soon as a ring yields one            L216-227
```

## Facts this establishes
1. **Enumeration** is over **seed-derived structure-placement candidates** (one per 32×32-chunk region), in **square rings** (Chebyshev distance in regions), ring 0 first. No block-coordinate scanning. A ring of radius r has 8r edge regions (1 for r=0); radius R examines up to (2R+1)² candidates.
2. **Not distance-ordered within a ring:** the first valid start in `k`(x)-major / `l`(z)-minor scan order wins (L273-275). Across rings the result is the first ring containing any valid start.
3. **The only filter before ST-01's own code is arithmetic** (`applyAdditionalChunkRestrictions`, frequency 1.0) plus caches. **No vanilla biome prefilter** exists in `StructureCheck.checkStart` for this structure; the biome check vanilla adds (`isValidBiome` on the stub) runs **after** `findGenerationPoint`.
4. **Each successful candidate is evaluated twice**: once by `StructureCheck.canCreateStructure` on the server thread, and again by the chunk pipeline's `Structure.generate` (no reuse of the first result; `featureChecks` stores only a boolean).
5. `getChunk(…, STRUCTURE_STARTS)` also runs `createStructures` for **every** structure set possible in that chunk (vanilla structures included).
6. The located position is `getLocatePos(chunk)` (chunk minimum corner, Y 0).
