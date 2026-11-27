# 02 — Construction-plane analysis

Status: **analysis only. The production plane (median of the 6,561 `WORLD_SURFACE_WG` heights) is unchanged. Choosing another plane is an owner decision.**

## Definitions

* `h`: production base height (first free Y) of a footprint column. The natural top solid block is `h − 1`.
* `yf`: walking level Y0 of the assembly.
* Cut `= max − yf`; skirt `= yf − min`; diff `= max − min`.
* **Excavation**: solid natural blocks above Y0 that must be removed, Σ max(0, h − 1 − yf).
* **Body above ground**: assembly cells (Y−32..Y0) placed where there was air or fluid above the natural ground, Σ max(0, yf + 1 − h).
* **Exposed body face**: on the one-block ring outside the footprint, Σ / max of (yf − h) where the outside ground is below Y0. It measures how much of the underground body becomes a visible wall. Values above 33 mean the outside ground is lower than the body floor.
* **Boundary cliff**: on the same ring, Σ / max of (h − 1 − yf) where the outside ground is above Y0. Nothing outside the footprint is cut, so this is the natural wall left standing at the footprint edge.

Planes compared:

| Plane | Definition |
|---|---|
| `median` | production: sorted[n/2] |
| `mean` | round(mean) |
| `trimmed10` | mean of the central 80 % |
| `p25`, `p40`, `p60`, `p75` | percentiles |
| `midrange` | ⌊(min + max) / 2⌋ |
| `window_clamped_median` | median clamped into the admissible window [max − cut, min + skirt] when that window is non-empty |

## Eligibility by plane (221 full-Desert anchors, 20 seeds)

| Plane | Policy D eligible | Policy B eligible | Policy B density / 100 km² |
|---|---|---|---|
| median (production) | 46 | **172** | 2.050 |
| mean | 47 | 174 | 2.074 |
| trimmed10 | 47 | 172 | 2.050 |
| p25 | 45 | 162 | 1.931 |
| p40 | 46 | 165 | 1.967 |
| p60 | 41 | 173 | 2.062 |
| p75 | 38 | 175 | 2.086 |
| midrange | 61 | 182 | 2.170 |
| window_clamped_median | 61 | **182** | 2.170 |

182 is the ceiling for Policy B: every anchor with diff ≤ 40 has an admissible plane ([01](01-baseline-and-policy-comparison.md)). Only `midrange` and `window_clamped_median` reach it.

## Correction metrics on eligible anchors, Policy B (median / p90 / max)

| Plane | n | Cut | Skirt | Excavation blocks | Exposed body face max | Boundary cliff max |
|---|---|---|---|---|---|---|
| median | 172 | 8 / 18 / 24 | 10 / 21 / 32 | 5,258 / 13,915 / 28,733 | 7 / 16 / 55 | 6 / 16 / 23 |
| mean | 174 | 8 / 18 / 24 | 10 / 21 / 32 | 5,231 / 12,783 / 24,164 | 7 / 16 / 54 | 6 / 15 / 24 |
| trimmed10 | 172 | 8 / 18 / 24 | 10 / 21 / 32 | 5,232 / 13,683 / 24,552 | 7 / 16 / 54 | 6 / 15 / 23 |
| p25 | 162 | 10 / 19 / 24 | 7 / 20 / 31 | 10,962 / 24,674 / 45,789 | 5 / 14 / 53 | 8 / 17 / 23 |
| p40 | 165 | 9 / 18 / 24 | 9 / 21 / 32 | 7,114 / 17,277 / 35,441 | 6 / 16 / 54 | 7 / 16 / 22 |
| p60 | 173 | 7 / 18 / 24 | 11 / 22 / 32 | 3,778 / 10,326 / 21,978 | 8 / 17 / 55 | 6 / 15 / 24 |
| p75 | 175 | 6 / 15 / 24 | 13 / 23 / 30 | 1,584 / 6,055 / 14,775 | 10 / 19 / 56 | 4 / 13 / 24 |
| midrange | 182 | 11 / 17 / 20 | 11 / 17 / 20 | 8,842 / 43,876 / 85,783 | 8 / 16 / 46 | 9 / 16 / 19 |
| window_clamped_median | 182 | 8 / 21 / 24 | 10 / 21 / 32 | 5,738 / 14,722 / 28,733 | 7 / 16 / 55 | 7 / 18 / 24 |

For reference, Policy D with the median plane gives: cut 5 / 8 / 10, skirt 6 / 8 / 8, excavation 2,927 / 8,293 / 11,332, exposed face max 5 / 8 / 11, cliff max 4 / 7 / 10. The full table for all planes is in [results/analysis.md](results/analysis.md).

## Findings

1. **Median, mean and trimmed mean are practically interchangeable.** Their eligibility differs by at most 2 anchors and their volumes are within a few percent. Desert footprints are unimodal enough that the robust and non-robust centres coincide.
2. **Percentiles trade excavation against exposure.** Low planes (p25) double the excavation (p90 24,674) but reduce exposed body faces. High planes (p75) cut excavation to ~30 % but raise exposure (p90 19, median 10) and eligibility only marginally. Neither changes coverage materially.
3. **Midrange maximises eligibility but explodes volume.** It balances cut and skirt (both ≤ 20) and accepts all 182. However, on footprints that are mostly high with a low pit it digs the whole footprint down: p90 excavation 43,876 and max 85,783, 3× the median plane.
4. **The window-clamped median is the only plane that reaches the 182 ceiling while keeping the median's volume profile.** It differs from the median only on the 10 anchors the median rejects for cut, so p90 excavation rises just 13,915 → 14,722 and the maximum is unchanged.
5. Within Policy D the same clamp raises eligibility 46 → 61, because it rescues the cut/skirt rejections. This is noted for completeness; it is **not** proposed for Policy D.

## Options for owner decision (none adopted)

| Option | Effect under B | Cost |
|---|---|---|
| Keep `median` (production) | 172 eligible | none; behaviour identical to today's algorithm |
| `window_clamped_median` | 182 eligible (+10, +5.8 %) | a different `yf` on 10 anchors per 20 seeds; deterministic, same inputs |
| `midrange` | 182 eligible | 3× worst-case excavation; not recommended |

The plane determines where the pyramid sits vertically and how much terrain is removed, so it is a gameplay-visible choice. The probe implements it only as a measurement.
