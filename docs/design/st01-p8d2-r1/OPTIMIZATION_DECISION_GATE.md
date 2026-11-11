# ST-01 P8-D2-R1 — Optimization decision gate (recommendation only; nothing implemented)

| Option | Evidence-based assessment | Recommendation |
|---|---|---|
| **R1 Filter-order optimization** | Demonstrated *within* eligibility (spread-first sweep, height-range early exit): exact verdicts, 2.9× less eligibility work. Stage order itself is already right. | **Yes, as part of R2** (it is a change to how eligibility is computed, not to the stage order) |
| **R2 Eligibility computation optimization** | Terrain sampling is 97.8 % of the time. Exact-equivalent early exit removes most work for terrain-failing candidates; cannot reduce the ≈ 2.7 s an eligible candidate needs (its median needs all 6,561 heights) and does not remove server-thread blocking. Would touch production `St01Eligibility` (same verdicts, partial diagnostic numbers for early rejects) — **requires owner approval and the P8-C2-C regression suite**. | **Recommended (secondary)** |
| **R3 Search orchestration / caching** | The remaining floor (≈ 6 s for any found target: 2 × full evaluation) and all server-thread blocking can only be removed by not doing the work on demand: reuse the first evaluation (avoid H5), prepare off the request path, and cache **validated** starts (a warm lookup costs 0.2 ms). P8-D2 showed safe non-blocking preparation is not yet demonstrated. | **Recommended (primary)** |
| **R4 Alternative target selection** | If any valid pyramid in range is acceptable (owner: a map may point to a distant valid pyramid), a request can be served from already-validated starts instead of a fresh nearest search. Relaxing "nearest" **alone** does not reduce cost of an on-demand search: vanilla already stops at the first ring with a success and returns the first valid start in scan order (not the exact nearest); any target must still be validated (≈ 6 s cold). It helps **only together with R3** (choose among cached, validated starts). | **Yes, combined with R3** |
| **R5 No change** | The data show material, correctness-preserving gains are available. | not recommended |

## Does relaxing nearest-target selection help?
- **On its own: no.** The measured cost is (a) cheap rejections up to the first success and (b) the success's own full evaluation, twice. "Any valid target" still requires finding and validating one; with vanilla's ring order the first success is already what is returned.
- **With a cache of validated starts: yes, decisively** (0.2 ms warm vs 6–29 s cold). The cache must store only starts that passed the production `isValid()` check, never candidate anchors.

## Correctness guard rails for any later implementation
- Never return a coordinate that has not passed the production start validation (no false positives); the cheap-first variant must stay an exact reordering of production predicates (no simplified locator).
- Keep the approved placement rules unchanged (81×81 Desert, 16/10/8, 32/8 TRIANGULAR 91401, Policy D, overlap permitted).
- Any change to `St01Eligibility` must re-run the P8-C2-C 20-seed generation and assembly suite.

## Status
Stop here. No optimization is implemented; R2/R3/R4 require explicit owner approval and a separate implementation phase.
