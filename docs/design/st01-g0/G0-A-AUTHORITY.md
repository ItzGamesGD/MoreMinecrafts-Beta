# G0-A — Authority reconciliation

Status: G0 BLOCKED at two geometry interfaces. This packet does not approve its recommendations.
Base: `03685656f72faf7cd0f1a931dc7519ebc31b7378` (B2). D1: `e229d001be06076b9d199c4e3fd8fd2be790e6b5`.
O-14 inspected at `7e37b786de8fcf36980ec732cc8079bb24e6a7fd`; MM-SG main `61a09eebafa9beee0944eba266e7f05a9543eec5`.
The named C2-A branch exists locally at B2 but has no remote ref; it is not an additional implementation baseline.

## Source notation

Paths below are relative to `docs/design/` unless specified otherwise. Sections are exact headings or table IDs. P1–P6 refer to files in the B2 tree. P8–P11 secret-runtime records and O-14 refer to the pinned O-14 tree; they are not assumed merged into B2. A newer date alone is not supersession evidence.

| Item | Source and section | Current geometry/value | Authority | Dependencies / construction blocker | Resolution proposed |
|---|---|---|---|---|---|
| Envelope | `../ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md`, Invariants | 81×81; aboveground 40 | APPROVED | Does not define cavities/materials | Preserve |
| Layer silhouette | `tools/st01-p1/st01_p1_model.py`, pyramid_mask | Y=0..39, half-width 40−Y | PROPOSED | Exact step profile needs adoption | Adopt formula, no extra exterior projections |
| Entrance/forecourt | P1 conditional approval §1, §3; P1 model SPACES S00–S03 | Source boxes in generated inventory | CONDITIONALLY_APPROVED | Numerical geometry remains provisional | Adopt source boxes after review |
| Grand hall | P1 conditional approval F-1 | S04 X−18..18/Y0..17/Z−6..18 | CONDITIONALLY_APPROVED | Architectural proportion not final | Retain current representative hall |
| Gallery | P1 model ELEMS; P1 conditional approval F-6 | U ledges, supports unresolved | PROPOSED | Support choice unresolved | Two support piers; see G0-B |
| Altar | P1 conditional approval F-2; S08–S10 | S10 X−7..7/Y11..17/Z−20..−12 | CONDITIONALLY_APPROVED | Roof margin 3 at rear top | Retain, no external embellishment |
| Descent/E1 | P1 conditional approval §3; S05–S07 | E1 X8..15/Y−6..−3/Z−13..−7 | CONDITIONALLY_APPROVED | Route and tread details | Freeze model plus source treads |
| Underground room count | P2 conditional approval §2 | Three L1 tombs, three L2 stores | PROPOSED | Counts explicitly not final | Recommend 3+3; no new rooms |
| Underground coordinates | P2 conditional approval §2 | Representative boxes in inventory | CONDITIONALLY_APPROVED | Provisional values not promoted | Freeze candidate-C-adjusted registry as proposal |
| P2 candidate C | P6 user decisions §1 item 3 | Seven adjusted components | APPROVED | Applies to overlay only | Preserve all seven; hash pinned in validation |
| Corridor widths | BUILD Q37–Q42; P2 conditional approval §2 | Main progression 7→5→3 | APPROVED | Local ceiling profile unresolved | Preserve widths and 3-high central passage |
| Corbel ceiling | BUILD Q17; P2 conditional approval P2-F5 | Qualitative stepped-in ceiling | UNSPECIFIED | No complete block profile | Separate profile validation before adoption |
| Wall thickness | P2 engine MIN_WALL; P6 registry dependency | Representative 2 blocks | PROPOSED | Needs full voxel checking | Adopt 2 where compatible; no silent cavity shrink |
| D1 | D1 NBT, P7 approval §1 | 39×20×39 at reference origin (−40,−24,2) | APPROVED | Immutable; overrides P3 reconstruction | Use exact bytes only |
| PRIMARY | P7 approval §1 P3-F6 | (−21,−20,33)/(−20,−20,33), north | APPROVED | No SECONDARY physical alias | Preserve |
| Guardian | P7 approval §1 D-E | (−21,−22,26), authored facing south | APPROVED | No final combat changes | Preserve metadata and B2 semantics |
| Treasury | P4-B conditional adoption §§1–2 | Interior X6..30/Y−30..−19/Z16..36, P4 source profiles | CONDITIONALLY_APPROVED | Final material mapping, orientation | Retain EAST candidate pending interfaces |
| Crypt | P6 open decisions D-G; P4 spec crypt() | 15×13×7; six niches | PROPOSED | Requires explicit approval | Recommend existing representative geometry |
| Secret cover | P6 secret entrance state §1 | X−9..−6/Y−23/Z33..35 | APPROVED | Must conceal descent | Preserve unless owner resolves seam differently |
| Secret descent vs D1 | P6 secret state §1 + actual D1, generated validation | Twelve additional solid cells at Y−24 | CONFLICTING | Immutable blocks vs existing descent air | Owner decision D1-SEAM |
| Secret EAST/WEST | O-14 §1 A.5 + current owner requirement; P6 model_json/mirror | Existing WEST also moves hall/anchor to X+21 | CONFLICTING | Not an independent secret-only variant | Owner decision SECRET-WEST |
| Palette | BUILD Q20/Q27 etc., regional visual descriptions | Sandstone/inlay directions, no complete state map | UNSPECIFIED | Exact visual identity needs approval | Palette proposal in G0-B, not automatic authority |
| Block entities | Actual D1; BUILD Q72–Q93 treasury directions | D1 records available; other chest/pile construction incomplete | UNSPECIFIED | Must not infer new loot or SECONDARY | Preserve D1; resolve treasury construction separately |
| Traversal/seams | P6 component spec and links | Registry connection graph | CONDITIONALLY_APPROVED | Abstract graph ≠ actual-asset route | Full navigation required after seam decisions |
| Depth | P6 open decisions D-H; P11 approval §3 Q98 | Lowest measured structure −32; no new final cap | UNSPECIFIED | Measured value not authority | Recommend explicit −32 design reservation; not adopted |
| C1 generation policy | Current owner's G0 request §3, restating approved C1 | 32/8 TRIANGULAR; height variation12/cut8/skirt6; full pyramid; four rotations; reject unsafe conflicts; footprint ground + entrance check | APPROVED | Algorithms/parameter semantics not implemented here | Preserve; no geometry override implied |
| B2 identity/catalyst | Current owner requirements + B2 RoyalChestRuntime/Data | Dimension + transformed anchor; 5% and conditional 50:50; SECONDARY absent | APPROVED | Future lifecycle remains C2-B/C2-C | No runtime or reward changes |

## Supersession limits

Current user authorization supersedes historical statements withholding implementation permission, but does not approve provisional dimensions. P6 candidate C supersedes only its seven listed component coordinates. P7 approves EAST, PRIMARY B and Guardian 3B. P11 resolves its listed operational settings, not Q15 or Q98. O-14 and current C1 decisions resolve generation policy, not the two newly measured geometry conflicts. The existing full WEST model is comparison evidence, not authority to move D1.

## Exact source path index

- `docs/ST01_BUILD_DECISIONS_Q01_Q08_2026-11-05.md`: Q01–Q100 tables and invariants.
- `docs/design/st01-p1/ST01_P1_CONDITIONAL_APPROVAL_RECORD_2026-11-06.md`: §1, F-1/F-2/F-6, §3.
- `docs/design/st01-p2/ST01_P2_CONDITIONAL_APPROVAL_RECORD_2026-11-06.md`: §2, P2-F5.
- `docs/design/st01-p3/ST01_P3_CONDITIONAL_APPROVAL_RECORD_2026-11-07.md`: royal hall conditions; superseded only where explicit D1/P7 approval applies.
- `docs/design/st01-p4/ST01_P4B_CONDITIONAL_ADOPTION_RECORD_2026-11-07.md`: §§1–2.
- `docs/design/st01-p5/ST01_P5_INTEGRATION_VERIFICATION_2026-11-07.md`: integration evidence, not automatic approval of proposed values.
- `docs/design/st01-p6/ST01_P6_USER_DECISIONS_RECORD_2026-11-07.md`: §1 items1–5.
- `docs/design/st01-p6/ST01_P6_OPEN_DECISIONS_2026-11-07.md`: D-G/D-H.
- `docs/design/st01-p6/ST01_P6_SECRET_ENTRANCE_STATE_SPEC_2026-11-07.md`: §1 geometry.
- `docs/design/st01-p7/ST01_P7_USER_APPROVAL_RECORD_2026-11-08.md`: §1 approved EAST/PRIMARY/Guardian.
- O-14 tree: `docs/design/st01-p8/ST01_P8_USER_APPROVAL_RECORD_2026-11-07.md` §§1,3; P9/P10 context incorporated through P11's explicit approval comparison.
- O-14 tree: `docs/design/st01-p11/ST01_P11_01_USER_APPROVAL_AND_AUTHORITY_SYNC_2026-11-08.md` §§2–3: operational settings, unchanged Q15/Q98 boundary.
- O-14 tree: `docs/design/st01-o14/ST01_O14_01_NATURAL_WORLDGEN_FEASIBILITY_STUDY_2026-11-09.md` §1 A: secret-area choice and post-placement boundary. Current owner's explicit C1 numbers supersede its unapproved parameter entries only.

The packet does not claim exhaustive resolution of all historical design records. In particular, final construction prescriptions and complete source-to-block mapping remain incomplete because the two interfaces conflict with current constraints.
