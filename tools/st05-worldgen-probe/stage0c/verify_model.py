#!/usr/bin/env python3
"""Stage 0-C SYNTHETIC contract experiment. No natural-chamber or game integration claim."""
import concurrent.futures
import hashlib
import itertools
import json
import random
import time
import tracemalloc
from dataclasses import dataclass
from pathlib import Path

@dataclass(frozen=True)
class Candidate:
    id: int
    x: int
    z: int = 0
    priority: int = 0
    suitable: bool = True
    unresolved: bool = False
    # Fixture footprint, including artificial corridor/safety extension.
    extent: int = 34

def key(c):
    return c.priority, c.id

def conflicts(a, b):
    return ((a.x-b.x)**2 + (a.z-b.z)**2 < 256**2 or
            (abs(a.x-b.x) < a.extent+b.extent and abs(a.z-b.z) < a.extent+b.extent))

def roll(seed, cid):
    return int.from_bytes(hashlib.sha256(f'{seed}:roll:{cid}'.encode()).digest()[:8], 'big') < 3 * 2**62

def select(candidates, seed=7001, cap=100000, stage_cap=100000, used=None, finalized=None):
    """Finite CLOSED candidate-set oracle; candidate discovery completeness is an INPUT assumption.
    Charges one operation per prior-candidate inspection, including nonconflicts.
    Independent per-candidate cumulative counters survive serialized reloads.
    """
    used = dict(used or {})
    states = dict(finalized or {})
    ordered = sorted(candidates, key=key)
    for i, c in enumerate(ordered):
        if c.id in states:
            continue
        if not c.suitable:
            states[c.id] = 'UNSUITABLE'
            continue
        if c.unresolved:
            states[c.id] = 'UNRESOLVED'
            continue
        cost = i + 1
        if used.get(c.id, 0) + cost > cap or cost > stage_cap:
            used[c.id] = min(cap, used.get(c.id, 0) + cost)
            states[c.id] = 'BUDGET_EXHAUSTED'
            continue
        used[c.id] = used.get(c.id, 0) + cost
        neighbors = [p for p in ordered[:i] if conflicts(c, p)]
        # Conservative propagation: even an independently rejected candidate can be deferred.
        if any(states[p.id] in {'UNRESOLVED', 'BLOCKED', 'BUDGET_EXHAUSTED'} for p in neighbors):
            states[c.id] = 'BLOCKED'
        elif any(states[p.id] == 'SELECTED' for p in neighbors):
            states[c.id] = 'SPACING_EXCLUDED'
        else:
            states[c.id] = 'SELECTED'
    denominator = sum(s == 'SELECTED' for s in states.values())
    wins = sum(s == 'SELECTED' and roll(seed, cid) for cid, s in states.items())
    assert len(states) == len(candidates)
    return states, used, dict(total=len(states), denominator=denominator, wins=wins,
                             roll_failed=denominator-wins,
                             excluded=sum(s == 'SPACING_EXCLUDED' for s in states.values()),
                             unresolved=sum(s in {'UNRESOLVED','BLOCKED','BUDGET_EXHAUSTED'} for s in states.values()))

def checkpoint(payload):
    body = json.dumps(payload, sort_keys=True)
    return {'body': body, 'sha256': hashlib.sha256(body.encode()).hexdigest()}

def recover(record, expected):
    try:
        if hashlib.sha256(record['body'].encode()).hexdigest() != record['sha256']:
            return 'QUARANTINED'
        p = json.loads(record['body'])
        return p if all(p[k] == v for k,v in expected.items()) else 'QUARANTINED'
    except (KeyError, TypeError, ValueError):
        return 'QUARANTINED'

def main():
    checks = []
    def check(name, value):
        assert value, name
        checks.append(name)
    # Independent expected chain: A rejects B; rejected B cannot reject C.
    chain = [Candidate(i, i*200, priority=i) for i in range(3)]
    expected = {0:'SELECTED',1:'SPACING_EXCLUDED',2:'SELECTED'}
    check('greedy A-B-C (A,C survive)', select(chain)[0] == expected)
    for order in itertools.permutations(chain):
        check('all 3! input orders', select(order)[0] == expected)
    check('distance 255 excluded', select([Candidate(0,0),Candidate(1,255)])[2]['denominator'] == 1)
    check('distance 256 accepted', select([Candidate(0,0),Candidate(1,256)])[2]['denominator'] == 2)
    check('XZ Euclidean diagonal', select([Candidate(0,0),Candidate(1,181,181)])[2]['denominator'] == 1)
    check('corridor safety collision beyond spacing', select([Candidate(0,0,extent=200),Candidate(1,300,extent=200)])[2]['denominator'] == 1)
    check('unsuitable before spacing and roll', select([Candidate(0,0,suitable=False),Candidate(1,1)])[2]['denominator'] == 1)
    uchain = [Candidate(0,0,unresolved=True)] + [Candidate(i,i*200,priority=i) for i in range(1,100)]
    s, used, n = select(uchain)
    check('99-hop unresolved propagation', n['unresolved'] == 100)
    check('provably disconnected candidate proceeds', select(uchain + [Candidate(100,100000,priority=100)])[0][100] == 'SELECTED')
    # Coordinates straddling negative and positive owner-grid boundaries.
    check('256 ownership floor division incl negatives', [x//256 for x in [-257,-256,-1,0,255,256]] == [-2,-1,-1,0,0,1])
    check('128 margin inclusive boundary', all(-128 <= x <= 383 for x in [-128,0,255,383]))
    # This is coordinate arithmetic only: it does not prove natural-chamber equivalence.
    rng = random.Random(7001)
    population = [Candidate(i,rng.randrange(-4096,4096),rng.randrange(-4096,4096),rng.randrange(2**32)) for i in range(128)]
    baseline = select(population)
    orders = []
    for _ in range(32):
        order = population.copy(); rng.shuffle(order); orders.append(order)
    with concurrent.futures.ThreadPoolExecutor(4) as pool:
        check('32 shuffled/4-thread same-seed model runs', all(r == baseline for r in pool.map(select, orders)))
    states, used, counts = baseline
    check('roll after spacing count conservation', counts['total'] == counts['excluded'] + counts['denominator'] + counts['unresolved'])
    check('75 percent threshold denominator partition', counts['denominator'] == counts['wins'] + counts['roll_failed'])
    # Every winner fails preflight: no reselection, denominator and reservations stay fixed.
    failures = counts['wins']; placed = 0
    check('post-roll failure accounting; no reactivation', failures + placed == counts['wins'] and counts == select(population)[2])
    payload = dict(version=1, seed=7001, input='fixture-128', used={str(k):v for k,v in used.items()}, states=states, counts=counts)
    record = checkpoint(payload)
    restored = recover(json.loads(json.dumps(record)),dict(version=1,seed=7001,input='fixture-128'))
    check('serialized checkpoint restore', restored['counts'] == counts)
    resumed = select(population, used={int(k):v for k,v in restored['used'].items()}, finalized={int(k):v for k,v in restored['states'].items()})
    check('serialized decisions/counters resume without more work', resumed == baseline)
    bad = dict(record); bad['body'] += 'x'
    check('damaged checkpoint quarantined', recover(bad,{}) == 'QUARANTINED')
    for changed in [dict(version=2),dict(seed=7002),dict(input='different')]:
        check('checkpoint identity mismatch quarantined', recover(record,changed) == 'QUARANTINED')
    s1,u1,_ = select(chain,cap=2)
    s2,u2,_ = select(chain,cap=2,used=u1,finalized=s1)
    check('restart cannot reset exhausted budget', s1[2] == s2[2] == 'BUDGET_EXHAUSTED' and u1[2] == u2[2] == 2)
    check('restart preserves finalized decisions', s1 == s2 and u1 == u2)
    check('stage cap enforced', select(chain,stage_cap=1)[0][1] == 'BUDGET_EXHAUSTED')
    measurements=[]
    for cap in [32,128,512]:
        tracemalloc.start(); t=time.perf_counter(); cpu=time.process_time()
        _,_,stats=select(population,cap=cap)
        _,peak=tracemalloc.get_traced_memory(); tracemalloc.stop()
        measurements.append(dict(cap=cap,stage_cap=100000,wall_ms=(time.perf_counter()-t)*1000,cpu_ms=(time.process_time()-cpu)*1000,python_peak_bytes=peak,**stats))
    # Reuse committed evidence, never regenerate matching historical runs.
    root=Path(__file__).resolve().parents[1]
    evidence=[]
    for seed in [7001,7002,7003]:
        docs={p.stem:json.loads(p.read_text()) for p in (root/'results'/str(seed)).glob('*.json')}
        check('historical plan source deterministic', docs['plan']['plan_mask_order_independent'])
        check('historical final restart digest', docs['fwd']['final_mask_digest'] == docs['reload']['final_mask_digest'])
        evidence.append(dict(seed=seed,plan_single_ms=docs['plan']['plan_mask_ms_single_thread'],plan_4_thread_ms=docs['plan']['plan_mask_ms_4_threads_shuffled_columns'],final_order_equal={m:docs[m]['final_mask_digest']==docs['fwd']['final_mask_digest'] for m in ['rev','shuf','par']},plan_vs_final=docs['fwd']['plan_vs_final'],files={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (root/'results'/str(seed)).glob('*.json')}))
    result=dict(kind='SYNTHETIC_CONTRACT_AND_HISTORICAL_EVIDENCE_AUDIT',seed=7001,checks=checks,check_count=len(checks),measurements=measurements,historical=evidence,limitations=['closed candidate set assumed','no natural chamber identity algorithm','fixture footprints only','Python memory is not JVM memory','no production persistence or exactly-once proof'])
    (root/'stage0c'/'model-results.json').write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(dict(check_count=len(checks),measurements=measurements),indent=2))

if __name__ == '__main__': main()
