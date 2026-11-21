"""Heaven P3.5-E: forest 19-template preparation (read only, nothing generated).

For every forest template: full palette, terrain-id role (P2.5 depth roles), vanilla vegetation, building blocks and
terrain-id blocks inside buildings (the P3 column rule), jigsaws (pool, final_state), and a geometric comparison with
the meadow template of the same name (are the islands the same shape with different blocks?). Then a DRY RUN of the
P3 meadow generator with the forest palette CANDIDATES (owner decision pending) to estimate plantable area, giants
(F10 trial formula, not final), trees, vegetation and unplaced reasons. Writes
docs/design/heaven_sky_island_p3_5_2026-11-21/data/forest_prep.json."""
import os, sys, json, collections, time
HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, os.path.join(HERE, '..', 'heaven-p3'))
import build_meadow as bm          # noqa: E402

ROOT = bm.ROOT; D = bm.D; NS = bm.NS
OUT = os.path.join(ROOT, 'docs/design/heaven_sky_island_p3_5_2026-11-21/data')
P25 = os.path.join(ROOT, 'docs/design/heaven_sky_island_p2_5_2026-11-20/data/template_survey.json')
VEG_KEYS = ('leaves', 'short_grass', 'tall_grass', 'fern', 'poppy', 'daisy', 'bluet', 'allium', 'dandelion', 'cornflower', 'tulip', 'dead_bush',
            '_log', 'sapling', 'mushroom', 'vine', 'lily', 'sweet_berry', 'moss_carpet', 'flower', 'bush', 'snow', 'leaf_litter', 'petals')
# CANDIDATE palettes for the dry run (NOT approved): A = P1 meadow slots + podzol/mossy as loam/stone; B = keep forest accents vanilla
CANDIDATES = {
    'A_all_heaven': {'minecraft:grass_block': f'{NS}:heaven_grass_block', 'minecraft:dirt': f'{NS}:heaven_loam', 'minecraft:podzol': f'{NS}:heaven_loam',
                     'minecraft:coarse_dirt': f'{NS}:heaven_loam', 'minecraft:stone': f'{NS}:heaven_cream_stone', 'minecraft:andesite': f'{NS}:heaven_cream_stone_pale',
                     'minecraft:tuff': f'{NS}:heaven_cream_stone_pale', 'minecraft:mossy_cobblestone': f'{NS}:heaven_cream_stone_pale'},
    'B_keep_forest_accents': {'minecraft:grass_block': f'{NS}:heaven_grass_block', 'minecraft:dirt': f'{NS}:heaven_loam',
                              'minecraft:stone': f'{NS}:heaven_cream_stone', 'minecraft:andesite': f'{NS}:heaven_cream_stone_pale',
                              'minecraft:podzol': 'minecraft:podzol', 'minecraft:mossy_cobblestone': 'minecraft:mossy_cobblestone'},   # kept as terrain
}


def is_veg(bid): return any(k in bid.split(':', 1)[1] for k in VEG_KEYS) and bid != 'minecraft:snow_block'


def main():
    roles = json.load(open(P25))['theme_roles']['forest']['blocks']
    rows = {}; palette_all = collections.Counter(); veg_all = collections.Counter()
    for name in bm.NAMES:
        nf, f = bm.load(f'{D}/structure/sky_islands/forest/{name}.nbt')
        nm, m = bm.load(f'{D}/structure/sky_islands/meadow/{name}.nbt')
        c = collections.Counter(b for b, _, _ in f.values()); palette_all.update(c)
        veg = {k: v for k, v in c.items() if is_veg(k)}; veg_all.update(veg)
        solid_f = {p for p, (b, _, _) in f.items() if not is_veg(b) and b not in ('minecraft:air',)}
        solid_m = {p for p, (b, _, _) in m.items() if not bm.VANILLA_VEG.__contains__(b) and b not in ('minecraft:air',)}
        jig = [dict(pos=list(p), orientation=pr.get('orientation'), pool=str(n['pool']), final_state=str(n['final_state']))
               for p, (b, pr, n) in f.items() if b == 'minecraft:jigsaw']
        rows[name] = dict(size=[int(v) for v in nf['size']], same_size_as_meadow=[int(v) for v in nf['size']] == [int(v) for v in nm['size']],
                          non_vegetation_positions=len(solid_f), meadow_non_vegetation_positions=len(solid_m),
                          shape_identical_to_meadow=solid_f == solid_m, shape_jaccard=round(len(solid_f & solid_m) / max(1, len(solid_f | solid_m)), 4),
                          vegetation=veg, jigsaws=jig)
    out = dict(theme_roles=roles, palette=dict(palette_all.most_common()), vegetation=dict(veg_all.most_common()), templates=rows)
    species = bm.species_structures(); pools = bm.attached_footprints()
    for cand, slot in CANDIDATES.items():
        t0 = time.time(); est = {}
        for name in bm.NAMES:
            _, blocks = bm.load(f'{D}/structure/sky_islands/forest/{name}.nbt')
            vv = {b for _, (b, _, _) in blocks.items() if is_veg(b)}
            isl = bm.Island(name, pools, species, theme='forest', slot=slot, veg=vv).build()
            left = collections.Counter(b for b, _, _ in isl.world.values() if b.startswith('minecraft:') and b not in ('minecraft:water', 'minecraft:jigsaw', 'minecraft:air'))
            est[name] = dict(plantable=int(isl.plant.sum()), grass=int(isl.grass.sum()), giant_requested=isl.giant_req, giant_placed=len(isl.giants),
                             trees=len(isl.trees), vegetation=len(isl.veg), terrain_converted=isl.stats['terrain_converted'],
                             terrain_id_kept_as_structure=isl.stats['terrain_id_kept_as_structure'], vanilla_blocks_left=dict(left.most_common(8)),
                             jigsaw_final_state_mapped=isl.stats['jigsaw_final_state_mapped'], unplaced=dict(isl.reasons),
                             survival_violations=isl.survival_violations(), walkable=isl.walkability()['walkable_share'], seconds=round(time.time() - t0, 1))
        out[f'dry_run_{cand}'] = dict(palette=slot, templates=est, seconds=round(time.time() - t0, 1),
                                      totals={k: sum(e[k] for e in est.values()) for k in ('plantable', 'giant_requested', 'giant_placed', 'trees', 'vegetation')})
        print(cand, out[f'dry_run_{cand}']['totals'], out[f'dry_run_{cand}']['seconds'], 's')
    os.makedirs(OUT, exist_ok=True)
    json.dump(out, open(f'{OUT}/forest_prep.json', 'w'), indent=1)
    print('palette', out['palette']); print('vegetation', out['vegetation'])
    for k, r in rows.items(): print(k, r['same_size_as_meadow'], r['shape_identical_to_meadow'], r['shape_jaccard'], len(r['jigsaws']))


if __name__ == '__main__':
    main()
