#!/usr/bin/env python3
"""SKY-TOWER GEOMETRY-03 candidate exploration only; no NBT or game imports."""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import importlib.util
import itertools
import json
import math
from pathlib import Path

HERE = Path(__file__).resolve().parent
PREV = HERE.parent / 'sky_tower_geometry_02'
BASE_HEAD = '2138ba9eb882010b3c1df9338ee134343709ed73'
FACING = {(1,0): 'east',(-1,0): 'west',(0,1): 'south',(0,-1): 'north'}
COLORS = {'stairs': '#e0ad57', 'landing': '#e0ad57', 'bridge': '#bba06c',
          'pillar': '#695e55','arch': '#7d8f91','canopy': '#468b88','rail': '#b66c57',
          'balcony_floor': '#76a5ce', 'balcony_rail': '#b66c57','balcony_support': '#8ca0ab',
          'gateway_frame': '#b49bc0', 'baseline': '#c9c9c9'}


def disc(d):
    return {(x,z) for x in range(-27,28) for z in range(-27,28) if x*x+z*z < (d/2)**2}


def pad(p):
    return {(p[0]+dx,p[1]+dz) for dx in (-1,0,1,2) for dz in (-1,0,1,2)}


def rotate(p, turns):
    x,z = p
    for _ in range(turns % 4):
        x,z = -z-1,x
    return x,z


def rotate_cell(p, turns):
    # Blocks rotate about the baseline axis at (.5,.5), not about an even-width pad centre.
    x,z = p
    for _ in range(turns % 4):
        x,z = -z,x
    return x,z


def grid_loop(a, b):
    quarter = [(a,0)]
    for z in range(1,b+1):
        quarter.append((a,z))
    x,z = quarter[-1]
    while x > b:
        x -= 1
        quarter.append((x,z))
        z += 1
        quarter.append((x,z))
    while x >= 0:
        x -= 1
        quarter.append((x,z))
    return [rotate(p,t) for t in range(4) for p in quarter[:-1]]


def clear_width(lane, obstacles, floor_y):
    return sum(all((x,y,z) not in obstacles for y in (floor_y+1,floor_y+2)) for x,_,z in lane)


def link_arch(h, turns):
    cells=set()
    prev=h+5
    for x in range(15,22):
        top=h+5+round(math.sqrt(max(0,9-(x-18)**2)))
        for Y in range(min(prev,top),max(prev,top)+1):
            for z in (-1,0):
                X,Z=rotate_cell((x,z),turns)
                cells.add((X,Y,Z))
        prev=top
    return cells


def rail_openings(rail, attached_floor):
    return {(x,y,z) for x,y,z in rail if (x,y-1,z) not in attached_floor}


def source_inputs():
    expected = {'geometry02b/baseline.json': 'f7f0967efcad8734d6211076bb284f3a574325a4',
                'geometry02b/results.json': '64114252b713a6720fe5e91e934639e1a1ac36ff',
                'geometry02_model.py': '24295fa1513bfcee66e9c2999cac163219fb77d4',
                '../SKY_TOWER_DESIGN_DECISIONS_Q01_Q41_2026-11-05.md': 'b258e49ae80a04c02a806dd881b701295f9cb288'}
    hashes = {}
    for path, sha in expected.items():
        raw = (PREV/path).read_bytes()
        actual = hashlib.sha1(b'blob '+str(len(raw)).encode()+b'\0'+raw).hexdigest()
        if actual != sha:
            raise ValueError('BASE MISMATCH: '+path)
        hashes[path] = actual
    base = json.loads((PREV/'geometry02b/baseline.json').read_text())
    assert base['candidate'] == 'B25-T3' and base['slab_thickness'] == 1
    return base, hashes


def schedule(loop, rises):
    """Full-block rises on straight cardinal runs; balcony/gateway flats protected.

    A higher tread uses a bottom stair with two half-block collision surfaces.
    Values below are candidate construction parameters, not new design authority.
    """
    n = len(loop)
    directions = [(loop[(i+1)%n][0]-p[0],loop[(i+1)%n][1]-p[1]) for i,p in enumerate(loop)]
    h, records, balconies = 0, [], []
    for lap,rise in enumerate(rises):
        sites = [n//4,3*n//4] if lap%2 == 0 else [0,n//2]
        protected = set()
        for center,radius in [(0,4), *((i,6) for i in sites)]:
            protected.update((center+j)%n for j in range(-radius,radius+1))
        allowed = [i for i in range(1,n) if i not in protected and
                   directions[i] == directions[(i-1)%n] == directions[(i+1)%n]]
        assert len(allowed) >= rise, (lap,len(allowed),rise)
        chosen = {allowed[(j*len(allowed))//rise] for j in range(rise)}
        start = h
        for i,p in enumerate(loop):
            if i in chosen:
                h += 1
            records.append({'lap':lap,'station':i,'xz':p,'relative_floor':h,'direction':directions[i],
                            'rise': int(i in chosen), 'landing': i in protected,
                            'turn': directions[i] != directions[(i-1)%n]})
        assert h-start == rise
        for site in sites:
            balconies.append({'lap':lap,'station':site,'relative_floor':records[lap*n+site]['relative_floor']})
    records.append({'lap':len(rises),'station':0,'xz':loop[0],'relative_floor':h,
                    'direction':directions[0],'rise':0,'landing':True,'turn':False})
    return records, balconies


def candidate_search():
    env, wall = disc(45), disc(31)
    results = []
    for a,b in ((17,6),(17,7),(17,8),(17,9),(18,6),(18,7)):
        loop = grid_loop(a,b)
        walk = set().union(*(pad(p) for p in loop))
        for phase in (0,3):
            columns = []
            for i in range(phase,len(loop),7):
                px,pz = loop[i]
                # Candidate outer body centres closest to this route station.
                options = []
                for x,z in env-wall:
                    cells = {(x+dx,z+dz) for dx in (-1,0,1) for dz in (-1,0,1)}
                    if cells <= env-wall:
                        r = math.hypot(px+.5,pz+.5)
                        tx,tz = (px+.5)*(r+3.5)/r,(pz+.5)*(r+3.5)/r
                        if (x-px)**2+(z-pz)**2 <= 36:
                            options.append(((x-tx)**2+(z-tz)**2 + 2*len(cells & walk),
                                            len(cells & walk),x,z))
                _,_,x,z = min(options)
                columns.append((i,x,z))
            blocked = set().union(*({(x+dx,z+dz) for dx in (-1,0,1) for dz in (-1,0,1)} for _,x,z in columns)) & walk
            results.append({'a':a,'b':b,'phase':phase,'loop_length':len(loop),
                'walk_wall_overlap_columns':len(walk & wall),'walk_outside45_columns':len(walk-env),
                'pillar_walk_overlap_columns':len(blocked),'pillars':columns})
    eligible = [r for r in results if not r['walk_wall_overlap_columns'] and not r['walk_outside45_columns']]
    best = min(eligible,key=lambda r:(r['pillar_walk_overlap_columns'],r['a'],r['b'],r['phase']))
    return results,best


def build(conv, base, selected):
    groups = defaultdict(set)
    states = {}
    objects = []
    void = set()
    reserved = {}
    inner, wall = (set(map(tuple,base['columns'][k])) for k in ('inner','wall'))
    full = inner | wall
    baseline = {}
    for layer in base['models'][conv]['layers']:
        cols = wall if layer['zone'] == 'WALL' else full
        baseline.update({(x,layer['y'],z): ('full','') for x,z in cols})
    floor0 = base['vertical'][conv][1]['walking_floor_y']
    floors = [r['walking_floor_y'] for r in base['vertical'][conv][1:]]
    loop = grid_loop(selected['a'],selected['b'])
    records, balc_sites = schedule(loop,[b-a for a,b in zip(floors,floors[1:])])

    def put(name,cells,kind='full',facing=''):
        cells = set(cells)
        groups[name].update(cells)
        for p in cells:
            # Full masonry takes precedence over a stair at an incompatible contact.
            if p not in states or kind == 'full':
                states[p] = (kind,facing)
        return cells

    heights = defaultdict(dict)
    for idx,r in enumerate(records):
        h = floor0+r['relative_floor']
        r['floor_y'] = h
        for p in pad(r['xz']):
            heights[p][h] = idx
    floor_by_column = defaultdict(list)
    for (x,z), levels in sorted(heights.items()):
        clusters = []
        for y in sorted(levels):
            if clusters and y-clusters[-1][-1] <= 1:
                clusters[-1].append(y)
            else:
                clusters.append([y])
        for cluster in clusters:
            y = max(cluster)
            r = records[levels[y]]
            dx,dz = r['direction']
            behind = heights.get((x-dx,z-dz),{})
            is_stair = y-1 in behind and y not in behind
            put('stairs',{(x,y,z)},'stair' if is_stair else 'full',FACING[dx,dz] if is_stair else '')
            floor_by_column[x,z].append(y)
    # Exact walking/collision shapes are exported; no hidden slab or automatic jumping.
    for i,floor in enumerate(floors):
        put('bridge',{(x,floor,z) for x in range(11,20) for z in range(-1,3)})
        if i < 4:
            opening = {(x,y,z) for x in range(12,16) for y in range(floor+1,floor+6) for z in range(-1,3)}
            void.update(opening & set(baseline))
            # Two arch planes, 3-block jamb spacing each side of 4-wide opening.
            frame = set()
            for x in (12,13,15,16):
                for z in range(-3,5):
                    t = z-0.5
                    top = floor+5+round(math.sqrt(max(0,3.5**2-t*t)))
                    frame.update((x,y,z) for y in (top,top+1))
                frame.update((x,y,z) for z in (-3,4) for y in range(floor+1,floor+7))
            put('gateway_frame',frame)
            # Motif volumes are reservations, not invented designs or collision solids.
            reserved['F'+str(i+1)] = {'opening':sorted(opening),
                'motif':sorted((x,y,z) for x in (14,15) for y in range(floor+8,floor+10) for z in range(-5,7)),
                'pilaster':sorted((x,y,z) for x in (14,15) for y in range(floor+1,floor+7) for z in (-4,5)),
                'common_center_and_symmetric_extensions':True,'progression':i+1,
                'actual_pattern':None,'arch_apex_y':max(p[1] for p in frame),'opening_floor_y':floor}
    walk_states = {p:states[p] for name in ('stairs','bridge') for p in groups[name]}
    # Pillars: complete body, 2 levels of foot and capital, material accents.
    pillars = []
    n = len(loop)
    for lap in range(5):
        sites = selected['pillars'] if lap<4 else [(0,*selected['pillars'][0][1:])]
        for j,(station,x,z) in enumerate(sites):
            r = records[lap*n+station]
            fy = r['floor_y']
            cells = {(x+dx,y,z+dz) for dx in (-1,0,1) for dz in (-1,0,1) for y in range(fy,fy+6)}
            # Two candidate tiers INSIDE the 3x3 envelope; not oversized implicit 5x5 bases.
            foot = {(x+dx,fy-1,z+dz) for dx in (-1,0,1) for dz in (-1,0,1)} | {(x+dx,fy-2,z+dz) for dx in (-1,0,1) for dz in (0,)}
            capital = {(x+dx,fy+6,z+dz) for dx in (-1,0,1) for dz in (0,)} | {(x+dx,fy+7,z+dz) for dx in (-1,0,1) for dz in (-1,0,1)}
            cells |= foot | capital
            put('pillar',cells)
            obj = {'id':f'P{lap+1}-{j+1}','lap':lap,'station':station,'center_xz':[x,z],
                   'floor_y':fy,'cells':sorted(cells),'foot_tiers_y':[fy-2,fy-1],
                   'capital_tiers_y':[fy+6,fy+7],
                   'dark_stripe':sorted((X,y,Z) for X,Z in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)) for y in range(fy,fy+6))}
            pillars.append(obj)
    # Pairwise semicircular arcade arch with two adjacent radial molding planes.
    arches = []
    for a,b in zip(pillars,pillars[1:]):
        x0,z0 = a['center_xz']; x1,z1 = b['center_xz']
        length = math.hypot(x1-x0,z1-z0)
        spring = min(a['floor_y'],b['floor_y'])+5
        count = max(1,math.ceil(length*3))
        line = []
        for j in range(count+1):
            t = j/count
            x,z = round(x0+(x1-x0)*t),round(z0+(z1-z0)*t)
            y = spring+round(math.sqrt(max(0,(length/2)**2-((t-.5)*length)**2)))
            if line and y != line[-1][1]:
                for Y in range(min(y,line[-1][1]),max(y,line[-1][1])+1):
                    line.append((x,Y,z))
            line.append((x,y,z))
        offset = (1 if (x0+x1)>=0 else -1,0) if abs(x0+x1)>abs(z0+z1) else (0,1 if z0+z1>=0 else -1)
        cells = set(line) | {(x+offset[0],y,z+offset[1]) for x,y,z in line}
        put('arch',cells)
        arches.append({'from':a['id'],'to':b['id'],'kind':'semicircle_double_molding',
                       'span':length,'spring_y':spring,'apex_y':max(p[1] for p in cells),'cells':sorted(cells)})
    # Landings/balconies and exterior-wall arch/canopy joint.
    balconies,canopies,links = [],[],[]
    for item in balc_sites:
        lap,station = item['lap'],item['station']
        # Cardinal sites: width is tangent and outward is the normal.
        t = (station//(n//4))%4
        h = floor0+item['relative_floor']
        # Cardinal transform about (.5,.5) preserves all Minecraft blocks.
        clear2 = {rotate_cell((x,z),t) for x in range(20,23) for z in range(-3,4)}
        connector2 = {rotate_cell((x,z),t) for x in range(16,20) for z in range(-3,4)}
        # Half-disc-ish raster with 7x3 clear rectangle embedded; rounded ends are extra.
        footprint = clear2 | {rotate_cell((x,z),t) for x in range(19,24) for z in range(-4,5)
                              if ((x-19)/5)**2+(z/5)**2 <= 1}
        boundary = {p for p in footprint if any((p[0]+dx,p[1]+dz) not in footprint|connector2 for dx,dz in FACING)}
        # Explicitly reserve clear 7x3; any rail overlapping it will be reported.
        rail = {(x,h+1,z) for x,z in boundary}
        posts = {(x,y,z) for x,z in sorted(boundary)[::4] for y in (h+1,h+2)}
        floor_cells = {(x,h,z) for x,z in footprint|connector2}
        support = {(x,h-1,z) for x,z in footprint}
        support |= {(x,h-2,z) for x,z in footprint if (x,z) not in boundary}
        # Small decorative lower arch: half-circle in outward/tangent plane.
        small = set()
        for z in range(-2,3):
            x,Z = rotate_cell((21,z),t)
            small.add((x,h-4+round(math.sqrt(max(0,4-z*z))),Z))
        support |= small
        put('balcony_floor',floor_cells);put('balcony_rail',rail|posts);put('balcony_support',support)
        balconies.append({'id':f'B{lap+1}-{len([b for b in balconies if b["lap"]==lap])+1}',
            'lap':lap,'station':station,'rotation_quarters':t,'floor_y':h,
            'clear7x3':sorted((x,h,z) for x,z in clear2),'connector':sorted((x,h,z) for x,z in connector2),
            'floor':sorted(floor_cells),'rail_and_posts':sorted(rail|posts),'support':sorted(support),
            'decorative_arch':sorted(small)})
        # Link arch from outer wall to selected landing support; canopy on the same joint.
        link = link_arch(h,t)
        put('arch',link)
        # 2 candidate setbacks, support edge at radial column 21; 1 beyond =22.
        canopy = set()
        for dy,edge in ((9,22),(10,21),(11,20)):
            for x in range(16,edge+1):
                for z in range(-3,4):
                    X,Z = rotate_cell((x,z),t);canopy.add((X,h+dy,Z))
        put('canopy',canopy)
        molding = {(x,y+1,z) for x,y,z in link if y == max(p[1] for p in link)}
        put('canopy',molding)
        links.append({'lap':lap,'station':station,'cells':sorted(link),'spring_y':h+5,
                      'apex_y':max(p[1] for p in link),'molding':sorted(molding)})
        canopies.append({'lap':lap,'station':station,'cells':sorted(canopy|molding),
                         'support_outer_column':21,'outward_max_column':22,'tiers':[h+9,h+10,h+11]})
    # A continuous outer boundary rail following every actual stair floor level.
    walk2 = set(heights)
    rail = set()
    for (x,z),levels in floor_by_column.items():
        # Exterior-facing edge only; small full-cube rail is a conservative candidate.
        for dx,dz in FACING:
            q = x+dx,z+dz
            if q not in walk2 and q not in full and math.hypot(*q)>math.hypot(x,z):
                for y in levels:
                    rail.add((q[0],y+1,q[1]))
    put('rail',rail_openings(rail,groups['balcony_floor']))
    # Wide test frames: 4 contiguous columns perpendicular to cardinal travel.
    sections = []
    for idx,r in enumerate(records):
        dx,dz = r['direction']; x,z = r['xz']
        pts = [(x+j,z) if dz else (x,z+j) for j in (-1,0,1,2)]
        samples = []
        for X,Z in pts:
            ys = floor_by_column[X,Z]
            y = min(ys,key=lambda Y:abs(Y-r['floor_y']))
            samples.append((X,y,Z))
        sections.append({'record':idx,'lap':r['lap'],'station':r['station'],
                         'direction':r['direction'],'floor_cells':samples})
    return {'convention':conv,'groups':dict(groups),'states':states,'baseline':baseline,
            'void':void,'reservations':reserved,'records':records,'sections':sections,
            'pillars':pillars,'arches':arches,'balconies':balconies,'canopies':canopies,
            'links':links,'floors':floors,'loop':loop,'walk_states':walk_states}


def module(name):
    spec = importlib.util.spec_from_file_location(name,HERE/(name+'.py'))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def dump(path,data):
    path.write_text(json.dumps(data,ensure_ascii=False,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')


def export(model,out):
    conv = model['convention']
    members = defaultdict(list)
    for name,cells in sorted(model['groups'].items()):
        for p in cells:
            members[p].append(name)
    dark = {tuple(p) for obj in model['pillars'] for p in obj['dark_stripe']}
    with (out/f'blocks_{conv}.csv').open('w',newline='') as f:
        w=csv.writer(f,lineterminator='\n');w.writerow(['x','y','z','shape','facing','groups','material_role'])
        for p in sorted(model['states']):
            w.writerow([*p,*model['states'][p],'|'.join(members[p]),'dark_vertical_accent' if p in dark else 'stone'])
    with (out/f'route_{conv}.csv').open('w',newline='') as f:
        w=csv.writer(f,lineterminator='\n')
        w.writerow(['lap','station','x','planned_floor_y','z','dx','dz','rise','turn','landing'])
        for r in model['records']:
            w.writerow([r['lap'],r['station'],r['xz'][0],r['floor_y'],r['xz'][1],*r['direction'],r['rise'],int(r['turn']),int(r['landing'])])
    segments=[]
    for r in model['records'][:-1]:
        key=(r['lap'],tuple(r['direction']),r['rise'],r['landing'])
        if segments and segments[-1]['key']==key:
            segments[-1]['horizontal_blocks']+=1
            segments[-1]['rise_blocks']+=r['rise']
            segments[-1]['end_station']=r['station']
        else:
            segments.append({'key':key,'start_station':r['station'],'end_station':r['station'],
                             'horizontal_blocks':1,'rise_blocks':r['rise'],'xyz':[r['xz'][0],r['floor_y'],r['xz'][1]]})
    dump(out/f'components_{conv}.json',{k:model[k] for k in ('pillars','arches','balconies','canopies','links')})
    dump(out/f'gateway_reservations_{conv}.json',{'gateways':model['reservations'],
        'portal_void_overlay':sorted(model['void']),'sanctum_has_no_new_gateway':True,
        'note':'Baseline is immutable. Effective collision uses this explicit proposed portal mask; detailed motifs are not selected.'})
    dump(out/f'movement_segments_{conv}.json',{'segments':segments,'quarter_grid_corner_pad':4,
         'route_order_is_not_navigation_proof':True,'lap_rises':[b-a for a,b in zip(model['floors'],model['floors'][1:])],
         'endpoint_floors':model['floors'],'horizontal_blocks_per_turn':len(model['loop']),
         'landing_runs_are_planned_not_all_proven_flat_after_union':True})


def write_report(out,results,selected,hashes):
    lines=['# SKY-TOWER-GEOMETRY-03 — 외부 나선 계단 및 아케이드 통합 좌표 검증',
        '', '**판정: UNVERIFIED — 이번 통합 후보는 확정 조건 전체를 충족하지 못했다. 설계 전체의 불가능성은 증명되지 않았다.**',
        '',f'기준 HEAD `{BASE_HEAD}`, 브랜치 `claude/sky-tower-geometry-02-coords-hgbfo7`.',
        '원격 HEAD 일치를 확인하고 Q01–Q118, GEOMETRY-02B 결과 및 B25-T3 원본 해시를 확인했다.',
        'B25-T3·s=1과 C1/C2 비교를 유지하며 기존 기준 모델 파일은 변경하지 않는다.',
        '', '## 범위·방법·재현', '',
        '사용자 요청에 따라 좌표 탐색과 실패 근거까지 수행했다. 모든 상세값은 검사 후보이며 설계 승인으로 승격하지 않는다.',
        'Minecraft 격자에서 면 인접 경로를 생성하고, 폭 4 사각 패드의 합집합으로 코너를 연결했다. 긴 직선에서만 상승을 배치했다.',
        '계단은 `bottom stair`의 아래 반층 전체 + 진행 방향 위 반층 충돌 상자로 표현한다. 회전 장식 stair 모양의 자동 연결은 사용하지 않았다.',
        '12개 제한된 경로·기둥 위상 후보를 비교했다. 선택 기준은 외벽/예약 원판 침범 0, 그 안에서 기둥의 평면 보행 영역 중첩 최소다.',
        '연속 곡선·방사 두께·단순 비중첩만으로 통행 PASS를 만들지 않았다. 실패한 후보를 외경 확장이나 폭 축소로 보정하지 않았다.',
        '', '```sh','python3 -I test_geometry03.py','python3 -I geometry03_model.py --out data',
        'python3 -I geometry03_model.py --out /tmp/sky-tower-g03-recheck','diff -qr data /tmp/sky-tower-g03-recheck','```',
        '', '생성기는 표준 라이브러리만 사용한다. 종료 0은 검사가 실행됐다는 뜻이며 통합 설계 PASS가 아니다.',
        '', '## 산출물', '',
        '- `geometry03_model.py`: 고정 기준 입력, 후보 탐색·좌표 생성·내보내기.',
        '- `geometry03_audit.py`: 실제 점유·단면·접합·머리 공간 검사.',
        '- `geometry03_navigation.py`: 생성 경로 인덱스를 사용하지 않는 별도 AABB BFS.',
        '- `geometry03_render.py`, `test_geometry03.py`: 좌표 기반 SVG 및 회귀 검사.',
        '- `data/blocks_C1.csv`, `blocks_C2.csv`: 모든 신규 블록 `(x,y,z)`, 충돌 형상, 계단 방향, 소속 부재. 원본 점유는 GEOMETRY-02B에서 불변 재사용.',
        '- `route_*.csv`, `movement_segments_*.json`: 각 이동의 위치·상승·거리·방향·회전·참과 층 연결.',
        '- `components_*.json`: 기둥 몸체/2단 주초·주두·명암선, 아치, 발코니 보행/난간/받침/장식 아치, 부분 차양 좌표.',
        '- `gateway_reservations_*.json`: 개구부·문양·벽기둥 예약. 실제 도안은 null. 옥상 신규 관문 없음.',
        '- `audit_*.json`: 모든 단면의 폭, 쌍별 중첩 전체 좌표, 수직 간격·접합 검사.',
        '- `navigation_*.json`: 각 층/성소 및 발코니의 실제 탐색 결과와 발견된 경로 좌표.',
        '- `bare_stair_control_*.json`: 원본/개구부/계단/연결 바닥만 남긴 별도 대조 검사와 경로 좌표.',
        '- `candidate_search.json`, `results.json`: 후보 비교와 종합 판정.',
        '- `plan_*_F1..F4.svg`, `plan_*_SANCTUM.svg`, `wireframe_*.svg`: 실제 좌표 투영. 와이어프레임은 선택 y면과 점유 열을 표시하는 희소 뷰.',
        '', '## 수직·경로 모델', '',
        '층별 보행 바닥 블록 y는 C1 `[10,21,33,47,64]`, C2 `[9,20,32,46,63]`이다. 두 후보 모두 구간 상승은 `[11,12,14,17]`이다.',
        f'선택된 탐색 경로는 a={selected["a"]}, b={selected["b"]}, 기둥 위상 {selected["phase"]}이며 한 회전 수평 거리는 {selected["loop_length"]}블록이다.',
        '순서열의 코너는 4×4 패드로 연결한다. 평탄 참과 상승은 반복되며, 합집합 뒤 실제 바닥 높이와 폭은 별도로 검사한다.',
        '기둥은 경로상 7스텝마다 목표를 두고 가까운 3×3 외곽 위치를 탐색했다. 끝 성소 도착부 기둥까지 81개, 인접 기둥 사이 아케이드 아치 80개다. 실제 중심 간 거리도 기록하며 `약 7`의 허용 오차를 임의 승인하지 않는다.',
        '3×3 내에서 2단 주초·주두를 비교하나, 정교한 단차 블록 상태·재료 구성의 확정은 아니다. 기존 전투실 주기둥 8개와 혼동하지 않는다.',
        '발코니는 상승 구간당 2개, 총 8개다. 인접 층간 구간은 위치를 90° 회전하며, 7×3 유효 사각형을 포함한 둥근 외곽 후보를 별도 좌표로 만든다.',
        '부분 차양은 발코니 참 8곳에만 두고 3단 후퇴·지지 외측 +1 후보를 사용한다. 전환부 차양과 처마 통합은 미완이다.',
        '', '## C1/C2 검사 결과', '',
        '| 검사 | C1 | C2 |','|---|---|---|']
    for a,b in zip(results['C1']['checks'],results['C2']['checks']):
        lines.append(f'| {a["id"]} ({", ".join(a["design_ids"])}) | {a["status"]} | {b["status"]} |')
    for conv in ('C1','C2'):
        result=results[conv];checks={c['id']:c for c in result['checks']}
        nav=result['navigation']
        lines.extend(['',f'### {conv}의 재현 가능한 실패 근거','',
            f'- 단면 유효 폭 분포: `{checks["FULL_WIDTH_4"]["evidence"]["histogram"]}`. 높이가 같은 4열 조건도 함께 검사한다.',
            f'- 최초 폭/단차 실패: `{json.dumps(checks["FULL_WIDTH_4"]["evidence"]["first_failed"],ensure_ascii=False)}`.',
            f'- 아치·차양 아래 최소 수직 여유: `{checks["ARCH_CANOPY_CLEARANCE"]["evidence"]["minimum"]}`블록. 부족 위치는 audit 파일에 전부 저장했다.',
            f'- 각 층/성소 BFS 도달: `{nav["floors"]["reached"]}`, 시작 유효 `{nav["floors"]["start_standable"]}`, 방문 `{nav["floors"]["visited_states"]}`.',
            f'- 계단 대조 모델의 F2/F3/F4/성소 도달: `{result["bare_stair_control"]["reached"]}`. 주변 부재를 제거한 모델에서의 존재 증거이며 통합 PASS가 아니다.',
            f'- 발코니 중앙 BFS 도달: `{nav["balconies"]["reached"]}`.',
            f'- 기둥의 실제 중심 간 거리 범위: `{checks["PILLAR_SPACING_ABOUT7"]["evidence"]["actual_center_distance_range"]}`블록.',
            f'- 유효 7×3 바닥의 첫 방해 위치: `{next((b for b in checks["BALCONY_CLEAR_7X3"]["evidence"] if b["blocked_air"]),None)}`.'])
    lines.extend(['', '## 독립 통행 검사와 접촉 판정', '',
        'BFS는 route 순서·단면 인덱스를 가져오지 않는다. 원본 점유에서 명시된 관문 예약 개구부만 뺀 충돌 공간에 새 부재의 실제 형상을 합쳐 탐색한다.',
        '0.25블록 수평 격자, 플레이어 AABB 폭 0.6·높이 1.8, 최대 상승 0.5, 최대 하강 1블록을 사용한다. 점프·비행·벽 통과는 허용하지 않는다.',
        '경로 검사는 결계 해제 후의 기하를 대상으로 한다. Q19–Q22의 완료 판정·영구 해제·우회 허용 기능을 구현하거나 변경하지 않는다.',
        '매 이동은 지면 지지, 머리 공간, 상승 시 수직 쓸림과 수평 쓸림을 검사한다. 탐색 한도에 닿으면 실패 확정 대신 UNVERIFIED다.',
        '경로를 발견하면 해당 충돌 모델 안에서 한 사람이 통과한다는 존재 증거다. 폭 4·난간 안전·바닐라 기능 검증을 대신하지 않는다.',
        '찾지 못한 경우에도 유한 샘플 탐색의 결과이며 모든 연속 위치나 다른 설계 후보의 불가능성을 뜻하지 않는다.',
        '슬래브와 연결 바닥의 같은 y 접촉, 기둥-아치와 아치-차양의 석조 접합은 의도된 접촉으로 구별한다.',
        '그러나 보행 머리 공간에 들어온 기둥·난간·지지부나 평탄 7×3을 가리는 부재는 접합으로 정당화하지 않는다. 쌍별 중첩을 삭제하지 않고 모두 내보낸다.',
        '', '## 미해결 조건·대안·재결정', '',
        '| 항목 | 근거와 후속 대안 | 사용자 재결정 |','|---|---|---|',
        '| Q43/Q98 폭 4와 반복 기둥 | 현 후보의 곡선부 단면을 기둥이 침범한다. 더 정교한 4열 코너·기둥 위상/위치 공동 탐색이 필요하다. 4×4 패드 방식의 실패를 모든 폭4 방식의 불가능성으로 일반화하지 않는다. | 폭·단면 변경은 금지. 기존 값 내 탐색에는 불필요 |',
        '| Q74/Q77/Q80 발코니 | 평탄 7×3과 난간/기둥을 함께 배치한 현 후보에 침범이 남는다. 참의 기둥 위치와 발코니 난간 경계를 함께 최적화해야 한다. | 7×3 축소·발코니 삭제/이동 규칙 변경은 결정 필요 |',
        '| Q86–Q92/Q107–Q111 | 부재의 접합과 통행 여유는 서로 다른 검사다. 관문과 이웃 회랑 정점, 주두 시작 높이의 대응 관계·전환부 통합이 증명되지 않았다. | 확정 조건 변경 없이 새 좌표 후보 비교 가능 |',
        '| Q85 약 7블록 | 명목 스테이션 간격과 실제 중심 간 거리를 모두 공개했다. 허용 오차를 정하지 않았다. | 최종 판정에 허용 범위 필요 |',
        '| Q110–Q118 관문 문양 | 공통 중앙+양측/층별 발전을 위한 예약만 만들었다. 상세 도안·부분 부조 상태는 미정이다. | 실제 도안 확정 시 필요 |',
        '| 기단·최상층 | GEOMETRY-02B의 단별 치수, 곡선 계단, 12성소 아치·제단·부유 고리 미확정 상태 유지. | 별도 치수 결정 필요 |',
        '| 기존 처마·외곽 받침 | Q37 및 Q103–Q105 상세 좌표가 없고 2단 주초/주두도 후보 단차다. 전체 외곽 공존 증거에 포함시키지 못했다. | 미확정 치수는 후속 비교/결정 필요 |',
        '', '**확정 조건 변경 없음. CONFLICT(설계 전체 불가능성)로 판정한 항목 없음.** 현재 출력은 실패를 포함한 좌표 후보와 검증 근거이며 승인 가능한 완성 배치가 아니다.',
        '계단 폭 축소, 기둥 축소, 발코니 삭제, 일반 구조의 외경 51 확장은 대안으로 실행하지 않았다.',
        'NBT·Java·월드젠·풍화·기존 구조물 파일 변경은 NOT APPLICABLE. Minecraft 런타임·시각 수용 검사는 NOT RUN이다.',
        '', '## 기준 입력 Git blob', '', *[f'- `{p}`: `{s}`' for p,s in hashes.items()],
        '', '자동 산출물은 두 번 생성하여 바이트 단위 재현성을 검사한다. 커밋·푸시 전 변경 경로가 이 검증 폴더에 한정되는지 확인한다.'])
    (out/'REPORT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')


def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--out',type=Path,default=HERE/'data')
    args=ap.parse_args();args.out.mkdir(parents=True,exist_ok=True)
    base,hashes=source_inputs()
    candidates,selected=candidate_search()
    dump(args.out/'candidate_search.json',{'candidates':candidates,'selected':selected,
         'scope':'Finite 12-member construction family, not an exhaustive impossibility proof.'})
    audit_module=module('geometry03_audit');nav=module('geometry03_navigation');render=module('geometry03_render')
    results={}
    for conv in ('C1','C2'):
        model=build(conv,base,selected);export(model,args.out)
        result=audit_module.audit(model)
        blocks={p:v for p,v in model['baseline'].items() if p not in model['void']}
        blocks.update(model['states'])
        start=(45,model['floors'][0]+1,1)
        goals=[(45,h+1,1) for h in model['floors'][1:]]
        print(conv,'independent floor navigation',flush=True)
        floor_nav=nav.Scene(blocks).search(start,goals)
        balc_goals=[]
        for b in model['balconies']:
            xs=[p[0] for p in b['clear7x3']];zs=[p[2] for p in b['clear7x3']]
            balc_goals.append((4*round(sum(xs)/len(xs))+1,b['floor_y']+1,4*round(sum(zs)/len(zs))+1))
        print(conv,'independent balcony navigation',flush=True)
        balc_nav=nav.Scene(blocks).search(start,balc_goals)
        dump(args.out/f'navigation_{conv}.json',{'floors':floor_nav,'balconies':balc_nav,
            'floor_goals':goals,'balcony_goals':balc_goals,'portal_overlay_required':True})
        result['navigation']={'floors':{k:v for k,v in floor_nav.items() if k!='witness_paths'},
                              'balconies':{k:v for k,v in balc_nav.items() if k!='witness_paths'}}
        for label,value in [('FLOOR_ACCESS_BFS',floor_nav),('BALCONY_ACCESS_BFS',balc_nav)]:
            ok=all(value['reached']) and value['start_standable']
            result['checks'].append({'id':label,'status':'PASS' if ok else 'UNVERIFIED',
                 'design_ids':['Q32','Q43','Q82'],'candidate_satisfies':ok,
                 'evidence':{k:v for k,v in value.items() if k!='witness_paths'}})
        # Control: remove arcade/rail/canopy obstacles to isolate stair/gateway construction.
        bare={p:v for p,v in model['baseline'].items() if p not in model['void']}
        bare.update(model['walk_states'])
        print(conv,'bare stair navigation control',flush=True)
        control=nav.Scene(bare).search(start,goals)
        dump(args.out/f'bare_stair_control_{conv}.json',control)
        result['bare_stair_control']={k:v for k,v in control.items() if k!='witness_paths'}
        dump(args.out/f'audit_{conv}.json',result)
        results[conv]=result
        render.render(model,args.out)
        print(conv,Counter(c['status'] for c in result['checks']),floor_nav['reached'],flush=True)
    dump(args.out/'results.json',{'base_head':BASE_HEAD,'baseline_candidate':'B25-T3','slab':1,
        'source_hashes':hashes,'collision_states':{'full':'unit full-block cube',
            'stair':'stairs[half=bottom,shape=straight,facing=<CSV facing>]'},
        **{conv:{key:results[conv][key] for key in ('checks','navigation','bare_stair_control')} for conv in ('C1','C2')},
        'overall':'UNVERIFIED','design_impossibility_proven':False,
        'NBT_Java_worldgen_weathering':'NOT APPLICABLE'})
    write_report(args.out,results,selected,hashes)
    return 0


if __name__=='__main__':
    raise SystemExit(main())
