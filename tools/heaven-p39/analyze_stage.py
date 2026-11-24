"""P3.12 stage summary. usage: analyze_stage.py <stage.json> <expected-heaven-themes csv> [--ref <json>:<name> ...]
Per theme: template family vs expectation, jigsaw/vanilla-block counts, habitat, loot/spawner/box equality with the vanilla control (c10_vanilla.json), fingerprint equality with refs."""
import json, sys
R = 'build/heaven-worldgen/results/'
ok = lambda r: isinstance(r, dict) and 'fingerprint' in r
st = json.load(open(sys.argv[1]))['islands']; heaven = set(sys.argv[2].split(',')) - {''}
refs = [(a.split(':')[1], json.load(open(a.split(':')[0]))['islands']) for a in sys.argv[3:] if a != '--ref']
ctl = json.load(open(R + 'c10_vanilla.json'))['islands']
be = lambda x: (x['loot_table_containers'], x['block_entities'].get('mob_spawner', 0) + x['block_entities'].get('trial_spawner', 0))
allok = True
for th in ('meadow', 'forest', 'mountain_meadow'):
    ks = [k for k in st if k.split('#')[0] == th and ok(st[k])]
    want = th in heaven
    fam = sum(st[k]['any_heaven_template'] == want and (st[k]['any_vanilla_theme_template'] != want) for k in ks)
    jig = sum(st[k]['jigsaw_left'] for k in ks); van = sum(st[k]['vanilla_theme_blocks'] for k in ks)
    func = all(be(st[k]) == be(ctl[k]) and st[k]['box'] == ctl[k]['box'] for k in ks if ok(ctl.get(k)))
    hab = (sum(st[k]['habitat_support'] for k in ks), sum(st[k]['standable_columns'] for k in ks))
    eq = {n: sum(st[k]['fingerprint'] == r[k]['fingerprint'] for k in ks if ok(r.get(k))) for n, r in refs}
    first = max(st[k]['habitat_first_call_ms'] for k in ks); mx = max(st[k]['habitat_max_call_ms'] for k in ks)
    good = fam == len(ks) and jig == 0 and func and (van == 0 or not want or th == 'meadow')
    allok &= good
    print(f"{th}: sites={len(ks)} expected={'heaven' if want else 'vanilla'} family_ok={fam}/{len(ks)} jigsaw_left={jig} vanilla_theme_blocks={van} loot/spawner/box==control={func} habitat={hab[0]}/{hab[1]} fp_equal={eq} habitat_first_ms<={first:.0f} max_call_ms<={mx:.0f} -> {'OK' if good else 'CHECK'}")
sys.exit(0 if allok else 1)
