# INTEGRATION-02 체크포인트 — 검증된 B 보존, C 보류

> Phase C 후속 편심 좌표 비교(미채택): [PHASE_C_CANDIDATES.md](PHASE_C_CANDIDATES.md). 실제 통행·지지 및 월드젠 구현은 미검증.

## 2026-11-08 재개 감사

원격 `feature/sky-tower-integration-02`의 기존 HEAD `e05d5054ac8c1691de9a9b42933e47462cd19c35`는 01 `387a50c`의 후손이며, 이미 존재하는 검증된 작업을 재사용했다. 승인 NBT 동결 및 16 스포너·8 금고·72 공기 셀을 재확인했고 `audit_worldgen.py`의 여섯 섬 소켓/입구 반례가 재현됐다. 독립된 새 작업 트리에서 `./gradlew --no-daemon test`는 BUILD SUCCESSFUL. 오프라인 첫 시도는 캐시에 Loom이 없어 시작하지 못했고 네트워크 의존성 해소 후 통과했다. 이 재개 턴에서 GameTest 전체·실제 플레이를 새로 통과시켰다고 주장하지 않는다.

`island_overlap_probe.py` / `island_overlap_probe.json`은 중심 정렬 진단 원점 `(-3,17,-3)`에서 승인 탑의 비공기 블록이 여섯 섬의 기존 비공기 블록과 각각 198/220/334/9/4/11개 겹침을 기록한다. 특히 눈·잎·나무·지형과 겹친다. 이는 고정된 중심 배치의 실제 좌표 집합 중첩이며 다른 배치 전체의 불가능성 증명이 아니다. 렌더·프로세서·회전·청크 분할을 통한 실제 월드 생성 결과는 아니다.

`island_offset_screen.py` / `island_offset_screen.json`은 y17, 기본 회전에서 x/z 편심 각각 −24..24, 섬마다 2,401개를 추가로 검사한다. 기존 비공기 블록과 중첩 0이면서 입구 아래 y16에 비공기 블록이 적어도 하나 있는 후보는 snowy/meadow/forest 각 0개, badlands 4개, desert 30개, rocky 1개였다. 비공기는 낙관적인 지표일 뿐 단단한 지지·5칸 접근을 증명하지 않는다. 다른 높이·회전·연결대는 검사하지 않았으므로 전체 불가능성 결론이 아니다.

입구 후보 앞 연결면 `x41..45,y16,z19..23` 25칸과 그 앞쪽 머리 공간 `x40..45,y17..18,z19..23`은 여섯 NBT에서 기존 비공기 블록과 중첩이 없었다. 이것은 국소 후보 좌표 증거에 불과하며 지지·접근·기존 식생 보존·전체 경로·회전·월드젠 검증 또는 승인된 접속 디자인이 아니다. 섬/탑 전체 중첩과 빈 경계/높이 문제는 별개다.

사용자 지시의 '기존 섬을 수용하기 위해 중대한 건축 변경이 필요하다면 임의로 변경하지 않는다'는 중단 조건에 따라, 기존 식생/지형과의 중첩을 어떻게 보존할지 및 접속·배치 방식을 확정하기 전 Phase C 생산 연결은 보류한다. Phase B를 수정하거나 승인 원본을 덮어쓰지 않았다. 이후 후보별로 실제 섬 손상·출입 경로·jigsaw 경계·높이·자동 등록을 시험해야 한다.

기준: 원격 feature/sky-tower-integration-01 @387a50c, 시작 clean. 새 feature/sky-tower-integration-02. WIP050ebfc 원격 푸시 완료. 본 파일을 포함하는 후속 검증/수정 커밋은 `git log -2 --oneline`과 최종 응답에서 확인한다. 승인 건축16ad9d5, Q01–Q118, P1–P6,01 산출물은 변경하지 않았다.

## 구현·검증 완료 범위

- A: 기존 생성/저장/파괴 경로 직접 감사. 실제 구형 탑은 별도 jigsaw child다. 여섯 스타일43×74×43 parent/상부58 여유와 승인50×80×49가 맞지 않는다. 중심 원점(-3,17,-3), 입구 아래 열에 지지면0개. audit_worldgen.py/worldgen_audit.json 및 WORLDGEN_REPLACEMENT.md 참조.
- B:72셀 소유권과 정확한 건조 기본 상태 검증, 실제 성공 변경 후 스포너 파괴 완료, 첫 완료 원인 보존, schema1 하위호환, 완료층 재설치 금지, 외부 블록/상태 보존 구현. 엔티티 저장과 바닐라 인접 조회 halo가 미준비면 강제 로드 없이 보류한다. 별도 보상/보호/복귀/가상 엔티티 없음. 독립 코드 리뷰 및 halo/정확한 상태 scoped 리뷰의 잔여 확정 결함 없음.
- 최종 unit588 PASS(기존583+추가5), 집중 서버 GameTest4 PASS, 데이터 검증/패키징 성공. waterlogged_green_build.txt 및 unit_summary.json 참조. package_verification.json은 최종 JAR과 내부 승인 NBT 해시를 기록한다.
- 실제 모드 서버:4회전 각72/총288 barrier, 혼합 완료·외부 stone 보존·스포너 재설치 시 진행 보존·청크 언로드/로드·16개 인스턴스 재시작 복원 PASS. 저장 소유권으로18셀 해제를 확인했다. 최종 halo 코드로 재설치/완료 해제를, 최종 exact-state 코드로 외부 waterlogged 보존을 확인했다.32금고 vanilla 설정도 PASS. live_setup/restore/check/state/vaults.json 및 서버 로그 발췌 참조. 전용 서버는 정상 종료했다.

## 전체 회귀의 한계와 실행 이력

1. 기준 재사용 월드:18건 콜로세움 실패.01시 ArenaRecovery 저널을07시에 재사용해65분 제한을 넘겼다. 원본은 build/run/gameTest.integration02-contaminated-20261108-0719에 보존. baseline_failures.json 참조. 콜로세움 코드는 수정하지 않았다.
2. 중간 새530: 엔티티 저장 미로드 RED를 찾았고, 동시 compileJava의 클래스 출력 교체로 실행 중 JVM이 RewardDelivery$State를 읽지 못해 중단됐다(final_build.txt). 전체527 PASS가 아니다. 원본 월드는 build/run/gameTest.integration02-interrupted-20261108-0736에 보존했다.
3. 다른 실행/소스를 동결한 최종 단독530:529 PASS·기존 absenceGraceResumesOnReturnAndExpiresOtherwise1 FAIL(serial_final_build.txt).1.5초 테스트 유예와 tick 기반 복귀 가정이 배치 지연에서 어긋났다. 같은 기존 테스트를 코드/값 변경 없이 새 월드에서 단독 실행하면 PASS(legacy_grace_isolated.txt). 전체 배치가 녹색이라고 하지 않는다.
4. 신규 결계 경계: 엔티티 저장/조회 halo와 waterlogged 상태 RED→GREEN, 최종 신규4 GameTest/588unit/패키징 PASS. 상세 TEST_REPORT.md. serial_source_freeze.json은 단독530 당시, final_source_freeze.json은 최종 수정본 소스 해시다.

## 재개 명령과 데이터

```sh
git switch feature/sky-tower-integration-02
git log -2 --oneline
git status --short
python3 -I docs/planning/sky_tower_integration_01/verify_template.py
python3 -I docs/planning/sky_tower_integration_02/audit_worldgen.py
```

Java25/프록시/새 GameTest 월드/단독 실행 순서는 REPRODUCE.md를 따른다. 같은 build/classes를 쓰는 서버가 실행 중일 때 다른 Gradle 컴파일을 하지 않는다. 기존 테스트 월드는 삭제하지 않고 보존 이동했다. `/tmp/sky-tower-integration02-focused`에도 이전 focused/world를 별도 이름으로 보존했다.

전용 실제 월드는 `/tmp/sky-tower-integration-server`, 포트25595/RCON25596. 암호는 저장소에 넣지 않았다. x10000..10900은 이미 사용했고 restore/check/state가 진행을 전진시켰으므로 그대로 재실행하지 않는다. 새 검증은 새 빈 PROBE_ORIGIN_X로 setup부터 수행한다. 일반 개발/플레이 월드를 probe에 사용하지 않는다. `/tmp` 보존은 영구 보장되지 않는다.

## 남은 범위

C: 생산 자연 생성 교체/자동 등록은 미연결. 기존 정책/구형 탑을 보존했고 섬을 임의 확대하지 않았다. 빈 경계50×97×49(+7/+23/+6), 물리 접속/편심 배치 후보와 높이 위험은 WORLDGEN_REPLACEMENT.md. 섬 접속 변경 검토 후 생성 어댑터·자동 등록·신규 청크/회전/간섭 시험을 이어가야 한다.

D: 실제 플레이어 전투·금고 사용·클라이언트·멀티플레이·재접속은 미실행. 전원 차단/쓰기 실패 원자성도 미검증. 옥상 특별 보상은 승인된 보류 항목이다. **INTEGRATION-02 전체 완료 또는 FULLY LIVE VERIFIED를 선언하지 않는다.**

현재 등록된 전체 GameTest는531개(기존527+신규4)다. serial 전체 실행은 네 번째 waterlogged 검사를 추가하기 전530개였으며, 그 뒤 정확한 상태 소유권의 국소 수정은 신규4개 집중 실행과 전체588 unit/패키징으로 검증했다. 최종531개 전체 재실행은 NOT RUN이다.
