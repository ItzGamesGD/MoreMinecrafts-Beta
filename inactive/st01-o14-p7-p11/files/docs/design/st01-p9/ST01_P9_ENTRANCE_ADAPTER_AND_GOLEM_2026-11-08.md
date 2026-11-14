# ST-01 P9 — 비밀 입구 운영 어댑터와 소형 금 골렘 (2026-11-08)

상태: **IMPLEMENTED / COMPILE PASS / UNIT PASS / GAMETEST PASS.** 구조물·월드젠이 없어 합성 아레나에서만 검증했다. 렌더링·실제 플레이는 UNVERIFIED.

## 1. 새로 구현한 것 (기존 코드에는 없던 기능)

| 클래스(`dev.itzgamesgd.moreminecrafts.st01`) | 역할 |
| --- | --- |
| `EntranceCore` | Minecraft 의존 없는 인스턴스별 P7 상태기계(P8 프로토타입 포팅). 신호는 비권위, 진실은 영속 승리 기록 + 입구 레코드 + 월드 블록. 모든 연산 멱등, CLOSED로 되돌리는 전이 없음 |
| `EntranceState`, `EntranceConfig` | 5개 상태, 설정(**운영 기본값 없음**: 대체 개방 시간·도착 반경 미승인, 미설치 시 런타임 비활성) |
| `SecretEntranceData` | 차원별 SavedData(`moreminecrafts:st01_secret_entrance`), 무덤 앵커 키(= `TombEncounterData` 키). 상태·남은 시간·덮개 적용·참여자·방향 영속. `put`이 상태 후퇴를 거부 |
| `EntranceLayout` | 앵커(왕실 상자) 기준 구조물 상대 기하: P6 12칸 절개(dx 12..15, dy −3, dz 0..2; 서쪽은 X 반전, 승인된 기하 그대로), **PROPOSED** 정지 지점(Z2)·골렘 생성 칸. D-3 미확정이라 방향은 항상 명시 인자 |
| `McPorts` | 포트의 Minecraft 구현: 승리 기록 조회, 청크 로드 확인, 12칸 제거(공기가 아닌 칸만 제거, 되돌리는 코드 없음), 인스턴스별 골렘 조회/생성, **서버 권위 도착 판정**(골렘 서버 위치 ≤ 반경, 지상) |
| `SecretEntranceRuntime` | `TombGuardianEncounters.VICTORY` 구독(참여자만 기록; 상태 전이는 다음 틱), `END_SERVER_TICK`에서 인스턴스별 `EntranceCore.step`, 리더(StickyNearest) 선정, 레코드 저장. 인스턴스 등록 API는 있으나 **운영에서 호출하는 곳 없음**. `TestHooks`(테스트 전용 스폰 실패) |
| `GoldGuideGolem`, `GoldGuideGolemTuning`, `SecretEntranceEntities` | 소형 금 골렘 엔티티(별도 `PathfinderMob`, 아이언 골렘 아님) |
| `GuideTargetSelector` | StickyNearest(마진 후보값) |
| 클라이언트 `GoldGuideGolemRenderer` + 텍스처 + 번역 | 바닐라 아이언 골렘 모델·애니메이션 재사용, 금색 텍스처(아이언 골렘 텍스처를 색조 변환) |

P7/P8의 상태 전이 규칙은 그대로다: 유효 승리 → 타이머 즉시 시작(골렘 생성 성공 여부와 무관) → 골렘 도착이면 ENTRANCE_OPEN, 시간 만료이면 FALLBACK_OPEN → 레코드 영속 → 재시작/청크 재로드 시 OPEN 재적용 → 자동 복원 없음.

## 2. 보스와의 연결 방식

기존 보스 코드를 수정한 곳은 CR-1/CR-2 범위뿐이다. 입구는 `VICTORY` 이벤트를 구독하고, 유실 대비로 틱마다 `TombEncounterData.isVictory`를 다시 읽어 레코드를 재조정한다(이벤트는 신호일 뿐). 이벤트는 보스 `die()` 처리 중 호출되므로 골렘 생성은 다음 틱에서 일어난다(보스 사망 처리는 변경하지 않음).

## 3. 소형 금 골렘 — 기술 결과

| 항목 | 결과 |
| --- | --- |
| 방식 | **별도 소형 엔티티**(P8 권장 B) + `Attributes.SCALE` 단일 원천 |
| 모델/애니메이션 | 바닐라 `IronGolemModel` 재사용(렌더러 컴파일 PASS; 화면 확인은 UNVERIFIED) |
| 외형 | 금색 텍스처 128×128 (아이언 골렘 텍스처 재색) |
| **발견: SCALE 기본값만으로는 충돌 상자가 줄지 않음** | 엔티티 생성자 시점에는 속성이 없어 `refreshDimensions()`가 스케일 1로 계산됨. 게임테스트에서 `scale=0.5`인데 상자 1.4×2.7로 측정됨. 생성자 끝에서 `refreshDimensions()` 호출로 해결. **게임테스트 실측: 상자 0.7×1.35, 스케일 0.5** (GAMETEST PASS) |
| 후보 크기(미승인) | scale 0.5, 속도 속성 0.25, 리더 대기 거리 10칸 — 모두 `GoldGuideGolemTuning` 상수, 최종값 아님 |
| 경로 | 지상 내비게이션으로 정지 지점까지 이동, 리더가 대기 거리보다 멀면 정지(대기). 서버가 게임테스트에서 실제 도착 시 입구를 열었음(지상, 도착 반경 이내, 절개 칸 위가 아님) |
| 안전 정지 지점 | 구역 Z2(덮개에서 중심 간 1.58칸, P7/P8) 후보를 `EntranceLayout.stopPoint`로 구현. 합성 아레나(평지)에서 검증. 실제 홀(제단·기둥)에서의 실경로는 구조물이 없어 UNVERIFIED |
| 보스 AI와 분리 | 별도 클래스, 보스 코드와 무관 |
| 저장 | `noSave`: 재시작·청크 언로드 시 사라지고, 타이머가 개방을 보장(재생성 옵션 `respawnAfterRestart`은 구현됐으나 기본 꺼짐; 단위 테스트만) |
| 무적 | 일반 피해 무시, 허공·/kill은 통과(소실 시 타이머로 대체 개방) |

## 4. 아직 막혀 있는 결정 (임의 확정하지 않음)

| ID | 항목 | 운영에서의 처리 |
| --- | --- | --- |
| O-1 | 대체 개방 시간 | 기본값 없음. 설정 미설치면 입구 런타임 비활성 |
| O-4 | 도착 반경 | 기본값 없음(설정 필수). 정지 지점은 PROPOSED Z2 |
| O-6 | 골렘 최종 크기·속도·생성 위치 | `GoldGuideGolemTuning` 후보 상수(0.5 / 0.25 / 대기 10칸), 최종값 아님 |
| O-9 | 승리 판정 수치(피해 비율·시간 창) | 운영은 수치 없는 구조 최소 규칙. 수치 규칙은 `Params` 명시 설치(테스트 전용) |
| O-12 | 최종 개방 연출 | 구현 안 함(블록 즉시 제거) |
| O-14 | 월드젠 배치 | 입구 인스턴스를 등록하는 운영 코드 없음(런타임은 사실상 비활성) |
| D-3 | 동·서 방향 | `Orientation`은 항상 명시 인자 |
| Q15 / Q98 | 비밀 묘실 사양 / 최대 깊이 | 이번 코드가 사용하지 않음 |
| O-5 | 낙하 가장자리 | 변경하지 않음. 게임테스트 실측: 4블록 낙하, 생물 체력 20→19 |
| O-7 / O-8 | 재시작 시 골렘 재생성 / 저장 불일치 | 재생성 옵션(기본 꺼짐), 불일치 시 열지 않음+이상 표시(`EntranceCore.anomaly`) — 단위 테스트까지 |
