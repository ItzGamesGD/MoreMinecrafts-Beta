# D13-03 I06-P4 — ST-01 integration readiness review (2026-11-08)

Status: **implementation blocked by unresolved design inputs**. This is an integration review, not a new structure authority or approval to use proposed coordinates. No reward, boss, world-generation, NBT, or runtime behavior was changed.

## Baseline and source review

- Requested baseline `48038bc9018fc9001635bf9a2f242cd2da62b57c` is the merge commit for PR #243 (I06-P3), whose head was `bcb58ff6bbbc0ee1b79f0d56502c6756c636bb8e`. PR #243 is MERGED into `implement/d13-03-i05-pyramid-royal-chest-rewards-20261106`.
- PR #237 (I05) remains OPEN. It and existing design branches were left intact.
- Reviewed P4 geometry (`work/st01-p4-secret-area-20261107` @ `0d486fd1`), P4-B/P5 (`work/st01-p5-integration-20261107` @ `f7bf07fc`), and P6 (`work/st01-p6-final-spec-20261107` @ `06a262d7`). These are separate remote branches; their geometry reports are static model outputs, not structure NBT or world-generation artifacts.
- P4-B is explicitly a conditional, document-only adoption. P5 calls its integrated geometry PROPOSED and says it does not approve NBT/world generation/boss/reward implementation. P6's implementation handoff says implementation is not approved and lists required unresolved decisions.

## Coordinates and encounter inputs

P6's east/west model lists these local candidate chest cells (origin at pyramid ground center, X east+, Z south+, Y layer; inclusive bounds):

| Role | East candidate | West candidate | Model status |
| --- | --- | --- | --- |
| PRIMARY | (-21, -20, 33) | (21, -20, 33) | CONDITIONALLY APPROVED; chest size UNVERIFIED (P3-F6) |
| SECONDARY | (-3, -22, 25..26) | (3, -22, 25..26) | CONDITIONALLY APPROVED; chest size UNVERIFIED (P3-F6) |

These are local design coordinates, not placed block coordinates in a generated structure. D-I (east vs west) is unresolved, and P6 marks all world behavior/NBT as UNVERIFIED.

P6 D-E leaves the Tomb Guardian anchor and emergence point unselected. The document lists S 5.0 / E-W 12.5 / N 17.0 as candidates, not approved `BlockPos` values. No structure-supplied anchor or spawn position can therefore be passed safely to `TombGuardianEncounters.onRoyalChestInteraction` or `RoyalChestRuntime.register`.

## Runtime and identity path

I06-P3 provides `RoyalChestRuntime.register(level, structureInstanceId, tombAnchor, bossSpawn, primary, secondary)`. `RoyalChestData` persists instance IDs, positions, catalyst assignment, generated slots, and remaining stacks in per-dimension `SavedData`; identity is the supplied stable ID plus chest role, while coordinates only locate chests. Re-registering an ID preserves rewards and catalyst while relocating positions.

The repository has no ST-01 structure generator, NBT, or structure-instance ID creation/storage/reuse path. The call belongs in the future structure placement/generation path once it has a stable per-instance ID and approved positions. No such call site exists on the baseline, so adding one now would require invented identity, coordinates, and world-generation behavior.

## Decision gates

The current P6 open-decision record blocks direct integration on D-E (anchor/spawn), D-I (orientation), and D-N (biome/height/terrain/alignment/spacing and missing worldgen hook). Completing the full ST-01 implementation also requires D-A/D-B (secret entrance state), D-F (golem), D-G (crypt), and D-H (depth). The model also leaves the PRIMARY/SECONDARY chest dimensions unverified. These values remain unchanged and un-inferred here.

When the required inputs are approved, the intended integration point is the ST-01 generation hook after stable instance identity, both chest positions, and the encounter positions are available. Registration must be idempotent for the same instance ID. Existing I06-P3 reward tables, probabilities, quantities, boss rules, and persistence semantics remain the contract.

## Validation status

- Existing I06-P3 tests cover runtime behavior using synthetic registered positions; they do not prove ST-01 placement or multi-structure identity in generated worlds.
- Full Fabric CI / GameTest command (`./gradlew clean test build compileGametestJava runGameTest --no-daemon`): **NOT RUN**. Gradle starts when using `/workspace/gradle-home`, but cannot resolve Fabric Loom 1.17.21 because Maven network access is unreachable from this environment. GitHub CI remains the validation path after pushing the Draft PR.
- Actual server behavior: **LIVE_SERVER_UNVERIFIED**. No server session or generated ST-01 structure is available in this review.
