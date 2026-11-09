# ST-01 P10 — 실제 왕실 묘실 테스트 픽스처와 복셀 일치 검증 (산출물 1)

상태: **구현·검증 완료(픽스처/복셀/좌표/거울/음성 대조: PASS).** 구조물 기하는 변경하지 않았다. D-3(동·서 방향), Q15(비밀 묘실 사양), Q98(최종 깊이 상한)은 미확정으로 유지하며, 동·서를 둘 다 시험했을 뿐 어느 쪽도 채택하지 않았다.

## 1. 무엇을 만들었나

| 항목 | 내용 |
| --- | --- |
| 원본 | P1–P6의 **수정하지 않은** 복셀 엔진(`tools/st01-p1…p6`)이 만드는 P6 통합 월드(P2-F1 후보 C 오버레이 적용, P3 왕실 묘실, P4-B 측면 배치). 닫힘(CLOSED) 상태 |
| 내보내기 | `tools/st01-p10/export_fixture.py` → `src/gametest/resources/st01/p10_fixture_{E,W}.json` (동/서 각각 엔진이 독립 생성, 서쪽은 엔진의 거울 모델). 영역 X −41..31(서쪽 −31..41) × Y −31..−4 × Z −10..41 = 106,288칸, 고체 84,777칸, RLE 4,221구간 |
| 배치 | `RoyalTombFixture`(게임테스트): 모델 좌표 → 월드 좌표는 **순수 평행이동**(원점 오프셋)만 사용. 한 칸씩 쓰며 공기 칸도 명시적으로 지움. 배치 소요 약 1.0–1.3 s |
| 블록 팔레트 | 암석/벽 `stone`, 기둥 `sandstone`, 상인방 `cut_sandstone`, 오벨리스크 `chiseled_sandstone`, 제단 `smooth_sandstone`, 석관 `polished_andesite`, 상자 칸 `gold_block`, 석상·장식 기둥 `stone_bricks`, 화로 `nether_bricks`, 덮개 12칸 `stone_bricks`. 모델의 충돌 대용(proxy)과 같이 **상자·석상·화로도 전체 블록**이다(시각 충실도 아님, 충돌/통행 검증용) |
| 포함 요소 | 홀 내부 33×33, 수직 벽 8, 코벨 천장 8단(1:2), 기둥 10개(기단·몸통·주두) + 상인방 6, 중앙 오벨리스크(기단 5×5×2·몸통 3×3×9·끝 1×1×2), 제단+발판, 석관, 상자 2, 석상 3, 화로 4, 감실/벽감, 실제 입구 문(3×3, 벽 두께 3)과 그 앞 연결 통로, P1·P2 공간과 P4 비밀 구역, **덮개 12칸**, 덮개 둘레의 실제 바닥·계단 충돌 지형 |
| 추가하지 않은 것 | 평탄한 우회 경로, 임의의 통로 확장, 예외 블록. 테스트를 통과시키려는 기하 변경은 없다 |

## 2. 검증 (모두 두 겹: 설계 도구 + 실제 서버)

### 2.1 설계 도구 `tools/st01-p10/verify_fixture.py` (결과: `generated/p10_fixture_verification.json`, 16 PASS / 0 FAIL)

| 검사 | 내용 | 결과 |
| --- | --- | --- |
| V1 | 내보낸 닫힘/열림 월드의 다이제스트가 **승인된 P6 모델 JSON의 다이제스트와 동일** (동: 닫힘 `ad3fdcbff639b65e`, 열림 `68751fe73cb9539d` / 서: 닫힘 `847f340c8da83c40`, 열림 `bfe7c106e96413c3`) | PASS |
| V2 | 디코딩한 픽스처 영역 = 새로 만든 엔진 월드(칸 단위), 영역이 P1/P2/P4 공동(空洞) 7,247칸과 홀 예약 영역을 모두 덮음 | PASS |
| V3 | 서쪽 = 동쪽의 정확한 X 거울(고체 마스크·요소 종류 층·영역) | PASS |
| V4a | P3 요소 상자(기둥 10×{기단,몸통,주두}, 상인방 6, 오벨리스크 3, 제단 2, 석관, 상자 2, 석상 3, 화로 4, 감실 기둥 8)가 모두 고체이고 요소 종류가 맞음 | PASS |
| V4b | 홀 내부 33×33 × 높이 8 안에 목록의 요소 외 고체 0칸 | PASS |
| V4c | 코벨 8단: 각 단의 반폭 = 16−2j까지 열린 공기(목록 요소 제외), 바깥 한 칸은 고체 어깨 | PASS |
| V5 | 덮개 = 상자 기준 dx 12..15(서쪽은 반전), dy −3, dz 0..2 의 12칸; 닫힘에서 고체, 열림에서 공기; 닫힘↔열림 차이는 정확히 그 12칸 | PASS |
| V6 | 변이 대조 5종(고체→공기 1칸, 공기→고체 1칸, X로 한 칸 이동, 서쪽을 동쪽으로 가져옴, 거울 1칸 깨뜨림) 모두 검출 | PASS |

실행: `python3 -I tools/st01-p10/verify_fixture.py src/gametest/resources/st01 <out.json>`

### 2.2 실제 서버 게임테스트 (`RoyalTombGolemGameTests`, `RoyalTombSafetyGameTests`)

| 테스트 | 근거 | 결과 |
| --- | --- | --- |
| `realHallFixtureMatchesTheApprovedVoxelModelEastAndWest` | 배치된 월드를 칸 단위로 읽어 모델과 비교: 불일치 E 0 / W 0, 고체 84,777/84,777 (동·서 동수); 모델의 동·서 거울 불일치 0(고체)/0(요소 종류); **배치된 두 월드의 거울** 불일치 0 | GAMETEST PASS |
| `coordinateTransformAnchorCoverStopAndSpawnCellsAreConsistent` | `EntranceLayout.coverCells`(P9 코드, 앵커 상대) = 모델 덮개 12칸(평행이동 후) 동·서 모두 일치; 닫힘에서 12칸 고체; 덮개 둘레 바닥 고리 고체; 앵커 = `CHEST_PRIMARY`; 정지 칸·골렘 생성 칸 서 있기 가능 | GAMETEST PASS |
| `negativeControlsMutatedFixturesAreDetected` | 벽 1칸 제거 → 불일치 1 검출, 장애물 1칸 추가 → 2 검출, 잘못된 방향의 덮개 칸은 모델과 다름, 비거울 동·서 모델 46,261칸 차이 | GAMETEST PASS |

## 3. 좌표 변환 규약

```
world = model + origin   (origin은 테스트가 정하는 먼 위치. 회전·반전·스케일 없음)
anchor = origin + (hallCx, hallFloor + 2, hallCz + 12)      // 동 (−21,−20,33), 서 (+21,−20,33)
cover  = anchor + (±12..15, −3, 0..2)                          // 서쪽은 X 반전
stop   = anchor + (±10, −2, +3)   (EntranceLayout.stopPoint, PROPOSED)
spawn  = anchor + (0, −2, −6)     (EntranceLayout.golemSpawn, PROPOSED)
```
`hallCx`는 각 쪽 모델이 가진 값(동 −21, 서 +21)이다. 이 값에 부호를 한 번 더 곱하면 서쪽 앵커가 틀어지는데, 작성 중 실제로 이 실수를 서쪽 덮개 불일치로 잡아냈다(수정 후 PASS).

## 4. 한계 (정직한 범위)

- 픽스처 영역 밖의 자연 암석은 내보내지 않았다(바깥은 테스트 세계의 허공). 구조물 외곽 지형·피라미드 껍질·월드젠은 O-14로 미구현이며 이번 범위가 아니다.
- 상자/석상/화로는 전체 블록으로 대체했다(모델의 충돌 대용과 동일). 실제 상자 블록의 낮은 충돌 높이는 검증하지 않았다.
- 조명·소리·파티클·텍스처 등 시각 요소는 대상이 아니다. **LIVE VALIDATION PENDING.**
