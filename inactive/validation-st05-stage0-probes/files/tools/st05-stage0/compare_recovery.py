"""Small counterexamples for strategy comparison, SIMULATED ONLY."""
import json

ORIGINAL, DESIRED, PLAYER = 'air', 'emerald', 'oak'


def replay(world, acknowledged=False, reconcile=False):
    world = list(world)
    if acknowledged:
        return world, 'COMPLETE'  # negative control: trusting a flag skips real verification
    if reconcile and any(s not in (ORIGINAL, DESIRED) for s in world):
        return world, 'CONFLICT'
    return [DESIRED, DESIRED], 'COMPLETE'


visible_edit = [DESIRED, PLAYER]
assert replay(visible_edit)[0][1] != PLAYER
assert replay(visible_edit, reconcile=True) == (visible_edit, 'CONFLICT')
missing = [DESIRED, ORIGINAL]
assert replay(missing, acknowledged=True) == (missing, 'COMPLETE')
assert replay(missing, reconcile=True) == ([DESIRED, DESIRED], 'COMPLETE')
assert replay(replay(missing, reconcile=True)[0], reconcile=True)[0] == [DESIRED, DESIRED]
# Two histories with IDENTICAL recovery inputs: unpublished second write versus player undo.
history_unwritten = {'journal': 'APPLYING', 'blocks': missing}
history_player_undid = {'journal': 'APPLYING', 'blocks': missing}
assert history_unwritten == history_player_undid
assert replay(history_player_undid['blocks'], reconcile=True)[0][1] != ORIGINAL
print(json.dumps({
    'kind': 'SIMULATED_ONLY / negative controls',
    'blind_replay': 'FAIL: overwrites visible player edit',
    'completion_bit_only': 'FAIL: COMPLETE with missing chunk write',
    'state_reconciliation': 'PASS fixture repair/idempotence; FAIL original-state player edit',
    'indistinguishable_histories': [history_unwritten, history_player_undid],
    'rollback': 'Not implemented: restoring old snapshots also needs provenance to preserve later edits',
    'staging': 'Not implemented: eventual multi-chunk publication still needs atomic visibility; no proof from staging alone',
    'a03': 'BLOCKER',
    'decision_required': 'Q28 atomic visibility vs restart repair, and verifiable edit provenance/lifecycle; no relaxation adopted'
}, indent=2))
