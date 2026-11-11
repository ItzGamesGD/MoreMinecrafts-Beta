import importlib.util, pathlib, unittest
p=pathlib.Path(__file__).with_name('p5_width.py')
s=importlib.util.spec_from_file_location('w',p); w=importlib.util.module_from_spec(s)
if p.exists(): s.loader.exec_module(w)
class WidthTests(unittest.TestCase):
 def test_axis(self):
  self.assertTrue(hasattr(w,'ray_box'),'missing analytic box intersection')
  self.assertEqual(w.ray_box(1,0,(15.5,-.5,16.5,.5),.3),(15.2,16.8))
 def test_diagonal(self):
  self.assertTrue(hasattr(w,'ray_box'))
  import math
  d=math.sqrt(.5)
  a,b=w.ray_box(d,d,(1,1,2,2),.3)
  self.assertAlmostEqual(b-a,1.6/d)
 def test_subtract(self):
  self.assertTrue(hasattr(w,'subtract'))
  self.assertEqual(w.subtract([(15.5,19.5)],[(15.5,15.8),(19.2,20)]),[(15.8,19.2)])
if __name__=='__main__': unittest.main()
