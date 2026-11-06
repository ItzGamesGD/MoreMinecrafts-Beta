"""Geometry probes: boundary failures must remain distinct from feasibility."""
import importlib.util
from pathlib import Path
import unittest


class Geometry02BTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(__file__).with_name('geometry02b_verify.py')
        assert path.exists(), 'GEOMETRY-02B verifier is missing'
        spec = importlib.util.spec_from_file_location('g02b', path)
        cls.g = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.g)

    def test_depth_two_requires_actual_backing_and_side_enclosure(self):
        full = {(x, z) for x in range(4) for z in range(-1, 2)}
        wall = full - {(0, z) for z in range(-1, 2)}
        good = self.g.probe_niche((1, 0), (1, 0), 1, wall, full)
        self.assertEqual(good['run'], 3)
        self.assertTrue(good['enclosed'])
        thin = self.g.probe_niche((2, 0), (1, 0), 1, wall, full)
        self.assertFalse(thin['enclosed'])
        side = self.g.probe_niche((1, 1), (1, 0), 1, wall, full)
        self.assertFalse(side['enclosed'])

    def test_overlap_coordinates_and_bounding_square_are_distinct(self):
        central = {(0, 0)}
        square = self.g.square((0, 0), 7)
        ring = square - self.g.square((0, 0), 3)
        self.assertEqual(sorted(square & central), [(0, 0)])
        self.assertFalse(ring & central)
        self.assertEqual(len(square), 49)
        self.assertEqual(len(ring), 40)

    def test_existing_candidates_keep_height_and_roof_open(self):
        g = self.g
        for convention in ('C1', 'C2'):
            occ, rows = g.old.build_3d('B', g.old.disc('B', 25),
                g.old.disc('B', 31) - g.old.disc('B', 25), convention, 1)
            self.assertTrue(all(0 <= y < 80 for x, y, z in occ))
            self.assertFalse(any(y >= 65 for x, y, z in occ))
            self.assertEqual([r['clear_h'] for r in rows[1:5]], [10, 11, 13, 16])


if __name__ == '__main__':
    unittest.main()
