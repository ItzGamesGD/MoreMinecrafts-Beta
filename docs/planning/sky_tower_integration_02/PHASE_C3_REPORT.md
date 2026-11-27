# SKY-TOWER-INTEGRATION-02 — Phase C-3 구현 및 검증 보고

## 상태

- **IMPLEMENTED:** 여섯 하늘섬 `tower.nbt`를 승인 탑과 섬별 최소 연결대로 합성하고, 기존 weighted start-pool의 가중치와 부모 Jigsaw 구조 정의를 유지했다.
- **SERVER VERIFIED:** 여섯 합성 리소스를 실제 Fabric GameTest 서버에 배치했다. 스포너 16개, 금고 8개, 결계 예약 공백 72칸과 네 방향 회전 변환을 확인했다.
- **NATURAL WORLDGEN SERVER VERIFIED:** 새 월드의 자연 Jigsaw 생성에서는 forest, rocky, meadow 합성 조각을 확인했다. 관찰된 세 스타일은 Structure Start 기반 자동 등록, 스포너·금고 앵커, 72개 결계 공간, 단일 복합 조각 및 구형 tower 자식 미생성을 통과했다.
- **POST-INTEGRATION QA PENDING:** 클라이언트 이동, 시각적 지지 확인, 멀티플레이어는 미검증이다. 자연 생성 인스턴스의 서버 재시작 후 Structure Start 재발견과 SavedData 복원은 격리 시험 월드에서 확인했다. 진행 비트는 실제 전투가 아니라 probe가 API로 설정한 표식이다.
- 여섯 스타일 전체 자연 생성 샘플은 이번 월드 탐색에서 확보되지 않았다. 미관이나 보행을 검증된 것으로 표현하지 않는다.

## 기준과 구현

작업 브랜치는 `feature/sky-tower-integration-02`, 시작 HEAD는 `1f45fd104dead53449b0c421841b2feeaf9a113b`이다. 승인 탑 원본 `full_01.nbt` SHA-256은 작업 전후 `cec5bced051fdddcf5ebe80ed267ea3c5749126559ef982f82ac1b4dd435cd27`로 동일하다. 기존 Phase B 결계·스포너·금고 로직은 수정하지 않았다.

로컬 Python 환경에서 프로젝트가 이미 선언한 `nbtlib==2.0.4`를 설치해 사용했다. `scripts/structures/requirements.txt`에 추가 의존성은 넣지 않았다. 합성은 NBT 바이너리 수작업 파싱 없이 이 라이브러리로 결정적으로 수행하고, gzip 출력의 mtime을 0으로 고정했다.

기존 C-2 후보 B(`dx=-12,dz=0,y=17`, 부모 회전 기준)를 그대로 사용했다. 원래 섬 커넥터 `(21,16,21)`은 합성 조각에서 `(36,16,24)`이고, 승인 탑의 로컬 최소점은 합성 조각 기준 `(0,17,0)`이다. 크기는 모든 스타일에서 `58×97×49` 블록이다. 탑의 커넥터 상대 점유 범위는 X `-36..13`, Y `+1..+80`, Z `-24..24`이다. 이 루트 요소의 Jigsaw 커넥터는 `minecraft:empty`로 종료해 완성 탑 위에 구형 `tower_5/6/7` 조각이 한 번 더 붙지 않게 했다. 기존 start pool의 `tower` 선택 가중치 3과 나머지 항목 가중치 `3,3,4,6,2`는 그대로다. 합계 21 중 tower 선택률은 Jigsaw 시작 풀 선택 기준 `3/21 = 14.29%`이다.

구조 정의의 `size:4`, `max_distance_from_center:80`, 시작 높이, biome tag, structure set의 spacing·frequency·salt는 바꾸지 않았다. 합성 탑은 시작 풀의 루트 단일 요소 안에 들어가며 확장 커넥터가 종료되므로 탑 크기만큼 후속 Jigsaw 경로가 더 뻗지 않는다. 실제 natural-generation 표본의 piece bounding box도 아래에 기록했다.

기존 섬 요소를 탑 블록으로 덮은 좌표 외에, 탑 점유 상자 안에서 승인 탑이 비워 둔 위치에 남아 있던 섬 블록은 명시적 air로 치워 내부 나뭇잎·줄기·지형 블록이 탑 공간을 막지 않게 했다. 이 국소 제거는 다음과 같다.

| 스타일 | 승인 탑 블록과 기존 섬의 덮어쓰기 | 탑 내부 여유 공간을 비운 기존 블록 |
|---|---:|---:|
| snowy | 171 | 124 (눈 84, spruce leaves 32, spruce logs 8) |
| meadow | 147 | 114 (oak leaves 74, 짧은 풀 18, 키 큰 풀 11, oak log 1, 꽃 10) |
| forest | 143 | 425 (oak leaves 236, birch leaves 143, oak logs 18, birch logs 9, 기타 19) |
| badlands | 6 | 7 (dead bush 4, orange terracotta 3) |
| desert | 1 | 7 (dead bush 5, cactus 2) |
| rocky | 15 | 10 (cobblestone 9, mossy cobblestone 1) |

이 조정은 새 장식이 아니라 탑 점유 상자 안의 충돌 제거다. 특히 forest 수목 제거 범위는 상대적으로 크므로 외관 검토가 필요하다. 기존 기능 블록·승인 구조물 손상을 자동으로 승인했다는 뜻은 아니다.

합성 조각에 추가한 C-2 연결대·지지 복셀 수는 snowy 37, meadow 33, forest 35, badlands 35, desert 34, rocky 38이다. 연결 경로의 실제 보행과 시각적 지지 품질은 후속 클라이언트 QA로 남아 있다.

자동 등록은 `ServerChunkEvents.CHUNK_LOAD`에서 저장된 Structure Start의 `SinglePoolElement`를 확인하고, 합성 piece 위치·회전으로 탑 원점을 복원해 pending 큐에 넣는다. piece 전체 X/Z 청크가 로드되고 16 Trial Spawner·8 Vault가 기대 위치에 있으며 72개 결계 셀이 비어 있을 때만 기존 `SkyTowerData`에 등록한다. 동일 ID는 건너뛰고 겹침은 오류로 남기며, 서버 종료 시 메모리 큐를 비우므로 재시작 후 로드되는 Structure Start에서 다시 발견한다. 실제 디스크 저장/재시작 후 같은 Structure Start에서 Site 수가 1에서 1로 유지되고 진행 마스크 1이 복원되는 것을 확인했다. 이 진행 비트는 시험용 API 표식이며 Trial Spawner 전투 완료를 검증한 결과는 아니다. 자동 등록을 별도 신규 쓰기 없이 재수집해 중복이 생기지 않는 것도 확인했다.

## 자연 생성 서버 표본

- 새 월드 seed `2026110902`, 반경 24 청크: 2,401 청크를 조사해 forest를 발견했다. rotation 3, Structure Start `moreminecrafts:sky_island_forest`, piece bounds `X=-288..-240, Y=137..233, Z=103..160`, 복원 Site `(-288,154,160,r3)`이다. 16/16 스포너, 8/8 금고, 72/72 공백, 모든 piece 청크 로드, 단일 composite, legacy tower 자식 0, 자동 등록 PASS.
- 새 월드 seed `2026110903`, 반경 160 청크: 103,041 청크 조사 후 forest와 rocky 두 스타일을 찾았다. forest rotation 0 bounds `X=1600..1657, Y=182..278, Z=192..240`, Site `(1600,199,192,r0)`; rocky rotation 1 bounds `X=192..240, Y=120..216, Z=672..729`, Site `(240,137,672,r1)`. 두 표본 모두 위 앵커·공백·청크·중복·자동 등록 검사를 PASS했다.
- 별도 새 월드 seed `42`, 반경 160 청크에서 meadow(rotation 0), forest(rotation 3), rocky(rotation 0)를 추가 확인했다. 각각 `Site (528,158,-1488,r0)`, `(-352,132,-224,r3)`, `(1872,171,-880,r0)`이며 모두 16/16 스포너, 8/8 금고, 72/72 공백, 자동 등록, 단일 composite, legacy tower child 0을 통과했다.
- `snowy`, `badlands`, `desert`는 두 탐색에서 자연 생성 표본을 찾지 못해 **UNVERIFIED**다. 탐색 부재는 배치 실패나 구조 충돌의 증거가 아니다.

원시 결과는 [반경 24 결과](PHASE_C3_SERVER_PROBE_RADIUS24.json), [seed 2026110903 반경 160 결과](PHASE_C3_SERVER_PROBE_RADIUS160.json), [seed 42 결과](PHASE_C3_SERVER_PROBE_SEED42.json), [저장/재시작 검사](PHASE_C3_SERVER_RESTART.json)에 있다. 반경 160 프로브가 기록한 103,041 청크 탐색에는 약 3분 47초가 걸렸다. 각 natural-generation probe의 전체 `pass:false`는 여섯 스타일 모두를 관찰하지 못한 표본 범위 결과다. 관찰된 개별 스타일의 `pass:true`와 구분한다.

## 검사 결과

- `python3 -m unittest scripts.structures.sky_islands.test_tower_composite -v`: 5 tests PASS. 결정성, 승인 NBT SHA, 16/8 앵커, C-2 중첩 수, 6개 start-pool 가중치를 확인했다.
- `python3 scripts/structures/sky_islands/validate.py`: 56개 템플릿 PASS. 기존 ruin·tower·temple 요소, NBT 좌표와 풀 참조도 포함한다.
- `python3 scripts/structures/sky_islands/generate.py --check`: PASS. JSON 연결과 여섯 결정적 composite NBT를 비교한다.
- `./gradlew test --offline` (JDK 25): 590 JUnit tests PASS.
- `runGameTest`의 `sky_tower_game_tests*`: fresh test world에서 5/5 PASS. 원래 Phase B 4개 테스트와 여섯 합성 스타일의 블록 배치·회전·앵커·결계 공간 검사를 포함한다.
- 실제 자연 생성 서버 프로브: forest, rocky, meadow 개별 표본 PASS; snowy, badlands, desert 표본은 UNVERIFIED.
- 저장/재시작 프로브: 동일 Site 1→1, Structure Start 재발견, progress mask 1→1 PASS. 값 1은 시험 API 표식이다. 실제 클라이언트 이동·시각과 다중 접속은 미실행.

## 후속 QA 및 구현 범위

C-3 코드는 provisional C-2 좌표의 구현이다. 기존 섬 경로에서 입구까지 클라이언트 보행, 다리 난간과 기단 지지의 시각적 타당성, 스타일별 외관, 네 방향 각각의 자연 생성 회전 표본은 아직 확인하지 않았다. 서버 재시작 후 Structure Start 재발견과 시험용 SavedData 표식 복원은 확인했다. 실제 전투 진행을 통한 복원, 클라이언트 이동·외관 및 멀티플레이 QA는 남았다. 기존 승인 NBT·Phase B 결계·Trial Spawner·vanilla Vault 설정·특별 옥상 보상은 변경하지 않았다. C-3 검증 산출물만으로 자연 생성 후보의 최종 시각 승인이나 클라이언트 게임플레이 완료를 선언하지 않는다.
