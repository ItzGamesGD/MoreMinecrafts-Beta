"""Targeted contract checks only: never invokes a Subject or probe."""
import copy
import unittest
import oracle as O
import negatives as N
import tier_a as T


class Corrections(unittest.TestCase):
    def test_frozen_golden_regeneration_refused_before_loading(self):
        from unittest.mock import patch
        with patch.object(O.os.path, 'lexists', return_value=True), patch.object(O, 'load') as load:
            with self.assertRaisesRegex(SystemExit, 'Golden regeneration forbidden'):
                O.main()
            load.assert_not_called()

    def test_missing_defect_is_not_expanded(self):
        self.assertNotIn('UNKNOWN_REPORTED_AS_EMPTY', N.OUTCOME['N-MISSING-MEANS-EMPTY']['classes'])

    def test_pending_is_evaluable_and_deferral_is_not(self):
        compare = getattr(O, 'compare_fields', None)
        self.assertIsNotNone(compare, 'machine-readable comparator missing')
        for wrong in ([], '0', 0, None, 'UNKNOWN'):
            self.assertEqual(compare({'x': 'PENDING'}, {'x': wrong}, [])['verdict'], 'FAIL')
        self.assertEqual(compare({'x': 'PENDING'}, {'x': 'PENDING'}, [])['verdict'], 'PASS')
        self.assertEqual(compare({}, {'x': []}, ['x'])['verdict'], 'UNKNOWN')
        self.assertEqual(compare({'a': '1'}, {'a': '1', 'x': []}, ['x'])['verdict'], 'PASS')

    def test_late_checkpoints(self):
        a, b = T.LATE['checkpoints']
        self.assertFalse(a['population_complete'])
        self.assertEqual(a['unknown_edges'], [{'u': 'C', 'v': 'D'}])
        self.assertTrue(b['population_complete'])
        self.assertEqual(b['unknown_edges'], [])
        self.assertEqual(b['finalized_accepted'], ['B', 'D'])
        self.assertEqual(b['finalized_rejected'], ['A', 'C'])


if __name__ == '__main__':
    unittest.main()

class GeneratedContracts(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import json
        from pathlib import Path
        cls.golden = json.loads(Path(O.HERE, 'goldens-v1.json').read_text())
        cls.inputs = {r['fixture_id']: r for r in O.load('inputs-v1.json')}

    def test_replay_values_and_independent_canonical_bytes(self):
        import hashlib
        import json
        count = blocked = available = deferred = 0
        for fid, schedules in self.golden['prefix_states'].items():
            events = {e['index']: e for e in self.inputs[fid]['events']}
            for item in schedules.values():
                count += 1
                delivered = [events[i] for i in item['delivered']]
                # Independent membership checks from literal input payloads, not prefix_state().
                qualified = {e['unit'] for e in delivered if e['kind'] == 'QUALIFIED'}
                self.assertEqual(set(item['state']['qualified']), qualified)
                self.assertEqual(set(item['state']['rolled']), {e['unit'] for e in delivered if e['kind'] == 'ORIGINAL_ROLL'})
                self.assertEqual(item['state'], item['roundtrip_state'])
                for field in item['state']:
                    wrong = copy.deepcopy(item['state'])
                    wrong[field] = None
                    self.assertEqual(O.compare_fields(item['state'], wrong, item['deferred'])['verdict'], 'FAIL')
                deferred += len(item['deferred'])
                if item['state_sha256']['status'] == 'BLOCKED':
                    blocked += 1
                else:
                    available += 1
                    self.assertEqual(item['model_state'], item['final_model_state'])
                for key, digest_key in [('final_model_state', 'final_state_sha256'), ('model_state', 'state_sha256')]:
                    if key not in item:
                        continue
                    # Stdlib encoder is independent of oracle.cj; compare bytes AND digest.
                    data = (json.dumps(item[key], indent=2, sort_keys=True, ensure_ascii=True) + '\n').encode()
                    self.assertEqual(data, (O.cj(item[key]) + '\n').encode())
                    self.assertEqual(hashlib.sha256(data).hexdigest(), item[digest_key]['sha256'])
                    self.assertEqual(item[key]['counters']['physical_completed'], '0')
                final = self.golden['fixtures'][fid]
                for k in O.MODEL_SETS:
                    self.assertEqual(item['final_model_state']['sets'][k], final['sets'][k])
                self.assertEqual(item['final_model_state']['applied_event_ids'], sorted(e['event_id'] for e in events.values()))
        self.assertEqual((count, deferred), (1086, 24))
        self.assertEqual((available, blocked), (74, 1012))

    def test_control_sessions_and_detection(self):
        controls = O.load('controls-v1.json')
        rows = controls.get('controls', [])
        row = next(r for r in rows if r.get('id') == 'N-MISSING-MEANS-EMPTY')
        self.assertEqual(int(row['sessions']), 4)
        self.assertEqual(row['outcome_classes'], ['REROLLED_AS_UNROLLED'])
        self.assertEqual(len(row['targets']), 4)
        self.assertEqual({r['case_no'] for r in row['detect_on']['schedules']}, {'76', '78'})
        for fid in ('P01/ACCT-LOSS-BASE-SOURCE', 'P01/ACCT-LOSS-MODEL-SUCCESS-SOURCE'):
            g = self.golden['fixtures'][fid]
            self.assertEqual(N.classify_missing_means_empty(g, {}), 'OK')
            self.assertEqual(N.classify_missing_means_empty(g, {'reroll': 'a'}), 'REROLLED_AS_UNROLLED')
            self.assertEqual(N.classify_missing_means_empty(g, {'field_values': {'pending': []}}), 'U-OUTCOME-UNMATCHED')
            events = dict(g['event_dispositions'])
            removed = dict(events)
            removed.pop(next(iter(removed)))
            for altered in (removed, dict(events, **{'999': 'APPLIED'})):
                self.assertEqual(N.classify_missing_means_empty(g, {'event_dispositions': altered}), 'U-OUTCOME-UNMATCHED')
            for value in ('1', '3'):
                self.assertEqual(N.classify_missing_means_empty(g, {'counters': dict(g['counters'], roll_evidenced=value)}), 'U-OUTCOME-UNMATCHED')
            self.assertEqual(N.classify_missing_means_empty(g, {'event_dispositions': dict(events, **{'7': 'APPLIED'})}), 'U-OUTCOME-UNMATCHED')
            self.assertEqual(N.classify_missing_means_empty(g, {'sets': dict(g['sets'], roll_state_unknown=[])}), 'U-OUTCOME-UNMATCHED')
            self.assertEqual(N.classify_missing_means_empty(g, {'unit_roll_state': dict(g['unit_roll_state'], a='WIN')}), 'U-OUTCOME-UNMATCHED')
            self.assertEqual(N.classify_missing_means_empty(g, {'unit_roll_state': dict(g['unit_roll_state'], b='LOSS')}), 'U-OUTCOME-UNMATCHED')
            corrupt = dict(events)
            corrupt[next(iter(corrupt))] = 'CORRUPT'
            self.assertEqual(N.classify_missing_means_empty(g, {'event_dispositions': corrupt}), 'U-OUTCOME-UNMATCHED')
            self.assertEqual(N.classify_missing_means_empty(g, {'unit_roll_state': dict(g['unit_roll_state'], a='NOT_ROLLED')}), 'REROLLED_AS_UNROLLED')
        for fid in ('P01/ACCT-LOSS-BASE-PROJECTION', 'P01/ACCT-LOSS-MODEL-SUCCESS-PROJECTION'):
            self.assertEqual(self.golden['fixtures'][fid]['projection_rebuilds'], '1')

    def test_late_value_mismatches_at_each_checkpoint(self):
        checkpoints = self.golden['fixtures']['P01/LATE-01']['checkpoints']
        self.assertEqual(O.compare_late_checkpoints(checkpoints, checkpoints)['verdict'], 'PASS')
        self.assertEqual(O.compare_late_checkpoints(checkpoints, list(reversed(checkpoints)))['verdict'], 'UNKNOWN')
        self.assertEqual(O.compare_late_checkpoints(checkpoints, checkpoints[1:])['verdict'], 'UNKNOWN')
        premature = copy.deepcopy(checkpoints)
        premature[0]['finalized_accepted'] = ['D']
        self.assertEqual(O.compare_late_checkpoints(checkpoints, premature)['verdict'], 'FAIL')
        for cp in checkpoints:
            for field in ('stage', 'population_complete', 'unknown_edges', 'pending', 'finalized_accepted', 'finalized_rejected'):
                bad = copy.deepcopy(cp)
                bad[field] = None
                self.assertEqual(O.compare_fields(cp, bad, [])['verdict'], 'FAIL')

    def test_b3_documentation(self):
        from pathlib import Path
        text = Path(O.TOOL).parents[1].joinpath('docs/design/ST05_STAGE0_P0_PROBE_01_BINDING_PLAN.md').read_text()
        self.assertIn('33 fit checks instead of 32', text)
        self.assertNotIn('27 fit checks instead of 24', text)
