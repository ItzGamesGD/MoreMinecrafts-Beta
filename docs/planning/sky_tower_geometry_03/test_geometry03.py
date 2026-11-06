"""Failure cases for geometric reachability, not implementation snapshots."""
import importlib.util
from pathlib import Path
import unittest

HERE = Path(__file__).resolve().parent


def load(name):
    path = HERE / (name + '.py')
    assert path.exists(), 'missing GEOMETRY-03 module: ' + name
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class Geometry03Tests(unittest.TestCase):
    def test_loop_is_closed_face_connected_and_four_wide_outside_wall(self):
        g = load('geometry03_model')
        loop = g.grid_loop(17, 8)
        self.assertEqual(len(loop), len(set(loop)))
        self.assertTrue(all(abs(a[0]-b[0])+abs(a[1]-b[1]) == 1
                            for a,b in zip(loop, loop[1:]+loop[:1])))
        columns = set().union(*(g.pad(p) for p in loop))
        self.assertFalse(columns & g.disc(31))
        self.assertTrue(columns <= g.disc(45))

    def test_route_search_rejects_air_gap_and_low_ceiling(self):
        nav = load('geometry03_navigation')
        solids = {(x,0,z): ('full','') for x in range(5) for z in range(3)}
        scene = nav.Scene(solids)
        self.assertTrue(scene.search((2,1,2), [(14,1,2)])['reached'][0])
        gap = {p:v for p,v in solids.items() if p[0] != 2}
        self.assertFalse(nav.Scene(gap).search((2,1,2), [(14,1,2)])['reached'][0])
        roof = solids | {(2,2,z): ('full','') for z in range(3)}
        self.assertFalse(nav.Scene(roof).search((2,1,2), [(14,1,2)])['reached'][0])

    def test_bottom_stair_is_walkable_without_one_block_jump(self):
        nav = load('geometry03_navigation')
        blocks = {(0,0,z): ('full','') for z in range(3)}
        blocks.update({(1,1,z): ('stair','east') for z in range(3)})
        blocks.update({(x,1,z): ('full','') for x in (2,3) for z in range(3)})
        # quarter-grid u=1 -> x=.375; u=13 -> x=3.375; y is feet height.
        self.assertTrue(nav.Scene(blocks).search((1,1,5), [(13,2,5)])['reached'][0])
        blocks.update({(1,1,z): ('full','') for z in range(3)})
        self.assertFalse(nav.Scene(blocks).search((1,1,5), [(13,2,5)])['reached'][0])

    def test_pillar_intrusion_cannot_be_hidden_by_one_person_path(self):
        g = load('geometry03_model')
        lane = {(x,1,0) for x in range(4)}
        self.assertEqual(g.clear_width(lane, {(3,2,0)}, 1), 3)
        self.assertEqual(g.clear_width(lane, {(x,4,0) for x in range(4)}, 1), 4)

    def test_wall_link_raster_keeps_face_connected_masonry(self):
        g=load('geometry03_model')
        self.assertTrue(hasattr(g,'link_arch'),'wall-link face-connection raster is missing')
        cells=g.link_arch(10,0)
        self.assertIn((15,15,0),cells)
        self.assertIn((21,15,0),cells)
        self.assertEqual(load('geometry03_audit').connected(cells),1)

    def test_balcony_attachment_rail_opens_only_at_same_floor(self):
        g=load('geometry03_model')
        self.assertTrue(hasattr(g,'rail_openings'),'balcony attachment rail mask is missing')
        self.assertEqual(g.rail_openings({(20,11,0),(20,22,0)}, {(20,10,0)}),{(20,22,0)})


if __name__ == '__main__':
    unittest.main()
