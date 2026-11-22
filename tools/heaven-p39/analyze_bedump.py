"""P3.13: compare block-entity dumps (HeavenWorldgenProbe -Dmm.heaven.bedump) between stages.
usage: analyze_bedump.py <site> <label:file> ...   Reports, per stage: blocks that need a block entity, how many were in the chunk map BEFORE any access (inMap), how many
exist after level.getBlockEntity() (access), and for the stages after the first one whether the (position, block, nbt-hash) set equals the first stage's."""
import json, sys, collections
site = sys.argv[1]; stages = [(a.split(':', 1)[0], json.load(open(a.split(':', 1)[1]))['islands'].get(site)) for a in sys.argv[2:]]
def parse(rows):
    d = {}
    for r in rows:
        pos, blk, inmap, acc, nbt, chunk = r.split(' ', 5); d[pos] = (blk, inmap.split('=')[1], acc.split('=')[1], nbt.split('=')[1], chunk)
    return d
ref = None
for label, row in stages:
    if not isinstance(row, dict) or 'bedump' not in row: print(label, 'no bedump', str(row)[:80]); continue
    d = parse(row['bedump']); inmap = sum(v[1] == 'True' for v in d.values()); acc = sum(v[2] == 'yes' for v in d.values()); full = any(v[2] != '-' for v in d.values())
    kinds = collections.Counter(v[0] for v in d.values())
    line = f"{label:14} blocks_with_BE={len(d)} inMap_before_access={inmap} exist_after_access={acc if full else 'n/a'} kinds={dict(kinds)}"
    if full:
        if ref is None: ref = (label, d)
        else:
            same_pos = set(d) == set(ref[1]); diff = [p for p in d if p in ref[1] and (d[p][0], d[p][3]) != (ref[1][p][0], ref[1][p][3])]
            line += f" | vs {ref[0]}: same_positions={same_pos} nbt_or_block_differs={len(diff)}"
    print(line)
