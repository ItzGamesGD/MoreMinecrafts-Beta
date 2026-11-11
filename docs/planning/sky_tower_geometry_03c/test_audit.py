import unittest
from audit import center_inside,corner_inside

class BoundaryTests(unittest.TestCase):
    def test_axis_center_and_cube_corner(self):
        self.assertTrue(center_inside(22,0,45))
        self.assertFalse(center_inside(23,0,45))
        self.assertFalse(corner_inside(22,0,45))
        self.assertTrue(corner_inside(21,0,45))
    def test_diagonal_and_balcony_exception(self):
        self.assertFalse(center_inside(17,17,45))
        self.assertTrue(center_inside(17,17,51))
        self.assertTrue(center_inside(15,15,45))
        self.assertTrue(corner_inside(15,15,45))

if __name__=='__main__':unittest.main()
