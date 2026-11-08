# Phase C-2 시험 보고

## 좌표 및 모델

- `phase_c2_prototypes.py`: 432개 좌표 시제품 생성. 탑 비공기뿐 아니라 NBT가 명시한 모든 블록을 보존해 가정된 배치 순서에 따라 재생한다. 템플릿의 structure_void는 덮어쓰지 않는 것으로 처리하며, 실제 processor와 jigsaw 생성 경로에 동일하게 적용되는지 확인하지 않았다. 같은 블록 ID라도 다른 상태·NBT는 변경 개수에서 누락될 수 있다.
- `phase_c2_verify.py`: 생성기와 다른 네 개의 직접 회전 수식과 역변환, 입구 하부 slab, 부모→탑 덮어쓰기 및 부속물 순차 재생, 다리 블록 좌표를 432행에서 독립 대조했다. **PASS는 이 좌표 재생에 한정**한다. 독립 검사도 같은 원본 NBT와 제한된 고체 모델에 의존한다. `phase_c2_verification.json`에 머리 공간의 비공기 블록과 다리 끝 다음 바닥 표본을 기록했다. Minecraft 충돌 상자 및 보행은 검사하지 않는다.
- 상대 회전 좌표 4종을 재생했으나 실제 서버 실험은 상대 회전 0만 했다. 네 부모 월드 회전, 경계 청크 생성·저장/재로드 및 실제 지그소/processor 순서는 **NOT RUN**.

## 전용 Minecraft 서버

Minecraft 26.3/Fabric 개발 서버, 새 전용 월드 `/tmp/sky-tower-phase-c2-server-20261109`; 기존 자연 생성 설정 및 플레이 월드를 수정하지 않았다. 실제 `/place template` 명령으로 섬 부모와 승인 탑을 수동 배치하고 시제품 slab·벽·받침을 `setblock`했다. 결과와 모든 명령 응답은 [phase_c2_server_results.json](phase_c2_server_results.json)에 있다. 각각의 섬 로컬 원점은 (14000,80,1000), (14200,80,1000), …, (15000,80,1000)이며 탑 템플릿 원점은 각 섬 원점+(-15,17,-3)이다.

여섯 섬에서 템플릿 로드 응답과 모든 지정 부속 블록 setblock 응답을 받았고, 별도의 스크립트로 각 setblock 명령 좌표가 최종 시제품 데이터에 존재하는지 대조했다. 입구 slab과 다리 slab 각각 한 칸의 서버 블록 검사를 통과했다. 이는 **표본 확인만 PASS**다. 탑 전체 블록의 동등성, 주변 자식 콘텐츠, 서버에서의 시각적 지지 판정, 입구 전체 및 첫 전투층 연결, 네 회전, 청크 언로드/재진입은 **NOT RUN**이다. 강제 청크 로드 상태에서 수동 배치했으므로 자연 생성 성공의 증거가 아니다.

실제 플레이어 보행, 클라이언트 시점·스크린샷, 멀티플레이는 **NOT RUN**. 연결대의 좌표상 slab 연속성이나 텔레포트를 보행 성공으로 취급하지 않는다. 그림은 서버 스크린샷이 아니라 좌표를 투영한 이미지다.

서버 재현에는 별도의 빈 전용 서버 디렉터리, eula 및 로컬 전용 RCON 설정, Java 25/Fabric 개발 서버가 필요하다. `phase_c2_server_probe.py`는 지정된 `/tmp/sky-tower-phase-c2-server-20261109`에만 연결되며 RCON 자격값은 해당 디렉터리의 `server.properties`에서 읽고 결과 파일에 저장하지 않는다. 시험은 원점 x=14000부터 서로 다른 빈 구역을 사용하고 종료 시 저장·서버 정지한다. 이미 사용한 구역은 `PHASE_C2_ORIGIN_X`로 다른 빈 위치를 지정한다. 일반 서버와 플레이 월드에서 실행하지 않는다.

## 콜로세움 GameTest 회귀

`moreminecrafts-gametest:colosseum_game_tests_absence_grace_resumes_on_return_and_expires_otherwise`:

- 기준 이전 기록: `baseline_regression.txt`는 attempt가 null인 실패, `serial_final_build.txt`는 `return resumes the same attempt` 실패. 92f862f 보고의 전체 531건 재실행도 530/531로 `grace running on tick 49` 실패였다. 상황·실패 메시지가 다르므로 단일 원인을 단정하지 않는다.
- 별도 detached 기준 `b7e306ab199b80317022e46c253806e099f8b049`에서 새 테스트 월드와 단독 필터로 실행: **1/1 PASS**, Gradle BUILD SUCCESSFUL. 이번 서버 출력 `/tmp/sky-tower-phase-c2-baseline.log`.
- `92f862f` 현재 브랜치에서 별도 새 테스트 월드와 동일 필터로 실행: **1/1 PASS**, Gradle BUILD SUCCESSFUL. 출력 `/tmp/sky-tower-phase-c2-isolated.log`.
- `git diff b7e306a 92f862f -- src build.gradle gradle.properties`는 비어 있다. 이번 Phase C-2는 문서·검사 스크립트 외의 코드와 리소스를 변경하지 않았다. 이 사실은 원인과 무관함을 증명하지는 않는다. 테스트는 1,500ms의 실시간 유예와 tick 40/50/60/75의 관측을 함께 사용한다. 전체 배치 부하·월드 상태·순서 의존성은 확인되지 않아 **UNRESOLVED BASELINE FAILURE**로 유지한다. 콜로세움 코드를 수정하지 않았다.
- Phase C-2에서 전체 531건을 다시 실행하지 않았다. 588개 단위 테스트는 92f862f의 기록에서 성공한 결과를 재사용하며 이번에는 **NOT RUN**이다. 자연 생성과 결계·스포너 코드를 변경하지 않았다는 사실과 검증 여부를 구분한다.
