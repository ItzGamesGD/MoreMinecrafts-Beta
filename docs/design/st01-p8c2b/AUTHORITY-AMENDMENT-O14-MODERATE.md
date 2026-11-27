# ST-01 authority amendment — natural-worldgen placement and MODERATE terrain (2026-11-10)

Status: **CURRENT AUTHORITY** for ST-01 natural-worldgen placement and terrain eligibility/adaptation.
Source: the owner's P8-C2-B brief (2026-11-10), §2 "Approved Configuration — Locked" and its "Authority conflict
resolution" paragraph. This record synchronizes that approval into the repository; it does not add any design.

## 1. Superseded record (preserved, not rewritten)

The C1 generation policy recorded in
[`../st01-g0/G0-A-AUTHORITY.md`](../st01-g0/G0-A-AUTHORITY.md) (row "C1 generation policy") and restated in
[`../st01-g0-r1/AUTHORITY-DELTA.md`](../st01-g0-r1/AUTHORITY-DELTA.md) lists:

| Quantity | G0-A / AUTHORITY-DELTA (C1) |
|---|---|
| Height variation over the footprint | 12 |
| Cut (excavation) | 8 |
| Skirt (foundation exposure) | 6 |

These documents remain the historical record of the C1 approval and are not edited.

## 2. Approved production values (locked)

| Parameter | Value |
|---|---|
| Structure | ST-01 Pyramid (complete P8-C2-A assembly) |
| Footprint | 81×81 blocks |
| Spacing | 32 chunks |
| Separation | 8 chunks |
| Spread | TRIANGULAR |
| Salt | 91401 |
| Biome requirement | The entire 81×81 footprint must be Desert |
| Terrain policy | MODERATE |
| Maximum height variation | **16** blocks |
| Maximum excavation / cut | **10** blocks |
| Maximum foundation / skirt | **8** blocks |
| Underground fluid safety | Required |

None of these values may change without explicit owner approval. Code: `St01PlacementConfig`, the structure set
`data/moreminecrafts/worldgen/structure_set/st01_pyramid.json` (asserted equal by `St01WorldgenResourceTest`).

## 3. Scope of the supersession

* **Superseded:** the C1 values 12 / 8 / 6, and only as worldgen terrain-eligibility and terrain-adaptation limits
  (the quantities "height variation", "cut" and "skirt" of a candidate footprint). For those quantities the
  authoritative production values are 16 / 10 / 8.
* **Not a geometric conflict.** The 12/8/6 and 16/10/8 values describe the same three terrain quantities; neither set
  constrains pyramid geometry. Pyramid geometry, the stepped exterior, D1 bytes, the 12 seam cells, the Y−23 cover,
  rooms and routes remain governed by the [G0-R1 owner approval record](../st01-p8c2a/G0-R1-OWNER-APPROVAL-RECORD.md)
  and are unchanged.
* **Unchanged from C1:** 32/8 TRIANGULAR; complete pyramid; four verified rotations; reject unsafe conflicts;
  footprint-based ground with entrance access.
* **Unchanged elsewhere:** PRIMARY chest, canonical identity (dimension + transformed world-space tomb anchor),
  absent SECONDARY and deferred catalyst entitlement, Guardian behavior, reward probabilities.
* The P8 plan's earlier UNAPPROVED placement entries (different salt, spacing marked unapproved) are superseded by §2.

## 4. Definitions used by the implementation (HOW, recorded for owner review — not new design values)

The quantities are the O-14 definitions, evaluated on every one of the 6,561 footprint columns:
`WORLD_SURFACE_WG` base heights; height variation = max − min; foundation level `yf` = median; cut = max − `yf`;
skirt = `yf` − min. The pyramid's walking layer (authored Y0) is placed at `yf`.

The following implementation definitions were needed to implement already-approved requirements and are listed
in the P8-C2-B report as owner-review items. They are not promoted to design authority by this record:

* entrance access: reject a candidate when any column of the row directly outside the main-entrance opening rises
  more than 1 block above the walking level;
* underground fluid safety: reject a candidate when the generator's base terrain holds fluid anywhere in the complete
  assembly's underground block (81×32×81, authored Y−32..−1) plus a 1-block margin;
* orientation: one of the four rotations from the structure start's seeded random (vanilla convention);
* EAST/WEST: the approved 50:50 selection as a pure function of world seed and transformed tomb anchor.

## 5. Explicitly not decided here

Vanilla-structure overlap policy (mineshafts, desert villages, other structures) is **not** approved policy. See the
P8-C2-B report for measured overlaps and alternatives.
