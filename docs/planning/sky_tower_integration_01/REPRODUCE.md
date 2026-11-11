# 통합 재현

Java25/Minecraft26.3/Fabric0.161.0+26.3, Loom1.17.21.

```sh
./gradlew test
python3 -I docs/planning/sky_tower_integration_01/verify_template.py
```

현재 환경에서는 Java가 환경 HTTP(S)_PROXY를 자동 사용하지 않아 최초 플러그인 해석이 실패했다. 아티팩트200응답 확인 후 아래 Java 프록시/시스템CA 옵션으로 해소했다. 프로젝트 의존성 버전은 바꾸지 않았다.

```sh
JAVA_HOME=/workspace/toolchains/jdk-25.0.4.1 GRADLE_USER_HOME=/workspace/gradle-home ./gradlew -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts --no-daemon test
```

## 격리 서버

`/tmp/sky-tower-integration-server`에 eula.txt와 server.properties를 준비한다. 로컬주소127.0.0.1, 게임25595/RCON25596, 임의 비밀 암호, online-mode=false, whitelist=false, pause-when-empty-seconds=0. vanilla flat 테스트 월드이며 자연 생성 승인 시험이 아니다. `/tmp`는 영구 보존을 보장하지 않는다.

```sh
./gradlew -I docs/planning/sky_tower_integration_01/integration_server.init.gradle runServer --args='nogui'
python3 -I docs/planning/sky_tower_integration_01/live_probe.py setup
python3 -I docs/planning/sky_tower_integration_01/live_probe.py chunk_cycle
python3 -I docs/planning/sky_tower_integration_01/live_probe.py stop
# 같은 전용 월드로 서버를 다시 시작한 뒤
python3 -I docs/planning/sky_tower_integration_01/live_probe.py restore
python3 -I docs/planning/sky_tower_integration_01/live_probe.py stop
```

현재 환경의 프록시/Java 옵션은 위와 같이 덧붙인다. init script는 Loom runDir을 전용/tmp로 설정한다. JavaExec workingDir만 바꾸면 Loom이 다시 덮어쓰므로 정상 개발 run/world를 사용하지 않도록 주의한다.

setup은 이 전용 월드의 forceload 티켓을 비우고 기본 x7000/7300/7600/7900,y65,z1000에 네 방향 템플릿을 배치한다. **빈 전용 월드에서만 사용**한다. 이미 등록된 원점은 초기화하지 않는다. 새 테스트가 필요하면 `PROBE_ORIGIN_X`를 바꾸고 restore에도 동일 값을 지정한다. 실제 위치는 템플릿(0,0,0) 기준이며 구조물 중심/회전 후 최소 모서리와 다르다.

`/mmskytower register x y z rotation`은 현재 차원에서 이미 배치된 탑의24앵커가 로드되어 있어야 성공한다. `/mmskytower status`는 격리 상태·인스턴스·완료비트를 표시한다. 완료비트 강제 명령은 없다. 테스트의 setblock cooldown은 합성 vanilla 상태이며 플레이어 전투 성공을 뜻하지 않는다. 빈 서버 일시정지 시 tick sprint로 실제 tick을 진행한다.

생산 자연 생성 structure_set/빈도/기존 탑 섬 대체는 연결하지 않았다. 단독 리소스의 수동 배치는 `/place template moreminecrafts:sky_tower/full_01 …`로 가능하다.

최종 setup은 첫 탑의16개 스포너에 합성 cooldown을 주입해 누적 완료비트15→255→4095→65535를 확인했다. 다른 새3개 탑은0을 유지한다. chunk_cycle은 실제 execute if loaded 실패/성공으로 언로드·재로드를 확인한다. restore는12개 저장 인스턴스(이전8개 포함)의 전체 status를 비교했다. 새 월드의 개수는4개가 정상이다.

## 배포 패키지와 회귀

```sh
./gradlew build
```

기존 빌드의 server GameTest 회귀도 실행된다. 산출 JAR은 `build/libs/moreminecrafts-0.1.0-alpha.1.jar`. 구조물 파일은 이 JAR 안의 `data/moreminecrafts/structure/sky_tower/full_01.nbt`이며 해시 대조 결과는 package_verification.json에 기록했다. 다른 머신의 재빌드 JAR 전체 해시는 ZIP 메타데이터 등의 영향으로 달라질 수 있으므로 건축 동결 판정에는 내부 NBT의 해시와 바이트 동일성을 사용한다. JAR을 설치해도 승인 전 자연 생성이나 물리 결계가 자동 활성화되지 않는다.
