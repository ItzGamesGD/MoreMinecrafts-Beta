# SKY-TOWER-INTEGRATION-01 architecture

기준: feature/sky-tower-full-build-01 @16ad9d5. 시작 원격16ad9d5, main cde842d. merge-base가main과 같고 건축 브랜치가355커밋 앞서 있으므로 그 HEAD에서 feature/sky-tower-integration-01 생성. main/기존 브랜치 변경·병합 없음.

## 실제 기존 시스템

- `worldgen/structure/sky_island_*.json`의 vanilla jigsaw 및 `worldgen/template_pool/sky_islands/*/start.json`. 탑은 여러 섬 NBT에 이미 구형 건축으로 합성되어 있다. 승인 탑의 단독 NBT를 단순히 구형 tower.nbt에 덮으면 섬의 지면·크기·진입을 보장할 수 없어 교체하지 않았다.
- `StructurePoolContributions`: ancient-city 등 기존 풀 확장. 범용 던전 진행 엔진이 아니다.
- `QuestStructureEntry`, `QuestVaultRewards`: 퀘스트 목표 기록이며 탑 스포너·문을 관리하는 API가 아니다.
- `ColosseumData/Runtime/Controller`: ST05 전용 경기장·웨이브·시간/복구 엔진. Q07의 바닐라 시련 대신 이를 쓰면 별도 웨이브가 되므로 재사용하지 않는다.
- 저장은 기존 프로젝트와 같은 vanilla `SavedData`/Codec 방식을 재사용한다. 새 이벤트 버스나 별도 보상 엔진을 만들지 않았다.
- 바닐라 시련 완료·열쇠·금고의 플레이어별 사용·보상 영속성은 원래 BlockEntity가 소유한다. SkyTowerData는 이를 복제하지 않는다.

## 구현 흐름

수동 명령 배치(approved NBT)
→ `/mmskytower register x y z rotation` (16스포너+8금고 앵커가 해당 차원에서 로드되어 있는지 확인)
→ dimension|templateOrigin|rotation 안정 ID 및 중복/겹침 거부
→ Overworld SavedData 레지스트리
→ 실제 TrialSpawner.tickServer HEAD/TAIL 관찰
→ vanilla 보상 배출 후 COOLDOWN 진입 또는 저장된 COOLDOWN 재관찰
→ 정확한 해당 탑·차원의 스포너 앵커 하나에만 영구 완료 비트
→ 층당4비트의 quorum이 모이면 논리적 floorCleared
→ **물리 결계 해제 미연결, 사용자 결정 대기**.

`SkyTowerCompletionObserver`는 스포너 생성·보상·적 제거·플레이어 상태를 변경하지 않는다. 몹 부재, ACTIVE→WAITING reset, 단순 플레이어 이탈은 완료가 아니다. cooldown 시작/종료 tick을 모두 관찰해 재시작 후 만료 첫 tick에도 기록 가능하다. 이미 기록된 비트는 다시 dirty 처리하지 않는다. 훅은 등록되지 않은 탑/다른 차원의 앵커를 완료하지 않는다.

`SkyTowerProgress`는 총16비트(4×4), 서버 스레드 전용이다. 층을 우회해 올라간 경우에도 실제 완료 사실을 그대로 기록하며 Q22에 없는 강제 복귀·보호·참여자 리셋은 추가하지 않는다. allFloorsCleared는 전투 사실이며 성소 도달/전체 콘텐츠 완료 이벤트가 아니다.

`SkyTowerData`의 version1 snapshot은 차원/원점/회전/완료 비트를 저장한다. 재등록은 초기화하지 않는다. 잘못된 mask·회전·중복/겹침 저장 레코드·미지원 버전은 레지스트리를 quarantine하여 진행 변경을 차단하고 이 상태 자체도 저장한다. 구조적으로 잘못된 필드/헤더는 vanilla resultOrPartial로 정상 빈 레지스트리가 되지 않도록 감싸며, 손상 입력의 JSON 정규화 사본을 quarantine_payload에 보존한다. 실제 파일 손상/쓰기 실패/전원 차단 원자성은 별도 장애 주입 검증 전 보장하지 않는다. 이 단계의 Java는 아이템 지급이나 물리 결계 조작을 하지 않는다.

## 아직 연결하지 않은 부분

자연 생성의 풀 교체·빈도/섬 접속·인스턴스 자동 등록, 플레이어 진입/성소 완료 정책, 물리 결계, 하늘섬 보상 테이블 연결은 보류. `DECISION_REQUIRED.md`의 확정 전 임의의 기본값을 적용하지 않는다. 수동 등록 명령은 자연 생성 훅의 대체 구현이 아니다.
