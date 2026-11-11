import importlib.util,pathlib,unittest
H=pathlib.Path(__file__).resolve().parent;s=importlib.util.spec_from_file_location('v',H/'p5_verify.py');v=importlib.util.module_from_spec(s);s.loader.exec_module(v)
class ReviewTests(unittest.TestCase):
 def test_full_block_overrides_bottom_slab(self):
  rows,obj,res=v.read(H/'data','C1');rows.append(dict(group='tower_base_core',x='-19',y='16',z='-4',shape='full',facing=''))
  r=v.check(rows,obj,res,'C1',False)
  self.assertFalse(r['headroom']['pass'])
  self.assertFalse(r['overlaps']['pass'])
if __name__=='__main__':unittest.main()
