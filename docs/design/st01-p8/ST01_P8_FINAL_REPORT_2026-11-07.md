# ST-01 P8 — 최종 보고서 (2026-11-07)

상태: **프로토타입·기술 검증 보고 / 최종 보스·피라미드 구현 승인 아님.** 상태 표기: IMPLEMENTED(격리 하네스 구현), STATIC PASS, LIVE VERIFIED(없음), UNVERIFIED, BLOCKED.

- 기준: `work/st01-p7-secret-entrance-state-20261107` @ `5610683dd004071b0e9967e22d463e57d6b871ee` → 신규 `work/st01-p8-dummy-prototype-20261107`
- 변경 범위: `tools/st01-p8/`, `docs/design/st01-p8/`만(기존 Q01–Q100, P1–P7, `src/**` 변경 0)

## 1. 판정 요약

PASS 39 / FAIL 0 / UNVERIFIED 1. 음성 대조 16/16 검출(무결함 대조군 FAIL 0).

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
python3 -I tools/st01-p8/st01_p8_mutation_tests.py            # 음성 대조 16개
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
| T0 | no production default for the fallback timer (TEST-ONLY values never become defaults) | PASS |
| T1 | entrance cannot open before a valid defeat (timer, requests, spoofed signals) | PASS |
| T2 | environment-only death is not a victory: no entrance progress, chest sealed, no reward, retry allowed | PASS |
| T3 | administrative kill after participation is not a victory | PASS |
| T3b | a token player hit followed by an environmental death (share 10%, window long past) is not a victory (COMBINED) | PASS |
| T4a | valid victory starts the timer immediately at the defeat | PASS |
| T4b | golem spawn failure never stops the timer: FALLBACK_OPEN, cover applied once, reward granted once | PASS |
| T5a | duplicate defeat signals create exactly one golem and one guiding state | PASS |
| T5b | arrival opens (ENTRANCE_OPEN); duplicate arrival/open requests mutate the world exactly once; reward once | PASS |
| T5c | OPEN is terminal: no timer effect, no auto-restore | PASS |
| T6a | stalled golem (path failure) never opens by arrival; fallback opens | PASS |
| T6b | golem lost/killed while guiding: fallback opens | PASS |
| T7a | restart while guiding keeps state and remaining time; no golem is assumed | PASS |
| T7b | after restart the fallback still opens it | PASS |
| T7c | OPEN survives another restart, cover stays open, no second world mutation | PASS |
| T7d | OPEN record with reverted cover blocks (lost/regenerated chunk): the OPEN record is authoritative and the cover is re-opened idempotently (never closed) | PASS |
| T14 | respawn-after-restart option: exactly one new golem, state GUIDING, remaining time unchanged | PASS |
| T15 | OPEN record but no persisted victory (inconsistent save): cover is NOT opened, anomaly is flagged for reporting | PASS |
| T8 | unloaded cover chunk: record opens, blocks change only when the chunk loads, exactly once | PASS |
| T9 | lost defeat signal: the persisted victory record alone creates the entrance record and the timer | PASS |
| T10a | two simultaneous defeats: A opens by arrival while B keeps guiding (no cross-talk) | PASS |
| T10b | B opens by fallback on its own timer; one golem per instance; one reward per instance | PASS |
| T10c | a defeat in instance A never moves instance B | PASS |
| T11 | selector Nearest: never follows an offline player; empty when everyone is offline | PASS |
| T11 | selector StickyNearest: never follows an offline player; empty when everyone is offline | PASS |
| T11 | selector Farthest: never follows an offline player; empty when everyone is offline | PASS |
| T12 | matrix: COMBINED accepts a full player kill and rejects /kill | PASS |
| T13 | differential conformance: 4000 random event sequences / 29743 steps replayed against the P7 reference model give identical (state, cover, golems, victory, remaining, spawn-pending) | PASS |

## 9. 음성 대조

| 결함 | 기대 FAIL | 실제 FAIL | 결과 |
| --- | --- | --- | --- |
| timer only runs while guiding (golem spawn failure stalls it) | T4b | T13, T4b | 검출 |
| entrance progresses on any death, not on a valid victory | T2, T3, T3b | T2, T3, T3b | 검출 |
| boss marks victory/grants reward for invalid kills | T2, T3, T3b | T2, T3, T3b | 검출 |
| administrative kill accepted | T3 | T12, T3 | 검출 |
| second golem can be spawned by a repeated signal (coordinator and world-port guards both removed) | T5a | T13, T5a | 검출 |
| restart forgets the remaining time | T7a | T13, T7a | 검출 |
| restart forgets the entrance state | T7a | T13, T7a, T7c, T7d | 검출 |
| cover applied while the chunk is unloaded | T8 | T13, T8 | 검출 |
| OPEN record no longer repairs reverted blocks | T7d | T7d | 검출 |
| stalled golem can still arrive | T6a | T6a | 검출 |
| victory flag is global instead of per instance | T10c | T10c | 검출 |
| OPEN record without a victory still opens the cover | T15 | T15 | 검출 |
| timer expiry off by one | T13 | T13 | 검출 |
| chest interaction opens the entrance | T1 | T1, T13 | 검출 |
| a TEST-ONLY looking timer becomes the production default | T0 | T0 | 검출 |
| guide selector follows offline players | T11 | T11 | 검출 |

## 10. 전체 증거

| 측 | ID | 점검 | 판정 | 증거 |
| --- | --- | --- | --- | --- |
| Java | T0 | no production default for the fallback timer (TEST-ONLY values never become defaults) | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T1 | entrance cannot open before a valid defeat (timer, requests, spoofed signals) | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T2 | environment-only death is not a victory: no entrance progress, chest sealed, no reward, retry allowed | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T3 | administrative kill after participation is not a victory | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T3b | a token player hit followed by an environmental death (share 10%, window long past) is not a victory (COMBINED) | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T4a | valid victory starts the timer immediately at the defeat | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T4b | golem spawn failure never stops the timer: FALLBACK_OPEN, cover applied once, reward granted once | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T5a | duplicate defeat signals create exactly one golem and one guiding state | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T5b | arrival opens (ENTRANCE_OPEN); duplicate arrival/open requests mutate the world exactly once; reward once | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T5c | OPEN is terminal: no timer effect, no auto-restore | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T6a | stalled golem (path failure) never opens by arrival; fallback opens | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T6b | golem lost/killed while guiding: fallback opens | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T7a | restart while guiding keeps state and remaining time; no golem is assumed | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T7b | after restart the fallback still opens it | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T7c | OPEN survives another restart, cover stays open, no second world mutation | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T7d | OPEN record with reverted cover blocks (lost/regenerated chunk): the OPEN record is authoritative and the cover is re-opened idempotently (never closed) | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T14 | respawn-after-restart option: exactly one new golem, state GUIDING, remaining time unchanged | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T15 | OPEN record but no persisted victory (inconsistent save): cover is NOT opened, anomaly is flagged for reporting | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T8 | unloaded cover chunk: record opens, blocks change only when the chunk loads, exactly once | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T9 | lost defeat signal: the persisted victory record alone creates the entrance record and the timer | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T10a | two simultaneous defeats: A opens by arrival while B keeps guiding (no cross-talk) | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T10b | B opens by fallback on its own timer; one golem per instance; one reward per instance | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T10c | a defeat in instance A never moves instance B | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T11 | selector Nearest: never follows an offline player; empty when everyone is offline | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T11 | selector StickyNearest: never follows an offline player; empty when everyone is offline | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T11 | selector Farthest: never follows an offline player; empty when everyone is offline | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T12 | matrix: COMBINED accepts a full player kill and rejects /kill | PASS | in-memory harness run (javac 21); no Minecraft server |
| Java | T13 | differential conformance: 4000 random event sequences / 29743 steps replayed against the P7 reference model give identical (state, cover, golems, victory, remaining, spawn-pending) | PASS | in-memory harness run (javac 21); no Minecraft server |
| E | GG1 | Scaled iron-golem body (scale 0.4..1.0) reaches every arrival-zone class from every hall start (HALL_DOOR, 4 emergence candidates) with the vanilla path-node footprint; scale 1.5 reported separately | PASS | s=0.4: bbox 0.56x1.08, node 1x1x2, zones reached (min over starts) 5/5; s=0.5: bbox 0.7x1.35, node 1x1x2, zones reached (min over starts) 5/5; s=0.6: bbox 0.84x1.62, node 1x1x2, zones reached (min over starts) 5/5; s=0.7: bbox 0.98x1.89, node 1x1x2, zones reached (min over starts) 5/5; s=0.8: bbox 1 |
| E | GG2 | Main-route / hall-door clear sections (from the P2/P3 models) vs golem node footprint: narrowest section and the largest scale whose node size fits it | PASS | sections {'C1': (7, 5), 'C2': (7, 5), 'C3': (7, 5), 'R': (5, 4), 'T': (3, 3), 'HALL_DOOR': (3, 3)}; narrowest clear 3 wide x 3 high; fits per scale {0.4: True, 0.5: True, 0.6: True, 0.7: True, 0.8: True, 1.0: True, 1.5: False}; the native-size golem (scale 1.0, node 2x2x3) fits |
| E | GG3 | Stop-point safety: largest scale whose body stays fully off the cut when its centre stands in the nearest zone Z0 (centre-to-cut 0.5) = 2*d/1.4; and the cut (4x3) can host a fully unsupported body for every scale <= 2.14, so stop points must be outside the cut | PASS | Z0 nearest centre-to-cut 0.5; max scale with body disjoint from the cut at Z0 = 0.714; partially overhanging bodies stay supported by the remaining floor in vanilla physics (UNVERIFIED in-game), fully unsupported bodies fall |
| W | GG1 | Scaled iron-golem body (scale 0.4..1.0) reaches every arrival-zone class from every hall start (HALL_DOOR, 4 emergence candidates) with the vanilla path-node footprint; scale 1.5 reported separately | PASS | s=0.4: bbox 0.56x1.08, node 1x1x2, zones reached (min over starts) 5/5; s=0.5: bbox 0.7x1.35, node 1x1x2, zones reached (min over starts) 5/5; s=0.6: bbox 0.84x1.62, node 1x1x2, zones reached (min over starts) 5/5; s=0.7: bbox 0.98x1.89, node 1x1x2, zones reached (min over starts) 5/5; s=0.8: bbox 1 |
| W | GG2 | Main-route / hall-door clear sections (from the P2/P3 models) vs golem node footprint: narrowest section and the largest scale whose node size fits it | PASS | sections {'C1': (7, 5), 'C2': (7, 5), 'C3': (7, 5), 'R': (5, 4), 'T': (3, 3), 'HALL_DOOR': (3, 3)}; narrowest clear 3 wide x 3 high; fits per scale {0.4: True, 0.5: True, 0.6: True, 0.7: True, 0.8: True, 1.0: True, 1.5: False}; the native-size golem (scale 1.0, node 2x2x3) fits |
| W | GG3 | Stop-point safety: largest scale whose body stays fully off the cut when its centre stands in the nearest zone Z0 (centre-to-cut 0.5) = 2*d/1.4; and the cut (4x3) can host a fully unsupported body for every scale <= 2.14, so stop points must be outside the cut | PASS | Z0 nearest centre-to-cut 0.5; max scale with body disjoint from the cut at Z0 = 0.714; partially overhanging bodies stay supported by the remaining floor in vanilla physics (UNVERIFIED in-game), fully unsupported bodies fall |
| E | P7.GA1 | (re-run of the P7 geometry on the same world) Player standing on a cover cell when the cover opens (FIND-P6-2): each of the 12 columns is standable while CLOSED, the player lands on a standable cell (no burial), headroom >= 4, can climb back to the hall floor and can walk on to the vault; drop <= 4 (FIND-P6-1) | PASS | max drop 4 (fall damage 1 under the vanilla 'above 3' rule); per column (drop/headroom): (-9, 33):1/5, (-9, 34):1/5, (-9, 35):1/5, (-8, 33):2/5, (-8, 34):2/5, (-8, 35):2/5, (-7, 33):3/5, (-7, 34):3/5, (-7, 35):3/5, (-6, 33):4/5, (-6, 34):4/5, (-6, 35):4/5; all can climb out True; all reach vault ent |
| E | P7.GA6 | (re-run of the P7 geometry on the same world) While SEALED / GUARDIAN_DEFEATED / GOLEM_GUIDING the world is the CLOSED world: hall voxel-identical to P3 above the floor top, so golem walking and boss/player movement see the P3-verified floor | PASS | differing cells 0 |
| W | P7.GA1 | (re-run of the P7 geometry on the same world) Player standing on a cover cell when the cover opens (FIND-P6-2): each of the 12 columns is standable while CLOSED, the player lands on a standable cell (no burial), headroom >= 4, can climb back to the hall floor and can walk on to the vault; drop <= 4 (FIND-P6-1) | PASS | max drop 4 (fall damage 1 under the vanilla 'above 3' rule); per column (drop/headroom): (6, 33):4/5, (6, 34):4/5, (6, 35):4/5, (7, 33):3/5, (7, 34):3/5, (7, 35):3/5, (8, 33):2/5, (8, 34):2/5, (8, 35):2/5, (9, 33):1/5, (9, 34):1/5, (9, 35):1/5; all can climb out True; all reach vault entry True |
| W | P7.GA6 | (re-run of the P7 geometry on the same world) While SEALED / GUARDIAN_DEFEATED / GOLEM_GUIDING the world is the CLOSED world: hall voxel-identical to P3 above the floor top, so golem walking and boss/player movement see the P3-verified floor | PASS | differing cells 0 |
| E/W | EW1 | East and west golem-size/stop-safety results are identical (mirror) | PASS | scales/passages equal: True |
| - | LV1 | Real Minecraft server: entity collision/pathfinding/scale attribute, block updates, chunk reload, multiplayer, real boss death sources, damage events | UNVERIFIED | needs Java 25 + Minecraft 26.3 toolchain and the gametest runner; not available here; no production code was changed |

## 11. 범위 밖 발견

없음. P1 O-1(권위 인덱스 동기화), P6 D-J(P2 승인 기록에 후보 C 반영)는 별도 승인 작업으로 남아 있다.
