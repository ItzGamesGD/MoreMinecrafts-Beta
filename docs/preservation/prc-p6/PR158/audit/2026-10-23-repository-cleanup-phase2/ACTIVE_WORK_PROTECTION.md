# Active work protection

Snapshot: 2026-10-23 UTC. GitHub REST branch and PR collections, paginated at 100. Current branch SHAs are from the live branch collection; PR heads and bases are from the live PR collection. Classifications are conservative. No existing ref was changed.

Primary counts for existing items: PRs: **0 ACTIVE, 65 SUSPECTED_ACTIVE, 3 PROTECTED, 0 UNKNOWN**. Branches: **0 ACTIVE, 65 SUSPECTED_ACTIVE, 15 PROTECTED, 24 UNKNOWN**. Counts use evidence thresholds, not a claim that no one is currently working.

All 65 PRs other than #154, #156, and #157 are SUSPECTED_ACTIVE. Their present open state, 66 Draft PRs overall, uncertain agent status, and unresolved integration/test/review work prevent an affirmative inactivity finding. Their head branches are SUSPECTED_ACTIVE. See the exact per-item classifications in [REPOSITORY_INVENTORY.md](REPOSITORY_INVENTORY.md).

#154 is PROTECTED as audit evidence. #156 is PROTECTED as the Batch A cumulative integration baseline with unresolved quest, UI, visual and owner decisions. #157 is PROTECTED as the security remediation source. Their source branches are PROTECTED. Checkpoint branches and main are also PROTECTED. Remaining 24 branches without current open PRs and without checkpoint protection are UNKNOWN because explicit release, live worktree status, unique-content preservation and dependency absence have not been established. UNKNOWN is excluded from cleanup.

Especially protect accessories #142–#153, bounty/contracts #148/#150/#151, structure work, and Stage23/XP Quest chains. The territory feature is shelved, but its historical documents and branches remain excluded absent specific preservation and release evidence. No GitHub timestamp or CI outcome is used as sole inactivity evidence.
