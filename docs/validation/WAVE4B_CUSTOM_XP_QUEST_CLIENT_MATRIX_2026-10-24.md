# Wave 4B — custom XP Quest client validation matrix

**Plan only; no scenario was executed by this documentation task.** Source candidate: PR #156 `05a378211a6e4576609416096d3515a6b1dee36f`. Its callback and clean-restart evidence is historical support, not physical input or final acceptance. Run against an exact future JAR and server pair; pin both source commits before testing. Do not test with a stale or duplicate mod JAR.

## Evidence record for every run

Record one row per test identifier, GUI scale, language and input method:

| Field | Required entry |
|---|---|
| Identity | Test ID; source commit; **exact client and server JAR SHA-256**; mod list |
| Environment | Windows version, Java version, Minecraft/Fabric version, GPU/driver, display resolution, windowed/fullscreen, GUI scale, language, save/world ID, server type and source/JAR |
| Input | Physical OS mouse/keyboard, Fabric TestInput, or direct Screen callback; mark **physical input: YES/NO** |
| Result | Expected result, observed result, timestamp, PASS/FAIL/BLOCKED, screenshot or video filename, relevant client/server log line or run URL |
| State | Player UUID/test alias, quest ID, progress/complete/claimed state, raw XP and level/progress/`totalExperience` before and after where relevant |

A direct callback can support IMPLEMENTED or CI_VERIFIED; it cannot mark PHYSICAL_INPUT_VERIFIED. Never record an unrun case as PASS.

## Test matrix

| ID | Input / setup | Expected observable result | Evidence |
|---|---|---|---|
| S01 | Physical advancement key in world, scales 1–4 | Four-card selector opens once; vanilla key path remains usable | Video and client log |
| S02 | Physical mouse at center of each of four cards, fresh selector each time | Advancements, Knowledge, Research, Quest open their correct screens | Four screenshots and log |
| S03 | Physical mouse just inside/outside each card edge and Close edge, scales 1–4 | Inside activates correct target; outside does not; Close closes | Cursor-position captures |
| S04 | Physical mouse hover then Tab/Shift+Tab and arrows | Hover and keyboard focus are distinct and visible; focus order is usable | Video at two scales |
| S05 | Physical Enter/Space on focused cards; Escape and Close | Same destinations as mouse; Escape/Close leave no stuck input | Video and log |
| S06 | Physical resize and fullscreen toggle, then repeat S02/S03 | Render and hitboxes remain aligned; no clipped cards | Before/after captures |
| S07 | Advancements card and Back/Escape | Actual vanilla Advancements content opens and remains accessible | Screen capture |
| K01 | Knowledge: mouse through all seven disciplines | Correct populated tree for each; sector and tree Back route correctly | Seven captures |
| K02 | Knowledge: physical node click and tooltip/details | Correct node, achieved state and approved detail fields; no XP/material debit | Capture and XP ledger |
| K03 | Knowledge: reconnect and data sync | Same unlock state and selection behavior after reconnect | Before/after logs |
| R01 | Research: mouse through all seven disciplines | Correct populated tree and details in each; Back chain correct | Seven captures |
| R02 | Research: eligible node click; prerequisite, cost and confirmation/cancellation behavior under current Research authority | Current approved one-click/confirmation rule is followed; server validates and debits exact cost once | Client/server logs and XP ledger |
| R03 | Research: ineligible/max-level/repeat clicks | No unauthorized purchase or debit; visible state updates | Server result and captures |
| R04 | Research: reconnect and fresh sync | Purchased level, availability, cost and XP display agree with server | Before/after logs |
| Q01 | Quest chapter index and sub-branch selection | Correct chapter/branch/node mapping; no new quest registered | Video, ID and catalog count |
| Q02 | Physical wheel across semantic zoom boundaries in both directions | Index/sub-branch/full-node levels change at designed thresholds without flicker at hysteresis boundaries | Video with zoom values |
| Q03 | Click nodes near viewport and tooltip edges at scales 1–4 | Correct node selected; tooltip remains readable and within available space | Cursor and tooltip captures |
| Q04 | Press-drag from node and blank canvas; click without drag | Drag pans without claiming; click selects and only eligible click claims | Video and request log |
| Q05 | Tooltip wheel, Shift+Up/Down, PageUp/PageDown, arrows, Enter/Space, Escape/Back | Scroll and focus actions work; no unintended claim or trapped screen | Video and request log |
| Q06 | Complete a quest with no immediate claim | Completion toast appears once; completed-unclaimed indicator appears; Quest XP remains unchanged | Capture and XP ledger |
| Q07 | Inspect not-completed, completed-unclaimed, claimed nodes | All three states are distinct; incomplete node cannot claim | Three captures |
| Q08 | Physically claim completed-unclaimed node once | Exactly one server result and one exact raw-XP grant; claimed indicator updates | Paired logs and XP ledger |
| Q09 | Repeat click, double-click, Enter and duplicate packet after Q08 | Server rejects repeats; no second XP grant | Server result and XP ledger |
| Q10 | Hold completed reward; relog, die/respawn, change dimension, restart server; then claim | Unclaimed reward persists and remains claimable once at each state boundary | Save/reconnect logs |
| Q11 | Repeat Q10 after claiming | Claimed state persists and never repays | Save/reconnect logs |
| Q12 | Two real clients/players with different progress | Progress, claims and rewards stay per player; no cross-credit | Both client and server logs |
| Q13 | Delay, duplicate and stale/reordered sync or request in controlled test | Pending clears/retries only as intended; server state wins; no double payment | Packet/order log and state |
| Q14 | Before/after every claim and deduction | Raw XP, level, progress and `totalExperience` agree exactly on server and client | Numeric ledger |
| Q15 | Scales 1–4, English/Korean, resize/fullscreen | Card labels, chapters, tooltips and indicators fit without obscuring target input | Paired captures |
| Q16 | Disconnect during pending claim; reopen after reconnect | Final server state reconciles; pending state does not falsely pay or block forever | Client/server logs |

Use a disposable world for state-changing cases. For Q10/Q11 preserve a backup and identify whether the result proves clean restart only; it does not prove power-loss durability. Q13 requires an explicitly controlled test fixture and must be BLOCKED if unavailable.

## Windows owner-run sequence

1. Record both source commits and JAR SHA-256 values; remove duplicate mod JARs. Save environment details above.
2. Run S01–S07 with **real OS mouse and keyboard** at scales 1–4, windowed and fullscreen. Save failures with cursor position and `latest.log` around `SELECTOR_MOUSE_EVENT`, `SELECTOR_INPUT_DISPATCH`, and `SELECTOR_SCREEN_TRANSITION`.
3. Run K01–K03 and R01–R04. Record exact node IDs and server XP before/after. Follow the current Research interaction authority when checking purchase/cancel behavior.
4. Run Q01–Q09 on a known completed, unclaimed quest. Record the quest ID and numeric raw-XP ledger. Test drag before claim.
5. Run Q10–Q16 in the backed-up world with two clients where needed. Keep separate evidence for clean restart, multiplayer and controlled stale-packet tests.
6. Mark each row PASS, FAIL or BLOCKED with physical-input YES/NO. Submit captures/logs and an owner visual verdict separately.

## Independent acceptance gates

| Gate | Current state from 2026-10-24 decision | Evidence needed to pass |
|---|---|---|
| DESIGN_APPROVED | **PASS — architecture only** | Dated owner amendment in current authority |
| IMPLEMENTED | NOT ESTABLISHED by this decision | Source review against the accepted design and exact candidate SHA |
| CI_VERIFIED | NOT ESTABLISHED by this decision | Passing relevant CI on exact candidate SHA |
| PHYSICAL_INPUT_VERIFIED | NOT ESTABLISHED | S01–S07 and relevant K/R/Q cases with real OS input |
| MULTIPLAYER_VERIFIED | NOT ESTABLISHED | Q12 with two real players and server record |
| PERSISTENCE_VERIFIED | NOT ESTABLISHED | Q10/Q11 with relog, death, dimension and server restart |
| OWNER_VISUAL_ACCEPTED | NOT ESTABLISHED | Owner review of actual client captures and interaction |
| RELEASE_READY | NOT ESTABLISHED | All required gates plus content/registration and defect decisions |

## Outstanding-risk ledger

| Risk | Known evidence | Closure condition |
|---|---|---|
| Historical selector clicks produced no transition | Earlier branches failed; #156 callbacks pass but physical OS input was not run | Physical S01–S07 on exact candidate, with failing boundary identified if reproduced |
| Small GUI scales, resize and fullscreen | Hitboxes use a common scaled image transform; no physical scale 1–4 evidence | S03, S06, Q03 and Q15 |
| Quest architecture was formerly in conflict with authority | Owner approved custom architecture on 2026-10-24 | Authority amendment recorded; still run interactions |
| Claim once under delayed or duplicate input | Server claim-once and exact-XP tests reported on #156; physical and reordered traffic not established | Q08/Q09/Q13/Q14 |
| Quest persistence and migration | #156 clean restart evidence; old-save and power-loss behavior not established here | Q10/Q11 and separately approved migration/power-loss scope |
| Research/Knowledge client paths | #156 callback navigation evidence, physical input pending | K01–K03, R01–R04 |
| 200-slot presentation versus content release | #156 has 75 registered quests; 125 remain unregistered | Preserve count unless separately approved and individually validated |
