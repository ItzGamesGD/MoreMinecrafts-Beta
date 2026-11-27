# 재현 — WIP

저장소 루트에서:

```sh
python3 -I docs/planning/sky_tower_geometry_03c/p6/reproduce_width.py
python3 -I docs/planning/sky_tower_geometry_03c/p6/portal_experiment.py
python3 -I docs/planning/sky_tower_geometry_03c/p6/pillar_experiment.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/test_p5.py
```

마지막은 P5 회귀 8건이며 P6에서 요구한 9건의 새 음성 대조로 간주하지 않는다. `portal_experiment.py`는 P5를 읽고 P6에만 결과를 쓴다. 생성기와 독립인 면 검사는 P5의 좌표와 비교하며, G1의 도달/경계 등은 기존 P5 검사기를 재사용한다. C2와 실제 게임은 미실행.
