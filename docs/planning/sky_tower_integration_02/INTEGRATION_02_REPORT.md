# SKY-TOWER-INTEGRATION-02 — Phase B 구현, C 보류

**INTEGRATION-02 전체 구현 완료 아님. FULLY LIVE VERIFIED 아님.** 승인된72칸 런타임 결계와 필수 스포너 파괴 진행을 구현했다. 기존 섬의 자식 슬롯에 승인 탑이 맞지 않아 생산 자연 생성 교체와 자동 등록은 보류했다. 섬을 임의 확대하거나 승인 건축을 수정하지 않았다.

## 기준과 변경

원격 feature/sky-tower-integration-01 @387a50c와 일치하고 시작 작업 트리는 clean이었다. 기존357ec45/387a50c를 조사하고 feature/sky-tower-integration-02를 생성했다. 승인 건축16ad9d5, 금고8개의 vanilla NBT, Q01–Q118, P1–P6 및01 산출물은 보존한다. main 수정·강제 푸시·PR·병합은 없다.

사용자가 확정한 기존 하늘섬 정책 재사용/구형 탑 교체, vanilla 금고 유지,72칸 barrier, 필수 스포너 파괴 완료 인정/추가 보상 없음, 옥상 보상 보류는 재질문하지 않았다. 새 빈도·보상·보호·강제 복귀 시스템을 만들지 않았다.

기존 SkyTowerData/Site/TrialSpawner COOLDOWN 관찰과 vanilla SavedData를 확장했다. schema1에 optional destroyed_spawners/owned_barriers를 추가하며 구버전 완료를 보존한다. LevelChunk의 실제 성공한 변경 후에만 스포너 제거와 barrier 소유권 해제를 기록한다. 전 셀과 인접 엔티티 조회 청크가 준비되기 전 설치하지 않으며 강제 로드하지 않는다. 외부 블록/엔티티를 덮지 않고, 소유한 barrier만 제거한다.4스포너 혼합 완료의 영구 해제, 다른 차원/탑 격리, 완료 원인 보존과 재설치 시 진행 보존을 구현했다.

변경 코드는 SkyTowerData/SkyTowerLayout/SkyTowerIntegration, 신규 SkyTowerBarriers/SkyTowerBlockChanges와 LevelChunk mixin/등록이다. 새 unit1파일, GameTest1파일과 테스트 엔트리를 추가했다. 다른 콘텐츠의 Java·구조물 NBT·월드젠 정책·보상 리소스는 수정하지 않았다. 금고는 vanilla 설정 그대로이며 QuestVaultRewards의 기존 관찰과 충돌하는 추가 지급 로직이 없다. 옥상·유물·특별 보상·전체 완료 이벤트도 추가하지 않았다.

## 자연 생성 수용 충돌

실제 경로는 섬 중앙 jigsaw → tower_top → 별도 tower_5/6/7이다.01의 직접 합성 설명과 달라 이번 직접 감사 결과로 정정했다. 여섯 스타일 모두43×74×43 parent와 상부58 여유에 신규50×80×49가 맞지 않는다. 중심 정렬 원점(-3,17,-3)의 AABB는(-3,17,-3)..(46,96,45)이며 입구 아래 열에 섬 블록이 없다. 승인 NBT에 incoming jigsaw도 없어 이름만 pool에 바꾸지 않았다.

WORLDGEN_REPLACEMENT.md와 worldgen_audit.json에 빈 경계 확장50×97×49(+7/+23/+6), 최소6블록 반경 부족의 물리 접속/편심 배치 후보, 상부 월드 높이 위험을 기록했다. 실제 섬 접속 형태와 배치는 사용자 검토가 필요하다. 이는 설계 전체의 불가능성 증명이 아니라 기존 중앙 슬롯의 수용 실패다. **현재 생산은 구형 탑을 계속 생성하며 신규 자연 생성/자동 등록은 미구현이다.**

## 검증과 판정

기준 unit583건 PASS, 새 lifecycle/인접 저장 경계5개 포함 최종 unit588건 PASS. 최종 집중 build의 신규 서버 GameTest3건도 PASS다. 실제26.3 모드 서버에서4회전288결계 설치, 실제 명령 제거/합성 cooldown 혼합 완료, 외부 석재 보존, 청크 재로드, 정상 서버 재시작과 소유권 복원,32금고의 vanilla 설정 보존을 확인했다. 최종 halo 코드로 결계를 재설치하고 실제 제거 완료 후 해제하는 시험도 PASS다.

회귀 실행을 혼동하지 않는다. 첫 기준 GameTest는 예전 콜로세움 복구 저널을 재사용해18건 실패했다. 그 월드를 보존하고 새530 실행을 시작했으며, 새 엔티티 로드 경계 반례를 찾았다. 이어 병렬 컴파일이 실행 중 JVM의 lazy 클래스 로딩과 충돌해 중단됐다. 이를 기존527 PASS로 기록하지 않는다. 해당 결함/실행 순서를 수정하고, 다른 실행과 소스를 동결한 최종 단일530 build를 serial_final_build.txt로 검사 중이다. 세부 결과는 TEST_REPORT.md와 CHECKPOINT.md를 참조한다.

실제 플레이어 전투·금고 사용·클라이언트·멀티플레이는 미검증이다. 검증된 B를 커밋·푸시로 보존하며 정확한 SHA는 Git 이력과 최종 응답에 기록한다. 다음 필수 작업은 최소 섬 접속안의 검토, 생성 어댑터/자동 등록 및 신규 청크 검증이다. 옥상 특별 보상은 승인된 이번 범위 밖의 보류 항목이다.
