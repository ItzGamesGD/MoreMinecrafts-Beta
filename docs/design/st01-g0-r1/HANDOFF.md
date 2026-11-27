# Construction handoff — proposed geometry, not production authority

## Exact reproduction

Requires Python3 and NumPy (verified with 2.3.5). From repository root:

    python3 tools/st01-g0-r1/blueprint.py --out work/st01-g0-r1-replay

The generator only writes design JSON. It does not write production NBT, Java or assets. Source hashes in blueprint.json pin the source engines, parser and generator. `block-state-reference.json` is a 21-block subset of MM-SG's pinned Minecraft26.3 reference, with server-JAR SHA1/version metadata. MM-SG's exporter was not run or modified.

For each route, apply ordered `operations` to the 81×72×81 domain X/Z−40..40, Y−32..39 initially air. Use states exactly as specified. Then overlay original D1 at (−40,−24,2) with verbatim NBT/block entities and finally apply the 12 exception entries. The generator records a complete independent replay comparison. Air/default cells are explicit through the domain and operation semantics. Do not use bounding boxes to replace component box unions.

Use `connections`, component origins/offsets/parents/order, final block-entity positions and palettes from blueprint.json. `generated/COMPONENTS.md` is a human-readable coordinate table. `d1-exceptions.json` records each assembly/local coordinate, original/post state, constraints and all four rotations. Do not place any old P3 reconstruction or SECONDARY placeholder.

Whole rotations and translated origin are independent inputs; access route is a separate enum. The generator includes an exact design-only replay of the O-14 measurement `mix64/selectOrientation` function, using the unchanged candidate-centre X/Z and seed. It does not derive selection from the old mirrored hall, call B2 reward RNG or create a new production selection policy. Compare the Java reference program for fixed vectors. This demonstrates representation independence and stable mapping, not an empirical probability proof. C2-B/C2-C retain the approved persistent deterministic 50:50 selection contract.

## C2-A restart gates

1. Owner approves or amends the three numerical/route/visual bundles. Record approvals narrowly in current authority.
2. Retain original D1 hash and exact exception/cover masks. No other D1 edits.
3. Run all design checks and reproduce signatures after any amended geometry; keep all eight orientation cases.
4. A C2-A implementation must validate actual exported assets and actual Minecraft placement/block entities; these design checks do not replace that validation.
5. Do not activate natural spawning, runtime registration, secret unlock behavior, loot or harvest policies during C2-A. C2-B owns the natural-placement/server-thread handoff; C2-C owns persistent placement-progress recovery.

This is a deterministic construction candidate that can be exported after approval. It is not G0-R1 APPROVED / CONSTRUCTION READY until essential approvals are actually received.
