#!/usr/bin/env python3
"""Builds docs/design/st01-p8/. Usage: python3 -I build_p8_report.py <repo_root>"""
import os, sys, json, shutil, subprocess
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
root = sys.argv[1]; dest = os.path.join(root, "docs/design/st01-p8"); gen = os.path.join(dest, "generated")
shutil.rmtree(gen, ignore_errors=True); os.makedirs(gen)
for cmd in (["st01_p8_verify.py", gen], ["st01_p8_mutation_tests.py", "--out", os.path.join(gen, "p8_mutation_results.json")]):
    rc = subprocess.run([sys.executable, "-I", os.path.join(HERE, cmd[0])] + cmd[1:], capture_output=True, text=True); assert rc.returncode == 0, rc.stdout[-1500:] + rc.stderr[-1500:]
J = json.load(open(os.path.join(gen, "p8_verification.json"))); MU = json.load(open(os.path.join(gen, "p8_mutation_results.json")))
ch = J["checks"]; cnt = lambda v: sum(c["verdict"] == v for c in ch)
java = [c for c in ch if c["side"] == "Java"]
sc = J["golem"]["east"]["scales"]; zones = J["p7geo"]["zones"]
scale_tbl = "\n".join(f"| {r['scale']} | {r['bbox'][0]}×{r['bbox'][1]} | {r['nav'][0]}×{r['nav'][0]}×{r['nav'][1]} | {r['reach_min']}/5 | {r['margin_to_cut']['Z0']} | {r['margin_to_cut']['Z1']} | {r['margin_to_cut']['Z2']} |" for r in sc)
zone_tbl = "\n".join(f"| {k} | {v['cells']} | {v['min_dist_to_cover']} |" for k, v in zones.items())
sel = "\n".join(f"| {l.split()[1]} | {l.split()[2].split('=')[1]} | {l.split()[3].split('=')[1]} | {l.split()[4].split('=')[1]} |" for l in J["selectors"])
mat = []
for l in J["matrix"]:
    pol = l.split(":")[0].split()[1]; items = [x.strip("[]") for x in l.split("] [")]
    mat.append((pol, [i.split(" => ")[-1].strip("]") for i in l[len("MATRIX " + pol + ": "):].split("] [")], [i.split(" => ")[0].strip("[ ") for i in l[len("MATRIX " + pol + ": "):].split("] [")]))
names = mat[0][2]
mat_tbl = "| 상황 | " + " | ".join(p for p, _, _ in mat) + " |\n| --- |" + " --- |" * len(mat) + "\n" + "\n".join(f"| {n} | " + " | ".join(m[1][i] for m in mat) + " |" for i, n in enumerate(names))
mut = "\n".join(f"| {m['mutation']} | {', '.join(m['expected'])} | {', '.join(m['failed'])} | {'검출' if m['detected'] else '**미검출**'} |" for m in MU["results"])
evid = "\n".join(f"| {c['side']} | {c['id']} | {c['title']} | {c['verdict']} | {c['evidence'][:300]} |" for c in ch)
jt = "\n".join(f"| {c['id']} | {c['title']} | {c['verdict']} |" for c in java)
def w(n, t): open(os.path.join(dest, n), "w").write(t)

w("ST01_P8_USER_APPROVAL_RECORD_2026-11-07.md", """# ST-01 P8 — P7 추가 사용자 승인 기록 (2026-11-07)

상태: **기록 문서 / 설계 전용.** Q01–Q100 설계 권위를 변경하지 않고 최종 보스·피라미드 구현을 승인하지 않는다. 기준: `work/st01-p7-secret-entrance-state-20261107` @ `5610683dd004071b0e9967e22d463e57d6b871ee`(작업 트리 깨끗, origin 일치 확인), 신규 브랜치 `work/st01-p8-dummy-prototype-20261107`.

## 1. 확정된 게임플레이 정책

| # | 정책 | P8에서의 적용 |
| --- | --- | --- |
| 1.1 | Tomb Guardian 최종 크기·외형·패턴·AI는 후속 승인. 이번에는 최소 더미만, 더미가 최종 사양을 정하면 안 됨, 최종 보스 코드와 교체 가능한 인터페이스 | `BossLifecycle`/`DefeatReport` 경계와 `DummyBoss`(공격·외형·크기 없음) |
| 1.2 | 유효한 보스 처치 확정 시점부터 대체 개방 타이머 시작. 골렘 생성 성공과 무관. O-1 값 미승인, 임시 시간은 TEST-ONLY | 타이머는 승리 확정(`reconcile`)에서 시작. `Config.productionDefaults()`는 타이머 미설정이며 코드가 이를 거부(T0). 테스트 값 6틱/2틱은 TEST-ONLY |
| 1.3 | 참여 플레이어가 있는 정상 전투에서만 승리 인정. 비플레이어 요인만의 사망은 자동 승리 아님. 참여 판정의 시간 창·기여 기준·사망 원인 처리는 기존 코드를 조사해 제안 | 감사 문서 §2, 더미 보스 명세 §3. 영속 처치 기록 ≠ 승리 인정이 *구분되지 않음* → 변경 요청 CR-1(승인 대기, 적용 안 함) |
| 1.4 | 구조물 인스턴스 단위 공동 진행, 골렘 최대 1마리, 상태는 인스턴스별, 이탈·사망·로그아웃이 타인의 진행을 막지 않음. 골렘 추종 대상은 후보 비교 | `InstanceId`(차원+앵커)별 `EntranceCoordinator`, `InstanceRegistry`; 선택기 3종 비교 |
| 1.5 | P6 12블록 제거형 A안 유지. 한 번 OPEN이면 정상 게임플레이에서 복원 없음. 재시작·청크 재로드 후 OPEN 복원. 전이는 멱등 | 복원 전이 없음(I2), OPEN 기록 권위(T7, T8, T7d), 멱등(T5b) |
| 1.6 | 아이언 골렘 모델·애니메이션 활용 소형 금색 골렘 우선 검토, 엔티티 축소 vs 별도 소형 엔티티 비교, 통로 통과·안전 정지 지점 최우선, 크기·속도·생성 위치 미확정 | 골렘 기술 검증 문서 |

## 2. 기술 검증으로 처리(숫자는 사용자가 지정하지 않음)

O-4, O-6, O-5, O-7, O-8, O-13 — 후보·근거·위험·검증 결과를 `ST01_P8_TECHNICAL_DECISION_RECOMMENDATIONS_2026-11-07.md`에 보고한다. 권장안은 **제안**이며 자동 확정이 아니다.

## 3. 후속 보류(P8 승인으로 간주하지 않음)

O-1 최종 대기 시간, O-12 최종 개방 연출, O-14 월드젠 배치, D-3, Q15, Q98, Tomb Guardian 최종 크기·전투 AI·외형.

## 4. 변경 제한 준수

Q01–Q100, P1–P7 산출물, `src/**`, 기존 보스·보상 코드는 수정하지 않았다. 신규 파일은 `tools/st01-p8/`(격리된 프로토타입·하네스)와 `docs/design/st01-p8/`뿐이다. 프로토타입은 Gradle 빌드에 포함되지 않는다.
""")

w("ST01_P8_CODE_COMPATIBILITY_AUDIT_2026-11-07.md", """# ST-01 P8 — 기존 코드 호환성 감사 (2026-11-07)

상태: **읽기 전용 조사.** 저장소 코드는 수정하지 않았다. 구분: EXISTING(이미 있음), MISSING(없음), INCOMPATIBLE(승인된 정책과 맞지 않음). 인메모리 코드 읽기와 grep 결과이며 게임 내 동작은 UNVERIFIED이다.

## 1. 요약표

| # | 항목 | 판정 | 근거 |
| --- | --- | --- | --- |
| 1 | 처치 이벤트 발행 위치 | **EXISTING** | `TombGuardianEncounters.DEFEATED`(`:31`)는 `guardianDied`(`:138-143`)에서 `markDefeated`가 true일 때 1회 발행. `guardianDied`는 `TombGuardian.die()`(`:240-248`)의 `super.die` 이후 호출 |
| 2 | 영속 처치 기록 = 승리 인정? | **INCOMPATIBLE** | `die()`는 `isBound(...)`이면(`:243`) 사망 원인과 무관하게 `credited`로 보고 `markDefeated`(`TombEncounterData:127`)로 영구 기록한다. "사망"과 "승리"가 구분되지 않는다 |
| 3 | 전투 참여 플레이어 추적 | **MISSING**(보스) / **EXISTING**(관례) | `TombGuardian`에는 피해 기록이 없고 `getKillCredit()`(`:245`)만 쓴다. 저장소에는 선례 `progression/QuestMonumentParticipation`(피해 기반, 투사체·소유 엔티티는 플레이어로 귀속, 존재만으로는 증거 아님, 12,000틱 창)과 Fabric `ServerLivingEntityEvents` 사용(`ColosseumRuntime`, `QuestMonumentParticipation`)이 있다 |
| 4 | 보상 지급 조건 | **INCOMPATIBLE** | 보스 전리품은 루트 테이블 `loot_table/entities/tomb_guardian.json`(`ancient_heart` 50%, `random_chance` 조건만 있고 플레이어 처치 조건 없음)과 `shouldDropLoot = !lootSuppressed && super`(`:254`)로 결정된다. `lootSuppressed`는 고아·비바인딩 때만 켜진다(`:181, :244`). 따라서 바인딩된 보스가 환경 요인으로만 죽어도 전리품이 나올 수 있다(바닐라 드롭 경로는 UNVERIFIED) |
| 5 | 상자 봉인·해제 조건 | **INCOMPATIBLE**(조건) / **MISSING**(블록) | `isRoyalChestSealed = !isDefeated`(`:65-67`)이므로 비플레이어 사망도 봉인을 푼다. 상자 블록 자체와 호출부는 없다 |
| 6 | 구조물 인스턴스 식별 | **EXISTING**(키) / **MISSING**(구조물) | 무덤 키 = 차원별 SavedData + `tombAnchor.asLong()`(`TombGuardianEncounters:62`). ST-01 구조물·인스턴스 등록부는 없다 |
| 7 | 재시작·청크 복구 | **EXISTING**(보스) / **MISSING**(입구·골렘) | `TombEncounterData` SavedData(`:24-40`), `presence`/`NOT_LOADED` 처리(`TombGuardianEncounters:123-131`), 보스 청크 추적(`TombGuardian:176-186`). 입구 상태·골렘 영속은 없다 |
| 8 | `DEFEATED` 구독자 | **MISSING** | 운영 코드 구독 0, 게임테스트 1곳(`TombGuardianGameTests:48`)뿐 |
| 9 | 아이언 골렘 엔티티·렌더 | **MISSING**(사용) / **EXISTING**(선례) | `IronGolem` 사용 없음(`ContractCatalog` 문자열만). 선례: `Attributes.SCALE`을 믹스인으로 설정(`ZombieMixin:72`, `SkeletonMixin:81`), 바닐라 렌더러+텍스처만 교체(`CustomMobRenderers`), 커스텀 엔티티 `sized(w,h)` 등록과 바닐라 모델 스케일 렌더러(`TombGuardianEntities`, `TombGuardianRenderer`) |
| 10 | 라이브 검증 경로 | **EXISTING** | Fabric 게임테스트 러너와 `TombGuardianGameTests`(보스 단독 시나리오). 이번 세션은 Java 25/Minecraft 26.3 도구체인이 없어 실행하지 못했다 |

## 2. 충돌 해설과 변경 요청(승인 대기)

**CR-1 (INCOMPATIBLE 2·4·5의 공통 원인): 사망과 승리를 분리.** 정책 1.3은 "비플레이어 요인만의 사망은 승리가 아니며, 유효하지 않은 처치로 개방·보상이 없어야 한다"고 한다. 기존 코드는 영속 `defeated`가 곧 승리·상자 봉인 해제·전리품 조건이다. 둘을 구분하려면 운영 코드 변경이 필요하다.

| 항목 | 내용 |
| --- | --- |
| 범위 | `boss/TombGuardian.java`(`die`의 credited 판정), `boss/TombEncounterData.java`(승리 목록 + 하위 호환 코덱), `boss/TombGuardianEncounters.java`(`guardianDied`) 및 해당 단위 테스트 |
| 행동 | 처치 시 참여 판정(더미 명세 §3)을 거쳐 유효할 때만 `markDefeated`/`DEFEATED`/전리품. 무효 처치는 바인딩 해제(재도전 가능), `lootSuppressed=true` |
| 마이그레이션 | 기존 세이브의 `defeated`는 승리로 간주(필드 유지) |
| 위험 | 기존 보상 동작이 바뀐다(비플레이어 사망 시 전리품 소멸) → 사용자 승인 필요 |
| 상태 | **적용하지 않음. 승인 대기.** P8 프로토타입은 이 구분을 가정한 `EncounterStore`(`markDeath`/`markVictory`)로 격리 구현했다 |

**CR-2 (선택): 참여 추적.** `TombGuardian`을 고치지 않고 `ServerLivingEntityEvents.AFTER_DAMAGE`를 구독하는 신규 클래스로 구현 가능(`tombAnchor()` 접근자가 이미 공개). 선례 `QuestMonumentParticipation`의 귀속 규칙을 재사용한다.

## 3. 보류하지 않은 사실

- 기존 이벤트는 인메모리 신호이고 영속 기록이 진실이므로, 입구 상태는 항상 영속 기록에서 재조정해야 한다(P7과 동일, 프로토타입 T9).
- 전투 중 `die()`에서 이벤트가 호출되므로 리스너는 골렘 생성을 다음 틱으로 미뤄야 한다(프로토타입은 `pumpSpawn`을 틱 핸들러로 분리).
""")

w("ST01_P8_DUMMY_BOSS_PROTOTYPE_SPEC_2026-11-07.md", f"""# ST-01 P8 — 더미 보스 프로토타입 및 승리 판정 명세 (2026-11-07)

상태: **IMPLEMENTED(격리 하네스)** + **제안(승리 판정 규칙)**. 파일: `tools/st01-p8/java/st01proto/{{DummyBoss,ParticipationTracker,Ports}}.java`. 최종 보스 크기·외형·공격·AI는 포함하지 않으며 결정하지 않는다.

## 1. 경계(교체 가능 인터페이스)

| 인터페이스 | 역할 | 실제 보스와의 연결(제안, 미구현) |
| --- | --- | --- |
| `BossLifecycle.setListener(Consumer<DefeatReport>)` | 사망 보고를 받는 쪽 | `TombGuardianEncounters.DEFEATED`를 어댑터가 `DefeatReport`로 변환 |
| `DefeatReport(id, validVictory, participants, reason)` | 사망 결과(유효 여부 포함) | CR-1 이후 보스 쪽이 판정 |
| `EncounterStore.markDeath/markVictory` | 사망과 승리의 분리 영속 | 기존 `TombEncounterData`는 단일 `defeated` → CR-1 필요 |
| `RewardPort.grant` | 보상 | 기존 전리품 로직은 건드리지 않음(더미는 카운터) |

## 2. 더미 동작

피해(플레이어/기타)를 받고 `die(cause, tick)`에서 판정 후 `store`를 갱신하고 보고를 발행한다. 공격·AI·외형·크기 없음. 유효 처치만 `markVictory`+`grant`, 무효 처치는 봉인 유지·보상 없음·재도전 허용.

## 3. 승리 판정 규칙(제안; 모든 수치는 TEST-ONLY)

기존 선례(`QuestMonumentParticipation`)와 일치: **참여자 = 보스에게 피해를 준 플레이어**(소유 엔티티·투사체는 주인에게 귀속), **존재만으로는 증거 아님**.

| 정책 | 정의 |
| --- | --- |
| ANY_DAMAGE | 플레이어가 한 번이라도 피해를 줌 |
| LAST_HIT_WINDOW | 마지막 플레이어 피해가 사망 직전 창 이내(플레이어가 막타면 항상 충족) |
| DAMAGE_SHARE | 플레이어 누적 피해 ≥ 최대 체력 × 비율 |
| COMBINED(권장) | DAMAGE_SHARE 그리고 LAST_HIT_WINDOW |

공통: 관리자 `/kill`(COMMAND_KILL)은 항상 무효. 파라미터(비율 0.5, 창 100틱)는 **TEST-ONLY**이며 승인되지 않았다. 창 후보의 근거: 바닐라 플레이어 처치 기억 100틱(UNVERIFIED), 저장소 선례 12,000틱(몬먼트 참여).

판정 결과 행렬(하네스 실행 결과; VALID/invalid):

{mat_tbl}

COMBINED를 권장하는 이유: ANY_DAMAGE는 "한 대 치고 용암에 빠뜨려도 승리", LAST_HIT_WINDOW는 "한 대 치고 환경이 거의 다 깎아도 승리", DAMAGE_SHARE는 "환경이 막타를 치고 한참 뒤 죽어도 승리"를 허용한다. 최종 비율·창은 사용자 결정이다(O-9 세분화).

## 4. 하네스 검증 결과(IMPLEMENTED, 인메모리)

{jt}

주의: 위는 서버 없이 동작하는 인메모리 하네스이다. 실제 사망 원인 분류(`DamageSource` 매핑), 소유 엔티티 귀속, 이벤트 타이밍은 **UNVERIFIED**이다.
""")

w("ST01_P8_GOLEM_TECHNICAL_VERIFICATION_2026-11-07.md", f"""# ST-01 P8 — 소형 금 골렘 기술 검증 (2026-11-07)

상태: **STATIC PASS(기하) / UNVERIFIED(게임 내)**. 크기·속도·생성 위치는 확정하지 않는다. 아래 크기는 *기술 창*을 찾기 위한 탐색 격자이며 제안 값이 아니다.

## 1. 구현 방식 비교

| 기준 | A. 바닐라 `IronGolem` + `Attributes.SCALE` | B. 별도 소형 엔티티(`PathfinderMob` 파생, 전용 `EntityType`) + 바닐라 아이언 골렘 모델/애니메이션 재사용 |
| --- | --- | --- |
| 모델·애니메이션 재사용 | 그대로 | 렌더러에서 바닐라 골렘 렌더러·모델 레이어 재사용 + 텍스처만 교체(선례: `CustomMobRenderers`) |
| 충돌 상자·경로 크기 | SCALE 속성이 상자를 함께 축소(선례: `ZombieMixin`/`SkeletonMixin`의 SCALE 사용). 경로 노드 크기는 상자에서 파생 | `EntityType.sized(w,h)` 또는 SCALE 속성으로 명시. 렌더/충돌 분리 위험은 SCALE 속성을 단일 원천으로 쓰면 제거 |
| 전투/마을 행동 | **바닐라 아이언 골렘 목표(몹 공격, 마을 수호 등)를 제거해야 함**(믹스인 필요) | 목표를 직접 구성(안내·대기만) — 안정적 |
| 유일성·영속 제어 | 바닐라 스폰·저장 규칙과 얽힘 | 전용 타입으로 인스턴스별 1마리 규칙을 코드로 강제하기 쉬움 |
| 구현 범위 | 믹스인 추가(타 시스템 영향 위험) | 등록 + 렌더러 + 목표 클래스(신규 파일 위주) |
| 반응성 | — | — |

**권장(제안):** B. 이유: 안내 전용 목표만 필요하고, 바닐라 골렘의 방어·공격 행동을 제거하는 믹스인 위험을 피하며, 유일성 규칙을 직접 강제할 수 있다. 크기는 SCALE 속성 한 값으로 렌더·충돌·경로를 함께 조절하는 것을 권한다. **UNVERIFIED:** SCALE 속성이 렌더·상자·경로에 미치는 정확한 효과, 바닐라 골렘 렌더러 재사용 가능 여부는 26.3 소스/실서버에서 확인하지 못했다.

## 2. 크기·통로·정지 안전 측정(동·서 동일)

바닐라 지상 경로 노드 크기 규칙 `floor(크기+1)`을 가정(지식 기반; 26.3 미확인). 아이언 골렘 기본 1.4×2.7. 시작점: 홀 입구, 보스 등장 후보 4개(상자 위 단 시작점은 폭 2 이상 자세가 성립하지 않아 제외).

| scale | 충돌 상자(폭×높이) | 경로 노드(칸) | 도달 구역(최소) | 정지 여유 Z0 | Z1 | Z2 |
| --- | --- | --- | --- | --- | --- | --- |
{scale_tbl}

(정지 여유 = 구역 최근접 칸 중심에서 덮개 가장자리까지 거리 − 몸체 반폭. 양수여야 몸체가 절개에서 떨어져 있다.)

구역(덮개 직사각형으로부터 거리 구간; P7과 동일):

| 구역 | 칸 수 | 최근접 거리 |
| --- | --- | --- |
{zone_tbl}

판독:
- **통로:** 홀 입구·P2 출구 문턱의 가장 좁은 단면은 3폭×3높이. 노드 크기 2×2×3(scale ≈0.72–1.0)까지 통과하고 scale 1.5(노드 3×3×5)는 통과하지 못해 도달 구역이 3/5로 줄어든다(GG1, GG2).
- **정지 지점:** Z0(최근접)에 중심을 두고 몸체가 절개와 겹치지 않는 최대 scale = 2·0.5/1.4 ≈ 0.714. 그보다 크면 Z1 이상에 세워야 한다(GG3).
- **기술 창(제안 아님):** scale ≤ 0.71이면 노드 1×1×2로 모든 구역에 도달하고 Z0에도 몸체가 겹치지 않게 정지할 수 있다. 이보다 크게 하려면 정지 지점을 Z1/Z2로 옮기면 된다(통로 제약은 scale ≈1.0까지 없음).
- **낙하:** 몸체가 절개 위에 *완전히* 올라서면 지지 바닥이 없어 떨어진다(절개 4×3은 scale ≤ 2.1까지 완전히 담을 수 있음). 일부만 걸친 몸체는 남은 바닥이 지지하는 것이 바닐라 물리이나 UNVERIFIED. 따라서 정지 지점은 절개 칸 밖이어야 하고, 도착 판정은 정지 지점 반경 이내로 하되 반경은 몸체가 절개 위에 완전히 오르지 못하도록 제한한다.

## 3. 서버 권위·유일성

- 도착 판정은 서버 틱에서 골렘 서버 엔티티 위치와 정지 지점을 비교한다(클라이언트 값 사용 안 함). 하네스 `GolemPort.atStopPoint`가 이 경계이다.
- 유일성: 코디네이터가 생성 전에 `aliveCount`를 확인하고, 포트 계약은 두 번째 골렘 생성 금지. 중복 신호 시나리오 T5a와 음성 대조로 검증(IMPLEMENTED, 인메모리).

## 4. 연출 의도

보스 처치 후 금빛 입자로 사라지며 작은 금 골렘으로 전환되는 의도는 유지하되 최종 연출은 구현하지 않았다(O-12 보류).
""")

w("ST01_P8_STATE_MACHINE_INTEGRATION_PROTOTYPE_2026-11-07.md", """# ST-01 P8 — 상태기계 통합 프로토타입 (2026-11-07)

상태: **IMPLEMENTED(격리 하네스, 인메모리)** — Gradle 빌드·운영 코드와 분리(`tools/st01-p8/java/st01proto`, javac 21로 컴파일, 의존성 없음). **LIVE VERIFIED 아님.**

## 1. 구성

| 클래스 | 역할 |
| --- | --- |
| `InstanceId` | 구조물 인스턴스 = 차원 + 앵커(기존 `tombKey`와 동일 키) |
| `EntranceCoordinator` | 인스턴스별 P7 상태기계. 신호는 비권위, 진실은 `EncounterStore.isVictory` + 레코드 + 월드 블록 |
| `InstanceRegistry` | 인스턴스별 코디네이터, 상태 공유 없음 |
| `Ports` | `BossLifecycle`, `EncounterStore`, `GolemPort`, `CoverPort`, `RewardPort` — 실제 구현으로 교체되는 경계 |
| `DummyBoss`, `ParticipationTracker` | 더미 보스와 승리 판정 |
| `GuideTargetSelector` | 골렘 추종 대상 후보 3종 |
| `FakeWorld` | 영속(승리·사망·덮개) / 휘발(골렘·청크) 구분을 갖는 인메모리 월드 |
| `Config` | 타이머 기본값 없음(O-1), TEST-ONLY 값은 명시 생성자만 |

## 2. 흐름(구현됨)

1. 더미 사망 → 판정 → 유효하면 `markVictory`+보상, 무효면 봉인 유지·재도전.
2. 신호(또는 틱)에서 `reconcile`: 승리 기록이 있고 SEALED이면 `GUARDIAN_DEFEATED`로 만들고 **그 순간 타이머 시작**.
3. 틱: 타이머 감소 → 만료 시 `FALLBACK_OPEN`; `pumpSpawn`은 골렘을 한 번 생성 시도(실패해도 타이머 유지); `pollArrival`은 서버 권위 도착을 확인해 `ENTRANCE_OPEN`.
4. 개방 상태 + 청크 로드 시 덮개 12칸을 멱등하게 제거(이미 열려 있으면 변화 없음, 닫는 연산 없음).
5. 스냅샷 4항목(차원, 앵커, 상태, 남은 시간, 덮개 적용)으로 재시작 복원. 골렘은 재시작 후 존재한다고 가정하지 않음(선택 옵션 `respawnAfterRestart`, 기본 꺼짐).

## 3. P7 참조 모델과의 일치

P7 상태기계에서 무작위 이벤트 열 4,000개(총 약 3만 단계)를 생성해 하네스에 재생하고 매 단계 (상태, 덮개, 골렘 수, 승리, 남은 시간, 생성 대기)가 동일한지 비교했다(T13: PASS). 하네스가 P7 설계를 충실히 구현함을 보이는 차분 시험이며 실제 서버 동작을 보이는 것은 아니다.

## 4. P7 대비 변경·추가

- 열림 기록을 권위로 보고 **되돌려진 블록을 멱등하게 다시 연다**(T7d). P7은 `coverApplied`만 믿었다.
- **저장 불일치(열림 기록인데 승리 기록 없음)** 에서는 덮개를 열지 않고 `anomaly`를 표시한다(T15, O-8 제안).
- `respawnAfterRestart` 옵션(T14, O-7 후보).
""")

w("ST01_P8_MULTIPLAYER_PERSISTENCE_TESTS_2026-11-07.md", f"""# ST-01 P8 — 멀티플레이·영속성 테스트 (2026-11-07)

상태: **IMPLEMENTED(인메모리 하네스) / STATIC PASS**. 실제 서버 다중 접속·재시작은 **UNVERIFIED**.

## 1. 요구 시나리오와 결과

| 요구 | 하네스 테스트 | 결과 |
| --- | --- | --- |
| 두 플레이어가 동시에 처치 이벤트 발생 | T5a(중복 신호 3회→골렘 1마리), T5b | PASS |
| 한 플레이어 로그아웃 | T11 선택기: 오프라인 참여자는 후보가 아님 | PASS |
| 안내 중 플레이어 분산 | T11 시뮬레이션(아래 표) | PASS(비교) |
| 모든 플레이어가 일시 이탈 | T11 전원 오프라인 → 대상 없음; 진행은 인스턴스 타이머로 계속(T4b, P7 E5 계열) | PASS |
| 재접속 후 진행 재개 | 진행 상태는 인스턴스 소유라 접속 상태와 무관(T7a/T7b), 재시작 후 복원 | PASS |
| 구조물 인스턴스 두 개 동시 활성 | T10a/T10b/T10c: 한쪽은 도착으로, 다른 쪽은 대체 개방으로 열림, 골렘·보상 각 1, 한쪽 처치가 다른 쪽에 영향 없음 | PASS |

## 2. 추종 대상 후보 비교(스크립트 분산 시나리오: 60틱, P1 흔들림, P2는 일시 접속 해제)

| 후보 | 대상 전환 횟수 | 오프라인 대상 선택 | 전원 오프라인 시 빈 값 |
| --- | --- | --- | --- |
{sel}

- Nearest는 두 참여자 거리가 비슷하면 매 틱 번갈아 선택(전환 과다), Farthest도 마찬가지이며 골렘이 방향을 자주 바꿀 위험이 있다.
- **StickyNearest(마진 있는 가장 가까운 참여자)** 가 전환이 가장 적다. 마진은 TEST-ONLY. 최종 알고리즘·마진은 사용자 결정(O-10)이며 이 표는 후보 비교이다. 어떤 후보든 "참여자 중 온라인만", 존재만으로 후보가 되지 않는다.

## 3. 영속성

| 상황 | 테스트 | 결과 |
| --- | --- | --- |
| 안내 중 재시작 → 상태·남은 시간 유지, 골렘 가정 없음 | T7a | PASS |
| 재시작 후 대체 개방 | T7b | PASS |
| OPEN 후 재시작: 열림 유지, 월드 변경 1회 | T7c | PASS |
| 열림 기록 + 블록 되돌림(청크 재생성): 멱등 재개방 | T7d | PASS |
| 만료 시 청크 언로드 → 로드 시 1회 적용 | T8 | PASS |
| 처치 신호 유실 → 영속 승리 기록으로 복구 | T9 | PASS |
| 열림 기록인데 승리 기록 없음(저장 불일치) → 열지 않음, 이상 표시 | T15 | PASS |
| 재시작 후 골렘 재생성 옵션 | T14 | PASS |
""")

w("ST01_P8_TECHNICAL_DECISION_RECOMMENDATIONS_2026-11-07.md", f"""# ST-01 P8 — 미해결 기술 결정별 권장안 (2026-11-07)

상태: **권장안(제안) / 자동 확정 아님.** 각 항목: 후보, 근거, 충돌 위험, 검증 결과. 수치는 승인 값이 아니다. 기존 사용자 승인 사양을 바꾸는 항목은 명시한다.

## O-4 도착 판정 반경과 안전 정지 지점

| 후보 | 근거 | 위험 | 검증 |
| --- | --- | --- | --- |
| AZ-1 정지 지점 Z2(덮개에서 ≥1.58) | 몸체가 절개와 떨어짐(scale 0.7에서 여유 1.09) | 구멍에서 약간 떨어져 선 모습 | STATIC PASS(GG3) |
| AZ-2 Z0 + 덮개 칸 제외 판정 | 구멍 바로 옆 | scale ≤0.71일 때만 몸체 분리 | STATIC PASS |
| AZ-3 도착 후 개방 직전 정지 지점으로 이동 | 위치 보장 | 추가 상태·이동 | 미구현 |

**권장:** AZ-1(정지 지점 Z2의 한 칸, 도착 반경은 (정지 지점–절개 거리 − 몸체 반폭) 이하). 도착 판정은 서버 틱의 서버 엔티티 위치로 한다. 반경 수치는 미확정.

## O-6 골렘 크기·속도·생성 위치

- 크기: 기술 창 scale ≤ 0.71(노드 1×1×2, Z0도 안전), 필요 시 더 크게 하려면 Z1/Z2 정지. 통로 제약은 ≈1.0까지 없음(GG2). 구현 방식은 별도 소형 엔티티(B) 권장(골렘 기술 검증 문서).
- 속도: 기술 제약은 안내 시간뿐(홀 입구→Z0 최대 {J['p7geo']['Lmax']}걸음). 속도는 O-1 타이머 하한 관계식 입력이며 미확정.
- 생성 위치: (a) 보스 사망 위치에서 가장 가까운 *덮개 칸이 아닌* 서 있기 가능 칸(연출 연속), (b) 고정 칸. **권장:** (a), 실패 시 (b)로 대체. 생성 실패는 타이머에 영향 없음(T4b).

## O-5 낙하 가장자리 안전 대책

P7/P8 측정: 최대 낙차 {J['p7geo']['max_fall']}(X=−6 열, 피해 1 가능). 후보: (1) 허용(구멍 외관 유지) (2) 가장자리 난간 (3) 구멍 형상/첫 단 변경(P3–P6 재검증, 승인 사양 변경 가능성). **권장:** 프로토타입 단계에서는 허용하고 실서버 낙하 테스트 후 결정. 기하 변경은 자동 확정하지 않는다.

## O-7 재시작 시 골렘 재생성

후보: 재생성 안 함(P7 기본) / 한 번 재생성. 하네스에서 두 방식 모두 구현·검증(T7, T14). **권장:** 재생성 옵션은 꺼 두고 타이머에 의존(보장은 이미 충족). 사용자가 연출 연속성을 원하면 옵션을 켠다.

## O-8 저장 불일치 감지·복구

후보: 열림 기록 + 승리 기록 없음 → (a) 무시하고 이상만 기록 (b) 승리 기록 가정 후 개방 (c) 관리자 수동. **권장:** (a)(T15 구현) — 처치 전 개방 금지 원칙. 반대 방향(열림 기록인데 블록이 되돌아감)은 기록을 권위로 보고 멱등 재개방(T7d).

## O-13 보스·상자 앵커 후보

상자 앵커 = P3 `CHEST_PRIMARY` 블록: 동쪽 {tuple(J['p7geo']['anchor'])}(서쪽은 X 반전). 덮개 칸은 앵커 기준 dx +12..+15, dy −3, dz 0..2. 보스 등장 후보 중 상자 접근점에 가장 가까운 S형(상자 5.0, 덮개 13.4)을 **권장**(충격파 반경 6.0 밖). BOSS-R1은 실전 검증 대기라 확정하지 않는다.

## 승리 판정 추가 권장(O-9 세분화)

COMBINED 규칙(더미 보스 명세 §3) + CR-1(사망/승리 분리)을 사용자가 승인해야 기존 보스에 반영할 수 있다.
""")

final = f"""# ST-01 P8 — 최종 보고서 (2026-11-07)

상태: **프로토타입·기술 검증 보고 / 최종 보스·피라미드 구현 승인 아님.** 상태 표기: IMPLEMENTED(격리 하네스 구현), STATIC PASS, LIVE VERIFIED(없음), UNVERIFIED, BLOCKED.

- 기준: `work/st01-p7-secret-entrance-state-20261107` @ `5610683dd004071b0e9967e22d463e57d6b871ee` → 신규 `work/st01-p8-dummy-prototype-20261107`
- 변경 범위: `tools/st01-p8/`, `docs/design/st01-p8/`만(기존 Q01–Q100, P1–P7, `src/**` 변경 0)

## 1. 판정 요약

PASS {cnt('PASS')} / FAIL {cnt('FAIL')} / UNVERIFIED {cnt('UNVERIFIED')}. 음성 대조 {sum(m['detected'] for m in MU['results'])}/{len(MU['results'])} 검출(무결함 대조군 FAIL 0).

| 영역 | 상태 |
| --- | --- |
| 더미 보스·승리 판정 | IMPLEMENTED(인메모리) / STATIC PASS |
| 코디네이터·타이머·덮개 멱등 개방 | IMPLEMENTED / STATIC PASS, P7 모델과 차분 시험 일치 |
| 멀티플레이(인스턴스 격리, 단일 골렘) | IMPLEMENTED / STATIC PASS |
| 영속·재시작·청크 시나리오 | IMPLEMENTED / STATIC PASS |
| 골렘 통로·정지 안전 | STATIC PASS(기하) |
| 실제 서버(엔티티 충돌·경로탐색·SCALE 속성·블록 변경 타이밍·청크 재로드·다중 접속·실제 사망 원인) | **UNVERIFIED** (도구체인 없음: Java 25 + Minecraft 26.3) |
| 기존 보상 시스템 회귀 | 코드 변경 0이므로 회귀 없음(STATIC); 단 기존 코드는 승인 정책과 **INCOMPATIBLE**(CR-1) |

## 2. 필수 검증 항목별 결과

| 항목 | 결과 |
| --- | --- |
| 보스 처치 전 입구 개방 불가 | STATIC PASS (T1, 타이머·요청·상자 접촉·스푸핑 신호) |
| 무효한 처치로 진행 불가 | STATIC PASS (T2, T3, T3b: 환경 사망, /kill, 약한 참여) |
| 유효 처치 후 타이머 시작 | STATIC PASS (T4a: 승리 확정 즉시 시작) |
| 골렘 생성 실패 시 대체 개방 | STATIC PASS (T4b) |
| 중복 이벤트 멱등성 | STATIC PASS (T5a/T5b) |
| 구조물별 골렘 최대 한 마리 | STATIC PASS (T5a, T10b + 음성 대조) |
| 구조물별 상태 격리 | STATIC PASS (T10a–c) |
| OPEN 영속성·재시작 복구 | STATIC PASS (T7a–d, T8, T9) |
| 골렘의 실제 통로 통과 가능성 | STATIC PASS(기하; 노드 크기 규칙은 지식 기반, 게임 내 UNVERIFIED) |
| 플레이어 점유 상태 덮개 제거 안전성 | STATIC PASS (P7 GA1 재실행: 매몰 없음, 최대 낙차 4) |
| 기존 보상 시스템 회귀 없음 | 코드 미수정; 정책 불일치는 CR-1로 보고 |

## 3. 핵심 발견

1. **영속 처치 기록 ≠ 승리 인정이 아니다(INCOMPATIBLE).** 기존 코드는 바인딩된 보스가 어떤 원인으로 죽어도 영구 처치·상자 봉인 해제·전리품(루트 테이블에 플레이어 조건 없음)을 인정한다. 정책 1.3을 지키려면 CR-1(운영 코드 변경)이 필요하며 **승인 대기**로 두었다.
2. 참여 추적은 보스에 없으나 저장소에 선례(`QuestMonumentParticipation`)와 Fabric 이벤트가 있어 보스를 고치지 않고 구독으로 구현할 수 있다(CR-2).
3. 신규 구현 필요: `DEFEATED` 구독자, 골렘 엔티티·렌더러, 입구 SavedData, 덮개 개방기. 모두 현재 없다(MISSING).
4. 골렘: 별도 소형 엔티티 + 바닐라 모델 재사용 방식 권장. 통로는 scale ≈1.0까지 통과, 정지 안전 기준 scale ≤ 0.71(Z0). 1.5배는 통과 불가.
5. P7 대비 보강 2건: 열림 기록 권위의 멱등 재개방, 저장 불일치 시 비개방+이상 표시.

## 4. 변경 파일

`tools/st01-p8/`: `java/st01proto/*.java`(10개 소스·테스트), `st01_p8_conformance.py`, `st01_p8_golem.py`, `st01_p8_verify.py`, `st01_p8_mutation_tests.py`, `build_p8_report.py`. `docs/design/st01-p8/`: 문서 8개 + `generated/`.

## 5. 검증 명령

```
python3 -I tools/st01-p8/st01_p8_verify.py <out_dir>          # javac 컴파일 + 하네스 + P7 차분 시험 + 골렘 기하
python3 -I tools/st01-p8/st01_p8_mutation_tests.py            # 음성 대조 {len(MU['results'])}개
python3 -I tools/st01-p8/build_p8_report.py .                 # 문서 재생성
```

## 6. 실제 서버 테스트 여부

**실행하지 않았다.** 프로젝트는 Java 25와 Minecraft 26.3(Loom)을 요구하나 이 환경은 Java 21만 있고 게임 서버·게임테스트 러너를 실행할 수 없다. 기존 `TombGuardianGameTests` 방식의 라이브 테스트가 후속 경로이다.

## 7. 남은 사용자 결정

- **승인 대기 변경:** CR-1(사망/승리 분리, 기존 보상 동작 변화), CR-2(참여 추적 구독)
- **권장안 승인 필요:** O-4 정지 지점·반경, O-6 크기·속도·생성 위치·구현 방식, O-5 낙하 가장자리, O-7, O-8, O-9 승리 규칙 수치(비율·창), O-10 추종 대상, O-13 앵커/등장 지점
- **후속 보류 유지:** O-1, O-12, O-14, D-3, Q15, Q98, Tomb Guardian 최종 사양

## 8. 하네스 테스트 목록

| ID | 내용 | 판정 |
| --- | --- | --- |
{jt}

## 9. 음성 대조

| 결함 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- |
{mut}

## 10. 전체 증거

| 측 | ID | 점검 | 판정 | 증거 |
| --- | --- | --- | --- | --- |
{evid}

## 11. 범위 밖 발견

없음. P1 O-1(권위 인덱스 동기화), P6 D-J(P2 승인 기록에 후보 C 반영)는 별도 승인 작업으로 남아 있다.
"""
w("ST01_P8_FINAL_REPORT_2026-11-07.md", final)
print("ok", cnt("PASS"), cnt("FAIL"), cnt("UNVERIFIED"))
