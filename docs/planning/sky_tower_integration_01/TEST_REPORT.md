# INTEGRATION-01 검증 결과

판정: **부분 구현 검증 PASS / 전체 콘텐츠 LIVE_SERVER_UNVERIFIED / LIVE_CLIENT_UNVERIFIED**. 자연 생성·물리 결계·신규 보상 연결은 정책 미확정으로 NOT RUN이며, 전체 진행 PASS가 아니다.

| 검사 | 판정 | 근거와 범위 |
|---|---|---|
| 승인 건축 동결 | PASS | `verify_template.py`, `anchor_map.json`: git16ad9d5 원본·건축 폴더·설치 리소스 바이트 동일. SHA256 cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27. DataVersion5023, size50×80×49 |
| 패키징 | PASS | 최종 `test assemble` 및 전체 `build` 성공. `package_verification.json`의 JAR 내부 NBT 바이트 동일성 확인 |
| 기존 서버 GameTest 회귀 | PASS (527건) | `gradle_build.txt`: All 527 required tests passed. 전체 build도 BUILD SUCCESSFUL로 종료 |
| 실제 기능 앵커 | PASS | NBT에서16 trial spawner·8 vault 추출. 제안 결계72좌표는 모두 공기. 결계 설치 승인을 뜻하지 않음 |
| 전체 단위 회귀 | PASS | `gradle_final_test.txt`, `unit_test_summary.json`: 129클래스·583테스트, 실패/오류/생략0. 신규12건 포함 |
| 인스턴스·상태 전이 | PASS (unit) | 차원/원점/회전 ID, 회전 앵커, 다른 탑 분리, 잘못된 앵커 거부, 중복 완료·재등록 시 비트 보존, 층별4개 quorum, 겹침 거부 |
| 완료 신호 | PASS (unit) | ACTIVE/실패 reset은 완료 아님. 보상 배출 뒤 COOLDOWN만 기록. 재로드 첫 tick의 cooldown 만료도 포착 |
| 저장 schema 손상 | PASS (unit) | 리뷰에서 vanilla resultOrPartial의 부분 디코딩 문제를 RED 재현 후 수정. 잘못된 헤더/필드·중복/미래 버전은 입력 보존 및 quarantine, 정상 빈 레지스트리로 진행하지 않음. `quarantine_red.txt`는 수정 전 실패 증거 |
| 실제 Fabric26.3 모드 서버·4방향 배치 | PASS (해당 범위) | `live_setup.json`: /place template strict, 4회전의 실제24기능앵커 등록 성공. 이는 모든 블록의 사후 좌표 대조나 자연 생성 검증이 아님 |
| 실제 서버의 완료 관찰·독립성 | PASS (합성 상태) | 새4개 인스턴스 중 첫 탑의16스포너에 vanilla cooldown 상태 주입. 층별 누적 mask15→255→4095→65535. 나머지3개 mask0. 재등록·동일 이벤트 반복 후65535 유지 |
| 청크 언로드·로드 | PASS (해당 범위) | `live_chunk_cycle.json`: execute if loaded 실패로 실제 언로드 확인, 재로드 후 성공 확인, 전체 레지스트리 문자열 동일 |
| 재시작 복원 | PASS (정상 종료/재시작) | `live_restore.json`: 같은 월드 stop/start 후12개 인스턴스의 전체 status가 종료 전과 일치. 첫 탑65535, 나머지 기존 기록 보존. 전원 차단/쓰기 실패 복구를 의미하지 않음 |
| 실제 전투·플레이어 진입·재접속·멀티플레이 | LIVE_SERVER_UNVERIFIED | RCON 시험은 플레이어 전투/접속을 수행하지 않음. 공유 상태의 중복성은 unit/합성 신호로만 검사 |
| 자연 생성·지형 접속·다른 구조물 간섭 | NOT RUN | 생성 정책 확정 전 새 structure_set/기존 풀 교체 없음 |
| 물리 결계·보상 드롭·성소 전체 완료 | NOT RUN | 사용자 결정 대기. 논리 floorCleared와 물리 통행 제한을 구분 |
| 실제 클라이언트 이동·상호작용 | LIVE_CLIENT_UNVERIFIED | 이번 통합의 클라이언트 실행/스크린샷 없음. FULL BUILD의 기존 외관 검증을 승계하지 않음 |

파일 손상으로 gzip/NBT 자체를 읽을 수 없는 경우, 디스크 쓰기 실패와 강제 전원 종료 원자성은 검증하지 않았다. quarantine은 디코딩 가능한 저장 입력의 스키마·의미 오류를 다룬다. 이 단계 Java에는 아이템 지급과 결계 블록 변경이 없어 별도 지급 장부나 새 보상 확률을 도입하지 않는다.

초기 Gradle 플러그인 실패는 Java 프록시 설정 문제였으며 버전 변경 없이 해소했다. 초기 빈 서버가 자동 정지되어 합성 이벤트가 관찰되지 않은 경우 tick sprint로 재개한 뒤 확인했고, 최종 시험은 pause-when-empty-seconds=0으로 실행했다. 원래 개발 월드 대신 `/tmp/sky-tower-integration-server`를 사용했다.

추가 저장 경계: 첫 레코드의 과도한 정수 원점이 빈 overlap 검사를 통과하여 이후 앵커 tick에서 overflow 나는 결함을 `coordinate_overflow_red.txt`로 재현했다. 생성 시 회전된 전체 템플릿 끝점의 표현 가능성을 검증하여 기존 restore의 quarantine으로 전달하도록 수정했다. 3축 MAX 원점 회귀를 포함한 최종583 unit PASS이며, 네 회전 범위는 독립 리뷰에서 확인했다. 기존 정상 명령 좌표·건축 치수는 바꾸지 않았다.

추가 좌표 guard를 포함한 최종 코드로 전용 월드를 다시 시작하여12개 인스턴스 status의 정확한 일치를 재확인했다(live_restore.json, live_final_server_excerpt.txt).
