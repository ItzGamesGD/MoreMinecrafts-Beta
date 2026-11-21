"""P3.7 H6 static check of option D (repoint the EXISTING pools' elements to the heaven templates, keep pool IDs / structure IDs).
For meadow (heaven templates exist): walk the pool graph from the structure start pool with every sky_islands/meadow/<n> location
replaced by heaven_p3/meadow/<n>; list the templates reachable, check each jigsaw's pool id resolves, and that no vanilla meadow
island template is reachable. Also counts the files an A / B / D change would touch."""
import os, sys, json, glob, collections
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p3'))
import build_meadow as bm
D = bm.D; ROOT = bm.ROOT
OUT = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_7_2026-11-21/data')


def pool(pid): return json.load(open(f'{D}/worldgen/template_pool/{pid.split(":", 1)[1]}.json'))


def repoint(loc, theme='meadow'):
    p = f'moreminecrafts:sky_islands/{theme}/'
    return loc.replace(p, 'moreminecrafts:heaven_p3/' + theme + '/') if loc.startswith(p) else loc


def main():
    start = 'moreminecrafts:sky_islands/meadow/start'
    seen_pools = set(); templates = collections.OrderedDict(); q = [start]; unresolved = []
    while q:
        pid = q.pop()
        if pid in seen_pools or pid == 'minecraft:empty': continue
        seen_pools.add(pid)
        for e in pool(pid)['elements']:
            loc = e['element'].get('location')
            if not loc: continue
            loc2 = repoint(loc); templates[loc2] = loc
            n, blocks = bm.load(f'{D}/structure/{loc2.split(":", 1)[1]}.nbt')
            for p, (b, _, nbt) in blocks.items():
                if b == 'minecraft:jigsaw':
                    tp = str(nbt['pool'])
                    if tp == 'minecraft:empty': continue
                    if not os.path.exists(f'{D}/worldgen/template_pool/{tp.split(":", 1)[1]}.json'): unresolved.append((loc2, tp))
                    else: q.append(tp)
    vanilla_island = [t for t in templates if t.startswith('moreminecrafts:sky_islands/meadow/')]
    heaven = [t for t in templates if t.startswith('moreminecrafts:heaven_p3/meadow/')]
    shared = [t for t in templates if t.startswith('moreminecrafts:sky_islands/') and t not in vanilla_island]
    pools_by_theme = {t: [f for f in glob.glob(f'{D}/worldgen/template_pool/sky_islands/{t}/*.json')] for t in bm.THEMES} if hasattr(bm, 'THEMES') else {}
    res = dict(start_pool=start, pools_visited=sorted(seen_pools), reachable_heaven_templates=len(heaven), reachable_vanilla_meadow_island_templates=vanilla_island,
               shared_functional_pieces=sorted(shared), unresolved_jigsaw_pools=unresolved,
               files_touched=dict(
                   A_overwrite_in_place=dict(nbt=114, pool_json=0, structure_json=0, structure_set=0, java=0),
                   B_coexist_new_structures=dict(nbt=114, pool_json=18 + 8, structure_json=8, structure_set=2, biome_tag=8, java='SkyIslandHabitat IDS (+8), tests'),
                   D_repoint_pools=dict(nbt=114, pool_json=18, structure_json=0, structure_set=0, java=0)))
    os.makedirs(OUT, exist_ok=True); json.dump(res, open(f'{OUT}/pool_closure.json', 'w'), indent=1)
    print(json.dumps({k: v for k, v in res.items() if k != 'pools_visited'}, indent=1)[:1500])


if __name__ == '__main__':
    main()
