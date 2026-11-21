"""Heaven P3.8: production-path heaven templates for meadow and forest (H1=D, H6, H4=A, H5=forest density).

  meadow  structure/sky_islands/heaven/meadow/*.nbt  = the P3 heaven templates (V5 tree density 2/807, spacing 8); regenerated
          here and required to be byte-identical to structure/heaven_p3/meadow/* (which stays untouched).
  forest  structure/sky_islands/heaven/forest/*.nbt  generated from the forest ORIGINALS: palette A (every forest terrain block
          -> heaven; podzol -> loam, mossy cobblestone -> pale cream stone), forest density (6 trees / 807 plantable columns,
          spacing 5). The four representative templates must be byte-identical to the approved P3.7 comparison (a_forest).
Preserved: size, terrain shape, buildings, water, jigsaw position / orientation / NBT (only grass / dirt final_state mapped).
Jigsaw `pool` values are NOT changed: option D keeps the pool IDs and repoints the pools' elements (see pending_pools.py).
Nothing in src/main/resources/data worldgen references these templates yet."""
import os, sys, json, hashlib, collections, time
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p3')); sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p37'))
import build_meadow as bm          # noqa: E402
import forest_compare as fc        # noqa: E402

ROOT = bm.ROOT; D = bm.D
OUT = f'{D}/structure/sky_islands/heaven'
DOC = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_8_2026-11-21/data')
FOREST_DENSITY = dict(tree_rate=6.0, tree_spacing=5)
sha = lambda p: hashlib.sha256(open(p, 'rb').read()).hexdigest()


def main():
    species = bm.species_structures(); pools = bm.attached_footprints()
    report = {}
    for theme in ('meadow', 'forest'):
        rows = {}
        for name in bm.NAMES:
            t0 = time.time()
            _, blocks = bm.load(f'{D}/structure/sky_islands/{theme}/{name}.nbt')
            if theme == 'meadow':
                isl = bm.Island(name, pools, species).build()
            else:
                isl = bm.Island(name, pools, species, theme='forest', slot=fc.P['A'], veg={b for _, (b, _, _) in blocks.items() if fc.is_veg(b)}).build(**FOREST_DENSITY)
            bm.OUT = f'{OUT}/{theme}'
            path, pal = isl.write()
            if theme == 'meadow':
                assert sha(path) == sha(f'{D}/structure/heaven_p3/meadow/{name}.nbt'), f'meadow/{name} differs from the P3 template'
            cmp = f'{ROOT}/src/gametest/resources/data/moreminecrafts-gametest/structure/heaven_forest_cmp/a_forest/{name}.nbt'
            if theme == 'forest' and os.path.exists(cmp): assert sha(path) == sha(cmp), f'forest/{name} differs from the approved P3.7 comparison'
            left = collections.Counter(b for b, _, _ in isl.world.values() if b.startswith('minecraft:') and b not in ('minecraft:water', 'minecraft:jigsaw', 'minecraft:air'))
            fam = collections.Counter(v[2] for v in isl.veg.values())
            rows[name] = dict(size=isl.size, source_sha256=sha(f'{D}/structure/sky_islands/{theme}/{name}.nbt'), output=os.path.relpath(path, ROOT), output_sha256=sha(path),
                              output_bytes=os.path.getsize(path), palette=pal, plantable=int(isl.plant.sum()), grass=int(isl.grass.sum()),
                              giants=dict(requested=isl.giant_req, placed=len(isl.giants)), trees=dict(requested=isl.tree_req, placed=len(isl.trees)),
                              vegetation=len(isl.veg), by_family=dict(fam), unplaced=dict(isl.reasons), survival_violations=isl.survival_violations(),
                              walkable=isl.walkability()['walkable_share'], conversion=dict(isl.stats), vanilla_blocks_left=dict(left.most_common(10)),
                              jigsaws=[dict(pos=list(p), pool=str(n['pool']), final_state=str(n['final_state'])) for p, _, n in isl.jigsaws],
                              seconds=round(time.time() - t0, 2))
        report[theme] = rows
        tot = lambda k: sum(r[k] if not isinstance(r[k], dict) else r[k]['placed'] for r in rows.values())
        print(theme, 'giants', tot('giants'), 'trees', tot('trees'), 'vegetation', tot('vegetation'), 'violations', sum(sum(r['survival_violations'].values()) for r in rows.values()))
    os.makedirs(DOC, exist_ok=True)
    json.dump(report, open(f'{DOC}/heaven_production.json', 'w'), indent=1)


if __name__ == '__main__':
    main()
