"""Heaven P3.5-A: rotation support for the giant-flower part families.

Gives heaven_giant_stem / heaven_giant_leaf / heaven_giant_bloom the same horizontal `facing` the medium flowers
already use: the part tables (data/moreminecrafts/heaven/flora/<family>.json) get facing=true and the blockstates get
facing=north/east/south/west variants that reuse the SAME models rotated by y=0/90/180/270. No model, texture, block ID
or NBT changes; states without `facing` in existing NBT (P2 island, P3 templates, species structures) read as north,
i.e. exactly what they were. With facing, StructureTemplate rotation turns both the cell position (x,z)->(-z,x) and
the model (HeavenPartBlock.rotate), so a rotated flower keeps the V5 silhouette and height.
Idempotent. P2 tool output for the other families is untouched."""
import json, os
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
RES = os.path.join(ROOT, 'src/main/resources')
FACINGS = ['north', 'east', 'south', 'west']
NS = 'moreminecrafts'

for fam in ('heaven_giant_stem', 'heaven_giant_leaf', 'heaven_giant_bloom'):
    tp = f'{RES}/data/{NS}/heaven/flora/{fam}.json'
    t = json.load(open(tp)); n = t['count']
    t['facing'] = True
    with open(tp, 'w') as f: json.dump(t, f, separators=(',', ':')); f.write('\n')
    variants = {}
    for i in range(n):
        for fi, fc in enumerate(FACINGS):
            v = {'model': f'{NS}:block/heaven/{fam}/p_{i:03d}'}
            if fi: v['y'] = 90 * fi
            variants[f'facing={fc},part={i}'] = v
    bp = f'{RES}/assets/{NS}/blockstates/{fam}.json'
    with open(bp, 'w') as f: json.dump({'variants': variants}, f, separators=(',', ':')); f.write('\n')
    print(fam, n, 'parts ->', len(variants), 'variants')
