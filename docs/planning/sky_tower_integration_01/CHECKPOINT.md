# INTEGRATION-01 진행 체크포인트 — 부분 구현

기준16ad9d5, 신규 feature/sky-tower-integration-01. 건축 브랜치는 최신 원격main cde842d를 이미 포함. 승인 NBT/디자인·P1–P6 불변.

완료: Phase A 기존 jigsaw/퀘스트/콜로세움/vanilla SavedData 조사, 실제16스포너/8금고 앵커 확인. Phase B 승인 NBT 바이트 동일 리소스/개발용 풀, 안정 ID·회전/겹침 방지·SavedData. Phase C vanilla cooldown 관찰과 영구 공유 완료비트. 물리 결계/보상 변경/자연 생성 미활성화. DECISION_REQUIRED.md의 사용자 응답 대기.

최종 전체 단위582건 PASS, 신규11건 포함. 손상 저장 필드가 partial decode로 빠져나가는 리뷰 결함을 테스트로 재현(RED)한 후 입력 보존/quarantine으로 수정(GREEN). 이전 실제 모드 서버에서4방향 배치·등록은 성공했고, synthetic cooldown이 빈 서버 정지로 처음0인 것을 tick sprint 후15로 확인했다. 이는 실제 전투 검증 아님. 환경 재시작으로 서버가 종료되어 최종 코드의 live setup/save/restart/restore를 다시 진행해야 한다.

재개: REPRODUCE.md와 integration_server.init.gradle 사용. /tmp/sky-tower-integration-server의 독립 테스트 월드/포트25595·RCON25596. pause-when-empty-seconds=0 권장. live_probe.py setup은 PROBE_ORIGIN_X 기본4000의 새4개 위치를 사용하므로 이미 등록된 경우 다른 빈 원점을 지정한다. setup→save/stop→서버 재시작→restore. 비밀 RCON 암호는 저장소에 넣지 않는다.

미완료: 자연 생성/지면 접속/자동 인스턴스 등록, 물리 결계, 보상테이블/옥상특별보상/성소전체완료정책, 실제 전투 및 클라이언트·다중플레이·파일쓰기실패 시험. INTEGRATION COMPLETE 아님. 기존 원장과 건축본을 바꾸지 않고, 확정되지 않은 정책을 기본값으로 채우지 않는다.
