import unittest
import simulate

class BalanceTests(unittest.TestCase):
    def test_catalog_and_probabilities(self):
        simulate.validate()
        self.assertEqual(len(simulate.MOBS),40)
        for _,_,kind,target,*_ in simulate.SCENARIOS:
            self.assertIn(target,{'combat':simulate.MOBS,'delivery':simulate.ITEMS,'exploration':simulate.STRUCTURES,'special':simulate.STRUCTURES}[kind])
        self.assertEqual(sum(simulate.SPECIAL),100)
        self.assertTrue(all(sum(t[2])==100 for t in simulate.TIERS.values()))
    def test_shared_progress_and_duplicate_entries(self):
        self.assertEqual(simulate.duplicates()['kills_to_finish'],16)
        self.assertEqual(simulate.duplicates()['counters_after_8'],[8,8])
        self.assertEqual(simulate.stacking()[-1]['shared_kills'],12)
        self.assertEqual(simulate.stacking()[-1]['emerald_net_mean'],45)
    def test_delivery_consumption(self):
        self.assertEqual(simulate.delivery_competition()['completed_in_order'],[True,False])
        self.assertEqual(simulate.delivery_competition()['remaining_inventory'],0)
    def test_deterministic_simulation(self):
        self.assertEqual(simulate.run(),simulate.run())
        self.assertEqual(len(simulate.run()),8)

if __name__=='__main__': unittest.main()
