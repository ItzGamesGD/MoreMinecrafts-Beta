# ST-01 P7-R — 사용자 승인 기록 및 결정 경계 문서 (2026-11-08)

상태: **사용자 승인 반영 및 3B 조건부 검증 완료 (UPDATED WHAT RECORD)**  
기준 커밋: `06a262d70d9b5154d1ffccb1bbdfc4601b2b984f` (`origin/work/st01-p6-final-spec-20261107`)  
작업 브랜치: `work/st01-p7-east-primary-guardian-20261108`

---

## 1. 사용자 확정 결정 (APPROVED WHAT)

다음 결정은 사용자에 의해 공식 승인 및 채택되었으며, 설계의 불변 기준선으로 적용된다.

| 항목 | 승인된 결정 내용 | 좌표 및 형상 명세 | 상태 |
| --- | --- | --- | :---: |
| **D-I** | **기준 방향 = EAST** | • ST-01 피라미드 전체의 기준 방향을 EAST로 확정.<br>• WEST는 비교 및 정합성 검증용 거울(mirror) 모델로만 보존.<br>• P4-B 조건부 공간 설계 및 레이아웃-C 오버레이를 유지. | **APPROVED** (확정) |
| **P3-F6** | **PRIMARY 왕실 상자 = 후보 B 승인** | • 실제 점유 좌표: `(-21, -20, 33)`, `(-20, -20, 33)` (2블록)<br>• 기존 자리표시자에서 동쪽(+X)으로 확장.<br>• 상자 정면은 북쪽(-Z, 홀 입구 방향)을 정면으로 개폐.<br>• 두 블록은 하나의 논리적 PRIMARY 대형 보상 상자로 작동. | **APPROVED** (확정) |
| **D-E** | **Tomb Guardian = 후보 3B 조건부 채택** | • 스폰 후보 좌표: `(-21, -22, 26)`<br>• 바닥 제의용 광장 중앙점, 남쪽(상자/플레이어)을 응시.<br>• 공격 판정, 충돌, 실제 플레이 조건 정밀 검증 후 최종 승인 결정. | **CONDITIONALLY ADOPTED** (조건부 채택) |

> [!IMPORTANT]
> **구현 승인과의 엄격한 분리**  
> D-I 확정 및 PRIMARY 후보 B 승인, Guardian 후보 3B의 조건부 채택에도 불구하고, **Minecraft 구조물 NBT 생성, 월드젠 훅 추가, RoyalChestRuntime.register(...) 실제 연결은 아직 승인되지 않았다**.  
> 본 작업은 설계 및 기하, 판정 검증 단계이며, 실제 구현 코드는 일체 수정하거나 연결하지 않는다.

---

## 2. P7 원격 게시 복구 상태 (P7-R)

1. **로컬 작업 보존:**  
   - P7 커밋 `a260fcabdffaa6377b23435c29be98fdcfadb39e` 및 16개 신규 문서/도구 파일이 로컬 브랜치 `work/st01-p7-east-primary-guardian-20261108`에 온전히 보존되어 있음을 확인하였다.
2. **원격 푸시 실패 원인 진단:**  
   - 원본 저장소(`ItzGamesGD/MoreMinecrafts-Beta.git`) 푸시 시 `HTTP 403 Permission denied to ItzGamesGD` 오류가 발생함.
   - 이는 샌드박스 프록시(`agy-plugin-proxy`)의 GitHub App 토큰이 해당 저장소에 쓰기 권한이 부여되지 않았거나, 대화 세션이 프로젝트 리소스에 바인딩되지 않아 포크 권한이 제한되었기 때문임.
3. **복구 및 산출물 보존 대책:**  
   - 권한이 없는 환경에서도 작업을 손실 없이 이전할 수 있도록 **Git Bundle (`st01-p7-east-primary-guardian.bundle`)** 및 **Git Patch (`st01-p7-east-primary-guardian.patch`)**를 자동 생성하여 영구 보존함.
   - 사용자가 로컬 터미널에서 즉시 원본 저장소로 푸시하고 Draft PR을 생성할 수 있는 절차를 완비함.
