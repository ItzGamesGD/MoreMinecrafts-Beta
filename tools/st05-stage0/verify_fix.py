"""Validate NEW Stage 0-FIX observations; do not re-run the old probe."""
import json
import pathlib
import sys

p = pathlib.Path(sys.argv[1])
docs = {n: json.loads((p / (n + '.json')).read_text()) for n in
        ('prepare', 'interrupt', 'recover', 'verify', 'order')}
assert all(d['seed'] == 20261030 for d in docs.values())
assert docs['prepare']['rows']['materialize']['failure_before_mutation']
assert docs['interrupt']['rows']['resume']['state'] == 'APPLYING'
assert docs['interrupt']['rows']['resume']['writes'] == 1
r, v, o = (docs[n]['rows'] for n in ('recover', 'verify', 'order'))
for name in ('resume', 'complete_gap', 'all_written', 'erased'):
    assert r[name]['first']['state'] == v[name]['first']['state'] == 'COMPLETE'
    assert r[name]['duplicate']['writes'] == v[name]['first']['writes'] == 0
    assert v[name]['duplicate']['writes'] == 0
    assert r[name]['first']['plan_hash'] == v[name]['first']['plan_hash']
for name in ('resume', 'complete_gap', 'all_written'):
    assert o[name]['state'] == 'COMPLETE'
    assert o[name]['plan_hash'] == v[name]['first']['plan_hash']
assert r['complete_gap']['first']['previous_state'] == 'COMPLETE'
assert r['complete_gap']['first']['writes'] == 1  # COMPLETE was stale; actual chunk state wins
assert r['all_written']['first']['writes'] == 0  # blocks saved, completion not recorded
for name in ('conflict', 'corrupt', 'materialize'):
    assert r[name]['first']['writes'] == v[name]['first']['writes'] == 0
assert r['conflict']['first']['state'] == v['conflict']['first']['state'] == 'CONFLICT'
assert r['corrupt']['first']['state'] == v['corrupt']['first']['state'] == 'INVALID'
assert r['erased']['first']['writes'] == 1
assert r['erased_edit_negative_control']['overwritten']
print(json.dumps({
    'runtime': 'Minecraft 26.3, five independent JVMs',
    'recovery_interrupted_then_restarted': 'PASS for two-marker fixture',
    'duplicate_replay_no_block_writes': 'PASS',
    'stale_complete_revalidated': 'PASS',
    'all_blocks_written_before_complete_record': 'PASS',
    'corrupt_journal_and_failed_publish_no_world_writes': 'PASS',
    'visible_conflict_preserves_edit': 'PASS',
    'fresh_reverse_order_same_immutable_plan': 'PASS',
    'erased_to_original_state_edit': 'FAIL: state-only reconciliation overwrites indistinguishable edit',
    'strict_physical_atomicity': 'NOT SOLVED: partial blocks exist before recovery',
    'full_arena_connection_collision_plan': 'NOT IMPLEMENTED',
    'a03': 'BLOCKER / OWNER_DECISION_REQUIRED',
    'timing_and_heap': {n: {k: d[k] for k in ('elapsed_ms', 'used_heap_bytes')} for n,d in docs.items()}
}, indent=2))
