"""Set-based audits. Navigation is separately implemented in geometry03_navigation.py."""
from collections import Counter, deque
import itertools
import math


def connected(cells):
    remaining = set(cells)
    count = 0
    while remaining:
        count += 1
        q = [remaining.pop()]
        while q:
            x,y,z = q.pop()
            for p in ((x+1,y,z),(x-1,y,z),(x,y+1,z),(x,y-1,z),(x,y,z+1),(x,y,z-1)):
                if p in remaining:
                    remaining.remove(p);q.append(p)
    return count


def touches(a,b):
    return bool(a & b) or any((x+dx,y+dy,z+dz) in b for x,y,z in a
        for dx,dy,dz in ((1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)))


def audit(model):
    groups,states = model['groups'],model['states']
    baseline = set(model['baseline'])-model['void']
    normal = set().union(*(s for k,s in groups.items() if not k.startswith('balcony_')))
    balcony = set().union(*(s for k,s in groups.items() if k.startswith('balcony_')))
    checks = []

    def check(id,ok,evidence,ids):
        # No candidate-family failure is promoted to design-wide impossibility.
        checks.append({'id':id,'design_ids':ids,'status':'PASS' if ok else 'UNVERIFIED',
                       'candidate_satisfies':ok,'evidence':evidence})

    out45 = sorted(p for p in normal if abs(p[0])>22 or abs(p[2])>22)
    out51 = sorted(p for p in balcony if abs(p[0])>25 or abs(p[2])>25)
    out80 = sorted(p for p in normal|balcony if not 0<=p[1]<=79)
    def corner_radius(cells):
        return max(math.hypot(x+dx-.5,z+dz-.5) for x,y,z in cells for dx,dz in ((0,0),(0,1),(1,0),(1,1)))
    check('BASE_XZ_45',not out45,{'overflow':out45,'far_corner_diameter':2*corner_radius(normal),
           'measurement':'X/Z occupied columns, corner Euclidean diameter separately recorded'},['Q26','Q91'])
    rb=corner_radius(balcony)
    check('BALCONY_XZ_51',not out51 and rb<=25.5,{'overflow':out51,'far_corner_diameter':2*rb,
           'common_center_radius_excess_over_22_5':rb-22.5,'radius_excess_limit':3},['Q79','Q80'])
    loop=model['loop']
    winding=0
    for (x,z),(X,Z) in zip(loop,loop[1:]+loop[:1]):
        a,b,c,d=x+.5,z+.5,X+.5,Z+.5
        winding+=math.atan2(a*d-b*c,a*c+b*d)
    rises=[b-a for a,b in zip(model['floors'],model['floors'][1:])]
    check('STAIR_TURNS_AND_RISE',abs(winding-2*math.pi)<1e-8 and rises==[11,12,14,17],
          {'degrees_per_turn':math.degrees(winding),'horizontal_steps_per_turn':len(loop),
           'floor_blocks':model['floors'],'rise_per_turn':rises,
           'note':'Route schedule constraint; actual block-space reachability is independently checked.'},['Q25','Q28','Q29','Q30'])
    check('HEIGHT_80',not out80,{'overflow':out80,'occupied_max_y':max(p[1] for p in normal|balcony),
          'reserved_sanctum_apex_y':79,'apex_not_built':True},['Q27','Q28','Q41'])
    stairs = groups['stairs'] | groups['bridge']
    stair_wall = sorted(stairs & baseline)
    # Slab-on-slab at the same room floor is an intended bridge contact.
    contacts = [p for p in stair_wall if p[1] in model['floors'] and p in groups['bridge']]
    blockers = [p for p in stair_wall if p not in set(contacts)]
    check('STAIR_WALL',not blockers,{'blocking_overlap':blockers,'intentional_floor_contact':contacts,
         'portal_void_count':len(model['void'])},['Q32','Q43','Q110'])
    width_rows = []
    obstacles = set().union(*(s for k,s in groups.items() if k not in ('stairs','bridge','balcony_floor')))
    all_obstacles = obstacles | baseline
    for s in model['sections']:
        blocked = []
        heights = []
        for x,y,z in s['floor_cells']:
            heights.append(y)
            hit = [(x,Y,z) for Y in (y+1,y+2) if (x,Y,z) in all_obstacles]
            if hit:
                blocked.append({'floor':[x,y,z],'obstruction':hit,
                                'groups':sorted(k for k,cells in groups.items() if any(tuple(p) in cells for p in hit))})
        width_rows.append({**s,'clear_columns':4-len(blocked),'floor_height_spread':max(heights)-min(heights),
                           'blocked':blocked})
    check('FULL_WIDTH_4',all(r['clear_columns']==4 and r['floor_height_spread']==0 for r in width_rows),
          {'histogram':dict(sorted(Counter(r['clear_columns'] for r in width_rows).items())),
           'uneven_sections':sum(r['floor_height_spread']!=0 for r in width_rows),
           'first_failed':next((r for r in width_rows if r['clear_columns']<4 or r['floor_height_spread']),None),
           'note':'Four contiguous axis-perpendicular columns at every route station; actual player BFS is separate.'},['Q43','Q98'])
    head_air = {(x,Y,z) for x,y,z in stairs for Y in (y+1,y+2)}
    for group,id,qs in [('pillar','STAIR_PILLAR',['Q43','Q98']),('rail','STAIR_RAIL',['Q31','Q43'])]:
        hits = sorted(groups[group] & head_air)
        check(id,not hits,{'obstruction_voxels':hits},qs)
    # Pair intersections are retained regardless of whether stone contacts may be intentional.
    intersections = {}
    for a,b in itertools.combinations(sorted(groups),2):
        hits = groups[a] & groups[b]
        if hits:
            intersections[a+'__'+b] = sorted(hits)
    by_id = {p['id']:set(map(tuple,p['cells'])) for p in model['pillars']}
    arch_rows = []
    for a in model['arches']:
        cells = set(map(tuple,a['cells']))
        arch_rows.append({'from':a['from'],'to':a['to'],'components_face':connected(cells),
            'touch_from':touches(cells,by_id[a['from']]),'touch_to':touches(cells,by_id[a['to']]),
            'spring_y':a['spring_y'],'apex_y':a['apex_y']})
    check('PILLAR_ARCH_CONNECTION',all(r['components_face']==1 and r['touch_from'] and r['touch_to'] for r in arch_rows),
          arch_rows,['Q86','Q87','Q88'])
    links=[]
    for obj in model['links']:
        cells=set(map(tuple,obj['cells']))
        links.append({'lap':obj['lap'],'station':obj['station'],'touch_wall':touches(cells,baseline),
                      'touch_pillar':touches(cells,groups['pillar']),'components_face':connected(cells)})
    check('WALL_LINK_SUPPORT_CONNECTION',all(r['touch_wall'] and r['touch_pillar'] and r['components_face']==1 for r in links),
          links,['Q99','Q100','Q103','Q105'])
    # Minimum vertical arch/canopy clearance over each real tread column (top surface is y+1).
    clearance = []
    overhead = defaultdict_list(groups['arch'] | groups['canopy'])
    for x,y,z in sorted(stairs):
        above = [Y for Y in overhead.get((x,z),[]) if Y>y]
        if above:
            Y = min(above)
            clearance.append({'tread':[x,y,z],'lowest_overhead_y':Y,'clearance':Y-y-1,
                               'obstruction_groups':[k for k in ('arch','canopy') if (x,Y,z) in groups[k]]})
    check('ARCH_CANOPY_CLEARANCE',all(r['clearance']>=2 for r in clearance),
          {'minimum':min((r['clearance'] for r in clearance),default=None),
           'under2': [r for r in clearance if r['clearance']<2]},['Q86','Q89','Q108'])
    joints = []
    for link,canopy in zip(model['links'],model['canopies']):
        L,C = set(map(tuple,link['cells'])),set(map(tuple,canopy['cells']))
        joints.append({'lap':link['lap'],'station':link['station'],'touch':touches(L,C),
                       'shared':sorted(L&C),'molding':link['molding'],
                       'note':'Masonry contact is intentional; tread headspace audited separately.'})
    check('ARCH_CANOPY_JOINT',all(r['touch'] for r in joints),joints,['Q108','Q109'])
    balcony_rows = []
    for b in model['balconies']:
        clear = set(map(tuple,b['clear7x3']))
        volume = {(x,Y,z) for x,y,z in clear for Y in (y+1,y+2)}
        hits = volume & (obstacles | baseline)
        connector = set(map(tuple,b['connector']))
        floor = set(map(tuple,b['floor']))
        other_floor_hits = [(x,y,z) for x,y,z in connector if any((x,Y,z) in stairs for Y in (y-1,y+1))]
        balcony_rows.append({'id':b['id'],'clear_count':len(clear),'blocked_air':sorted(hits),
            'intrusion_by_group':{k:sorted(volume&cells) for k,cells in groups.items() if volume&cells},
            'connector_touches_stair':touches(connector,stairs),
            'floor_connected':connected(floor)==1,'connector_other_level_floor':other_floor_hits,
            'rail_intrusion':sorted(volume & groups['balcony_rail'])})
    check('BALCONY_CLEAR_7X3',all(r['clear_count']==21 and not r['blocked_air'] for r in balcony_rows),
          balcony_rows,['Q74','Q77','Q80'])
    check('BALCONY_FLOOR_CONNECTION',all(r['connector_touches_stair'] and r['floor_connected'] for r in balcony_rows),
          {'note':'Touching floor sets alone does NOT imply navigability; separate BFS includes each clear centre.',
           'balconies':balcony_rows},['Q73','Q82'])
    reserved = {k:{'motif_count':len(v['motif']),'pilaster_count':len(v['pilaster']),
                  'air_overlap':sorted((set(map(tuple,v['motif'])) | set(map(tuple,v['pilaster']))) & head_air),
                  'apex_y':v['arch_apex_y'],'actual_pattern':None} for k,v in model['reservations'].items()}
    check('GATE_RESERVED_SPACE',all(not v['air_overlap'] for v in reserved.values()),reserved,['Q110','Q112','Q113','Q118'])
    # Equality requires a genuinely associated arcade arch; arbitrary matching numbers are insufficient.
    check('GATE_ARCADE_APEX_AND_CAPITAL',False,
          {'gate_apexes':[v['arch_apex_y'] for v in model['reservations'].values()],
           'wall_link_springs':[v['spring_y'] for v in model['links']],
           'pillar_capital_first_tiers':[p['capital_tiers_y'][0] for p in model['pillars']],
           'reason':'No matched gate-neighbour arch/capital association has been proven.'},['Q107','Q111'])
    spacing = []
    for a,b in zip(model['pillars'],model['pillars'][1:]):
        if a['lap'] == b['lap']:
            x,z=a['center_xz'];X,Z=b['center_xz']
            spacing.append({'from':a['id'],'to':b['id'],'nominal_path_steps':b['station']-a['station'],
                            'actual_center_distance':math.hypot(X-x,Z-z)})
    check('PILLAR_SPACING_ABOUT7',False,
          {'nominal_steps':sorted({r['nominal_path_steps'] for r in spacing}),
           'actual_center_distance_range':[min(r['actual_center_distance'] for r in spacing),max(r['actual_center_distance'] for r in spacing)],
           'pairs':spacing,'reason':'Approximate tolerance and arc-path projection of square bodies unapproved; distances reported, not rounded to PASS.'},['Q84','Q85'])
    check('PATTERN_AND_MATERIAL_DETAIL',False,'Gateway motifs remain reservations; two-tier stair-block profiles/material states and perceptual half-circle shape require further validation.', ['Q93','Q94','Q95','Q96','Q97','Q113','Q118'])
    return {'checks':checks,'width_sections':width_rows,'pairwise_intersections':intersections,
            'arch_clearances':clearance,'global_verdict':'UNVERIFIED',
            'design_impossibility_proven':False,'candidate_violations_are_not_global_CONFLICT':True}


def defaultdict_list(cells):
    out = {}
    for x,y,z in cells:
        out.setdefault((x,z),[]).append(y)
    return out
