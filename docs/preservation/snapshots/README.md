# Point-in-time snapshots of living design branches

A snapshot here is a byte-exact copy of a file as it existed at a named commit of a branch the owner is still editing.
It is **preservation, not authority**: nothing in this folder is routed from `docs/CONTENT_AUTHORITY_INDEX.md`, and the
branch itself remains the place where the design continues.

| Snapshot | Source branch | Source commit | Blob | Supersedes in this repository |
|---|---|---|---|---|
| `SKY_ISLANDS_T2_CONTEXT_2026-10-27@631d9504.md` | `docs/sky-islands-design-context-20261027` | `631d95044aab4b06728f6d35a8d66b819fa3ec2d` (2026-10-27 10:38 +0900) | `f5aa758e4354cdfcc7b10208f094a23bb1b870a7` | The earlier copy at `docs/design/SKY_ISLANDS_T2_CONTEXT_2026-10-27.md` on `checkpoint/wave12-unified-20261027` (commit `8ceff945`, blob `a91e385d`). That checkpoint is deliberately not rewritten; section 5 of the earlier copy was replaced by the owner in `631d9504`. |

The snapshot's own header marks the design as in progress and not an implementation instruction.
