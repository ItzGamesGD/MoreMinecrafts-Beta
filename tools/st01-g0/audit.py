#!/usr/bin/env python3
"""Read-only G0 authority/geometry diagnostics; writes JSON/Markdown only, never NBT. Python stdlib only."""
import argparse, gzip, hashlib, json, struct
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
BASE = '03685656f72faf7cd0f1a931dc7519ebc31b7378'
D1_HASH = '7bdc1d09858c619b2be1f09b3874eec816354a142a032df3a7f6060a69d89423'

def nbt(raw):
    data = gzip.decompress(raw); offset = 0
    def read(n):
        nonlocal offset
        value = data[offset:offset+n]; offset += n
        if len(value) != n: raise ValueError('Truncated NBT')
        return value
    def number(fmt): return struct.unpack('>'+fmt, read(struct.calcsize('>'+fmt)))[0]
    def string(): return read(number('H')).decode()
    def tag(t):
        if t in range(1, 7): return number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t == 8: return string()
        if t == 9:
            kind, count = number('B'), number('i'); return [tag(kind) for _ in range(count)]
        if t == 10:
            result = {}
            while True:
                kind = number('B')
                if kind == 0: return result
                name = string(); result[name] = tag(kind)
        if t in (7,11,12): return [number({7:'b',11:'i',12:'q'}[t]) for _ in range(number('i'))]
        raise ValueError(t)
    kind = number('B'); string(); return tag(kind)

def cells(box):
    a,b,c,d,e,f = box
    for x in range(a,b+1):
        for y in range(c,d+1):
            for z in range(e,f+1): yield x,y,z

def rotate(p, r):
    x,y,z = p
    return [(x,y,z),(-z,y,x),(-x,y,-z),(z,y,-x)][r]

def main(out):
    folder = ROOT/'docs/design/st01-p6/generated'
    east = json.loads((folder/'p6_model_east.json').read_text())
    west = json.loads((folder/'p6_model_west.json').read_text())
    raw = (ROOT/'src/main/resources/data/moreminecrafts/structure/pyramid/royal_hall.nbt').read_bytes()
    parsed = nbt(raw)
    actual = {}
    for block in parsed['blocks']:
        p = tuple(a+b for a,b in zip(block['pos'],(-40,-24,2)))
        actual[p] = parsed['palette'][block['state']]
    cover = {tuple(p) for p in east['cover_cells']}
    conflicts = {}
    for element in east['elements']:
        if not element['id'].startswith('P4:'): continue
        hit = sorted({p for box in element['boxes'] for p in cells(box)
                      if p in actual and p not in cover and actual[p]['id'] not in ('minecraft:air','minecraft:cave_air','minecraft:void_air')})
        if hit: conflicts[element['id']] = {'count':len(hit),'cells':hit}
    def get(model, name): return next(e for e in model['elements'] if e['id'] == name)
    matrix = []
    anchor = (-21,-20,33)
    for r, name in enumerate(('SOUTH','WEST','NORTH','EAST')):
        # Authored entrance is +Z = SOUTH; Minecraft's CW90 maps +Z to -X.
        for secret, model in [('EAST',east),('WEST',west)]:
            b = get(model, 'P3:CHEST_PRIMARY')['bbox']
            source_anchor = (b[0],b[2],b[4])
            matrix.append({'whole_rotation':name,'quarter_turns_clockwise':r,'secret_source':secret,
                           'required_anchor':rotate(anchor,r),'source_anchor':rotate(source_anchor,r),
                           'anchor_compatible':source_anchor == anchor})
    sources = ['docs/design/st01-p6/generated/p6_model_east.json',
               'docs/design/st01-p6/generated/p6_model_west.json',
               'docs/design/st01-p6/generated/p6_p2c_overlay.json',
               'tools/st01-p1/st01_p1_model.py','tools/st01-p4/p4_spec.py']
    components = east['elements']
    bounds_ok = all(all(b[i] <= b[i+1] for i in (0,2,4)) for e in components for b in e['boxes'])
    unique_ok = len({e['id'] for e in components}) == len(components)
    rot_ok = all(rotate(rotate(p,r),(4-r)%4) == p for p in [(-40,-32,-40),(40,39,40),anchor] for r in range(4))
    assert bounds_ok and unique_ok and rot_ok
    checks = [
        {'id':'component_bounds','status':'PASS' if bounds_ok else 'FAIL'},
        {'id':'unique_component_ids','status':'PASS' if unique_ok else 'FAIL'},
        {'id':'rotation_inverse','status':'PASS' if rot_ok else 'FAIL'},
        {'id':'d1_integrity','status':'PASS' if hashlib.sha256(raw).hexdigest()==D1_HASH else 'FAIL'},
        {'id':'d1_secret_interface','status':'FAIL' if conflicts else 'PASS'},
        {'id':'independent_secret_orientation','status':'FAIL' if any(not m['anchor_compatible'] for m in matrix) else 'PASS'},
        {'id':'full_voxel_overlaps','status':'NOT RUN'},
        {'id':'entrance_to_hall_navigation','status':'NOT RUN'},
        {'id':'secret_concealment_and_access','status':'NOT RUN'},
        {'id':'full_envelope_and_reservation','status':'NOT RUN'},
        {'id':'ceiling_and_seam_clearance','status':'NOT RUN'}]
    report = {'checks':checks,'status' :'G0 BLOCKED','base':BASE,'d1_sha256':hashlib.sha256(raw).hexdigest(),
              'd1_hash_pass':hashlib.sha256(raw).hexdigest()==D1_HASH,
              'd1_size':parsed['size'],'block_records':len(parsed['blocks']),
              'd1_entities':len(parsed['entities']), 'data_version':parsed['DataVersion'],
              'sources':{p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in sources},
              'd1_vs_secret_cavity_conflicts_excluding_cover':conflicts,
              'matrix':matrix,
              'limitations':['Matrix diagnoses existing source variants, not a newly verified full assembly.',
                              'Solid intersection is a block-coordinate conflict; no material-specific navigation inference.',
                              'No complete materialized blueprint or traversal validation is claimed.']}
    proposal = {'status':'PROPOSED_NOT_APPROVED','base':BASE,
                'coordinates':'inclusive [xmin,xmax,ymin,ymax,zmin,zmax]; +X east,+Z south,+Y up; origin ground centre',
                'source_components':east['elements'],
                'excluded_from_construction':[e['id'] for e in east['elements'] if e['id'].startswith('P3:') or e['id']=='P1R:S07'],
                'exclusion_reason':'P3 reconstruction is replaced by immutable D1; old secondary placeholder must never be placed; S07 is reservation only.',
                'adoption':'Freeze P1/P2 representative coordinates and P6 candidate-C overlay only after owner approval; D1 replaces all P3 reconstruction.',
                'crypt_proposal':'Adopt current representative 15x13x7 cavity and six niches; remains owner decision.',
                'secret_west':'UNRESOLVED: existing WEST source moves D1; no automatic mirroring permitted.',
                'shell_proposal':{'layers':'y=0..39; abs(x),abs(z)<=40-y; carve only explicitly specified cavities','base_bounds':[-40,40,-32,-1,-40,40]},
                'material_proposal':{'mass':'minecraft:sandstone','columns':'minecraft:chiseled_sandstone',
                    'floor':'minecraft:smooth_sandstone','inlay':'minecraft:cut_sandstone',
                    'air':'minecraft:air','sarcophagus':'minecraft:cut_sandstone',
                    'stair_policy':'full-block stepped treads from source boxes; no implicit slab or stair substitution',
                    'block_entities':'D1 verbatim; non-D1 chest/loot/pile conversion unresolved',
                    'decoration':'No random decoration proposed; exact fixtures and region mapping remain incomplete'}}
    out.mkdir(parents=True,exist_ok=True)
    (out/'validation.json').write_text(json.dumps(report,indent=2)+'\n')
    (out/'candidate-components.json').write_text(json.dumps(proposal,indent=2)+'\n')
    lines = ['# G0 component coordinate inventory','', 'All source statuses are retained, not promoted. See G0-A for authority corrections.', '', '| ID | Kind | Inclusive bounding box X/Y/Z | Source status |','|---|---|---|---|']
    for e in east['elements']:
        lines.append(f"| {e['id']} | {e['kind']} | {e['bbox']} | {e['status']} |")
    (out/'COMPONENTS.md').write_text('\n'.join(lines)+'\n')
    print(json.dumps({'d1_hash_pass':report['d1_hash_pass'],'components':len(east['elements']),
                      'conflicts':{k:v['count'] for k,v in conflicts.items()},
                      'source_matrix_compatible':sum(m['anchor_compatible'] for m in matrix),
                      'source_matrix_total':len(matrix)}))

if __name__ == '__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path,required=True)
    main(parser.parse_args().out)
