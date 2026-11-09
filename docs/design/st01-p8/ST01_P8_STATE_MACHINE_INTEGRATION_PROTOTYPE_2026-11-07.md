# ST-01 P8 — 상태기계 통합 프로토타입 (2026-11-07)

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
