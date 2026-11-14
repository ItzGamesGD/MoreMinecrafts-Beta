import unittest
from structures import intersects
class StructureTest(unittest.TestCase):
    def test_sphere_touches_box(self):
        self.assertTrue(intersects((0,0,0),2,(2,-1,-1,4,1,1)))
        self.assertFalse(intersects((0,0,0),2,(3,-1,-1,4,1,1)))
