# SKY-TOWER-INTEGRATION-01 — 부분 구현

**INTEGRATION COMPLETE 아님.** 승인된 건축을 그대로 모드 리소스에 설치하고 개발용 배치·인스턴스 저장·바닐라 시련 완료 관찰을 구현했다. 자연 생성 정책, 기능 금고 설정, 물리 결계 및 옥상 보상/완료 정책은 미확정이라 연결하지 않았다. 사용자의 FULL BUILD 외관 최종 승인은 존중하며 건축 디자인을 변경하지 않았다.

## 기준과 변경 범위

- 기준 브랜치 feature/sky-tower-full-build-01, 승인/시작 HEAD `16ad9d59b4f518b38c981b22c4517498dc1aad2d`.
- 시작 원격 main `cde842dd2cd3d472515aaecb80ad7e78f5da5ce3`; 건축 브랜치가 main을 포함해 이 기준에서 신규 feature/sky-tower-integration-01을 생성했다. 다른 브랜치 수정·병합·PR·강제 푸시 없음.
- 구현 커밋 `357ec45` 원격 푸시 완료. 후속 검증 커밋은 이 파일을 포함하는 Git 이력에서 확인한다. 체크포인트에 재개 절차를 남긴다.
- 승인 NBT를 `data/moreminecrafts/structure/sky_tower/full_01.nbt`로 바이트 복사했다. 건축 폴더·Q01–Q118·P1–P6 원본 변경 없음.
- Java 변경은 신규 skytower 패키지5클래스, trial tick 관찰 mixin1개, 초기화1줄 및 mixin 등록1개. 신규 JUnit3파일, 비활성 개발용 template pool1개, 본 문서 폴더가 나머지 변경이다.

## 구현한 연결

`/place template moreminecrafts:sky_tower/full_01 …` → operator `/mmskytower register x y z rotation` → 실제16스포너/8금고 앵커 확인 → 안정적인 차원·템플릿 원점·회전 ID → vanilla Overworld SavedData → TrialSpawner.tickServer의 실제 COOLDOWN 관찰 → 해당 인스턴스의16비트 영구 완료 기록 → 층당4개 완료 사실.

같은 탑의 재등록·같은 스포너 신호는 진행을 초기화하거나 중복 처리하지 않는다. 다른 탑·다른 앵커는 분리한다. 겹치는 등록은 거부한다. 저장 스키마가 손상되면 임의의 새 게임으로 시작하지 않고 원입력을 보존한 quarantine으로 진행 변경을 중단한다. SavedData/Codec은 프로젝트의 기존 저장 패턴을 따른다.

바닐라 TrialSpawner의 적 생성·플레이어별 열쇠 지급·VaultServerData의 사용/보상 기록을 그대로 사용한다. `QuestVaultRewards`의 기존 관찰은 그대로 적용된다. ST05 콜로세움의 자체 웨이브/시간 엔진은 천공탑용 범용 API가 아니므로 복제하거나 억지로 연결하지 않았다. 플레이어 진입 퀘스트는 실제 generated StructureStart에 의존하므로 명령 배치를 자연 생성 진입으로 오인하지 않는다.

## 미연결 범위

| 영역 | 현재 상태 |
|---|---|
| 월드젠 | 독립 NBT/template pool 로드·개발 배치 가능. 자연 생성 미활성화. 기존 하늘섬 NBT에는 구형 탑이 합성되어 있어 단순 파일 교체는 안전하지 않음 |
| 전투층 | 4×4 바닐라 스포너 완료 관찰·논리 quorum·공유 영구 저장 구현. 실제 플레이어 전투 진행은 미검증 |
| 결계 | 논리 완료 사실만 구현. 초기 물리 차단·해제는 미구현. 72공기칸의 최소 런타임 barrier 제안은 승인 대기 |
| 보상 | 승인본의 vanilla 금고 설정 보존. 현재 하늘섬 전용 테이블 연결은 기능 NBT 변경 승인을 기다림. Java 추가 지급 없음 |
| 성소 | 건축/앵커 보존. 도달과 전체 완료를 동일시하지 않으며 별도 유물 능력·새 보상을 만들지 않음 |
| 재접속/다중플레이 | 구조물 단위 공유 완료 데이터는 플레이어 세션과 독립. 실제 플레이어 재접속/동시 참여 시험은 미실행 |

## 검증과 남은 결정

전체 단위583건 PASS(신규12건 포함), 승인 NBT 동결 PASS. 실제26.3 모드 서버에서 네 방향 배치·등록, 합성 COOLDOWN을 통한4층 완료비트 누적, 다른 탑과 분리, 중복 처리 방지, 청크 재로드를 확인했다. 동일 월드의 정상 서버 재시작 후12개 인스턴스 전체 상태가 종료 전과 일치했다. 기존 서버 GameTest527건과 전체 build/최종 test assemble도 PASS. 배포 JAR은 build/libs/moreminecrafts-0.1.0-alpha.1.jar이며 TEST_REPORT.md 및 해당 로그에 근거를 기록했다. 실제 전투/결계/자연 생성의 전체 진행은 LIVE_SERVER_UNVERIFIED, 클라이언트는 LIVE_CLIENT_UNVERIFIED다.

[DECISION_REQUIRED.md](DECISION_REQUIRED.md)에 생성 정책, 기능 금고 설정, 물리 결계 최소안, 파괴된 필수 스포너의 처리, 옥상 특별보상/전체 완료를 분리했다. 사용자 응답 전 임의 기본값을 넣지 않았다. 확정 뒤 기존 섬 접속 및 자연 생성 자동 등록, 승인된 결계/보상 연결, 실제4층 전투·재접속/재시작·다중플레이·클라이언트 검증을 이어가야 한다.

전체 변경 파일은 CHANGED_FILES.txt에 기록했다. 상세 설계는 ARCHITECTURE.md, 실제 좌표는 ANCHOR_MAP.md/anchor_map.json, 검사는 TEST_REPORT.md, 명령은 REPRODUCE.md를 참조한다.
