# P5 integration verification plan

Goal: preserve K2 and test coexistence with B25-T3; never promote a prototype to final design.
Spec: user SKY-TOWER-GEOMETRY-03C-P5 and ../../SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md.
Architecture: immutable P3 exports → explicit integrated occupancy + separate trial reservations → independent checker. Python stdlib; frozen P3 navigation reused read-only.

- [x] A: remote HEAD d3e6b4fb; read authority and P1–P4 reports; run unchanged P3 suite including P2. Require exit 0 before expansion.
- [x] B: analytic ray/box intersections, physical clear span and 0.6×1.8 AABB centre span; sample angles and disclose global-minimum limitations. Unit tests: axis corridor 4→3.4; diagonal erosion; slab height.
- [x] C: export C1/C2 tower plus unchanged K2; retain duplicates as claims. Check floor ownership, overlaps, headroom and BFS routes independently. Reserve absent base access / roofless sanctum rather than inventing them.
- [x] D/E: preserve baseline, enumerate balcony translations; record rejections and local scores; reserve all requested decorative classes as trial ranges, not approved dimensions. Report reservation intersections without deleting anything.
- [x] F: run integration checks, eight injected faults, deterministic regeneration, plans/sections; attempt runtime prerequisite checks and run feasible server checks. Distinguish NOT RUN from PASS.
- [x] Review evidence, commit only p5, recheck remote HEAD, normal push to specified branch.

Review focus: radial sample ≠ continuous global proof; floor ownership ≠ y translation of whole tower; intentional joins ≠ collisions; reservation ≠ occupied geometry; historic LIVE ≠ new LIVE.

Execution note: all investigations run; incomplete structural/live/global-width certifications remain explicit UNVERIFIED/NOT RUN in P5_REPORT, not accepted as PASS. Fresh independent review found slab/full join checker defect; test_review reproduced RED then GREEN after compatible-shape/portal-area join and penetration fixes. No new design ruling made.
