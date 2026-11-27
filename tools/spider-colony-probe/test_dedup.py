import unittest
from dedup import merge_overlapping_cores

class DedupTest(unittest.TestCase):
    def test_overlapping_cores_merge_but_distant_cores_remain(self):
        cores = [dict(point=[0,0,0],diameter_blocks=20,component=1),
                 dict(point=[9,0,0],diameter_blocks=20,component=1),
                 dict(point=[50,0,0],diameter_blocks=20,component=1)]
        self.assertEqual(len(merge_overlapping_cores(cores)), 2)
    def test_separate_components_never_merge(self):
        cores = [dict(point=[0,0,0],diameter_blocks=20,component=1),
                 dict(point=[1,0,0],diameter_blocks=20,component=2)]
        self.assertEqual(len(merge_overlapping_cores(cores)), 2)
