# Bounty & Commissions v1.0 — Approved Design (2026-10-19)

**State:** User-approved architecture; **not implemented and not gameplay tested** by this PR. Distinct from rejected territorial bulk-sales contracts and distinct from the existing one-time XP quest / advancement system. Companion checkpoint: `TERRITORY_ECONOMY_APPROVED_2026-10-19.md`.

## 1. Access / interaction [APPROVED]
- Add **의뢰 / Commissions** entry to the existing MM advancement/progression UI. **No bulletin board or required NPC interaction.**
- Four commission types: **Kill**, **Delivery**, **Exploration**, **Special Bounty**.
- **Special bounty contract items** are obtained from the already-designed *rare merchant caravan*. Player examines the **exact objective(s) and exact payout before purchase**. Holding the contract in gameplay and **right-clicking** activates it.
- Special-contract item is an exception to the no-contracts rule in the *territory economy*: it belongs to this separately approved bounty system.
- Normal commissions: shared **world-wide six-offer board** consisting of **2 kill, 2 delivery, 2 exploration**. The shared offer set rotates **every three in-game days**.
- Each player's **accepted progress and completion** are private to that player; same world offer can be accepted by different players without shared credit.
- Normal simultaneous active limit **3**. Special active limit **1**, tracked separately.
- No duplicate payout or repeated completion of the same accepted instance.

## 2. Generation/difficulty [APPROVED]
- Normal offer difficulty mix: **novice 45%**, **medium 35%**, **hard 20%**.
- Conditions per generated offer: novice **1**, medium **1–2**, hard **2–3**.
- Generate legal conditions first, evaluate difficulty/score, assign payout. Do not allow impossible target or structurally invalid condition combinations.
- The approved payout *bands* are in **emerald blocks**:
  - novice: **2–4**
  - medium: **5–9**
  - hard: **10–18**
- Score→payout mapping, target weights, item counts, and precise difficulty thresholds require later balance selection. Avoid converting placeholder numbers into fixed design.

## 3. Progress rules [APPROVED]
- **Kill:** only direct kills performed **after accepting** count. A mercenary's independent kill is **not** a player direct kill by default; do not silently credit pet/guard/mercenary damage or pre-accept kills. Determine exact attribution in implementation tests.
- **Delivery:** player may submit already-owned items; do not demand item acquisition after accepting. Submission must consume inventory exactly once.
- **Exploration:** validate the **specific structure instance**; repeating the same instance cannot generate repeated exploration credit/reward for that same player. Persist instance identity robustly, including multiplayer/server restarts.
- **Special:** procedurally composite objectives with additional challenge. Separate slot and activation from normal commission acceptance.
- Additional late-End/high-tier Shulker killing commissions remain **gated** on the future End structure expansion and End travel-distance mitigation item. Do not enable impossible or punitive long-distance conditions prematurely.

## 4. Special contract tiers [APPROVED CONCEPT / OPEN NUMBERS]
- High / Rare / Legendary special bounty tiers.
- Before purchase, show exact obligations and payout; once acquired, objectives and reward remain **fixed** for that contract instance.
- **Important:** older suggested example price bands (**4/10/20 emerald blocks**) and reward bands (**12–20 / 25–40 / 50–80**) are **NOT finalized approvals**. Treat all special-contract numeric pricing/rewards as **OPEN**.

## 5. Implementation and UI phase gates [APPROVED]
1. **Phase 1:** server-authoritative commission generation/state, progression attribution, persistence, validation and duplicate-payout protection.
2. **Phase 2:** wire one **temporary functional button** in the old advancement UI for backend verification. **Not** a polished redesign.
3. **Phase 3:** inspect the real existing image-backed progression UI implementation, resources, and architecture.
4. **Phase 4:** **redesign the whole progression UI** against verified backends, including commissions. Preserve separate system entitlements and identify conflicts with prior UI authority; no speculative image approximation.
5. **Phase 5:** Minecraft **live validation**, including physical interactions, multiplayer state, persistence, acceptance paths and visuals.
- Never label Phase 2 synthetic button tests as UI live acceptance.
- No claims of CI PASS, live PASS, or merged product code follow from this documents-only checkpoint.

## 6. Test requirements (not yet executed)
- Offer set is exactly 2/2/2 on refresh; refresh does not duplicate accepted rewards.
- Acceptance can survive refresh, server restart, disconnect/reconnect; direct-kill tracking starts only at acceptance.
- Delivery pre-owned stack acceptance and exact atomic subtraction, no inventory race/duplication.
- Structure-instance identity protection across dimension/coords and repeated visits.
- Deterministic reward quote, reward grant idempotency, contract right-click consumes/activates exactly once.
- Special slot independent of three normal slots.
- No activation of End-only content before prerequisites exist.
- Multiplayers view same world offer slots but maintain individual completion.

## 7. Deferred
- Exact normal objective rosters, target count bands, score weights, special contract fees/rewards, localization/UI art and specific End exceptions.
