# MoreMinecrafts — 장신구 시스템 추가 결정 및 +10 강화표 (2026-10-17)

**Status: user-approved design record, documentation only.** This supplements `docs/ACCESSORY_RELIC_SYSTEM_DECISIONS_2026-10-16.md`. Later user decisions here supersede conflicting "proposed/unresolved" labels in that older document. **No gameplay implementation, loot tables, new research nodes, or UI asset work authorized.**

## Confirmed equipment and UX contract

- Exactly **five category-specific accessory slots**, one each: 전투 / 방어·생존 / 지원·회복 / 탐험·이동 / 생활·채집. An accessory fits only its assigned category. No two of one category unless slots are explicitly expanded in a later decision.
- Five accessory slots appear in the **normal player inventory** (no separate accessory screen). Precise layout TBD.
- All five equipped accessory icons appear vertically by the hotbar/offhand region; cooldown overlay resembles vanilla pearl/chorus cooldown. Final HUD positioning/rendering TBD.
- Only **combat** accessories may be manually activated. There is only one combat slot, so at most one manual active accessory at a time. A dedicated activation key is planned. Other categories are passive or conditionally automatic.
- Combat: 별의 계약 (conditional passive), 피의 계약, 환영의 거울, 시간의 고리, 중력의 심장, 영혼의 사슬 (last five manual). Defense: 잿불 반지, 생명의 고리. Support: 생명의 씨앗. Exploration: 설원의 부적, 심해의 진주. Lifestyle: 바다의 속삭임.
- 생명의 씨앗 activates automatically when **wearer's own health reaches ≤40%**. Edge-trigger behavior to avoid repeated triggering while staying low is a technical recommendation, not separately approved.
- 환영의 거울 summons a **physical damageable sword-melee clone**, visually copying wearer/armor/sword; clone-only translucent rendering, ~55% opacity target, fade in/out. Mannequin rendering feasibility, shaders, and final opacity are untested. Restrictions from original roster remain.
- 시간의 고리 uses **genuine entity time slowing** (mob movement, AI, attacks, animations, projectiles) while wearer remains normal; compatibility/MP/boss rules remain unresolved.
- 중력의 심장 creates a pull field **centered on aimed point**, not wearer.
- 영혼의 사슬 automatically links nearby enemies around aimed enemy. **Damage spread is a fixed percentage of the original direct hit per other linked target** (up to 40% at +10), not a recursively chained or split pool. No propagation of propagated damage, and prevent AoE double-counting. Link count includes initial target. At +10 a direct hit of 20 deals 8 to each other linked target. This is intentionally allowed to scale with number of targets.

## Enhancement: approved structure

- All **12 accessories** have independent **+0 to +10** deterministic growth tables below. These tables are approved as the current balancing direction; gameplay validation may motivate later explicit amendments.
- **100% success**; no failure, downgrade, or destruction.
- Reuse **existing anvil** for T1/T2 equipment and accessories. Proposed UI adds a third input slot: target equipment/accessory + existing Enhancement Stone + accessory-only **Relic Catalyst**; one output slot. T1/T2 equipment does not require catalyst. UI layout/visual design **deferred**; adding an input slot also requires ScreenHandler/server synchronization, not merely a texture change.
- **One dedicated accessory catalyst item type** (working name 유물 촉매); it is required **only at +5 and +10**. One catalyst at each milestone is the current quantity assumption; user explicitly approved milestone-only requirement, not necessarily final quantity.
- Current cost alignment with approved T1/T2 raw-XP enhancement: +1…+5 each 40 raw XP, +6…+10 each 70 raw XP; one Enhancement Stone each ordinary step, **two** at +5 and +10; catalyst one at +5 and +10 as above. Total per accessory **550 raw XP, 12 stones, 2 catalysts** (catalyst count provisional). Across all 12: 6,600 raw XP, 144 stones, 24 catalysts (if one each).
- Deduct exact raw XP, not whole XP levels. Must be server-authoritative and **transactional** (XP, stones, catalyst, enhancement and inventory rollback on any failure). Existing equipment enhancement uses the same 40/70 raw-XP bands through +10, and +20 max with higher bands; do not modify equipment rules.
- Catalyst acquisition **NOT designed**. Specific dungeon/trial/boss structures, drop tables, loot percentages, renewal, rarity, and distribution are all **deferred until dungeon system design**. Do not implement guessed loot.

## Per-accessory +0…+10 growth tables

Rows use the order **+0,+1,+2,+3,+4,+5,+6,+7,+8,+9,+10**. Units and labels in each table are normative for current balancing; the cooldowns not listed as scaling remain unchanged from the existing design (some are still proposed).

### 전투

**환영의 거울** — clone HP (% wearer): `50,60,60,60,70,70,70,80,80,90,90`; melee damage (% wearer baseline): `40,40,45,45,50,50,55,55,60,60,65`; duration (sec): `12,12,12,13,13,14,14,15,15,15,16`. Cooldown candidate 120s.

**시간의 고리** — radius (blocks): `5,5.5,5.5,6,6,6.5,6.5,7,7,7.5,8`; duration (sec): `3,3,3.5,3.5,4,4,4.5,4.5,5,5,5`; target entity time progression **20% fixed**. Cooldown candidate 180s. Exact entity tick simulation unverified.

**피의 계약** — damage bonus (%): `35,37.5,37.5,40,40,42.5,42.5,45,47.5,47.5,50`; duration (sec): `12,12,13,13,14,14,15,15,15,16,16`. **Approved invariant:** activation consumes 30% current HP as unmitigable cost, natural regeneration disabled, cooldown 90s.

**중력의 심장** — pull radius (blocks): `5,5,5.5,5.5,6,6,6,6.5,7,7.5,8`; duration (sec): `4,4,4,4.5,4.5,4.5,5,5,5.5,5.5,6`; target cap: `12,14,14,14,14,16,16,16,16,18,18`. Candidate cooldown 100s; boss rules unverified.

**별의 계약** — melee damage bonus (%): `8,9,10,10,11,12,13,13,14,15,16`; HP activation threshold (% maximum HP, at or below): `50,50,50,55,55,55,55,60,60,60,60`. Conditional passive.

**영혼의 사슬** — max linked mobs incl. primary: `3,4,4,4,5,5,6,6,7,7,8`; fixed original direct-hit propagation (%) **per each other target**: `25,25,28,28,28,32,32,32,32,36,40`; link reach (blocks): `8,8,8,10,10,10,10,12,12,12,12`. Base duration candidate 8s; cooldown candidate 90s. Auto-link around aimed enemy; no recursive spread. Previous research-based link-cap growth idea is superseded by enhancement.

### 방어·생존

**잿불 반지** — post-fire-hit fire damage reduction (%): `40,45,45,50,50,55,55,60,60,65,65`; protection duration (sec): `3,3,3.4,3.4,3.8,3.8,4.2,4.2,4.6,4.6,5`. Cooldown 30s.

**생명의 고리** — Regeneration I duration (sec): `4,4.5,4.5,5,5,5.5,5.5,6,6,6.5,7`; cooldown (sec): `120,120,115,115,110,110,105,105,100,100,100`. Trigger HP ≤25% fixed; Regeneration level remains I.

### 지원·회복

**생명의 씨앗** — heal amount (HP per 2 sec): `3,3,3,4,4,4,4,4,5,5,5`; radius (blocks): `5,5.5,5.5,5.5,6,6,6.5,6.5,6.5,7,7`; duration (sec): `12,12,13,13,13,14,14,15,15,15,16`. Wearer HP ≤40% automatic activation fixed. Existing proposals of initial +4 absorption HP, ally/tamed-animal targeting, 150s cooldown and nonstacking need validation. **Balance watch:** +10 5HP/2sec for 16sec can restore up to 40HP per affected target depending on tick timing.

### 탐험·이동

**설원의 부적** — powder snow sinking speed reduction (%): `70,75,75,80,80,85,85,90,90,95,95`; **additional** slipping reduction relative to baseline accessory effect (%): `0,0,4,4,8,8,12,12,16,16,20`. Baseline absolute ice-slip formula still TBD.

**심해의 진주** — water breathing (sec): `8,9,9,11,11,13,13,14,14,16,16`; cooldown (sec): `45,45,42,42,39,39,36,36,33,33,30`. Trigger remains crouch motionless underwater for 2 sec.

### 생활·채집

**바다의 속삭임** — fishing minigame success-zone expansion (%): `10,12,12,14,16,16,18,20,22,24,25`; additional input timing tolerance (sec): `0,0,0.05,0.05,0.05,0.10,0.10,0.10,0.10,0.10,0.10`. No fish rarity/size/weight changes. Actual minigame timing architecture must be inspected; timing-tolerance stat can be replaced with equivalent fair assistance if needed.

## Explicitly deferred / acceptance notes

1. **Do not implement or assign catalyst loot yet.** Dungeon/trial/boss system not finalized. Catalyst name/art/recipe and drop rates not final.
2. No final visual UI layout yet for anvil/inventory/HUD. User may redesign **both existing enhancement UI and accessory enhancement UI** later.
3. Time slowing and physical translucent clone require feasibility tests, especially multiplayer and renderer compatibility.
4. Verify server-side transaction and rollback at every mutation point; enforce category slot validation and active-only combat category.
5. Do not silently alter original approved accessory behavior or T1/T2 enhancement rules; record later revisions explicitly.
