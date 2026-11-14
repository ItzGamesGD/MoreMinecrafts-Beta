# ST-01 P11 산출물 6 — O-9 원소 피해원 귀속 감사 (O-9 Elemental Damage Source Attribution Audit)

- **작성일:** 2026-11-08
- **범위:** 작업 D — T2 원소 무기 및 각종 피해 유형의 `TombVictoryPolicy` 귀속 전수 조사 및 검증 (**조사·검증 전용, 운영 승리 판정 정책 변경 없음**)

---

## 1. 감사 원칙 및 현재 운영 승리 판정 코드 구조

1. **운영 정책 무변경 원칙 준수:**
   - 본 감사는 조사 및 검증만 수행하며, `TombVictoryPolicy`의 운영 로직(방식 A: 구조적 최소 조건, `Params = null`)을 방식 B나 미승인 피해 비율·참여 시간 창으로 변경하지 않았다.
2. **현재 P9–P11 운영 승리 판정 메커니즘 (`TombVictoryPolicy.java` & `TombGuardian.java`):**
   - **비치명 피해 기록 (`ServerLivingEntityEvents.AFTER_DAMAGE`):**
     - `TombGuardianEncounters.initialize()`에서 `!blocked && taken > 0.0F && guardian.isAlive() && !guardian.isDeadOrDying()`일 때 `guardian.recordDamage(source, taken)` 호출.
     - `TombVictoryPolicy.attributedPlayer(source)`가 `ServerPlayer`를 반환하면 `Participation.recordHit(player.getUUID(), taken, gameTime)`에 누적.
   - **치명타(Killing Blow) 귀속 및 최종 판정 (`TombGuardian.die(DamageSource source)`):**
     - `AFTER_DAMAGE` 이벤트는 엔티티가 사망한 틱(`!guardian.isAlive()`)에는 호출되지 않거나 `die()` 이후에 실행되므로, `TombGuardian.die(source)` 내부에서 `participation.foldKillingBlow(TombVictoryPolicy.attributedPlayer(source), getMaxHealth(), level.getGameTime())`로 치명타를 합산한 뒤 `participation.judge(TombVictoryPolicy.classify(source), level.getGameTime(), TombVictoryPolicy.currentParams())`를 호출한다.
   - **귀속 및 분류 함수 (`TombVictoryPolicy.java:121-132`):**
     ```java
     public static ServerPlayer attributedPlayer(DamageSource source) {
         Entity cause = source.getEntity();
         if (cause instanceof ServerPlayer player) return player;
         if (cause instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer owner) return owner;
         return null;
     }
     public static DeathCause classify(DamageSource source) {
         if (source.is(DamageTypes.GENERIC_KILL)) return DeathCause.COMMAND_KILL;
         if (attributedPlayer(source) != null) return DeathCause.PLAYER;
         if (source.getEntity() != null) return DeathCause.MOB;
         return DeathCause.ENVIRONMENT;
     }
     ```
   - **방식 A(`Params == null`)에서의 판정 (`TombVictoryPolicy.java:83-88`):**
     - `cause == DeathCause.PLAYER` (즉, **치명타의 `DamageSource`에서 `attributedPlayer(source) != null`**)이고 `!parts.isEmpty()`이면 **유효 승리(`valid = true`)**, 그렇지 않으면 **무효 사망(`valid = false`, 보상 드롭 억제, 상자 봉인 유지, 비밀 입구 `SEALED` 유지, 보스 재도전 허용)**.

---

## 2. T2 원소 무기 및 전체 피해 유형별 귀속 감사 매트릭스

저장소 내 모든 T2 원소 무기(`src/main/java/dev/itzgamesgd/moreminecrafts/special/`), 일반/하위 장비(`equipment/`), 연금술 상태 효과(`alchemy/`), 인챈트(`mixin/BookOption*`), 장신구(`accessory/`) 및 바닐라 피해원을 전수 조사한 결과는 다음과 같다.

| 분류 | 세부 공격 / 피해 유형 | 피해 발생 코드 위치 | Minecraft `DamageSource` / `DamageType` | `getDirectEntity()` (직접 엔티티) | `getEntity()` (공격 주체) | 소유자 추적 가능 여부 (`attributedPlayer`) | 실제 P9/P11 승리 판정 결과 (`classify` / 치명타 시) | 문제 여부 (위음성 / 위양성) | 검증 상태 |
|---|---|---|---|---|---|---|---|---|---|
| **1. 플레이어 직접 공격** | 바닐라 근접 공격 및 모든 커스텀 근접 무기 기본 타격 (`FlameSwordItem`, `WindSwordItem`, `ElementalSwordItem`, `EarthMaceItem`, `Dagger`, `Greatsword`, `Gauntlet`) | 바닐라 `Player.attack()` | `damageSources().playerAttack(player)` (`minecraft:player_attack`) | `ServerPlayer` | `ServerPlayer` | **가능** (`cause instanceof ServerPlayer`) | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **1. 플레이어 직접 공격** | 철퇴 강타 (`EarthMaceItem` / 바닐라 `MaceItem` 낙하 스매시) | 바닐라 `MaceItem` | `damageSources().mace(player)` (`minecraft:mace_smash`) | `ServerPlayer` | `ServerPlayer` | **가능** (`cause instanceof ServerPlayer`) | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **1. 플레이어 직접 공격** | 대검 우클릭 휩쓸기 (`GreatswordItem.use`) 및 건틀릿 후방 충격 (`GauntletItem.hurtEnemy`) | `GreatswordItem.java:37-42`, `GauntletItem.java:27` | `damageSources().playerAttack(attacker)` | `ServerPlayer` | `ServerPlayer` | **가능** (`cause instanceof ServerPlayer`) | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **CODE AUDITED** |
| **2. 플레이어 소유 투사체** | 일반 활 및 T2 **`WIND_ARCHERS_BOW`** (`ModItems.WIND_ARCHERS_BOW`, 바닐라 `BowItem` + `ProjectileWeaponItemMixin` 탄속/피해 보너스) | 바닐라 `AbstractArrow.onHitEntity()`, `ProjectileWeaponItemMixin.java:41` | `damageSources().arrow(arrow, owner)` (`minecraft:arrow`) | `Arrow` / `SpectralArrow` (`Projectile` implements `TraceableEntity`, `setOwner(player)`) | `ServerPlayer` (`arrow.getOwner()`) | **가능** (`source.getEntity()`가 `arrow.getOwner()`인 `ServerPlayer` 반환) | `PLAYER` → **유효 승리 (`VALID`)** | 정상 (화살 자체가 치명타일 때) | **GAMETEST VERIFIED** |
| **2. 플레이어 소유 투사체** | 삼지창(`Trident`), 쇠뇌 화살/폭죽(`FireworkRocketEntity`), 투척 물약(`ThrownPotion`) 등 `Projectile` | 바닐라 `Projectile` 서브클래스 | `trident(trident, owner)`, `fireworks(rocket, owner)`, `indirectMagic(potion, owner)` | 해당 `Projectile` | `ServerPlayer` (`projectile.getOwner()`) | **가능** (`source.getEntity() == ServerPlayer`) | `PLAYER` → **유효 승리 (`VALID`)** | 정상 (투사체 직격/즉시 피해일 때) | **GAMETEST VERIFIED** |
| **2. 플레이어 소유 소환수** | 길들인 늑대(`Wolf`) 등 `TamableAnimal` / `OwnableEntity` | 바닐라 `Mob.doHurtTarget()` | `damageSources().mobAttack(wolf)` (`minecraft:mob_attack`) | `Wolf` (`OwnableEntity`) | `Wolf` (`wolf.getOwner() == ServerPlayer`) | **가능** (`cause instanceof OwnableEntity` → `getOwner()`) | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **3. 화염 및 지속 화상 (T2 화염검 등)** | **T2 화염검(`FlameSwordItem`)** 10타 패시브 화염 휩쓸기 (`FlameSwordEffects.sweep`, 3.5 피해) | `FlameSwordEffects.java:34` | `attacker.damageSources().playerAttack(attacker)` | `ServerPlayer` | `ServerPlayer` | **가능** | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **3. 화염 및 지속 화상 (T2 화염검 등)** | **T2 화염검(`FlameSwordItem`)** 우클릭 액티브 `FlameCleaveAbility` **직격 피해** (10.0 피해) | `FlameCleaveAbility.java:51` | `player.damageSources().playerAttack(player)` | `ServerPlayer` | `ServerPlayer` | **가능** | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **3. 화염 및 지속 화상 (T2 화염검 등)** | **T2 화염검(`FlameSwordItem`)** 우클릭 액티브 `FlameCleaveAbility` **3초 점화(`target.igniteForSeconds(3.0F)`) 이후 화상 DOT** 및 바닐라/T1 `Fire Aspect`·`Flame` 인챈트(`BookOptionIgniteMixin`) 화상 DOT | `FlameCleaveAbility.java:52`, `BookOptionIgniteMixin.java:15`, 바닐라 `Entity.baseTick()` | `damageSources().onFire()` (`minecraft:on_fire`) | `null` | `null` | **불가** (`Entity.remainingFireTicks`에 점화 주체 미저장; 단 `LivingEntity.getKillCredit()`에는 최근 공격 플레이어가 남을 수 있음) | `ENVIRONMENT` → **무효 사망 (`INVALID_DEATH`)** | **[핵심 결함 F-1: 위음성(False Negative)]** 플레이어가 화염검 스킬이나 화염 인챈트 무기로 보스 체력의 99%를 깎고 마지막 1 HP가 화상 DOT로 깎여 죽으면 **무효 사망 처리(보상 없음, 상자 봉인 유지, 비밀 입구 미개방, 보스 리셋)** | **GAMETEST VERIFIED** (`o9ElementalBurnDotFalseNegativeAndLastHitFalsePositiveAudit`) |
| **4. 독·위더·연금술 상태 효과** | 바닐라 독(`MobEffects.POISON`, 체력 1까지만 감소) / 위더(`MobEffects.WITHER`, 치명 가능) | 바닐라 `PoisonMobEffect`, `WitherMobEffect` | `damageSources().magic()` / `damageSources().wither()` | `null` | `null` | **불가** (`MobEffectInstance` 적용 주체가 tick 피해 `DamageSource`에 전달되지 않음) | `ENVIRONMENT` → **무효 사망 (`INVALID_DEATH`)** | **[위음성 F-2]** 플레이어가 부여한 위더/지속 상태 효과 틱이 마지막 1 HP를 깎으면 무효 사망 처리 | **GAMETEST VERIFIED** |
| **4. 독·위더·연금술 상태 효과** | 커스텀 감전 물약 효과 **`ShockMobEffect` (`ModAlchemy.SHOCK`)** 주기적 펄스 피해 (`PULSE_DAMAGE`) | `ShockMobEffect.java:26` (`entity.hurt(level.damageSources().magic(), ShockTiming.PULSE_DAMAGE)`) | `level.damageSources().magic()` (`minecraft:magic`) | `null` | `null` | **불가** (`damageSources().magic()`에 투척 플레이어 엔티티가 없음) | `ENVIRONMENT` → **무효 사망 (`INVALID_DEATH`)** | **[위음성 F-2]** 플레이어가 감전 물약을 던져 부여한 `ShockMobEffect` 펄스가 치명타가 되면 무효 사망 처리 | **GAMETEST VERIFIED** |
| **5. 번개 피해** | **T2 번개검(`LIGHTNING_SWORD`)** 5타 패시브 보너스(+4.0) 및 우클릭 액티브 `LightningSlash`(8.0 대시 베기) | `ElementalSwordItem.java:38-44, 121` | `player.damageSources().playerAttack(player)` (번개 엔티티를 소환하지 않고 `T2ElementalVfx` 파티클만 생성) | `ServerPlayer` | `ServerPlayer` | **가능** | `PLAYER` → **유효 승리 (`VALID`)** | 정상 (T2 번개검은 실제 `LightningBolt` 엔티티를 쓰지 않고 `playerAttack`을 사용하므로 안전) | **GAMETEST VERIFIED** |
| **5. 번개 피해** | 바닐라 `LightningBolt` 엔티티 (자연 낙뢰 또는 삼지창 `Channeling` 등 `bolt.setCause(ServerPlayer)`가 설정된 낙뢰) | 바닐라 `Entity.thunderHit()` | `damageSources().lightningBolt()` 또는 `source(LIGHTNING_BOLT, bolt)` | `null` 또는 `LightningBolt` | `null` 또는 `LightningBolt` | **현재 코드상 불가** (`LightningBolt`는 `OwnableEntity`를 구현하지 않고 별도의 `getCause()` 메서드를 가짐!) | `ENVIRONMENT` (원인 없음) 또는 `MOB` (`getEntity() == bolt`) → **무효 사망 (`INVALID_DEATH`)** | **[위음성 F-3]** `LightningBolt.setCause(ServerPlayer)`가 설정된 플레이어 유발 낙뢰라도 `LightningBolt`가 `OwnableEntity`가 아니므로 `attributedPlayer`가 `null`을 반환해 무효 사망 처리됨 (그 외 낙뢰가 붙인 바닥 불/화상도 `inFire`/`onFire`로 `ENVIRONMENT`) | **GAMETEST VERIFIED** |
| **6. 기타 T2 원소 특수 피해** | **T2 바람검(`WindSwordItem`)** 10타 패시브 휩쓸기(3.0) 및 우클릭 액티브 `WindBladeAbility` 원거리 검기(5.0) | `WindSwordItem.java:38`, `WindBladeAbility.java:47` | `player.damageSources().playerAttack(player)` | `ServerPlayer` | `ServerPlayer` | **가능** | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **6. 기타 T2 원소 특수 피해** | **T2 유수검(`FLOWING_WATER_SWORD`)** 우클릭 액티브 `WaterDiveRuntime` 낙하 수주 폭발(7.0 AoE) | `WaterDiveRuntime.java:106` | `player.damageSources().playerAttack(player)` | `ServerPlayer` | `ServerPlayer` | **가능** | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **6. 기타 T2 원소 특수 피해** | **T2 대지 철퇴(`EarthMaceItem`)** 패시브 지진파(`EarthSeismicRuntime`, 3.0) 및 우클릭 액티브 `EarthShatter` 전방 원뿔 분쇄(6.0) | `EarthSeismicRuntime.java:75`, `EarthMaceItem.java:64` | `player.damageSources().playerAttack(player)` | `ServerPlayer` | `ServerPlayer` | **가능** | `PLAYER` → **유효 승리 (`VALID`)** | 정상 | **GAMETEST VERIFIED** |
| **6. 기타 T2 원소 특수 피해** | **T2 암흑 에너지 지팡이(`DarkEnergyStaffItem`)** | `DarkEnergyStaffItem.java:10` | 일반 `Item` 플레이스홀더 (특수 발사체/스킬 미구현, 기본 평타만 존재) | `ServerPlayer` | `ServerPlayer` | **가능** (평타 기준) | `PLAYER` → **유효 승리 (`VALID`)** | 향후 지팡이 투사체/DOT 구현 시 `playerAttack` 또는 `OwnableEntity` 귀속 필수 | **CODE AUDITED** |
| **6. 기타 장비 효과** | T1 장신구 충격파(`T1Shockwave.java:39`) | `T1Shockwave.java:39` | `knockback(..., playerAttack(player), 0)` (피해량 0, 넉백 전용) | — | — | 피해 없음 | 사망 원인 아님 | — | **CODE AUDITED** |
| **7. 환경 피해** | 용암(`lava`), 불 블록(`inFire`), 낙하(`fall`), 선인장(`cactus`), 동상(`freeze`), 익사(`drown`), 공허(`fellOutOfWorld`), 주인 없는 폭발(`explosion(null, null)`) | 바닐라 환경 피해 | 각 환경 `DamageSource` | `null` | `null` | 해당 없음 (`null`) | `ENVIRONMENT` → **무효 사망 (`INVALID_DEATH`)** | **[경계 사례 P-1: 환경 99% + 플레이어 1타 막타]** 용암/환경이 보스 체력의 99.5%(199 HP)를 깎은 뒤 플레이어가 마지막 1 HP만 때려 죽이면, 현재 방식 A(`Params == null`, 최소 피해 비율 미승인)에서는 **유효 승리로 인정됨** | **GAMETEST VERIFIED** (`o9ElementalBurnDotFalseNegativeAndLastHitFalsePositiveAudit`) |
| **8. 다른 몹 공격** | 적대적 몹 근접/원거리 공격 (`Zombie`, `Skeleton`, `BanditArcher` 등 비소유 몹) | 바닐라 / 커스텀 몹 공격 | `mobAttack(mob)` / `arrow(arrow, mob)` | `Mob` / `Arrow` | `Mob` (`!(mob instanceof ServerPlayer)`, 소유자 없음) | 해당 없음 (`null`) | `MOB` → **무효 사망 (`INVALID_DEATH`)** | 플레이어가 99% 피해를 준 뒤 주변 몹이 마지막 1 HP를 가로채면 방식 A에서는 무효 사망 처리됨 | **GAMETEST VERIFIED** |
| **9. 관리자 명령** | `/kill` 명령어 (`Entity.kill(ServerLevel)`) | 바닐라 `/kill` | `damageSources().genericKill()` (`minecraft:generic_kill`) | `null` | `null` | 해당 없음 (`null`) | `COMMAND_KILL` → **무효 사망 (`INVALID_DEATH`)** | 정상 (사전 플레이어 참여가 있어도 `/kill`은 항상 무효) | **GAMETEST VERIFIED** |

---

## 3. 핵심 발견 사항: 위음성(False Negatives) 및 위양성(False Positives) 상세 분석

### 3.1 플레이어 공격인데 무효 처리되는 사례 (False Negatives)

1. **[핵심 결함 F-1] T2 화염검(`FlameSwordItem`)의 `FlameCleaveAbility` 점화 및 화염 인챈트(`Fire Aspect` / `Flame`) 화상 DOT 마무리:**
   - **원인:** `FlameCleaveAbility.java:51-52`는 직격 피해(`10.0F`)를 `playerAttack(player)`로 가한 직후 `target.igniteForSeconds(3.0F)`를 호출한다. 마인크래프트의 엔티티 화상 틱(`Entity.baseTick()`)은 `damageSources().onFire()`를 사용하여 피해를 주며, 이때 `source.getEntity()`와 `source.getDirectEntity()`가 모두 `null`이다.
   - **결과:** 플레이어가 화염검으로 정당하게 보스와 싸워 체력 200 중 199를 깎았더라도, `FlameCleaveAbility`(또는 화염 인챈트)가 붙인 3초 화상 DOT가 마지막 1 HP를 깎아 보스가 죽으면 `TombVictoryPolicy.classify(source)`가 `DeathCause.ENVIRONMENT`를 반환한다. 그 결과 **고대 심장(Ancient Heart) 드롭이 억제되고, 왕실 상자는 봉인된 채로 남으며, 비밀 입구는 열리지 않고, 보스 인카운터가 초기화**된다.
   - **검증:** `RoyalTombSafetyGameTests.o9ElementalBurnDotFalseNegativeAndLastHitFalsePositiveAudit`에서 실제 `TombGuardian`에 199 HP `playerAttack` + `igniteForSeconds(3.0F)` + 5 HP `onFire()` 치명타를 가해 `isVictory == false`, `isRoyalChestSealed == true`, `EntranceState == SEALED`가 됨을 확인했다.

2. **[결함 F-2] 연금술 감전 물약(`ShockMobEffect`) 및 상태 효과(`WITHER`) 마무리:**
   - **원인:** `ShockMobEffect.java:26`은 주기적 감전 펄스 피해를 `entity.hurt(level.damageSources().magic(), ShockTiming.PULSE_DAMAGE)`로 가하며, 바닐라 위더 효과는 `damageSources().wither()`를 사용한다. 두 경우 모두 `source.getEntity()`가 `null`이다.
   - **결과:** 플레이어가 투척한 물약/효과로 부여된 상태 효과의 틱 피해가 마지막 일격이 되면 `ENVIRONMENT`로 분류되어 무효 사망 처리된다.

3. **[결함 F-3] 플레이어 유발 낙뢰(`LightningBolt.setCause(ServerPlayer)`) 미귀속:**
   - **원인:** `TombVictoryPolicy.attributedPlayer(DamageSource)`는 `cause instanceof OwnableEntity`만 검사한다. 그러나 마인크래프트의 `LightningBolt` 엔티티는 `OwnableEntity` 인터페이스를 구현하지 않고 독자적인 `getCause()` (`@Nullable ServerPlayer`) 메서드를 사용한다. (단, T2 `LIGHTNING_SWORD`는 `LightningBolt` 엔티티를 소환하지 않고 `playerAttack(player)`를 직접 사용하므로 이 결함의 영향을 받지 않는다.)

4. **[경계 사례 F-4] 플레이어 99% 기여 후 주변 몹/환경에 의한 마지막 1타 손실:**
   - 현재 방식 A(마지막 타격 귀속)에서는 플레이어가 보스 체력의 99%를 깎았더라도 마지막 1 HP가 환경(불, 용암, 낙하)이나 다른 몹에게 깎이면 무효 사망이 된다.

### 3.2 환경 공격인데 유효 처리되는 사례 (False Positives)

1. **[순수 환경 피해 단독 사망]: 없음 (0건)**
   - 용암, 불, 낙하, 선인장, 익사, 공허, 주인 없는 폭발 등이 마지막 일격을 가하면 `classify(source)`가 항상 `ENVIRONMENT`를 반환하므로 순수 환경 사망이 유효 승리로 둔갑하는 일은 없다 (`SecretEntranceGameTests.environmentalDeathIsNotAVictoryChestStaysSealedAndRetryWorks`).
2. **[실질적 환경 처치 + 플레이어 1타 가로채기 (Effective-Environment Last-Hit Steal, P-1)]:**
   - **원인:** 현재 O-9의 최소 플레이어 피해 비율(`minPlayerDamageShare`)과 시간 창(`windowTicks`)이 미승인 상태(`Params == null`)이므로, 보스가 용암이나 환경 피해로 체력 200 중 **199 HP (99.5%)**를 잃은 상태에서 플레이어가 **단 1 HP (0.5%)**의 마지막 한 대만 치면 `cause == DeathCause.PLAYER` 및 `!parts.isEmpty()`가 성립하여 **유효 승리로 인정**된다.
   - **검증:** `RoyalTombSafetyGameTests.o9ElementalBurnDotFalseNegativeAndLastHitFalsePositiveAudit` Case 2에서 199 HP `lava()` + 1 HP `playerAttack(pl)` 시 유효 승리로 처리됨을 확인했다.

---

## 4. 개선 가능성 및 회귀 위험 분석 (사용자 결정 지원용)

작업 D의 규칙에 따라 아래 개선안은 **코드에 적용하지 않고** 향후 사용자(Owner)의 O-9 정책 결정을 위한 기술 옵션으로만 제시한다.

| 개선 옵션 | 내용 | 해결되는 문제 | 잠재적 회귀 위험 및 한계 |
|---|---|---|---|
| **옵션 1: `attributedPlayer`에 `LightningBolt.getCause()` 추가** | `if (cause instanceof LightningBolt bolt && bolt.getCause() != null) return bolt.getCause();` | 플레이어 유발 낙뢰(`Channeling` 등) 귀속 누락(F-3) 해결 | 회귀 위험 극히 낮음 (`LightningBolt.getCause()`는 바닐라 표준 API). 단, 낙뢰가 생성한 바닥 불(`inFire`/`onFire`)은 별도 처리 필요 |
| **옵션 2: `FlameCleaveAbility` / 점화 시 점화 주체 타임스탬프 기록 또는 `LivingEntity.getKillCredit()` 보조 참조** | 보스가 `onFire` / `magic` / `wither` 등 비엔티티 DOT로 사망했을 때, 최근 `N`틱(예: `FlameCleave` 점화 지속시간 60틱 또는 바닐라 `lastHurtByPlayerMemoryTime = 100틱`) 이내에 플레이어가 직격 피해를 입혔거나 점화한 기록이 있으면 플레이어 귀속으로 인정 | **T2 화염검(`FlameCleaveAbility`) 및 화염 인챈트 화상 DOT 마무리 위음성(F-1)** 및 상태 효과 마무리 위음성(F-2) 해결 | 만약 보스가 자연 용암/불에 빠져 죽기 직전(100틱 이내)에 플레이어가 1대만 때린 경우, 단순 `getKillCredit()`만 쓰면 용암사가 유효 승리로 판정될 위험(위양성)이 있음. 따라서 **`FlameCleaveAbility` 등 플레이어 무기가 부여한 점화 만료 시각(`ignitedByPlayerUntilTick`)을 `TombGuardian`에 명시적으로 기록**하거나, **옵션 3(최소 피해 비율 승인)과 결합**하는 것이 안전함 |
| **옵션 3: O-9 `Params(minPlayerDamageShare, windowTicks)` 승인 또는 방식 B(참여 기반 판정) 채택** | 예: `minPlayerDamageShare = 0.50` (50% 이상 플레이어 피해) 및 `windowTicks = 600` (최근 30초 내 플레이어 타격) 승인, 또는 `/kill`이 아닌 사망에서 해당 조건 충족 시 마지막 1타가 화상 DOT여도 유효 승리로 인정 (P10 Deliverable 8의 `R_B` 규칙) | 화염검 DOT 위음성(F-1), 상태 효과 위음성(F-2), 99% 기여 후 막타 손실(F-4), 그리고 99% 용암 + 1타 가로채기 위양성(P-1)을 **동시에 해결** | 사용자(Owner)의 O-9 정책 결정(`minPlayerDamageShare`, `windowTicks`, 마지막 타격 필수 여부)이 선행되어야 함 |
