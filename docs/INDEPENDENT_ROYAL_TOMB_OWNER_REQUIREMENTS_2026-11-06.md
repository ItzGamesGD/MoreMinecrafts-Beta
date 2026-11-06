> **SUPERSEDED — 2026-11-06 OWNER CORRECTION.** The owner confirmed that the intended royal tomb is the internal royal burial hall of **ST-01 giant pyramid**, not a separate independent structure. All applicable gold golem guidance and treasure-room requirements have been incorporated into `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md`. This file is retained only for historical traceability; it does **not** authorize creation of an independent royal tomb, independent boss, or separate reward economy. Existing ST-01 Tomb Guardian and chest rules take precedence. Design integration approved; implementation NOT approved.

# Independent Royal Tomb — Owner Requirements and Conflict/Audit Gate

Date: 2026-11-06
Status: OWNER REQUIREMENTS RECORDED; EXISTING DESIGN AUDIT INCOMPLETE; NO IMPLEMENTATION APPROVAL
Structure: **independent royal tomb**, NOT ST-01 giant pyramid's internal royal burial hall.

## 1. Scope and non-transfer rules

- Independent royal tomb and ST-01 giant pyramid are distinct structures.
- Independent royal tomb has its own boss, combat space, treasure room and reward economy.
- Never automatically transplant ST-01 Tomb Guardian, D09, or D13-03-I05/I06 chest/reward rules into independent royal tomb.
- Preserve all existing owner-approved independent royal tomb structure, combat, rewards, and persistence rules. Report any conflict before resolving it.
- The ST-01 architectural BUILD Q01-Q62 ledger is NOT an independent royal tomb design authority.

## 2. Dedicated boss

- Design an independent royal tomb boss distinct from ST-01 Tomb Guardian; if a previously approved dedicated boss design exists, preserve it first.
- Do not represent unimplemented features as implemented.
- Add a post-victory recognition scene; preserve existing boss respawn, drops and persistent state behavior.

## 3. Post-victory gold golem guide sequence

Only after normal successful boss defeat:
1. Complete boss death and encounter termination through the normal rules.
2. Show golden particles as boss disappears, visually suggesting transformation into a **small gold golem**.
3. Guide golem bows or otherwise recognizes the player.
4. Golem walks toward the independent royal tomb treasure room and guides the player.
5. Wait if the player falls behind. Interrupted or failed guidance must **never permanently lock treasure room access**.

Preferred engineering investigation: keep real boss death/state/drop processing separate from the guide entity and presentation. No implementation approved yet.

## 4. Expanded independent treasure room

- Create a dedicated large treasure chest model/container for this structure.
- First audit existing independent royal tomb chest count, placement and any already-approved chest behavior; only then determine additional chest count and layout.
- Display real mineral blocks, including gold and diamond blocks, stacked in the treasure room to create a visibly rich vault.
- **Open decision:** mineral blocks mineable rewards vs decoration-only; no default.
- Candidate chest loot (NOT approved loot table): Ethereum, Adamantium, Mithril, gold, Netherite, diamond, accessories, enhancement stones, smithing templates, etc.
- Verify registered item IDs, item existence, acquisition rarity and economy balance before approving specific item counts, rates, chest distribution or drops. No item IDs or probabilities invented here.

## 5. Audit / implementation gates

1. Retrieve the independent royal tomb's latest approved design and actual implementation status from GitHub, including boss, chest count/location, treasury access and persistent defeat logic.
2. Compare existing decisions to requirements above. Report actual conflicts, missing records, ambiguous naming, and missing implementation separately.
3. Preserve prior approved work. Identify and quarantine any ST-01-derived assumptions before integrating independent structure requirements.
4. Ask only the first **genuinely unresolved** design question after the audit; do not repeat already-approved decisions.
5. Do NOT modify Java code, NBT, worldgen, assets or loot tables without separate explicit implementation approval.

## Initial conflict review (limited evidence; not a completed repository-wide audit)

- **Confirmed naming/scope risk:** ST-01's architectural ledger uses “Royal Tomb” in its title while documenting the giant pyramid's *internal* royal burial hall, Tomb Guardian and two royal chests. These do not establish independent royal tomb rules. This scope conflict is explicitly resolved by separating the two documents; no ST-01 gameplay rule is transferred.
- **Unverified:** the latest independent royal tomb design authority, its approved boss, chest count, treasury geometry, and actual code/NBT status have NOT been established from the limited GitHub searches conducted during this recording. Search returned no matching default-branch file excerpts for Korean 왕릉, “royal tomb”, “mausoleum” or “gold golem”; that is NOT proof of absence, particularly for non-default branches.
- **Potential, not confirmed, conflicts:** gold golem guidance vs existing boss persistence/treasure gating; large chest/physical mineral blocks vs approved rewards and economy; whether “Ethereum” is an actual registered item. These require source checks, not assumptions.

## Next design question status

**BLOCKED pending targeted independent royal tomb authority audit.** Do not substitute an ST-01 question or invent an existing independent royal tomb decision. Once audited, begin with the first unresolved choice, likely guide golem behavior/access fallback or treasury loot status, depending on what is already approved.

Cross-reference: docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md on the same design branch, which is exclusively ST-01 pyramid architecture.
