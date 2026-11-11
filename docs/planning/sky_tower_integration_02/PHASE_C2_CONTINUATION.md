# Phase C-2 후속 실험: 회전·청크 및 측면 접속

## 기준과 판정

시작 브랜치 `feature/sky-tower-integration-02`, 로컬·원격 HEAD `f286ad79df30f105538cd6af77bd7abd579ea737`, 작업 트리 clean. 사용자가 지정한 92f862f 체크포인트의 후손이다. 이전 C-2의 432개 후보와 별도 서버 시험을 보존하고 미검증 항목만 이어서 실험했다. 판정은 여전히 **PARTIAL / UNVERIFIED**이며, 승인 가능한 최종 배치는 없다. 자연 생성·탑 NBT·Phase B는 변경하지 않았다.

## 여섯 섬의 네 월드 회전

서버에서 섬 부모와 탑을 함께 `none`, `clockwise_90`, `180`, `counterclockwise_90`으로 돌리고, 추가 블록의 좌표도 같은 회전으로 변환했다. 기존의 B형 대표 표본(dx=-12,dz=0,y17,폭3)의 **자식 상대 회전은 0으로 고정**했다. 여섯 섬 × 네 회전 = 24개 수동 배치에서 섬 jigsaw, 탑 입구, 다리, 난간, 지지대, 스포너, 금고 각 1개를 검사했다. 모두 168/168 표본 검사 PASS다.

meadow 네 회전은 저장 후 강제 로드를 해제하고 tick sprint 1200을 실행했다. `execute if loaded`가 네 경우 모두 실패해 해당 입구 청크가 언로드된 사실을 확인했다. 다시 강제 로드 후 입구·지지대·스포너 각 1개를 검사해 12/12 PASS다. 총 180/180 표본 검사다. [명령과 응답](phase_c2_rotation_reload_results.json)에는 모든 좌표와 결과가 있다. 원본 템플릿과 지지 블록의 전체 내용을 비교한 것은 아니며, 자식의 다른 상대 회전, 모든 섬의 재로드, 이웃 청크·실제 지그소 월드젠 및 충돌 상자는 **UNVERIFIED**다. 기존 결계의 런타임 등록 및 진행을 검증한 실험도 아니다.

## 다리 끝과 측면 접속

기존 대표 다리의 끝 바로 바깥 열 (39,z20..22)의 부모 섬 블록을 다시 조사했다. 제한된 전체 블록 ID를 가정한 지형 상단은 다리 slab의 y17.5보다 snowy -2.5(2열), meadow -0.5/-0.5/-1.5, forest -1.5(3열), badlands -2.5(3열), desert 미발견/-2.5/-1.5, rocky -2.5(3열)다. 미발견은 y0..17에서 목록에 있는 바닥을 찾지 못했다는 뜻이다. 이는 한쪽 접근 방향의 정적 자료이며 실제 보행 실패나 다른 경로의 불가능성은 증명하지 않는다. [열별 자료](phase_c2_landing_audit.json).

각 섬의 동일 배치에 대해 다리 측면 난간 2개를 생략하여 기존 섬 지면으로 이어지는 별도의 실험 후보를 만들었다. 승인된 탑과 기존 섬을 바꾸는 명령은 후보에 추가하지 않았다. 추가 부속물은 3칸 폭 slab 바닥과 남은 난간, 기존의 실험 지지대다. 다음은 **단일 경로를 제안한 좌표**이며 안전한 전체 통행로 판정이 아니다.

| 섬 | 섬쪽 시작 바닥 | 난간 개방 좌표(기존 후보의 난간을 설치하지 않음) | 제안 경로 머리 높이의 비공기 |
|---|---|---|---|
| snowy | (35,16,23) | (35,18,23), (36,18,23) | 없음 |
| meadow | (36,16,19) | (36,18,19), (37,18,19) | tall_grass (34,18,21) |
| forest | (36,16,19) | (36,18,19), (37,18,19) | oak_leaves (34,19,21) |
| badlands | (36,16,19) | (35,18,19), (36,18,19) | 없음 |
| desert | (36,16,23) | (36,18,23), (37,18,23) | 없음 |
| rocky | (36,16,19) | (35,18,19), (36,18,19) | 없음 |

좌표는 부모 섬 로컬이다. 지면 상단 y17에서 slab 상단 y17.5로 이어지는 제안이며, 각 경로에서 연속한 열은 상하좌우 인접하고 바닥 높이 차는 0.5 이하이다. 눈·풀·나뭇잎 및 난간의 Minecraft 충돌 형태와 플레이어 크기는 이 좌표 검사만으로 알 수 없다. 특히 meadow의 tall_grass는 비공기라는 사실만으로 통행 방해라고 단정하지 않고, forest의 나뭇잎은 충돌 검증이 필요하다. 난간 개방부의 추락 위험·보행 폭, 섬 내부 기존 동선과 출발점의 연결, 입구→첫 전투층, 기단 전체의 지지·시각적 품질은 **UNVERIFIED**다.

측면 시작점에서 부모의 원래 y16 블록 ID가 그대로 남아 있고 y17·18이 모두 air인 평탄 셀의 상하좌우 연결 성분을 별도로 탐색했다. snowy 40, meadow 178, forest 184, badlands 197, desert 91, rocky 204셀이다. [전체 셀 좌표와 제한](phase_c2_island_component.json). 이는 원본 부모와 탑·부속물을 가정대로 재생한 정적 ID 모델에서만 확인한 결과이며 높이 변화, 다른 지그소 자식, 블록 상태, 게임 내 충돌 및 식별된 기존 플레이 경로는 포함하지 않는다. 면적만으로 실제 접근 가능성을 통과시키지 않는다.

여섯 측면 시제품을 별도의 전용 서버 위치에 다시 수동 배치하고, 제안 경로 전체에서 바닥 ID와 두 머리 높이 블록 ID를 검사했다. 정적 모델과 서버 ID가 모두 일치했다. 별도 경로 검사기가 원본 NBT와 출력 좌표·서버 응답을 재생하고 맨해튼 인접성·높이 차이를 확인했다. 이는 실제 플레이어 이동 또는 개별 블록의 충돌 형태 검사 PASS가 아니다. [시제품 좌표](phase_c2_side_access.json) · [독립 검증](phase_c2_side_verification.json) · [서버 명령/응답](phase_c2_side_server_results.json) · [좌표 평면도](phase_c2_side_access.png). 평면도는 Minecraft 스크린샷이 아니다.

## 실제 클라이언트와 다음 결정

관리 환경의 원격 데스크톱은 enabled이나 상태 unknown이었고, 이번 도구 목록에서 데스크톱 조작 도구를 사용할 수 없었다. 셸의 DISPLAY/WAYLAND_DISPLAY와 Xvfb도 확인되지 않았다. 실제 클라이언트 보행과 스크린샷은 **NOT RUN**이다. 단일 경로가 서버의 공기 블록을 지난다는 점을 실제 보행으로 취급하지 않는다. 사용자에게 대형 구조물이나 광범위한 지형 변경을 승인받을 필요성이 입증되지 않았으므로 그런 변경을 요청하거나 적용하지 않는다. C-3 자연 생성 연결은 실제 보행·기단 지지·주요 콘텐츠·실제 생성 순서 검증이 완료될 때까지 진행할 근거가 부족하다.

## 재현과 시험 월드 보호

```sh
python3 -I docs/planning/sky_tower_integration_02/phase_c2_landing_audit.py
python3 -I docs/planning/sky_tower_integration_02/phase_c2_side_access.py
python3 -I docs/planning/sky_tower_integration_02/phase_c2_side_verify.py
python3 -I docs/planning/sky_tower_integration_02/phase_c2_island_component.py
MPLCONFIGDIR=/tmp/sky-tower-mpl XDG_CACHE_HOME=/tmp/sky-tower-cache python3 -I docs/planning/sky_tower_integration_02/phase_c2_side_image.py
```

서버 탐침은 `phase_c2_rotation_reload_probe.py`와 `phase_c2_side_server_probe.py`다. 로컬 RCON을 갖춘 전용 시험 서버와 `PHASE_C2_SERVER_DIR` 외에 각각 `PHASE_C2_ROTATION_ORIGIN_X`, `PHASE_C2_SIDE_ORIGIN_X`에 **매번 새로운 빈 구역**을 명시해야 한다. 기존 플레이 월드에서 실행하지 않는다. 서버 탐침은 수동으로 블록을 변경하며 종료 시 저장하고 서버를 정지한다.

이 후속 실험에서 전체 GameTest와 588 단위 테스트는 재실행하지 않았다. 이전 보고의 530/531 전체 결과와 b7e306a/92f862f 단독 1/1 결과를 보존하며, 콜로세움 원인은 **UNRESOLVED BASELINE FAILURE**다.
