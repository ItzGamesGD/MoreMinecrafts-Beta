# INTEGRATION-02 재현

기준387a50c → feature/sky-tower-integration-02. Java25/Minecraft26.3/Fabric0.161.0+26.3/Loom1.17.21. 원격01과 승인16ad9d5의 원본은 보존한다.

```sh
python3 -I docs/planning/sky_tower_integration_01/verify_template.py
python3 -I docs/planning/sky_tower_integration_02/audit_worldgen.py
./gradlew test
./gradlew build
```

이 환경의 Java/프록시 옵션:

```sh
JAVA_HOME=/workspace/toolchains/jdk-25.0.4.1 GRADLE_USER_HOME=/workspace/gradle-home ./gradlew -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon build
```

## GameTest의 깨끗한 저장 상태

기준 회귀에서 `build/run/gameTest/world/data/moreminecrafts-st05`의 예전 콜로세움 복구 저널이 재사용되어18건이 실패했다. deleteGameTestRunDir task가 모든 독립 저널을 새 월드로 초기화한다고 가정하지 않는다. 테스트 서버 종료 후 `build/run/gameTest`가 이미 있으면 **테스트 월드임을 확인하고** 다른 고유 이름으로 보존 이동한 뒤 build한다. 이 작업에서는 `build/run/gameTest.integration02-contaminated-20261108-0719`에 원본을 보존했고 `baseline_failures.json`에 파일 timestamps/해시를 기록했다. 사용자 플레이 월드나 실행 중인 월드는 이동하지 않는다. 중간 final_build.txt는 클래스 로딩 충돌로 중단됐다. 새 단독 전체530 결과는 serial_final_build.txt(529통과/기존1실패), 최종 새4건 집중 결과는 waterlogged_green_build.txt를 확인한다.

## 실제 모드 서버

01에서 사용한 `/tmp/sky-tower-integration-server` 전용 월드/포트25595·RCON25596을 사용한다. localhost, 임의 비밀 암호, pause-when-empty-seconds=0, eula준비가 필요하다. 비밀 암호는 저장소에 넣지 않는다. 일반 개발 월드에 아래 probe를 실행하지 않는다.

```sh
./gradlew -I docs/planning/sky_tower_integration_02/server.init.gradle runServer --args='nogui'
python3 -I docs/planning/sky_tower_integration_02/live_probe.py setup
python3 -I docs/planning/sky_tower_integration_02/live_probe.py stop
# 같은 전용 월드로 위 서버를 다시 시작
python3 -I docs/planning/sky_tower_integration_02/live_probe.py restore
python3 -I docs/planning/sky_tower_integration_02/live_probe.py vaults
python3 -I docs/planning/sky_tower_integration_02/live_probe.py check
python3 -I docs/planning/sky_tower_integration_02/live_probe.py state
python3 -I docs/planning/sky_tower_integration_02/live_probe.py stop
```

현재 환경에서는 위 Java/프록시 옵션도 붙인다. setup은 전용 월드의 forceload 티켓을 비우고 기본 원점x10000/10300/10600/10900,y65,z1000에4방향 탑을 새로 설치한다. 이미 사용한 원점은 재사용하지 말고 `PROBE_ORIGIN_X`를 새 빈 원점으로 지정하고 restore에도 동일 값을 준다. 기존 인스턴스의 진행을 초기화하지 않는다.

setup은 실제 명령 블록 제거와 합성 cooldown을 사용한다. 일반 플레이어 전투·금고 사용·클라이언트/멀티플레이 시험이 아니다. restore는 종료 전 전체 레지스트리 문자열을 비교하고, 아직 미완료였던 두 번째 탑의 스포너4개를 제거해 **저장된 소유권으로 결계를 제거할 수 있는지** 검사한다. 따라서 restore는 상태를 추가 진행시키므로 같은 setup 결과에 대해1회만 사용한다. 반복 restore가 필요하면 새 setup/별도 전용 월드를 사용한다.

생산 월드젠은 아직 구형 탑 경로다. 자동 등록을 수동 명령으로 대신했다고 주장하지 않는다. Phase C 최소 변경 제안은 WORLDGEN_REPLACEMENT.md.


## 최종 검사의 실행 순서

같은 build/classes를 쓰는 개발 서버 또는 GameTest가 실행 중일 때 다른 Gradle 컴파일을 병렬 실행하지 않는다. 이번 중간 회귀에서 컴파일 출력 교체와 기존 JVM의 lazy class loading이 충돌했다. 최종 검사는 모든 다른 서버/Gradle을 종료하고 소스를 동결한 뒤, 새 GameTest 월드로 단일 build를 실행했다(serial_final_build.txt).

집중 재현은 `./gradlew -I docs/planning/sky_tower_integration_02/focused.init.gradle build`. 이 경우 서버 GameTest는 천공탑4건으로 필터링되고 전체 unit/패키징은 수행된다. 전체530 실행과 구분한다. `/tmp/sky-tower-integration02-focused/world`가 이전 실행에서 남아 있으면 서버 종료 후 고유 이름으로 보존 이동해 새 월드로 재현한다. halo_build.txt는 이전 집중 월드를 world.before-halo로 보존한 다음 실행했다.

`check`는 restore 종료 상태를 대조한 뒤 세 번째 탑의 소유 결계1개를 삭제·재설치하고 필수 스포너4개 제거로18셀 해제까지 검사한다. 상태를 전진시키므로 setup/restore/check/state는 각1회 순서대로 실행한다. `vaults`는32금고의 vanilla 설정을 읽는 검사다.26.3 VaultConfig는 기본 loot_table 값을 저장할 때 생략하므로, 필드가 없으면 공식 기본값 TRIAL_CHAMBERS_REWARD로 해석한다(vault_codec_reference.txt).


`state`는 네 번째 탑의 소유 barrier를 외부 waterlogged 상태로 변경한 뒤 필수 스포너4개 제거 후에도 그 상태가 보존되고 다른 소유 벽만 해제되는지 검사한다. 최종 build 증거는 waterlogged_green_build.txt(전체 unit588+집중 GameTest4+패키징)다. 전체 배치 serial_final_build.txt는529/530이며 다음 기존 테스트 단독 재현과 구분한다.

```sh
./gradlew -Dmmskytower.testFilter='moreminecrafts-gametest:colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise' -I docs/planning/sky_tower_integration_02/focused.init.gradle runGameTest
```

단독 기존 테스트 실행 전에도 focused/world를 새로 준비한다. 위 격리 실행은 기존1.5초 테스트를 통과시켰지만 전체 배치의 부하 민감성을 수정한 것은 아니다. 콜로세움 코드·유예값은 변경하지 않았다.
