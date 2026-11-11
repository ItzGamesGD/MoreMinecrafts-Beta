# ST-01 P8-D2 — Explorer-map technical feasibility and performance validation

Status: **research complete for the evidence listed; no production code changed.** Builds on P8-D1 (PR #268, `a598054b`) and the P8-C2-C Policy D head
(PR #266, `c376488d`); none of these is merged. Owner-approved direction: [A4 + B1 + hybrid](OWNER_DECISION_A4_B1_HYBRID.md). Everything about acquisition details is still a proposal.

| Document | Content |
|---|---|
| [OWNER_DECISION_A4_B1_HYBRID.md](OWNER_DECISION_A4_B1_HYBRID.md) | the approved decisions and what stays provisional |
| [VANILLA_EXPLORER_MAP_AUDIT.md](VANILLA_EXPLORER_MAP_AUDIT.md) | how the 26.3 mechanism works; ST-01 targeting proof; the `skip_existing_chunks` finding |
| [CARTOGRAPHER_TRADE_FEASIBILITY.md](CARTOGRAPHER_TRADE_FEASIBILITY.md) | additive trade fixture: merge, preservation, availability, persistence |
| [STRUCTURE_SEARCH_BENCHMARK.md](STRUCTURE_SEARCH_BENCHMARK.md) | radius unit and cold/warm latency across seeds and radii |
| [NONBLOCKING_SEARCH_OPTIONS.md](NONBLOCKING_SEARCH_OPTIONS.md) | thread-safety constraints and N1–N5 experiments |
| [BOUNTY_MAP_INTEGRATION.md](BOUNTY_MAP_INTEGRATION.md) | A4-1…A4-4 feasibility |
| [OVERLAP_DETECTION_AUDIT.md](OVERLAP_DETECTION_AUDIT.md) | masking of ST-01 by overlapping structures, test specification |
| [XP_QUEST_COMPATIBILITY.md](XP_QUEST_COMPATIBILITY.md) | audit of the explorer-map XP quest |
| [RECOMMENDED_ARCHITECTURE.md](RECOMMENDED_ARCHITECTURE.md) | recommended hybrid structure; proposed P8-D3 / P8-D4 boundaries |
| [OWNER_DECISIONS_REQUIRED.md](OWNER_DECISIONS_REQUIRED.md) | the remaining decisions |
| [probe/](probe/) and [evidence/](evidence/) | probe source (text), runner scripts, fixture datapack, raw JSON outputs |

## Reproducing
Add `dev.itzgamesgd.moreminecrafts.worldgenprobe.st01d2.St01D2Probe` (from `probe/St01D2Probe.java.txt`) under `src/gametest/java/…/worldgenprobe/st01d2/`, list it in the gametest
`fabric.mod.json` entrypoints, copy `probe/fixture-pack/` to `build/st01-p8d2/pack-src/st01d2/`, set `JAVA_HOME` to JDK 25, then run `probe/run_all.sh`, `probe/run_extra.sh`
and `probe/run_extra2.sh`. **None of that is committed as code**; the probe is inert unless `-Dmm.st01.d2.out` is set.
