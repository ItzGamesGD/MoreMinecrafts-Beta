"""Heaven P3: multi-cell plant layouts for the runtime plant-instance rules (F6 / F8).

Reads the committed P2 species structures (structure/heaven_flora/{giant_*,medium_*}.nbt) and their
pivots (heaven/manifest_p2.json) and writes data/moreminecrafts/heaven/plant_layouts.json:
  every cell of a plant relative to its root (pivot), unrotated (north).  The runtime uses it to tell
  which blocks belong to ONE plant (HeavenPlants): a cell is matched only if the world holds exactly the
  expected block + part (+ facing) at the expected offset, so touching neighbours are never included.
Read-only over P2 files."""
import os, json, nbtlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
D = os.path.join(ROOT, 'src/main/resources/data/moreminecrafts')
GROUPS = {'giant': ['giant_starlotus', 'giant_lily', 'giant_daffodil'],
          'medium': [f'medium_{s}_{k}' for s in ('lavender', 'hydrangea', 'phlox') for k in (0, 1)]}


def main():
    pivots = json.load(open(f'{D}/heaven/manifest_p2.json'))['p2']['structures']
    out = []
    for group, names in GROUPS.items():
        for name in names:
            f = nbtlib.load(f'{D}/structure/heaven_flora/{name}.nbt')
            pal = [(str(p['id']), {k: str(v) for k, v in p.get('properties', {}).items()}) for p in f['palette']]
            pv = pivots[name]['pivot']
            cells = []
            for b in sorted(f['blocks'], key=lambda b: tuple(int(v) for v in b['pos'])):
                bid, props = pal[int(b['state'])]
                off = [int(b['pos'][i]) - pv[i] for i in range(3)]
                cells.append(off + [bid, int(props.get('part', 0))])
            out.append({'id': name, 'group': group, 'cells': cells})
    path = f'{D}/heaven/plant_layouts.json'
    with open(path, 'w') as fh:
        json.dump({'source': 'structure/heaven_flora (P2), offsets relative to the plant root, facing north',
                   'layouts': out}, fh, separators=(',', ':'))
        fh.write('\n')
    print(path, {l['id']: len(l['cells']) for l in out})


if __name__ == '__main__':
    main()
