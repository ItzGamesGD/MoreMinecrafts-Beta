# Sky Tower Full Build 01

Minecraft Java **26.3**, DataVersion **5023**, datapack **121.0**. 수동 배치용 완성형 건축 후보이며 월드젠 등록이나 Java 게임 시스템 변경은 포함하지 않는다. 시각적 디자인 승인은 사용자 검토 대기다.

## 설치 및 배치

`sky_tower_full_01_datapack.zip`을 테스트 월드의 `datapacks/`에 넣고 `/reload`한다. 기존 건축물이 없는 충분한 빈 공간에서 지면 위에 서서:

```mcfunction
/function sky_tower:place_here
```

또는 최소 모서리 좌표를 직접 정한다:

```mcfunction
/place template sky_tower:full_01 X Y Z none none 1.0 0 strict
```

NBT 크기는 **50×80×49**. 최소 모서리 기준 중심축은 `(24.5,0,24.5)`이고, 입구는 +X 방향이다. 바닥 y0은 기존 지면 **상면**에 맞춘다. 동쪽 진입부의 반블록은 지면→반블록→기단으로 연결한다. 시험 서버에서는 template Y65와 지면 블록 y64를 사용했다. 공중에 배치할 경우 입구 지면/섬을 별도로 연결해야 한다. NBT에는 공기가 생략되어 있으므로 기존 구조 위에 반복 배치하면 이전 블록이 남는다. 재검증은 빈 구역에서 한다.

## 재생성 및 정적 검사

Python3 표준 라이브러리:

```sh
python3 -I docs/planning/sky_tower_full_build_01/build.py
python3 -I docs/planning/sky_tower_full_build_01/verify.py
python3 -I docs/planning/sky_tower_full_build_01/safety.py
python3 -I docs/planning/sky_tower_full_build_01/connectivity.py
python3 -I docs/planning/sky_tower_full_build_01/width.py
python3 -I docs/planning/sky_tower_full_build_01/passing.py
python3 -I docs/planning/sky_tower_full_build_01/test_regressions.py
python3 -I docs/planning/sky_tower_full_build_01/package.py
```

`verify.json`에 37개 목표의 점프 없는 AABB 경로, `safety.json`에 면 보호, `connectivity.json`에 석재 연결, `width.json`에 표본 단면 폭, `passing.json`에 분리 차선 결과가 기록된다. 경로는 `paths.json`, 전체 점유 좌표는 `blocks.csv`, 제단/발코니 앵커는 `model.json`이다. 검증기 종료코드0만으로 완성/합격을 판단하지 않는다.

## 실제 시험 재현

공식 배포본을 `/tmp/fullbuild-mc`에 준비하려면 `fetch_client.py`를 실행한다. 공식26.3 서버·클라이언트·라이브러리·에셋을 SHA1 대조해 받는다. Java25, Xvfb/X11, xdotool, Mesa lavapipe Vulkan 드라이버, Pillow(화면 캡처)가 필요하다. 테스트에서는 Xvfb :88 1280×720, llvmpipe LLVM19 소프트웨어 렌더러를 사용했다.

격리 서버는 localhost25585/RCON25586, flat, online-mode=false, whitelist=false로 설정했다. 비밀 RCON 암호는 임시 `server.properties`에만 둔다. 운영 서버에서 이 테스트 설정을 사용하지 않는다. Mojang 서버 약관을 확인한 후 전용 테스트 월드를 생성한다.

1. `server_prepare.py /tmp/fullbuild-mc` — 임시 데이터팩/검사 함수 생성.
2. 공식 server.jar를 Java25로 시작. 실행 중 준비한 경우 `reload` 후 로드 완료를 기다린다.
3. `server_check.py /tmp/fullbuild-mc` — 테스트 구역을 비우고 strict 배치, 전체196,000좌표 대조. **지정된 테스트 좌표만 사용하며 기존 건축물이 있는 서버에서 실행하지 않는다.**
4. `launch_client.py` — 실제 클라이언트를 :88에서 실행, FullBuilder로 localhost에 접속. Java 경로 등 환경별 경로는 스크립트 상단을 확인한다. 게임이 키보드 포커스를 가지고 autoJump=false인지 확인한다.
5. `live_walk.py` — 기단에서 F1–F4 중앙, 성소 중앙까지 실제 W키 보행.
6. `live_balconies.py` — 실제 발코니 진입·복귀·난간 밀기8곳.
7. HUD를 F1로 숨긴 후 `capture.py` — 실제 클라이언트 프레임10장.

실제 보행 도중 `capture.py`나 다른 tp 명령을 실행하면 시험이 무효가 된다. 정적 경로와 실제 보행, 폭 측정, 두 플레이어의 교행은 별개 결과다.

## 런타임 범위

시련 스포너16개는 바닐라 좀비 normal/ominous configuration을 사용한다. 금고8개는 바닐라 trial_key와 trial_chambers/reward loot table을 사용한다. 층 완료 감지·영구 결계 해제·모드의 특별 보상은 이 패키지에서 구현하지 않는다. 관련 Java/월드젠은 변경하지 않았고, 각 층 관문과 앵커는 후속 런타임 연결에 사용할 수 있다. 완성형 건축 후보 제출과 모드 통합/디자인 승인은 구분한다.
