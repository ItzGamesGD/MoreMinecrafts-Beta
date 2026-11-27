import importlib.util
from pathlib import Path
import unittest
import tempfile
import json
import csv

HERE=Path(__file__).resolve().parent


def load(name):
    path=HERE/(name+'.py')
    assert path.exists(),'missing GEOMETRY-03B '+name
    s=importlib.util.spec_from_file_location(name,path)
    m=importlib.util.module_from_spec(s);s.loader.exec_module(m);return m


class Geometry03BTests(unittest.TestCase):
    def test_common_schedule_keeps_interturn_clearance(self):
        g=load('search')
        loop=g.old.grid_loop(17,12)
        records=g.schedule(loop)
        N=len(loop)
        for i in range(N):
            for lap,budget in enumerate((11,12,14)):
                self.assertGreaterEqual(records[(lap+1)*N+i]['floor']-records[lap*N+i]['floor'],budget)

    def test_wide_lane_is_not_a_single_actor_path(self):
        v=load('verify')
        blocks={(x,0,0):('full','') for x in range(4)}
        self.assertTrue(v.check_lane([(x,0,0) for x in range(4)],blocks)['pass'])
        blocks[3,1,0]=('full','')
        r=v.check_lane([(x,0,0) for x in range(4)],blocks)
        self.assertFalse(r['pass'])
        self.assertEqual(r['blocked_columns'],[[3,0,0]])

    def test_half_stair_headspace_uses_collision_shapes(self):
        v=load('verify')
        blocks={(x,0,0):('stair','south') for x in range(4)}
        self.assertTrue(v.check_lane([(x,0,0) for x in range(4)],blocks)['pass'])
        blocks[2,2,0]=('full','')
        self.assertFalse(v.check_lane([(x,0,0) for x in range(4)],blocks)['pass'])

    def test_arch_molding_stays_outside_walking_columns(self):
        g=load('search')
        walk={(x,z) for x in range(16,20) for z in range(-8,9)}
        path=g.arch_path((21,-5),(21,5),walk,g.box(22))
        self.assertTrue(path)
        self.assertFalse(set().union(*(g.small_pad(p) for p in path)) & walk)

    def test_risers_do_not_split_four_wide_corner_sections(self):
        g=load('search')
        *_,frames=g.stairs(17,12)
        bad=[r for r in frames if len({p[1] for p in r['cells']})!=1]
        self.assertFalse(bad, 'a future riser leaked into a turning cross-section')

    def test_flat_join_uses_actual_support_instead_of_forcing_lower_half(self):
        v=load('verify')
        blocks={(x,1,z):('full','') for x in (0,2) for z in range(4)}
        blocks.update({(1,1,z):('stair','east') for z in range(4)})
        self.assertTrue(v.check_lane([(1,1,z) for z in range(4)],blocks)['pass'])

    def test_balcony_rails_cannot_take_a_stair_walking_column(self):
        g=load('search');m=g.build(b=12)
        walking={(x,z) for x,y,z in m['groups']['stairs']}
        self.assertFalse({(x,z) for x,y,z in m['groups']['balcony_rail']} & walking)

    def test_successive_stairs_use_neighbour_half_step_support(self):
        v=load('verify')
        blocks={(0,0,z):('full','') for z in range(4)}
        blocks.update({(1,1,z):('stair','east') for z in range(4)})
        blocks.update({(2,2,z):('stair','east') for z in range(4)})
        blocks.update({(3,2,z):('full','') for z in range(4)})
        self.assertTrue(v.check_lane([(1,1,z) for z in range(4)],blocks)['pass'])

    def test_rotated_reservation_exceedance_coordinates_match_export(self):
        g=load('search');m=g.build(b=12)
        with tempfile.TemporaryDirectory() as folder:
            out=Path(folder);g.export(m,out,rotation=1)
            with (out/'blocks_C1.csv').open() as f:
                expected={(int(r['x']),int(r['z'])) for r in csv.DictReader(f)
                    if not r['group'].startswith('balcony') and int(r['x'])**2+int(r['z'])**2>=22.5**2}
            actual=set(map(tuple,json.loads((out/'claims_C1.json').read_text())['outside_original_disc45_columns']))
            self.assertEqual(actual,expected)


if __name__=='__main__':unittest.main()
