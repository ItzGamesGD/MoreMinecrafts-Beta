# DEFECT REGISTER

기준 `ee37afe`. CONFIRMED=소스/바이트코드/문서로 근거 확보, SUSPECTED=근거 부족(실행 검증 필요).

합계 34건 — P0 1 · P1 16 · P2 17 / CONFIRMED 23 · SUSPECTED 11

## D-01 [P0·CONFIRMED] Knowledge 카운터 상한 때문에 수량 증가 퀘스트 17개가 달성 불가

- **근거:** `KnowledgeConditionProgress.advance` → `Math.min(sharedMaximumTarget, …)`; `KnowledgeRuntime.recordCondition`이 `condition.<key>`를 Knowledge 최대 목표에서 고정
- **최소 재현:** 광석 1,000개 채굴 후 `condition.ore_items_mined` 확인
- **예상 결과:** 값이 256에서 멈춤 → xpq.min.ore_1000은 영원히 미완료
- **필요 테스트:** Unit(advance 상한) + GameTest(257번째 채굴 후 값 불변)
- **수정 권고:** 대상 17개(매트릭스 D-01)는 상한 없는 `quest.counter.*` 전용 카운터로 바꾸고 추적을 R→I로 정정. 소급 주장 삭제

## D-02 [P1·CONFIRMED] 통계 기반 Knowledge 카운터 5개도 상한이 있어 소급 원천으로 잘못 지정됨

- **근거:** `KnowledgeRuntime.refresh`: boat/horse/elytra/breed/trade를 Stats에서 읽지만 recordCondition 상한 적용
- **최소 재현:** 보트 20,000블록 이동
- **예상 결과:** condition.boat_distance=5000에서 정지
- **필요 테스트:** GameTest
- **수정 권고:** `Stats.CUSTOM.BOAT_ONE_CM` 등을 직접 조회(S). 소급은 가능

## D-03 [P1·CONFIRMED] 설치한 광석·블록의 재채굴이 채굴 퀘스트 진행으로 집계됨(악용)

- **근거:** KnowledgeRuntime 블록 파괴 콜백은 `*_ore`면 출처 구분 없이 +1; 바닐라 BLOCK_MINED도 출처 무관. 고대 잔해/아다만티움 잔해는 항상 자기 자신을 드롭
- **최소 재현:** 실크터치로 다이아 광석 1개를 64회 설치·파괴
- **예상 결과:** min.diamond_ore_64 완료
- **필요 테스트:** GameTest(설치 블록 재채굴 무집계)
- **수정 권고:** 자연 생성 판별(K5 NaturalTreeProvenance와 같은 청크 마커 방식, 또는 플레이어 설치 블록 마커) 도입. 불가하면 해당 퀘스트를 고유 종류 수 기반으로 재설계

## D-04 [P1·CONFIRMED] 블록 설치 통계가 설치·파괴 반복으로 무한 누적

- **근거:** 26.3 `ItemStack.useOn`이 Stats.ITEM_USED를 지급(바이트코드 확인). 출처 구분 없음
- **최소 재현:** 블록 1개를 반복 설치·파괴
- **예상 결과:** bld.place_200000(엔드게임) 달성
- **필요 테스트:** GameTest
- **수정 권고:** 설치 후 일정 시간 유지된 블록만 집계하거나, 엔드게임 보상을 고유 블록 종류형으로 대체. 보상 재평가는 사용자 결정

## D-05 [P1·CONFIRMED] 차원별 이동 통계가 바닐라에 없음

- **근거:** 26.3 Stats.CUSTOM 목록에 차원 구분 없음(javap 확인)
- **최소 재현:** —
- **예상 결과:** xpq.dim.nether_travel_50k를 S로 추적 불가
- **필요 테스트:** Unit
- **수정 권고:** 네더 체류 중 이동 거리 신규 추적(I). 소급 불가로 정정

## D-06 [P1·CONFIRMED] biomes_all_overworld의 대상 목록이 정의되지 않았고 이름도 모순

- **근거:** 이름은 overworld인데 조건은 3차원 전체·"데이터팩 기준 목록". 바이옴 레지스트리는 데이터팩·모드로 바뀌고 생성되지 않는 바이옴(the_void 등)이 포함될 수 있음
- **최소 재현:** 데이터팩 바이옴 추가
- **예상 결과:** 달성 불가 또는 조건 변동
- **필요 테스트:** Unit(고정 목록)
- **수정 권고:** 고정된 명시 목록(26.3 바닐라 생성 바이옴)으로 정의하고 이름 정정 → VERIFY

## D-07 [P1·CONFIRMED] tame_5: 여우는 길들일 수 없고 조건 문장이 자체 모순

- **근거:** 26.3 `Fox extends Animal`(TamableAnimal 아님). TAME_ANIMAL 트리거는 TamableAnimal, AbstractHorse에서만 발생(라마 포함)
- **최소 재현:** —
- **예상 결과:** "여우 포함 5종"은 불가. "6종 중 5"는 여우가 빠지면 사실상 5종 전부
- **필요 테스트:** Unit
- **수정 권고:** {늑대, 고양이, 앵무새, 말, 라마} 5종 전부로 정정(여우 삭제)

## D-08 [P1·CONFIRMED] 해저 신전 공략 퀘스트의 플레이어 귀속 수단이 없음

- **근거:** `T2CoreAcquisition.rewardCompletedMonument(ServerLevel, ElderGuardian)`에 플레이어 정보 없음. 청크 미로드 시 완료는 서버 틱 재시도에서 발생
- **최소 재현:** 엘더 가디언 처치 후 청크 언로드
- **예상 결과:** 완료 시점에 귀속 플레이어 없음
- **필요 테스트:** GameTest(지연 완료 귀속)
- **수정 권고:** MonumentRewardData의 대기 항목에 신전 내 엘더 가디언 처치자 UUID 집합을 저장. "최종 처치자 단독"인지 "참여자 전원"인지는 사용자 결정 → VERIFY

## D-09 [P1·SUSPECTED] 전리품 퀘스트의 귀속·경로·"서로 다른 상자" 키가 미정의

- **근거:** `RandomizableContainer.unpackLootTable(Player)`가 GENERATE_LOOT를 트리거함(확인). 상자 수레(폐광) 경로는 미확인. 호퍼 추출은 플레이어가 null
- **최소 재현:** 다른 플레이어가 먼저 열기
- **예상 결과:** 먼저 연 사람만 진행
- **필요 테스트:** GameTest(블록 상자·수레 상자)
- **수정 권고:** `LootTableTrigger.trigger(ServerPlayer, key)` 중앙 훅 사용. 고유성 키 = 차원+좌표 또는 엔티티 UUID

## D-10 [P2·CONFIRMED] 연구 XP 지출 이력이 저장되지 않음

- **근거:** ResearchPurchaseService는 레벨만 저장. ResearchRootMigration이 루트를 이관할 수 있음
- **최소 재현:** 이관 플레이어
- **예상 결과:** 지출 0인데 보유 가치 있음
- **필요 테스트:** Unit
- **수정 권고:** "보유 연구 원장 가치 ≥ N"으로 이름·의미를 정정(소급 가능)

## D-11 [P1·CONFIRMED] recipes_200: "서로 다른 레시피"인데 근거 카운터는 제작 횟수

- **근거:** KnowledgeRecipeCraftCountMixin은 횟수 +1(상한 50)
- **최소 재현:** 동일 레시피 200회
- **예상 결과:** 의미 불일치
- **필요 테스트:** Unit
- **수정 권고:** 레시피 ID 고유 집합을 신규 추적(I)

## D-12 [P2·SUSPECTED] cave_dwellers의 엔티티 ID가 레지스트리 ID와 불일치 가능

- **근거:** 조건은 클래스명(small_cave_spider 등). CustomMobCatalog 문자열은 cave_bug, small_spider, swamp_spider 등
- **최소 재현:** —
- **예상 결과:** 등록 ID와 매칭 실패
- **필요 테스트:** Unit(ID 존재)
- **수정 권고:** 레지스트리 ID로 재작성

## D-13 [P2·SUSPECTED] 금고·엔드 도시·상자 "서로 다른 N개"의 고유성 키 미정의

- **근거:** A 조건 문구뿐
- **최소 재현:** —
- **예상 결과:** 중복 집계 또는 불가
- **필요 테스트:** Unit
- **수정 권고:** 금고=좌표(바닐라 VaultServerData는 금고별 수령자 기록), 엔드 도시=구조물 시작 청크

## D-14 [P2·SUSPECTED] dragon_rematch 판정 시점

- **근거:** `EnderDragonFight.hasPreviouslyKilledDragon()` 존재(확인). `setDragonKilled`가 플래그를 갱신하므로 처치 전에 읽어야 함
- **최소 재현:** 첫 드래곤
- **예상 결과:** 오판 가능
- **필요 테스트:** GameTest
- **수정 권고:** setDragonKilled HEAD에서 이전 값 캡처. 다중 참여 시 킬 크레딧만 인정

## D-15 [P1·SUSPECTED] MM 적대 몹 종류 수(25종 목표)가 미검증

- **근거:** CustomMobCatalog 문자열에 서식지(habitat) 항목이 섞여 있음. 실제 등록 엔티티 타입 수와 자연 스폰 여부 미확인(40종 생태 감사 미완)
- **최소 재현:** —
- **예상 결과:** 25종 미만이면 달성 불가
- **필요 테스트:** Unit(레지스트리 집계) + Live
- **수정 권고:** 등록·자연스폰 몹 목록 확정 후 목표 결정. 현재 VERIFY 유지

## D-16 [P2·CONFIRMED] alc.ingredients_all 대상 목록이 모호함

- **근거:** 실제 로스터는 10종(KnowledgeRuntime.refresh)
- **최소 재현:** —
- **예상 결과:** "전종"이 무엇인지 불명
- **필요 테스트:** Unit
- **수정 권고:** 10종 명시

## D-17 [P2·SUSPECTED] 강화 퀘스트의 소급·귀속

- **근거:** R은 현재 인벤토리의 ENHANCEMENT_LEVEL만 확인 가능. 거래받은 장비도 통과
- **최소 재현:** +20 장비 양도
- **예상 결과:** 받은 사람 완료
- **필요 테스트:** GameTest
- **수정 권고:** "강화 작업 성공(본인 수행)" 이벤트 기준으로 하고, 소급은 보유 장비 1회 스캔으로 허용할지 사용자 결정

## D-18 [P2·SUSPECTED] 단검 식별

- **근거:** 단검은 BasicWeaponItem 계열로 클래스 공유
- **최소 재현:** —
- **예상 결과:** 오판 가능
- **필요 테스트:** Unit
- **수정 권고:** 아이템 ID 태그(#moreminecrafts:daggers) 도입

## D-19 [P2·SUSPECTED] omen_raid: 습격 흉조 레벨 판정 API 미확인

- **근거:** —
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** GameTest
- **수정 권고:** Raid 레벨 API 확인 후 VERIFY

## D-20 [P1·CONFIRMED] 완료 토스트가 재접속 스냅샷에서 다시 뜰 위험

- **근거:** 승인안은 "토스트 정확히 1회". D 설계는 동기화 시 트리를 재생성함
- **최소 재현:** 재접속
- **예상 결과:** 완료된 퀘스트 토스트 재발생
- **필요 테스트:** Integration(클라이언트)
- **수정 권고:** 델타에서 complete 0→1 전이일 때만 토스트. 스냅샷 수신 시 토스트 금지

## D-21 [P1·CONFIRMED] 수령 pending의 해제 조건 누락

- **근거:** D §1.3은 결과 패킷 또는 claimed 델타 수신 시에만 해제
- **최소 재현:** 응답 유실 또는 화면 재생성
- **예상 결과:** 영구 대기
- **필요 테스트:** Integration
- **수정 권고:** 타임아웃(예: 5초), 화면 재오픈, 접속 재설정 시 pending 해제

## D-22 [P2·SUSPECTED] 키보드·컨트롤러로 수령할 경로 없음

- **근거:** 바닐라 AdvancementsScreen은 노드 포커스 탐색이 없음(INFERRED)
- **최소 재현:** —
- **예상 결과:** 마우스 전용
- **필요 테스트:** Live
- **수정 권고:** 접근성 결정: 마우스 전용 허용 또는 별도 키

## D-23 [P1·SUSPECTED] 수령 저장 원자성 주장이 미검증

- **근거:** D 문서는 "같은 플레이어 파일이라 함께 저장"이라고 주장. 저장 시점과 비정상 종료 거동은 미검증
- **최소 재현:** 수령 직후 강제 종료
- **예상 결과:** 중복 또는 손실 여부 미확인
- **필요 테스트:** GameTest/Integration(강제 종료 시뮬레이션)
- **수정 권고:** 검증 전 "보장"이라고 주장하지 말 것

## D-24 [P1·CONFIRMED] 현재 강화 코드는 XP를 차감한 뒤 아이템을 수정하며 롤백이 없음

- **근거:** `EnhancementOperation`: 54행 XpLevels.pay → 58행 equipment.set(...)
- **최소 재현:** 결과 적용 중 예외
- **예상 결과:** XP만 차감
- **필요 테스트:** GameTest(승인된 롤백 요구사항)
- **수정 권고:** authority대로 스냅샷·롤백 구현(구현 단계 과제)

## D-25 [P2·CONFIRMED] B 문서 §7 표 형식 깨짐

- **근거:** 79행에 D-E4 행이 이전 행 끝에 이어 붙음
- **최소 재현:** —
- **예상 결과:** 렌더링 깨짐
- **필요 테스트:** 정합성 스크립트
- **수정 권고:** 줄바꿈 정정(다음 문서 수정 작업에서)

## D-26 [P2·CONFIRMED] B 문서 본문 권장(B 시작)이 D 확정과 충돌

- **근거:** B §6, §7 D-E1
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** —
- **수정 권고:** 본문 정정 또는 이력 표기

## D-27 [P2·CONFIRMED] D 문서 §2가 "지급은 giveExperiencePoints 유지"를 제안해 authority(지급·차감 모두 정확 계산)와 충돌

- **근거:** D §2, D-X1
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** —
- **수정 권고:** 본문 정정

## D-28 [P2·CONFIRMED] C 문서 §5 XP가 B 기준이고 미승인 장비를 전제

- **근거:** C §5
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** —
- **수정 권고:** 유지하되 "미승인·미등록" 표기 강화

## D-29 [P2·CONFIRMED] authority 내부의 옛 문장이 인라인으로 대체 표시되지 않음

- **근거:** INDEX 1행 배너와 UI 문서 18·29·39행은 여전히 "XP 퀘스트 disabled/DEFERRED"(문서 끝에 amendment만 추가됨)
- **최소 재현:** —
- **예상 결과:** 독자가 오판할 위험
- **필요 테스트:** —
- **수정 권고:** 해당 행에 "superseded by 2026-10-17 amendment" 표기

## D-30 [P1·CONFIRMED] 선택기 카드 활성화에 필요한 아트·결정이 미해결

- **근거:** ProgressionSelectionScreen은 정적 이미지(approved_full_panel.png)에 비활성 카드를 그려 둠
- **최소 재현:** —
- **예상 결과:** 카드를 활성화해도 시각적으로 비활성
- **필요 테스트:** Live
- **수정 권고:** 새 패널 이미지 또는 오버레이 방식 결정

## D-31 [P2·CONFIRMED] FUTURE eqp.t3_special/t3_set이 미승인 C 문서를 참조

- **근거:** A 조건 "장비 확장안 C 참고". T3 LOCKED 로스터는 7종
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** —
- **수정 권고:** LOCKED 7종 기준으로 조건 재작성

## D-32 [P1·CONFIRMED] 기존 월드 업데이트 시 소급(R/S) 퀘스트가 한꺼번에 완료됨

- **근거:** 매트릭스의 R/S 행. 바닐라 통계·Knowledge 관측 키는 설치 이전 이력을 포함
- **최소 재현:** 오래된 월드에 업데이트
- **예상 결과:** 수만 XP를 즉시 수령 가능
- **필요 테스트:** GameTest(로그인 소급 평가)
- **수정 권고:** 의도 여부 사용자 결정: 소급 허용 / 설치 시점 기준선 / 소급분 상한

## D-33 [P2·SUSPECTED] 엔드게임에 보상이 쏠려 중반 병목 가능

- **근거:** D곡선: 엔드게임 비중 약 37%. 시간당 XP는 가정값
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** Live 플레이테스트
- **수정 권고:** 곡선 재검토(D-E4)

## D-34 [P2·CONFIRMED] EnchantmentTrees에 laser_arrow 노드가 있으나 인챈트 JSON이 없음(범위 밖 발견)

- **근거:** data/moreminecrafts/enchantment는 11개, 트리 문자열에는 laser_arrow 포함
- **최소 재현:** —
- **예상 결과:** —
- **필요 테스트:** —
- **수정 권고:** 범위 밖 보고만
