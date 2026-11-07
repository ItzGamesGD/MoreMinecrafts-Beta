# FULL-BUILD-01 진행 체크포인트

기준 c43b480, 신규 feature/sky-tower-full-build-01. Phase A–C 전체 건축 후보 구현 및 NBT 생성. 높이80, 전투층4, 기단 나선 진입·외부 계단·발코니8·내부 제단/스포너16·보상 금고8·성소12아치/유물 구현. DESIGN APPROVED 아님.

정적 전체13목표 도달, 보행면 가장자리0. 폭 표본 최소는 기단3.814 / 중간4.539~5.000 / 성소접속3.929. 공칭 디딤판 띠6과 실제 충돌 폭을 구분한다. 경계는 기존 D45보다 증가(실측은 model/verify 참조).

서버/클라이언트는 /tmp/fullbuild-mc에 공식26.3을 설치, Xvfb :88 / lavapipe로 실제 클라이언트 접속 확인. 첫 스포너16상태 전환과 이전 후보 잔여1776블록을 구분·진단했다. server_check.py는 배치 전 구역을 비우도록 수정. 정리된 월드에서 보행 시험 재진행 중. 스크린샷 세트·회귀·최종보고·패키징 미완료.

재개 명령: python3 -I docs/planning/sky_tower_full_build_01/build.py; verify.py; safety.py; width.py (동일 폴더의 각 파일 실행). 서버 준비 server_prepare.py /tmp/fullbuild-mc; 서버 reload 후 server_check.py /tmp/fullbuild-mc; 실제 클라이언트 접속 상태에서 live_walk.py. /tmp 환경은 재생성 필요할 수 있다. 이전 P1–P6/권위 문서/월드젠 불변.
