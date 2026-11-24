"""P3.14: per-target integrity over a stage matrix of HeavenWorldgenProbe results (bedump=full).
usage: analyze_matrix.py <label:file> <label:file> ...   (first = reference single-stage generation)
For every site/target in the reference: family flags, unresolved jigsaws, block-entity dump equality (position + block + NBT hash) against the other stages,
loot/spawner counts, Habitat support, and which sites are missing. Only dumps taken after access (bedump=full) are compared; counts of the chunk map are NOT used (lazy init)."""
import json, sys, collections
stages = [(a.split(':', 1)[0], json.load(open(a.split(':', 1)[1]))['islands']) for a in sys.argv[1:]]
ref_label, ref = stages[0]
ok = lambda r: isinstance(r, dict) and 'fingerprint' in r
def dump(r): return {x.split(' ')[0]: (x.split(' ')[1], x.split(' ')[4]) for x in r.get('bedump', [])}
rows = collections.defaultdict(lambda: collections.Counter()); issues = []
for k, r in ref.items():
    t = k.split('#')[0]
    if not ok(r): rows[t]['sites_not_generated'] += 1; continue
    c = rows[t]; c['sites'] += 1; c['jigsaw_left'] += r['jigsaw_left']; c['blocks_with_BE'] += len(r.get('bedump', []))
    c['loot_containers'] += r['loot_table_containers']; c['habitat_ok'] += r['habitat_support']; c['standable'] += r['standable_columns']
    if r['jigsaw_left']: issues.append(f'{ref_label} {k}: jigsaw_left={r["jigsaw_left"]}')
    for lab, st in stages[1:]:
        o = st.get(k)
        if not ok(o): c[f'{lab}_missing'] += 1; issues.append(f'{lab} {k}: not generated'); continue
        a, b = dump(r), dump(o)
        if a != b: c[f'{lab}_BE_differs'] += 1; issues.append(f'{lab} {k}: BE dump differs ({len(a)} vs {len(b)})')
        else: c[f'{lab}_BE_equal'] += 1
        if o['jigsaw_left']: issues.append(f'{lab} {k}: jigsaw_left={o["jigsaw_left"]}')
        if o['box'] != r['box']: issues.append(f'{lab} {k}: box differs')
for t, c in sorted(rows.items()): print(f'{t:18}', dict(c))
print('ISSUES:', len(issues)); [print('  ', i) for i in issues[:20]]
