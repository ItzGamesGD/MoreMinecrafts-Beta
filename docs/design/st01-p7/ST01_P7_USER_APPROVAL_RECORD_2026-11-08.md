# ST-01 P7-S — 사용자 승인 기록 및 결정 경계 문서 (2026-11-08)

상태: **사용자 승인 반영 완료 (D-I=EAST, PRIMARY=B, GUARDIAN=3B 확정) 및 P8 구현 준비 진입**  
기준 커밋: `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f` (`origin/work/st01-p6-final-spec-20261107`)  
작업 브랜치: `work/st01-p7-east-primary-guardian-20261108`

---

## 1. 사용자 확정 결정 (APPROVED WHAT)

다음 세 가지 항목은 사용자에 의해 공식 승인 및 확정(USER APPROVED)되었으며, 후속 P8 구현의 불변 기준선으로 적용된다.

| 항목 | 승인된 결정 내용 | 좌표 및 형상 명세 | 상태 |
| --- | --- | --- | :---: |
| **D-I** | **기준 방향 = EAST** | • ST-01 피라미드 전체의 기준 방향을 EAST로 확정.<br>• WEST는 비교 및 정합성 검증용 거울(mirror) 모델로만 보존.<br>• P4-B 조건부 공간 설계 및 레이아웃-C 오버레이를 유지. | **USER APPROVED** (확정) |
| **P3-F6** | **PRIMARY 왕실 상자 = 후보 B 승인** | • 실제 점유 좌표: `(-21, -20, 33)`, `(-20, -20, 33)` (2블록)<br>• 기존 자리표시자에서 동쪽(+X)으로 확장.<br>• 상자 정면은 북쪽(-Z, 홀 입구 방향)을 정면으로 개폐.<br>• 두 블록은 하나의 논리적 PRIMARY 대형 보상 상자로 작동. | **USER APPROVED** (확정) |
| **D-E** | **Tomb Guardian = 후보 3B 승인** | • 스폰 후보 좌표: `(-21, -22, 26)`<br>• 바닥 제의용 광장 중앙점, 남쪽(상자/플레이어 방향, Yaw 0.0)을 응시.<br>• 단상 2m 고도차 충격파 면역(feetAboveFloor >= 0.6m) 및 6.55s 초기 대응창 확보. | **USER APPROVED** (확정) |

> [!IMPORTANT]
> **설계 확정과 구현·게임플레이 검증의 엄격한 분리**  
> 1. D-I, PRIMARY B, Guardian 3B의 사용자 승인(WHAT) 완료에도 불구하고, **Minecraft 구조물 NBT 생성, 월드젠 훅 추가, RoyalChestRuntime.register(...) 실제 연결은 아직 구현하지 않는다**.  
> 2. 실제 서버/클라이언트 환경에서의 인게임 전투와 연출은 여전히 **`LIVE_SERVER_UNVERIFIED`** 상태이다. (설계 형상·기하·규칙 에뮬레이션 검증 완료 != 인게임 실기 검증 완료).

---

## 2. P7 원격 보존 복구 상태 (Phase A)

1. **로컬 작업 보존 및 커밋 이력:**  
   - P7 커밋 `a260fcabdffaa6377b23435c29be98fdcfadb39e`
   - P7-R 커밋 `86d50ec17060864695188776e8db4d11edb097a7`
   - 위 커밋들이 로컬 브랜치 `work/st01-p7-east-primary-guardian-20261108`에 온전히 보존되어 있음.
2. **원격 푸시 실패 원인 진단:**  
   - 원본 저장소(`ItzGamesGD/MoreMinecrafts-Beta.git`) 푸시 시 `HTTP 403 Permission denied to ItzGamesGD` 발생.
   - 샌드박스 프록시(`agy-plugin-proxy`)의 인증 토큰이 원본 저장소 쓰기 권한이 없거나 미바인딩 상태임.
3. **복구 및 산출물 보존 대책:**  
   - 권한이 없는 환경에서도 작업을 손실 없이 이전할 수 있도록 **Git Bundle (`st01-p7-r-guardian-3b.bundle`)** 및 **Git Patch (`st01-p7-r-guardian-3b.patch`)**를 영구 생성·검증함.
   - 사용자가 권한 있는 환경에서 즉시 원격 게시할 수 있는 단일 명령어 이전 절차를 수립함.

---

## 3. 미승인 항목 (NON-APPROVED / OUT OF SCOPE)

다음 항목들은 이번 단계에서 자동 승인되지 않으며, 후속 P8 구현 단계에서 별도 계획 및 승인을 거친다.
- ST-01 전체 구조물 NBT 바이너리 구현
- 월드젠 설정 (바이옴 태그, 스폰 빈도, 높이 분포, 지형 적응)
- 구조물 인스턴스별 고유 ID 생성 경로
- 런타임 연동 (`RoyalChestRuntime.register` 호출)
- 기존 보상 드랍 확률 및 Guardian 보스 AI 전투 규칙 변경
