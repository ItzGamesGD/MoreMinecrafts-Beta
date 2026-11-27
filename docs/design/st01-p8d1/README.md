# ST-01 P8-D1 — Independent pyramid discovery systems: architecture and design package

Status: **PROPOSAL for owner decision. Nothing here is approved or implemented.** Documentation-only; no production code changed.
Base: P8-C2-C head (`c376488d`), unmerged. Owner-confirmed inputs: Policy D, preserved placement parameters, independent channels,
vanilla overlap permitted ([amendment](AUTHORITY-AMENDMENT-VANILLA-OVERLAP-PERMITTED.md)).

| File | Content |
|---|---|
| [EXISTING_SYSTEM_AUDIT.md](EXISTING_SYSTEM_AUDIT.md) | verified baseline; bounty and villager audits with status and source references; 26.3 API facts; findings F-1…F-6 |
| [DISCOVERY_CHANNEL_A_BOUNTY.md](DISCOVERY_CHANNEL_A_BOUNTY.md) | contract alternatives and proposed mechanics |
| [DISCOVERY_CHANNEL_B_VILLAGER.md](DISCOVERY_CHANNEL_B_VILLAGER.md) | trade/information alternatives and proposed mechanics |
| [SHARED_LOCATION_ARCHITECTURE.md](SHARED_LOCATION_ARCHITECTURE.md) | eligible vs valid vs generated; measured locator probe; proposed non-blocking service |
| [ARCHITECTURE_COMPARISON.md](ARCHITECTURE_COMPARISON.md) | vanilla-first / custom / hybrid, recommendation (hybrid) |
| [OWNER_DECISIONS_REQUIRED.md](OWNER_DECISIONS_REQUIRED.md) | decisions, cross-channel defaults |
| [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) | phases, benchmark proposal, test matrix |
| [probe/](probe/) | the read-only locator probe source and its raw result (evidence) |

Headline findings: (1) contracts carry no location today; (2) vanilla's locator returns valid starts only, but a cold search blocked the
server thread for 6–9 s at radius 20 in the probe; (3) 26.3 villager explorer maps are data-driven, so Channel B is cheap but unverified in
this repo; (4) the bounty entry detector can miss an ST-01 visit where it overlaps another structure (code-derived); (5) neither
"visited" nor "reward claimed" is tracked anywhere.
