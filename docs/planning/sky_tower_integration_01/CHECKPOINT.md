# INTEGRATION-01 체크포인트 — 부분 구현

시작/건축 기준16ad9d5. 브랜치 feature/sky-tower-integration-01. 구현 커밋357ec45 원격 푸시 완료. 후속 증거 커밋은 이 파일을 포함하는 `git log -1`로 확인한다. 원격 재확인 결과 건축16ad9d5·main cde842d·통합357ec45로 예상과 일치했다. main/건축/Q01–Q118/P1–P6 수정 없음.

## 완료 범위

- Phase A: 기존 jigsaw/퀘스트/바닐라 금고/콜로세움/SavedData 조사. 실제16스포너·8금고 앵커와72공기칸 결계 제안 좌표 추출. 사용자가 외관 최종 승인한 사실을 새 보고서에 기록.
- Phase B 부분: 승인 NBT 바이트 동일 설치·개발 template pool·수동 명령 배치/등록. 차원+원점+회전 ID, 겹침 거부, 재등록 시 보존, SavedData 구현. 자연 생성/자동 등록 미연결.
- Phase C 부분: 바닐라 실제 COOLDOWN 관찰, 영구 공유16완료비트·층당4quorum. 물리 결계/보상 변경/성소 완료 이벤트 미연결.
- Phase D 부분: unit583건 PASS(신규12). 독립 리뷰 결함 partial decode를 RED→quarantine/입력보존 GREEN으로 수정. 추가 원점 overflow도 RED 재현 후 Site 경계 검증으로 수정·독립 리뷰 완료. 실제 Fabric26.3 모드 서버4회전 배치/등록 PASS. 합성 cooldown으로15→255→4095→65535, 다른 새3개 탑0, 중복/재등록 보존 PASS. 실제 청크 언로드/로드 PASS. 정상 서버 재시작 후12개 인스턴스 전체 status 일치 PASS. 실제 전투/클라이언트 시험 아님.

## 재개

```sh
git switch feature/sky-tower-integration-01
git log -2 --oneline
python3 -I docs/planning/sky_tower_integration_01/verify_template.py
# Java25 환경에서
./gradlew test
```

서버 명령·환경 옵션은 REPRODUCE.md. 전용 `/tmp/sky-tower-integration-server`는 시험 후 정상 종료했다. setup 기본 원점7000은 이미 사용했으므로 이 월드에서 재시험할 때 `PROBE_ORIGIN_X`를 새 빈 원점으로 지정하고 restore에도 동일 값 사용. RCON 비밀은 저장소에 넣지 않았다. `/tmp` 소실 시 전용 월드를 새로 준비한다. 일반 개발 월드를 probe에 사용하지 않는다.

## 보류와 한계

DECISION_REQUIRED.md의 사용자 응답 대기: 자연 생성 정책/구형 섬 교체,8금고 기능 테이블 변경,72칸 런타임 결계,파괴된 스포너 복구정책,옥상특별보상/전체완료정책. 기본값을 만들지 않는다. 승인된 범위에서만 이어 구현한다.

남은 실제 검증: 신규 청크 자연 생성·지면접속·회전/간섭·자동 인스턴스 등록, 물리 결계와4층 실제 전투 진행, 보상/다중플레이/재접속, 클라이언트 상호작용. gzip 자체 손상·디스크쓰기실패·전원차단 원자성은 검증하지 않았다. **INTEGRATION COMPLETE 아님.**

최종 `test assemble` 및 전체 `build` 성공, 기존 서버 GameTest527 required PASS. 추가 좌표 guard를 포함한 최종 코드도 실제 서버에서12개 저장 인스턴스 복원을 재확인 후 정상 종료했다. live_restore.json/live_final_server_excerpt.txt 참조.
