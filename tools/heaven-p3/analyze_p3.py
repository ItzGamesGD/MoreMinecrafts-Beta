"""Heaven P3-B / P3-E analysis (read only; writes docs/design/heaven_sky_island_p3_2026-11-20/data/*.json).

1. jigsaw_recheck.json      every jigsaw of every sky-island / sky-tower template re-read from the NBT and compared with
                            the P2.5 inventory (90 air / 30 vanilla terrain / 8 stone), plus the meadow P3 mapping result.
2. structure_protection.json per theme: building blocks of the 19 island templates and of the attached pieces, the
                            terrain-id blocks that belong to a building (column rule), and why P2.5 counted 0 for ruins /
                            temple (their buildings are separate jigsaw pieces).
3. expansion_estimate.json  DRY RUN of the P3 placement on the 95 non-meadow templates with an ASSUMED role-based palette
                            (surface -> heaven grass, subsurface -> loam, rock -> cream stone). Nothing is written to the
                            resources; the palette per theme is an owner decision (see the P3 report, E)."""
import os, sys, json, glob, collections, time
import nbtlib

HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import build_meadow as bm                       # noqa: E402

ROOT = bm.ROOT; D = bm.D; NS = bm.NS
OUT = bm.REPORT
THEMES = ['meadow', 'forest', 'snowy', 'desert', 'badlands', 'rocky']
P25 = os.path.join(ROOT, 'docs/design/heaven_sky_island_p2_5_2026-11-20/data')
VEG_KEYS = ('leaves', 'short_grass', 'tall_grass', 'fern', 'poppy', 'daisy', 'bluet', 'allium', 'dandelion', 'cornflower', 'tulip', 'dead_bush',
            'cactus', '_log', 'sapling', 'mushroom', 'vine', 'lily', 'kelp', 'seagrass', 'sweet_berry', 'moss_carpet', 'flower', 'snow')


def is_veg(bid): return any(k in bid.split(':', 1)[1] for k in VEG_KEYS) and bid not in ('minecraft:snow_block',)


def jigsaw_recheck():
    files = sorted(glob.glob(f'{D}/structure/sky_islands/**/*.nbt', recursive=True) + glob.glob(f'{D}/structure/sky_tower/**/*.nbt', recursive=True))
    rows = []
    for f in files:
        rel = os.path.relpath(f, f'{D}/structure')[:-4]
        n, blocks = bm.load(f)
        for p, (bid, props, nbt) in blocks.items():
            if bid != 'minecraft:jigsaw': continue
            fs = str(nbt['final_state'])
            cls = 'air' if fs == 'minecraft:air' else 'structure_stone' if fs in ('minecraft:stone_bricks', 'minecraft:smooth_quartz') else 'vanilla_terrain'
            rows.append(dict(template=rel, pos=list(p), orientation=props.get('orientation'), pool=str(nbt['pool']), final_state=fs, cls=cls))
    p25 = json.load(open(f'{P25}/jigsaw_inventory.json'))['jigsaws']
    p25_cls = collections.Counter('air' if j['final_state'] == 'minecraft:air' else 'structure_stone' if j['final_state'] in ('minecraft:stone_bricks', 'minecraft:smooth_quartz') else 'vanilla_terrain' for j in p25)
    key = lambda j: (j['template'], tuple(j['pos']))
    same = {key(j) for j in rows} == {key(j) for j in p25}
    meadow = []
    for name in bm.NAMES:
        _, o = bm.load(f'{D}/structure/sky_islands/meadow/{name}.nbt'); _, h = bm.load(f'{D}/structure/heaven_p3/meadow/{name}.nbt')
        for p, (bid, props, nbt) in o.items():
            if bid != 'minecraft:jigsaw': continue
            hb = h[p]
            diff = sorted(k for k in set(nbt) | set(hb[2]) if str(nbt.get(k)) != str(hb[2].get(k)))
            meadow.append(dict(template=name, pos=list(p), orientation=props.get('orientation'), heaven_orientation=hb[1].get('orientation'),
                               pool=str(nbt['pool']), final_state=str(nbt['final_state']), heaven_final_state=str(hb[2]['final_state']), changed_keys=diff))
    return dict(total=len(rows), by_class=dict(collections.Counter(j['cls'] for j in rows)), p25_by_class=dict(p25_cls), same_positions_as_p25=same,
                by_final_state=dict(collections.Counter(j['final_state'] for j in rows).most_common()), meadow_p3=meadow, jigsaws=rows)


def theme_slots():
    roles = json.load(open(f'{P25}/template_survey.json'))['theme_roles']
    out = {}
    for th in THEMES:
        m = {}
        for b, r in roles[th]['blocks'].items():
            m[b] = {'surface': bm.GRASS, 'subsurface': f'{NS}:heaven_loam', 'rock': f'{NS}:heaven_cream_stone'}[r['role']]
        out[th] = m
    out['meadow'] = dict(bm.SLOT)              # the approved P1 map (pale stone for andesite / tuff)
    return out


def structure_protection(slots):
    out = {}
    attached = collections.Counter()
    for f in ['ruins_a', 'ruins_b', 'ruins_c', 'ruins_d', 'temple', 'tower_5', 'tower_6', 'tower_7']:
        _, blocks = bm.load(f'{D}/structure/sky_islands/{f}.nbt')
        for _, (bid, _, _) in blocks.items():
            if bid not in ('minecraft:jigsaw', 'minecraft:air'): attached[bid] += 1
    for th in THEMES:
        terr = set(slots[th]); kinds = collections.Counter(); per = {}
        for name in bm.NAMES:
            _, blocks = bm.load(f'{D}/structure/sky_islands/{th}/{name}.nbt')
            cols = collections.defaultdict(list)
            for p, (bid, _, _) in blocks.items(): cols[(p[0], p[2])].append((p[1], bid))
            surface = [k for k, v in slots[th].items() if v == bm.GRASS]
            gt = {c: max(y for y, b in l if b in surface) for c, l in cols.items() if any(b in surface for _, b in l)}
            top_grass = max(gt.values()) if gt else -1
            sm = {}
            for c, l in cols.items():
                ys = [y for y, b in l if b not in terr and not is_veg(b) and b not in ('minecraft:water', 'minecraft:jigsaw', 'minecraft:air')]
                if ys: sm[c] = min(ys)
            building = collections.Counter(); kept_terrain = collections.Counter()
            for p, (bid, _, _) in blocks.items():
                c = (p[0], p[2])
                if bid in terr:
                    if (c in gt and p[1] > gt[c]) or (c in sm and p[1] > sm[c]) or (sm and p[1] > top_grass): kept_terrain[bid] += 1
                elif not is_veg(bid) and bid not in ('minecraft:water', 'minecraft:jigsaw', 'minecraft:air'):
                    building[bid] += 1
            kinds.update(building); kinds.update(kept_terrain)
            if building or kept_terrain:
                per[name] = dict(building_blocks=sum(building.values()), terrain_id_in_building=dict(kept_terrain), top=dict(building.most_common(8)))
        out[th] = dict(protected_block_ids=sorted(kinds), templates_with_buildings=per)
    out['attached_pieces_all_themes'] = dict(blocks=dict(attached.most_common()),
        note='ruins_a-d, temple and tower_5-7 are shared by all themes and attached by jigsaw; P2.5 surveyed only the island templates, '
             'so meadow/ruins and meadow/temple (terrain only) showed 0 structure blocks. They are not converted and are protected by the '
             'attached-piece footprint mask.')
    return out


def expansion(slots):
    species = bm.species_structures(); pools = bm.attached_footprints()
    est = {}
    for th in THEMES:
        rows = {}; t0 = time.time()
        veg = {b for b in set() }
        for name in bm.NAMES:
            _, blocks = bm.load(f'{D}/structure/sky_islands/{th}/{name}.nbt')
            vv = {bid for _, (bid, _, _) in blocks.items() if is_veg(bid)}
            t1 = time.time()
            isl = bm.Island(name, pools, species, theme=th, slot=slots[th], veg=vv).build()
            rows[name] = dict(plantable=int(isl.plant.sum()), giant_requested=isl.giant_req, giant_placed=len(isl.giants), trees=len(isl.trees),
                              vegetation_blocks=len(isl.veg), terrain_converted=isl.stats['terrain_converted'],
                              terrain_id_kept_as_structure=isl.stats['terrain_id_kept_as_structure'], structure_kept=isl.stats['structure_kept'],
                              jigsaw_final_state_mapped=isl.stats['jigsaw_final_state_mapped'], unplaced=dict(isl.reasons),
                              survival_violations=isl.survival_violations(), seconds=round(time.time() - t1, 2))
        est[th] = dict(palette_assumed=slots[th], templates=rows, seconds=round(time.time() - t0, 1),
                       totals=dict(plantable=sum(r['plantable'] for r in rows.values()), giants=sum(r['giant_placed'] for r in rows.values()),
                                   giants_requested=sum(r['giant_requested'] for r in rows.values()), vegetation=sum(r['vegetation_blocks'] for r in rows.values())))
        print(th, est[th]['totals'], est[th]['seconds'], 's')
    return est


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    jr = jigsaw_recheck()
    json.dump(jr, open(f'{OUT}/jigsaw_recheck.json', 'w'), indent=1)
    print('jigsaws', jr['total'], jr['by_class'], 'p25', jr['p25_by_class'], 'same positions', jr['same_positions_as_p25'])
    slots = theme_slots()
    sp = structure_protection(slots)
    json.dump(sp, open(f'{OUT}/structure_protection.json', 'w'), indent=1)
    for th in THEMES: print(th, sp[th]['protected_block_ids'][:12], list(sp[th]['templates_with_buildings']))
    ex = expansion(slots)
    json.dump(ex, open(f'{OUT}/expansion_estimate.json', 'w'), indent=1)
