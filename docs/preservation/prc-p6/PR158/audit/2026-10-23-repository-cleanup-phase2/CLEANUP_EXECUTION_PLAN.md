# Cleanup execution plan

Snapshot: 2026-10-23 UTC. GitHub REST branch and PR collections, paginated at 100. Current branch SHAs are from the live branch collection; PR heads and bases are from the live PR collection. Classifications are conservative. No existing ref was changed.

1. **Safe initial closure batch:** empty. Do not close any existing PR.
2. **Deferred review:** all 65 suspected-active PRs and 24 unknown branches; obtain explicit owner/agent work-status and release evidence, then rerun full ownership and dependency comparisons per item.
3. **Protected work:** #154, #156, #157, their source branches, main and checkpoint branches; preserve accessory, bounty, structure and XP Quest stacks while still uncertain.
4. **Owner decisions required:** which agents/tasks have finished; whether any historical branch is expressly released; whether unique work is intentionally disposable; resolution of Batch A UI/quest and accessory/bounty/structure review gates. No design decision is made here.
5. **Rollback/recovery:** before any future authorized cleanup, pin every exact ref SHA in the inventory and preserve PR URLs, merge bases, commit and file lists. Restore a deleted ref only from a verified recorded SHA. This audit authorizes no cleanup.

Expected existing repository counts after an approved cleanup using the *currently verified* candidate set: **68 open PRs, 104 existing branches**. This audit itself adds one Draft PR and one branch, so the immediate repository count after publication is expected to be **69 open PRs and 105 branches**, subject to concurrent changes. These are projections, not completed cleanup.

Before any later execution, refresh all branch HEADs, PR head/base edges, CI/reviews and active work status. The current candidate set is empty, so no destructive action is scheduled.
