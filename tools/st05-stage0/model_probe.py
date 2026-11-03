#!/usr/bin/env python3
"""Counterexamples only: deliberately NOT an approved cavern/spacing policy."""
import concurrent.futures
import itertools
import json
import random
import time
import tracemalloc


def components(nodes):
    remaining = set(nodes)
    result = []
    while remaining:
        root = min(remaining)
        q = [root]
        remaining.remove(root)
        group = {root}
        while q:
            x, z = q.pop()
            for p in ((x-1,z), (x+1,z), (x,z-1), (x,z+1)):
                if p in remaining:
                    remaining.remove(p)
                    group.add(p)
                    q.append(p)
        result.append((min(group), len(group)))
    return sorted(result)


def run():
    tracemalloc.start()
    start = time.perf_counter()
    # Same connected cave crosses region x=512; late bridge merges local identities.
    tiles = [{(x, 0) for x in range(500, 512)},
             {(x, 0) for x in range(512, 525)}, {(525, 0), (526, 0)}]
    expected = components(set.union(*tiles))
    permutations = list(itertools.permutations(tiles))
    def scan(order):
        return components(set.union(*order))
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        results = list(pool.map(scan, permutations))
    assert all(r == expected for r in results)
    # Only proves canonicalization AFTER complete topology is known.
    before = components(tiles[1])
    assert before[0][0] != expected[0][0]
    disconnected = {(0,0), (2,0)}
    assert len(components(disconnected)) == 2
    assert len(components(disconnected | {(1,0)})) == 1
    restored = {tuple(p) for p in json.loads(json.dumps(sorted(set.union(*tiles))))}
    assert components(restored) == expected
    # Illustrative distance=100; NOT a selected gameplay spacing value.
    centers = {'A': 0, 'B': 90, 'C': 180}
    def greedy(order):
        accepted = []
        for name in order:
            if all(abs(centers[name]-centers[n]) >= 100 for n in accepted):
                accepted.append(name)
        return sorted(accepted)
    outcomes = {tuple(greedy(p)) for p in itertools.permutations(centers)}
    assert outcomes == {('A','C'), ('B',)}
    # Both win their one roll: spacing forbids retaining both. Do not resolve this silently.
    both_wins = [random.Random(seed).random() < .75 and
                 random.Random(seed+1).random() < .75 for seed in range(1000)]
    assert any(both_wins)
    print(json.dumps({
        'kind': 'SIMULATED_ONLY',
        'complete_graph_input_orders_in_parallel_workers': len(results),
        'complete_graph_canonical_ids_identical': True,
        'json_restart_identical': True,
        'bounded_local_id_changes_when_frontier_expands': [before, expected],
        'late_bridge_merges_two_previous_ids': True,
        'first_arrival_spacing_outcomes': sorted(outcomes),
        'proposal_only': 'canonical minimum after proven closed component; lexicographic priority among competing roll winners',
        'proposal_adopted': False,
        'owner_decision_required': 'closed-component/budget-exhaustion semantics; spacing precedence; all fit-eligible roll denominator vs spacing exclusions',
        'a02': 'BLOCKER', 'a03': 'NOT_PROVEN_BY_MODEL',
        'elapsed_ms': round((time.perf_counter()-start)*1000, 3),
        'peak_bytes': tracemalloc.get_traced_memory()[1]
    }, indent=2))


if __name__ == '__main__':
    run()
