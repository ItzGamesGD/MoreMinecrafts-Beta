# INTEGRATION-02 검증 결과

2026-11-08 재개 확인: 원격 `e05d505`의 격리 작업 트리에서 승인 NBT 동결 검사와 여섯 섬 소켓/입구 감사를 재실행해 PASS했다. `./gradlew --no-daemon test`는 전체 단위 588건, 실패·오류 0으로 BUILD SUCCESSFUL. 원본과 별도 경로에 재생성한 `island_overlap_probe.json` 바이트 비교도 통과했다. 이 재개 턴에 전체 GameTest·실제 서버/클라이언트·자연 생성을 새로 검증하지 않았다.

**Phase B의 검증된 범위만 PASS. 전체02는 미완료이며 전체 회귀가 일괄 녹색이라고 주장하지 않는다.** 자연 생성 교체/자동 등록/섬 접속은 Phase C 수용 충돌로 보류다.

| 검사 | 판정 | 근거·범위 |
|---|---|---|
| 승인 NBT·기존 설계 보존 | PASS |01 verify_template.py와 package_verification.json: 승인16ad9d5·설치본·최종 JAR 내부 NBT가 동일.16스포너·8금고·72공기칸 확인 |
| 기존 unit 기준 | PASS | baseline_unit_summary.json:583건, 실패0 |
| 최종 unit 전체 | PASS | unit_summary.json / waterlogged_green_build.txt:588건, 실패·오류·생략0. 기존583+추가5 |
| 최종 천공탑 GameTest | PASS (집중4건) | waterlogged_green_build.txt:4건 모두 PASS, 전체 unit·데이터 검증·패키징도 성공 |
| 전체 서버 GameTest 배치 | 529 PASS / 1 FAIL | serial_final_build.txt:530건 완료, 기존 absenceGraceResumesOnReturnAndExpiresOtherwise 한 건 실패. 신규3건은 모두 통과 |
| 위 기존 실패의 단독 재현 | PASS (코드/값 변경 없음) | legacy_grace_isolated.txt: 새 월드에서 해당1건 PASS·BUILD SUCCESSFUL. 전체 배치 실패 기록을 대체하지 않음 |
| 실제4회전 결계 | PASS (서버 블록 검사) | live_setup.json: 각72개, 총288개 실제 barrier 확인 |
| 실제 제거/쿨다운 혼합 완료 | PASS (서버 합성 시험) | 실제 명령 제거3개→mask7/cause7, 네 번째 cooldown→mask15/cause7, 결계18개 제거. 실제 플레이어 전투는 아님 |
| 영구 상태·외부 블록·재설치 | PASS (서버 해당 범위) | live_setup.json: 전투4층 mask65535/cause65527, 외부 stone 보존, 완료층 재설치 없음, 스포너 재설치로 초기화 없음 |
| 청크 언로드/로드 | PASS (서버 해당 범위) | 미완료 탑의 execute if loaded 실패/성공, 기존 결계 유지 |
| 정상 서버 재시작·소유권 | PASS (해당 범위) | live_restore.json:16개 인스턴스 상태 일치, 저장 소유권으로 다음 탑18개 결계 실제 제거 |
| 인접 엔티티 조회 halo | PASS (최종 unit/GameTest) | 이웃 저장 미준비 음성 대조와 청크 경계에 몸체가 걸치는 실제 엔티티 보류. EntitySectionStorage의 수평±2 조회 범위까지 검사하며 강제 로드 없음 |
| 최종 서버의 재조정 | PASS (해당 범위) | live_check.json: 소유 결계1개 실제 삭제→재설치, 다른 탑의 필수4개 제거→18개 해제 |
| 외부 waterlogged 상태 보존 | PASS (최종 GameTest/서버) | waterlogged RED→정확한 기본 상태 비교 수정. GameTest의 stale 소유권 주입에도 wet 상태 보존. live_state.json은 실제 외부 wet 변경 후 완료에도 wet 유지 및 다른 소유 벽 제거 확인 |
| 실제 금고 설정32개 | PASS (설정 범위) | live_vaults.json: vanilla table/key 유지. 기본 table 필드 생략은 공식 VaultConfig.DEFAULT/Codec 규칙(vault_codec_reference.txt) |
| 현재 섬 슬롯 수용 | CONFLICT | worldgen_audit.json: 여섯 스타일의 중앙 슬롯 AABB 불일치와 입구 아래 지지면0개 |
| 자연 생성 교체·자동 등록 | NOT RUN / 미구현 | 생산 월드젠 변경 없음. WORLDGEN_REPLACEMENT.md의 최소 변경 후보 검토 필요 |
| 실제 전투·금고 사용 | LIVE_SERVER_UNVERIFIED | 명령 제거/합성 cooldown/서버 GameTest와 실제 플레이를 구분 |
| 클라이언트·멀티플레이·재접속 | LIVE_CLIENT_UNVERIFIED | 실제 플레이어 세션·동시 참여 시험 미실행 |

## 실패와 수정 이력

1. baseline_regression.txt는 이전01시 콜로세움 ArenaRecovery 저널을07시에 재사용해18건 실패했다.65분 시한을 지난 저장 시도를 복원했다. 원본 테스트 월드는 보존했으며 실패 이름·저널 timestamps/해시는 baseline_failures.json에 있다. 콜로세움 코드는 변경하지 않았다.
2. 중간 final_build.txt는 새 엔티티 저장 미로드 반례1건을 찾았고, 이어 병렬 compileJava가 실행 중 JVM의 RewardDelivery$State lazy-load와 충돌해 중단됐다. 기존527 전체 통과 기록이 아니다. 다른 실행을 종료하고 소스를 동결한 새 단독 실행으로 검사를 반복했다.
3. 엔티티 저장 및 인접 조회 halo를 검사하도록 수정했다. halo_build.txt에서 신규3건·unit588·패키징 성공. 이후 실제26.3 barrier의 WATERLOGGED 변경 반례를 waterlogged_red.txt로 재현하고, 정확한 기본 상태 소유권으로 수정했다. 최종 waterlogged_green_build.txt의 신규4건·unit588·패키징은 PASS다.
4. 전체 단독530의 유일한 기존 실패는 테스트 전용 grace1500ms와 tick40이탈→tick60복귀 가정이다. 배치 부하 로그에57초/1153tick 지연이 기록됐고, 복귀 전에 ABSENCE_EXPIRED가 발생했다. 같은 기존 테스트를 새 월드에서 코드·값 변경 없이 단독 실행하면 통과했다. 이를 근거로 전체 배치가 녹색이었다고 해석하지 않는다.

구조물 단위 공유·다른 인스턴스 격리·첫 원인 보존·구버전 완료/소유권 기본값은 unit 검사, 실제 블록/엔티티·변경 훅·결계 제거는 GameTest 및 격리 서버로 확인했다. 실제 플레이어 전투·바닐라 금고의 개인별 지급/QuestVaultRewards 상호작용은 새 라이브 PASS로 승계하지 않았다. 전원 차단/쓰기 실패의 월드·SavedData 원자성도 검증하지 않았다.

현재 등록된 전체 GameTest는531개(기존527+신규4)다. serial 전체 실행은 네 번째 waterlogged 검사를 추가하기 전530개였으며, 그 뒤 정확한 상태 소유권의 국소 수정은 신규4개 집중 실행과 전체588 unit/패키징으로 검증했다. 최종531개 전체 재실행은 NOT RUN이다.
