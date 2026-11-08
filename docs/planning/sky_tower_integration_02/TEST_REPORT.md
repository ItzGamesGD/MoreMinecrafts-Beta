# INTEGRATION-02 tests

구현된 Phase B와 전체02 완료를 구분한다. 자연 생성 교체/자동등록/새 지형 접속은 Phase C 수용 충돌로 보류이며 PASS가 아니다.

| 검사 | 현재 결과 | 근거 |
|---|---|---|
| 승인 NBT 및 기존 설계 보존 | PASS | 01 verify_template.py: 승인16ad9d5와 리소스 바이트 동일,16스포너/8금고/72공기칸 확인 |
| 기존 unit 기준 | PASS | baseline_unit_summary.json:583건,실패0 |
| 기존 서버 GameTest 첫 실행 | CONFLICT (재사용 월드) | baseline_regression.txt:18건 콜로세움 실패.01시 ArenaRecovery저널을07시 재사용해65분 시한 만료. 원인·실패 이름·보존 경로는 baseline_failures.json |
| 새 unit 전체 | PASS | unit_summary.json / halo_build.txt:588건. 기존583+추가5 lifecycle 검사, 실패0 |
| 최종 신규 GameTest3 | PASS (집중 범위) | halo_build.txt: 엔티티 저장 및 인접 조회 halo 수정 후3건 PASS·전체 unit/build 성공 |
| 전체530 최종 직렬 회귀 | 실행 중 | serial_final_build.txt. 소스/다른 실행을 동결하고 새 월드에서 단일 build |
| 실제 모드 서버4회전 결계 | PASS | live_setup.json: 각72개,총288개 실제 barrier 확인 |
| 실제 제거/정상 완료의 혼합 진행 | PASS (서버 합성 시험) | 첫3개 실제 명령 제거→mask7/cause7,네 번째 cooldown→mask15/cause7,결계18개 제거. 실제 플레이어 전투가 아님 |
| 외부 블록 보존·영구 상태 | PASS (서버 해당 범위) | 두 번째 층의 소유 barrier를 실제 삭제 후 stone으로 교체.4층 완료mask65535/cause65527에서stone유지,완료층air유지,재설치스포너로진행초기화없음 |
| 청크 언로드/로드 | PASS (서버 해당 범위) | 미완료 두 번째 탑 execute if loaded 실패/성공 확인,기존결계유지 |
| 정상 서버 재시작·소유권 복원 | PASS (해당 범위) | live_restore.json:16개 인스턴스 정확한 상태 일치,저장 소유권으로 다음 탑18개 결계 실제 제거 |
| 최종 halo 코드 서버 재조정 | PASS (해당 범위) | live_check.json: 소유 결계1개 삭제→재설치,다른 탑4스포너 실제 제거→18개 해제 |
| 실제 금고 설정32개 | PASS (설정 범위) | live_vaults.json: vanilla loot table/key 유지. VaultConfig.DEFAULT의 loot_table 생략 규칙은 vault_codec_reference.txt 참조 |
| 자연 생성 교체·자동 등록 | NOT RUN / 미구현 | 현재 슬롯/입구 지지면 반례,월드젠생산변경없음 |
| 실제 스포너 전투·금고 상호작용 | LIVE_SERVER_UNVERIFIED | RCON 상태 주입/명령 제거와 단위/서버GameTest만 수행 |
| 클라이언트·멀티플레이·재접속 | LIVE_CLIENT_UNVERIFIED | 실제 플레이어 세션/동시 참여 시험 미실행 |

단위 검사는72셀의4회전/중복 없음,다른 인스턴스 격리,정상↔파괴 경합의첫원인보존,완료쿼럼,소유권코덱복원,구버전schema기본값/진행보존을 검사한다. 새 GameTest는 실제 승인 NBT 배치·앵커·결계 설치/해제,비공기/엔티티설치보류·외부barrier보존,실제블록삭제훅/cooldown혼합완료를 검사한다.

추가 아이템 지급 코드·금고 설정 변경은 없다. 실제 바닐라 금고의 플레이어별 지급·QuestVaultRewards 상호작용은 별도 실제 클라이언트 검증을 하지 않았으므로 새 라이브 PASS로 올리지 않는다. 전원 차단/쓰기 실패의 월드·SavedData 원자성은 검증하지 않았다.

중간 final_build.txt는 신규 엔티티 저장 경계 RED1건을 기록했고, 이후 병렬 compileJava로 실행 중 서버의 RewardDelivery$State lazy-load가 실패해 중단됐다. 기존527 전체 완료 기록이 아니다. 단위 소스 및 리소스 동결 후 serial_final_build.txt를 단독 실행하여 최종 전체 결과를 확인한다. 이 실행 순서 문제 때문에 콜로세움 코드나 게임 규칙을 바꾸지 않았다.
