import unittest
import numpy as np
from analysis import components, clearance

class AnalysisTest(unittest.TestCase):
    def test_diagonal_air_does_not_connect(self):
        a=np.zeros((3,3,3),dtype=bool)
        a[0,0,0]=a[1,1,1]=True
        labels,count=components(a)
        self.assertEqual(count,2)
        self.assertNotEqual(labels[0,0,0],labels[1,1,1])

    def test_clearance_counts_boundary_as_solid(self):
        a=np.ones((5,5,5),dtype=bool)
        d=clearance(a)
        self.assertEqual(d[2,2,2],3)
        self.assertEqual(d[0,0,0],1)

if __name__ == '__main__': unittest.main()

class CoreTest(unittest.TestCase):
    def test_separate_cores_with_narrow_connection_remain_distinct(self):
        from analysis import core_summary
        a=np.zeros((15,7,7),dtype=bool)
        a[1:6,1:6,1:6]=True
        a[9:14,1:6,1:6]=True
        a[6:9,3,3]=True
        result=core_summary(a,2)
        self.assertEqual(result['air_components'],1)
        self.assertEqual(result['core_count'],2)

class BoundaryTest(unittest.TestCase):
    def test_boundary_component_ids_include_all_faces(self):
        from analysis import boundary_component_ids
        labels=np.zeros((5,5,5),dtype=int)
        labels[0,2,2]=4; labels[2,2,2]=5; labels[2,2,4]=6
        self.assertEqual(boundary_component_ids(labels), {4,6})
