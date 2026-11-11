#!/usr/bin/env python3
"""ST-01 P8-C2-B counterfactual audit: analysis path 2 (independent recomputation).

    python3 -I tools/st01-p8c2b-audit/verify_independent.py <analysis-dir> [<results-dir>]

Deliberately shares NO code with analyze.py. It reads only the exported candidates.csv (stable schema) and, for the final
density comparison, summary.json. Scenario membership is recomputed with SQL over the RAW measurement columns (not from the
eligible_* columns analyze.py wrote), per-seed totals by GROUP BY, the 2x2 table by conditional counts, and the Poisson
interval with a different algorithm (log-space series, grid scan + refinement). Any disagreement exits non-zero.
"""
import csv, json, math, re, sqlite3, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
adir = Path(sys.argv[1])
rows = list(csv.DictReader(open(adir / 'candidates.csv')))
summary = json.loads((adir / 'summary.json').read_text())
fails = []


def check(name, ok, detail=''):
    print(('PASS ' if ok else 'FAIL ') + name + (' ' + str(detail) if detail else ''))
    if not ok: fails.append(name)


db = sqlite3.connect(':memory:')
db.execute('''CREATE TABLE c (seed INT, chunk_x INT, chunk_z INT, footprint_desert TEXT, stub_cell_desert TEXT, height_variation INT, required_cut INT,
  required_skirt INT, entrance_max_obstruction INT, fluid_blocks_total INT, fluid_blocks_in_volume INT, production_verdict TEXT, actual_valid_start TEXT,
  ea INT, eb INT, ec INT, ed INT)''')
for r in rows:
    db.execute('INSERT INTO c VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)', (int(r['seed']), int(r['chunk_x']), int(r['chunk_z']), r['footprint_desert'], r['stub_cell_desert'],
               int(r['height_variation']), int(r['required_cut']), int(r['required_skirt']), int(r['entrance_max_obstruction']), int(r['fluid_blocks_total']),
               int(r['fluid_blocks_in_volume']), r['production_verdict'], r['actual_valid_start'],
               r['eligible_A'] == 'true', r['eligible_B'] == 'true', r['eligible_C'] == 'true', r['eligible_D'] == 'true'))

# limits are read from the production source by an independent regex (not shared with analyze.py)
src = (ROOT / 'src/main/java/dev/itzgamesgd/moreminecrafts/pyramid/worldgen/St01PlacementConfig.java').read_text()
lim = {k: int(re.search(k + r'\s*=\s*(\d+);', src).group(1)) for k in ('MAX_HEIGHT_DIFFERENCE', 'MAX_CUT', 'MAX_SKIRT', 'ENTRANCE_MAX_STEP_UP')}
check('locked limits 16/10/8/1', (lim['MAX_HEIGHT_DIFFERENCE'], lim['MAX_CUT'], lim['MAX_SKIRT'], lim['ENTRANCE_MAX_STEP_UP']) == (16, 10, 8, 1), lim)

TERRAIN = ("footprint_desert='true' AND stub_cell_desert='true' AND height_variation<=%d AND required_cut<=%d AND required_skirt<=%d"
           % (lim['MAX_HEIGHT_DIFFERENCE'], lim['MAX_CUT'], lim['MAX_SKIRT']))
ENT = 'entrance_max_obstruction<=%d' % lim['ENTRANCE_MAX_STEP_UP']
FLU = 'fluid_blocks_total=0'


def q(sql): return db.execute(sql).fetchall()


n = {k: q('SELECT COUNT(*) FROM c WHERE %s AND %s' % (TERRAIN, cond))[0][0] for k, cond in {
    'n11': ENT + ' AND ' + FLU, 'n10': ENT + ' AND NOT (' + FLU + ')', 'n01': 'NOT (' + ENT + ') AND ' + FLU, 'n00': 'NOT (' + ENT + ') AND NOT (' + FLU + ')'}.items()}
T = q('SELECT COUNT(*) FROM c WHERE ' + TERRAIN)[0][0]
check('2x2 sums to terrain-qualified', sum(n.values()) == T, (n, T))
sc = {'A': n['n11'], 'B': n['n11'] + n['n01'], 'C': n['n11'] + n['n10'], 'D': T}
for k, col in zip('ABCD', ('ea', 'eb', 'ec', 'ed')):
    check('scenario %s count equals SQL recomputation' % k, q('SELECT COUNT(*) FROM c WHERE %s=1' % col)[0][0] == sc[k] == summary['scenario_counts'][k], sc[k])
direct = {'A': q('SELECT COUNT(*) FROM c WHERE %s AND %s AND %s' % (TERRAIN, ENT, FLU))[0][0],
          'B': q('SELECT COUNT(*) FROM c WHERE %s AND %s' % (TERRAIN, FLU))[0][0],
          'C': q('SELECT COUNT(*) FROM c WHERE %s AND %s' % (TERRAIN, ENT))[0][0], 'D': T}
check('scenario counts from direct SQL conjunctions equal the 2x2 algebra', direct == sc, direct)
check('2x2 equals analyze.py contingency', [n['n11'], n['n10'], n['n01'], n['n00']] == [summary['contingency_terrain_qualified'][k] for k in
      ('n11_ent_pass_fluid_pass', 'n10_ent_pass_fluid_fail', 'n01_ent_fail_fluid_pass', 'n00_ent_fail_fluid_fail')])
check('no duplicate candidate records', q('SELECT COUNT(*) FROM (SELECT seed,chunk_x,chunk_z FROM c GROUP BY 1,2,3 HAVING COUNT(*)>1)')[0][0] == 0)
check('production verdict ELIGIBLE count equals scenario A', q("SELECT COUNT(*) FROM c WHERE production_verdict='ELIGIBLE'")[0][0] == sc['A'])
check('actual valid starts (real world) equal scenario A', q("SELECT COUNT(*) FROM c WHERE actual_valid_start='true'")[0][0] == sc['A'])
check('every actual start is production-eligible', q("SELECT COUNT(*) FROM c WHERE actual_valid_start='true' AND production_verdict<>'ELIGIBLE'")[0][0] == 0)
check('entrance-qualified sequential = n11+n10', q("SELECT COUNT(*) FROM c WHERE production_verdict IN ('ELIGIBLE','REJECT_FLUID')")[0][0] == n['n11'] + n['n10'])
# production REJECT_FLUID can only occur after entrance passes, REJECT_ENTRANCE precedes fluid
check('production REJECT_FLUID == n10', q("SELECT COUNT(*) FROM c WHERE production_verdict='REJECT_FLUID'")[0][0] == n['n10'])
check('production REJECT_ENTRANCE == n01+n00', q("SELECT COUNT(*) FROM c WHERE production_verdict='REJECT_ENTRANCE'")[0][0] == n['n01'] + n['n00'])
check('fluid margin-0 pass implies zero in-volume fluid', q('SELECT COUNT(*) FROM c WHERE fluid_blocks_total=0 AND fluid_blocks_in_volume>0')[0][0] == 0)

# per-seed via GROUP BY
per = {s: dict(zip('ABCD', v)) for s, *v in q('SELECT seed, SUM(ea), SUM(eb), SUM(ec), SUM(ed) FROM c GROUP BY seed')}
seeds = summary['seeds']
for k in 'ABCD':
    got = [per.get(s, {}).get(k, 0) or 0 for s in seeds]
    check('per-seed %s equals analyze.py' % k, got == [summary['scenario_stats'][k]['per_seed'][str(s)] for s in seeds])

# density with an independently derived area: potential anchors are enumerated inside [-radius, radius] per axis
area = len(seeds) * (2 * summary['radius_blocks'] / 1000.0) ** 2
check('area', abs(area - summary['area_km2_total']) < 1e-9, area)
for k in 'ABCD':
    d = sc[k] * 100.0 / area
    check('density %s' % k, abs(d - summary['scenario_stats'][k]['density_per_100km2']) < 1e-12, round(d, 6))


def lpmf(k, mu): return -mu + k * math.log(mu) - math.lgamma(k + 1) if mu > 0 else (0.0 if k == 0 else -math.inf)
def cdf(k, mu): return sum(math.exp(lpmf(i, mu)) for i in range(k + 1))


def interval(k):
    # grid scan then bisection refine (different from analyze.py's pure bisection with a Poisson cdf recurrence)
    def first(pred, hi):
        mu = 0.0; step = hi / 4000.0
        while mu < hi and not pred(mu): mu += step
        a, b = max(0.0, mu - step), mu
        for _ in range(80):
            m = (a + b) / 2
            a, b = (a, m) if pred(m) else (m, b)
        return (a + b) / 2
    lo = 0.0 if k == 0 else first(lambda mu: 1 - cdf(k - 1, mu) >= 0.025, k + 60)
    hi = first(lambda mu: cdf(k, mu) <= 0.025, k + 60 + 10 * math.sqrt(k + 1))
    return lo, hi


for k in 'ABCD':
    lo, hi = interval(sc[k]); a, b = summary['scenario_stats'][k]['poisson_ci95_per_100km2']
    check('Poisson 95%% interval %s' % k, abs(lo * 100 / area - a) < 1e-4 and abs(hi * 100 / area - b) < 1e-4, (round(lo * 100 / area, 4), round(hi * 100 / area, 4)))
check('relative density vs A', all(abs((sc[k] / sc['A']) - summary['scenario_stats'][k]['relative_to_A']) < 1e-12 for k in 'BCD') if sc['A'] else True)

print('INDEPENDENT_VERIFICATION', 'FAIL' if fails else 'PASS', 'failures=%s' % fails)
sys.exit(1 if fails else 0)
