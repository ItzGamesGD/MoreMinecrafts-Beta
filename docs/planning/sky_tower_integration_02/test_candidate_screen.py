#!/usr/bin/env python3
"""Regression checks for the candidate survey, not Minecraft movement tests."""
import csv
import gzip
import importlib.util
import json
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('candidate_screen', HERE / 'candidate_screen.py')
screen = importlib.util.module_from_spec(spec)
spec.loader.exec_module(screen)

class CandidateSurveyTests(unittest.TestCase):
    def test_rotation_inverse_and_four_turns(self):
        for x, z in [(0, 0), (24, 0), (-7, 18), (1, -9)]:
            for r in range(4):
                self.assertEqual((x,z), screen.rot(*screen.rot(x,z,r),(-r)%4))
            self.assertEqual((x,z), screen.rot(x,z,4))

    def test_prior_centered_overlap_regression(self):
        expected = {'snowy':198,'meadow':220,'forest':334,'badlands':9,'desert':4,'rocky':11}
        for style, count in expected.items():
            with self.subTest(style=style):
                self.assertEqual(count, screen.inspect(style,0,0,17,0)['overlap_count'])

    def test_grid_matches_detailed_centered_results(self):
        with (HERE / 'candidate_grid.csv').open(newline='') as f:
            grid=list(csv.DictReader(f))
        self.assertEqual(1800,len(grid))
        for row in grid:
            if row['dx']=='0' and row['dz']=='0' and row['base_y']=='17' and row['relative_rotation_quarters']=='0':
                detail=screen.inspect(row['style'],0,0,17,0)
                self.assertEqual(detail['overlap_count'],int(row['solid_overlap']))
                self.assertEqual(detail['base_columns_with_nonair_below'],int(row['nonair_below_base']))

    def test_data_covers_candidate_style_rotation_product(self):
        data = json.loads((HERE / 'candidate_phase_c.json').read_text())
        rows = data['candidates']
        self.assertEqual(72,len(rows))
        self.assertEqual(72,len({(r['candidate'],r['style'],r['relative_rotation_quarters']) for r in rows}))
        self.assertTrue(all(r['route_status']=='UNVERIFIED' and r['support_status']=='UNVERIFIED' for r in rows))
        with gzip.open(HERE / 'candidate_phase_c_voxels.csv.gz','rt') as f:
            self.assertEqual('candidate,component,x,y,z,block', f.readline().strip())

if __name__ == '__main__':
    unittest.main()
