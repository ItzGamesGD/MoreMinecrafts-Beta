import unittest
import numpy as np
from bottleneck import widest_clearance

class BottleneckTest(unittest.TestCase):
    def test_narrow_bridge_limits_clearance(self):
        a=np.zeros((13,7,7),bool)
        a[1:5,1:6,1:6]=True
        a[8:12,1:6,1:6]=True
        a[5:8,3,3]=True
        self.assertEqual(widest_clearance(a, [(3,3,3),(9,3,3)], [1,2,3])[(0,1)],1)
    def test_disconnected_is_zero(self):
        a=np.zeros((5,5,5),bool); a[1,1,1]=True; a[3,3,3]=True
        self.assertEqual(widest_clearance(a, [(1,1,1),(3,3,3)], [1])[(0,1)],0)
