# P5 재현과 체크포인트

Python 3 표준 라이브러리만 필요하다. 저장소 루트에서 실행한다. P1–P4 출력 디렉터리에 쓰지 않는다.

```sh
python3 -I docs/planning/sky_tower_geometry_03c/p3/test_p3.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_generate.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_reproduce.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_verify.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_width.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_explore.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_render.py
python3 docs/planning/sky_tower_geometry_03c/p5/test_width.py
python3 docs/planning/sky_tower_geometry_03c/p5/test_review.py
python3 -I docs/planning/sky_tower_geometry_03c/p5/test_p5.py
```

`p5_verify.py`의 종료코드0은 분석 실행 성공이다. 구조의 합격을 뜻하지 않는다. `verify_*.json`에서 height/fall_edges 실패, boundary/arches/balconies/reach 결과를 읽는다. `p5_width.py` 역시 분석을 출력하며 폭<4를 예외로 감추지 않는다. `test_p5.py`의 성공은 의도한 결함을 검출했다는 뜻이다.

산출물:

- `data/blocks_C1/C2.csv`: 부재별 실제 점유 **claims**. 동일 복셀 접합도 행을 보존한다.
- `data/objects_*.json`: 원본 K2 메타데이터. C2 객체 cells/top은 원본 C1이며 `transform.dy=-1`을 적용해야 한다. CSV는 이미 변환됐다.
- `data/layers_*.json`: B25-T3 층 귀속. 기단은 C2에서도 y0 시작.
- `data/reservations_*.json`: 별도 탐색 상자. 블록으로 세지 않는다.
- `data/width_*.csv`, `width_summary_*.json`: 각도 표본의 연속 반경 구간. 전역 최소 인증이 아니다.
- `data/verify_*.json`, `paths_*.json`: 독립 통합 검사와 정적 보행 증거.
- `data/balcony_*`: 원본 접속 분석과 56개 국소 이동 비교. 전 조건 통과 대체 모델 없음.
- `data/svg/*horizontal_y*.svg`: 각 규약 모든 y0..79 단면(160장). 빈 상층도 그대로 표시한다.
- `data/svg/*XY*`, `*ZY*`: 중심/관문/발코니 근처 수직 단면16장 및 예약 투영2장. 실제 렌더/스크린샷 아님.
- `data/source_hashes.json`, `reproduction.json`, `*_tests.txt`: 불변 기준과 재현 결과.

## 실제 서버 재현

공식 version manifest의26.3 server.jar SHA1 `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c`, Java25를 사용한다. 서버·월드·jar·임시 NBT는 저장소 밖 임시 디렉터리에 둔다. 예시 `/tmp/p5-minecraft`.

- `server.properties`: localhost 바인딩, game port25585, RCON25586, 로컬 비밀 암호, flat 월드, generate-structures=false. 이 암호를 커밋하거나 로그에 출력하지 않는다.
- 테스트용 서버의 약관 설정 후 기동해 빈 월드를 만든다.
- `python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_server.py prepare /tmp/p5-minecraft`
- 서버를 재시작하거나 `reload`하고 데이터팩 로딩 완료를 기다린다. 이 스크립트는 검증 중 자동 reload하지 않는다.
- `python3 -I docs/planning/sky_tower_geometry_03c/p5/p5_server.py check /tmp/p5-minecraft`

`prepare`는 26.3용 `id/properties` 팔레트, DataVersion5023의 임시 구조 NBT와 10,000명령 이하로 나눈 검증 함수를 만든다. 실제 블록과 미지정 공기 좌표 모두 검사한다. `check`는 strict 배치 후 scoreboard 오류 수를 읽는다. `server_manifest.json`에 임시 템플릿 SHA256·원점·범위·대조 수가 있다. 기존 NBT·월드젠에 연결하지 않는다. `/tmp` 월드는 보존을 보장하지 않으므로 스크립트로 다시 만들 수 있어야 한다.

## 재개 지점

- 완료된 증거를 재수행하지 않고 `P5_REPORT.md`와 각 JSON 결과부터 읽는다.
- 다음 좌표 작업: 폭4를 실제 면 기준으로 확보하는 수정 탐색, 초기/종료 포털의12개 보호 면, 기단→F1, 성소12아치/y79, 장식 예약 교차를 다룬다.
- U1–U7/C1/C2 해석을 사용자 승인 없이 확정하지 않는다. P5 표본 최소를 전역 최소로 승격하지 않는다.
- 전체 클라이언트 동선·8발코니·모서리 시험은 NOT RUN이며 별도 실행이 필요하다.
