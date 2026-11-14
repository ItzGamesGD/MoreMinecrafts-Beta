# ST-01 O-14 Phase 3.1: 재현성 및 검증 파이프라인 보고서
(Phase 3.1 Reproducibility & Pipeline Verification Report)

- **문서 버전:** 1.0
- **작성일:** 2026-11-09
- **검증 환경:** GitHub Actions (`ubuntu-latest`, Temurin JDK 25, Minecraft 26.3, Fabric Loader)
- **작업 브랜치:** `work/st01-o14-phase3-1-validation-20261109`
- **검증 커밋 SHA:** `44ca14d6934c11b11ca0fca876a3b2bfa04a259c`
- **검증 실행 PR:** [Draft PR #252](https://github.com/ItzGamesGD/MoreMinecrafts-Beta/pull/252)
- **CI 실행 ID:** `42720234382` (상태: `SUCCESS`, 실행 시간: 3m 40s)

---

## 1. 재현성 보증 (Reproducibility Guarantee)

### 1.1 시드 1002 결정론적 재현 (Deterministic Replay Verification)
- **검증 대상:** 실제 사막 후보가 풍부하게 존재하는 World Seed `1002`
- **실행 절차:**
  1. 클린 런 디렉터리(`build/st01-phase3-calibration/run`)에서 `level-seed=1002`로 첫 번째 헤드리스 서버 구동 $\to$ `st01_phase3_seed1002.json` 생성.
  2. 디렉터리 완전 초기화 후 동일 시드로 두 번째 독립 헤드리스 서버 구동 $\to$ `replay_phase3_seed1002.json` 생성.
  3. 두 파일의 주요 메트릭 및 후보 리스트에 대해 비트 단위 일치 검증:
     - `world_seed`: 1002 == 1002 (일치)
     - `stage2_woodland_mansion_baseline.placement_metrics`: 100% 완전 일치
     - `stage3_st01_placement_simulation`: 6개 설정 전수 100% 완전 일치
- **판정:** **`BIT_FOR_BIT_MATCH_CONFIRMED` (재현성 100% 통과)**

---

## 2. 5개 시드 독립 실행 파이프라인 (Multi-Seed Pipeline)

각 시드는 독립된 `server.properties` (`level-seed=$seed`)와 격리된 월드 인스턴스를 통해 순차 실행되었습니다:

| 시드 | 서버 포트 | 탐색 반경 | 대저택 발견 수 | ST-01 100% 사막 후보 (32/8) | ST-01 탐색적 적격 (32/8) | 로그 파일 |
|---|---|---|---|---|---|---|
| **Seed 1001** | 임의 지정 | 10,240 | 3개 | 0개 (사막 결손) | 0개 | `st01-phase3-seed1001.log` |
| **Seed 1002** | 임의 지정 | 10,240 | 4개 | 16개 | 1개 | `st01-phase3-seed1002.log` |
| **Seed 1003** | 임의 지정 | 10,240 | 2개 | 23개 | 4개 | `st01-phase3-seed1003.log` |
| **Seed 1004** | 임의 지정 | 10,240 | 4개 | 17개 | 2개 | `st01-phase3-seed1004.log` |
| **Seed 1005** | 임의 지정 | 10,240 | 2개 | 0개 (사막 결손) | 0개 | `st01-phase3-seed1005.log` |
| **Replay 1002**| 임의 지정 | 10,240 | 4개 | 16개 | 1개 | `replay-phase3-seed1002.log`|

---

## 3. 데이터 산출물 및 인벤토리 (Data Artifacts Inventory)

| 파일 경로 | 내용 및 용도 |
|---|---|
| [`docs/design/st01-o14/phase3_1_raw_measurements.json`](file:///home/antigravity/ItzGamesGD/MoreMinecrafts-Beta/docs/design/st01-o14/phase3_1_raw_measurements.json) | 5개 시드 전수 실측 원시 데이터셋 및 재현성 검증 레코드 |
| [`docs/design/st01-o14/phase3_1_statistical_summary.json`](file:///home/antigravity/ItzGamesGD/MoreMinecrafts-Beta/docs/design/st01-o14/phase3_1_statistical_summary.json) | 풀링 밀도, 신뢰구간, 평균/최소 거리, 대저택 대비 희소성비 통계 요약 |
| [`docs/design/st01-o14/phase3_1_configuration_comparison.csv`](file:///home/antigravity/ItzGamesGD/MoreMinecrafts-Beta/docs/design/st01-o14/phase3_1_configuration_comparison.csv) | 6개 Spacing/Separation 설정 간의 실측 비교 테이블 |
| [`docs/design/st01-o14/phase3_1_independent_audit_result.json`](file:///home/antigravity/ItzGamesGD/MoreMinecrafts-Beta/docs/design/st01-o14/phase3_1_independent_audit_result.json) | Stage D 통계적 분리 가능성 감사 결과 |
| [`docs/design/st01-o14/phase3_discrepancies_audit.json`](file:///home/antigravity/ItzGamesGD/MoreMinecrafts-Beta/docs/design/st01-o14/phase3_discrepancies_audit.json) | Stage A Phase 3 결함(A1~A4) 감사 결과 |

---

## 4. 로컬 재현 가이드 (Local Reproduction Guide)

JDK 25 및 Python 3 환경에서 다음 명령으로 동일 파이프라인을 100% 재현할 수 있습니다:

```bash
# 1. 단위 테스트 및 감사 스크립트 실행
python3 tools/st01-o14/audit_phase3_discrepancies.py

# 2. 다중 시드 집계 실행
python3 tools/st01-o14/aggregate_phase3_1_multiseed.py docs/design/st01-o14 \
  <path_to_replay_1002.json> \
  <path_to_seed_1001.json> <path_to_seed_1002.json> <path_to_seed_1003.json> \
  <path_to_seed_1004.json> <path_to_seed_1005.json>

# 3. Stage D 독립 통계 감사 실행
python3 tools/st01-o14/audit_phase3_1.py \
  docs/design/st01-o14/phase3_1_raw_measurements.json \
  docs/design/st01-o14/phase3_1_statistical_summary.json \
  docs/design/st01-o14/phase3_1_independent_audit_result.json
```
