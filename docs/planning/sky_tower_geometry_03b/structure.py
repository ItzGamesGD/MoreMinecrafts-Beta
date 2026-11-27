"""Independent object/voxel join audit and final report supplement.

Run after run.py. Reads exported CSV/JSON only; does not import the generator.
"""
from collections import deque
import csv
import json
from pathlib import Path
import sys


def components(cells):
    remaining=set(cells);n=0
    while remaining:
        n+=1;todo=[remaining.pop()]
        while todo:
            x,y,z=todo.pop()
            for p in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
                if p in remaining:remaining.remove(p);todo.append(p)
    return n


def touches(A,B):
    return bool(A&B) or any((x+dx,y+dy,z+dz) in B for x,y,z in A
        for dx,dy,dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)))


def audit(folder,conv):
    objects=json.loads((folder/f'objects_{conv}.json').read_text())
    rot,dy=objects['transform']['rotation_quarters'],objects['transform']['dy']
    def T(p):
        x,y,z=p
        for _ in range(rot%4):x,z=-z,x
        return x,y+dy,z
    groups={}
    with (folder/f'blocks_{conv}.csv').open() as f:
        for r in csv.DictReader(f):groups.setdefault(r['group'],set()).add((int(r['x']),int(r['y']),int(r['z'])))
    piers={p['id']:p for p in objects['pillars']};arches={a['id']:a for a in objects['arches']}
    checks=[]
    def add(id,ok,evidence):checks.append({'id':id,'status':'PASS' if ok else 'CANDIDATE FAILURE','evidence':evidence})
    for a in objects['arches']:
        cells=set(map(T,a['cells']));A=set(map(T,piers[a['from']]['cells']));B=set(map(T,piers[a['to']]['cells']))
        add(a['id'],components(cells)==1 and cells<=groups['arcade_arch'] and touches(cells,A) and touches(cells,B),
            {'components_face':components(cells),'touch_from':touches(cells,A),'touch_to':touches(cells,B)})
    links={o['id']:o for o in objects['links_canopies'] if o['id'].startswith('L')}
    for L in links.values():
        p=piers[L['support']];cells=set(map(T,L['cells']));P=set(map(T,p['cells']))
        endpoint=T((p['xz'][0],p['capital_start'],p['xz'][1]))
        add(L['id'],components(cells)==1 and cells<=groups['wall_link_arch'] and endpoint in cells&P and L['spring']==p['capital_start'],
            {'capital_arch_common_voxel':endpoint,'capital_start':p['capital_start']+dy,'arch_start':L['spring']+dy,
             'components_face':components(cells),'design':'Q99/Q107'})
    for C in (o for o in objects['links_canopies'] if o['id'].startswith('C')):
        cells=set(map(T,C['cells']));L=set(map(T,links[C['link']]['cells']))
        delta=C['outer_column']-C['support_outer_column']
        add(C['id'],components(cells)==1 and touches(cells,L) and delta<=1 and C['tiers'] in (2,3),
            {'touch_link':touches(cells,L),'components_face':components(cells),
             'outward_extension':delta,'tiers':C['tiers'],'design':'Q90/Q91/Q108/Q109'})
    walks=groups['stairs'];head={(x,Y,z) for x,y,z in walks for Y in (y+1,y+2,y+3)}
    for key,G in objects['gateway_reservations'].items():
        cells=set(map(T,G['arch_cells']));associated=arches[G['associated_arcade']]
        peak=max(y for x,y,z in cells);other=max(T(p)[1] for p in associated['cells'])
        reserves=set(map(T,G['motif']+G['pilaster']))
        add(key,peak==other and cells<=groups['gateway_frame'] and not reserves&head and G['detailed_pattern'] is None,
            {'gateway_apex_y':peak,'associated_arcade':associated['id'],'arcade_apex_y':other,
             'reservation_head_overlap':sorted(reserves&head),'actual_pattern':None,'design':'Q110-Q118'})
    return {'status':'PASS' if all(c['status']=='PASS' for c in checks) else 'CANDIDATE FAILURE',
            'checks':checks,'in_game_validation':'NOT RUN',
            'interpretation':'Joins and reservations only; no approval of detailed tier shape, motif, spacing or reserve-domain interpretation.'}


def main(out):
    results={}
    envelope=json.loads((out/'envelope.json').read_text())
    for conv in ('C1','C2'):
        normal=set()
        with (out/'best'/f'blocks_{conv}.csv').open() as f:
            for row in csv.DictReader(f):
                if not row['group'].startswith('balcony'):normal.add((int(row['x']),int(row['z'])))
        outside=sorted(p for p in normal if p[0]**2+p[1]**2>=22.5**2)
        cp=out/'best'/f'claims_{conv}.json';claims=json.loads(cp.read_text())
        claims['outside_original_disc45_columns']=outside
        cp.write_text(json.dumps(claims,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
        envelope[conv]['outside_original_disc45_columns']=outside
        if conv=='C1':
            svg=['<svg xmlns="http://www.w3.org/2000/svg" width="780" height="810">',
                 '<rect width="780" height="810" fill="#faf9f5"/>',
                 '<text x="20" y="25" font-family="sans-serif" font-size="17">Best candidate: normal-member occupied-column union</text>',
                 '<text x="20" y="48" font-family="sans-serif" font-size="12">Magenta = outside frozen circular D45 reserve. Dashed = X/Z 45 bound.</text>']
            for x,z in sorted(normal):
                color='#bd3fa2' if (x,z) in set(outside) else '#768b96'
                svg.append(f'<rect x="{380+x*13}" y="{395+z*13}" width="13" height="13" fill="{color}" stroke="#fff" stroke-width=".3"/>')
            svg.append('<rect x="94" y="109" width="585" height="585" fill="none" stroke="#202020" stroke-dasharray="5 3"/>')
            svg.append('<text x="20" y="765" font-family="sans-serif" font-size="12">All Y layers projected; exact outside-mask columns are in claims/envelope JSON. Overall UNVERIFIED.</text></svg>')
            (out/'best_envelope_plan.svg').write_text('\n'.join(svg)+'\n')
    (out/'envelope.json').write_text(json.dumps(envelope,sort_keys=True,separators=(',',':'))+'\n')
    for conv in ('C1','C2'):
        r=audit(out/'best',conv);results[conv]=r
        (out/'best'/f'structure_{conv}.json').write_text(json.dumps(r,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
    p=out/'REPORT.md';text=p.read_text();marker='\n## 추가 독립 접합·예약 검사\n'
    text=text.split(marker)[0]
    # The complete reproducibility recipe includes this independent pass.
    if 'python3 -I structure.py data\n' not in text:
        text=text.replace('python3 -I run.py --out data\n','python3 -I run.py --out data\npython3 -I structure.py data\n')
    if 'python3 -I structure.py /tmp/sky-tower-g03b-recheck\n' not in text:
        text=text.replace('python3 -I run.py --out /tmp/sky-tower-g03b-recheck\n',
             'python3 -I run.py --out /tmp/sky-tower-g03b-recheck\npython3 -I structure.py /tmp/sky-tower-g03b-recheck\n')
    text+=marker+'\nstructure.py는 생성기를 불러오지 않고 내보낸 CSV/객체 좌표만 읽는다. 기둥-아치 면 인접, 연결 아치-주두 공통 시작 복셀, 차양 접합·단수·돌출, 관문과 지정 회랑의 실제 정점 및 예약 공간 비침범을 확인한다.\n\n'
    for conv,r in results.items():text+=f'- {conv}: **{r["status"]}**, {len(r["checks"])}개 객체 접합·예약 검사. 상세 결과 best/structure_{conv}.json.\n'
    text+='\n이 통과는 Q85 허용 간격·원형 예약 마스크 해석·상세 도안·인게임 검증의 미확정 상태를 해소하지 않는다. 전체 판정과 GEOMETRY-04 구현 보류는 유지한다.\n'
    envelope=json.loads((out/'envelope.json').read_text())
    text+='\n일반 부재의 실제 블록 모서리 유클리드 직경은 '+str(envelope['C1']['normal']['far_corner_diameter'])+'블록이다. X/Z 45와 혼동하지 않으며, 원형 실루엣이나 유클리드 지름 45까지 충족했다고 주장하지 않는다.\n'
    text+='\n수치 치수 변경 0은 모든 설계 변경이 0이라는 증명이 아니다. 원형 실루엣/예약대 밖 점유를 허용하려면 해당 해석에 대한 사용자 결정이 필요하다. 그 승인을 선행했다고 가정하지 않는다.\n'
    text+='\n[최선 후보의 일반 부재 점유·원형 예약대 이탈 평면](best_envelope_plan.svg)에서 마스크 밖 열을 자홍색으로 표시했다.\n'
    text=text.replace('- original/: 같은 형식의 기존 후보와 단계별 재검사.',
        '- original/: 기존 후보의 단계별 재검사와 경로. 원본 좌표는 보존된 GEOMETRY-03 자료를 참조하며, 정규화 중간 CSV/객체 파일은 검증과 도면 생성 후 제거한다.')
    p.write_text(text)
    overall=json.loads((out/'results.json').read_text());overall['independent_structure']={k:r['status'] for k,r in results.items()}
    (out/'results.json').write_text(json.dumps(overall,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n')
    # These are new task-local normalized intermediates, never the frozen GEOMETRY-03 files.
    for kind in ('blocks','claims','objects'):
        for conv in ('C1','C2'):
            ext='csv' if kind=='blocks' else 'json'
            (out/'original'/f'{kind}_{conv}.{ext}').unlink(missing_ok=True)
    print('Independent joins:',{k:r['status'] for k,r in results.items()})


if __name__=='__main__':main(Path(sys.argv[1]))
