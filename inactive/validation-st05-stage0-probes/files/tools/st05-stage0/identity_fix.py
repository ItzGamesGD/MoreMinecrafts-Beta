"""Closed-topology identity / durable claim experiment. No production policy adopted."""
import concurrent.futures
import hashlib
import itertools
import json
import pathlib
import sqlite3
import subprocess
import sys
import tempfile


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def groups(nodes):
    remaining = set(nodes)
    found = []
    while remaining:
        root = min(remaining)
        pending, group = [root], set()
        remaining.remove(root)
        while pending:
            p = pending.pop()
            group.add(p)
            x,z = p
            for n in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                if n in remaining:
                    remaining.remove(n)
                    pending.append(n)
        found.append(group)
    return sorted(found, key=min)


def cavern_ids(nodes):
    return [digest([20261031, 'minecraft:overworld', min(g)]) for g in groups(nodes)]


def claim(db, key):
    with sqlite3.connect(db, timeout=20) as c:
        c.execute('PRAGMA synchronous=FULL')
        return c.execute('INSERT OR IGNORE INTO claims(id) VALUES (?)', (key,)).rowcount


def main():
    # 3 connected-component fixtures, first straddles Anvil x=512 boundary.
    tiles = [{(x,0) for x in range(500,512)}, {(x,0) for x in range(512,525)},
             {(x,20) for x in range(500,525)}, {(x,0) for x in range(720,730)}]
    ids = cavern_ids(set.union(*tiles))
    orders = list(itertools.permutations(tiles))
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        observed = list(pool.map(lambda order: cavern_ids(set.union(*order)), orders))
    assert all(i == ids for i in observed) and len(ids) == len(set(ids)) == 3
    with tempfile.TemporaryDirectory() as d:
        db = str(pathlib.Path(d)/'claims.db')
        with sqlite3.connect(db) as c:
            c.execute('PRAGMA journal_mode=WAL')
            c.execute('CREATE TABLE claims(id TEXT PRIMARY KEY)')
        requests = ids * 32
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            changes = list(pool.map(lambda key: claim(db,key), requests))
        assert sum(changes) == 3
        # Real second Python process, not a JSON round trip; deliberately a model DB, not MC SavedData.
        child = subprocess.check_output([sys.executable, __file__, '--retry', db, *ids], text=True).strip()
        assert json.loads(child) == [0,0,0]
        with sqlite3.connect(db) as c:
            assert c.execute('SELECT count(*) FROM claims').fetchone()[0] == 3
    # Explicit candidate priority on a FULLY KNOWN finite conflict graph, never production.
    centers = dict(zip(ids, (0,90,180)))  # 100 is illustrative, not approved spacing
    # Synthetic hard-fit fixtures: at most 8 tests, exactly one roll if a safe candidate exists.
    checks = {k: [False, True] for k in ids}
    eligible = [k for k in ids if any(checks[k][:8])]
    rolls = {k: int(digest(['roll',k]),16) % 4 < 3 for k in eligible}
    def place(order):
        winners = [k for k in order if rolls[k]]
        selected = []
        for k in sorted(winners, key=lambda k: (digest(['priority',k]), k)):
            if all(abs(centers[k]-centers[p]) >= 100 for p in selected):
                selected.append(k)
        return selected
    placements = [place(order) for order in itertools.permutations(eligible)]
    assert all(v == placements[0] for v in placements)
    # Same observed window, differing global topology through an unseen route.
    observed_nodes = {(x,0) for x in range(500,525)} | {(x,2) for x in range(500,525)}
    connected_outside = observed_nodes | {(499,z) for z in range(3)}
    window = lambda nodes: {p for p in nodes if 500 <= p[0] <= 524}
    assert window(connected_outside) == window(observed_nodes)
    assert len(groups(observed_nodes)) == 2 and len(groups(connected_outside)) == 1
    old_claims = cavern_ids(observed_nodes)
    merged_ids = cavern_ids(connected_outside)
    assert set(old_claims) != set(merged_ids)
    print(json.dumps({
        'kind': 'SIMULATED_ONLY; no Minecraft cave qualification/roll/placement',
        'closed_complete_graph_orders_in_parallel': len(orders),
        'stable_ids': ids,
        'sqlite_concurrent_claim_requests': len(requests), 'unique_claims': sum(changes),
        'second_process_retries_new_claims': json.loads(child),
        'candidate_priority': 'SHA256(priority, canonical_id), canonical_id tie-break, greedy on complete winners',
        'priority_order_results_equal': True, 'priority_adopted': False,
        'synthetic_fit_eligible_denominator': len(eligible), 'single_rolls': len(rolls),
        'roll_wins': sum(rolls.values()), 'final_placements': len(placements[0]),
        'spacing_exclusions': sum(rolls.values())-len(placements[0]),
        'same_local_observation_global_component_counts': [2,1],
        'old_ids_after_late_merge': old_claims, 'merged_ids': merged_ids,
        'durable_claims_cannot_fix_changing_identity': True,
        'real_75pct_denominator': 'UNKNOWN; no real rolls drawn',
        'stop': 'OWNER_DECISION_REQUIRED before treating frontier components as eligible or adopting spacing priority',
        'a02': 'BLOCKER'
    }, indent=2))


if __name__ == '__main__':
    if len(sys.argv)>1 and sys.argv[1]=='--retry':
        print(json.dumps([claim(sys.argv[2],key) for key in sys.argv[3:]]))
    else:
        main()
