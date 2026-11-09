# Stage 23-A PR #92 / #93 reconciliation (2026-10-19)

Status: **review draft; do not regard open historical PRs as merged**.

Baseline: PR #124 HEAD `5ea5f54614943c8fd00f047b7f8379f385d6e5e8`.
Historical input heads: PR #92 `f632f4f521c782c9614a3bbb71f1ab5579a3358d`, PR #93 `803d2e5df5fe2f990ff2f2e5ef3e01c6dcf37955`.

## Function mapping

| Source concern | Current accepted implementation | Interpretation |
| --- | --- | --- |
| #92 beekeeping .18 | `ResearchBeehiveMixin` + `ResearchBeehiveHarvestGuard` + `Research23AReconciliationGameTests` | Superseding guarded implementation; do **not** re-add removed `ResearchBeehiveHarvestMixin` |
| #92 mining durability .03 | `ResearchMiningContext` tracks the exact tool and effective mining scope, with existing `ItemStackMixin` | Stricter successor than original player-scoped context; do **not** overwrite |
| #92/#93 Survival & Expedition .55 | `ResearchMovementExhaustionMixin` and `Research23AEffectRules.exhaustionAmount`; sprint/swim-only 8/16/24% | Equivalent approved gameplay on the scoped vanilla hunger exhaustion calls |
| #92/#93 XP Control .67 | `ResearchExperienceOrbMixin` + `Research23AEffectRules.withinXpCollectionRadius` + `Research23AXpCollectionGameTests` | 4/6-block paid-radius pickup via vanilla playerTouch; no double XP claim |
| #93 Fisheries Study .19 | `FishingLogic.gradeWeights` + `Research23AEffectRules.fishGradeWeights` + exact six-by-four table test + `FisheriesStudyGameTests` | Current largest-remainder/tie-break implementation and live-path GameTest |
| #93 separate movement utility `ResearchParallelBranchRules` / `ResearchMovementExhaustion` | Unified `Research23AEffectRules`, direct `ResearchMovementExhaustionMixin` | Historical helper no longer needed |
| #92/#93 manifests | Original PR branches and reviews remain available | Historical, not canonical; original source retained on remote branches |

## Regressions and acceptance

- Current `Research23AEffectRulesTest` checks all 24 Fisheries Study tables, tie handling, sprint/swim-only reductions, inclusive XP radii, and research purchase gates.
- Current `Research23AXpCollectionGameTests` covers vanilla pickup, multiplayer recipient isolation, edge radius, fractional XP, and Mending.
- Current `Research23AReconciliationGameTests` checks guarded beehive results and mining scope.
- This reconciliation specifically restores PR #93's **research level codec serialization regression** as a test without restoring deprecated helpers.
- Original code and documents remain recoverable at original SHAs even when a PR is closed. Do not delete their historical branches here.

## Remaining gates

- The new reconciliation test must pass CI at its exact branch HEAD.
- Mapping represents **source inspection plus available test evidence**, not a newly completed live acceptance.
- PR #124 is Draft; future integrators must keep this reconciliation test alongside the accepted Stage 23-A implementation.
- Do not replay entire #92/#93 patches onto current code: original and successor mixins overlap and may duplicate effects.
