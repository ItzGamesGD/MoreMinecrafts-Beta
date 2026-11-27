"""Report exploratory clearance cores; no extrapolation to actual colony frequency."""
import json, sys, re
from collections import Counter
from pathlib import Path
from dedup import merge_overlapping_cores
from structures import intersects, expand_box

for path in sorted(Path(sys.argv[1]).glob('*.json.analysis.json')):
    data=json.loads(path.read_text())
    meta=json.loads(Path(str(path).removesuffix('.analysis.json')).read_text())
    cores=merge_overlapping_cores(data['cores'])
    boxes=[]
    for item in meta['structure_starts']:
        m=re.search(r'minX=(-?\d+), minY=(-?\d+), minZ=(-?\d+), maxX=(-?\d+), maxY=(-?\d+), maxZ=(-?\d+)',item)
        if m: boxes.append(tuple(map(int,m.groups())))
    overlaps=sum(any(intersects((c['point'][0],c['point'][2],c['point'][1]),c['diameter_blocks']/2,expand_box(b,meta.get('structure_buffer_blocks_PROVISIONAL',0))) for b in boxes) for c in cores)
    groups=Counter(c['body_path_component'] for c in cores if c['body_path_component'])
    print(json.dumps({'seed':data['seed'], 'core_count_after_overlap_merge':len(cores),
          'core_diameters': [round(c['diameter_blocks'],1) for c in cores],
          'body_path_groups_of_2_or_more':sum(v>=2 for v in groups.values()),
          'core_without_body_path':sum(c['body_path_component']==0 for c in cores),
          'cores_intersecting_known_structure_boxes':overlaps, 'fluid_cells':data['fluid_cells'], 'chunk_ms_total':round(meta['chunk_request_total_ms'],1),
          'chunk_ms_p95':round(meta['chunk_request_ms_p95'],1)}))
