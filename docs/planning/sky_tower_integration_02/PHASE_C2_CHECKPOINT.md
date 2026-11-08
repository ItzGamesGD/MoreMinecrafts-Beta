# Phase C-2 재개 체크포인트

시작점 92f862f, 동일 브랜치. 이 체크포인트는 Phase C 문서를 수정하지 않으며, 후보·검증 도구와 비교 자료만 추가한다. 현재 [PHASE_C2_REPORT.md](PHASE_C2_REPORT.md)의 판정은 PARTIAL / UNVERIFIED다.

재개 순서:

1. [후보](PHASE_C2_CANDIDATES.md)와 [지지·접근](PHASE_C2_SUPPORT_AND_ACCESS.md)의 개별 문제를 확인하고 기존 경로와 섬의 자식 콘텐츠를 실제 블록 및 상태로 식별한다.
2. 섬쪽 다리 종점, 오르내리는 단차 및 난간을 실제 충돌 상자와 실측 동선에 맞춰 수정한 별도의 시제품을 탐색한다. 탑 본체는 수정하지 않는다.
3. 시제품의 기단 지지 형상, 주요 콘텐츠, 실제 네 부모 회전과 청크 경계·저장/재로드를 독립적으로 검증한다. 자연 생성의 지그소 순서·블록 상태/엔티티·processor를 실측한다.
4. 전용 서버에서 전 구간 및 섬별 후속 실험을 하고 실제 클라이언트로 섬 경로→연결대→입구→첫 전투층을 걷는다. 스크린샷과 시야를 검토한다.
5. 기존 지형을 상당히 변경하거나 대형 구조물을 필요로 한다는 구체적 후보가 생기면 규모와 이미지를 갖춰 사용자에게 결정을 요청한다. 지금은 그런 변경이 불가피하다고 증명되지 않았다.
6. 기존 전체 GameTest의 콜로세움 실패는 [시험 보고](PHASE_C2_TEST_REPORT.md)의 독립 실행과 분리한다. 원인은 UNRESOLVED BASELINE FAILURE로 유지한다.

좌표 재현:

```sh
python3 -I docs/planning/sky_tower_integration_02/phase_c2_prototypes.py
python3 -I docs/planning/sky_tower_integration_02/phase_c2_verify.py
MPLCONFIGDIR=/tmp/sky-tower-mpl XDG_CACHE_HOME=/tmp/sky-tower-cache python3 -I docs/planning/sky_tower_integration_02/phase_c2_images.py
```

서버 탐침은 `phase_c2_server_probe.py`, [결과](phase_c2_server_results.json)는 별도 전용 서버의 수동 `/place template`, 개별 setblock 및 표본 확인 기록이다. 임의의 기존 서버/월드에서 실행하지 않는다. 자동 월드젠, 전체 블록 동등성, 실제 게임 보행 테스트가 아니다.
