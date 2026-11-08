# INTEGRATION-02 checkpoint — Phase B 구현 / C 보류

기준: 원격 feature/sky-tower-integration-01 @387a50c 일치, 시작 clean. 새 feature/sky-tower-integration-02. 승인 건축16ad9d5, Q01–Q118, P1–P6, INTEGRATION-01 원본 불변.

## 완료와 증거

- A: 기존 생성·저장·파괴 경로 직접 감사. 실제 구형 탑은 별도 jigsaw child다. 여섯 스타일의43×74×43 parent/상부58 여유와 승인50×80×49가 맞지 않는다. 중심 원점(-3,17,-3), 입구 아래(46,y≤16,21)에 지지면0개. audit_worldgen.py/worldgen_audit.json 및 WORLDGEN_REPLACEMENT.md 참조.
- B: 런타임72셀 소유권, 실제 블록 제거 후 파괴 완료, 첫 완료 원인 보존, schema1 하위호환, 완료층 재설치 금지, 외부 블록 보존 구현. 별도 보상 없음. 독립 리뷰 및 엔티티 조회 halo scoped 리뷰에서 잔여 확정 결함 없음.
- 단위: 기준583 PASS. 추가 lifecycle/인접 저장 경계5개 포함 최종588 PASS(unit_summary.json).
- 집중 GameTest: 최종 halo_build.txt에서 신규3 PASS 및 build 성공. 구버전/다른 탑 격리·실제 스포너 제거·쿨다운·외부 블록/엔티티 검사.
- 실제 서버: 4회전 각72/총288 barrier 설치, 혼합 원인 완료, 외부 stone 보존, 스포너 재설치로 진행 초기화 없음, 청크 언로드/로드, 정상 서버 재시작 및 저장된 소유권으로18셀 제거 PASS. 실제32금고 vanilla 설정 PASS. 최종 halo 코드로 barrier 재설치와 실제 제거 완료를 다시 확인(live_check.json). 서버는 정상 종료했다.

## 회귀 실행을 구분할 것

1. baseline_regression.txt: 예전 GameTest 월드의01시 콜로세움 ArenaRecovery 저널을07시에 재사용해18건 실패. 원본은 build/run/gameTest.integration02-contaminated-20261108-0719에 보존. baseline_failures.json의 timestamps/해시 참조. 콜로세움 코드는 수정하지 않았다.
2. final_build.txt: 새530 실행에서 신규 엔티티 로드 경계1건 RED를 찾았다. 이후 실행 중 다른 Gradle compileJava가 클래스 출력을 바꿔 RewardDelivery$State lazy-load에서 NoClassDefFoundError로 중단됐다. 전체 기존527 PASS가 아니다. 이 동시 실행 순서 문제를 감추지 않는다. 중단 월드는 build/run/gameTest.integration02-interrupted-20261108-0736에 보존.
3. focused_build.txt → halo_build.txt: 엔티티 저장 로드와 바닐라 x/z±2 조회 halo 검사로 수정, 최종 신규3 GameTest와588 unit/build PASS.
4. **serial_final_build.txt 실행 중**: 다른 Gradle/서버를 모두 종료하고 소스를 동결한 뒤 새 GameTest 월드에서 전체530을 단일 build로 실행. final_source_freeze.json 기록. 종료 전까지 Java·리소스를 편집하거나 다른 Gradle 작업을 시작하지 않는다. 문서/Git 작업만 가능.

## 재개

```sh
git status --short
tail -30 docs/planning/sky_tower_integration_02/serial_final_build.txt
```

현재 최종 단일 build 결과를 기다리고 있다. 종료 후 결과를 TEST_REPORT/INTEGRATION_02_REPORT에 반영하고 변경 파일·동결 해시·원본 보존을 확인하여 커밋·원격 푸시한다. Java25/프록시/전용 서버 절차는 REPRODUCE.md. 실제 서버 probe는 x10000..10900을 이미 사용했고 restore/check가 진행을 더 전진시켰으므로 그대로 재실행하지 않는다. 새 시험은 새 빈 PROBE_ORIGIN_X로 setup부터 수행한다. RCON 비밀은 저장소에 넣지 않는다.

## 미완료

C: 생산 자연 생성 교체와 자동 등록은 미연결. 기존 정책/구형 탑을 보존했고 섬을 임의 확대하지 않았다. 최소 빈 경계50×97×49 및 물리 접속 후보는 WORLDGEN_REPLACEMENT.md. 이 수용 변경은 사용자 검토가 필요하다.

D: 실제 플레이어 전투·금고 사용·클라이언트·멀티플레이·재접속은 미실행. 전원 차단/쓰기 실패 원자성도 미검증. 옥상 특별 보상은 승인된 보류 항목. **INTEGRATION-02 전체 완료 또는 FULLY LIVE VERIFIED를 선언하지 않는다.**
