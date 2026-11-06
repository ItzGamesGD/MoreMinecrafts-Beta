#!/usr/bin/env python3
"""Reproduce first, then bounded search, independently audit, render and report."""
from collections import Counter
import argparse
import hashlib
import importlib.util
import itertools
import json
from pathlib import Path
import subprocess
import sys
import tempfile

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]


def load(name):
    spec=importlib.util.spec_from_file_location('g03b_'+name,HERE/(name+'.py'))
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m


def save(path,data):path.write_text(json.dumps(data,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')


def reproduce(out):
    manifest=json.loads((HERE/'source_manifest.json').read_text())
    for entry in manifest['files']:
        data=(ROOT/entry['path']).read_bytes()
        actual=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
        if actual!=entry['sha']:raise ValueError('BASE MISMATCH '+entry['path'])
    source=HERE.parent/'sky_tower_geometry_03'
    with tempfile.TemporaryDirectory(prefix='g03b-reproduce-') as target:
        subprocess.run([sys.executable,'-I',str(source/'geometry03_model.py'),'--out',target],check=True,stdout=subprocess.DEVNULL)
        files=sorted(p.name for p in (source/'data').iterdir() if p.is_file())
        for name in files:
            if (source/'data'/name).read_bytes()!=(Path(target)/name).read_bytes():
                raise ValueError('FAILURE NOT REPRODUCED '+name)
    report=json.loads((source/'data/results.json').read_text());cases={}
    for conv in ('C1','C2'):
        r=report[conv];checks={c['id']:c for c in r['checks']}
        assert r['navigation']['floors']['reached']==[True,False,False,False]
        assert r['bare_stair_control']['reached']==[True]*4
        assert checks['STAIR_PILLAR']['evidence']['obstruction_voxels']
        assert checks['ARCH_CANOPY_CLEARANCE']['evidence']['minimum']==0
        assert all(b['blocked_air'] for b in checks['BALCONY_CLEAR_7X3']['evidence'])
        under=checks['ARCH_CANOPY_CLEARANCE']['evidence']['under2']
        cases[conv]={'floor_access':r['navigation']['floors']['reached'],
          'bare_access':r['bare_stair_control']['reached'],
          'pillar_first':checks['STAIR_PILLAR']['evidence']['obstruction_voxels'][0],
          'pillar_ids':['Q43','Q98'],
          'overhead_first':under[0],'overhead_by_group':dict(Counter(g for p in under for g in p['obstruction_groups'])),
          'overhead_ids':['Q86','Q89','Q108'],'balcony_first':checks['BALCONY_CLEAR_7X3']['evidence'][0],
          'balcony_ids':['Q74','Q77','Q80'],'original_first_section_failure':checks['FULL_WIDTH_4']['evidence']['first_failed']}
    result={'status':'PASS','head':manifest['head'],'source_blobs_checked':len(manifest['files']),
            'artifacts_byte_identical':len(files),'cases':cases,
            'condition':'Frozen 0.6x1.8 AABB, quarter-block BFS, no jump, 4-column checks; not in-game acceptance.'}
    save(out/'reproduction.json',result);return result


def legacy(search):
    original=search.old;_,best=original.candidate_search();m=original.build('C1',original.source_inputs()[0],best)
    groups={}
    for name,cells in m['groups'].items():
        group='stairs' if name in ('stairs','bridge') else 'arcade_arch' if name=='arch' else name
        groups.setdefault(group,{})
        groups[group].update({p:m['walk_states'][p] if name in ('stairs','bridge') else ('full','') for p in cells})
    floor_columns={}
    for x,y,z in m['walk_states']:floor_columns.setdefault((x,z),[]).append(y)
    frames=[]
    for i,s in enumerate(m['sections']):
        r=m['records'][i]
        pad=[]
        if r['turn']:
            for x,z in original.pad(r['xz']):
                pad.append((x,min(floor_columns[x,z],key=lambda y:abs(y-r['floor_y'])),z))
        frames.append({'index':i,'lap':s['lap'],'station':s['station'],'direction':s['direction'],
                       'cells':s['floor_cells'],'turn_pad':pad})
    return {'params':{'legacy':True},'groups':groups,'baseline':m['baseline'],'void':m['void'],'frames':frames,
            'records':m['records'],'posts':[dict(p,floor=p['floor_y'],xz=p['center_xz']) for p in m['pillars']],
            'arches':[dict(a,id='ORIGINAL-'+str(i),spring=a['spring_y'],apex=a['apex_y'],raster_mode='original_chord') for i,a in enumerate(m['arches'])],
            'balconies':[dict(b,clear=b['clear7x3'],floor=b['floor_y']) for b in m['balconies']],
            'objects':[],'gateways':m['reservations'],'domain_outside_disc45':[]}


def packed_nav(result):
    navigation=[]
    for stage in result['stages']:
        nav=stage['navigation']
        if not nav:continue
        paths=nav.pop('witness_paths')
        packed=[]
        for path in paths:
            runs=[]
            for a,b in zip(path,path[1:]):
                d=tuple(b[i]-a[i] for i in range(3))
                if runs and tuple(runs[-1][:3])==d:runs[-1][3]+=1
                else:runs.append([*d,1])
            # Verify lossless coordinates before discarding duplicated path prefixes.
            rebuilt=[path[0]] if path else []
            for dx,dy,dz,n in runs:
                for _ in range(n):
                    x,y,z=rebuilt[-1];rebuilt.append([x+dx,y+dy,z+dz])
            assert rebuilt==path
            packed.append({'start':path[0] if path else None,'runs':runs,'points':len(path)})
        navigation.append({'stage':stage['stage'],'paths':packed})
    return {'format':'start XYZ then repeat each (dx,dy,dz,count); exact .25/.5-grid coordinates','stages':navigation}


def summary(result):
    stages=result['stages'];last=stages[-1]
    return {'status':last['status'],'stages':[{'stage':s['stage'],'status':s['status'],
        'width_status':s['width']['status'],'first_width_failure':s['width']['first_failure'],
        'first_disconnected_target':s['first_disconnected_target'],
        'floor_access':s['navigation']['reached'][:4] if s['navigation'] else None,
        'balcony_access':s['navigation']['reached'][4:] if s['navigation'] else None,
        'minimum_overhead_clearance':s['width']['minimum_overhead_clearance'],
        'first_bad_balcony':next((b for b in s['balconies'] if not b['pass']),None)} for s in stages]}


def main():
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--out',type=Path,default=HERE/'data')
    args=ap.parse_args();out=args.out;out.mkdir(parents=True,exist_ok=True)
    print('Frozen failure reproduction',flush=True);rep=reproduce(out)
    search,verify=load('search'),load('verify')
    table=[];models={}
    with tempfile.TemporaryDirectory(prefix='g03b-search-') as temp:
        for n,(domain,b,phase,shift,lift,canopy) in enumerate(itertools.product(
                ('disc45','axis45'),(11,12),(0,3,5),(1,-1),(2,3),(0,-1))):
            # 96 hypotheses; candidate variables are not design tolerance approvals.
            model=search.build(17,b,phase,domain,shift,lift,canopy)
            p=Path(temp)/str(n);search.export(model,p)
            result=verify.audit(p,'C1',False);last=result['stages'][-1]
            failures=(len(last['width']['failed_frames'])+len(last['width']['failed_transitions'])+
                      len(last['width']['failed_turn_pads'])+sum(not q['pass'] for q in last['balconies'])+
                      sum(len(last[k]) for k in ('overflow45','overflow51','overflow80')))
            joint_fail=sum(not all(v for k,v in j.items() if k.startswith('touch')) for j in result['joints'])
            spaces=result['pillar_spacing'];deviation=max(abs(p['projected_station_difference']-7) for p in spaces)
            row={'id':n,'parameters':model['params'],'static_failures':failures,'joint_failures':joint_fail,
                 'outside_original_disc45_columns':len(model['domain_outside_disc45']),
                 'spacing_xz_min':min(p['actual_xz_distance'] for p in spaces),'spacing_xz_max':max(p['actual_xz_distance'] for p in spaces),
                 'projected_spacing_min':min(p['projected_station_difference'] for p in spaces),
                 'projected_spacing_max':max(p['projected_station_difference'] for p in spaces),
                 'stages_static':[s['width']['status'] for s in result['stages']],
                 'BFS':'NOT RUN during static screening','status':'CANDIDATE FAILURE' if failures or joint_fail else 'UNVERIFIED'}
            row['rank']=[failures+joint_fail,len(model['domain_outside_disc45']),deviation,n]
            table.append(row)
            if n%12==11:print('Static candidates',n+1,flush=True)
        best=min(table,key=lambda r:r['rank']);strict=min((r for r in table if r['parameters']['domain']=='disc45'),key=lambda r:r['rank'])
        save(out/'candidate_table.json',{'candidates':table,'best':best,'best_disc45_control':strict,
            'note':'C1 screened statically; selected C1/C2 independently tested. Approx-7 tolerance remains undefined.'})
    p=dict(best['parameters']);p['domain_name']=p.pop('domain');model=search.build(**p)
    oldmodel=legacy(search)
    results={};legacy_results={}
    for label,m in (('original',oldmodel),('best',model)):
        folder=out/label
        for conv in ('C1','C2'):
            rotation=0 if label=='original' else 1 # explicit start/entry rotation; base remains D4 invariant
            search.export(m,folder,conv,rotation)
            print(label,conv,'six independent integration stages',flush=True)
            audited=verify.audit(folder,conv,True)
            if label=='original':
                assert audited['stages'][-1]['navigation']['reached'][:4]==[True,False,False,False]
            save(folder/f'navigation_{conv}.json',packed_nav(audited))
            save(folder/f'audit_{conv}.json',audited)
            (legacy_results if label=='original' else results)[conv]=summary(audited)
    save(out/'results.json',{'base_head':rep['head'],'reproduction':'PASS','best_candidate':best,
        'best':results,'original':legacy_results,'overall':'UNVERIFIED','CONFLICT':False,
        'geometry04':{'coordinate_refinement_ready':True,'final_implementation_ready':False,
                     'conditions':['original circle reservation interpretation','Q85 spacing tolerance','tier/gateway details','runtime collision acceptance']}})
    load('render').render(out)
    report(out,rep,table,best,strict,results,legacy_results)
    print('GEOMETRY-03B finished: overall UNVERIFIED; detailed numerical status in results.json',flush=True)


def report(out,rep,table,best,strict,results,legacy_results):
    lines=['# SKY-TOWER-GEOMETRY-03B — 통합 배치 재탐색',
      '',f'기준 커밋 `{rep["head"]}`. 기존 브랜치 `claude/sky-tower-geometry-02-coords-hgbfo7` 원격 HEAD 일치 확인 후 수행했다.',
      '**종합: UNVERIFIED. 수치 기하의 통과와 설계 전체 승인·인게임 검증을 구별한다.**',
      '기존 GEOMETRY-02B/03 파일과 Q01–Q118은 변경하지 않는다. B25-T3, 바닥판 1블록, C1/C2 비교를 유지한다.',
      '', '## 재현·실행 절차', '',
      f'원본 Git blob {rep["source_blobs_checked"]}개를 대조하고 기존 자동 산출물 {rep["artifacts_byte_identical"]}개를 바이트 단위로 재현했다. 불일치 시 새 후보 생성 전에 중단한다.',
      '① 기존 실패 재현 → ② 96개 정적 후보 비교 → ③ 최선 후보와 원본을 각각 C1/C2·6단계로 독립 검사 → ④ 좌표 기반 실패 도면·보고서 생성 → ⑤ 재생성 대조 및 브랜치 커밋·푸시.',
      '검증기는 search.py를 import하지 않고 내보낸 CSV와 실제 블록 충돌 형상을 읽는다. 원격은 GitHub 연결 도구로 확인·갱신하며 로컬 검증 스냅샷의 원본 해시를 고정한다.',
      '', '```sh','python3 -I test_geometry03b.py','python3 -I run.py --out data',
      'python3 -I run.py --out /tmp/sky-tower-g03b-recheck','diff -qr data /tmp/sky-tower-g03b-recheck',
      '# 별도 검증기만 실행','python3 -I verify.py data/best --convention C1','```',
      '', 'run.py 종료 0은 검증 실행 성공이며 전체 설계 PASS를 뜻하지 않는다. verify.py의 단독 실행은 audit 파일만 갱신한다.',
      '', '## 기존 실패 재현', '',
      '| 검사 | C1 근거 | 관련 설계/검사 조건 |','|---|---|---|',
      '| F2 이후 단절 | F2/F3/F4/성소 `[true,false,false,false]`; 계단 대조는 전부 true | Q25/Q32/Q43, 폭0.6·높이1.8 AABB, 점프 없음 |',
      f'| 기둥의 폭 침범 | `{rep["cases"]["C1"]["pillar_first"]}` 외 전체 좌표 reproduction.json | Q43/Q98, 보행 열의 머리 공간 점유 |',
      f'| 아치·차양 머리 공간 | `{rep["cases"]["C1"]["overhead_first"]}` | Q86/Q89/Q108, 기존 최소 여유 0 |',
      f'| 발코니 7×3 | 첫 발코니 `{rep["cases"]["C1"]["balcony_first"]["blocked_air"][0]}` 외 18개 공기 복셀을 기둥이 차지 | Q74/Q77/Q80, 21개 바닥 열의 사용 가능 공간 |',
      f'원본의 머리 공간 실패 원인별 수는 `{rep["cases"]["C1"]["overhead_by_group"]}`이다. 없는 부재 원인을 추정해서 추가하지 않았다.',
      'C2의 실패는 y가 1블록 낮고 접근 결과는 같다. 강화한 폭 검증기는 원본 계단 단독에서도 코너의 단면 높이 불일치를 검출한다.',
      '', '## 탐색 변수와 최선 후보', '',
      '비교: 기둥 위상 0/3/5, 참·코너 위치 b=11/12, 발코니 회전 진행 ±방향, 아치 시작 높이 +2/+3, 차양 수직 오프셋 0/−1, 원형 마스크/축 점유 상자.',
      '최선 후보의 출입 시작점은 기존 좌표계에서 90° 회전시켰다. 기단의 주출입구 설계를 바꾼 것이 아니며 원본은 D4 대칭이다.',
      f'선택 `{best["id"]}`: `{best["parameters"]}`.',
      f'정적 검사 실패 0인 후보 수: {sum(r["static_failures"]==0 and r["joint_failures"]==0 for r in table)} / {len(table)}. BFS를 실행하지 않은 후보는 PASS로 승격하지 않는다.',
      '완전한 후보별 결과는 candidate_table.json에 있다. 아래 표의 숫자는 거리/높이 후보이며 허용 오차를 승인하지 않는다.',
      '', '| 후보 | 마스크 | b | 위상 | 회전 진행 | 아치 | 차양 | 정적 실패 | 접합 실패 | 원형 밖 열 |',
      '|---|---|---|---|---|---|---|---|---|---|']
    for r in table:
        p=r['parameters'];lines.append(f'| {r["id"]} | {p["domain"]} | {p["b"]} | {p["phase"]} | {p["shift_direction"]} | {p["arch_lift"]} | {p["canopy_lift"]} | {r["static_failures"]} | {r["joint_failures"]} | {r["outside_original_disc45_columns"]} |')
    lines.extend(['', '## 외경 해석을 숨기지 않는 판정', '',
      'GEOMETRY-02B에서 요청된 외경 기준은 X/Z 점유 폭이다. axis45 후보는 일반 부재의 좌표가 −22..22, 발코니는 −25..25인지 검사한다.',
      'disc45 대조군은 기둥·아치의 탐색 마스크를 원판으로 제한한 경우다. 계단 자체의 원판 이탈도 원형 밖 열 수에 포함한다. 모든 부재가 원판 안에 들어간 후보라는 뜻은 아니다.',
      '그러나 원형 예약 마스크 D45는 X/Z 상자와 다른 집합이다. axis45의 일부 일반 부재는 그 원판 밖에 있다. 원본 예약 집합을 덮어쓰거나 발코니 51 예외를 일반 부재에 적용하지 않았다.',
      f'최선 후보의 원형 예약 마스크 밖 일반 점유 열 수는 {best["outside_original_disc45_columns"]}개이며 claims 파일에 좌표를 모두 저장했다.',
      f'원판 안 기둥을 고수한 대조 후보 {strict["id"]}는 정적 실패 {strict["static_failures"]}개다. 이 실패를 전체 설계 CONFLICT로 일반화하지 않는다.',
      '**원래의 원형 예약 집합까지 불변의 점유 한계로 요구한다면 최선 후보는 승인할 수 없다.** 축 점유 제한을 만족한 존재 증거와 이 해석 문제를 분리한다.',
      '따라서 이 결과를 모든 확정 조건의 무조건적 PASS로 보고하지 않는다. 대각선 실제 블록 모서리 측정값과 마스크 밖 좌표는 envelope.json에 기록한다.',
      '', '## 단계별 독립 검사', '',
      '순서: 1 계단 / 2 +기둥 / 3 +난간 / 4 +발코니 / 5 +아치 / 6 +부분 차양·관문 예약.',
      '| 모델 | 귀속 | 단계 | 폭·단차 | F2/F3/F4/성소 | 발코니 8곳 | 최소 머리 공간 |','|---|---|---|---|---|---|---|'])
    for label,rr in (('원본',legacy_results),('최선',results)):
        for conv,r in rr.items():
            for st in r['stages']:
                lines.append(f'| {label} | {conv} | {st["stage"]} | {st["width_status"]} | {st["floor_access"]} | {st["balcony_access"]} | {st["minimum_overhead_clearance"]} |')
    lines.extend(['', '각 단계의 최초 폭 침범·상승 부적합 위치와 최초 미도달 목표는 original/best의 audit_C1/C2.json에 저장한다.',
      '최초 미도달 목표는 연속 공간 전체의 불가능성 증명이 아니다. 발견된 경로는 navigation 파일의 시작점과 이동 벡터·반복 횟수로 손실 없이 복원된다.',
      '폭 검사는 한 플레이어 BFS와 독립이다. 매 단면 4개 연속 보행 열·실제 지지면·계단 반층 전이·방향·인접 단면 연결, 코너 4×4 패드의 평탄·머리 공간을 검사한다.',
      '머리 공간은 블록이 빈지만 보지 않고, 아래 반층/진행 방향 위 반층의 실제 충돌 상자와 플레이어 AABB로 검사한다. 연속 계단에서 이웃 반층에 발이 올라가는 경우도 포함한다. 각 열 안과 인접 단면 사이에서 수평·상승 쓸림까지 검사한다.',
      'BFS는 별도의 동결된 GEOMETRY-03 충돌 탐색기로 0.25 격자, AABB 0.6×1.8, 상승≤0.5, 하강≤1, 점프·비행 없이 수행한다. 탐색 한도는 결과에 공개한다.',
      '실제 Minecraft의 stair 자동 모양 변경·상태·경계 상호작용·멀티플레이 통행을 검증한 것은 아니다. 인게임 검증 NOT RUN.',
      '같은 y의 바닥 접촉, 기둥-아치, 아치-차양 몰딩의 돌 접촉은 의도된 접합이다. 보행 AABB나 발코니 사용 공간의 점유는 접합으로 면제하지 않는다.',
      '', '## C1/C2 및 미확정 조건', '',
      '새 부재는 C2가 C1보다 y−1이며, 원본 모델을 각각 사용해 다시 탐색한다. 동일한 결과가 나와도 귀속을 확정하지 않는다.',
      f'기둥 중심 간 실제 X/Z 거리: 최선 후보 {best["spacing_xz_min"]:.3f}–{best["spacing_xz_max"]:.3f}. 경로 투영 스테이션 간격 {best["projected_spacing_min"]}–{best["projected_spacing_max"]}. 3D 거리와 모든 쌍은 audit에 있다.',
      '| 조건 | 남은 결정/검증 |','|---|---|',
      '| 원형 예약 마스크 | X/Z45 범위 내 마스크 밖 일반 부재 허용 여부. 허용을 가정해 승인하지 않음 |',
      '| Q85 약 7 | 실제 간격이 제시된 후보를 허용하는 오차 범위. 임의 PASS 없음 |',
      '| Q93/Q94 | 두 단의 블록 상태·단차 상세; 현재 표준 단면 안의 후보 |',
      '| Q107/Q111 | 객체별 주두·연결 아치 시작/관문·연관 회랑 정점의 대응을 좌표로 기록. 시각적 수용·실제 단차의 표현 검증 필요 |',
      '| Q113–Q118 | 공통 중앙+양측·층별 발전 예약 유지. 실제 문양/부조 도안 null |',
      '| 기단·성소 | 기존의 단별 반경·곡선 계단·12성소 아치·보상 제단·부유 장식 상세 미정 유지 |',
      '| Q37/Q44/Q103–105 | 상세 처마·계단 받침·벽 밀착 받침 일부가 아직 완성 좌표가 아님. 외곽 전체 승인 근거로 사용하지 않음 |',
      '', '## 최소 변경 대안과 GEOMETRY-04', '',
      '치수 변경 0인 대안은 이 axis45 배치를 원형 마스크 밖 좌표와 함께 검토하는 것이다. 기존 수치의 외경·높이·폭·기둥·발코니 수/면적을 줄이거나 늘리지 않았다.',
      '원래 원형 예약대 점유까지 엄격히 유지해야 한다면 이 대안은 미승인이다. 우선 더 효율적인 4열 코너와 기둥/참 공동 재배치를 탐색해야 하며, 그 전에는 폭/기둥 축소나 외경 확대가 최소 변경이라고 증명할 수 없다.',
      '기둥 간격 허용 범위의 명시가 우선 필요한 결정이다. 간격 허용·실루엣/예약 마스크 해석만으로 해결 가능한지 검토한 뒤에야 수치 치수 변경을 제안해야 한다.',
      '**GEOMETRY-04: 좌표 정밀화·해석 결정용 검토 착수 가능. 최종 배치 확정 및 NBT/Java/월드젠 착수는 보류.**',
      'CANDIDATE FAILURE는 특정 후보의 실제 실패다. CONFLICT로 판정한 설계 전체 불가능성은 없다. NBT·Java·월드젠·풍화·PR 병합 없음.',
      '', '## 파일 안내', '',
      '- source_manifest.json: 지정 HEAD와 원본 Git blob 해시.',
      '- reproduction.json: 원본 4종 실패, 부재·설계 ID·검사 조건.',
      '- candidate_table.json: 96개 변수 조합 및 정적 결과. 미실행 BFS는 NOT RUN.',
      '- best/blocks_C1/C2.csv: 실제 좌표·형상·방향·부재별 행. 의도된 중첩을 보존.',
      '- best/claims_C1/C2.json: 단면·코너·개구부·원형 밖 좌표. 원본 바닥판은 별도 불변 입력.',
      '- best/objects_C1/C2.json: 기둥·명암선·아치·차양·발코니·관문 예약·이동 구간. C2/회전은 명시된 변환을 적용.',
      '- original/: 같은 형식의 기존 후보와 단계별 재검사.',
      '- audit_C1/C2.json, navigation_C1/C2.json: 독립 단계 검사와 경로 좌표의 손실 없는 표현.',
      '- failure_*.svg: 원본 실패 좌표의 평면·단면. envelope.json: 외곽·높이·대각 모서리 측정.',
      '- results.json: 종합·C1/C2·GEOMETRY-04 게이트.'])
    (out/'REPORT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')


if __name__=='__main__':main()
