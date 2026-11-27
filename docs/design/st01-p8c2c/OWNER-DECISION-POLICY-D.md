# ST-01 P8-C2-C — Owner decision: Policy D (final)

Date: 2026-11-11 · Status: **FINAL, owner-approved** · Applies to: ST-01 Pyramid natural worldgen (P8-C2-C onward, prospectively)

## 1. Decision

The owner approved **Policy D** for production. Natural ST-01 Pyramid generation no longer rejects a candidate because of
(a) an obstructed entrance approach or (b) fluid in the underground inspection volume. This is an intentional gameplay
design decision, not a temporary bypass or debugging switch.

1. Policy D is final and owner-approved.
2. Entrance obstruction is **no longer** a rejection condition.
3. Underground fluid presence (water or lava, including the former one-block margin) is **no longer** a rejection condition.
4. Buried or elevated entrances are permitted; players may have to excavate them.
5. Natural fluid irregularities (underground water, water inside or next to chambers) are permitted.
6. No automatic entrance excavation, ramp, stair, tunnel or terrain flattening outside the footprint is required or allowed to be added.
7. No additional fluid sealing, barrier or broad fluid deletion outside the assembly is required or allowed to be added.
8. Every other constraint is unchanged (table below).
9. The counterfactual rarity audit (PR #264, `audit/st01-p8c2b-counterfactual-rarity-20261111`, report commit `569fc789`) is preserved as historical evidence.
10. The change applies prospectively to P8-C2-C production generation. P8-C2-B records are not rewritten.

## 2. Old policy versus production policy

| Criterion | P8-C2-B production (Policy A, superseded) | P8-C2-C production (Policy D, current) |
|---|---|---|
| Footprint 81×81, entire footprint Desert | required | required (unchanged) |
| Spacing / separation / spread / salt | 32 / 8 / TRIANGULAR / 91401 | unchanged |
| Height variation / cut / skirt | ≤16 / ≤10 / ≤8 | unchanged |
| Rotation | existing deterministic four-rotation selection | unchanged |
| EAST/WEST route | existing deterministic 50:50 selection | unchanged |
| Entrance approach | reject if any checked column in front of the entrance exceeds the walking level by >1 | **removed** |
| Underground fluid | reject if fluid anywhere in the 81×32×81 volume plus a 1-block margin | **removed** |

The superseding applies only to the two removed criteria. `AUTHORITY-AMENDMENT-O14-MODERATE.md` still governs the
terrain limits; its sentence "underground fluid safety required" is superseded by this record.

## 3. What this does not authorize

Engineering-integrity requirements stay in force: approved NBT assets are not modified; structure registration is not
broken; royal chest rewards are neither duplicated nor deleted; Guardian behavior is unchanged; no writes outside the
intended structure placement region; crashes, infinite generation loops, missing pieces, invalid block entities and data
corruption remain defects. Environmental water and buried entrances are accepted outcomes, **not** defects.

## 4. Scope notes

- The old entrance/fluid evaluation may exist only in isolated historical diagnostics (the audit branch). No production
  flag restores it.
- Vanilla-structure overlap policy is still not decided by this record.
- Future discovery direction: see `FUTURE-DESIGN-NOTE-EXPLORATION-DISCOVERY.md` (direction only, not implemented).
