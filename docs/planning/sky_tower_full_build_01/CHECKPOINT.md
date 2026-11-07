# FULL-BUILD-01 제출 체크포인트

브랜치 feature/sky-tower-full-build-01. 기준 c43b480, 중간 커밋81c3abb. 이 파일을 포함한 최종 제출 커밋은 `git log -1 --oneline`으로 확인한다. Phase A–E 건축 후보 제작·수정·패키징 완료. FULL BUILD COMPLETE(건축 후보), DESIGN APPROVED는 사용자 시각 검토 대기.

최종 NBT: sky_tower_full_01.nbt, 50×80×49, 32,335블록, SHA256 cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27. 수동 배치 datapack ZIP 생성. 기단·전투층4·외부5상승구간·발코니8·스포너16·금고8·관문/차양/문양·성소12아치/부유 유물 포함.

최종 서버 strict 배치196,000좌표 불일치0. 실제 클라이언트 기단→F1–F4→성소 중앙163.29초 무점프 도달, 실제8발코니 진입·복귀·난간 밀기 높이 유지. 실제10스크린샷. 마지막 발코니 바닥5칸 복구 후 주동선1292표본을 최종 몸통 점유에 재생해 충돌0; 수정된 발코니는 최종 실제 재시험. 대표 스포너 ACTIVE/몹 생성, 금고 실제 키 소비/보상 배출 smoke test PASS.

정적37목표 도달, 보행면0, 의도하지 않은 분리 석재0, 발코니168칸 평탄, 중앙8접속부 충돌0, 10개 분리차선 PASS, 회귀13항목 PASS, 동일 NBT 재생성. 최종 종합15항목 final_checks.json 모두 PASS. 스크린샷/치수 변경/판정은 FULL_BUILD_01_REPORT.md·DESIGN_CHANGES.md 참조.

남은 범위: 사용자 시각 승인, 모드 런타임의 층 완료/영구 결계 로직, 월드젠 연결, 전체 전투 반복·ominous·재시작·두 클라이언트 동시 교행/전투는 별도 작업. 이 작업에서 자동 진행하지 않는다. 기단/성소 접속부 폭 표본 최소3.814/3.929로 원목표4를 소폭 밑돈다. 일반 D45 대신D51. 지면 상면에 template y0을 맞춰 진입 지면을 연결할 것.

재현: README.md의 build.py → verify.py → safety.py → connectivity.py → width.py → passing.py → test_regressions.py → package.py. 실제환경은 fetch_client.py·server_prepare.py·server_check.py·launch_client.py·live_walk.py·live_balconies.py·capture.py. /tmp/fullbuild-mc는 임시 환경이며 영구 보존 보장 없음. 기존 P1–P6/원장/Java/월드젠 불변, PR/병합 없음.
