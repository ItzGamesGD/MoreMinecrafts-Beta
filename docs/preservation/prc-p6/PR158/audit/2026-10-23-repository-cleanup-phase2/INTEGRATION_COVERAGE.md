# Integration coverage and ownership

Snapshot: 2026-10-23 UTC. GitHub REST branch and PR collections, paginated at 100. Current branch SHAs are from the live branch collection; PR heads and bases are from the live PR collection. Classifications are conservative. No existing ref was changed.

This is a dependency map for protection, not a claim of semantic equivalence. Exact PR head/base pairs appear in the inventory.

| Area | Current ownership / evidence | Remaining uncertainty |
|---|---|---|
| Batch A, XP Quest, Knowledge, Research | [#156](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/156) `05a378211a6e4576609416096d3515a6b1dee36f` is the cumulative recovery baseline. Its [integration report](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/integration/batch-a-recovery-20261022/docs/integration/2026-10-22-batch-a/INTEGRATION.md) describes reused Q1 and UI work, 75 registered quests and 125 unregistered slots. | Custom screen authority, quest release holds, Windows physical interaction and unfinished slots remain open; donor branches are retained. |
| Security | [#157](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/157) `219d74f89ae253c98b4c2f2156f884d13dba09f6` supplies the exact-fingerprint Gitleaks remediation; its [report](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/security/gitleaks-remediation-20261022-codex-725159/docs/security/GITLEAKS_REMEDIATION_2026-10-22.md) refers to #156. | Do not infer #157 was merged into #156; both remain Draft. |
| Accessories | #142–#153 include stacked infrastructure, persistence, client, visual, and enhancement work. Live bases in inventory reveal stack edges. | Gameplay effects, inventory UI, resource assets and tests need per-commit coverage and live validation; all retained. |
| Bounty / contracts | #148, #150, #151 are potentially partial and dependent implementations. | No release or complete replacement proof; all retained. |
| Structures | Structure generation, building design and tests may be in active chains. | Only this repository is in scope; no cross-repository deletion inference. |
| Territory | Shelved feature retains design/provenance evidence. | Shelving is not branch release. |

The [#154 dependency graph](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/blob/audit/repository-pr-reconciliation-20261022/docs/integration/2026-10-22-pr-reconciliation/PR_DEPENDENCY_GRAPH.md) records earlier merge-base and file-set work against a prior snapshot. Its conclusions require revalidation against every current head before candidate classification. A live PR body can be stale: #156's description cites `725159e`, while the live head is `05a378211a6e4576609416096d3515a6b1dee36f`.

**Coverage status:** Complete semantic preservation, unique commit/file coverage, live worktree ownership and full dependency ancestry were not established for every item. Candidate gates therefore fail conservatively.
